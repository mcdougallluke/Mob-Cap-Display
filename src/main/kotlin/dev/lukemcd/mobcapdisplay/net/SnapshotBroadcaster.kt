package dev.lukemcd.mobcapdisplay.net

import dev.lukemcd.mobcapdisplay.config.ConfigManager
import dev.lukemcd.mobcapdisplay.tally.LevelSnapshot
import dev.lukemcd.mobcapdisplay.tally.LocalStats
import dev.lukemcd.mobcapdisplay.tally.MobCapTracker
import java.util.UUID
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.MobCategory

object SnapshotBroadcaster {
	private val subscribers = HashMap<UUID, Subscription>()

	fun register() {
		ServerPlayNetworking.registerGlobalReceiver(SubscribePayload.TYPE) { payload, context ->
			onSubscribe(context.player(), payload)
		}
		ServerPlayConnectionEvents.DISCONNECT.register { listener, _ -> subscribers.remove(listener.player.uuid) }
		ServerLifecycleEvents.SERVER_STOPPED.register { subscribers.clear() }
		ServerTickEvents.END_SERVER_TICK.register { server ->
			if (server.tickCount % ConfigManager.config.updateIntervalTicks == 0) broadcast(server)
		}
	}

	private fun onSubscribe(player: ServerPlayer, payload: SubscribePayload) {
		val subscription = payload.subscription
		if (subscription == null) {
			// a client on another protocol version gets a version-only reply so it can tell the player
			if (ServerPlayNetworking.canSend(player, SnapshotPayload.TYPE)) {
				ServerPlayNetworking.send(player, SnapshotPayload(Protocol.VERSION, body = null))
			}
			return
		}

		if (subscription.enabled) subscribers[player.uuid] = subscription
		else subscribers.remove(player.uuid)
	}

	private fun broadcast(server: MinecraftServer) {
		for ((uuid, subscription) in subscribers) {
			val player = server.playerList.getPlayer(uuid) ?: continue
			if (!ServerPlayNetworking.canSend(player, SnapshotPayload.TYPE)) continue
			val body = snapshotFor(server, player, subscription) ?: continue
			ServerPlayNetworking.send(player, SnapshotPayload(Protocol.VERSION, body))
		}
	}

	private fun snapshotFor(server: MinecraftServer, player: ServerPlayer, subscription: Subscription): SnapshotBody? {
		val level = player.level()
		val snapshot = MobCapTracker.snapshot(level) ?: return null

		val others =
			if (!subscription.otherDimensions) emptyList()
			else server.allLevels.filter { it != level }.mapNotNull { other ->
				MobCapTracker.snapshot(other)?.let { levelCaps(other, it, local = emptyMap()) }
			}
		return SnapshotBody(levelCaps(level, snapshot, snapshot.localFor(player.uuid)), others)
	}

	private fun levelCaps(level: ServerLevel, snapshot: LevelSnapshot, local: Map<MobCategory, LocalStats>): LevelCaps =
		LevelCaps(level.dimension(), snapshot.spawnableChunks, snapshot.gameTime, snapshot.categories, local)
}

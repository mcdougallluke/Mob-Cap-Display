package dev.lukemcd.mobcapdisplay.client

import dev.lukemcd.mobcapdisplay.net.Protocol
import dev.lukemcd.mobcapdisplay.net.SnapshotBody
import dev.lukemcd.mobcapdisplay.net.SnapshotPayload
import dev.lukemcd.mobcapdisplay.net.SubscribePayload
import dev.lukemcd.mobcapdisplay.net.Subscription
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents

enum class ServerSupport { UNKNOWN, SUPPORTED, NOT_INSTALLED, INCOMPATIBLE }

object ClientConnection {
	val hudEnabled: Boolean
		get() = HudConfigManager.config.enabled
	var support = ServerSupport.UNKNOWN
		private set
	var serverProtocol: Int? = null
		private set
	var latest: SnapshotBody? = null
		private set
	private var receivedAtMillis = 0L

	val latestAgeMillis: Long?
		get() = if (latest == null) null else System.currentTimeMillis() - receivedAtMillis

	fun register() {
		ClientPlayNetworking.registerGlobalReceiver(SnapshotPayload.TYPE) { payload, _ -> onSnapshot(payload) }
		ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
			if (ClientPlayNetworking.canSend(SubscribePayload.TYPE)) onServerHasMod() else support = ServerSupport.NOT_INSTALLED
		}
		ServerboundPlayChannelEvents.REGISTER.register { _, _, _, channels ->
			if (SubscribePayload.TYPE.id() in channels) onServerHasMod()
		}
		ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> reset() }
	}

	fun setHudEnabled(enabled: Boolean) {
		HudConfigManager.update { it.enabled = enabled }
		if (!enabled) latest = null
		if (support == ServerSupport.SUPPORTED) sendSubscription()
	}

	private fun onServerHasMod() {
		if (support == ServerSupport.SUPPORTED || support == ServerSupport.INCOMPATIBLE) return
		support = ServerSupport.SUPPORTED
		if (hudEnabled) sendSubscription()
	}

	private fun sendSubscription() {
		ClientPlayNetworking.send(SubscribePayload(Protocol.VERSION, Subscription(hudEnabled, otherDimensions = false)))
	}

	private fun onSnapshot(payload: SnapshotPayload) {
		val body = payload.body
		if (body == null) {
			support = ServerSupport.INCOMPATIBLE
			serverProtocol = payload.protocol
			latest = null
			return
		}
		if (!hudEnabled) return
		latest = body
		receivedAtMillis = System.currentTimeMillis()
	}

	private fun reset() {
		support = ServerSupport.UNKNOWN
		serverProtocol = null
		latest = null
	}
}

package dev.lukemcd.mobcapdisplay

import dev.lukemcd.mobcapdisplay.command.BreakdownCommand
import dev.lukemcd.mobcapdisplay.config.ConfigManager
import dev.lukemcd.mobcapdisplay.net.SnapshotBroadcaster
import dev.lukemcd.mobcapdisplay.net.SnapshotPayload
import dev.lukemcd.mobcapdisplay.net.SubscribePayload
import dev.lukemcd.mobcapdisplay.tally.CapMath
import dev.lukemcd.mobcapdisplay.tally.MobCapTracker
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object MobCapDisplay : ModInitializer {
	const val MOD_ID: String = "mob-cap-display"

	val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		ConfigManager.load()
		PayloadTypeRegistry.serverboundPlay().register(SubscribePayload.TYPE, SubscribePayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(SnapshotPayload.TYPE, SnapshotPayload.CODEC)
		ServerLevelEvents.UNLOAD.register { _, level -> MobCapTracker.forget(level) }
		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> BreakdownCommand.register(dispatcher) }
		SnapshotBroadcaster.register()

		if (FabricLoader.getInstance().isDevelopmentEnvironment && !CapMath.matchesVanilla()) {
			LOGGER.warn("NaturalSpawner.MAGIC_NUMBER no longer matches CapMath; global caps will be wrong")
		}
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}

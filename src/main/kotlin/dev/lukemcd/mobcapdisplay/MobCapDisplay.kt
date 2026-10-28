package dev.lukemcd.mobcapdisplay

import dev.lukemcd.mobcapdisplay.tally.CapMath
import dev.lukemcd.mobcapdisplay.tally.MobCapTracker
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object MobCapDisplay : ModInitializer {
	const val MOD_ID: String = "mob-cap-display"

	val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		ServerLevelEvents.UNLOAD.register { _, level -> MobCapTracker.forget(level) }

		if (FabricLoader.getInstance().isDevelopmentEnvironment && !CapMath.matchesVanilla()) {
			LOGGER.warn("NaturalSpawner.MAGIC_NUMBER no longer matches CapMath; global caps will be wrong")
		}
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}

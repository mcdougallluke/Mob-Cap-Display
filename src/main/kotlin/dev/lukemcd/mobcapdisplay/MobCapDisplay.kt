package dev.lukemcd.mobcapdisplay

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object MobCapDisplay : ModInitializer {
	const val MOD_ID: String = "mob-cap-display"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}

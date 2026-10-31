package dev.lukemcd.mobcapdisplay.client

import dev.lukemcd.mobcapdisplay.MobCapDisplay
import dev.lukemcd.mobcapdisplay.config.JsonConfigFile

object HudConfigManager {
	private val file = JsonConfigFile("${MobCapDisplay.MOD_ID}-client.json", HudConfig::class.java, ::HudConfig)

	var config = HudConfig()
		private set

	fun load() {
		config = file.load().also(::validate)
	}

	fun update(change: (HudConfig) -> Unit) {
		change(config)
		file.save(config)
	}

	// Gson leaves an unknown or null enum value as null despite the Kotlin type
	@Suppress("SENSELESS_COMPARISON")
	private fun validate(config: HudConfig) {
		if (config.counts == null) config.counts = HudCounts.BOTH
		config.typesPerCategory = config.typesPerCategory.coerceIn(HudConfig.TYPES_PER_CATEGORY_RANGE)
		config.scale = config.scale.coerceIn(HudConfig.SCALE_RANGE)
		config.positionX = config.positionX.coerceIn(0.0, 1.0)
		config.positionY = config.positionY.coerceIn(0.0, 1.0)
	}
}

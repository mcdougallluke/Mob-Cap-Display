package dev.lukemcd.mobcapdisplay.config

import dev.lukemcd.mobcapdisplay.MobCapDisplay

object ConfigManager {
	private val file = JsonConfigFile("${MobCapDisplay.MOD_ID}.json", ModConfig::class.java, ::ModConfig)

	var config = ModConfig()
		private set

	fun load() {
		config = file.load()
		validate(config)
	}

	private fun validate(config: ModConfig) {
		val interval = config.updateIntervalTicks.coerceIn(ModConfig.UPDATE_INTERVAL_RANGE)
		if (interval != config.updateIntervalTicks) {
			MobCapDisplay.LOGGER.warn(
				"updateIntervalTicks {} is outside {}, using {}",
				config.updateIntervalTicks, ModConfig.UPDATE_INTERVAL_RANGE, interval,
			)
			config.updateIntervalTicks = interval
		}
	}
}

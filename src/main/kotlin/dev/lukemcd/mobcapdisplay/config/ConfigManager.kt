package dev.lukemcd.mobcapdisplay.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import dev.lukemcd.mobcapdisplay.MobCapDisplay
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import net.fabricmc.loader.api.FabricLoader

object ConfigManager {
	private val path: Path = FabricLoader.getInstance().configDir.resolve("${MobCapDisplay.MOD_ID}.json")
	private val gson = GsonBuilder().setPrettyPrinting().create()

	var config = ModConfig()
		private set

	fun load() {
		config = if (Files.exists(path)) read() else ModConfig().also(::write)
		validate(config)
	}

	// A broken file is left untouched so the player's edits survive until they fix it.
	private fun read(): ModConfig = try {
		Files.newBufferedReader(path).use { gson.fromJson(it, ModConfig::class.java) } ?: ModConfig()
	} catch (e: IOException) {
		fallBackToDefaults(e)
	} catch (e: JsonParseException) {
		fallBackToDefaults(e)
	}

	private fun fallBackToDefaults(cause: Exception): ModConfig {
		MobCapDisplay.LOGGER.warn("Couldn't read {}, using defaults until it's fixed: {}", path, cause.message)
		return ModConfig()
	}

	private fun write(config: ModConfig) {
		try {
			Files.createDirectories(path.parent)
			Files.newBufferedWriter(path).use { gson.toJson(config, it) }
		} catch (e: IOException) {
			MobCapDisplay.LOGGER.warn("Couldn't write default config to {}: {}", path, e.message)
		}
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

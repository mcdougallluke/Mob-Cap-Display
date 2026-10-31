package dev.lukemcd.mobcapdisplay.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import dev.lukemcd.mobcapdisplay.MobCapDisplay
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import net.fabricmc.loader.api.FabricLoader

class JsonConfigFile<T : Any>(fileName: String, private val type: Class<T>, private val defaults: () -> T) {
	private val path: Path = FabricLoader.getInstance().configDir.resolve(fileName)

	fun load(): T = if (Files.exists(path)) read() else defaults().also(::save)

	fun save(config: T) {
		try {
			Files.createDirectories(path.parent)
			Files.newBufferedWriter(path).use { GSON.toJson(config, it) }
		} catch (e: IOException) {
			MobCapDisplay.LOGGER.warn("Couldn't write {}: {}", path, e.message)
		}
	}

	// A broken file is left untouched so the player's edits survive until they fix it.
	private fun read(): T = try {
		Files.newBufferedReader(path).use { GSON.fromJson(it, type) } ?: defaults()
	} catch (e: IOException) {
		fallBackToDefaults(e)
	} catch (e: JsonParseException) {
		fallBackToDefaults(e)
	}

	private fun fallBackToDefaults(cause: Exception): T {
		MobCapDisplay.LOGGER.warn("Couldn't read {}, using defaults until it's fixed: {}", path, cause.message)
		return defaults()
	}

	private companion object {
		val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
	}
}

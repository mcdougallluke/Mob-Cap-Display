package dev.lukemcd.mobcapdisplay.tally

import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.NaturalSpawner

object CapMath {
	// Copy of NaturalSpawner.MAGIC_NUMBER, per-chunk caps are defined in a 17x17 chunk area
	const val REFERENCE_CHUNK_AREA = 17 * 17

	fun globalCap(category: MobCategory, spawnableChunks: Int): Int =
		category.maxInstancesPerChunk * spawnableChunks / REFERENCE_CHUNK_AREA

	fun matchesVanilla(): Boolean = runCatching {
		NaturalSpawner::class.java.getDeclaredField("MAGIC_NUMBER")
			.apply { isAccessible = true }
			.getInt(null)
	}.getOrNull() == REFERENCE_CHUNK_AREA
}

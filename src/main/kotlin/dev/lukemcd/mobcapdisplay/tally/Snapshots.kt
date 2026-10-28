package dev.lukemcd.mobcapdisplay.tally

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory

data class TypeCount(val type: EntityType<*>, val count: Int)

data class CategoryStats(
	val cap: Int,
	val vanillaCount: Int,
	val perType: List<TypeCount>,
) {
	val count: Int = perType.sumOf { it.count }
	val matchesVanilla: Boolean get() = count == vanillaCount
}

data class LevelSnapshot(
	val gameTime: Long,
	val spawnableChunks: Int,
	val categories: Map<MobCategory, CategoryStats>,
) {
	val matchesVanilla: Boolean get() = categories.values.all { it.matchesVanilla }
}

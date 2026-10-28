package dev.lukemcd.mobcapdisplay.tally

import java.util.UUID
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

data class LocalStats(
	val cap: Int,
	val perType: List<TypeCount>,
) {
	val count: Int = perType.sumOf { it.count }
}

data class LevelSnapshot(
	val gameTime: Long,
	val spawnableChunks: Int,
	val categories: Map<MobCategory, CategoryStats>,
	val localByPlayer: Map<UUID, Map<MobCategory, LocalStats>>,
) {
	val matchesVanilla: Boolean get() = categories.values.all { it.matchesVanilla }

	fun localFor(player: UUID): Map<MobCategory, LocalStats> =
		localByPlayer[player] ?: categories.keys.associateWith { LocalStats(CapMath.localCap(it), emptyList()) }
}

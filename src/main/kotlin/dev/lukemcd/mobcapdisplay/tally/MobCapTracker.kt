package dev.lukemcd.mobcapdisplay.tally

import dev.lukemcd.mobcapdisplay.MobCapDisplay
import dev.lukemcd.mobcapdisplay.config.ConfigManager
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
import java.util.EnumMap
import java.util.UUID
import java.util.function.Consumer
import java.util.function.Supplier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ChunkMap
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.NaturalSpawner.SpawnState
import net.minecraft.world.level.chunk.LevelChunk

object MobCapTracker {
	private const val MISMATCH_WARNING_INTERVAL_MS = 60_000L

	val spawningCategories: List<MobCategory> = MobCategory.entries.filter { it != MobCategory.MISC }

	private val largestFirst = compareByDescending<TypeCount> { it.count }
		.thenBy { EntityType.getKey(it.type).toString() }

	private val snapshots = HashMap<ResourceKey<Level>, LevelSnapshot>()
	private var activeSample: Sample? = null
	private var lastMismatchWarningAt = 0L

	fun snapshot(level: ServerLevel): LevelSnapshot? = snapshots[level.dimension()]

	fun forget(level: ServerLevel) {
		snapshots.remove(level.dimension())
	}

	@JvmStatic
	fun sample(level: ServerLevel, createState: Supplier<SpawnState>): SpawnState {
		if (level.server.tickCount % ConfigManager.config.updateIntervalTicks != 0) return createState.get()

		val sample = Sample(level.chunkSource.chunkMap)
		activeSample = sample
		try {
			return createState.get().also { commit(level, sample, it) }
		} finally {
			activeSample = null
		}
	}

	@JvmStatic
	fun wrap(countEntity: Consumer<LevelChunk>, entity: Entity, category: MobCategory): Consumer<LevelChunk> {
		val sample = activeSample ?: return countEntity
		return Consumer { chunk ->
			countEntity.accept(chunk)
			sample.global.add(category, entity.type)
			// vanilla only counts Mobs toward the per-player local cap
			if (entity is Mob) sample.addLocal(chunk.pos, category, entity.type)
		}
	}

	private fun commit(level: ServerLevel, sample: Sample, state: SpawnState) {
		val vanillaCounts = state.mobCategoryCounts
		val snapshot = LevelSnapshot(
			gameTime = level.gameTime,
			spawnableChunks = state.spawnableChunkCount,
			categories = spawningCategories.associateWith { category ->
				CategoryStats(
					cap = CapMath.globalCap(category, state.spawnableChunkCount),
					vanillaCount = vanillaCounts.getInt(category),
					perType = sample.global.typeCounts(category),
				)
			},
			localByPlayer = sample.local.mapValues { (_, tally) ->
				spawningCategories.associateWith { LocalStats(CapMath.localCap(it), tally.typeCounts(it)) }
			},
		)
		snapshots[level.dimension()] = snapshot
		if (!snapshot.matchesVanilla) warnMismatch(level, snapshot)
	}

	private fun warnMismatch(level: ServerLevel, snapshot: LevelSnapshot) {
		val now = System.currentTimeMillis()
		if (now - lastMismatchWarningAt < MISMATCH_WARNING_INTERVAL_MS) return
		lastMismatchWarningAt = now

		val mismatches = snapshot.categories
			.filterValues { !it.matchesVanilla }
			.entries.joinToString { (category, stats) ->
				"${category.serializedName} ours=${stats.count} vanilla=${stats.vanillaCount}"
			}
		MobCapDisplay.LOGGER.warn("Per-type tally disagrees with vanilla in {}: {}", level.dimension().identifier(), mismatches)
	}

	private class Sample(private val chunkMap: ChunkMap) {
		val global = TypeTally()
		val local = HashMap<UUID, TypeTally>()
		private val playersNearChunk = HashMap<ChunkPos, List<ServerPlayer>>()

		fun addLocal(chunkPos: ChunkPos, category: MobCategory, type: EntityType<*>) {
			val players = playersNearChunk.getOrPut(chunkPos) { chunkMap.getPlayersCloseForSpawning(chunkPos) }
			for (player in players) local.getOrPut(player.uuid) { TypeTally() }.add(category, type)
		}
	}

	private class TypeTally {
		private val counts = EnumMap<MobCategory, Reference2IntOpenHashMap<EntityType<*>>>(MobCategory::class.java)

		fun add(category: MobCategory, type: EntityType<*>) {
			counts.getOrPut(category) { Reference2IntOpenHashMap() }.addTo(type, 1)
		}

		fun typeCounts(category: MobCategory): List<TypeCount> =
			counts[category].orEmpty()
				.map { (type, count) -> TypeCount(type, count) }
				.sortedWith(largestFirst)
	}
}

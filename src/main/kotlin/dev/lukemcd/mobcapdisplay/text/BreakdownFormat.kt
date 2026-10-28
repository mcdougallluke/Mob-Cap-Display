package dev.lukemcd.mobcapdisplay.text

import dev.lukemcd.mobcapdisplay.tally.CapMath
import dev.lukemcd.mobcapdisplay.tally.CategoryStats
import dev.lukemcd.mobcapdisplay.tally.LevelSnapshot
import dev.lukemcd.mobcapdisplay.tally.TypeCount
import kotlin.math.roundToInt
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentUtils
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.Level

object BreakdownFormat {
	private const val TEXT = 0xE6E6E6
	private const val SECONDARY = 0xABB2BF
	private const val MUTED = 0x7F848E
	private const val DIM = 0x4B5263
	private const val GREEN = 0x7BC67B
	private const val AMBER = 0xE5C07B
	private const val RED = 0xE06C75
	private const val BLUE = 0x61AFEF

	private const val BAR_SEGMENTS = 20
	private const val OVERVIEW_TYPE_LIMIT = 5
	// chat font is proportional, 20 bar segments plus two spaces is about twelve spaces wide
	private const val TYPE_LINE_INDENT = "            "

	fun overview(dimension: ResourceKey<Level>, snapshot: LevelSnapshot): List<Component> {
		val (empty, populated) = snapshot.categories.entries.partition { (_, stats) -> stats.count == 0 && stats.matchesVanilla }

		val lines = mutableListOf<Component>(overviewHeader(dimension, snapshot))
		for ((category, stats) in populated) {
			lines += categoryLine(category, stats, snapshot.spawnableChunks)
			if (stats.perType.isNotEmpty()) lines += typeSummary(stats.perType)
		}
		if (empty.isNotEmpty()) {
			lines += text("Empty: ${empty.joinToString { displayName(it.key) }}", MUTED)
		}
		return lines
	}

	fun detail(dimension: ResourceKey<Level>, category: MobCategory, stats: CategoryStats): List<Component> {
		val header = text(displayName(category), TEXT)
			.append(separator())
			.append(text(displayName(dimension), SECONDARY))
			.append(separator())
			.append(capUsage(stats))
			.append(text("  ${capStatus(stats)}", MUTED))
		if (!stats.matchesVanilla) header.append(mismatch(stats))

		if (stats.perType.isEmpty()) return listOf(header, text(" No mobs counted", MUTED))
		return listOf(header) + stats.perType.map { typeCount ->
			Component.literal(" ")
				.append(bar(typeCount.count, stats.count, BLUE))
				.append("  ")
				.append(typeEntry(typeCount))
		}
	}

	private fun overviewHeader(dimension: ResourceKey<Level>, snapshot: LevelSnapshot): MutableComponent =
		text("Mob caps", TEXT)
			.append(separator())
			.append(text(displayName(dimension), SECONDARY))
			.append(separator())
			.append(
				text("${snapshot.spawnableChunks} chunks", MUTED)
					.withHover(text("Spawnable chunks. Global caps scale with this count.", TEXT))
			)

	private fun categoryLine(category: MobCategory, stats: CategoryStats, spawnableChunks: Int): MutableComponent {
		val line = Component.empty()
			.append(bar(stats.count, stats.cap, capColor(stats)))
			.append("  ")
			.append(categoryLink(category, stats, spawnableChunks))
			.append(" ")
			.append(capUsage(stats))
		if (!stats.matchesVanilla) line.append(mismatch(stats))
		return line
	}

	private fun categoryLink(category: MobCategory, stats: CategoryStats, spawnableChunks: Int): MutableComponent {
		val perChunk = category.maxInstancesPerChunk
		val tooltip = text("Cap ${stats.cap} = $perChunk × $spawnableChunks ÷ ${CapMath.REFERENCE_CHUNK_AREA}\n", TEXT)
			.append(text("Vanilla counted ${stats.vanillaCount}\n", SECONDARY))
			.append(text("Click to list every mob type", MUTED))
		return text(displayName(category), TEXT).withStyle {
			it.withHoverEvent(HoverEvent.ShowText(tooltip))
				.withClickEvent(ClickEvent.RunCommand("/mcd breakdown ${category.serializedName}"))
		}
	}

	private fun typeSummary(perType: List<TypeCount>): MutableComponent {
		val line = Component.literal(TYPE_LINE_INDENT)
			.append(ComponentUtils.formatList(perType.take(OVERVIEW_TYPE_LIMIT), separator(), ::typeEntry))

		val hidden = perType.drop(OVERVIEW_TYPE_LIMIT)
		if (hidden.isNotEmpty()) {
			val tooltip = ComponentUtils.formatList(hidden, Component.literal("\n"), ::typeEntry)
			line.append(separator()).append(text("+${hidden.size} more", MUTED).withHover(tooltip))
		}
		return line
	}

	private fun typeEntry(typeCount: TypeCount): Component =
		text("${typeCount.count} ", TEXT).append(typeCount.type.description.copy().withColor(SECONDARY))

	private fun capUsage(stats: CategoryStats): MutableComponent = text("${stats.count}/${stats.cap}", capColor(stats))

	private fun capStatus(stats: CategoryStats): String =
		if (stats.count >= stats.cap) "natural spawns blocked" else "${stats.cap - stats.count} below cap"

	private fun mismatch(stats: CategoryStats): MutableComponent = text("  ✘ vanilla ${stats.vanillaCount}", RED)

	private fun capColor(stats: CategoryStats): Int = when {
		stats.count >= stats.cap -> RED
		stats.count * 2 >= stats.cap -> AMBER
		else -> GREEN
	}

	private fun bar(part: Int, whole: Int, color: Int): MutableComponent {
		val filled = filledSegments(part, whole)
		return text("|".repeat(filled), color).append(text("|".repeat(BAR_SEGMENTS - filled), DIM))
	}

	private fun filledSegments(part: Int, whole: Int): Int {
		if (whole <= 0) return BAR_SEGMENTS
		val segments = (part * BAR_SEGMENTS.toDouble() / whole).roundToInt().coerceAtMost(BAR_SEGMENTS)
		return if (part > 0) segments.coerceAtLeast(1) else segments
	}

	private fun displayName(category: MobCategory): String =
		category.serializedName.replace('_', ' ').replaceFirstChar { it.uppercase() }

	private fun displayName(dimension: ResourceKey<Level>): String {
		val id = dimension.identifier()
		return if (id.namespace == "minecraft") id.path else id.toString()
	}

	private fun separator(): MutableComponent = text(" · ", DIM)

	private fun text(content: String, color: Int): MutableComponent = Component.literal(content).withColor(color)

	private fun MutableComponent.withHover(tooltip: Component): MutableComponent =
		withStyle { it.withHoverEvent(HoverEvent.ShowText(tooltip)) }
}

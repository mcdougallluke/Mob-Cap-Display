package dev.lukemcd.mobcapdisplay.text

import dev.lukemcd.mobcapdisplay.tally.CapMath
import dev.lukemcd.mobcapdisplay.tally.CategoryStats
import dev.lukemcd.mobcapdisplay.tally.LevelSnapshot
import dev.lukemcd.mobcapdisplay.tally.LocalStats
import dev.lukemcd.mobcapdisplay.tally.TypeCount
import dev.lukemcd.mobcapdisplay.text.Palette.AMBER
import dev.lukemcd.mobcapdisplay.text.Palette.BLUE
import dev.lukemcd.mobcapdisplay.text.Palette.DIM
import dev.lukemcd.mobcapdisplay.text.Palette.MUTED
import dev.lukemcd.mobcapdisplay.text.Palette.RED
import dev.lukemcd.mobcapdisplay.text.Palette.SECONDARY
import dev.lukemcd.mobcapdisplay.text.Palette.TEXT
import dev.lukemcd.mobcapdisplay.text.Palette.capColor
import kotlin.math.roundToInt
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentUtils
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.Level
import net.minecraft.world.level.NaturalSpawner

object BreakdownFormat {
	private const val BAR_SEGMENTS = 20
	private const val OVERVIEW_TYPE_LIMIT = 5
	// chat font is proportional, 20 bar segments plus two spaces is about twelve spaces wide
	private const val TYPE_LINE_INDENT = "            "

	private const val DIMENSION_COMMAND = "/mcd breakdown"
	private const val NEAR_YOU_COMMAND = "/mcd breakdown local"

	private enum class View { DIMENSION, NEAR_YOU }

	fun dimensionOverview(dimension: ResourceKey<Level>, snapshot: LevelSnapshot, showToggle: Boolean): List<Component> {
		val header = text("Mob caps", TEXT)
			.append(separator())
			.append(text(dimension.displayName, SECONDARY))
			.append(separator())
			.append(
				text("${snapshot.spawnableChunks} chunks", MUTED)
					.withHover(text("Spawnable chunks. Global caps scale with this count.", TEXT))
			)
		if (showToggle) header.append(viewToggle(View.DIMENSION, category = null))

		val rows = snapshot.categories.map { (category, stats) ->
			Row(category, stats.count, stats.cap, stats.perType, globalTooltip(category, stats, snapshot.spawnableChunks), DIMENSION_COMMAND, mismatch(stats))
		}
		return overviewLines(header, rows)
	}

	fun nearYouOverview(local: Map<MobCategory, LocalStats>): List<Component> {
		val header = text("Mob caps", TEXT)
			.append(separator())
			.append(text("near you", SECONDARY))
			.append(separator())
			.append(text("within ${NaturalSpawner.SPAWN_DISTANCE_BLOCK} blocks", MUTED))
			.append(viewToggle(View.NEAR_YOU, category = null))

		val rows = local.map { (category, stats) ->
			Row(category, stats.count, stats.cap, stats.perType, localTooltip(stats), NEAR_YOU_COMMAND, warning = null)
		}
		return overviewLines(header, rows)
	}

	fun dimensionDetail(dimension: ResourceKey<Level>, category: MobCategory, stats: CategoryStats, showToggle: Boolean): List<Component> {
		val status = if (stats.count >= stats.cap) "natural spawns blocked" else "${stats.cap - stats.count} below cap"
		val header = text(category.displayName, TEXT)
			.append(separator())
			.append(text(dimension.displayName, SECONDARY))
			.append(separator())
			.append(capUsage(stats.count, stats.cap))
			.append(text("  $status", MUTED))
		mismatch(stats)?.let(header::append)
		if (showToggle) header.append(viewToggle(View.DIMENSION, category))
		return detailLines(header, stats.perType, stats.count)
	}

	fun nearYouDetail(category: MobCategory, global: CategoryStats, local: LocalStats): List<Component> {
		val status = when {
			global.count >= global.cap -> "natural spawns blocked"
			local.count >= local.cap -> "spawns blocked near you"
			else -> "${local.cap - local.count} below your cap"
		}
		val header = text(category.displayName, TEXT)
			.append(separator())
			.append(text("near you", SECONDARY))
			.append(separator())
			.append(capUsage(local.count, local.cap))
			.append(text("  $status", MUTED))
			.append(viewToggle(View.NEAR_YOU, category))
		return detailLines(header, local.perType, local.count)
	}

	private class Row(
		val category: MobCategory,
		val count: Int,
		val cap: Int,
		val perType: List<TypeCount>,
		val tooltip: Component,
		val viewCommand: String,
		val warning: Component?,
	)

	private fun overviewLines(header: Component, rows: List<Row>): List<Component> {
		val (empty, populated) = rows.partition { it.count == 0 && it.warning == null }

		val lines = mutableListOf(header)
		for (row in populated) {
			lines += rowLine(row)
			if (row.perType.isNotEmpty()) lines += typeSummary(row.perType)
		}
		if (empty.isNotEmpty()) {
			lines += text("Empty: ${empty.joinToString { it.category.displayName }}", MUTED)
		}
		return lines
	}

	private fun rowLine(row: Row): MutableComponent {
		val line = Component.empty()
			.append(bar(row.count, row.cap, capColor(row.count, row.cap)))
			.append("  ")
			.append(
				text(row.category.displayName, TEXT).withStyle {
					it.withHoverEvent(HoverEvent.ShowText(row.tooltip))
						.withClickEvent(ClickEvent.RunCommand("${row.viewCommand} ${row.category.serializedName}"))
				}
			)
			.append(" ")
			.append(capUsage(row.count, row.cap))
		row.warning?.let(line::append)
		return line
	}

	private fun detailLines(header: Component, perType: List<TypeCount>, total: Int): List<Component> {
		if (perType.isEmpty()) return listOf(header, text(" No mobs counted", MUTED))
		return listOf(header) + perType.map { typeCount ->
			Component.literal(" ")
				.append(bar(typeCount.count, total, BLUE))
				.append("  ")
				.append(typeEntry(typeCount))
		}
	}

	private fun viewToggle(active: View, category: MobCategory?): MutableComponent {
		val suffix = category?.let { " ${it.serializedName}" }.orEmpty()
		return Component.literal("     ")
			.append(viewOption("Dimension", active == View.DIMENSION, "$DIMENSION_COMMAND$suffix", "Show every loaded mob in this dimension"))
			.append(separator())
			.append(viewOption("Near you", active == View.NEAR_YOU, "$NEAR_YOU_COMMAND$suffix", "Show mobs counting toward your local cap"))
	}

	private fun viewOption(label: String, active: Boolean, command: String, hint: String): MutableComponent =
		if (active) text(label, TEXT).withStyle { it.withUnderlined(true) }
		else text(label, MUTED).withStyle {
			it.withHoverEvent(HoverEvent.ShowText(text(hint, TEXT)))
				.withClickEvent(ClickEvent.RunCommand(command))
		}

	private fun globalTooltip(category: MobCategory, stats: CategoryStats, spawnableChunks: Int): Component =
		text("Cap ${stats.cap} = ${category.maxInstancesPerChunk} × $spawnableChunks ÷ ${CapMath.REFERENCE_CHUNK_AREA}\n", TEXT)
			.append(text("Vanilla counted ${stats.vanillaCount}\n", SECONDARY))
			.append(text("Click to list every mob type", MUTED))

	private fun localTooltip(stats: LocalStats): Component =
		text("Each player's local cap is ${stats.cap}\n", TEXT)
			.append(text("Counts mobs in chunks within ${NaturalSpawner.SPAWN_DISTANCE_BLOCK} blocks of you\n", SECONDARY))
			.append(text("Click to list every mob type", MUTED))

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

	private fun capUsage(count: Int, cap: Int): MutableComponent = text("$count/$cap", capColor(count, cap))

	private fun mismatch(stats: CategoryStats): Component? =
		if (stats.matchesVanilla) null else text("  ✘ vanilla ${stats.vanillaCount}", RED)

	private fun bar(part: Int, whole: Int, color: Int): MutableComponent {
		val filled = filledSegments(part, whole)
		return text("|".repeat(filled), color).append(text("|".repeat(BAR_SEGMENTS - filled), DIM))
	}

	private fun filledSegments(part: Int, whole: Int): Int {
		if (whole <= 0) return BAR_SEGMENTS
		val segments = (part * BAR_SEGMENTS.toDouble() / whole).roundToInt().coerceAtMost(BAR_SEGMENTS)
		return if (part > 0) segments.coerceAtLeast(1) else segments
	}

	private fun separator(): MutableComponent = text(" · ", DIM)

	private fun text(content: String, color: Int): MutableComponent = Component.literal(content).withColor(color)

	private fun MutableComponent.withHover(tooltip: Component): MutableComponent =
		withStyle { it.withHoverEvent(HoverEvent.ShowText(tooltip)) }
}

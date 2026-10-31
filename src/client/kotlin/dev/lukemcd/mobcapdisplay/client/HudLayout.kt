package dev.lukemcd.mobcapdisplay.client

import dev.lukemcd.mobcapdisplay.net.LevelCaps
import dev.lukemcd.mobcapdisplay.tally.CapMath
import dev.lukemcd.mobcapdisplay.tally.TypeCount
import dev.lukemcd.mobcapdisplay.text.Palette.BLUE
import dev.lukemcd.mobcapdisplay.text.Palette.DIM
import dev.lukemcd.mobcapdisplay.text.Palette.MUTED
import dev.lukemcd.mobcapdisplay.text.Palette.SECONDARY
import dev.lukemcd.mobcapdisplay.text.Palette.TEXT
import dev.lukemcd.mobcapdisplay.text.Palette.capColor
import dev.lukemcd.mobcapdisplay.text.displayName
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentUtils
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.entity.MobCategory

data class HudBar(val fraction: Float, val color: Int)

sealed interface HudLine {
	data class Row(val bar: HudBar?, val label: Component, val values: List<Component>) : HudLine
	data class Note(val text: Component) : HudLine
}

data class HudPanel(val header: HudLine.Row, val lines: List<HudLine>)

object HudLayout {
	private const val DETAIL_TYPE_LIMIT = 10

	private enum class Column(val label: String) { DIMENSION("Dimension"), NEAR_YOU("Near you") }

	private class Usage(val count: Int, val cap: Int, val perType: List<TypeCount>)

	fun overview(caps: LevelCaps, config: HudConfig): HudPanel {
		val columns = columns(config.counts)
		val lines = mutableListOf<HudLine>()
		for (category in caps.global.keys) {
			val usages = usages(caps, category, columns)
			val shown = usages.first()
			if (config.hideEmptyCategories && shown.count == 0) continue

			lines += HudLine.Row(capBar(shown), text(category.displayName, TEXT), usages.map(::capUsage))
			if (config.typesPerCategory > 0 && shown.perType.isNotEmpty()) {
				lines += HudLine.Note(typeSummary(shown.perType, config.typesPerCategory))
			}
		}
		if (lines.isEmpty()) lines += HudLine.Note(text("No mobs counted", MUTED))
		return HudPanel(header(title("Mob caps", caps), columns), lines)
	}

	fun category(caps: LevelCaps, category: MobCategory, config: HudConfig): HudPanel {
		if (category !in caps.global) return overview(caps, config)
		val columns = columns(config.counts)
		val usages = usages(caps, category, columns)
		val shown = usages.first()
		val countsByType = usages.map { usage -> usage.perType.associate { it.type to it.count } }

		val lines = mutableListOf<HudLine>(HudLine.Row(capBar(shown), text("Total", TEXT), usages.map(::capUsage)))
		if (shown.perType.isEmpty()) lines += HudLine.Note(text("No mobs counted", MUTED))
		for (typeCount in shown.perType.take(DETAIL_TYPE_LIMIT)) {
			lines += HudLine.Row(
				HudBar(typeCount.count.toFloat() / shown.count, BLUE),
				typeCount.type.description.copy().withColor(SECONDARY),
				countsByType.map { counts -> typeTotal(counts[typeCount.type] ?: 0) },
			)
		}
		val hidden = shown.perType.size - DETAIL_TYPE_LIMIT
		if (hidden > 0) lines += HudLine.Note(text("+$hidden more", MUTED))
		return HudPanel(header(title(category.displayName, caps), columns), lines)
	}

	private fun columns(counts: HudCounts): List<Column> = buildList {
		if (counts.showDimension) add(Column.DIMENSION)
		if (counts.showNearYou) add(Column.NEAR_YOU)
	}

	private fun usages(caps: LevelCaps, category: MobCategory, columns: List<Column>): List<Usage> = columns.map { column ->
		when (column) {
			Column.DIMENSION -> caps.global.getValue(category).let { Usage(it.count, it.cap, it.perType) }
			Column.NEAR_YOU -> caps.local[category]?.let { Usage(it.count, it.cap, it.perType) }
				?: Usage(0, CapMath.localCap(category), emptyList())
		}
	}

	private fun header(title: Component, columns: List<Column>): HudLine.Row =
		HudLine.Row(bar = null, title, columns.map { text(it.label, MUTED) })

	private fun title(name: String, caps: LevelCaps): Component =
		text(name, TEXT).append(separator()).append(text(caps.dimension.displayName, SECONDARY))

	private fun capBar(usage: Usage): HudBar =
		HudBar(if (usage.cap <= 0) 1f else (usage.count.toFloat() / usage.cap).coerceAtMost(1f), capColor(usage.count, usage.cap))

	private fun capUsage(usage: Usage): Component = text("${usage.count}/${usage.cap}", capColor(usage.count, usage.cap))

	private fun typeTotal(count: Int): Component = text("$count", if (count > 0) TEXT else MUTED)

	private fun typeSummary(perType: List<TypeCount>, limit: Int): Component {
		val line = Component.empty()
			.append(ComponentUtils.formatList(perType.take(limit), separator(), ::typeEntry))
		val hidden = perType.size - limit
		if (hidden > 0) line.append(separator()).append(text("+$hidden more", MUTED))
		return line
	}

	private fun typeEntry(typeCount: TypeCount): Component =
		text("${typeCount.count} ", TEXT).append(typeCount.type.description.copy().withColor(SECONDARY))

	private fun separator(): MutableComponent = text(" · ", DIM)

	private fun text(content: String, color: Int): MutableComponent = Component.literal(content).withColor(color)
}

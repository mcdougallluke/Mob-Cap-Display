package dev.lukemcd.mobcapdisplay.client

import dev.lukemcd.mobcapdisplay.net.LevelCaps
import dev.lukemcd.mobcapdisplay.tally.CapMath
import dev.lukemcd.mobcapdisplay.tally.TypeCount
import dev.lukemcd.mobcapdisplay.text.Palette.DIM
import dev.lukemcd.mobcapdisplay.text.Palette.MUTED
import dev.lukemcd.mobcapdisplay.text.Palette.SECONDARY
import dev.lukemcd.mobcapdisplay.text.Palette.TEXT
import dev.lukemcd.mobcapdisplay.text.Palette.capColor
import dev.lukemcd.mobcapdisplay.text.displayName
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory

sealed interface HudLine {
	data class Row(
		val label: Component,
		val values: List<Component>,
		val face: EntityType<*>? = null,
		val types: List<TypeCount> = emptyList(),
		val hiddenTypes: Int = 0,
	) : HudLine

	data class Note(val text: Component) : HudLine
}

data class HudPanel(val header: HudLine.Row, val lines: List<HudLine>)

object HudLayout {
	private const val DETAIL_TYPE_LIMIT = 10

	private enum class Column(val label: String) { DIMENSION("Dim"), NEAR_YOU("Near") }

	private class Usage(val count: Int, val cap: Int, val perType: List<TypeCount>)

	fun overview(caps: LevelCaps, config: HudConfig): HudPanel {
		val columns = columns(config.counts)
		val lines = mutableListOf<HudLine>()
		for (category in caps.global.keys) {
			val usages = usages(caps, category, columns)
			val shown = usages.first()
			if (config.hideEmptyCategories && shown.count == 0) continue

			val types = shown.perType.take(config.typesPerCategory)
			lines += HudLine.Row(
				text(category.displayName, TEXT),
				usages.map(::capUsage),
				types = types,
				hiddenTypes = if (types.isEmpty()) 0 else shown.perType.size - types.size,
			)
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

		val lines = mutableListOf<HudLine>(HudLine.Row(text("Total", TEXT), usages.map(::capUsage)))
		if (shown.perType.isEmpty()) lines += HudLine.Note(text("No mobs counted", MUTED))
		for (typeCount in shown.perType.take(DETAIL_TYPE_LIMIT)) {
			lines += HudLine.Row(
				typeCount.type.description.copy().withColor(SECONDARY),
				countsByType.map { counts -> typeTotal(counts[typeCount.type] ?: 0) },
				face = typeCount.type,
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
		HudLine.Row(title, columns.map { text(it.label, MUTED) })

	private fun title(name: String, caps: LevelCaps): Component =
		text(name, TEXT).append(text(" · ", DIM)).append(text(caps.dimension.displayName, SECONDARY))

	private fun capUsage(usage: Usage): Component = text("${usage.count}/${usage.cap}", capColor(usage.count, usage.cap))

	private fun typeTotal(count: Int): Component = text("$count", if (count > 0) TEXT else MUTED)

	private fun text(content: String, color: Int): MutableComponent = Component.literal(content).withColor(color)
}

package dev.lukemcd.mobcapdisplay.client

import dev.lukemcd.mobcapdisplay.tally.TypeCount
import dev.lukemcd.mobcapdisplay.text.Palette
import kotlin.math.ceil
import kotlin.math.roundToInt
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import net.minecraft.world.entity.EntityType

class HudPanelRenderer(private val font: Font, private val panel: HudPanel, private val config: HudConfig) {
	private val scale = config.scale / 100f
	private val columnRights: List<Int>
	private val typesX: Int
	private val contentWidth: Int
	private val contentHeight: Int
	val width: Int
	val height: Int

	init {
		val rows = listOf(panel.header) + panel.lines.filterIsInstance<HudLine.Row>()
		var right = rows.maxOf { labelX(it) + font.width(it.label) }
		columnRights = panel.header.values.indices.map { column ->
			right += COLUMN_GAP + rows.maxOf { font.width(it.values[column]) }
			right
		}
		typesX = right + COLUMN_GAP
		val typesEnd = rows.filter { it.types.isNotEmpty() }.maxOfOrNull { typesX + typesWidth(it) } ?: 0
		val noteEnd = panel.lines.filterIsInstance<HudLine.Note>().maxOfOrNull { font.width(it.text) } ?: 0
		contentWidth = 2 * PADDING + maxOf(right, typesEnd, noteEnd)
		contentHeight = 2 * PADDING + HEADER_GAP + (panel.lines.size + 1) * LINE_HEIGHT - (LINE_HEIGHT - TEXT_HEIGHT)
		width = ceil(contentWidth * scale).toInt()
		height = ceil(contentHeight * scale).toInt()
	}

	// Positions are fractions of the space left beside the panel, so it never leaves the screen as it grows
	fun x(screenWidth: Int, fraction: Double): Int = MARGIN + (fraction * freeSpace(screenWidth, width)).roundToInt()

	fun y(screenHeight: Int, fraction: Double): Int = MARGIN + (fraction * freeSpace(screenHeight, height)).roundToInt()

	fun fractionX(x: Int, screenWidth: Int): Double = fraction(x, freeSpace(screenWidth, width))

	fun fractionY(y: Int, screenHeight: Int): Double = fraction(y, freeSpace(screenHeight, height))

	fun draw(graphics: GuiGraphicsExtractor, x: Int, y: Int, alpha: Int = OPAQUE) {
		val pose = graphics.pose()
		pose.pushMatrix()
		pose.translate(x.toFloat(), y.toFloat())
		pose.scale(scale)
		if (config.background) graphics.fill(0, 0, contentWidth, contentHeight, BACKGROUND)

		var lineY = PADDING
		drawRow(graphics, panel.header, lineY, alpha)
		lineY += LINE_HEIGHT + HEADER_GAP
		for (line in panel.lines) {
			when (line) {
				is HudLine.Row -> drawRow(graphics, line, lineY, alpha)
				is HudLine.Note -> drawText(graphics, line.text, PADDING, lineY, alpha)
			}
			lineY += LINE_HEIGHT
		}
		pose.popMatrix()
	}

	private fun drawRow(graphics: GuiGraphicsExtractor, row: HudLine.Row, y: Int, alpha: Int) {
		row.face?.let(MobFaces::of)?.let { face ->
			face.draw(graphics, PADDING + (MobFace.MAX_SIZE - face.width) / 2, faceY(face, y), alpha)
		}
		drawText(graphics, row.label, PADDING + labelX(row), y, alpha)
		row.values.forEachIndexed { column, value ->
			drawText(graphics, value, PADDING + columnRights[column] - font.width(value), y, alpha)
		}

		var entryX = PADDING + typesX
		for (typeCount in row.types) {
			val face = MobFaces.of(typeCount.type)
			if (face != null) face.draw(graphics, entryX, faceY(face, y), alpha)
			else drawText(graphics, typeName(typeCount.type), entryX, y, alpha)
			entryX += markerWidth(typeCount.type) + MARKER_GAP
			drawText(graphics, count(typeCount), entryX, y, alpha)
			entryX += font.width(count(typeCount)) + TYPE_GAP
		}
		if (row.hiddenTypes > 0) drawText(graphics, hiddenTypes(row), entryX, y, alpha)
	}

	private fun typesWidth(row: HudLine.Row): Int {
		val entries = row.types.sumOf { markerWidth(it.type) + MARKER_GAP + font.width(count(it)) + TYPE_GAP }
		return if (row.hiddenTypes > 0) entries + font.width(hiddenTypes(row)) else entries - TYPE_GAP
	}

	private fun markerWidth(type: EntityType<*>): Int = MobFaces.of(type)?.width ?: font.width(typeName(type))

	private fun typeName(type: EntityType<*>): Component = type.description.copy().withColor(Palette.SECONDARY)

	private fun count(typeCount: TypeCount): Component = Component.literal("${typeCount.count}")

	private fun hiddenTypes(row: HudLine.Row): Component = Component.literal("+${row.hiddenTypes}").withColor(Palette.MUTED)

	private fun faceY(face: MobFace, lineY: Int): Int = lineY + TEXT_MIDDLE - face.height / 2

	private fun drawText(graphics: GuiGraphicsExtractor, text: Component, x: Int, y: Int, alpha: Int) {
		graphics.text(font, text, x, y, ARGB.color(alpha, Palette.TEXT), config.textShadow)
	}

	private fun labelX(row: HudLine.Row): Int = if (row.face == null) 0 else FACE_COLUMN

	companion object {
		const val OPAQUE = 0xFF

		private const val MARGIN = 4
		private const val PADDING = 4
		private const val LINE_HEIGHT = 11
		private const val TEXT_HEIGHT = 9
		private const val TEXT_MIDDLE = 4
		private const val HEADER_GAP = 3
		private const val COLUMN_GAP = 8
		private const val FACE_COLUMN = MobFace.MAX_SIZE + 3
		private const val MARKER_GAP = 2
		private const val TYPE_GAP = 6
		private const val BACKGROUND = 0x90101010.toInt()

		private fun freeSpace(screen: Int, panel: Int): Int = (screen - panel - 2 * MARGIN).coerceAtLeast(0)

		private fun fraction(origin: Int, freeSpace: Int): Double =
			if (freeSpace == 0) 0.0 else ((origin - MARGIN).toDouble() / freeSpace).coerceIn(0.0, 1.0)
	}
}

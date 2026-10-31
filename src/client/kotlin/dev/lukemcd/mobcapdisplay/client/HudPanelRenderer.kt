package dev.lukemcd.mobcapdisplay.client

import dev.lukemcd.mobcapdisplay.text.Palette
import kotlin.math.roundToInt
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB

class HudPanelRenderer(private val font: Font, private val panel: HudPanel, private val config: HudConfig) {
	private val columnRights: List<Int>
	val width: Int
	val height: Int

	init {
		val rows = listOf(panel.header) + panel.lines.filterIsInstance<HudLine.Row>()
		var right = rows.maxOf { labelX(it) + font.width(it.label) }
		columnRights = panel.header.values.indices.map { column ->
			right += COLUMN_GAP + rows.maxOf { font.width(it.values[column]) }
			right
		}
		val noteEnd = panel.lines.filterIsInstance<HudLine.Note>().maxOfOrNull { LABEL_INDENT + font.width(it.text) } ?: 0
		width = 2 * PADDING + maxOf(right, noteEnd)
		height = 2 * PADDING + HEADER_GAP + (panel.lines.size + 1) * LINE_HEIGHT - 2
	}

	// Positions are fractions of the space left beside the panel, so it never leaves the screen as it grows
	fun x(screenWidth: Int, fraction: Double): Int = MARGIN + (fraction * freeSpace(screenWidth, width)).roundToInt()

	fun y(screenHeight: Int, fraction: Double): Int = MARGIN + (fraction * freeSpace(screenHeight, height)).roundToInt()

	fun fractionX(x: Int, screenWidth: Int): Double = fraction(x, freeSpace(screenWidth, width))

	fun fractionY(y: Int, screenHeight: Int): Double = fraction(y, freeSpace(screenHeight, height))

	fun draw(graphics: GuiGraphicsExtractor, x: Int, y: Int, alpha: Int = OPAQUE) {
		if (config.background) graphics.fill(x, y, x + width, y + height, BACKGROUND)

		val left = x + PADDING
		var lineY = y + PADDING
		drawRow(graphics, panel.header, left, lineY, alpha)
		lineY += LINE_HEIGHT + HEADER_GAP
		for (line in panel.lines) {
			when (line) {
				is HudLine.Row -> drawRow(graphics, line, left, lineY, alpha)
				is HudLine.Note -> drawText(graphics, line.text, left + LABEL_INDENT, lineY, alpha)
			}
			lineY += LINE_HEIGHT
		}
	}

	private fun drawRow(graphics: GuiGraphicsExtractor, row: HudLine.Row, x: Int, y: Int, alpha: Int) {
		row.bar?.let { drawBar(graphics, it, x, y + BAR_TOP, alpha) }
		drawText(graphics, row.label, x + labelX(row), y, alpha)
		row.values.forEachIndexed { column, value ->
			drawText(graphics, value, x + columnRights[column] - font.width(value), y, alpha)
		}
	}

	private fun drawBar(graphics: GuiGraphicsExtractor, bar: HudBar, x: Int, y: Int, alpha: Int) {
		val filled = (bar.fraction * BAR_WIDTH).roundToInt().let { if (bar.fraction > 0f) it.coerceAtLeast(1) else it }
		graphics.fill(x, y, x + filled, y + BAR_HEIGHT, ARGB.color(alpha, bar.color))
		graphics.fill(x + filled, y, x + BAR_WIDTH, y + BAR_HEIGHT, ARGB.color(alpha, Palette.DIM))
	}

	private fun drawText(graphics: GuiGraphicsExtractor, text: Component, x: Int, y: Int, alpha: Int) {
		graphics.text(font, text, x, y, ARGB.color(alpha, Palette.TEXT), config.textShadow)
	}

	private fun labelX(row: HudLine.Row): Int = if (row.bar == null) 0 else LABEL_INDENT

	companion object {
		const val OPAQUE = 0xFF

		private const val MARGIN = 4
		private const val PADDING = 4
		private const val LINE_HEIGHT = 10
		private const val HEADER_GAP = 3
		private const val COLUMN_GAP = 10
		private const val BAR_WIDTH = 24
		private const val BAR_HEIGHT = 3
		private const val BAR_TOP = 2
		private const val LABEL_INDENT = BAR_WIDTH + 5
		private const val BACKGROUND = 0x90101010.toInt()

		private fun freeSpace(screen: Int, panel: Int): Int = (screen - panel - 2 * MARGIN).coerceAtLeast(0)

		private fun fraction(origin: Int, freeSpace: Int): Double =
			if (freeSpace == 0) 0.0 else ((origin - MARGIN).toDouble() / freeSpace).coerceIn(0.0, 1.0)
	}
}

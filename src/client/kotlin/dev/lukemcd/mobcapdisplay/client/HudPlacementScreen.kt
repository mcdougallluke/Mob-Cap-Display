package dev.lukemcd.mobcapdisplay.client

import com.mojang.blaze3d.platform.InputConstants
import dev.lukemcd.mobcapdisplay.net.LevelCaps
import dev.lukemcd.mobcapdisplay.tally.CategoryStats
import dev.lukemcd.mobcapdisplay.tally.LocalStats
import dev.lukemcd.mobcapdisplay.tally.TypeCount
import dev.lukemcd.mobcapdisplay.text.Palette
import kotlin.math.roundToInt
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.Level

class HudPlacementScreen(private val parent: Screen?) : Screen(Component.translatable("mob-cap-display.placement.title")) {
	private var positionX = HudConfigManager.config.positionX
	private var positionY = HudConfigManager.config.positionY
	private var draggingPanel = false
	private var dragOffsetX = 0.0
	private var dragOffsetY = 0.0

	override fun init() {
		val buttonY = height - BUTTON_HEIGHT - 8
		addRenderableWidget(
			Button.builder(Component.translatable("mob-cap-display.placement.reset")) { moveTo(0.0, 0.0) }
				.bounds(width / 2 - BUTTON_WIDTH - 4, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build()
		)
		addRenderableWidget(
			Button.builder(CommonComponents.GUI_DONE) { onClose() }
				.bounds(width / 2 + 4, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build()
		)
	}

	override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
		if (minecraft.level == null) super.extractBackground(graphics, mouseX, mouseY, partialTick)
		else graphics.fill(0, 0, width, height, WORLD_DIM)
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
		val renderer = renderer()
		val x = renderer.x(width, positionX)
		val y = renderer.y(height, positionY)
		val highlighted = draggingPanel || contains(renderer, x, y, mouseX.toDouble(), mouseY.toDouble())
		graphics.outline(x - 1, y - 1, renderer.width + 2, renderer.height + 2, if (highlighted) OUTLINE_ACTIVE else OUTLINE)
		renderer.draw(graphics, x, y)
		graphics.centeredText(font, Component.translatable("mob-cap-display.placement.hint"), width / 2, height - BUTTON_HEIGHT - 24, ARGB.opaque(Palette.TEXT))

		graphics.nextStratum()
		super.extractRenderState(graphics, mouseX, mouseY, partialTick)
	}

	override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
		if (super.mouseClicked(event, doubleClick)) return true
		val renderer = renderer()
		val x = renderer.x(width, positionX)
		val y = renderer.y(height, positionY)
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !contains(renderer, x, y, event.x(), event.y())) return false

		draggingPanel = true
		dragOffsetX = event.x() - x
		dragOffsetY = event.y() - y
		return true
	}

	override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
		if (!draggingPanel) return super.mouseDragged(event, dragX, dragY)
		val renderer = renderer()
		positionX = renderer.fractionX((event.x() - dragOffsetX).roundToInt(), width)
		positionY = renderer.fractionY((event.y() - dragOffsetY).roundToInt(), height)
		return true
	}

	override fun mouseReleased(event: MouseButtonEvent): Boolean {
		if (!draggingPanel) return super.mouseReleased(event)
		draggingPanel = false
		moveTo(positionX, positionY)
		return true
	}

	override fun onClose() {
		minecraft.gui.setScreen(parent)
	}

	private fun moveTo(x: Double, y: Double) {
		positionX = x
		positionY = y
		HudConfigManager.update {
			it.positionX = x
			it.positionY = y
		}
	}

	private fun renderer(): HudPanelRenderer =
		HudPanelRenderer(font, MobCapHud.panelFor(ClientConnection.latest?.current ?: PREVIEW), HudConfigManager.config)

	private fun contains(renderer: HudPanelRenderer, x: Int, y: Int, pointX: Double, pointY: Double): Boolean =
		pointX >= x && pointX < x + renderer.width && pointY >= y && pointY < y + renderer.height

	private companion object {
		const val BUTTON_WIDTH = 150
		const val BUTTON_HEIGHT = 20
		const val WORLD_DIM = 0x40000000
		const val OUTLINE = 0x60FFFFFF
		const val OUTLINE_ACTIVE = 0xFFFFFFFF.toInt()

		// shown when there is no live snapshot, e.g. when opened from the title screen
		val PREVIEW = LevelCaps(
			dimension = Level.OVERWORLD,
			spawnableChunks = 289,
			gameTime = 0,
			global = mapOf(
				MobCategory.MONSTER to CategoryStats(70, 52, listOf(TypeCount(EntityTypes.ZOMBIE, 30), TypeCount(EntityTypes.SKELETON, 14), TypeCount(EntityTypes.CREEPER, 8))),
				MobCategory.CREATURE to CategoryStats(10, 6, listOf(TypeCount(EntityTypes.COW, 4), TypeCount(EntityTypes.SHEEP, 2))),
				MobCategory.AMBIENT to CategoryStats(15, 3, listOf(TypeCount(EntityTypes.BAT, 3))),
				MobCategory.WATER_CREATURE to CategoryStats(5, 0, emptyList()),
			),
			local = mapOf(
				MobCategory.MONSTER to LocalStats(70, listOf(TypeCount(EntityTypes.ZOMBIE, 21), TypeCount(EntityTypes.SKELETON, 9))),
				MobCategory.CREATURE to LocalStats(10, listOf(TypeCount(EntityTypes.COW, 3))),
				MobCategory.AMBIENT to LocalStats(15, emptyList()),
				MobCategory.WATER_CREATURE to LocalStats(5, emptyList()),
			),
		)
	}
}

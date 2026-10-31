package dev.lukemcd.mobcapdisplay.client

import dev.lukemcd.mobcapdisplay.MobCapDisplay
import dev.lukemcd.mobcapdisplay.net.LevelCaps
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.MobCategory

object MobCapHud : HudElement {
	private const val STALE_ALPHA = 0x70

	var selectedCategory: MobCategory? = null
		private set

	fun register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, MobCapDisplay.id("mob_caps"), this)
	}

	fun cycleCategory() {
		val categories = ClientConnection.latest?.current?.global?.keys ?: return
		val views = listOf<MobCategory?>(null) + categories
		selectedCategory = views[(views.indexOf(selectedCategory) + 1) % views.size]
	}

	fun panelFor(caps: LevelCaps): HudPanel {
		val config = HudConfigManager.config
		return selectedCategory?.let { HudLayout.category(caps, it, config) } ?: HudLayout.overview(caps, config)
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
		val minecraft = Minecraft.getInstance()
		// only the full F3 screen overlaps the HUD; showDebugScreen() is also true for in-world entries like F3+B hitboxes
		if (!ClientConnection.hudEnabled || minecraft.debugEntries.isOverlayVisible) return
		if (minecraft.gui.screen() is HudPlacementScreen) return
		val caps = ClientConnection.latest?.current ?: return

		val config = HudConfigManager.config
		val renderer = HudPanelRenderer(minecraft.font, panelFor(caps), config)
		// after a dimension change the server sends nothing until the new level has been sampled
		val alpha = if (caps.dimension == minecraft.level?.dimension()) HudPanelRenderer.OPAQUE else STALE_ALPHA
		renderer.draw(graphics, renderer.x(graphics.guiWidth(), config.positionX), renderer.y(graphics.guiHeight(), config.positionY), alpha)
	}
}

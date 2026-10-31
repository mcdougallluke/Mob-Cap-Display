package dev.lukemcd.mobcapdisplay.client

import com.mojang.blaze3d.platform.InputConstants
import dev.lukemcd.mobcapdisplay.MobCapDisplay
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping

object Keybinds {
	private val category = KeyMapping.Category.register(MobCapDisplay.id("main"))
	private val toggleHud = keyMapping("toggle_hud")
	private val cycleCategory = keyMapping("cycle_category")

	fun register() {
		ClientTickEvents.END_CLIENT_TICK.register { minecraft ->
			while (toggleHud.consumeClick()) {
				minecraft.player?.sendOverlayMessage(HudStatus.setHudEnabled(!ClientConnection.hudEnabled))
			}
			while (cycleCategory.consumeClick()) MobCapHud.cycleCategory()
		}
	}

	private fun keyMapping(name: String): KeyMapping = KeyMappingHelper.registerKeyMapping(
		KeyMapping("key.${MobCapDisplay.MOD_ID}.$name", InputConstants.UNKNOWN.value, category)
	)
}

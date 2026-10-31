package dev.lukemcd.mobcapdisplay.client

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

object ModMenuIntegration : ModMenuApi {
	override fun getModConfigScreenFactory(): ConfigScreenFactory<*> = ConfigScreenFactory { HudConfigScreen(it) }
}

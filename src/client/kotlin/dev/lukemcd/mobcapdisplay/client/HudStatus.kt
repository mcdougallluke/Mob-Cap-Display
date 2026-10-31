package dev.lukemcd.mobcapdisplay.client

import dev.lukemcd.mobcapdisplay.net.Protocol
import dev.lukemcd.mobcapdisplay.text.Palette.MUTED
import dev.lukemcd.mobcapdisplay.text.Palette.TEXT
import net.minecraft.network.chat.Component

object HudStatus {
	fun setHudEnabled(enabled: Boolean): Component {
		ClientConnection.setHudEnabled(enabled)
		val state = Component.literal(if (enabled) "Mob cap HUD on" else "Mob cap HUD off").withColor(TEXT)
		if (!enabled || ClientConnection.support == ServerSupport.SUPPORTED) return state
		return state.append(Component.literal(" · ${serverDescription()}").withColor(MUTED))
	}

	fun serverDescription(): String = when (ClientConnection.support) {
		ServerSupport.UNKNOWN -> "Not connected to a server yet"
		ServerSupport.SUPPORTED -> "Server has Mob Cap Display"
		ServerSupport.NOT_INSTALLED -> "Server doesn't have Mob Cap Display"
		ServerSupport.INCOMPATIBLE ->
			"Server runs Mob Cap Display protocol v${ClientConnection.serverProtocol}, you have v${Protocol.VERSION}. Update the mod on one side."
	}
}

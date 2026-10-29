package dev.lukemcd.mobcapdisplay.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback

object MobCapDisplayClient : ClientModInitializer {
	override fun onInitializeClient() {
		ClientConnection.register()
		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ -> HudCommand.register(dispatcher) }
	}
}

package dev.lukemcd.mobcapdisplay.client

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import dev.lukemcd.mobcapdisplay.net.LevelCaps
import dev.lukemcd.mobcapdisplay.text.Palette.MUTED
import dev.lukemcd.mobcapdisplay.text.Palette.TEXT
import java.util.Locale
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component

object HudCommand {
	fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
		dispatcher.register(
			ClientCommands.literal("mcdhud")
				.then(ClientCommands.literal("on").executes { setHud(it.source, enabled = true) })
				.then(ClientCommands.literal("off").executes { setHud(it.source, enabled = false) })
				.then(ClientCommands.literal("status").executes { showStatus(it.source) })
				.then(ClientCommands.literal("config").executes { openConfig(it.source) })
		)
	}

	// chat closes itself after running the command, so the screen opens on the next tick
	private fun openConfig(source: FabricClientCommandSource): Int {
		val minecraft = source.client
		minecraft.schedule { minecraft.gui.setScreen(HudConfigScreen(null)) }
		return Command.SINGLE_SUCCESS
	}

	private fun setHud(source: FabricClientCommandSource, enabled: Boolean): Int {
		source.sendFeedback(HudStatus.setHudEnabled(enabled))
		return Command.SINGLE_SUCCESS
	}

	private fun showStatus(source: FabricClientCommandSource): Int {
		source.sendFeedback(text(HudStatus.serverDescription(), TEXT))
		source.sendFeedback(text("HUD ${if (ClientConnection.hudEnabled) "on" else "off"}", TEXT))

		val latest = ClientConnection.latest
		val age = ClientConnection.latestAgeMillis
		if (latest == null || age == null) {
			source.sendFeedback(text("No snapshot received yet", MUTED))
			return Command.SINGLE_SUCCESS
		}

		val current = latest.current
		source.sendFeedback(
			text("Last snapshot ${String.format(Locale.ROOT, "%.1f", age / 1000.0)} s ago · ${current.dimension.identifier()} · ${current.spawnableChunks} chunks", MUTED)
		)
		for (line in categorySummaries(current)) source.sendFeedback(text(line, MUTED))
		return Command.SINGLE_SUCCESS
	}

	private fun categorySummaries(caps: LevelCaps): List<String> =
		caps.global.filterValues { it.count > 0 }.map { (category, stats) ->
			val local = caps.local[category]
			val localPart = local?.let { " · you ${it.count}/${it.cap}" }.orEmpty()
			"  ${category.serializedName} ${stats.count}/${stats.cap}$localPart · ${stats.perType.size} types"
		}

	private fun text(content: String, color: Int) = Component.literal(content).withColor(color)
}

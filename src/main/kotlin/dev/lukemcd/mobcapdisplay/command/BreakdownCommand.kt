package dev.lukemcd.mobcapdisplay.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import dev.lukemcd.mobcapdisplay.tally.LevelSnapshot
import dev.lukemcd.mobcapdisplay.tally.MobCapTracker
import dev.lukemcd.mobcapdisplay.text.BreakdownFormat
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.MobCategory

object BreakdownCommand {
	private const val CATEGORY_ARGUMENT = "category"

	private val UNKNOWN_CATEGORY = DynamicCommandExceptionType { name ->
		Component.literal("Unknown mob category: $name")
	}
	private val NOT_A_PLAYER = SimpleCommandExceptionType(Component.literal("Only players have a local mob cap"))

	fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
		dispatcher.register(
			Commands.literal("mcd").then(
				Commands.literal("breakdown")
					.executes { showDimension(it.source, category = null) }
					.then(
						Commands.literal("local")
							.executes { showNearYou(it.source, category = null) }
							.then(categoryArgument().executes { showNearYou(it.source, category(it)) })
					)
					.then(categoryArgument().executes { showDimension(it.source, category(it)) })
			)
		)
	}

	private fun categoryArgument(): RequiredArgumentBuilder<CommandSourceStack, String> =
		Commands.argument(CATEGORY_ARGUMENT, StringArgumentType.word())
			.suggests { _, builder ->
				SharedSuggestionProvider.suggest(MobCapTracker.spawningCategories.map { it.serializedName }, builder)
			}

	private fun category(context: CommandContext<CommandSourceStack>): MobCategory {
		val name = StringArgumentType.getString(context, CATEGORY_ARGUMENT)
		return MobCapTracker.spawningCategories.firstOrNull { it.serializedName == name }
			?: throw UNKNOWN_CATEGORY.create(name)
	}

	private fun showDimension(source: CommandSourceStack, category: MobCategory?): Int {
		val level = source.level
		val snapshot = snapshotFor(source, level) ?: return 0
		val showToggle = source.player != null

		val lines =
			if (category == null) BreakdownFormat.dimensionOverview(level.dimension(), snapshot, showToggle)
			else BreakdownFormat.dimensionDetail(level.dimension(), category, snapshot.categories.getValue(category), showToggle)
		return send(source, lines)
	}

	private fun showNearYou(source: CommandSourceStack, category: MobCategory?): Int {
		val player = source.player ?: throw NOT_A_PLAYER.create()
		val snapshot = snapshotFor(source, player.level()) ?: return 0
		val local = snapshot.localFor(player.uuid)

		val lines =
			if (category == null) BreakdownFormat.nearYouOverview(local)
			else BreakdownFormat.nearYouDetail(category, snapshot.categories.getValue(category), local.getValue(category))
		return send(source, lines)
	}

	private fun snapshotFor(source: CommandSourceStack, level: ServerLevel): LevelSnapshot? {
		val snapshot = MobCapTracker.snapshot(level)
		if (snapshot == null) {
			source.sendFailure(Component.literal("No mob cap data for ${level.dimension().identifier()} yet, try again in a moment"))
		}
		return snapshot
	}

	private fun send(source: CommandSourceStack, lines: List<Component>): Int {
		for (line in lines) source.sendSuccess({ line }, false)
		return Command.SINGLE_SUCCESS
	}
}

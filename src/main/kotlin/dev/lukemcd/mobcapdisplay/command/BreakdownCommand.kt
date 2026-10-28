package dev.lukemcd.mobcapdisplay.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import dev.lukemcd.mobcapdisplay.tally.LevelSnapshot
import dev.lukemcd.mobcapdisplay.tally.MobCapTracker
import dev.lukemcd.mobcapdisplay.text.BreakdownFormat
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.Level

object BreakdownCommand {
	private const val CATEGORY_ARGUMENT = "category"

	private val UNKNOWN_CATEGORY = DynamicCommandExceptionType { name ->
		Component.literal("Unknown mob category: $name")
	}

	fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
		dispatcher.register(
			Commands.literal("mcd").then(
				Commands.literal("breakdown")
					.executes { showOverview(it.source) }
					.then(
						Commands.argument(CATEGORY_ARGUMENT, StringArgumentType.word())
							.suggests { _, builder ->
								SharedSuggestionProvider.suggest(MobCapTracker.spawningCategories.map { it.serializedName }, builder)
							}
							.executes { showDetail(it.source, parseCategory(StringArgumentType.getString(it, CATEGORY_ARGUMENT))) }
					)
			)
		)
	}

	private fun parseCategory(name: String): MobCategory =
		MobCapTracker.spawningCategories.firstOrNull { it.serializedName == name }
			?: throw UNKNOWN_CATEGORY.create(name)

	private fun showOverview(source: CommandSourceStack): Int =
		send(source) { dimension, snapshot -> BreakdownFormat.overview(dimension, snapshot) }

	private fun showDetail(source: CommandSourceStack, category: MobCategory): Int =
		send(source) { dimension, snapshot -> BreakdownFormat.detail(dimension, category, snapshot.categories.getValue(category)) }

	private fun send(source: CommandSourceStack, format: (ResourceKey<Level>, LevelSnapshot) -> List<Component>): Int {
		val dimension = source.level.dimension()
		val snapshot = MobCapTracker.snapshot(source.level)
		if (snapshot == null) {
			source.sendFailure(Component.literal("No mob cap data for ${dimension.identifier()} yet, try again in a moment"))
			return 0
		}

		for (line in format(dimension, snapshot)) source.sendSuccess({ line }, false)
		return Command.SINGLE_SUCCESS
	}
}

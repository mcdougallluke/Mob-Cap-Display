package dev.lukemcd.mobcapdisplay.client

import com.mojang.serialization.Codec
import net.minecraft.client.OptionInstance
import net.minecraft.client.Options
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.level.NaturalSpawner

class HudConfigScreen(private val parent: Screen?) : Screen(Component.translatable("$KEY.title")) {
	private val layout = HeaderAndFooterLayout(this)

	override fun init() {
		layout.addTitleHeader(title, font)

		val grid = GridLayout().columnSpacing(COLUMN_SPACING).rowSpacing(ROW_SPACING)
		val rows = grid.createRowHelper(2)
		for (option in options()) rows.addChild(option.createButton(minecraft.options))
		rows.addChild(
			Button.builder(Component.translatable("$KEY.move")) { minecraft.gui.setScreen(HudPlacementScreen(this)) }
				.width(2 * OPTION_WIDTH + COLUMN_SPACING).build(),
			2,
		)
		layout.addToContents(grid)

		layout.addToFooter(Button.builder(CommonComponents.GUI_DONE) { onClose() }.width(DONE_WIDTH).build())
		layout.visitWidgets { addRenderableWidget(it) }
		repositionElements()
	}

	override fun repositionElements() {
		layout.arrangeElements()
	}

	override fun onClose() {
		minecraft.gui.setScreen(parent)
	}

	private fun options(): List<OptionInstance<*>> {
		val config = HudConfigManager.config
		return listOf(
			OptionInstance.createBoolean("$KEY.enabled", config.enabled) { ClientConnection.setHudEnabled(it) },
			countsOption(config.counts),
			typesOption(config.typesPerCategory),
			toggle("hide_empty", config.hideEmptyCategories) { hudConfig, value -> hudConfig.hideEmptyCategories = value },
			toggle("background", config.background) { hudConfig, value -> hudConfig.background = value },
			toggle("text_shadow", config.textShadow) { hudConfig, value -> hudConfig.textShadow = value },
		)
	}

	private fun countsOption(initial: HudCounts): OptionInstance<HudCounts> = OptionInstance(
		"$KEY.counts",
		OptionInstance.cachedConstantTooltip(Component.translatable("$KEY.counts.tooltip", NaturalSpawner.SPAWN_DISTANCE_BLOCK)),
		{ _, value -> Component.translatable("$KEY.counts.${value.name.lowercase()}") },
		OptionInstance.Enum(HudCounts.entries, Codec.STRING.xmap(HudCounts::valueOf, HudCounts::name)),
		initial,
	) { value -> HudConfigManager.update { it.counts = value } }

	private fun typesOption(initial: Int): OptionInstance<Int> = OptionInstance(
		"$KEY.types",
		OptionInstance.cachedConstantTooltip(Component.translatable("$KEY.types.tooltip")),
		{ caption, value -> Options.genericValueOrOffLabel(caption, value) },
		OptionInstance.IntRange(HudConfig.TYPES_PER_CATEGORY_RANGE.first, HudConfig.TYPES_PER_CATEGORY_RANGE.last),
		initial,
	) { value -> HudConfigManager.update { it.typesPerCategory = value } }

	private fun toggle(name: String, initial: Boolean, apply: (HudConfig, Boolean) -> Unit): OptionInstance<Boolean> =
		OptionInstance.createBoolean("$KEY.$name", initial) { value -> HudConfigManager.update { apply(it, value) } }

	private companion object {
		const val KEY = "mob-cap-display.options"
		const val OPTION_WIDTH = 150
		const val COLUMN_SPACING = 10
		const val ROW_SPACING = 4
		const val DONE_WIDTH = 200
	}
}

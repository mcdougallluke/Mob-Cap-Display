package dev.lukemcd.mobcapdisplay.text

import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.Level

val MobCategory.displayName: String
	get() = serializedName.replace('_', ' ').replaceFirstChar { it.uppercase() }

val ResourceKey<Level>.displayName: String
	get() = identifier().let { if (it.namespace == "minecraft") it.path else it.toString() }

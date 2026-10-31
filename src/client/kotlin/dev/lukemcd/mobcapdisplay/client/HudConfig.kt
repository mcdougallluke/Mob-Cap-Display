package dev.lukemcd.mobcapdisplay.client

enum class HudCounts(val showDimension: Boolean, val showNearYou: Boolean) {
	BOTH(showDimension = true, showNearYou = true),
	DIMENSION(showDimension = true, showNearYou = false),
	NEAR_YOU(showDimension = false, showNearYou = true),
}

class HudConfig {
	var enabled = false
	var counts = HudCounts.BOTH
	var typesPerCategory = 4
	var hideEmptyCategories = false
	var background = true
	var textShadow = true
	var positionX = 0.0
	var positionY = 0.0

	companion object {
		val TYPES_PER_CATEGORY_RANGE = 0..8
	}
}

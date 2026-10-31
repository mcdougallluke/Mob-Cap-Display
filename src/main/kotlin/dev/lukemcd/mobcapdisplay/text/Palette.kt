package dev.lukemcd.mobcapdisplay.text

object Palette {
	const val TEXT = 0xE6E6E6
	const val SECONDARY = 0xABB2BF
	const val MUTED = 0x7F848E
	const val DIM = 0x4B5263
	const val GREEN = 0x7BC67B
	const val AMBER = 0xE5C07B
	const val RED = 0xE06C75
	const val BLUE = 0x61AFEF

	fun capColor(count: Int, cap: Int): Int = when {
		count >= cap -> RED
		count * 2 >= cap -> AMBER
		else -> GREEN
	}
}

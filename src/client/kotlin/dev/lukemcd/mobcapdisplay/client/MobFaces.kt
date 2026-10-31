package dev.lukemcd.mobcapdisplay.client

import kotlin.math.ceil
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes

class FacePart(val u: Int, val v: Int, val width: Int, val height: Int, val x: Int = 0, val y: Int = 0)

// Faces are cut from each mob's own texture, so resource packs carry over.
// UVs are in the model's texture size, which some vanilla textures exceed (the ghast is drawn at twice its model size).
class MobFace(path: String, private val textureWidth: Int, private val textureHeight: Int, private val parts: List<FacePart>) {
	private val texture = Identifier.withDefaultNamespace("textures/entity/$path.png")
	private val shrink = ceil(maxOf(parts.maxOf { it.x + it.width }, parts.maxOf { it.y + it.height }) / MAX_SIZE.toFloat())

	val width = ceil(parts.maxOf { it.x + it.width } / shrink).toInt()
	val height = ceil(parts.maxOf { it.y + it.height } / shrink).toInt()

	fun draw(graphics: GuiGraphicsExtractor, x: Int, y: Int, alpha: Int) {
		val pose = graphics.pose()
		pose.pushMatrix()
		pose.translate(x.toFloat(), y.toFloat())
		pose.scale(1f / shrink)
		for (part in parts) {
			graphics.blit(
				RenderPipelines.GUI_TEXTURED, texture, part.x, part.y, part.u.toFloat(), part.v.toFloat(),
				part.width, part.height, textureWidth, textureHeight, ARGB.color(alpha, 0xFFFFFF),
			)
		}
		pose.popMatrix()
	}

	companion object {
		const val MAX_SIZE = 10
	}
}

object MobFaces {
	private val HUMANOID = listOf(FacePart(8, 8, 8, 8))
	private val VILLAGER = listOf(FacePart(8, 8, 8, 10), FacePart(26, 2, 2, 3, x = 3, y = 7))
	private val PIGLIN = listOf(FacePart(8, 8, 10, 8))
	private val SPIDER = listOf(FacePart(40, 12, 8, 8))
	private val SMALL_HEAD = listOf(FacePart(5, 5, 5, 5))
	private val GUARDIAN = listOf(FacePart(16, 16, 12, 12), FacePart(9, 1, 2, 2, x = 5, y = 5))
	private val HOGLIN = listOf(FacePart(80, 6, 14, 14), FacePart(80, 20, 14, 6, y = 14))
	private val HORSE = listOf(FacePart(2, 30, 3, 5), FacePart(0, 20, 7, 5, x = 3))
	private val CAMEL = listOf(FacePart(28, 7, 7, 7), FacePart(56, 6, 5, 5, x = 1))
	private val NAUTILUS = listOf(FacePart(2, 68, 10, 8))
	private val COW = listOf(FacePart(6, 6, 8, 8), FacePart(2, 34, 6, 3, x = 1, y = 5))
	private val CAT = listOf(FacePart(5, 5, 5, 4), FacePart(2, 26, 3, 2, x = 1, y = 2))
	private val LLAMA = listOf(FacePart(6, 20, 8, 8), FacePart(9, 9, 4, 4, x = 2, y = 3))
	private val SQUID = listOf(FacePart(12, 12, 12, 16))
	private val GHAST = listOf(FacePart(16, 16, 16, 16))

	private val faces: Map<EntityType<*>, MobFace> = mapOf(
		EntityTypes.ZOMBIE to MobFace("zombie/zombie", 64, 64, HUMANOID),
		EntityTypes.HUSK to MobFace("zombie/husk", 64, 64, HUMANOID),
		EntityTypes.DROWNED to MobFace("zombie/drowned", 64, 64, HUMANOID),
		EntityTypes.GIANT to MobFace("zombie/zombie", 64, 64, HUMANOID),
		EntityTypes.ZOMBIE_VILLAGER to MobFace("zombie_villager/zombie_villager", 64, 64, VILLAGER),
		EntityTypes.SKELETON to MobFace("skeleton/skeleton", 64, 32, HUMANOID),
		EntityTypes.STRAY to MobFace("skeleton/stray", 64, 32, HUMANOID),
		EntityTypes.BOGGED to MobFace("skeleton/bogged", 64, 32, HUMANOID),
		EntityTypes.WITHER_SKELETON to MobFace("skeleton/wither_skeleton", 64, 32, HUMANOID),
		EntityTypes.PARCHED to MobFace("skeleton/parched", 64, 32, HUMANOID),
		EntityTypes.CREEPER to MobFace("creeper/creeper", 64, 32, HUMANOID),
		EntityTypes.SPIDER to MobFace("spider/spider", 64, 32, SPIDER),
		EntityTypes.CAVE_SPIDER to MobFace("spider/cave_spider", 64, 32, SPIDER),
		EntityTypes.ENDERMAN to MobFace("enderman/enderman", 64, 32, HUMANOID),
		EntityTypes.WITCH to MobFace("witch/witch", 64, 128, VILLAGER),
		EntityTypes.SLIME to MobFace(
			"slime/slime", 64, 32,
			listOf(
				FacePart(6, 22, 6, 6, x = 1, y = 1),
				FacePart(8, 8, 8, 8),
				FacePart(34, 2, 2, 2, x = 1, y = 2),
				FacePart(34, 6, 2, 2, x = 5, y = 2),
				FacePart(33, 9, 1, 1, x = 4, y = 5),
			),
		),
		EntityTypes.MAGMA_CUBE to MobFace(
			"slime/magmacube", 64, 64,
			(0 until 8).map { slice -> FacePart(if (slice < 4) 8 else 40, 8 + 9 * (slice % 4), 8, 1, y = slice) },
		),
		EntityTypes.SULFUR_CUBE to MobFace("sulfur_cube/sulfur_cube_outer", 128, 128, listOf(FacePart(18, 18, 18, 18))),
		EntityTypes.BLAZE to MobFace("blaze/blaze", 64, 32, HUMANOID),
		EntityTypes.GHAST to MobFace("ghast/ghast", 64, 32, GHAST),
		EntityTypes.PHANTOM to MobFace("phantom/phantom", 64, 64, listOf(FacePart(5, 5, 7, 3))),
		EntityTypes.PIGLIN to MobFace("piglin/piglin", 64, 64, PIGLIN),
		EntityTypes.PIGLIN_BRUTE to MobFace("piglin/piglin_brute", 64, 64, PIGLIN),
		EntityTypes.ZOMBIFIED_PIGLIN to MobFace("piglin/zombified_piglin", 64, 64, PIGLIN),
		EntityTypes.HOGLIN to MobFace("hoglin/hoglin", 128, 64, HOGLIN),
		EntityTypes.ZOGLIN to MobFace("hoglin/zoglin", 128, 64, HOGLIN),
		EntityTypes.PILLAGER to MobFace("illager/pillager", 64, 64, VILLAGER),
		EntityTypes.VINDICATOR to MobFace("illager/vindicator", 64, 64, VILLAGER),
		EntityTypes.EVOKER to MobFace("illager/evoker", 64, 64, VILLAGER),
		EntityTypes.ILLUSIONER to MobFace("illager/illusioner", 64, 64, VILLAGER),
		EntityTypes.RAVAGER to MobFace("illager/ravager", 128, 128, listOf(FacePart(16, 16, 16, 20))),
		EntityTypes.VEX to MobFace("illager/vex", 32, 32, SMALL_HEAD),
		EntityTypes.GUARDIAN to MobFace("guardian/guardian", 64, 64, GUARDIAN),
		EntityTypes.ELDER_GUARDIAN to MobFace("guardian/guardian_elder", 64, 64, GUARDIAN),
		EntityTypes.SHULKER to MobFace("shulker/shulker", 64, 64, listOf(FacePart(6, 58, 6, 6))),
		EntityTypes.SILVERFISH to MobFace("silverfish/silverfish", 64, 32, listOf(FacePart(2, 2, 3, 2))),
		EntityTypes.ENDERMITE to MobFace("endermite/endermite", 64, 32, listOf(FacePart(2, 2, 4, 3))),
		EntityTypes.WARDEN to MobFace("warden/warden", 128, 128, listOf(FacePart(10, 42, 16, 16))),
		EntityTypes.CREAKING to MobFace("creaking/creaking", 64, 64, listOf(FacePart(6, 6, 6, 10))),
		EntityTypes.BREEZE to MobFace("breeze/breeze", 32, 32, HUMANOID),
		EntityTypes.WITHER to MobFace("wither/wither", 64, 64, HUMANOID),
		EntityTypes.ZOMBIE_HORSE to MobFace("horse/horse_zombie", 64, 64, HORSE),
		EntityTypes.CAMEL_HUSK to MobFace("camel/camel_husk", 128, 128, CAMEL),
		EntityTypes.ZOMBIE_NAUTILUS to MobFace("nautilus/zombie_nautilus", 128, 128, NAUTILUS),

		EntityTypes.COW to MobFace("cow/cow_temperate", 64, 64, COW),
		EntityTypes.MOOSHROOM to MobFace("cow/mooshroom_red", 64, 64, COW),
		EntityTypes.PIG to MobFace("pig/pig_temperate", 64, 64, listOf(FacePart(8, 8, 8, 8), FacePart(17, 17, 4, 3, x = 2, y = 4))),
		EntityTypes.SHEEP to MobFace("sheep/sheep", 64, 32, listOf(FacePart(8, 8, 6, 6))),
		EntityTypes.CHICKEN to MobFace("chicken/chicken_temperate", 64, 32, listOf(FacePart(3, 3, 4, 6), FacePart(16, 2, 4, 2, y = 2))),
		EntityTypes.WOLF to MobFace(
			"wolf/wolf", 64, 32,
			listOf(FacePart(17, 15, 2, 2), FacePart(17, 15, 2, 2, x = 4), FacePart(4, 4, 6, 6, y = 2), FacePart(4, 14, 3, 3, x = 1, y = 5)),
		),
		EntityTypes.CAT to MobFace("cat/cat_tabby", 64, 32, CAT),
		EntityTypes.OCELOT to MobFace("cat/ocelot", 64, 32, CAT),
		EntityTypes.FOX to MobFace("fox/fox", 48, 32, listOf(FacePart(7, 11, 8, 6), FacePart(9, 21, 4, 2, x = 2, y = 4))),
		EntityTypes.RABBIT to MobFace("rabbit/rabbit_brown", 64, 64, listOf(FacePart(5, 21, 5, 5))),
		EntityTypes.LLAMA to MobFace("llama/llama_creamy", 128, 64, LLAMA),
		EntityTypes.TRADER_LLAMA to MobFace("llama/llama_creamy", 128, 64, LLAMA),
		EntityTypes.PANDA to MobFace("panda/panda", 64, 64, listOf(FacePart(11, 15, 10, 10), FacePart(47, 18, 7, 5, x = 1, y = 5))),
		EntityTypes.PARROT to MobFace("parrot/parrot_red_blue", 32, 32, listOf(FacePart(4, 4, 2, 3))),
		EntityTypes.POLAR_BEAR to MobFace("bear/polarbear", 128, 64, listOf(FacePart(7, 7, 7, 7), FacePart(3, 47, 5, 3, x = 1, y = 4))),
		EntityTypes.TURTLE to MobFace("turtle/turtle", 128, 64, listOf(FacePart(9, 6, 6, 5))),
		EntityTypes.BEE to MobFace("bee/bee", 64, 64, listOf(FacePart(10, 10, 7, 7))),
		EntityTypes.GOAT to MobFace("goat/goat", 64, 64, listOf(FacePart(44, 56, 5, 7))),
		EntityTypes.FROG to MobFace(
			"frog/frog_temperate", 48, 48,
			listOf(FacePart(3, 3, 3, 2), FacePart(3, 8, 3, 2, x = 4), FacePart(9, 22, 7, 3, y = 2)),
		),
		EntityTypes.SNIFFER to MobFace("sniffer/sniffer", 192, 192, listOf(FacePart(8, 15, 18, 11))),
		EntityTypes.STRIDER to MobFace("strider/strider", 64, 128, listOf(FacePart(16, 16, 16, 14))),
		EntityTypes.ARMADILLO to MobFace("armadillo/armadillo", 64, 64, listOf(FacePart(43, 15, 10, 7))),
		EntityTypes.ALLAY to MobFace("allay/allay", 32, 32, SMALL_HEAD),
		EntityTypes.TADPOLE to MobFace("tadpole/tadpole", 16, 16, listOf(FacePart(3, 3, 3, 2))),
		EntityTypes.HAPPY_GHAST to MobFace("ghast/happy_ghast", 64, 64, GHAST),
		EntityTypes.CAMEL to MobFace("camel/camel", 128, 128, CAMEL),
		EntityTypes.HORSE to MobFace("horse/horse_brown", 64, 64, HORSE),
		EntityTypes.DONKEY to MobFace("horse/donkey", 64, 64, HORSE),
		EntityTypes.MULE to MobFace("horse/mule", 64, 64, HORSE),
		EntityTypes.SKELETON_HORSE to MobFace("horse/horse_skeleton", 64, 64, HORSE),
		EntityTypes.WANDERING_TRADER to MobFace("wandering_trader/wandering_trader", 64, 64, VILLAGER),

		EntityTypes.BAT to MobFace("bat/bat", 32, 32, listOf(FacePart(2, 9, 4, 3))),
		EntityTypes.AXOLOTL to MobFace("axolotl/axolotl_lucy", 64, 64, listOf(FacePart(5, 6, 8, 5))),
		EntityTypes.GLOW_SQUID to MobFace("squid/glow_squid", 64, 32, SQUID),
		EntityTypes.SQUID to MobFace("squid/squid", 64, 32, SQUID),
		EntityTypes.DOLPHIN to MobFace("dolphin/dolphin", 64, 64, listOf(FacePart(6, 6, 8, 7), FacePart(4, 17, 2, 2, x = 3, y = 4))),
		EntityTypes.NAUTILUS to MobFace("nautilus/nautilus", 128, 128, NAUTILUS),
		EntityTypes.COD to MobFace("fish/cod", 32, 32, listOf(FacePart(11, 3, 3, 4), FacePart(0, 7, 7, 4, x = 3))),
		EntityTypes.SALMON to MobFace("fish/salmon", 32, 32, listOf(FacePart(22, 3, 3, 4, y = 1), FacePart(0, 8, 7, 5, x = 3))),
		EntityTypes.PUFFERFISH to MobFace("fish/pufferfish", 32, 32, HUMANOID),
		EntityTypes.TROPICAL_FISH to MobFace("fish/tropical_a", 32, 32, listOf(FacePart(0, 6, 6, 3))),
	)

	fun of(type: EntityType<*>): MobFace? = faces[type]
}

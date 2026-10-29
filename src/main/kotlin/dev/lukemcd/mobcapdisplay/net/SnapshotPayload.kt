package dev.lukemcd.mobcapdisplay.net

import dev.lukemcd.mobcapdisplay.MobCapDisplay
import dev.lukemcd.mobcapdisplay.tally.CategoryStats
import dev.lukemcd.mobcapdisplay.tally.LocalStats
import dev.lukemcd.mobcapdisplay.tally.TypeCount
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.DecoderException
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.Level

data class LevelCaps(
	val dimension: ResourceKey<Level>,
	val spawnableChunks: Int,
	val gameTime: Long,
	val global: Map<MobCategory, CategoryStats>,
	val local: Map<MobCategory, LocalStats>,
)

data class SnapshotBody(val current: LevelCaps, val others: List<LevelCaps>)

data class SnapshotPayload(val protocol: Int, val body: SnapshotBody?) : CustomPacketPayload {
	override fun type(): CustomPacketPayload.Type<SnapshotPayload> = TYPE

	companion object {
		val TYPE = CustomPacketPayload.Type<SnapshotPayload>(MobCapDisplay.id("snapshot"))

		val CODEC: StreamCodec<RegistryFriendlyByteBuf, SnapshotPayload> =
			Protocol.versioned(SNAPSHOT_BODY, SnapshotPayload::protocol, SnapshotPayload::body, ::SnapshotPayload)
	}
}

private val MOB_CATEGORY: StreamCodec<ByteBuf, MobCategory> = ByteBufCodecs.STRING_UTF8.map(
	{ name -> MobCategory.entries.firstOrNull { it.serializedName == name } ?: throw DecoderException("Unknown mob category $name") },
	{ it.serializedName },
)

private val TYPE_COUNTS: StreamCodec<RegistryFriendlyByteBuf, List<TypeCount>> = StreamCodec.composite(
	ByteBufCodecs.registry(Registries.ENTITY_TYPE), TypeCount::type,
	ByteBufCodecs.VAR_INT, TypeCount::count,
	::TypeCount,
).apply(ByteBufCodecs.list())

private val CATEGORY_STATS: StreamCodec<RegistryFriendlyByteBuf, CategoryStats> = StreamCodec.composite(
	ByteBufCodecs.VAR_INT, CategoryStats::cap,
	ByteBufCodecs.VAR_INT, CategoryStats::vanillaCount,
	TYPE_COUNTS, CategoryStats::perType,
	::CategoryStats,
)

private val LOCAL_STATS: StreamCodec<RegistryFriendlyByteBuf, LocalStats> = StreamCodec.composite(
	ByteBufCodecs.VAR_INT, LocalStats::cap,
	TYPE_COUNTS, LocalStats::perType,
	::LocalStats,
)

private fun <V : Any> byCategory(values: StreamCodec<RegistryFriendlyByteBuf, V>): StreamCodec<RegistryFriendlyByteBuf, Map<MobCategory, V>> =
	ByteBufCodecs.map<RegistryFriendlyByteBuf, MobCategory, V, Map<MobCategory, V>>({ LinkedHashMap(it) }, MOB_CATEGORY, values)

private val LEVEL_CAPS: StreamCodec<RegistryFriendlyByteBuf, LevelCaps> = StreamCodec.composite(
	ResourceKey.streamCodec(Registries.DIMENSION), LevelCaps::dimension,
	ByteBufCodecs.VAR_INT, LevelCaps::spawnableChunks,
	ByteBufCodecs.VAR_LONG, LevelCaps::gameTime,
	byCategory(CATEGORY_STATS), LevelCaps::global,
	byCategory(LOCAL_STATS), LevelCaps::local,
	::LevelCaps,
)

private val SNAPSHOT_BODY: StreamCodec<RegistryFriendlyByteBuf, SnapshotBody> = StreamCodec.composite(
	LEVEL_CAPS, SnapshotBody::current,
	LEVEL_CAPS.apply(ByteBufCodecs.list()), SnapshotBody::others,
	::SnapshotBody,
)

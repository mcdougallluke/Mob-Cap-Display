package dev.lukemcd.mobcapdisplay.net

import dev.lukemcd.mobcapdisplay.MobCapDisplay
import io.netty.buffer.ByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class Subscription(val enabled: Boolean, val otherDimensions: Boolean)

data class SubscribePayload(val protocol: Int, val subscription: Subscription?) : CustomPacketPayload {
	override fun type(): CustomPacketPayload.Type<SubscribePayload> = TYPE

	companion object {
		val TYPE = CustomPacketPayload.Type<SubscribePayload>(MobCapDisplay.id("subscribe"))

		private val SUBSCRIPTION: StreamCodec<ByteBuf, Subscription> = StreamCodec.composite(
			ByteBufCodecs.BOOL, Subscription::enabled,
			ByteBufCodecs.BOOL, Subscription::otherDimensions,
			::Subscription,
		)

		val CODEC: StreamCodec<RegistryFriendlyByteBuf, SubscribePayload> =
			Protocol.versioned(SUBSCRIPTION, SubscribePayload::protocol, SubscribePayload::subscription, ::SubscribePayload)
	}
}

package dev.lukemcd.mobcapdisplay.net

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

object Protocol {
	const val VERSION = 1

	// A payload from another protocol version decodes with a null body
	fun <P : Any, T : Any> versioned(
		body: StreamCodec<in RegistryFriendlyByteBuf, T>,
		protocolOf: (P) -> Int,
		bodyOf: (P) -> T?,
		create: (protocol: Int, body: T?) -> P,
	): StreamCodec<RegistryFriendlyByteBuf, P> = StreamCodec.of<RegistryFriendlyByteBuf, P>(
		{ buf, payload ->
			ByteBufCodecs.VAR_INT.encode(buf, protocolOf(payload))
			bodyOf(payload)?.let { body.encode(buf, it) }
		},
		{ buf ->
			val protocol = ByteBufCodecs.VAR_INT.decode(buf)
			if (protocol == VERSION) {
				create(protocol, body.decode(buf))
			} else {
				buf.skipBytes(buf.readableBytes())
				create(protocol, null)
			}
		},
	)
}

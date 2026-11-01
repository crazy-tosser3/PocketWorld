package dev.crzto3.pocketworld.network;

import dev.crzto3.pocketworld.client.ClientPocketState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/** Данные для окна конфигуратора: гости, число животных и ответ на последнее действие. */
public record ConfiguratorInfoPacket(List<String> invited, int animals, String message) {

    public static void encode(ConfiguratorInfoPacket packet, FriendlyByteBuf buffer) {
        buffer.writeCollection(packet.invited(), (buf, name) -> buf.writeUtf(name, 32));
        buffer.writeVarInt(packet.animals());
        buffer.writeUtf(packet.message(), 128);
    }

    public static ConfiguratorInfoPacket decode(FriendlyByteBuf buffer) {
        return new ConfiguratorInfoPacket(
                buffer.readList(buf -> buf.readUtf(32)),
                buffer.readVarInt(),
                buffer.readUtf(128)
        );
    }

    public static void handle(ConfiguratorInfoPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientPocketState.setInfo(packet.invited(), packet.animals(), packet.message())));
        context.get().setPacketHandled(true);
    }
}

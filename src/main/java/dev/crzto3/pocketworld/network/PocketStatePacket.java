package dev.crzto3.pocketworld.network;

import dev.crzto3.pocketworld.client.ClientPocketState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Сообщает клиенту небо и погоду текущего кармана. */
public record PocketStatePacket(String sky, String weather) {

    public static void encode(PocketStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.sky(), 32);
        buffer.writeUtf(packet.weather(), 32);
    }

    public static PocketStatePacket decode(FriendlyByteBuf buffer) {
        return new PocketStatePacket(buffer.readUtf(32), buffer.readUtf(32));
    }

    public static void handle(PocketStatePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientPocketState.setState(packet.sky(), packet.weather())));
        context.get().setPacketHandled(true);
    }
}

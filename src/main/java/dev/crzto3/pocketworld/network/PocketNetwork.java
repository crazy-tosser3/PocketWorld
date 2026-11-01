package dev.crzto3.pocketworld.network;

import dev.crzto3.pocketworld.PocketWorld;
import dev.crzto3.pocketworld.dimension.PocketActions;
import dev.crzto3.pocketworld.storage.PocketDimensionData.PocketDimension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.Optional;

public final class PocketNetwork {

    private static final String PROTOCOL = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PocketWorld.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private PocketNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, ConfiguratorActionPacket.class,
                ConfiguratorActionPacket::encode, ConfiguratorActionPacket::decode,
                ConfiguratorActionPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, PocketStatePacket.class,
                PocketStatePacket::encode, PocketStatePacket::decode,
                PocketStatePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, ConfiguratorInfoPacket.class,
                ConfiguratorInfoPacket::encode, ConfiguratorInfoPacket::decode,
                ConfiguratorInfoPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendState(ServerPlayer player, PocketDimension dimension) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PocketStatePacket(dimension.getSky(), dimension.getWeather()));
    }

    public static void sendInfo(ServerPlayer player, PocketDimension dimension, String message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ConfiguratorInfoPacket(
                new ArrayList<>(dimension.getInvited().values()),
                PocketActions.countAnimals(player.server, dimension),
                message
        ));
    }
}

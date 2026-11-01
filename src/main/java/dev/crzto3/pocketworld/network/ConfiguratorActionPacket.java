package dev.crzto3.pocketworld.network;

import dev.crzto3.pocketworld.dimension.PocketActions;
import dev.crzto3.pocketworld.dimension.PocketDimensionManager;
import dev.crzto3.pocketworld.dimension.PocketDimensions;
import dev.crzto3.pocketworld.dimension.PocketTravel;
import dev.crzto3.pocketworld.menu.DimensionConfiguratorMenu;
import dev.crzto3.pocketworld.storage.PocketDimensionData;
import dev.crzto3.pocketworld.storage.PocketDimensionData.PocketDimension;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Нажатие кнопки в окне конфигуратора: действие, номер варианта и необязательное имя игрока. */
public record ConfiguratorActionPacket(int action, int value, String text) {

    public static final int TERRAIN = 0;
    public static final int BIOME = 1;
    public static final int SKY = 2;
    public static final int SIZE = 3;
    public static final int BUY_ANIMAL = 4;
    public static final int REGENERATE = 5;
    public static final int WEATHER = 6;
    public static final int REFRESH = 7;
    public static final int INVITE = 8;
    public static final int UNINVITE = 9;

    public ConfiguratorActionPacket(int action, int value) {
        this(action, value, "");
    }

    public static void encode(ConfiguratorActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.action());
        buffer.writeVarInt(packet.value());
        buffer.writeUtf(packet.text(), 32);
    }

    public static ConfiguratorActionPacket decode(FriendlyByteBuf buffer) {
        return new ConfiguratorActionPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(32));
    }

    public static void handle(ConfiguratorActionPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();

            if (player != null && player.containerMenu instanceof DimensionConfiguratorMenu menu) {
                apply(player, menu, packet);
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void apply(ServerPlayer player, DimensionConfiguratorMenu menu, ConfiguratorActionPacket packet) {
        UUID pocketId = menu.getBlockEntity().getSelectedPocketId();
        PocketDimension dimension = pocketId == null ? null : PocketDimensionData.get(player.server).get(pocketId);

        if (dimension == null) {
            return;
        }

        if (packet.action() == REFRESH) {
            PocketNetwork.sendInfo(player, dimension, "");
            return;
        }

        if (PocketDimensions.isPocket(player.level())) {
            PocketNetwork.sendInfo(player, dimension, "message.pocketworld.no_config_inside");
            return;
        }

        if (!PocketTravel.canManage(dimension, player)) {
            PocketNetwork.sendInfo(player, dimension, "message.pocketworld.not_owner");
            return;
        }

        String message = "";
        var server = player.server;

        switch (packet.action()) {
            case TERRAIN -> PocketDimensionManager.setTerrain(server, pocketId, pick(
                    PocketDimensionManager.AVAILABLE_TERRAINS, packet.value()));
            case BIOME -> PocketDimensionManager.setBiome(server, pocketId, pick(
                    PocketDimensionManager.PRESET_BIOMES, packet.value()));
            case SIZE -> PocketDimensionManager.setSize(server, pocketId, pick(
                    PocketDimensionManager.AVAILABLE_SIZES, packet.value()));
            case SKY -> PocketDimensionManager.setSky(server, pocketId, pick(
                    PocketDimensionManager.AVAILABLE_SKIES, packet.value()));
            case WEATHER -> PocketDimensionManager.setWeather(server, pocketId, pick(
                    PocketDimensionManager.AVAILABLE_WEATHER, packet.value()));
            case BUY_ANIMAL -> message = PocketActions.buyAnimal(player, dimension, packet.value());
            case REGENERATE -> message = PocketActions.regenerate(server, dimension)
                    ? "message.pocketworld.regenerated"
                    : "message.pocketworld.dimension_missing";
            case INVITE -> {
                ServerPlayer target = server.getPlayerList().getPlayerByName(packet.text());

                if (target != null) {
                    PocketTravel.invite(player, target, dimension);
                }
            }
            case UNINVITE -> PocketTravel.uninvite(server, dimension, packet.text());
            default -> {
            }
        }

        // Небо и погода применяются сразу у всех на острове (тик событий сам разошлёт состояние).
        PocketNetwork.sendInfo(player, dimension, message);
    }

    private static String pick(List<String> values, int index) {
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }
}

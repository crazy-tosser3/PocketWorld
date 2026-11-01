package dev.crzto3.pocketworld.dimension;

import dev.crzto3.pocketworld.storage.PocketDimensionData;
import dev.crzto3.pocketworld.storage.PocketDimensionData.PocketDimension;
import dev.crzto3.pocketworld.storage.PocketDimensionData.ReturnPoint;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;

/** Вход, выход и приглашения в карманные измерения. */
public final class PocketTravel {

    private PocketTravel() {
    }

    /** Может ли игрок войти без бутылки: владелец, приглашённый или оператор. */
    public static boolean canVisit(PocketDimension dimension, ServerPlayer player) {
        return dimension.isOwner(player.getUUID())
                || dimension.isInvited(player.getUUID())
                || player.hasPermissions(2);
    }

    public static boolean canManage(PocketDimension dimension, ServerPlayer player) {
        return dimension.isOwner(player.getUUID()) || player.hasPermissions(2);
    }

    public static boolean enter(ServerPlayer player, PocketDimension dimension) {
        MinecraftServer server = player.server;
        ServerLevel pocket = PocketDimensions.getLevel(server);

        if (pocket == null) {
            player.displayClientMessage(
                    Component.translatable("message.pocketworld.dimension_missing"), true);
            return false;
        }

        PocketDimensionData data = PocketDimensionData.get(server);

        if (!dimension.isGenerated()) {
            PocketPlot.generate(pocket, dimension);
            data.setGenerated(dimension.getId(), true);
        }

        if (!PocketDimensions.isPocket(player.level())) {
            data.setReturnPoint(player.getUUID(), new ReturnPoint(
                    player.level().dimension().location().toString(),
                    player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot()
            ));
        }

        teleportToSpawn(player, pocket, dimension);
        player.displayClientMessage(Component.translatable("message.pocketworld.entered"), true);
        return true;
    }

    public static void teleportToSpawn(ServerPlayer player, ServerLevel pocket, PocketDimension dimension) {
        Vec3 spawn = PocketPlot.spawn(dimension);

        player.teleportTo(pocket, spawn.x, spawn.y, spawn.z, 90.0F, 0.0F);
        player.fallDistance = 0.0F;
    }

    public static boolean leave(ServerPlayer player) {
        if (!PocketDimensions.isPocket(player.level())) {
            return false;
        }

        MinecraftServer server = player.server;
        ReturnPoint point = PocketDimensionData.get(server).takeReturnPoint(player.getUUID());
        ServerLevel target = null;

        if (point != null) {
            ResourceLocation id = ResourceLocation.tryParse(point.dimension());

            if (id != null) {
                target = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            }
        }

        if (target == null || PocketDimensions.isPocket(target)) {
            ServerLevel overworld = server.overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();

            player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0.0F, 0.0F);
        } else {
            player.teleportTo(target, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
        }

        player.fallDistance = 0.0F;
        player.displayClientMessage(Component.translatable("message.pocketworld.left"), true);
        return true;
    }

    /** Убирает гостя из списка и выпроваживает, если он сейчас на острове. */
    public static boolean uninvite(MinecraftServer server, PocketDimension dimension, String name) {
        UUID targetId = null;

        for (Map.Entry<UUID, String> entry : dimension.getInvited().entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                targetId = entry.getKey();
            }
        }

        if (targetId == null || !PocketDimensionData.get(server).uninvite(dimension.getId(), targetId)) {
            return false;
        }

        ServerPlayer guest = server.getPlayerList().getPlayer(targetId);

        if (guest != null
                && PocketDimensions.isPocket(guest.level())
                && PocketPlot.indexAt(guest.getX(), guest.getZ()) == dimension.getIndex()) {
            leave(guest);
            guest.sendSystemMessage(Component.translatable("message.pocketworld.kicked")
                    .withStyle(ChatFormatting.RED));
        }

        return true;
    }

    /** Приглашает игрока и отправляет ему кликабельное сообщение. */
    public static boolean invite(ServerPlayer owner, ServerPlayer target, PocketDimension dimension) {
        if (!canManage(dimension, owner)) {
            owner.sendSystemMessage(Component.translatable("message.pocketworld.not_owner")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        if (target == owner || dimension.isOwner(target.getUUID())) {
            owner.sendSystemMessage(Component.translatable("message.pocketworld.invite_self")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        String targetName = target.getGameProfile().getName();

        if (!PocketDimensionData.get(owner.server).invite(dimension.getId(), target.getUUID(), targetName)) {
            owner.sendSystemMessage(Component.translatable("message.pocketworld.already_invited", targetName)
                    .withStyle(ChatFormatting.YELLOW));
            return false;
        }

        owner.sendSystemMessage(Component.translatable("message.pocketworld.invited", targetName)
                .withStyle(ChatFormatting.GREEN));

        Component button = Component.translatable("message.pocketworld.join_button")
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/pocket visit " + dimension.getId()))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("message.pocketworld.join_hover"))));

        target.sendSystemMessage(Component.translatable(
                "message.pocketworld.invite_received",
                owner.getGameProfile().getName(),
                button
        ).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }
}

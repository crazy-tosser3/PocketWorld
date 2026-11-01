package dev.crzto3.pocketworld.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.crzto3.pocketworld.dimension.PocketActions;
import dev.crzto3.pocketworld.dimension.PocketDimensionManager;
import dev.crzto3.pocketworld.dimension.PocketDimensions;
import dev.crzto3.pocketworld.dimension.PocketPlot;
import dev.crzto3.pocketworld.dimension.PocketTravel;
import dev.crzto3.pocketworld.item.PocketBottleItem;
import dev.crzto3.pocketworld.storage.PocketDimensionData;
import dev.crzto3.pocketworld.storage.PocketDimensionData.PocketDimension;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * /pocket invite|uninvite|visit|leave|list|terrain|biome|regenerate
 */
public final class PocketCommands {

    private PocketCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pocket")
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> invite(context))))
                .then(Commands.literal("uninvite")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    PocketDimension dimension = current(context.getSource().getPlayer());

                                    if (dimension != null) {
                                        SharedSuggestionProvider.suggest(
                                                dimension.getInvited().values(), builder);
                                    }

                                    return builder.buildFuture();
                                })
                                .executes(context -> uninvite(context))))
                .then(Commands.literal("visit")
                        .then(Commands.argument("pocket", UuidArgument.uuid())
                                .executes(context -> visit(context))))
                .then(Commands.literal("leave")
                        .executes(context -> leave(context)))
                .then(Commands.literal("list")
                        .executes(context -> list(context)))
                .then(Commands.literal("terrain")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(
                                                PocketDimensionManager.AVAILABLE_TERRAINS, builder))
                                .executes(context -> terrain(context))))
                .then(Commands.literal("biome")
                        .then(Commands.argument("biome", ResourceLocationArgument.id())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggestResource(
                                                context.getSource().registryAccess()
                                                        .registryOrThrow(Registries.BIOME).keySet(),
                                                builder))
                                .executes(context -> biome(context))))
                .then(Commands.literal("sky")
                        .then(Commands.argument("theme", StringArgumentType.word())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(
                                                PocketDimensionManager.AVAILABLE_SKIES, builder))
                                .executes(context -> sky(context))))
                .then(Commands.literal("weather")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(
                                                PocketDimensionManager.AVAILABLE_WEATHER, builder))
                                .executes(context -> weather(context))))
                .then(Commands.literal("size")
                        .then(Commands.argument("size", StringArgumentType.word())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(
                                                PocketDimensionManager.AVAILABLE_SIZES, builder))
                                .executes(context -> size(context))))
                .then(Commands.literal("regenerate")
                        .executes(context -> regenerate(context, false))
                        .then(Commands.literal("confirm")
                                .executes(context -> regenerate(context, true)))));
    }

    /** Карман, в котором стоит игрок, либо карман из бутылки в руке. */
    private static PocketDimension current(ServerPlayer player) {
        if (player == null) {
            return null;
        }

        PocketDimensionData data = PocketDimensionData.get(player.server);

        if (PocketDimensions.isPocket(player.level())) {
            int index = PocketPlot.indexAt(player.getX(), player.getZ());
            return index < 0 ? null : data.getByIndex(index);
        }

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);

            if (stack.getItem() instanceof PocketBottleItem) {
                UUID id = PocketBottleItem.getPocketId(stack);

                if (id != null) {
                    return data.get(id);
                }
            }
        }

        return null;
    }

    private static PocketDimension requireManaged(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PocketDimension dimension = current(player);

        if (dimension == null) {
            fail(context, "message.pocketworld.no_pocket");
            return null;
        }

        if (!PocketTravel.canManage(dimension, player)) {
            fail(context, "message.pocketworld.not_owner");
            return null;
        }

        return dimension;
    }

    /** Настройки острова меняются только снаружи: с бутылкой в руке или через конфигуратор. */
    private static PocketDimension requireConfigurable(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        if (PocketDimensions.isPocket(context.getSource().getPlayerOrException().level())) {
            fail(context, "message.pocketworld.no_config_inside");
            return null;
        }

        return requireManaged(context);
    }

    private static void fail(CommandContext<CommandSourceStack> context, String key) {
        context.getSource().sendFailure(Component.translatable(key));
    }

    private static int invite(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer owner = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        PocketDimension dimension = requireManaged(context);

        return dimension != null && PocketTravel.invite(owner, target, dimension) ? 1 : 0;
    }

    private static int uninvite(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PocketDimension dimension = requireManaged(context);

        if (dimension == null) {
            return 0;
        }

        String name = StringArgumentType.getString(context, "player");

        if (!PocketTravel.uninvite(context.getSource().getServer(), dimension, name)) {
            context.getSource().sendFailure(Component.translatable("message.pocketworld.not_invited", name));
            return 0;
        }

        context.getSource().sendSuccess(
                () -> Component.translatable("message.pocketworld.uninvited", name), false);
        return 1;
    }

    private static int visit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PocketDimension dimension = PocketDimensionData.get(player.server)
                .get(UuidArgument.getUuid(context, "pocket"));

        if (dimension == null || !PocketTravel.canVisit(dimension, player)) {
            fail(context, "message.pocketworld.no_access");
            return 0;
        }

        return PocketTravel.enter(player, dimension) ? 1 : 0;
    }

    private static int leave(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!PocketTravel.leave(context.getSource().getPlayerOrException())) {
            fail(context, "message.pocketworld.not_in_pocket");
            return 0;
        }

        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int count = 0;

        for (PocketDimension dimension : PocketDimensionManager.getAll(player.server)) {
            boolean own = dimension.isOwner(player.getUUID());

            if (!own && !dimension.isInvited(player.getUUID())) {
                continue;
            }

            if (count++ == 0) {
                player.sendSystemMessage(Component.translatable("message.pocketworld.list_header")
                        .withStyle(ChatFormatting.GOLD));
            }

            String shortId = dimension.getId().toString().substring(0, 8);
            Component label = own
                    ? Component.translatable("message.pocketworld.list_own", shortId)
                    : Component.translatable("message.pocketworld.list_guest", shortId, dimension.getOwnerName());

            player.sendSystemMessage(label.copy().withStyle(style -> style
                    .withColor(ChatFormatting.AQUA)
                    .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND, "/pocket visit " + dimension.getId()))));
        }

        if (count == 0) {
            fail(context, "message.pocketworld.list_empty");
        }

        return count;
    }

    private static int terrain(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PocketDimension dimension = requireConfigurable(context);

        if (dimension == null) {
            return 0;
        }

        String terrain = StringArgumentType.getString(context, "type");

        if (!PocketDimensionManager.setTerrain(context.getSource().getServer(), dimension.getId(), terrain)) {
            fail(context, "message.pocketworld.invalid_terrain");
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable(
                dimension.isGenerated()
                        ? "message.pocketworld.terrain_set_regen"
                        : "message.pocketworld.terrain_set",
                terrain), false);
        return 1;
    }

    private static int biome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PocketDimension dimension = requireConfigurable(context);

        if (dimension == null) {
            return 0;
        }

        ResourceLocation biome = ResourceLocationArgument.getId(context, "biome");

        if (!PocketDimensionManager.setBiome(
                context.getSource().getServer(), dimension.getId(), biome.toString())) {
            fail(context, "message.pocketworld.invalid_biome");
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable(
                dimension.isGenerated()
                        ? "message.pocketworld.biome_set_regen"
                        : "message.pocketworld.biome_set",
                biome.toString()), false);
        return 1;
    }

    private static int sky(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PocketDimension dimension = requireConfigurable(context);

        if (dimension == null) {
            return 0;
        }

        String theme = StringArgumentType.getString(context, "theme");

        if (!PocketDimensionManager.setSky(context.getSource().getServer(), dimension.getId(), theme)) {
            fail(context, "message.pocketworld.invalid_sky");
            return 0;
        }

        context.getSource().sendSuccess(
                () -> Component.translatable("message.pocketworld.sky_set", theme), false);
        return 1;
    }

    private static int weather(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PocketDimension dimension = requireConfigurable(context);

        if (dimension == null) {
            return 0;
        }

        String weather = StringArgumentType.getString(context, "type");

        if (!PocketDimensionManager.setWeather(context.getSource().getServer(), dimension.getId(), weather)) {
            fail(context, "message.pocketworld.invalid_weather");
            return 0;
        }

        context.getSource().sendSuccess(
                () -> Component.translatable("message.pocketworld.weather_set", weather), false);
        return 1;
    }

    private static int size(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PocketDimension dimension = requireConfigurable(context);

        if (dimension == null) {
            return 0;
        }

        String size = StringArgumentType.getString(context, "size");

        if (!PocketDimensionManager.setSize(context.getSource().getServer(), dimension.getId(), size)) {
            fail(context, "message.pocketworld.invalid_size");
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable(
                dimension.isGenerated()
                        ? "message.pocketworld.size_set_regen"
                        : "message.pocketworld.size_set",
                size), false);
        return 1;
    }

    private static int regenerate(CommandContext<CommandSourceStack> context, boolean confirmed)
            throws CommandSyntaxException {
        PocketDimension dimension = requireConfigurable(context);

        if (dimension == null) {
            return 0;
        }

        if (!PocketActions.regenerate(context.getSource().getServer(), dimension)) {
            fail(context, "message.pocketworld.dimension_missing");
            return 0;
        }

        context.getSource().sendSuccess(
                () -> Component.translatable("message.pocketworld.regenerated"), false);
        return 1;
    }
}

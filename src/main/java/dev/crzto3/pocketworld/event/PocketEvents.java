package dev.crzto3.pocketworld.event;

import dev.crzto3.pocketworld.PocketWorld;
import dev.crzto3.pocketworld.command.PocketCommands;
import dev.crzto3.pocketworld.dimension.PocketDimensions;
import dev.crzto3.pocketworld.dimension.PocketPlot;
import dev.crzto3.pocketworld.dimension.PocketTravel;
import dev.crzto3.pocketworld.storage.PocketDimensionData;
import dev.crzto3.pocketworld.storage.PocketDimensionData.PocketDimension;
import dev.crzto3.pocketworld.network.PocketNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Random;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = PocketWorld.MOD_ID)
public class PocketEvents {

    private static final Map<UUID, String> SENT_SKY = new HashMap<>();

    /** Общая погода измерения всегда ясная: дождь и грозу показывает клиент для каждого острова отдельно. */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && !event.level.isClientSide
                && PocketDimensions.isPocket(event.level)) {
            event.level.setRainLevel(0.0F);
            event.level.setThunderLevel(0.0F);
        }
    }

    /** В карманном измерении не появляются враждебные существа. */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide
                && event.getEntity() instanceof Enemy
                && PocketDimensions.isPocket(event.getLevel())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT_SKY.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        PocketCommands.register(event.getDispatcher());
    }

    /** Не даёт игроку упасть в пустоту или уйти за пределы своего острова. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 10 != 0) {
            return;
        }

        if (!PocketDimensions.isPocket(player.level())) {
            SENT_SKY.remove(player.getUUID());
            return;
        }

        int index = PocketPlot.indexAt(player.getX(), player.getZ());
        PocketDimension dimension = index < 0
                ? null
                : PocketDimensionData.get(player.server).getByIndex(index);

        if (dimension == null) {
            PocketTravel.leave(player);
            return;
        }

        // Клиенту нужно знать тему неба текущего кармана.
        String state = dimension.getSky() + "|" + dimension.getWeather();

        if (!state.equals(SENT_SKY.get(player.getUUID()))) {
            SENT_SKY.put(player.getUUID(), state);
            PocketNetwork.sendState(player, dimension);
        }

        // Грозу сопровождают безвредные молнии.
        if ("thunder".equals(dimension.getWeather()) && player.getRandom().nextInt(60) == 0) {
            Vec3 at = PocketPlot.randomSurface(dimension, new Random());
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(player.level());

            if (bolt != null) {
                bolt.moveTo(at.x, at.y, at.z);
                bolt.setVisualOnly(true);
                player.level().addFreshEntity(bolt);
            }
        }

        if (!PocketPlot.isInside(dimension, player.getX(), player.getY(), player.getZ())) {
            PocketTravel.teleportToSpawn(player, (ServerLevel) player.level(), dimension);
        }
    }
}

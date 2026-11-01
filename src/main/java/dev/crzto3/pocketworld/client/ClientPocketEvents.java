package dev.crzto3.pocketworld.client;

import dev.crzto3.pocketworld.PocketWorld;
import dev.crzto3.pocketworld.dimension.PocketDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Погода кармана рисуется только на клиенте: сервер держит общую погоду мира,
 * поэтому здесь она плавно подменяется значением из настроек острова.
 */
@Mod.EventBusSubscriber(modid = PocketWorld.MOD_ID, value = Dist.CLIENT)
public class ClientPocketEvents {

    private static final float STEP = 0.02F;

    private static float rain;
    private static float thunder;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;

        if (level == null || level.dimension() != PocketDimensions.POCKET_LEVEL) {
            rain = 0.0F;
            thunder = 0.0F;
            return;
        }

        String weather = ClientPocketState.getWeather();
        float rainTarget = "clear".equals(weather) ? 0.0F : 1.0F;
        float thunderTarget = "thunder".equals(weather) ? 1.0F : 0.0F;

        rain = Mth.clamp(rain + Math.signum(rainTarget - rain) * STEP, 0.0F, 1.0F);
        thunder = Mth.clamp(thunder + Math.signum(thunderTarget - thunder) * STEP, 0.0F, 1.0F);

        level.setRainLevel(rain);
        level.setThunderLevel(thunder);
    }
}


package dev.crzto3.pocketworld.client;

import dev.crzto3.pocketworld.PocketWorld;
import dev.crzto3.pocketworld.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
        modid = PocketWorld.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ModClientEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(
                        ModMenuTypes.DIMENSION_CONFIGURATOR.get(),
                        DimensionConfiguratorScreen::new
                )
        );
    }

    @SubscribeEvent
    public static void onRegisterSkyEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(
                new ResourceLocation(PocketWorld.MOD_ID, "pocket"),
                new PocketSkyEffects()
        );
    }
}

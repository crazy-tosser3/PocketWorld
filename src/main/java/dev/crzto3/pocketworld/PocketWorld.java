package dev.crzto3.pocketworld;

import dev.crzto3.pocketworld.network.PocketNetwork;
import dev.crzto3.pocketworld.registry.ModBlockEntities;
import dev.crzto3.pocketworld.registry.ModBlocks;
import dev.crzto3.pocketworld.registry.ModCreativeTabs;
import dev.crzto3.pocketworld.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import dev.crzto3.pocketworld.registry.ModMenuTypes;

@Mod(PocketWorld.MOD_ID)
public class PocketWorld {
    public static final String MOD_ID = "pocketworld";

    public PocketWorld() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        PocketNetwork.register();
    }
}
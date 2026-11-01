package dev.crzto3.pocketworld.registry;

import dev.crzto3.pocketworld.PocketWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PocketWorld.MOD_ID);

    public static final RegistryObject<CreativeModeTab> POCKET_WORLD_TAB =
            CREATIVE_TABS.register("pocket_world_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("creativetab.pocketworld.pocket_world_tab"))
                            .icon(() -> ModItems.POCKET_BOTTLE.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.EMPTY_BOTTLE.get());
                                output.accept(ModItems.POCKET_BOTTLE.get());
                                output.accept(ModItems.RIFT_SHARD.get());

                                output.accept(ModBlocks.DIMENSION_CONFIGURATOR.get());
                                output.accept(ModBlocks.DIMENSION_EXIT.get());
                                output.accept(ModBlocks.POCKET_WALL.get());
                            })
                            .build()
            );

    public static void register(net.minecraftforge.eventbus.api.IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
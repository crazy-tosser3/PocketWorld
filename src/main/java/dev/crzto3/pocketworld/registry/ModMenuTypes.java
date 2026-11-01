
package dev.crzto3.pocketworld.registry;

import dev.crzto3.pocketworld.PocketWorld;
import dev.crzto3.pocketworld.menu.DimensionConfiguratorMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(
                    ForgeRegistries.MENU_TYPES,
                    PocketWorld.MOD_ID
            );

    public static final RegistryObject<MenuType<DimensionConfiguratorMenu>>
            DIMENSION_CONFIGURATOR = MENU_TYPES.register(
            "dimension_configurator",
            () -> IForgeMenuType.create(DimensionConfiguratorMenu::new)
    );

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}

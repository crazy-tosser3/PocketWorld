
package dev.crzto3.pocketworld.registry;

import dev.crzto3.pocketworld.PocketWorld;
import dev.crzto3.pocketworld.block.DimensionConfiguratorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(
                    ForgeRegistries.BLOCK_ENTITY_TYPES,
                    PocketWorld.MOD_ID
            );

    public static final RegistryObject<BlockEntityType<DimensionConfiguratorBlockEntity>>
            DIMENSION_CONFIGURATOR = BLOCK_ENTITIES.register(
            "dimension_configurator",
            () -> BlockEntityType.Builder.of(
                    DimensionConfiguratorBlockEntity::new,
                    ModBlocks.DIMENSION_CONFIGURATOR.get()
            ).build(null)
    );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

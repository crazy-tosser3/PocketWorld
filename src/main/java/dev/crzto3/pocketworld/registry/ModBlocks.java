
package dev.crzto3.pocketworld.registry;

import dev.crzto3.pocketworld.PocketWorld;
import dev.crzto3.pocketworld.block.DimensionConfiguratorBlock;
import dev.crzto3.pocketworld.block.DimensionExitBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    // Реестр блоков
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PocketWorld.MOD_ID);

    // Блок-конфигуратор измерений
    public static final RegistryObject<Block> DIMENSION_CONFIGURATOR =
            BLOCKS.register("dimension_configurator",
                    () -> new DimensionConfiguratorBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(3.5F)
                                    .requiresCorrectToolForDrops()
                                    .sound(SoundType.METAL)
                    ));

    // Блок выхода
    public static final RegistryObject<Block> DIMENSION_EXIT =
            BLOCKS.register("dimension_exit", () ->
                    new DimensionExitBlock(BlockBehaviour.Properties.of()
                            .strength(-1.0F, 3600000.0F)
                            .lightLevel(state -> 12)
                            .sound(SoundType.STONE)
                    )
            );

    // Стена карманного измерения
    public static final RegistryObject<Block> POCKET_WALL =
            BLOCKS.register("pocket_wall", () ->
                    new Block(BlockBehaviour.Properties.of()
                            .strength(-1.0F)
                            .sound(SoundType.STONE)
                    )
            );

    // Подключение регистрации к Forge
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}

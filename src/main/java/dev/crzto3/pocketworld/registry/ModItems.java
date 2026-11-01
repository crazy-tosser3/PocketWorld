
package dev.crzto3.pocketworld.registry;

import dev.crzto3.pocketworld.PocketWorld;
import dev.crzto3.pocketworld.item.PocketBottleItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    // Реестр предметов мода
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, PocketWorld.MOD_ID);

    // Пустая бутылка
    public static final RegistryObject<Item> EMPTY_BOTTLE =
            ITEMS.register("empty_bottle", () ->
                    new Item(new Item.Properties())
            );

    // Бутылка с карманным измерением
    public static final RegistryObject<Item> POCKET_BOTTLE =
            ITEMS.register("pocket_bottle",
                    () -> new PocketBottleItem(
                            new Item.Properties().stacksTo(1)
                    ));

    // Осколок разлома
    public static final RegistryObject<Item> RIFT_SHARD =
            ITEMS.register("rift_shard", () ->
                    new Item(new Item.Properties())
            );

    // Предметная форма блока конфигуратора
    public static final RegistryObject<Item> DIMENSION_CONFIGURATOR =
            ITEMS.register("dimension_configurator", () ->
                    new BlockItem(
                            ModBlocks.DIMENSION_CONFIGURATOR.get(),
                            new Item.Properties()
                    )
            );

    // Предметная форма блока выхода
    public static final RegistryObject<Item> DIMENSION_EXIT =
            ITEMS.register("dimension_exit", () ->
                    new BlockItem(
                            ModBlocks.DIMENSION_EXIT.get(),
                            new Item.Properties()
                    )
            );

    // Предметная форма стены карманного измерения
    public static final RegistryObject<Item> POCKET_WALL =
            ITEMS.register("pocket_wall", () ->
                    new BlockItem(
                            ModBlocks.POCKET_WALL.get(),
                            new Item.Properties()
                    )
            );

    // Подключение регистрации к Forge
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}

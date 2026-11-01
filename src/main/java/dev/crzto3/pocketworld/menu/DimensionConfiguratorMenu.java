
package dev.crzto3.pocketworld.menu;

import dev.crzto3.pocketworld.block.DimensionConfiguratorBlockEntity;
import dev.crzto3.pocketworld.registry.ModBlocks;
import dev.crzto3.pocketworld.registry.ModMenuTypes;

import dev.crzto3.pocketworld.dimension.PocketDimensionManager;
import dev.crzto3.pocketworld.dimension.PocketTravel;
import dev.crzto3.pocketworld.storage.PocketDimensionData;
import dev.crzto3.pocketworld.storage.PocketDimensionData.PocketDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DimensionConfiguratorMenu extends AbstractContainerMenu {

    private final DimensionConfiguratorBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public static final int DATA_COUNT = 6;
    /** Значения в data: 0 местность, 1 биом (-1 свой), 2 небо, 3 размер, 4 статус, 5 погода. */
    public static final int STATUS_NONE = 0;
    public static final int STATUS_OK = 1;
    public static final int STATUS_FOREIGN = 2;

    private final ContainerData data;

    // Конструктор для серверной стороны
    public DimensionConfiguratorMenu(
            int containerId,
            Inventory playerInventory,
            DimensionConfiguratorBlockEntity blockEntity,
            ContainerData data
    ) {
        super(ModMenuTypes.DIMENSION_CONFIGURATOR.get(), containerId);

        this.blockEntity = blockEntity;
        this.data = data;
        this.addDataSlots(data);

        this.access = ContainerLevelAccess.create(
                blockEntity.getLevel(),
                blockEntity.getBlockPos()
        );

        // Слот бутылки
        this.addSlot(new Slot(blockEntity, 0, 8, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.canPlaceItem(0, stack);
            }
        });

        // Инвентарь игрока
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        34 + column * 18,
                        158 + row * 18
                ));
            }
        }

        // Панель быстрого доступа
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(
                    playerInventory,
                    column,
                    34 + column * 18,
                    216
            ));
        }
    }

    // Конструктор для клиентской стороны
    public DimensionConfiguratorMenu(
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf data
    ) {
        this(
                containerId,
                playerInventory,
                getBlockEntity(playerInventory, data.readBlockPos()),
                new SimpleContainerData(DATA_COUNT)
        );
    }

    public int getData(int index) {
        return data.get(index);
    }

    /** Данные для сервера: текущие настройки карманного измерения из бутылки. */
    public static ContainerData serverData(DimensionConfiguratorBlockEntity blockEntity, Player player) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                UUID id = blockEntity.getSelectedPocketId();
                MinecraftServer server = player.getServer();
                PocketDimension dimension = id == null || server == null
                        ? null
                        : PocketDimensionData.get(server).get(id);

                if (dimension == null) {
                    return index == 4 ? STATUS_NONE : 0;
                }

                return switch (index) {
                    case 0 -> PocketDimensionManager.AVAILABLE_TERRAINS.indexOf(dimension.getTerrain());
                    case 1 -> PocketDimensionManager.PRESET_BIOMES.indexOf(dimension.getBiome());
                    case 2 -> PocketDimensionManager.AVAILABLE_SKIES.indexOf(dimension.getSky());
                    case 3 -> PocketDimensionManager.AVAILABLE_SIZES.indexOf(dimension.getSize());
                    case 5 -> PocketDimensionManager.AVAILABLE_WEATHER.indexOf(dimension.getWeather());
                    default -> player instanceof ServerPlayer serverPlayer
                            && PocketTravel.canManage(dimension, serverPlayer)
                            ? STATUS_OK : STATUS_FOREIGN;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    // Получаем Block Entity по координатам из сетевого пакета
    private static DimensionConfiguratorBlockEntity getBlockEntity(
            Inventory playerInventory,
            BlockPos position
    ) {
        BlockEntity blockEntity =
                playerInventory.player.level().getBlockEntity(position);

        if (blockEntity instanceof DimensionConfiguratorBlockEntity configurator) {
            return configurator;
        }

        throw new IllegalStateException(
                "Dimension configurator not found at " + position
        );
    }

    public DimensionConfiguratorBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                access,
                player,
                ModBlocks.DIMENSION_CONFIGURATOR.get()
        ) && blockEntity.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;

        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack source = slot.getItem();
            result = source.copy();

            // Перемещение из конфигуратора в инвентарь игрока
            if (index == 0) {
                if (!this.moveItemStackTo(source, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // В конфигуратор разрешено перемещать только бутылки
                if (!blockEntity.canPlaceItem(0, source)) {
                    return ItemStack.EMPTY;
                }

                if (!this.moveItemStackTo(source, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (source.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }
}

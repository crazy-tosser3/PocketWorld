
package dev.crzto3.pocketworld.block;

import dev.crzto3.pocketworld.item.PocketBottleItem;
import dev.crzto3.pocketworld.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.crzto3.pocketworld.menu.DimensionConfiguratorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.UUID;

public class DimensionConfiguratorBlockEntity
        extends BlockEntity implements Container, MenuProvider {
    private static final int SLOT_COUNT = 1;

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    private UUID selectedPocketId;

    public DimensionConfiguratorBlockEntity(
            BlockPos position,
            BlockState state
    ) {
        super(ModBlockEntities.DIMENSION_CONFIGURATOR.get(), position, state);
    }

    public UUID getSelectedPocketId() {
        return selectedPocketId;
    }

    public void setSelectedPocketId(UUID pocketId) {
        this.selectedPocketId = pocketId;
        setChanged();
    }

    public void clearSelectedPocket() {
        this.selectedPocketId = null;
        setChanged();
    }

    public ItemStack getBottle() {
        return items.get(0);
    }

    /**
     * Синхронизирует UUID выбранного измерения с бутылкой в слоте.
     */
    public void updateSelectedPocket() {
        ItemStack stack = getBottle();

        if (stack.isEmpty() || !(stack.getItem() instanceof PocketBottleItem)) {
            selectedPocketId = null;
            setChanged();
            return;
        }

        CompoundTag tag = stack.getTag();

        if (tag == null || !tag.hasUUID("PocketWorldId")) {
            selectedPocketId = null;
        } else {
            selectedPocketId = tag.getUUID("PocketWorldId");
        }

        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "block.pocketworld.dimension_configurator"
        );
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory inventory,
            Player player
    ) {
        return new DimensionConfiguratorMenu(
                containerId,
                inventory,
                this,
                DimensionConfiguratorMenu.serverData(this, player)
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        ContainerHelper.saveAllItems(tag, items);

        if (selectedPocketId != null) {
            tag.putUUID("SelectedPocketId", selectedPocketId);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        ContainerHelper.loadAllItems(tag, items);

        selectedPocketId = tag.hasUUID("SelectedPocketId")
                ? tag.getUUID("SelectedPocketId")
                : null;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return items.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }

        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }

        ItemStack result = ContainerHelper.removeItem(items, slot, amount);

        if (!result.isEmpty()) {
            updateSelectedPocket();
        }

        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }

        ItemStack result = ContainerHelper.takeItem(items, slot);

        if (!result.isEmpty()) {
            updateSelectedPocket();
        }

        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return;
        }

        if (!stack.isEmpty() && !(stack.getItem() instanceof PocketBottleItem)) {
            return;
        }

        items.set(slot, stack);

        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }

        updateSelectedPocket();
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5
        ) <= 64.0;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0
                && !stack.isEmpty()
                && stack.getItem() instanceof PocketBottleItem;
    }

    @Override
    public void clearContent() {
        items.set(0, ItemStack.EMPTY);
        selectedPocketId = null;
        setChanged();
    }
}

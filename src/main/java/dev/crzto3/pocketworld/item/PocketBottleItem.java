package dev.crzto3.pocketworld.item;

import dev.crzto3.pocketworld.dimension.PocketDimensionManager;
import dev.crzto3.pocketworld.dimension.PocketDimensions;
import dev.crzto3.pocketworld.dimension.PocketTravel;
import dev.crzto3.pocketworld.storage.PocketDimensionData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class PocketBottleItem extends Item {

    public static final String POCKET_ID_TAG = "PocketWorldId";

    public PocketBottleItem(Properties properties) {
        super(properties);
    }

    /** UUID измерения в бутылке или null, если бутылка ещё не использовалась. */
    @Nullable
    public static UUID getPocketId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(POCKET_ID_TAG) ? tag.getUUID(POCKET_ID_TAG) : null;
    }

    private static PocketDimensionData.PocketDimension bind(ItemStack stack, ServerPlayer player) {
        UUID pocketId = getPocketId(stack);

        if (pocketId == null) {
            pocketId = UUID.randomUUID();
            stack.getOrCreateTag().putUUID(POCKET_ID_TAG, pocketId);
        }

        PocketDimensionData.PocketDimension dimension =
                PocketDimensionManager.createOrGet(player.server, pocketId);

        if (dimension.getOwnerId() == null) {
            PocketDimensionData.get(player.server).setOwner(
                    pocketId, player.getUUID(), player.getGameProfile().getName());
        }

        return dimension;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (PocketDimensions.isPocket(level)) {
                PocketTravel.leave(serverPlayer);
            } else {
                // Бутылка служит ключом: войти может любой, у кого она в руках.
                PocketTravel.enter(serverPlayer, bind(stack, serverPlayer));
            }

            player.getCooldowns().addCooldown(this, 40);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** ПКМ бутылкой по игроку приглашает его в измерение. */
    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (!(target instanceof Player)) {
            return InteractionResult.PASS;
        }

        if (!player.level().isClientSide
                && player instanceof ServerPlayer owner
                && target instanceof ServerPlayer guest) {
            PocketTravel.invite(owner, guest, bind(stack, owner));
        }

        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        UUID id = getPocketId(stack);

        if (id != null) {
            tooltip.add(Component.literal("#" + id.toString().substring(0, 8))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        tooltip.add(Component.translatable("item.pocketworld.pocket_bottle.hint")
                .withStyle(ChatFormatting.GRAY));
    }
}

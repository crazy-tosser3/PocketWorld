
package dev.crzto3.pocketworld.block;

import dev.crzto3.pocketworld.dimension.PocketDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class DimensionConfiguratorBlock extends BaseEntityBlock {

    public DimensionConfiguratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new DimensionConfiguratorBlockEntity(position, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos position,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!level.isClientSide && PocketDimensions.isPocket(level)) {
            player.displayClientMessage(
                    Component.translatable("message.pocketworld.no_config_inside"), true);
            return InteractionResult.CONSUME;
        }

        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(position);

            if (blockEntity instanceof DimensionConfiguratorBlockEntity configurator
                    && player instanceof ServerPlayer serverPlayer) {

                NetworkHooks.openScreen(
                        serverPlayer,
                        configurator,
                        position
                );
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

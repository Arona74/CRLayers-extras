package io.arona74.aronalayersextras.mixin;

import io.arona74.aronalayersextras.LayerFallHandler;
import io.arona74.aronalayersextras.ModConfig;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockEntity.class)
public class FallingBlockMixin {

    @Shadow private BlockState blockState;
    @Shadow public boolean dropItem;

    @Inject(method = "fall", at = @At("TAIL"))
    private static void onSpawnFromBlock(Level world, BlockPos pos, BlockState state, CallbackInfoReturnable<FallingBlockEntity> cir) {
        if (!ModConfig.getInstance().enableLayersFallWithSand) return;
        if (!LayerFallHandler.isSandOrGravel(state)) return;

        LayerFallHandler.dropStackAbove(world, pos);
    }

    /**
     * Lands our blocks on top of a partial-height block instead of breaking them.
     *
     * A falling block comes to rest wherever its collision box stops, and
     * blockPosition() floors that. Landing on a full block leaves it in the
     * empty space above, which is what vanilla expects. Landing on a layer,
     * slab or rock leaves it INSIDE that block's own position, so vanilla's
     * replaceability test fails and the entity is discarded and popped as an
     * item -- the same reason vanilla sand breaks when it lands on a slab.
     *
     * That is exactly what happens to Conquest's rocks: they land on the layer
     * blocks that fell with them, one tick after those layers have placed
     * themselves. Placing one block higher puts the rock on the layer's surface,
     * which is where it was to begin with and the arrangement CR's own
     * updateShape expects.
     *
     * Vanilla sand and gravel are deliberately left alone -- only blocks this
     * mod launched take the redirect.
     */
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;canBeReplaced(Lnet/minecraft/world/item/context/BlockPlaceContext;)Z"
            )
    )
    private boolean landOnPartialBlocks(BlockState landingState, BlockPlaceContext context) {
        if (landingState.canBeReplaced(context)) return true;
        if (!ModConfig.getInstance().enableLayersFallWithSand) return false;
        if (this.blockState == null || !LayerFallHandler.fallsWithSand(this.blockState)) return false;

        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        Level level = self.level();
        BlockPos above = self.blockPosition().above();
        if (!level.getBlockState(above).canBeReplaced()) return false;
        if (!this.blockState.canSurvive(level, above)) return false;

        // FallingBlockEntity.fall clears WATERLOGGED on the way down, and the
        // vanilla landing path puts it back when the block settles in water.
        // This path has to do the same, or a rock that lands in a river leaves
        // a dry pocket behind. FluidTags.WATER rather than Fluids.WATER: it
        // covers flowing water as well as source blocks.
        BlockState landed = this.blockState;
        if (landed.hasProperty(BlockStateProperties.WATERLOGGED)
                && !landed.getValue(BlockStateProperties.WATERLOGGED)
                && level.getFluidState(above).is(FluidTags.WATER)) {
            landed = landed.setValue(BlockStateProperties.WATERLOGGED, true);
        }

        if (level.setBlock(above, landed, Block.UPDATE_ALL)) {
            // The caller is about to discard the entity because we answered
            // false. The block is already placed, so it must not also drop.
            this.dropItem = false;
        }
        return false;
    }
}

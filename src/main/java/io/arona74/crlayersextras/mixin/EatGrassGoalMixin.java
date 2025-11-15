package io.arona74.crlayersextras.mixin;

import io.arona74.crlayersextras.SheepGrassEatingHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.EatGrassGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EatGrassGoal.class)
public class EatGrassGoalMixin {
    private static final Identifier GRASS_LAYER_ID = new Identifier("conquest", "grass_block_layer");
    private static final Identifier LOAMY_DIRT_SLAB_ID = new Identifier("conquest", "loamy_dirt_slab");

    @Shadow
    @Final
    private MobEntity mob;

    @Shadow
    private World world;

    @Shadow
    private int timer;

    /**
     * Inject into canStart() to also detect grass_block_layer
     * We handle the complete check (including random) only when grass_block_layer is present
     * This prevents vanilla from doing a second random check
     */
    @Inject(method = "canStart", at = @At("HEAD"), cancellable = true)
    private void canStartWithGrassLayer(CallbackInfoReturnable<Boolean> cir) {
        BlockPos pos = this.mob.getBlockPos();
        BlockState state = this.world.getBlockState(pos);

        // Only handle if grass_block_layer is present
        if (!Registries.BLOCK.getId(state.getBlock()).equals(GRASS_LAYER_ID)) {
            // Not grass_block_layer, let vanilla handle it
            return;
        }

        // We have grass_block_layer, do the random check ourselves (same as vanilla)
        if (this.mob.getRandom().nextInt(this.mob.isBaby() ? 50 : 1000) != 0) {
            cir.setReturnValue(false);
        } else {
            cir.setReturnValue(true);
        }
    }

    /**
     * Inject into tick() to handle grass_block_layer consumption
     * This is where the actual eating happens when timer reaches 4
     */
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I", shift = At.Shift.AFTER))
    private void tickWithGrassLayer(CallbackInfo ci) {
        // Inject AFTER timer decrement. At timer == 4, the eating happens
        if (this.timer == 4) {
            BlockPos pos = this.mob.getBlockPos();
            BlockState state = this.world.getBlockState(pos);

            // Check if standing on grass_block_layer
            if (Registries.BLOCK.getId(state.getBlock()).equals(GRASS_LAYER_ID)) {
                if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                    // Convert grass_block_layer to loamy_dirt_slab
                    BlockState dirtSlabState = Registries.BLOCK.get(LOAMY_DIRT_SLAB_ID).getDefaultState();

                    // Copy properties to maintain orientation
                    dirtSlabState = SheepGrassEatingHandler.copyPropertiesPublic(state, dirtSlabState);

                    this.world.setBlockState(pos, dirtSlabState, 2);
                }

                // Call the mob's onEatingGrass to regrow wool
                this.mob.onEatingGrass();
            }
        }
    }
}

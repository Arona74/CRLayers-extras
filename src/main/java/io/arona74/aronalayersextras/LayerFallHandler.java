package io.arona74.aronalayersextras;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class LayerFallHandler {

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register(LayerFallHandler::onBlockBroken);
        AronaLayersExtras.LOGGER.info("Registered layer fall handler");
    }

    private static void onBlockBroken(Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!ModConfig.getInstance().enableLayersFallWithSand) return;
        if (!(world instanceof ServerLevel)) return;
        if (!isSandOrGravel(state)) return;

        dropStackAbove(world, pos);
    }

    public static boolean isSandOrGravel(BlockState state) {
        return state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL);
    }

    /**
     * Drops the run of layer blocks sitting directly on top of {@code pos}.
     *
     * Called from both entry points -- the player-break event above and the
     * FallingBlockEntity.fall mixin, which covers sand collapsing on its own.
     */
    public static void dropStackAbove(Level world, BlockPos pos) {
        BlockPos checkPos = pos.above();
        for (int i = 0; i < 32; i++) {
            BlockState above = world.getBlockState(checkPos);
            if (!fallsWithSand(above)) break;
            world.setBlock(checkPos, above.getFluidState().createLegacyBlock(), Block.UPDATE_CLIENTS);
            FallingBlockEntity.fall(world, checkPos, above);
            checkPos = checkPos.above();
        }
    }

    public static boolean fallsWithSand(BlockState state) {
        String id = Compat.blockId(state.getBlock());
        if (ModConfig.getInstance().AdditionalFallingBlocks.contains(id)) return true;

        int colon = id.indexOf(':');
        String ns = colon < 0 ? "minecraft" : id.substring(0, colon);
        String path = colon < 0 ? id : id.substring(colon + 1);
        if ("conquest".equals(ns)) {
            // Exclude mushroom layer blocks: CR's MushroomVanilla has a buggy
            // getStateForNeighborUpdate that crashes when the block is removed.
            if (path.contains("mushroom")) return false;
            // CR's decorative ground rocks are consistently plural and suffixed
            // -- limestone_rocks, andesite_rocks, smooth_tuff_rocks. The full
            // building blocks they are cut from are singular and unsuffixed
            // (mudstone, dark_tuff, pale_limestone), so anchoring on the
            // "_rocks" suffix picks up every rock and no solid block, and keeps
            // working when CR adds a new stone type.
            return path.contains("layer") || path.contains("slab") || path.endsWith("_rocks");
        }
        if ("vanillalayerplus".equals(ns)) {
            return path.endsWith("_layer");
        }
        return false;
    }
}

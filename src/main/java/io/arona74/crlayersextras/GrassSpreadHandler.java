package io.arona74.crlayersextras;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Random;

public class GrassSpreadHandler {
    private static final Identifier GRASS_LAYER_ID = new Identifier("conquest", "grass_block_layer");
    private static final Identifier LOAMY_DIRT_SLAB_ID = new Identifier("conquest", "loamy_dirt_slab");

    private static final Random RANDOM = new Random();

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(GrassSpreadHandler::onWorldTick);
        CRLayersExtras.LOGGER.info("Registered grass spreading handler");
    }

    private static void onWorldTick(ServerWorld world) {
        if (!ModConfig.getInstance().enableGrassSpreading) return;

        // Get the randomTickSpeed value (default is 3)
        int randomTickSpeed = world.getGameRules().getInt(net.minecraft.world.GameRules.RANDOM_TICK_SPEED);

        if (randomTickSpeed <= 0) {
            return;
        }

        // Process chunks around players - much more aggressively than before
        world.getPlayers().forEach(player -> {
            BlockPos playerPos = player.getBlockPos();
            int chunkX = playerPos.getX() >> 4;
            int chunkZ = playerPos.getZ() >> 4;

            // Process chunks around players
            int chunkRadius = 1; // 3x3 chunk area (matches vanilla simulation distance)

            for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                    WorldChunk chunk = world.getChunk(chunkX + dx, chunkZ + dz);

                    // Reduce tick rate to match vanilla spreading speed
                    // We're more efficient due to smart Y scanning, so we need fewer ticks
                    int ticksPerChunk = Math.max(1, randomTickSpeed / 3);

                    for (int i = 0; i < ticksPerChunk; i++) {
                        // 80% probability to skip this tick (only execute 20% of the time)
                        if (RANDOM.nextInt(5) != 0) {
                            continue;
                        }

                        int x = chunk.getPos().getStartX() + RANDOM.nextInt(16);
                        int z = chunk.getPos().getStartZ() + RANDOM.nextInt(16);

                        // Smart Y selection: focus on surface blocks where grass is more likely
                        // Check from top down to find the highest solid block
                        int y = world.getTopY();
                        BlockPos.Mutable mutablePos = new BlockPos.Mutable(x, y, z);

                        // Scan down to find surface (where grass would be)
                        for (int checkY = world.getTopY() - 1; checkY > world.getBottomY(); checkY--) {
                            mutablePos.setY(checkY);
                            BlockState checkState = world.getBlockState(mutablePos);

                            if (Registries.BLOCK.getId(checkState.getBlock()).equals(GRASS_LAYER_ID)
                                    || checkState.isOf(Blocks.GRASS_BLOCK)) {
                                trySpreadGrass(world, mutablePos.toImmutable());
                                break; // Found grass, try to spread it
                            } else if (!checkState.isAir() && checkState.isOpaque()) {
                                // Hit a non-grass solid block, stop searching this column
                                break;
                            }
                        }
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState copyProperties(BlockState source, BlockState target) {
        try {
            for (var property : source.getProperties()) {
                if (target.contains(property)) {
                    target = target.with((net.minecraft.state.property.Property<T>) property,
                                        (T) source.get(property));
                }
            }
        } catch (Exception e) {
            // If property copying fails, just return the target state as-is
        }
        return target;
    }

    private static void trySpreadGrass(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        // Check if this is a grass layer block or vanilla grass block
        if (!Registries.BLOCK.getId(state.getBlock()).equals(GRASS_LAYER_ID)
                && !state.isOf(Blocks.GRASS_BLOCK)) {
            return;
        }

        // Check if there's enough light (same as vanilla grass)
        if (world.getLightLevel(pos.up()) < 9) {
            return;
        }

        // Try to spread to neighboring blocks
        for (int i = 0; i < 4; i++) {
            BlockPos targetPos = pos.add(
                RANDOM.nextInt(3) - 1,
                RANDOM.nextInt(5) - 3,
                RANDOM.nextInt(3) - 1
            );

            BlockState targetState = world.getBlockState(targetPos);

            // Check if target is loamy dirt slab
            if (Registries.BLOCK.getId(targetState.getBlock()).equals(LOAMY_DIRT_SLAB_ID)) {
                // Check light level above the dirt slab (same as vanilla grass spreading)
                if (world.getLightLevel(targetPos.up()) >= 9) {
                    // Get the grass layer block to spread
                    BlockState grassLayerState = Registries.BLOCK.get(GRASS_LAYER_ID).getDefaultState();

                    // Copy properties from the dirt slab to maintain rotation, waterlogging, etc.
                    // This ensures the grass layer inherits the same orientation
                    grassLayerState = copyProperties(targetState, grassLayerState);

                    world.setBlockState(targetPos, grassLayerState, 3);
                }
            }
            // Also check if target is vanilla dirt block
            else if (Registries.BLOCK.getId(targetState.getBlock()).toString().equals("minecraft:dirt")) {
                BlockState aboveState = world.getBlockState(targetPos.up());

                // Only spread to dirt that has air above it
                // This prevents fighting with vanilla decay mechanics (e.g., when a layer block is placed above grass)
                if (aboveState.isAir() && world.getLightLevel(targetPos.up()) >= 9) {
                    // Convert vanilla dirt to vanilla grass_block
                    world.setBlockState(targetPos, Blocks.GRASS_BLOCK.getDefaultState(), 3);
                }
            }
        }
    }
}

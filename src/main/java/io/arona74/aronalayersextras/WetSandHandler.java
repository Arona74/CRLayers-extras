package io.arona74.aronalayersextras;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Conquest Reforged wet sand.
 *
 * Sand that touches water turns into the wet variant Conquest ships for it,
 * and turns back once the water is gone. "Touches water" means water on any of
 * the six faces, a waterlogged neighbour, or the sand block being waterlogged
 * itself -- a waterlogged sand layer is wet by definition.
 *
 * Which blocks pair up is entirely config-driven (ModConfig.WetSandMappings),
 * because the wet variants are Conquest block ids that this mod cannot compile
 * against. A pair whose blocks are not both registered is dropped, so with no
 * Conquest Reforged installed the whole handler resolves to nothing and stops.
 */
public class WetSandHandler {
    /** 3x3 chunks around each player, matching the other spread handlers. */
    private static final int CHUNK_RADIUS = 1;
    /** Columns sampled per chunk per tick. Any given column comes up every ~6s on average. */
    private static final int COLUMNS_PER_CHUNK = 2;
    /**
     * How far below the surface a column is scanned.
     *
     * Sand near water sits at the surface or just under it -- beaches, river
     * beds, the shallow end of an ocean. Deep ocean floor and sand walled into
     * a cave beside a waterlogged block are out of reach by design: scanning
     * every column to bedrock would cost an order of magnitude more for blocks
     * nobody looks at.
     */
    private static final int SCAN_DEPTH = 32;

    private static final Random RANDOM = new Random();

    private static ModConfig resolvedFrom;
    private static Mappings resolved;

    public static void register() {
        Compat.onEndLevelTick(WetSandHandler::onWorldTick);
        AronaLayersExtras.LOGGER.info("Registered wet sand handler");
    }

    private static void onWorldTick(ServerLevel world) {
        if (!ModConfig.getInstance().enableWetSands) return;

        Mappings mappings = mappings();
        if (mappings.isEmpty()) return;

        world.players().forEach(player -> {
            BlockPos playerPos = player.blockPosition();
            int chunkX = playerPos.getX() >> 4;
            int chunkZ = playerPos.getZ() >> 4;

            for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                    LevelChunk chunk = world.getChunk(chunkX + dx, chunkZ + dz);

                    for (int i = 0; i < COLUMNS_PER_CHUNK; i++) {
                        int x = chunk.getPos().getMinBlockX() + RANDOM.nextInt(16);
                        int z = chunk.getPos().getMinBlockZ() + RANDOM.nextInt(16);
                        scanColumn(world, mappings, x, z);
                    }
                }
            }
        });
    }

    private static void scanColumn(ServerLevel world, Mappings mappings, int x, int z) {
        // WORLD_SURFACE answers one past the highest non-air block, and counts
        // water as surface -- so a submerged sea bed still starts its scan at
        // the water line rather than at build height.
        int top = Math.min(world.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), Compat.topYExclusive(world)) - 1;
        int bottom = Math.max(top - SCAN_DEPTH, Compat.bottomY(world));

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
        for (int y = top; y >= bottom; y--) {
            pos.setY(y);
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) continue;

            String id = Compat.blockId(state.getBlock());

            String wet = mappings.dryToWet.get(id);
            if (wet != null) {
                if (touchesWater(world, pos, state)) convert(world, pos, state, wet);
                continue;
            }

            String dry = mappings.wetToDry.get(id);
            if (dry != null && !touchesWater(world, pos, state)) convert(world, pos, state, dry);
        }
    }

    private static boolean touchesWater(Level world, BlockPos pos, BlockState state) {
        if (isWater(state.getFluidState())) return true;

        BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            neighbour.setWithOffset(pos, direction);
            if (isWater(world.getBlockState(neighbour).getFluidState())) return true;
        }
        return false;
    }

    /**
     * Both water fluids, source and flowing.
     *
     * Spelled as two identity comparisons rather than FluidState.is(Fluid):
     * that overload is gone on 26.1.2 and newer, while getType() is on every
     * supported version.
     */
    private static boolean isWater(FluidState fluid) {
        return fluid.getType() == Fluids.WATER || fluid.getType() == Fluids.FLOWING_WATER;
    }

    private static void convert(ServerLevel world, BlockPos pos, BlockState from, String toId) {
        Block to = Compat.blockFromId(toId);
        if (to == Blocks.AIR) return;
        world.setBlock(pos.immutable(), copyProperties(from, to.defaultBlockState()), 3);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState copyProperties(BlockState source, BlockState target) {
        try {
            for (var property : source.getProperties()) {
                if (target.hasProperty(property)) {
                    target = target.setValue((net.minecraft.world.level.block.state.properties.Property<T>) property,
                                        (T) source.getValue(property));
                }
            }
        } catch (Exception e) {
            // If property copying fails, just return the target state as-is
        }
        return target;
    }

    /** The configured pairs, both ways round, with unresolvable ones dropped. */
    private static final class Mappings {
        final Map<String, String> dryToWet = new HashMap<>();
        final Map<String, String> wetToDry = new HashMap<>();

        boolean isEmpty() {
            return dryToWet.isEmpty();
        }
    }

    /**
     * Resolve the configured pairs once, on the first tick that needs them.
     *
     * Not at registration time: block registries are still filling then, so
     * every Conquest id would look unregistered and the feature would silently
     * do nothing. Rebuilt if the config object is replaced by ModConfig.load().
     */
    private static Mappings mappings() {
        ModConfig config = ModConfig.getInstance();
        if (resolved != null && resolvedFrom == config) return resolved;

        Mappings mappings = new Mappings();
        Map<String, String> configured = config.WetSandMappings;
        if (configured != null) {
            for (Map.Entry<String, String> entry : configured.entrySet()) {
                String dry = entry.getKey();
                String wet = entry.getValue();
                if (dry == null || wet == null || dry.equals(wet)) continue;

                if (Compat.blockFromId(dry) == Blocks.AIR || Compat.blockFromId(wet) == Blocks.AIR) {
                    AronaLayersExtras.LOGGER.info(
                            "Ignoring wet sand mapping {} -> {}: one of those blocks is not registered", dry, wet);
                    continue;
                }

                mappings.dryToWet.put(dry, wet);
                String claimed = mappings.wetToDry.putIfAbsent(wet, dry);
                if (claimed != null) {
                    AronaLayersExtras.LOGGER.warn(
                            "Wet sand mapping {} -> {} cannot dry back: {} already dries to {}",
                            dry, wet, wet, claimed);
                }
            }
        }

        if (mappings.isEmpty()) {
            AronaLayersExtras.LOGGER.info("Wet sand is enabled but no mapping resolved; the feature is inactive");
        }
        resolved = mappings;
        resolvedFrom = config;
        return mappings;
    }
}

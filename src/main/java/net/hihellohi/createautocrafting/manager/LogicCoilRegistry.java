package net.hihellohi.createautocrafting.manager;

import net.hihellohi.createautocrafting.blocks.LogicCoilBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks placed Logic Coils per dimension (registered from their block-entity tick) and recognises
 * them as dynamic multiblocks — a solid prism with a footprint up to 3x3 and a height of 3..16
 * (smallest 1x1x3, largest 3x3x16), in the spirit of Create's fluid tanks. Each coil contributes
 * {@link #STEPS_PER_COIL} crafting steps. Coils only count for a network if their assembly sits
 * within {@link #RANGE} blocks of a Crafting Distributor on that network.
 */
public final class LogicCoilRegistry {
    public static final int STEPS_PER_COIL = 3;
    private static final int RANGE = 32;
    private static final double RANGE_SQR = RANGE * RANGE;
    private static final int MAX_FOOTPRINT = 3;
    private static final int MIN_HEIGHT = 3;
    private static final int MAX_HEIGHT = 16;
    private static final int MAX_PRISM = MAX_FOOTPRINT * MAX_FOOTPRINT * MAX_HEIGHT; // 144

    private static final Map<ResourceKey<Level>, Set<BlockPos>> BY_DIMENSION = new ConcurrentHashMap<>();

    private LogicCoilRegistry() {}

    public static void add(Level level, BlockPos pos) {
        BY_DIMENSION.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    public static void remove(Level level, BlockPos pos) {
        Set<BlockPos> set = BY_DIMENSION.get(level.dimension());
        if (set != null) {
            set.remove(pos.immutable());
        }
    }

    public static boolean isOnNetwork(ServerLevel level, UUID networkId) {
        return countCoilsOnNetwork(level, networkId) > 0;
    }

    /** Total crafting steps available from valid coil assemblies on the network. */
    public static int craftingStepsOnNetwork(ServerLevel level, UUID networkId) {
        return countCoilsOnNetwork(level, networkId) * STEPS_PER_COIL;
    }

    /** Counts coils that belong to a valid multiblock assembly near the network's providers. */
    public static int countCoilsOnNetwork(ServerLevel level, UUID networkId) {
        Set<BlockPos> coils = BY_DIMENSION.get(level.dimension());
        if (coils == null || coils.isEmpty()) {
            return 0;
        }
        List<BlockPos> providers = PatternRegistry.getProvidersForNetwork(level, networkId);
        if (providers.isEmpty()) {
            return 0;
        }

        Set<BlockPos> all = new HashSet<>(coils);
        Set<BlockPos> visited = new HashSet<>();
        int total = 0;

        for (BlockPos start : all) {
            if (visited.contains(start)) {
                continue;
            }
            List<BlockPos> group = floodFill(start, all, visited);
            if (isValidPrism(group) && nearAnyProvider(group, providers)) {
                total += group.size();
            }
        }
        return total;
    }

    /**
     * Prism check read straight from world blocks (independent of network proximity): is the coil at
     * {@code start} part of a fully-filled prism within the size limits? (Retained as a utility; the
     * coil's look is now driven by Create connected textures, not a blockstate.)
     */
    public static boolean isInValidPrism(BlockGetter level, BlockPos start) {
        return prismFor(level, start) != null;
    }

    /**
     * If {@code start} belongs to a valid prism, returns {@code {ox, oy, oz, dx, dy, dz}} — this
     * block's offset from the prism's min corner plus the prism dimensions (used by the connected-
     * texture model to slice the shared sprite). Returns {@code null} when not part of a valid prism.
     */
    public static int[] prismFor(BlockGetter level, BlockPos start) {
        if (!isCoil(level, start)) {
            return null;
        }
        Set<BlockPos> group = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos s = start.immutable();
        group.add(s);
        queue.add(s);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir).immutable();
                if (isCoil(level, next) && group.add(next)) {
                    if (group.size() > MAX_PRISM) {
                        return null; // too large to be a valid prism
                    }
                    queue.add(next);
                }
            }
        }
        List<BlockPos> list = new ArrayList<>(group);
        if (!isValidPrism(list)) {
            return null;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : list) {
            minX = Math.min(minX, pos.getX()); maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY()); maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ()); maxZ = Math.max(maxZ, pos.getZ());
        }
        return new int[]{
                start.getX() - minX, start.getY() - minY, start.getZ() - minZ,
                maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1
        };
    }

    private static boolean isCoil(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof LogicCoilBlock;
    }

    private static List<BlockPos> floodFill(BlockPos start, Set<BlockPos> all, Set<BlockPos> visited) {
        List<BlockPos> group = new ArrayList<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            group.add(pos);
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (all.contains(next) && visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return group;
    }

    /** A connected group is valid iff it is a fully-filled prism within the size limits. */
    private static boolean isValidPrism(List<BlockPos> group) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : group) {
            minX = Math.min(minX, pos.getX()); maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY()); maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ()); maxZ = Math.max(maxZ, pos.getZ());
        }
        int dx = maxX - minX + 1;
        int dy = maxY - minY + 1;
        int dz = maxZ - minZ + 1;
        if (dx > MAX_FOOTPRINT || dz > MAX_FOOTPRINT || dy < MIN_HEIGHT || dy > MAX_HEIGHT) {
            return false;
        }
        return group.size() == dx * dy * dz; // solid, no gaps
    }

    private static boolean nearAnyProvider(List<BlockPos> group, List<BlockPos> providers) {
        for (BlockPos coil : group) {
            for (BlockPos provider : providers) {
                if (coil.distSqr(provider) <= RANGE_SQR) {
                    return true;
                }
            }
        }
        return false;
    }
}

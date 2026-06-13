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
    private static final int MIN_HEIGHT = 2; // forms from a 1x1x2 column upward
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

    /**
     * Counts coils that belong to a valid sub-prism assembly near the network's providers. Loose
     * coils stuck to a finished prism don't disqualify the prism — only coils that are part of a
     * valid box ({@link #assemblyOf} != null) are counted, matching what the connected texture shows
     * as assembled.
     */
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
            if (!nearAnyProvider(group, providers)) {
                continue;
            }
            for (BlockPos coil : group) {
                if (assemblyOf(level, coil) != null) {
                    total++;
                }
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

    /**
     * The largest valid solid coil box (footprint &le; 3x3, height 2..16) that <em>contains</em>
     * {@code pos}, as {@code {minX, minY, minZ, dx, dy, dz}}, or {@code null} if pos isn't part of
     * such a box. Unlike {@link #prismFor}, loose coils stuck to a finished assembly don't invalidate
     * it — the search finds the biggest prism the coil belongs to and ignores the extras. Two coils
     * in the same prism resolve to the same box, so connections stay within the assembly.
     */
    public static int[] assemblyOf(BlockGetter level, BlockPos pos) {
        if (!isCoil(level, pos)) {
            return null;
        }
        int px = pos.getX();
        int py = pos.getY();
        int pz = pos.getZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int[] best = null;
        long bestVol = 1; // require >= 2 blocks to count as assembled

        for (int dx = 1; dx <= MAX_FOOTPRINT; dx++) {
            for (int ox = 0; ox < dx; ox++) {
                int minX = px - ox;
                for (int dz = 1; dz <= MAX_FOOTPRINT; dz++) {
                    for (int oz = 0; oz < dz; oz++) {
                        int minZ = pz - oz;
                        if (!layerSolid(level, cursor, minX, dx, minZ, dz, py)) {
                            continue;
                        }
                        int maxY = py;
                        int minY = py;
                        while (maxY - minY + 1 < MAX_HEIGHT && layerSolid(level, cursor, minX, dx, minZ, dz, maxY + 1)) {
                            maxY++;
                        }
                        while (maxY - minY + 1 < MAX_HEIGHT && layerSolid(level, cursor, minX, dx, minZ, dz, minY - 1)) {
                            minY--;
                        }
                        int dy = maxY - minY + 1;
                        if (dy < MIN_HEIGHT) {
                            continue;
                        }
                        long vol = (long) dx * dz * dy;
                        if (vol > bestVol || (vol == bestVol && best != null && lessCorner(minX, minY, minZ, best))) {
                            best = new int[]{minX, minY, minZ, dx, dy, dz};
                            bestVol = vol;
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Is {@code pos} inside the box {@code {minX,minY,minZ,dx,dy,dz}}? */
    public static boolean inAssembly(int[] box, BlockPos pos) {
        return box != null
                && pos.getX() >= box[0] && pos.getX() < box[0] + box[3]
                && pos.getY() >= box[1] && pos.getY() < box[1] + box[4]
                && pos.getZ() >= box[2] && pos.getZ() < box[2] + box[5];
    }

    private static boolean layerSolid(BlockGetter level, BlockPos.MutableBlockPos cursor,
                                      int minX, int dx, int minZ, int dz, int y) {
        for (int x = minX; x < minX + dx; x++) {
            for (int z = minZ; z < minZ + dz; z++) {
                if (!(level.getBlockState(cursor.set(x, y, z)).getBlock() instanceof LogicCoilBlock)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Deterministic tiebreak between equal-volume boxes: smaller (minX, minY, minZ) wins. */
    private static boolean lessCorner(int minX, int minY, int minZ, int[] best) {
        if (minX != best[0]) {
            return minX < best[0];
        }
        if (minY != best[1]) {
            return minY < best[1];
        }
        return minZ < best[2];
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

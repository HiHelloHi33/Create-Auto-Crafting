package net.hihellohi.createautocrafting.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.hihellohi.createautocrafting.blocks.FactoryMemberBlock.Part;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Forming and breaking up the 2x2 factories. A factory is a flat 2x2 square containing exactly one
 * controller, one Energy Input and two Factory Cores, in any arrangement. The controller decides
 * which way is "front" (one of the four squares it can be the corner of).
 */
public final class Formation {
    private Formation() {}

    public static BlockPos partPos(BlockPos controller, Direction facing, Part part) {
        return controller.relative(facing, part.forward).relative(facing.getClockWise(), part.right);
    }

    public static BlockPos controllerPos(BlockPos pos, BlockState state) {
        Part part = state.getValue(FactoryMemberBlock.PART);
        Direction facing = state.getValue(FactoryMemberBlock.FACING);
        return pos.relative(facing, -part.forward).relative(facing.getClockWise(), -part.right);
    }

    @Nullable
    public static FactoryControllerBlockEntity getController(BlockGetter level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(controllerPos(pos, state));
        return be instanceof FactoryControllerBlockEntity c ? c : null;
    }

    @Nullable
    public static EnergyInputBlockEntity findEnergyInput(Level level, BlockPos controller, BlockState controllerState) {
        Direction facing = controllerState.getValue(FactoryMemberBlock.FACING);
        for (Part part : Part.values()) {
            if (level.getBlockEntity(partPos(controller, facing, part)) instanceof EnergyInputBlockEntity e) {
                return e;
            }
        }
        return null;
    }

    /** Looks for a controller in the 3x3 around {@code pos} that can now form, and forms it. */
    public static void tryFormAround(Level level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = pos.offset(dx, 0, dz);
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof FactoryControllerBlock<?> && !s.getValue(FactoryMemberBlock.FORMED)
                        && tryForm(level, p)) {
                    return;
                }
            }
        }
    }

    /** Tries each of the four squares the controller could be the corner of. */
    public static boolean tryForm(Level level, BlockPos controller) {
        BlockState cs = level.getBlockState(controller);
        if (!(cs.getBlock() instanceof FactoryControllerBlock<?> cb) || cs.getValue(FactoryMemberBlock.FORMED)) {
            return false;
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            List<BlockPos> others = new ArrayList<>();
            int cores = 0;
            int energy = 0;
            boolean ok = true;
            for (Part part : Part.values()) {
                if (part == Part.CONTROLLER) {
                    continue;
                }
                BlockPos p = partPos(controller, facing, part);
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof FactoryMemberBlock && s.getValue(FactoryMemberBlock.FORMED)) {
                    ok = false;
                } else if (s.getBlock() instanceof FactoryCoreBlock) {
                    cores++;
                } else if (s.getBlock() instanceof EnergyInputBlock) {
                    energy++;
                } else {
                    ok = false;
                }
                others.add(p);
            }
            if (!ok || cores != 2 || energy != 1) {
                continue;
            }
            for (Part part : Part.values()) {
                BlockPos p = partPos(controller, facing, part);
                BlockState s = level.getBlockState(p);
                level.setBlock(p, s.setValue(FactoryMemberBlock.FORMED, true)
                        .setValue(FactoryMemberBlock.FACING, facing)
                        .setValue(FactoryMemberBlock.PART, part)
                        .setValue(FactoryMemberBlock.KIND, cb.kind()), 3);
            }
            if (level.getBlockEntity(controller) instanceof FactoryControllerBlockEntity be) {
                be.onFormed();
            }
            return true;
        }
        return false;
    }

    /** Turns every member of the factory that {@code pos} belonged to back into a plain block. */
    public static void unform(Level level, BlockPos pos, BlockState state) {
        BlockPos controller = controllerPos(pos, state);
        Direction facing = state.getValue(FactoryMemberBlock.FACING);
        for (Part part : Part.values()) {
            BlockPos p = partPos(controller, facing, part);
            BlockState s = level.getBlockState(p);
            if (!p.equals(pos) && s.getBlock() instanceof FactoryMemberBlock && s.getValue(FactoryMemberBlock.FORMED)
                    && s.getValue(FactoryMemberBlock.FACING) == facing && s.getValue(FactoryMemberBlock.PART) == part) {
                level.setBlock(p, s.setValue(FactoryMemberBlock.FORMED, false)
                        .setValue(FactoryMemberBlock.KIND, FactoryKind.NONE), 3);
            }
        }
        if (level.getBlockEntity(controller) instanceof FactoryControllerBlockEntity be) {
            be.onUnformed();
        }
    }
}

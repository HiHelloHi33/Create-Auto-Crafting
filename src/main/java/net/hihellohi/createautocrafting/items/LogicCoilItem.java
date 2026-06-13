package net.hihellohi.createautocrafting.items;

import net.hihellohi.createautocrafting.blocks.LogicCoilBlock;
import net.hihellohi.createautocrafting.blocks.LogicCoilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BlockItem for the Logic Coil. Right-clicking the top of an existing coil (without sneaking) places
 * a whole new layer matching the footprint below it (like stacking a Create fluid tank). Sneak +
 * right-click places just a single coil, so you can extend a big setup one block at a time.
 */
public class LogicCoilItem extends BlockItem {
    private static final int MAX_FOOTPRINT_BLOCKS = 64;

    public LogicCoilItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        if (player != null && !player.isSecondaryUseActive()
                && context.getClickedFace() == Direction.UP
                && level.getBlockState(clicked).getBlock() instanceof LogicCoilBlock) {
            return placeLayer(context, clicked);
        }
        return super.useOn(context); // single coil (sneaking, or not onto a coil's top)
    }

    private InteractionResult placeLayer(UseOnContext context, BlockPos topCoil) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        int y = topCoil.getY() + 1;

        List<BlockPos> targets = new ArrayList<>();
        for (BlockPos fp : floodPlane(level, topCoil)) {
            BlockPos t = new BlockPos(fp.getX(), y, fp.getZ());
            if (level.getBlockState(t).canBeReplaced()) {
                targets.add(t);
            }
        }
        if (targets.isEmpty()) {
            return InteractionResult.PASS; // nothing to extend onto; fall through is handled by caller
        }

        boolean creative = player != null && player.isCreative();
        int budget = creative ? targets.size() : Math.min(targets.size(), stack.getCount());
        if (budget <= 0) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            BlockState coilState = getBlock().defaultBlockState();
            for (int i = 0; i < budget; i++) {
                level.setBlock(targets.get(i), coilState, Block.UPDATE_ALL);
                if (player != null && level.getBlockEntity(targets.get(i)) instanceof LogicCoilBlockEntity coil) {
                    coil.setOwner(player);
                }
            }
            level.playSound(null, topCoil, coilState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1f, 0.9f);
            if (!creative) {
                stack.shrink(budget);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** All coils on the same Y level connected to {@code start} (the footprint of that layer). */
    private static Set<BlockPos> floodPlane(Level level, BlockPos start) {
        Set<BlockPos> group = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos s = start.immutable();
        group.add(s);
        queue.add(s);
        int y = start.getY();
        Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        while (!queue.isEmpty() && group.size() < MAX_FOOTPRINT_BLOCKS) {
            BlockPos pos = queue.poll();
            for (Direction dir : dirs) {
                BlockPos next = pos.relative(dir);
                if (next.getY() == y && !group.contains(next)
                        && level.getBlockState(next).getBlock() instanceof LogicCoilBlock) {
                    BlockPos n = next.immutable();
                    group.add(n);
                    queue.add(n);
                }
            }
        }
        return group;
    }
}

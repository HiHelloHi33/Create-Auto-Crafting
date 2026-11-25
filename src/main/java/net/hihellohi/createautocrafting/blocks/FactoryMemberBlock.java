package net.hihellohi.createautocrafting.blocks;

import com.simibubi.create.AllItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Base of every block that can be part of a 2x2 factory: the Factory Core, the Energy Input and the
 * two controllers. On its own it is a plain block; once a controller finds a valid 2x2 around itself
 * (see {@link Formation}) all four blocks become {@code formed}, learn which part of the square they
 * are, and take on the factory's body look. Clicks on any formed part are handed to the controller.
 */
public abstract class FactoryMemberBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final DirectionProperty FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final EnumProperty<FactoryKind> KIND = EnumProperty.create("kind", FactoryKind.class);

    /** Where a block sits in the 2x2: the controller, in front of it, to its right, or diagonal. */
    public enum Part implements StringRepresentable {
        CONTROLLER("controller", 0, 0),
        FRONT("front", 1, 0),
        RIGHT("right", 0, 1),
        DIAGONAL("diagonal", 1, 1);

        private final String name;
        public final int forward;
        public final int right;

        Part(String name, int forward, int right) {
            this.name = name;
            this.forward = forward;
            this.right = right;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    protected FactoryMemberBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false).setValue(FACING, Direction.NORTH)
                .setValue(PART, Part.CONTROLLER).setValue(KIND, FactoryKind.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, FACING, PART, KIND);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(state.getBlock()) && !state.getValue(FORMED)) {
            Formation.tryFormAround(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && state.getValue(FORMED)) {
                Formation.unform(level, pos, state);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ---- clicks on a formed factory go to its controller --------------------------------------

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (AllItems.WRENCH.isIn(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        FactoryControllerBlockEntity controller = state.getValue(FORMED)
                ? Formation.getController(level, pos, state) : null;
        if (controller == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return controller.useItem(stack, player, hand, pos, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        FactoryControllerBlockEntity controller = state.getValue(FORMED)
                ? Formation.getController(level, pos, state) : null;
        if (controller == null) {
            return InteractionResult.PASS;
        }
        return controller.use(player, pos, hit);
    }

    // ---- shape: a formed Etching Factory is a Create-style basin stretched over the 2x2 ------------

    /** The basin corner for each quarter turn (0, 90, 180, 270): walls on two outer sides, floor, foot. */
    private static final VoxelShape[] BASIN_CORNER = new VoxelShape[4];

    static {
        // Create's basin, but with walls the full 16 high, 2 thick, floor up to 5, a 2-high foot. This is the north-west corner.
        double[][] boxes = {{0, 2, 0, 16, 16, 2}, {0, 2, 2, 2, 16, 16}, {0, 2, 0, 16, 5, 16}, {2, 0, 2, 16, 2, 16}};
        for (int q = 0; q < 4; q++) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : boxes) {
                double x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
                for (int i = 0; i < q; i++) { // quarter turn clockwise: (x, z) -> (16 - z, x)
                    double nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
                    x0 = nx0; x1 = nx1; z0 = nz0; z1 = nz1;
                }
                shape = Shapes.or(shape, Shapes.box(x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16));
            }
            BASIN_CORNER[q] = shape;
        }
    }

    private static VoxelShape shapeOf(BlockState state) {
        if (!state.getValue(FORMED) || state.getValue(KIND) != FactoryKind.ETCHER) {
            return Shapes.block();
        }
        int base = switch (state.getValue(PART)) {
            case CONTROLLER -> 270;
            case FRONT -> 0;
            case RIGHT -> 180;
            case DIAGONAL -> 90;
        };
        int facing = switch (state.getValue(FACING)) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
        return BASIN_CORNER[((base + facing) % 360) / 90];
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}

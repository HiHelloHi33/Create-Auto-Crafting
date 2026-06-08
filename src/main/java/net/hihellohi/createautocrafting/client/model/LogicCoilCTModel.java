package net.hihellohi.createautocrafting.client.model;

import net.hihellohi.createautocrafting.blocks.LogicCoilBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Clean-room nine-slice connected-texture model for the Logic Coil. The supplied sheets are a 4x4
 * grid of 16px tiles laid out as a frame (corners / edges / interior, where a drawn border marks an
 * <em>un</em>connected side). For each face this checks the four in-plane neighbour coils and picks
 * the matching tile: left column when there's no left neighbour, right column when there's no right
 * neighbour, else the interior column (same for rows). Side faces sample {@code block_connected};
 * the up/down caps sample {@code block_top_connected}. Installed via Create's model swapper.
 */
public class LogicCoilCTModel extends BakedModelWrapper<BakedModel> {

    /** Per face (indexed by Direction.get3DDataValue): the chosen tile, packed as (tx &lt;&lt; 2) | ty. */
    public static final ModelProperty<int[]> FACE_TILES = new ModelProperty<>();

    private static final FaceBakery BAKERY = new FaceBakery();
    private static final Vector3f FROM = new Vector3f(0, 0, 0);
    private static final Vector3f TO = new Vector3f(16, 16, 16);
    private static final ResourceLocation MODEL_LOC =
            new ResourceLocation("createautocrafting", "block/logic_coil");

    /** Per-face texture basis (indexed by Direction.get3DDataValue): texUp, texRight, usesTopSheet. */
    private static final Direction[] TEX_UP = new Direction[6];
    private static final Direction[] TEX_RIGHT = new Direction[6];
    private static final boolean[] TOP_SHEET = new boolean[6];
    static {
        set(Direction.DOWN, Direction.SOUTH, Direction.EAST, true);
        set(Direction.UP, Direction.NORTH, Direction.EAST, true);
        set(Direction.NORTH, Direction.UP, Direction.WEST, false);
        set(Direction.SOUTH, Direction.UP, Direction.EAST, false);
        set(Direction.WEST, Direction.UP, Direction.SOUTH, false);
        set(Direction.EAST, Direction.UP, Direction.NORTH, false);
    }

    private static void set(Direction face, Direction up, Direction right, boolean top) {
        int i = face.get3DDataValue();
        TEX_UP[i] = up;
        TEX_RIGHT[i] = right;
        TOP_SHEET[i] = top;
    }

    private TextureAtlasSprite sideSprite;
    private TextureAtlasSprite topSprite;

    public LogicCoilCTModel(BakedModel base) {
        super(base);
    }

    private TextureAtlasSprite sprite(boolean top) {
        if (top) {
            if (topSprite == null) {
                topSprite = atlas("block/block_top_connected");
            }
            return topSprite;
        }
        if (sideSprite == null) {
            sideSprite = atlas("block/block_connected");
        }
        return sideSprite;
    }

    private static TextureAtlasSprite atlas(String path) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(new ResourceLocation("createautocrafting", path));
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        int[] tiles = new int[6];
        for (Direction face : Direction.values()) {
            int i = face.get3DDataValue();
            boolean top = isCoil(level, pos.relative(TEX_UP[i]));
            boolean bottom = isCoil(level, pos.relative(TEX_UP[i].getOpposite()));
            boolean right = isCoil(level, pos.relative(TEX_RIGHT[i]));
            boolean left = isCoil(level, pos.relative(TEX_RIGHT[i].getOpposite()));
            // Nine-slice with 2-wide edges/interior: middle columns/rows alternate by position so a
            // 2x2 interior (and 2-wide edges) tile seamlessly across the structure.
            int pr = Math.floorMod(coord(pos, TEX_RIGHT[i].getAxis()), 2);
            int pu = Math.floorMod(coord(pos, TEX_UP[i].getAxis()), 2);
            int tx = !left ? 0 : (!right ? 3 : 1 + pr);
            int ty = !top ? 0 : (!bottom ? 3 : 1 + pu);
            tiles[i] = (tx << 2) | ty;
        }
        return modelData.derive().with(FACE_TILES, tiles).build();
    }

    private static int coord(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    private static boolean isCoil(BlockAndTintGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof LogicCoilBlock;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        int[] tiles = data.get(FACE_TILES);
        if (tiles == null) {
            return originalModel.getQuads(state, side, rand, data, renderType); // item / no context
        }
        if (side == null) {
            return List.of();
        }
        int packed = tiles[side.get3DDataValue()];
        return List.of(buildFace(side, packed >> 2, packed & 3));
    }

    private BakedQuad buildFace(Direction face, int tx, int ty) {
        float uMin = tx * 4f, uMax = uMin + 4f;  // 4 uv-units per tile (16 / 4 columns)
        float vMin = ty * 4f, vMax = vMin + 4f;
        BlockFaceUV uv = new BlockFaceUV(new float[]{uMin, vMin, uMax, vMax}, 0);
        BlockElementFace bef = new BlockElementFace(face, -1, "ct", uv);
        return BAKERY.bakeQuad(FROM, TO, bef, sprite(TOP_SHEET[face.get3DDataValue()]), face,
                BlockModelRotation.X0_Y0, null, true, MODEL_LOC);
    }
}

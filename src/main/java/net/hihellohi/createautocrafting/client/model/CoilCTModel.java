package net.hihellohi.createautocrafting.client.model;

import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.hihellohi.createautocrafting.blocks.LogicCoilBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Wraps the Logic Coil's model so a lone coil (no neighbouring coils on any side) renders with the
 * plain base texture, while any coil that touches another uses Create's connected-texture model.
 * The connected rendering is entirely Create's {@link CTModel}; this only adds the "isolated → base"
 * switch.
 */
public class CoilCTModel extends BakedModelWrapper<BakedModel> {

    private static final ModelProperty<Boolean> ISOLATED = new ModelProperty<>();

    private final CTModel connected;

    public CoilCTModel(BakedModel base, ConnectedTextureBehaviour behaviour) {
        super(base);
        this.connected = new CTModel(base, behaviour);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        boolean isolated = true;
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).getBlock() instanceof LogicCoilBlock) {
                isolated = false;
                break;
            }
        }
        ModelData ctData = connected.getModelData(level, pos, state, data);
        return ctData.derive().with(ISOLATED, isolated).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        Boolean isolated = data.get(ISOLATED);
        if (isolated != null && isolated) {
            return originalModel.getQuads(state, side, rand, data, renderType); // plain base texture
        }
        return connected.getQuads(state, side, rand, data, renderType);
    }
}

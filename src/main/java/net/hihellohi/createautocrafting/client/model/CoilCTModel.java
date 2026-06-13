package net.hihellohi.createautocrafting.client.model;

import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.hihellohi.createautocrafting.manager.LogicCoilRegistry;
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
 * Wraps the Logic Coil's model so the connected texture only applies when the coil is part of a
 * valid assembly (a solid prism — see {@link LogicCoilRegistry#prismFor}). Coils that aren't
 * assembled (a lone coil, or an incomplete/irregular group) render the plain base texture instead,
 * so an unfinished structure doesn't show a torn connected texture. The connected rendering itself
 * is entirely Create's {@link CTModel}; for a valid prism every neighbour is part of it, so the
 * connection naturally stays within the assembly.
 */
public class CoilCTModel extends BakedModelWrapper<BakedModel> {

    private static final ModelProperty<Boolean> ASSEMBLED = new ModelProperty<>();

    private final CTModel connected;

    public CoilCTModel(BakedModel base, ConnectedTextureBehaviour behaviour) {
        super(base);
        this.connected = new CTModel(base, behaviour);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        boolean assembled = LogicCoilRegistry.assemblyOf(level, pos) != null;
        ModelData ctData = connected.getModelData(level, pos, state, data);
        return ctData.derive().with(ASSEMBLED, assembled).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        Boolean assembled = data.get(ASSEMBLED);
        if (assembled == null || !assembled) {
            return originalModel.getQuads(state, side, rand, data, renderType); // plain base texture
        }
        return connected.getQuads(state, side, rand, data, renderType);
    }
}

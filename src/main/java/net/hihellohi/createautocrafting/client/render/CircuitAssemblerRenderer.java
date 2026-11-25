package net.hihellohi.createautocrafting.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.hihellohi.createautocrafting.blocks.CircuitAssemblerBlockEntity;
import net.hihellohi.createautocrafting.blocks.FactoryMemberBlock;
import net.hihellohi.createautocrafting.etching.AssemblyRecipe;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;

/**
 * Draws the Circuit Assembler's inside: a rack of component feeders along the back, the plate in the
 * middle, and a gantry with a pick-and-place head. While a craft runs, the head fetches each component in
 * turn and sets it on the plate; once all are placed the plate turns into the finished circuit.
 */
public class CircuitAssemblerRenderer implements BlockEntityRenderer<CircuitAssemblerBlockEntity> {
    private static final float FLOOR = 4 / 16f;
    private static final float RACK_Z = 0.72f;
    private static final float HEAD_HEIGHT = 5 / 16f;
    private static final float BEAM_Y = 0.92f;
    private static final float HEAD_HIGH = 0.55f;
    private static final float HEAD_LOW = FLOOR + 0.17f;
    /** Where each component ends up on the plate (offsets from the plate's middle, in local x/z). */
    private static final float[][] SPOTS = {{-0.1f, -0.1f}, {0.1f, -0.1f}, {-0.1f, 0.1f}, {0.1f, 0.1f}, {0f, 0f}};

    public CircuitAssemblerRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static float stationX(int slot) {
        return -0.8f + 0.4f * (slot - 1);
    }

    @Override
    public void render(CircuitAssemblerBlockEntity be, float pt, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!state.hasProperty(FactoryMemberBlock.FORMED) || !state.getValue(FactoryMemberBlock.FORMED)) {
            return;
        }
        Direction facing = state.getValue(FactoryMemberBlock.FACING);

        ms.pushPose();
        TableRender.toLocalFrame(ms, facing);

        // Component feeders (what's loaded in each slot).
        for (int s = CircuitAssemblerBlockEntity.FIRST_COMPONENT; s < CircuitAssemblerBlockEntity.SLOT_OUTPUT; s++) {
            TableRender.flatStack(be.inventory.getStackInSlot(s), ms, buffer, light, overlay,
                    stationX(s), FLOOR + 0.03, RACK_Z, 0.3f, 0f);
        }
        // Finished circuits wait on the front tray.
        TableRender.flatStack(be.inventory.getStackInSlot(CircuitAssemblerBlockEntity.SLOT_OUTPUT), ms, buffer,
                light, overlay, 0, FLOOR + 0.03, -RACK_Z, 0.4f, 0f);

        ItemStack plate = be.inventory.getStackInSlot(CircuitAssemblerBlockEntity.SLOT_PLATE);
        float plateY = FLOOR + 0.04f;

        float headX = 0;
        float headZ = 0;
        float headY = HEAD_HIGH;
        ItemStack carried = ItemStack.EMPTY;

        RecipeHolder<AssemblyRecipe> holder = be.bestRecipe();
        int[] used = be.usedSlots().clone();
        Arrays.sort(used);
        float t = be.isWorking() ? be.getProgress(pt) : 0f;
        int n = used.length;

        if (be.isWorking() && holder != null && n > 0) {
            float slice = 1f / (n + 1);
            int p = Math.min(n, (int) (t / slice));
            if (p >= n) {
                // All placed: the plate has become the finished circuit.
                TableRender.single(holder.value().result().copyWithCount(1), ms, buffer, light, overlay,
                        0, plateY, 0, 0.5f, 0f);
            } else {
                float u = (t - p * slice) / slice;
                TableRender.single(plate, ms, buffer, light, overlay, 0, plateY, 0, 0.5f, 0f);

                // Components already set on the plate.
                for (int k = 0; k < p; k++) {
                    placed(be, used[k], k, ms, buffer, light, overlay, plateY);
                }

                float sx = stationX(used[p]);
                float prevX = p == 0 ? 0 : SPOTS[p - 1][0];
                float prevZ = p == 0 ? 0 : SPOTS[p - 1][1];
                float spotX = SPOTS[p][0];
                float spotZ = SPOTS[p][1];
                if (u < 0.35f) {
                    float e = TableRender.ease(u / 0.35f);
                    headX = TableRender.lerp(prevX, sx, e);
                    headZ = TableRender.lerp(prevZ, RACK_Z, e);
                } else if (u < 0.5f) {
                    headX = sx;
                    headZ = RACK_Z;
                } else if (u < 0.85f) {
                    float e = TableRender.ease((u - 0.5f) / 0.35f);
                    headX = TableRender.lerp(sx, spotX, e);
                    headZ = TableRender.lerp(RACK_Z, spotZ, e);
                } else {
                    headX = spotX;
                    headZ = spotZ;
                }
                // Dips at the feeder (around u=0.42) and at the plate (around u=0.93).
                float dip = Math.max(tri((u - 0.42f) / 0.12f), tri((u - 0.93f) / 0.09f));
                headY = TableRender.lerp(HEAD_HIGH, HEAD_LOW, dip);

                boolean holding = u >= 0.42f && u < 0.93f;
                if (holding) {
                    carried = be.inventory.getStackInSlot(used[p]);
                }
                if (u >= 0.93f) {
                    placed(be, used[p], p, ms, buffer, light, overlay, plateY);
                }
            }
        } else {
            TableRender.single(plate, ms, buffer, light, overlay, 0, plateY, 0, 0.5f, 0f);
        }

        if (!carried.isEmpty()) {
            TableRender.single(carried.copyWithCount(1), ms, buffer, light, overlay,
                    headX, headY - 0.04, headZ, 0.2f, 0f);
        }
        renderGantry(ms, buffer, light, overlay, headX, headY, headZ);
        ms.popPose();
    }

    private static float tri(float x) {
        return Math.max(0f, 1f - Math.abs(x));
    }

    /** A component sitting on the plate at its assigned spot. */
    private void placed(CircuitAssemblerBlockEntity be, int slot, int spot, PoseStack ms, MultiBufferSource buffer,
                        int light, int overlay, float plateY) {
        TableRender.single(be.inventory.getStackInSlot(slot).copyWithCount(1), ms, buffer, light, overlay,
                SPOTS[spot][0], plateY + 0.04, SPOTS[spot][1], 0.2f, 0f);
    }

    /** The cross beam (riding on the top frame), the head on it, and the rod down to the nozzle. */
    private void renderGantry(PoseStack ms, MultiBufferSource buffer, int light, int overlay,
                              float x, float y, float z) {
        ms.pushPose();
        ms.translate(0, BEAM_Y, z);
        ms.scale(1.9f, 1f, 1f);
        ms.translate(-0.5, -0.5, -0.5);
        ClientModels.render(ClientModels.ASSEMBLER_GANTRY, ms, buffer, light, overlay);
        ms.popPose();

        // Rod from the beam down to the head.
        ms.pushPose();
        ms.translate(x, y + HEAD_HEIGHT, z);
        ms.scale(1f, Math.max(0.01f, BEAM_Y - y - HEAD_HEIGHT), 1f);
        ms.translate(-0.5, 0, -0.5);
        ClientModels.render(ClientModels.ASSEMBLER_ROD, ms, buffer, light, overlay);
        ms.popPose();

        ms.pushPose();
        ms.translate(x, y, z);
        ms.translate(-0.5, 0, -0.5);
        ClientModels.render(ClientModels.ASSEMBLER_HEAD, ms, buffer, light, overlay);
        ms.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CircuitAssemblerBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(2);
    }
}

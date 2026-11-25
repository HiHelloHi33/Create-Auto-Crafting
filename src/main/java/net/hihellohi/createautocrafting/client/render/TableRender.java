package net.hihellohi.createautocrafting.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Shared drawing helpers for the 2x2 machines. */
final class TableRender {
    private TableRender() {}

    /**
     * Moves the pose to the middle of the 2x2 and turns it so that local +X is the machine's right and
     * local -Z is its front. In that frame the controller block is at (-0.5, +0.5), FRONT (-0.5, -0.5),
     * RIGHT (+0.5, +0.5) and DIAGONAL (+0.5, -0.5), whatever way the machine faces.
     */
    static void toLocalFrame(PoseStack ms, Direction facing) {
        Direction right = facing.getClockWise();
        ms.translate(0.5 + (facing.getStepX() + right.getStepX()) * 0.5, 0,
                0.5 + (facing.getStepZ() + right.getStepZ()) * 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
    }

    /** A stack lying flat at the given local position, drawn as up to three layers by count. */
    static void flatStack(ItemStack stack, PoseStack ms, MultiBufferSource buffer, int light, int overlay,
                          double x, double y, double z, float scale, float spin) {
        if (stack.isEmpty()) {
            return;
        }
        int layers = stack.getCount() > 32 ? 3 : stack.getCount() > 8 ? 2 : 1;
        for (int i = 0; i < layers; i++) {
            single(stack, ms, buffer, light, overlay, x, y + i * 0.03, z, scale, spin + i * 17f);
        }
    }

    static void single(ItemStack stack, PoseStack ms, MultiBufferSource buffer, int light, int overlay,
                       double x, double y, double z, float scale, float spin) {
        if (stack.isEmpty()) {
            return;
        }
        ms.pushPose();
        ms.translate(x, y, z);
        ms.mulPose(Axis.YP.rotationDegrees(spin));
        ms.mulPose(Axis.XP.rotationDegrees(90));
        ms.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, overlay,
                ms, buffer, Minecraft.getInstance().level, 0);
        ms.popPose();
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    static float ease(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
}

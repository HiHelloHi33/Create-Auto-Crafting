package net.hihellohi.createautocrafting.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.hihellohi.createautocrafting.blocks.EtchingFactoryBlockEntity;
import net.hihellohi.createautocrafting.blocks.FactoryMemberBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

/**
 * Draws the Etching Factory's basin: acid you can see from above, the plate held by two small robot arms,
 * and the finished plate floating on the surface. While working the arms lower the plate into the acid,
 * hold it there, and lift it back out (the plate changes to its etched form while submerged).
 */
public class EtchingFactoryRenderer implements BlockEntityRenderer<EtchingFactoryBlockEntity> {
    private static final float FLOOR = 2 / 16f;
    private static final float INNER = 1 - 2 / 16f;
    private static final float ARM_TOP = 1.2f;

    public EtchingFactoryRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EtchingFactoryBlockEntity be, float pt, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!state.hasProperty(FactoryMemberBlock.FORMED) || !state.getValue(FactoryMemberBlock.FORMED)) {
            return;
        }
        Direction facing = state.getValue(FactoryMemberBlock.FACING);

        FluidStack fluid = be.getFluid();
        float fluidY = fluidLevel(fluid);

        ms.pushPose();
        TableRender.toLocalFrame(ms, facing);

        // ---- arms + plate ----
        ItemStack input = be.getInput();
        float armY = ARM_TOP;
        ItemStack shown = input;
        if (!input.isEmpty()) {
            if (be.isWorking()) {
                float t = be.getProgress(pt);
                float dipY = Math.max(FLOOR + 0.12f, fluidY - 0.04f);
                float down = t < 0.3f ? TableRender.ease(t / 0.3f) : t < 0.7f ? 1f : 1f - TableRender.ease((t - 0.7f) / 0.3f);
                armY = TableRender.lerp(ARM_TOP, dipY, down);
                ItemStack preview = be.previewResult();
                if (t > 0.5f && !preview.isEmpty()) {
                    shown = preview; // it has been etched by now
                }
            }
            renderArm(ms, buffer, light, overlay, -INNER, armY, 0);
            renderArm(ms, buffer, light, overlay, INNER, armY, 180);
            TableRender.flatStack(shown, ms, buffer, light, overlay, 0, armY + 0.0, 0, 0.5f, 0f);
        }

        // ---- finished plates float on the acid in the far corner ----
        ItemStack out = be.getOutput();
        if (!out.isEmpty()) {
            double bob = Math.sin((be.getLevel().getGameTime() + pt) / 12.0) * 0.01;
            TableRender.flatStack(out, ms, buffer, light, overlay, 0.5, Math.max(fluidY, FLOOR) + 0.03 + bob, -0.5, 0.45f, 20f);
        }

        // ---- the acid itself (drawn last so it tints what's under it) ----
        if (!fluid.isEmpty()) {
            renderFluid(ms, buffer, light, fluid, fluidY);
        }
        ms.popPose();
    }

    private static float fluidLevel(FluidStack fluid) {
        if (fluid.isEmpty()) {
            return FLOOR;
        }
        float fraction = Math.min(1f, fluid.getAmount() / (float) EtchingFactoryBlockEntity.TANK_CAPACITY);
        return FLOOR + 1 / 16f + fraction * (14 / 16f - FLOOR - 1 / 16f);
    }

    /** One robot arm: pivot on the basin wall, reaching in toward the plate. */
    private void renderArm(PoseStack ms, MultiBufferSource buffer, int light, int overlay, float x, float y, float yaw) {
        ms.pushPose();
        ms.translate(x, y, 0);
        ms.mulPose(Axis.YP.rotationDegrees(yaw));
        ms.scale(1.15f, 1f, 1f);
        ms.translate(-0.5, 0, -0.5);
        ClientModels.render(ClientModels.ETCHER_ARM, ms, buffer, light, overlay);
        ms.popPose();
    }

    private void renderFluid(PoseStack ms, MultiBufferSource buffer, int light, FluidStack fluid, float y) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ext.getStillTexture(fluid));
        int tint = ext.getTintColor(fluid);
        int r = tint >> 16 & 0xFF;
        int g = tint >> 8 & 0xFF;
        int b = tint & 0xFF;
        int a = 215;

        VertexConsumer vc = buffer.getBuffer(RenderType.translucent());
        PoseStack.Pose pose = ms.last();
        Matrix4f mat = pose.pose();
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float e = INNER;
        quad(vc, pose, mat, -e, y, -e, u0, v0, r, g, b, a, light);
        quad(vc, pose, mat, -e, y, e, u0, v1, r, g, b, a, light);
        quad(vc, pose, mat, e, y, e, u1, v1, r, g, b, a, light);
        quad(vc, pose, mat, e, y, -e, u1, v0, r, g, b, a, light);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Matrix4f mat, float x, float y, float z,
                             float u, float v, int r, int g, int b, int a, int light) {
        vc.addVertex(mat, x, y, z).setColor(r, g, b, a).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
    }

    @Override
    public AABB getRenderBoundingBox(EtchingFactoryBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(2);
    }
}

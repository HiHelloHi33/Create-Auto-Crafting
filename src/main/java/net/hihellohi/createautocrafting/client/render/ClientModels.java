package net.hihellohi.createautocrafting.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Moving parts of our machines (robot arms, gantry, head): standalone models the renderers draw by hand. */
public final class ClientModels {
    public static final ModelResourceLocation ETCHER_ARM = model("block/etcher_arm");
    public static final ModelResourceLocation ASSEMBLER_GANTRY = model("block/assembler_gantry");
    public static final ModelResourceLocation ASSEMBLER_HEAD = model("block/assembler_head");
    public static final ModelResourceLocation ASSEMBLER_ROD = model("block/assembler_rod");
    // Robot arm shown in a loose Circuit Assembler Controller.
    public static final ModelResourceLocation ARM_TURNTABLE = model("block/assembler_arm_turntable");
    public static final ModelResourceLocation ARM_LOWER_ARM = model("block/assembler_arm_lower_arm");
    public static final ModelResourceLocation ARM_UPPER_ARM = model("block/assembler_arm_upper_arm");
    public static final ModelResourceLocation ARM_CLAW = model("block/assembler_arm_claw");
    public static final ModelResourceLocation ARM_CLAW_LEFT = model("block/assembler_arm_claw_left");
    public static final ModelResourceLocation ARM_CLAW_RIGHT = model("block/assembler_arm_claw_right");
    public static final ModelResourceLocation ARM_HELD_COMPONENT = model("block/assembler_arm_held_component");
    public static final ModelResourceLocation ARM_HELD_CIRCUIT = model("block/assembler_arm_held_circuit");
    public static final ModelResourceLocation ARM_PLACED_COMPONENT = model("block/assembler_arm_placed_component");

    private ClientModels() {}

    private static ModelResourceLocation model(String path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, path));
    }

    public static void register(ModelEvent.RegisterAdditional event) {
        event.register(ETCHER_ARM);
        event.register(ASSEMBLER_GANTRY);
        event.register(ASSEMBLER_HEAD);
        event.register(ASSEMBLER_ROD);
        event.register(ARM_TURNTABLE);
        event.register(ARM_LOWER_ARM);
        event.register(ARM_UPPER_ARM);
        event.register(ARM_CLAW);
        event.register(ARM_CLAW_LEFT);
        event.register(ARM_CLAW_RIGHT);
        event.register(ARM_HELD_COMPONENT);
        event.register(ARM_HELD_CIRCUIT);
        event.register(ARM_PLACED_COMPONENT);
    }

    /** Draws one of the models at the current pose (model space: 0..16 px = one block). */
    public static void render(ModelResourceLocation id, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel baked = mc.getModelManager().getModel(id);
        mc.getBlockRenderer().getModelRenderer().renderModel(ms.last(), buffer.getBuffer(RenderType.solid()),
                null, baked, 1f, 1f, 1f, light, overlay);
    }
}

package net.hihellohi.createautocrafting.fluid;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** Client side of the Etching Acid: its own animated textures (no tint). */
public final class ModFluidClient {
    private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath("createautocrafting", "block/etching_acid_still");
    private static final ResourceLocation FLOW = ResourceLocation.fromNamespaceAndPath("createautocrafting", "block/etching_acid_flow");
    private static final ResourceLocation OVERLAY = ResourceLocation.fromNamespaceAndPath("createautocrafting", "block/etching_acid_still");
    public static final int TINT = 0xFFFFFFFF;

    private ModFluidClient() {}

    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOW;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return OVERLAY;
            }

            @Override
            public int getTintColor() {
                return TINT;
            }
        }, ModFluids.ETCHING_ACID_TYPE.get());
    }

    public static void setRenderLayers() {
        ItemBlockRenderTypes.setRenderLayer(ModFluids.ETCHING_ACID.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModFluids.ETCHING_ACID_FLOWING.get(), RenderType.translucent());
    }
}

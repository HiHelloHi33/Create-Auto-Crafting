package net.hihellohi.createautocrafting.client;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.logistics.packager.PackagerRenderer;
import com.simibubi.create.content.logistics.packager.PackagerVisual;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.ponder.foundation.PonderIndex;
import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.blocks.ModBlocks;
import net.hihellohi.createautocrafting.client.model.CoilCTBehaviour;
import net.hihellohi.createautocrafting.client.model.CoilCTModel;
import net.hihellohi.createautocrafting.client.ponder.CACPonderPlugin;
import net.hihellohi.createautocrafting.client.render.CircuitAssemblerRenderer;
import net.hihellohi.createautocrafting.client.render.ClientModels;
import net.hihellohi.createautocrafting.client.render.EtchingFactoryRenderer;
import net.hihellohi.createautocrafting.client.screen.CircuitAssemblerScreen;
import net.hihellohi.createautocrafting.fluid.ModFluidClient;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.hihellohi.createautocrafting.client.screen.CraftingDistributorScreen;
import net.hihellohi.createautocrafting.client.screen.PatternCardScreen;
import net.hihellohi.createautocrafting.manager.ClientPatternCache;
import net.hihellohi.createautocrafting.menu.ModMenus;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only setup: screens, block-entity renderers/visuals, connected textures, Ponder. */
public final class CreateAutoCraftingClient {
    private CreateAutoCraftingClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(CreateAutoCraftingClient::onClientSetup);
        modBus.addListener(CreateAutoCraftingClient::onRegisterScreens);
        modBus.addListener(CreateAutoCraftingClient::onRegisterRenderers);
        modBus.addListener(ClientModels::register);
        modBus.addListener(ModFluidClient::registerExtensions);
        // The craftable cache belongs to one server; don't carry it into the next world.
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientPatternCache.clear());
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.PATTERN_CARD.get(), PatternCardScreen::new);
        event.register(ModMenus.CRAFTING_DISTRIBUTOR.get(), CraftingDistributorScreen::new);
        event.register(ModMenus.CIRCUIT_ASSEMBLER.get(), CircuitAssemblerScreen::new);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // The distributor is a Packager, so it borrows Create's packager renderer (tray, hatch, box).
        event.registerBlockEntityRenderer(ModBlocks.CRAFTING_DISTRIBUTOR_BE.get(), PackagerRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.ETCHING_FACTORY_BE.get(), EtchingFactoryRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.CIRCUIT_ASSEMBLER_BE.get(), CircuitAssemblerRenderer::new);
    }

    private static ResourceLocation coilTex(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, "block/" + path);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Create's packager models rely on a code-assigned cutout layer; without it the
            // transparent texels of the distributor render solid black.
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CRAFTING_DISTRIBUTOR.get(), RenderType.cutoutMipped());

            // Flywheel visuals (used instead of the renderers above when Flywheel is active).
            SimpleBlockEntityVisualizer.builder(ModBlocks.CRAFTING_DISTRIBUTOR_BE.get())
                    .factory(PackagerVisual::new)
                    .skipVanillaRender(be -> false)
                    .apply();
            ModFluidClient.setRenderLayers();

            // The Factory Core connects to other cores like a casing (sheets: factory_core*.png).
            var coreBehaviour = new com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour(
                    CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, coilTex("factory_core"), coilTex("factory_core_connected")),
                    CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, coilTex("factory_core_top"), coilTex("factory_core_top_connected"))) {
                @Override
                public boolean connectsTo(net.minecraft.world.level.block.state.BlockState state,
                                          net.minecraft.world.level.block.state.BlockState other,
                                          net.minecraft.world.level.BlockAndTintGetter reader,
                                          net.minecraft.core.BlockPos pos, net.minecraft.core.BlockPos otherPos,
                                          net.minecraft.core.Direction face) {
                    // Only loose cores connect; a formed factory has its own body look.
                    return super.connectsTo(state, other, reader, pos, otherPos, face)
                            && !state.getValue(net.hihellohi.createautocrafting.blocks.FactoryMemberBlock.FORMED)
                            && !other.getValue(net.hihellohi.createautocrafting.blocks.FactoryMemberBlock.FORMED);
                }
            };
            CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(
                    ModBlocks.FACTORY_CORE.getId(),
                    model -> new com.simibubi.create.foundation.block.connected.CTModel(model, coreBehaviour));

            // Connect the Logic Coil with Create's CTM, using our own sheets in the same RECTANGLE
            // layout the fluid tank uses: block/block(+_connected) on sides, block/block_top(+_connected) on caps.
            CoilCTBehaviour behaviour = new CoilCTBehaviour(
                    CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, coilTex("block"), coilTex("block_connected")),
                    CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, coilTex("block_top"), coilTex("block_top_connected")));
            CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(
                    ModBlocks.LOGIC_COIL.getId(),
                    model -> new CoilCTModel(model, behaviour));

            // Ponder scenes (the W-key guide), mirroring Create's CreateClient.clientInit.
            PonderIndex.addPlugin(new CACPonderPlugin());
        });
    }
}

package net.hihellohi.createautocrafting.client;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import net.createmod.ponder.foundation.PonderIndex;
import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.blocks.ModBlocks;
import net.hihellohi.createautocrafting.client.model.CoilCTBehaviour;
import net.hihellohi.createautocrafting.client.model.CoilCTModel;
import net.hihellohi.createautocrafting.client.ponder.CACPonderPlugin;
import net.hihellohi.createautocrafting.client.screen.CraftingBlueprintScreen;
import net.hihellohi.createautocrafting.client.screen.PatternProviderScreen;
import net.hihellohi.createautocrafting.menu.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class CreateAutoCraftingClient {
    public static void register(IEventBus modBus) {
        modBus.addListener(CreateAutoCraftingClient::onClientSetup);
        modBus.addListener(CreateAutoCraftingClient::onRegisterScreens);
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.CRAFTING_BLUEPRINT.get(), CraftingBlueprintScreen::new);
        event.register(ModMenus.PATTERN_PROVIDER.get(), PatternProviderScreen::new);
    }

    private static ResourceLocation coilTex(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, "block/" + path);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Connect the Logic Coil with Create's CTM, using our own sheets in the same RECTANGLE
            // layout the fluid tank uses: block/block(+_connected) on sides, block/block_top(+_connected) on caps.
            CoilCTBehaviour behaviour = new CoilCTBehaviour(
                    CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, coilTex("block"), coilTex("block_connected")),
                    CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, coilTex("block_top"), coilTex("block_top_connected")));
            CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(
                    ModBlocks.LOGIC_COIL.getId(),
                    model -> new CoilCTModel(model, behaviour));

            // Register our Ponder scenes (W-key guide). Mirrors Create's CreateClient.clientInit.
            PonderIndex.addPlugin(new CACPonderPlugin());
        });
        CreateAutoCrafting.LOGGER.info("Create Auto Crafting client loaded");
    }
}

package net.hihellohi.createautocrafting.client;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import net.createmod.ponder.foundation.PonderIndex;
import net.hihellohi.createautocrafting.client.model.CoilCTBehaviour;
import net.minecraft.resources.ResourceLocation;
import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.blocks.ModBlocks;
import net.hihellohi.createautocrafting.client.model.CoilCTModel;
import net.hihellohi.createautocrafting.client.ponder.CACPonderPlugin;
import net.hihellohi.createautocrafting.client.screen.CraftingBlueprintScreen;
import net.hihellohi.createautocrafting.client.screen.PatternProviderScreen;
import net.hihellohi.createautocrafting.menu.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CreateAutoCrafting.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateAutoCraftingClient {
    public static void register(IEventBus modBus) {
        modBus.addListener(CreateAutoCraftingClient::onClientSetup);
    }

    private static ResourceLocation coilTex(String path) {
        return new ResourceLocation(CreateAutoCrafting.MODID, "block/" + path);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.CRAFTING_BLUEPRINT.get(), CraftingBlueprintScreen::new);
            MenuScreens.register(ModMenus.PATTERN_PROVIDER.get(), PatternProviderScreen::new);

            // Connect the Logic Coil with Create's CTM, using our own sheets in the same RECTANGLE
            // layout the fluid tank uses: block/block(+_connected) on sides, block/block_top(+_connected) on caps.
            CoilCTBehaviour behaviour = new CoilCTBehaviour(
                    CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, coilTex("block"), coilTex("block_connected")),
                    CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, coilTex("block_top"), coilTex("block_top_connected")));
            CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(
                    ModBlocks.LOGIC_COIL.getId(),
                    model -> new CoilCTModel(model, behaviour));
        });
        // Register our Ponder scenes (W-key guide). Mirrors Create's CreateClient.clientInit.
        PonderIndex.addPlugin(new CACPonderPlugin());
        CreateAutoCrafting.LOGGER.info("Create Auto Crafting client loaded");
    }
}

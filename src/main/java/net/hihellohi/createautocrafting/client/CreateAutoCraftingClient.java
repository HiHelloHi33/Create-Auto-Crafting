package net.hihellohi.createautocrafting.client;

import com.simibubi.create.CreateClient;
import net.createmod.ponder.foundation.PonderIndex;
import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.blocks.ModBlocks;
import net.hihellohi.createautocrafting.client.model.LogicCoilCTModel;
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

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.CRAFTING_BLUEPRINT.get(), CraftingBlueprintScreen::new);
            MenuScreens.register(ModMenus.PATTERN_PROVIDER.get(), PatternProviderScreen::new);

            // Wrap the Logic Coil's model with our nine-slice connected-texture model (uses Create's
            // model-swap hook, but the connection logic + the sheets are ours).
            CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(
                    ModBlocks.LOGIC_COIL.getId(),
                    LogicCoilCTModel::new);
        });
        // Register our Ponder scenes (W-key guide). Mirrors Create's CreateClient.clientInit.
        PonderIndex.addPlugin(new CACPonderPlugin());
        CreateAutoCrafting.LOGGER.info("Create Auto Crafting client loaded");
    }
}

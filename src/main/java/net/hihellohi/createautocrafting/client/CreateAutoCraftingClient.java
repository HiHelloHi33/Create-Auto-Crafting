package net.hihellohi.createautocrafting.client;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import net.createmod.ponder.foundation.PonderIndex;
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

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.CRAFTING_BLUEPRINT.get(), CraftingBlueprintScreen::new);
            MenuScreens.register(ModMenus.PATTERN_PROVIDER.get(), PatternProviderScreen::new);

            // Connect the Logic Coil with Create's own CTM. Placeholder: reuse Create's fluid-tank
            // sprite-shift entries (they reference Create's textures at runtime, nothing copied) so
            // it looks like a tank until proper textures are made.
            HorizontalCTBehaviour behaviour =
                    new HorizontalCTBehaviour(AllSpriteShifts.FLUID_TANK, AllSpriteShifts.FLUID_TANK_TOP);
            CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(
                    ModBlocks.LOGIC_COIL.getId(),
                    model -> new CoilCTModel(model, behaviour));
        });
        // Register our Ponder scenes (W-key guide). Mirrors Create's CreateClient.clientInit.
        PonderIndex.addPlugin(new CACPonderPlugin());
        CreateAutoCrafting.LOGGER.info("Create Auto Crafting client loaded");
    }
}

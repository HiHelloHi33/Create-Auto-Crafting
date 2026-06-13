package net.hihellohi.createautocrafting;

import com.mojang.logging.LogUtils;
import net.hihellohi.createautocrafting.blocks.ModBlocks;
import net.hihellohi.createautocrafting.client.CreateAutoCraftingClient;
import net.hihellohi.createautocrafting.items.ModDataComponents;
import net.hihellohi.createautocrafting.items.ModItems;
import net.hihellohi.createautocrafting.menu.ModMenus;
import net.hihellohi.createautocrafting.network.ModPackets;
import net.hihellohi.createautocrafting.registry.ModCreativeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(CreateAutoCrafting.MODID)
public class CreateAutoCrafting {
    public static final String MODID = "createautocrafting";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateAutoCrafting(IEventBus modBus, ModContainer modContainer) {
        ModDataComponents.register(modBus);
        ModItems.register(modBus);
        ModBlocks.register(modBus);
        ModMenus.register(modBus);
        ModCreativeTabs.register(modBus);
        ModPackets.register(modBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            CreateAutoCraftingClient.register(modBus);
        }

        LOGGER.info("Create Auto Crafting loaded — Pattern Providers & Logic Coils ready.");
    }
}

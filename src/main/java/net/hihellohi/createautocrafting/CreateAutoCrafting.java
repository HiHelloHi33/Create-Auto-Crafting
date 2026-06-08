package net.hihellohi.createautocrafting;

import com.mojang.logging.LogUtils;
import net.hihellohi.createautocrafting.blocks.ModBlocks;
import net.hihellohi.createautocrafting.client.CreateAutoCraftingClient;
import net.hihellohi.createautocrafting.items.ModItems;
import net.hihellohi.createautocrafting.menu.ModMenus;
import net.hihellohi.createautocrafting.network.ModPackets;
import net.hihellohi.createautocrafting.registry.ModCreativeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CreateAutoCrafting.MODID)
public class CreateAutoCrafting {
    public static final String MODID = "createautocrafting";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateAutoCrafting() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modBus);
        ModBlocks.register(modBus);
        ModMenus.register(modBus);
        ModCreativeTabs.register(modBus);
        ModPackets.register();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CreateAutoCraftingClient.register(modBus));

        LOGGER.info("Create Auto Crafting loaded — Pattern Providers & Logic Coils ready.");
    }
}

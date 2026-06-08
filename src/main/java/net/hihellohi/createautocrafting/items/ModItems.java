package net.hihellohi.createautocrafting.items;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.blocks.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CreateAutoCrafting.MODID);

    public static final RegistryObject<Item> PACKAGER_CRAFTER_KIT = ITEMS.register("packager_crafter_kit",
            () -> new PackagerCrafterKitItem(new Item.Properties()
                    .stacksTo(16)));

    public static final RegistryObject<Item> LOGIC_PROCESSOR = ITEMS.register("logic_processor",
            () -> new LogicProcessorItem(new Item.Properties()
                    .stacksTo(64)));

    public static final RegistryObject<Item> CRAFTING_BLUEPRINT = ITEMS.register("crafting_blueprint",
            () -> new CraftingBlueprintItem(new Item.Properties()
                    .stacksTo(1)));

    public static final RegistryObject<BlockItem> LOGIC_COIL = ITEMS.register("logic_coil",
            () -> new BlockItem(ModBlocks.LOGIC_COIL.get(),
                    new Item.Properties()
                            .stacksTo(64)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
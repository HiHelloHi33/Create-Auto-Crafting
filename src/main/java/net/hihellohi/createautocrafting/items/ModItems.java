package net.hihellohi.createautocrafting.items;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.blocks.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, CreateAutoCrafting.MODID);

    public static final DeferredHolder<Item, Item> PACKAGER_CRAFTER_KIT = ITEMS.register("packager_crafter_kit",
            () -> new PackagerCrafterKitItem(new Item.Properties()
                    .stacksTo(16)));

    public static final DeferredHolder<Item, Item> LOGIC_PROCESSOR = ITEMS.register("logic_processor",
            () -> new LogicProcessorItem(new Item.Properties()
                    .stacksTo(64)));

    public static final DeferredHolder<Item, Item> CRAFTING_BLUEPRINT = ITEMS.register("crafting_blueprint",
            () -> new CraftingBlueprintItem(new Item.Properties()
                    .stacksTo(1)));

    public static final DeferredHolder<Item, BlockItem> LOGIC_COIL = ITEMS.register("logic_coil",
            () -> new LogicCoilItem(ModBlocks.LOGIC_COIL.get(),
                    new Item.Properties()
                            .stacksTo(64)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

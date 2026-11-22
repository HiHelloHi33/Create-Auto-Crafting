package net.hihellohi.createautocrafting.items;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.blocks.ModBlocks;
import net.hihellohi.createautocrafting.fluid.ModFluids;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, CreateAutoCrafting.MODID);

    // ---- circuit line: unetched plate -> (Etching Factory) -> etched plate -> (Circuit Assembler) -> circuit ----
    public static final DeferredHolder<Item, Item> T1_UNETCHED_PLATE = tooltip("t1_unetched_plate");
    public static final DeferredHolder<Item, Item> T2_UNETCHED_PLATE = tooltip("t2_unetched_plate");
    public static final DeferredHolder<Item, Item> T1_ETCHED_PLATE = tooltip("t1_etched_plate");
    public static final DeferredHolder<Item, Item> T2_ETCHED_PLATE = tooltip("t2_etched_plate");
    public static final DeferredHolder<Item, Item> T1_CIRCUIT = tooltip("t1_circuit");
    public static final DeferredHolder<Item, Item> T2_CIRCUIT = tooltip("t2_circuit");
    public static final DeferredHolder<Item, Item> T3_CIRCUIT = tooltip("t3_circuit");

    // ---- components (they decide a circuit's tier) ------------------------------------------------
    public static final DeferredHolder<Item, Item> RESISTOR = tooltip("resistor");
    public static final DeferredHolder<Item, Item> INDUCTOR = tooltip("inductor");
    public static final DeferredHolder<Item, Item> DIODE = tooltip("diode");
    public static final DeferredHolder<Item, Item> CAPACITOR = tooltip("capacitor");
    public static final DeferredHolder<Item, Item> TRANSISTOR = tooltip("transistor");

    public static final DeferredHolder<Item, Item> ETCHING_ACID_BUCKET = ITEMS.register("etching_acid_bucket",
            () -> new net.minecraft.world.item.BucketItem(ModFluids.ETCHING_ACID.get(),
                    new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));

    public static final DeferredHolder<Item, Item> PATTERN_CARD = ITEMS.register("pattern_card",
            () -> new PatternCardItem(new Item.Properties().stacksTo(1)));

    // ---- block items ----------------------------------------------------------------------
    public static final DeferredHolder<Item, BlockItem> LOGIC_COIL = ITEMS.register("logic_coil",
            () -> new LogicCoilItem(ModBlocks.LOGIC_COIL.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> CRAFTING_DISTRIBUTOR = ITEMS.register("crafting_distributor",
            () -> new TooltipBlockItem(ModBlocks.CRAFTING_DISTRIBUTOR.get(), new Item.Properties(),
                    "tooltip.createautocrafting.crafting_distributor.1",
                    "tooltip.createautocrafting.crafting_distributor.2"));
    public static final DeferredHolder<Item, BlockItem> ETCHING_FACTORY = ITEMS.register("etching_factory_controller",
            () -> new TooltipBlockItem(ModBlocks.ETCHING_FACTORY.get(), new Item.Properties(),
                    "tooltip.createautocrafting.etching_factory.1",
                    "tooltip.createautocrafting.etching_factory.2"));
    public static final DeferredHolder<Item, BlockItem> FACTORY_CORE = ITEMS.register("factory_core",
            () -> new TooltipBlockItem(ModBlocks.FACTORY_CORE.get(), new Item.Properties(),
                    "tooltip.createautocrafting.factory_core.1", "tooltip.createautocrafting.factory_core.2"));
    public static final DeferredHolder<Item, BlockItem> ENERGY_INPUT = ITEMS.register("energy_input",
            () -> new TooltipBlockItem(ModBlocks.ENERGY_INPUT.get(), new Item.Properties(),
                    "tooltip.createautocrafting.energy_input.1", "tooltip.createautocrafting.energy_input.2"));
    public static final DeferredHolder<Item, BlockItem> CIRCUIT_ASSEMBLER = ITEMS.register("circuit_assembler_controller",
            () -> new TooltipBlockItem(ModBlocks.CIRCUIT_ASSEMBLER.get(), new Item.Properties(),
                    "tooltip.createautocrafting.circuit_assembler.1",
                    "tooltip.createautocrafting.circuit_assembler.2"));

    private static DeferredHolder<Item, Item> tooltip(String name) {
        return ITEMS.register(name, () -> new TooltipItem(new Item.Properties(),
                "tooltip.createautocrafting." + name + ".1"));
    }

    private ModItems() {}

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

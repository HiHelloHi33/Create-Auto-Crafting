package net.hihellohi.createautocrafting.blocks;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, CreateAutoCrafting.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateAutoCrafting.MODID);

    public static final DeferredHolder<Block, LogicCoilBlock> LOGIC_COIL =
            BLOCKS.register("logic_coil", LogicCoilBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogicCoilBlockEntity>> LOGIC_COIL_BE =
            BLOCK_ENTITIES.register("logic_coil", () ->
                    BlockEntityType.Builder.of(LogicCoilBlockEntity::new, LOGIC_COIL.get()).build(null));

    public static final DeferredHolder<Block, CraftingDistributorBlock> CRAFTING_DISTRIBUTOR =
            BLOCKS.register("crafting_distributor", CraftingDistributorBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraftingDistributorBlockEntity>> CRAFTING_DISTRIBUTOR_BE =
            BLOCK_ENTITIES.register("crafting_distributor", () ->
                    BlockEntityType.Builder.of(CraftingDistributorBlockEntity::new, CRAFTING_DISTRIBUTOR.get()).build(null));

    public static final DeferredHolder<Block, EtchingFactoryBlock> ETCHING_FACTORY =
            BLOCKS.register("etching_factory_controller", EtchingFactoryBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EtchingFactoryBlockEntity>> ETCHING_FACTORY_BE =
            BLOCK_ENTITIES.register("etching_factory_controller", () ->
                    BlockEntityType.Builder.of(EtchingFactoryBlockEntity::new, ETCHING_FACTORY.get()).build(null));

    public static final DeferredHolder<Block, CircuitAssemblerBlock> CIRCUIT_ASSEMBLER =
            BLOCKS.register("circuit_assembler_controller", CircuitAssemblerBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CircuitAssemblerBlockEntity>> CIRCUIT_ASSEMBLER_BE =
            BLOCK_ENTITIES.register("circuit_assembler_controller", () ->
                    BlockEntityType.Builder.of(CircuitAssemblerBlockEntity::new, CIRCUIT_ASSEMBLER.get()).build(null));

    /** The placed form of the Etching Acid bucket. */
    public static final DeferredHolder<Block, net.minecraft.world.level.block.LiquidBlock> ETCHING_ACID_BLOCK =
            BLOCKS.register("etching_acid", () -> new net.minecraft.world.level.block.LiquidBlock(
                    net.hihellohi.createautocrafting.fluid.ModFluids.ETCHING_ACID.get(),
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .ofFullCopy(net.minecraft.world.level.block.Blocks.WATER)));

    public static final DeferredHolder<Block, FactoryCoreBlock> FACTORY_CORE =
            BLOCKS.register("factory_core", FactoryCoreBlock::new);
    public static final DeferredHolder<Block, EnergyInputBlock> ENERGY_INPUT =
            BLOCKS.register("energy_input", EnergyInputBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyInputBlockEntity>> ENERGY_INPUT_BE =
            BLOCK_ENTITIES.register("energy_input", () ->
                    BlockEntityType.Builder.of(EnergyInputBlockEntity::new, ENERGY_INPUT.get()).build(null));

    private ModBlocks() {}

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        eventBus.addListener(ModBlocks::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // The distributor is a Packager, so it exposes the same item handler Create gives Packagers.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CRAFTING_DISTRIBUTOR_BE.get(),
                (be, side) -> be.inventory);

        // Cables put energy into the Energy Input.
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ENERGY_INPUT_BE.get(), (be, side) -> be.external);

        // Every part of a formed factory forwards items and fluids to its controller.
        for (var block : java.util.List.of(FACTORY_CORE, ENERGY_INPUT, ETCHING_FACTORY, CIRCUIT_ASSEMBLER)) {
            event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> {
                var c = formedController(level, pos, state);
                if (c instanceof EtchingFactoryBlockEntity e) {
                    return e.getAutomationHandler();
                }
                return c instanceof CircuitAssemblerBlockEntity a ? a.getAutomationHandler() : null;
            }, block.get());
            event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, be, side) -> {
                var c = formedController(level, pos, state);
                return c instanceof EtchingFactoryBlockEntity e ? e.getFluidHandler() : null;
            }, block.get());
        }
    }

    private static FactoryControllerBlockEntity formedController(net.minecraft.world.level.Level level,
                                                                 net.minecraft.core.BlockPos pos,
                                                                 net.minecraft.world.level.block.state.BlockState state) {
        return state.getValue(FactoryMemberBlock.FORMED) ? Formation.getController(level, pos, state) : null;
    }
}

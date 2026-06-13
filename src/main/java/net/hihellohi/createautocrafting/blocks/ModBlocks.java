package net.hihellohi.createautocrafting.blocks;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, CreateAutoCrafting.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateAutoCrafting.MODID);

    public static final DeferredHolder<Block, Block> LOGIC_COIL = BLOCKS.register("logic_coil",
            LogicCoilBlock::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogicCoilBlockEntity>> LOGIC_COIL_BE =
            BLOCK_ENTITIES.register("logic_coil",
                    () -> BlockEntityType.Builder.of(LogicCoilBlockEntity::new, LOGIC_COIL.get())
                            .build(null));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }
}

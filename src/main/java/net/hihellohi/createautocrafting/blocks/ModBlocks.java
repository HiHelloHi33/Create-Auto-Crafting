package net.hihellohi.createautocrafting.blocks;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CreateAutoCrafting.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CreateAutoCrafting.MODID);

    public static final RegistryObject<Block> LOGIC_COIL = BLOCKS.register("logic_coil",
            LogicCoilBlock::new);

    public static final RegistryObject<BlockEntityType<LogicCoilBlockEntity>> LOGIC_COIL_BE =
            BLOCK_ENTITIES.register("logic_coil",
                    () -> BlockEntityType.Builder.of(LogicCoilBlockEntity::new, LOGIC_COIL.get())
                            .build(null));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }
}
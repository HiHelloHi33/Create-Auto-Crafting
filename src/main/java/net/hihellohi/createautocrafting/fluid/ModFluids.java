package net.hihellohi.createautocrafting.fluid;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.blocks.ModBlocks;
import net.hihellohi.createautocrafting.items.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The Etching Acid the Etching Factory's basin is filled with. */
public final class ModFluids {
    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CreateAutoCrafting.MODID);
    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, CreateAutoCrafting.MODID);

    public static final DeferredHolder<FluidType, FluidType> ETCHING_ACID_TYPE = FLUID_TYPES.register("etching_acid",
            () -> new FluidType(FluidType.Properties.create()
                    .density(1200).viscosity(1500).canSwim(false).canDrown(true)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> ETCHING_ACID =
            FLUIDS.register("etching_acid", () -> new BaseFlowingFluid.Source(ModFluids.properties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> ETCHING_ACID_FLOWING =
            FLUIDS.register("flowing_etching_acid", () -> new BaseFlowingFluid.Flowing(ModFluids.properties()));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(ETCHING_ACID_TYPE, ETCHING_ACID, ETCHING_ACID_FLOWING)
                .bucket(ModItems.ETCHING_ACID_BUCKET)
                .block(ModBlocks.ETCHING_ACID_BLOCK)
                .slopeFindDistance(3).levelDecreasePerBlock(2);
    }

    private ModFluids() {}

    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
    }
}

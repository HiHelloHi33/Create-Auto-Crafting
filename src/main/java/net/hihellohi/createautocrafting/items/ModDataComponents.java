package net.hihellohi.createautocrafting.items;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Custom {@link DataComponentType}s. */
public final class ModDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CreateAutoCrafting.MODID);

    /** The recipe stored on a Pattern Card. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PatternData>> PATTERN =
            COMPONENTS.register("pattern", () -> DataComponentType.<PatternData>builder()
                    .persistent(PatternData.CODEC)
                    .networkSynchronized(PatternData.STREAM_CODEC)
                    .build());

    private ModDataComponents() {}

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}

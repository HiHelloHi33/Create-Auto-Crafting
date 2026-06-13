package net.hihellohi.createautocrafting.items;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom {@link DataComponentType}s. In 1.21 item state lives in components instead of NBT;
 * {@link #BLUEPRINT} replaces the old {@code Blueprint} compound tag on Crafting Blueprints.
 */
public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CreateAutoCrafting.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlueprintData>> BLUEPRINT =
            COMPONENTS.register("blueprint", () -> DataComponentType.<BlueprintData>builder()
                    .persistent(BlueprintData.CODEC)
                    .networkSynchronized(BlueprintData.STREAM_CODEC)
                    .build());

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}

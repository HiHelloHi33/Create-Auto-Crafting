package net.hihellohi.createautocrafting.registry;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.items.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateAutoCrafting.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createautocrafting"))
                    .icon(() -> new ItemStack(ModItems.T3_CIRCUIT.get()))
                    .displayItems((params, output) -> {
                        for (var item : java.util.List.of(ModItems.T1_UNETCHED_PLATE, ModItems.T2_UNETCHED_PLATE,
                                ModItems.T1_ETCHED_PLATE, ModItems.T2_ETCHED_PLATE,
                                ModItems.RESISTOR, ModItems.INDUCTOR, ModItems.DIODE, ModItems.CAPACITOR,
                                ModItems.TRANSISTOR, ModItems.T1_CIRCUIT, ModItems.T2_CIRCUIT, ModItems.T3_CIRCUIT,
                                ModItems.ETCHING_ACID_BUCKET)) {
                            output.accept(item.get());
                        }
                        output.accept(ModItems.FACTORY_CORE.get());
                        output.accept(ModItems.ENERGY_INPUT.get());
                        output.accept(ModItems.ETCHING_FACTORY.get());
                        output.accept(ModItems.CIRCUIT_ASSEMBLER.get());
                        output.accept(ModItems.CRAFTING_DISTRIBUTOR.get());
                        output.accept(ModItems.PATTERN_CARD.get());
                        output.accept(ModItems.LOGIC_COIL.get());
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus bus) {
        CREATIVE_TABS.register(bus);
    }
}

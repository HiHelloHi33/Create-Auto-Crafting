package net.hihellohi.createautocrafting.registry;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.blocks.ModBlocks;
import net.hihellohi.createautocrafting.items.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateAutoCrafting.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createautocrafting"))
                    .icon(() -> new ItemStack(ModItems.LOGIC_PROCESSOR.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.LOGIC_PROCESSOR.get());
                        output.accept(ModItems.PACKAGER_CRAFTER_KIT.get());
                        output.accept(ModItems.CRAFTING_BLUEPRINT.get());
                        output.accept(ModBlocks.LOGIC_COIL.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        CREATIVE_TABS.register(bus);
    }
}

package net.hihellohi.createautocrafting.menu;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CreateAutoCrafting.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<PatternCardMenu>> PATTERN_CARD =
            MENUS.register("pattern_card", () -> IMenuTypeExtension.create(PatternCardMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CraftingDistributorMenu>> CRAFTING_DISTRIBUTOR =
            MENUS.register("crafting_distributor", () -> IMenuTypeExtension.create(CraftingDistributorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CircuitAssemblerMenu>> CIRCUIT_ASSEMBLER =
            MENUS.register("circuit_assembler", () -> IMenuTypeExtension.create(CircuitAssemblerMenu::new));

    private ModMenus() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}

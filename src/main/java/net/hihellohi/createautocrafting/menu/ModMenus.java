package net.hihellohi.createautocrafting.menu;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CreateAutoCrafting.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<CraftingBlueprintMenu>> CRAFTING_BLUEPRINT =
            MENUS.register("crafting_blueprint",
                    () -> IMenuTypeExtension.create(CraftingBlueprintMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PatternProviderMenu>> PATTERN_PROVIDER =
            MENUS.register("pattern_provider",
                    () -> IMenuTypeExtension.create(PatternProviderMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}

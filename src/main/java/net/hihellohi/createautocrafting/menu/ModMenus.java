package net.hihellohi.createautocrafting.menu;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, CreateAutoCrafting.MODID);

    public static final RegistryObject<MenuType<CraftingBlueprintMenu>> CRAFTING_BLUEPRINT =
            MENUS.register("crafting_blueprint",
                    () -> IForgeMenuType.create(CraftingBlueprintMenu::new));

    public static final RegistryObject<MenuType<PatternProviderMenu>> PATTERN_PROVIDER =
            MENUS.register("pattern_provider",
                    () -> IForgeMenuType.create(PatternProviderMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}

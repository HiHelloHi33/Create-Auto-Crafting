package net.hihellohi.createautocrafting.client.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.items.ModItems;
import net.minecraft.resources.ResourceLocation;

/**
 * This mod's Ponder plugin. Registered with {@code PonderIndex.addPlugin(...)} during client setup
 * (mirroring Create's {@code CreateClient.clientInit}). Each {@code addStoryBoard} call binds a
 * scene to the item that shows it in the Ponder index; the {@code String} schematic path resolves
 * to {@code assets/createautocrafting/ponder/<path>.nbt} under this mod's namespace.
 *
 * <p>The Crafting Distributor has no item of its own (it is a converted Packager), so its scene is
 * attached to the Packager Crafter Kit — the item that creates it. Move that line to another
 * component if you'd rather surface it elsewhere.
 */
public class CACPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return CreateAutoCrafting.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(ModItems.CRAFTING_BLUEPRINT.getId(), "blueprint", CACScenes::blueprint);
        helper.addStoryBoard(ModItems.LOGIC_COIL.getId(), "logic_coil", CACScenes::logicCoil);
        helper.addStoryBoard(ModItems.PACKAGER_CRAFTER_KIT.getId(), "crafter_kit", CACScenes::crafterKit);
        helper.addStoryBoard(ModItems.PACKAGER_CRAFTER_KIT.getId(), "distributor", CACScenes::distributor);
    }
}

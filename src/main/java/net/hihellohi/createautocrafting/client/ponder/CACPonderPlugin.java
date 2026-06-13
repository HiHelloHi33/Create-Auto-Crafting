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
 * <p>Only the Logic Coil ships Ponder scenes; the Crafting Blueprint and Crafter Kit convey their
 * info through a Create-style "Press [Shift]" tooltip ({@code CACTooltips}) instead.
 */
public class CACPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return CreateAutoCrafting.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // Only the Logic Coil keeps a Ponder; the other items use a Shift-info tooltip instead.
        helper.addStoryBoard(ModItems.LOGIC_COIL.getId(), "coil_1", CACScenes::coilOne);
        //helper.addStoryBoard(ModItems.LOGIC_COIL.getId(), "coil_2", CACScenes::coilTwo);
    }
}

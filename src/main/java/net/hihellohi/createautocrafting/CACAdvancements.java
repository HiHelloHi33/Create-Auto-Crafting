package net.hihellohi.createautocrafting;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Grants the mod's advancements (criteria use minecraft:impossible and are awarded from code). */
public final class CACAdvancements {
    private CACAdvancements() {}

    public static void award(ServerPlayer player, String id, String criterion) {
        AdvancementHolder holder = player.server.getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, id));
        if (holder != null) {
            player.getAdvancements().award(holder, criterion);
        }
    }
}

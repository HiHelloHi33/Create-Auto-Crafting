package net.hihellohi.createautocrafting.api;

import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlock;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import net.hihellohi.createautocrafting.items.CraftingBlueprintItem;
import net.hihellohi.createautocrafting.manager.PatternRegistry;
import net.hihellohi.createautocrafting.pattern.CraftingPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PatternProviderHelper {
    public static final String NBT_PATTERN_PROVIDER = "CreateAutoCrafting.IsPatternProvider";
    public static final String NBT_PATTERNS = "CreateAutoCrafting.Patterns";
    public static final String NBT_OWNER = "CreateAutoCrafting.OwnerUUID";
    public static final String NBT_OWNER_NAME = "CreateAutoCrafting.OwnerName";
    public static final String NBT_CONVERTED_AT = "CreateAutoCrafting.ConversionTime";

    private PatternProviderHelper() {}

    /** Registry access used for the 1.21 ItemStack codec, derived from the packager's level. */
    private static net.minecraft.core.HolderLookup.Provider reg(PackagerBlockEntity packager) {
        return packager.getLevel().registryAccess();
    }

    public static boolean isPatternProvider(BlockEntity be) {
        return be instanceof PackagerBlockEntity
                && be.getPersistentData().getBoolean(NBT_PATTERN_PROVIDER);
    }

    public static void markPatternProvider(PackagerBlockEntity packager, UUID owner, String ownerName) {
        CompoundTag data = packager.getPersistentData();
        data.putBoolean(NBT_PATTERN_PROVIDER, true);
        data.putUUID(NBT_OWNER, owner);
        data.putString(NBT_OWNER_NAME, ownerName);
        data.putLong(NBT_CONVERTED_AT, System.currentTimeMillis());
        packager.setChanged();
    }

    @Nullable
    public static UUID getOwner(BlockEntity be) {
        if (!be.getPersistentData().hasUUID(NBT_OWNER)) {
            return null;
        }
        return be.getPersistentData().getUUID(NBT_OWNER);
    }

    @Nullable
    public static UUID getNetworkId(Level level, BlockPos packagerPos) {
        PackagerLinkBlockEntity link = findPackagerLink(level, packagerPos);
        if (link == null) {
            return null;
        }
        return link.behaviour != null ? link.behaviour.freqId : null;
    }

    @Nullable
    public static PackagerLinkBlockEntity findPackagerLink(Level level, BlockPos packagerPos) {
        for (Direction dir : Direction.values()) {
            BlockPos linkPos = packagerPos.relative(dir);
            BlockEntity be = level.getBlockEntity(linkPos);
            if (be instanceof PackagerLinkBlockEntity link) {
                PackagerBlockEntity packager = link.getPackager();
                if (packager != null && packager.getBlockPos().equals(packagerPos)) {
                    return link;
                }
            }
        }
        return null;
    }

    @Nullable
    public static LogisticallyLinkedBehaviour getLinkBehaviour(PackagerLinkBlockEntity link) {
        return link.behaviour;
    }

    // ---- blueprint patterns stored on the Pattern Provider --------------------

    /** Stores a copy of a configured Crafting Blueprint stack on the packager. */
    public static void addPattern(PackagerBlockEntity packager, ItemStack blueprint) {
        CompoundTag data = packager.getPersistentData();
        ListTag list = data.getList(NBT_PATTERNS, Tag.TAG_COMPOUND);
        list.add(blueprint.copyWithCount(1).save(reg(packager)));
        data.put(NBT_PATTERNS, list);
        packager.setChanged();
    }

    /** The blueprint stacks held by this Pattern Provider, in insertion order. */
    public static List<ItemStack> getPatternStacks(PackagerBlockEntity packager) {
        ListTag list = packager.getPersistentData().getList(NBT_PATTERNS, Tag.TAG_COMPOUND);
        List<ItemStack> out = new ArrayList<>(list.size());
        for (Tag tag : list) {
            out.add(ItemStack.parseOptional(reg(packager), (CompoundTag) tag));
        }
        return out;
    }

    public static int getPatternCount(PackagerBlockEntity packager) {
        return packager.getPersistentData().getList(NBT_PATTERNS, Tag.TAG_COMPOUND).size();
    }

    /** Removes and returns the blueprint at the given index, or empty if out of range. */
    public static ItemStack removePattern(PackagerBlockEntity packager, int index) {
        CompoundTag data = packager.getPersistentData();
        ListTag list = data.getList(NBT_PATTERNS, Tag.TAG_COMPOUND);
        if (index < 0 || index >= list.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = ItemStack.parseOptional(reg(packager), list.getCompound(index));
        list.remove(index);
        data.put(NBT_PATTERNS, list);
        packager.setChanged();
        return removed;
    }

    /** Replaces the provider's stored blueprints with the given list (used by the PRV menu). */
    public static void setPatterns(PackagerBlockEntity packager, List<ItemStack> blueprints) {
        ListTag list = new ListTag();
        for (ItemStack blueprint : blueprints) {
            if (!blueprint.isEmpty()) {
                list.add(blueprint.copyWithCount(1).save(reg(packager)));
            }
        }
        packager.getPersistentData().put(NBT_PATTERNS, list);
        packager.setChanged();
    }

    /** Decodes the provider's blueprints into resolved crafting patterns (skipping unconfigured). */
    public static List<CraftingPattern> getPatterns(PackagerBlockEntity packager) {
        List<CraftingPattern> out = new ArrayList<>();
        for (ItemStack blueprint : getPatternStacks(packager)) {
            ItemStack result = CraftingBlueprintItem.getResult(blueprint);
            if (result.isEmpty()) {
                continue;
            }
            out.add(new CraftingPattern(result, CraftingBlueprintItem.getIngredients(blueprint),
                    Math.max(1, result.getCount())));
        }
        return out;
    }

    /** Recomputes this provider's craftable patterns and publishes them to the network registry. */
    public static void refreshRegistry(Level level, BlockPos providerPos, PackagerBlockEntity packager) {
        PatternRegistry.update(providerPos, level.dimension(), getPatterns(packager));
    }

    /**
     * The package-port (Frogport/Postbox) address associated with this provider: scan the provider
     * position and its six neighbours for a package port and return its address filter. This is the
     * destination a craft request is sent to (e.g. a "Crafter" frogport sitting on the provider).
     */
    public static String resolveAddress(Level level, BlockPos providerPos) {
        BlockEntity here = level.getBlockEntity(providerPos);
        String found = addressOf(here);
        if (found != null) {
            return found;
        }
        for (Direction dir : Direction.values()) {
            found = addressOf(level.getBlockEntity(providerPos.relative(dir)));
            if (found != null) {
                return found;
            }
        }
        return "";
    }

    @Nullable
    private static String addressOf(@Nullable BlockEntity be) {
        if (be instanceof PackagePortBlockEntity port
                && port.addressFilter != null && !port.addressFilter.isBlank()) {
            return port.addressFilter;
        }
        return null;
    }
}

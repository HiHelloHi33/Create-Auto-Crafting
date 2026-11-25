package net.hihellohi.createautocrafting.blocks;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import net.hihellohi.createautocrafting.items.PatternCardItem;
import net.hihellohi.createautocrafting.manager.PatternRegistry;
import net.hihellohi.createautocrafting.pattern.CraftingPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A Packager that holds Pattern Cards. The cards live in {@link #patterns}; whenever they change (or
 * the block loads/unloads) the recipes they describe are published to / removed from the
 * {@link PatternRegistry}, which is what makes those items show up as craftable on the network.
 */
public class CraftingDistributorBlockEntity extends PackagerBlockEntity {
    public static final int PATTERN_SLOTS = 16;

    /** One card per slot; only configured Pattern Cards are accepted. */
    public final ItemStackHandler patterns = new ItemStackHandler(PATTERN_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return PatternCardItem.isConfigured(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            publishPatterns();
        }
    };

    public CraftingDistributorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CRAFTING_DISTRIBUTOR_BE.get(), pos, state);
    }

    // ---- pattern storage -----------------------------------------------------------------

    /** Puts a copy of the card into the first free slot. Returns false if full. */
    public boolean addPattern(ItemStack card) {
        for (int i = 0; i < PATTERN_SLOTS; i++) {
            if (patterns.getStackInSlot(i).isEmpty()) {
                patterns.setStackInSlot(i, card.copyWithCount(1));
                return true;
            }
        }
        return false;
    }

    /** The recipes of every card in this distributor. */
    public List<CraftingPattern> collectPatterns() {
        List<CraftingPattern> out = new ArrayList<>();
        for (int i = 0; i < PATTERN_SLOTS; i++) {
            ItemStack card = patterns.getStackInSlot(i);
            ItemStack result = PatternCardItem.getResult(card);
            if (!result.isEmpty()) {
                out.add(new CraftingPattern(result, PatternCardItem.getIngredients(card),
                        Math.max(1, result.getCount())));
            }
        }
        return out;
    }

    /** Server only: makes the registry match the cards currently inside. */
    public void publishPatterns() {
        if (level != null && !level.isClientSide) {
            PatternRegistry.update(GlobalPos.of(level.dimension(), worldPosition), collectPatterns());
        }
    }

    // ---- network helpers -----------------------------------------------------------------

    /** The Stock Link attached to this distributor, if any. */
    @Nullable
    public PackagerLinkBlockEntity findLink() {
        if (level == null) {
            return null;
        }
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(dir)) instanceof PackagerLinkBlockEntity link) {
                PackagerBlockEntity packager = link.getPackager();
                if (packager == this) {
                    return link;
                }
            }
        }
        return null;
    }

    /** The logistics network this distributor is linked to, or null if it has no Stock Link. */
    @Nullable
    public UUID getNetworkId() {
        PackagerLinkBlockEntity link = findLink();
        LogisticallyLinkedBehaviour behaviour = link == null ? null : link.behaviour;
        return behaviour == null ? null : behaviour.freqId;
    }

    /**
     * Where a craft request is sent: the address of a Frogport/Postbox on or next to this distributor
     * (e.g. a "Crafter" frogport), or "" if there is none.
     */
    public String resolveAddress() {
        if (level == null) {
            return "";
        }
        for (Direction dir : Direction.values()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(dir));
            if (be instanceof PackagePortBlockEntity port
                    && port.addressFilter != null && !port.addressFilter.isBlank()) {
                return port.addressFilter;
            }
        }
        return "";
    }

    // ---- lifecycle -----------------------------------------------------------------------

    @Override
    public void initialize() {
        super.initialize();
        publishPatterns();
    }

    /** Create calls this whenever the block entity is removed or its chunk unloads. */
    @Override
    public void invalidate() {
        super.invalidate();
        if (level != null && !level.isClientSide) {
            PatternRegistry.remove(GlobalPos.of(level.dimension(), worldPosition));
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level != null) {
            for (int i = 0; i < PATTERN_SLOTS; i++) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5, patterns.getStackInSlot(i));
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Patterns", patterns.serializeNBT(registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        patterns.deserializeNBT(registries, tag.getCompound("Patterns"));
    }
}

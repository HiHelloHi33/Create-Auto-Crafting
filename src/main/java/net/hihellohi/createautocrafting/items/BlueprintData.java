package net.hihellohi.createautocrafting.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The configured contents of a Crafting Blueprint, stored as a {@code DataComponent}
 * ({@link ModDataComponents#BLUEPRINT}). Replaces the 1.20.1 NBT-on-stack storage.
 *
 * <p>{@code grid} is always normalised to exactly 9 entries (recipe slots, in grid order;
 * empties allowed). {@code result} is the output stack (its count is the per-craft output count).
 */
public record BlueprintData(List<ItemStack> grid, ItemStack result, boolean locked) {

    public static final BlueprintData EMPTY =
            new BlueprintData(Collections.nCopies(9, ItemStack.EMPTY), ItemStack.EMPTY, false);

    public static final Codec<BlueprintData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("grid").forGetter(BlueprintData::grid),
            ItemStack.OPTIONAL_CODEC.fieldOf("result").forGetter(BlueprintData::result),
            Codec.BOOL.optionalFieldOf("locked", false).forGetter(BlueprintData::locked)
    ).apply(inst, BlueprintData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintData> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(9)), BlueprintData::grid,
            ItemStack.OPTIONAL_STREAM_CODEC, BlueprintData::result,
            ByteBufCodecs.BOOL, BlueprintData::locked,
            BlueprintData::new);

    /** Canonical constructor pads/truncates the grid to exactly 9 slots so callers never have to. */
    public BlueprintData {
        grid = normalise(grid);
    }

    private static List<ItemStack> normalise(List<ItemStack> in) {
        List<ItemStack> out = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            out.add(i < in.size() && in.get(i) != null ? in.get(i) : ItemStack.EMPTY);
        }
        return out;
    }

    public BlueprintData withLocked(boolean locked) {
        return new BlueprintData(grid, result, locked);
    }
}

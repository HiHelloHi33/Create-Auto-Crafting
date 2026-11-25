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
 * The recipe stored on a Pattern Card (the {@link ModDataComponents#PATTERN} component).
 *
 * <p>{@code grid} is always exactly {@value #GRID_SIZE} entries (3x3, row by row, empties allowed).
 * {@code result} is the output stack; its count is how many items one craft produces.
 */
public record PatternData(List<ItemStack> grid, ItemStack result, boolean locked) {
    public static final int GRID_SIZE = 9;

    public static final PatternData EMPTY =
            new PatternData(Collections.nCopies(GRID_SIZE, ItemStack.EMPTY), ItemStack.EMPTY, false);

    public static final Codec<PatternData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("grid").forGetter(PatternData::grid),
            ItemStack.OPTIONAL_CODEC.fieldOf("result").forGetter(PatternData::result),
            Codec.BOOL.optionalFieldOf("locked", false).forGetter(PatternData::locked)
    ).apply(inst, PatternData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PatternData> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(GRID_SIZE)), PatternData::grid,
            ItemStack.OPTIONAL_STREAM_CODEC, PatternData::result,
            ByteBufCodecs.BOOL, PatternData::locked,
            PatternData::new);

    /** Pads/truncates the grid to exactly 9 slots so callers never have to. */
    public PatternData {
        List<ItemStack> normalised = new ArrayList<>(GRID_SIZE);
        for (int i = 0; i < GRID_SIZE; i++) {
            normalised.add(i < grid.size() && grid.get(i) != null ? grid.get(i) : ItemStack.EMPTY);
        }
        grid = List.copyOf(normalised);
    }
}

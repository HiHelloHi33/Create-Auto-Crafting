package net.hihellohi.createautocrafting.blocks;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.BlockPos;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** A buffer of Forge Energy. Cables can only put energy in; the factory's controller draws it out. */
public class EnergyInputBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    public static final int CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 4_000;

    public final EnergyStorage storage = new EnergyStorage(CAPACITY, MAX_RECEIVE, Integer.MAX_VALUE) {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int received = super.receiveEnergy(toReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }
    };

    /** What the outside world sees: receive only. */
    public final IEnergyStorage external = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return storage.receiveEnergy(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return storage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return storage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    /** Last charge sent to clients, so the goggle HUD can show it. */
    private int syncedEnergy = -1;

    public EnergyInputBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ENERGY_INPUT_BE.get(), pos, state);
    }

    /** Pushes the charge to nearby clients (twice a second at most) when it changed. */
    public void serverTick() {
        if (level != null && level.getGameTime() % 10 == 0 && storage.getEnergyStored() != syncedEnergy) {
            syncedEnergy = storage.getEnergyStored();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** "Energy Input Info:" with the charge and capacity, in the same layout as Crafts & Additions' accumulators. */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        new LangBuilder("createautocrafting").add(Component.translatable("createautocrafting.tooltip.energy_input.info")
                .withStyle(ChatFormatting.WHITE)).forGoggles(tooltip);
        new LangBuilder("createautocrafting").add(Component.translatable("createautocrafting.tooltip.energy.stored")
                .withStyle(ChatFormatting.GRAY)).forGoggles(tooltip);
        new LangBuilder("createautocrafting").add(Component.literal(format(storage.getEnergyStored()))
                .append(" FE").withStyle(ChatFormatting.AQUA)).forGoggles(tooltip, 1);
        new LangBuilder("createautocrafting").add(Component.translatable("createautocrafting.tooltip.energy.capacity")
                .withStyle(ChatFormatting.GRAY)).forGoggles(tooltip);
        new LangBuilder("createautocrafting").add(Component.literal(format(storage.getMaxEnergyStored()))
                .append(" FE").withStyle(ChatFormatting.AQUA)).forGoggles(tooltip, 1);
        return true;
    }

    /** 1234 -> 1.2k, 2500000 -> 2.5M (like Crafts & Additions). */
    private static String format(int v) {
        if (v >= 1_000_000_000) {
            return Math.round(v / 1.0E8) / 10.0 + "G";
        }
        if (v >= 1_000_000) {
            return Math.round(v / 1.0E5) / 10.0 + "M";
        }
        if (v >= 1_000) {
            return Math.round(v / 100.0) / 10.0 + "k";
        }
        return Integer.toString(v);
    }

    /** Draws energy for the factory (returns how much was actually taken). */
    public int draw(int amount, boolean simulate) {
        int taken = storage.extractEnergy(amount, simulate);
        if (taken > 0 && !simulate) {
            setChanged();
        }
        return taken;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Energy", storage.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) {
            storage.deserializeNBT(registries, tag.get("Energy"));
        }
    }
}

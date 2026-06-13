package net.hihellohi.createautocrafting.blocks;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.hihellohi.createautocrafting.api.PatternProviderHelper;
import net.hihellohi.createautocrafting.integration.CreateLogisticsBridge;
import net.hihellohi.createautocrafting.integration.VirtualStockLink;
import net.hihellohi.createautocrafting.network.ModPackets;
import net.hihellohi.createautocrafting.network.VirtualStockSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class LogicCoilBlockEntity extends BlockEntity implements MenuProvider {
    private UUID ownerUUID;
    private String ownerName;
    private ItemStack requestedItem = ItemStack.EMPTY;
    private int requestedAmount;
    private int currentProgress;
    @Nullable
    private BlockPos linkedPackagerPos;
    @Nullable
    private UUID linkedNetworkId;
    private boolean isActive;

    private ItemStack clientDisplayItem = ItemStack.EMPTY;
    private int clientCurrentAmount;
    private int clientTargetAmount;

    public LogicCoilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.LOGIC_COIL_BE.get(), pos, state);
    }

    public void setOwner(Player player) {
        ownerUUID = player.getUUID();
        ownerName = player.getName().getString();
        setChanged();
    }

    public boolean canModify(Player player) {
        if (ownerUUID == null) {
            return true;
        }
        return player.getUUID().equals(ownerUUID) || player.hasPermissions(2);
    }

    public void requestCrafting(ItemStack item, int amount, ServerPlayer player) {
        if (level == null || level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (linkedPackagerPos == null) {
            linkedPackagerPos = findNearbyPatternProvider();
        }

        if (linkedPackagerPos == null) {
            return;
        }

        linkedNetworkId = PatternProviderHelper.getNetworkId(level, linkedPackagerPos);
        if (linkedNetworkId == null) {
            return;
        }

        requestedItem = item.copy();
        requestedAmount = amount;
        currentProgress = 0;
        isActive = true;

        VirtualStockLink stock = CreateLogisticsBridge.registerRequest(
                serverLevel, linkedPackagerPos, item, amount, player.getUUID());

        if (stock != null) {
            CreateLogisticsBridge.invalidateNetworkCache(stock.getNetworkId());
            setChanged();
            syncToClients(stock);
        }
    }

    @Nullable
    private BlockPos findNearbyPatternProvider() {
        if (level == null) {
            return null;
        }

        for (BlockPos checkPos : BlockPos.betweenClosed(
                worldPosition.offset(-8, -3, -8),
                worldPosition.offset(8, 3, 8))) {

            BlockEntity be = level.getBlockEntity(checkPos);
            if (be instanceof PackagerBlockEntity packager && PatternProviderHelper.isPatternProvider(packager)) {
                return checkPos.immutable();
            }
        }
        return null;
    }

    public void updateClientDisplay(ItemStack item, int current, int target) {
        clientDisplayItem = item;
        clientCurrentAmount = current;
        clientTargetAmount = target;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            net.hihellohi.createautocrafting.manager.LogicCoilRegistry.remove(level, worldPosition);
        }
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        // Advertise this coil so craft jobs can verify a coil is present on the network.
        net.hihellohi.createautocrafting.manager.LogicCoilRegistry.add(level, worldPosition);

        if (linkedPackagerPos == null || !isActive) {
            return;
        }

        VirtualStockLink stock = net.hihellohi.createautocrafting.manager.VirtualCraftingManager
                .getVirtualStock(linkedPackagerPos);

        if (stock != null) {
            currentProgress = stock.getCurrentAmount();
            if (stock.isFulfilled()) {
                onPatternComplete();
            } else if (level.getGameTime() % 20 == 0) {
                syncToClients(stock);
            }
        }
    }

    private void onPatternComplete() {
        isActive = false;
        currentProgress = requestedAmount;
        setChanged();
        if (linkedPackagerPos != null) {
            VirtualStockLink stock = net.hihellohi.createautocrafting.manager.VirtualCraftingManager
                    .getVirtualStock(linkedPackagerPos);
            if (stock != null) {
                syncToClients(stock);
            }
        }
    }

    private void syncToClients(VirtualStockLink stock) {
        if (level instanceof ServerLevel serverLevel) {
            VirtualStockSyncPacket packet = new VirtualStockSyncPacket(worldPosition, stock);
            ModPackets.sendToChunk(packet, serverLevel.getChunkAt(worldPosition));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
            tag.putString("OwnerName", ownerName);
        }
        if (linkedPackagerPos != null) {
            tag.putLong("LinkedPos", linkedPackagerPos.asLong());
        }
        if (linkedNetworkId != null) {
            tag.putUUID("LinkedNetwork", linkedNetworkId);
        }
        if (!requestedItem.isEmpty()) {
            tag.put("RequestedItem", requestedItem.save(registries));
            tag.putInt("RequestedAmount", requestedAmount);
            tag.putInt("CurrentProgress", currentProgress);
            tag.putBoolean("IsActive", isActive);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("OwnerUUID")) {
            ownerUUID = tag.getUUID("OwnerUUID");
            ownerName = tag.getString("OwnerName");
        }
        if (tag.contains("LinkedPos")) {
            linkedPackagerPos = BlockPos.of(tag.getLong("LinkedPos"));
        }
        if (tag.hasUUID("LinkedNetwork")) {
            linkedNetworkId = tag.getUUID("LinkedNetwork");
        }
        if (tag.contains("RequestedItem")) {
            requestedItem = ItemStack.parseOptional(registries, tag.getCompound("RequestedItem"));
            requestedAmount = tag.getInt("RequestedAmount");
            currentProgress = tag.getInt("CurrentProgress");
            isActive = tag.getBoolean("IsActive");
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.createautocrafting.logic_coil");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return null;
    }

    public ItemStack getClientDisplayItem() {
        return clientDisplayItem;
    }

    public int getClientCurrentAmount() {
        return clientCurrentAmount;
    }

    public int getClientTargetAmount() {
        return clientTargetAmount;
    }

    public boolean isActive() {
        return isActive;
    }
}

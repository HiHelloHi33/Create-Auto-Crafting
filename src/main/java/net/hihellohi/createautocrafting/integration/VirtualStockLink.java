package net.hihellohi.createautocrafting.integration;

import com.simibubi.create.content.logistics.packager.InventorySummary;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * A logic-side stock entry merged into Create's {@code LogisticsManager} summaries
 * so Stock Tickers and panels see coil-requested patterns as network stock.
 */
public class VirtualStockLink {
    private final UUID networkId;
    private final ItemStack item;
    private final int targetAmount;
    private final UUID requestingPlayer;
    private final long createdAt;
    private int currentAmount;
    private boolean fulfilled;

    public VirtualStockLink(UUID networkId, ItemStack item, int targetAmount, UUID requestingPlayer) {
        this.networkId = networkId;
        this.item = item.copy();
        this.item.setCount(1);
        this.targetAmount = targetAmount;
        this.requestingPlayer = requestingPlayer;
        this.currentAmount = 0;
        this.fulfilled = false;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public ItemStack getItem() {
        return item.copy();
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public int getCurrentAmount() {
        return currentAmount;
    }

    public synchronized void setCurrentAmount(int amount) {
        this.currentAmount = amount;
        this.fulfilled = currentAmount >= targetAmount;
    }

    public synchronized void addDelivered(int count) {
        setCurrentAmount(currentAmount + count);
    }

    public boolean isFulfilled() {
        return fulfilled;
    }

    public UUID getRequestingPlayer() {
        return requestingPlayer;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void mergeInto(InventorySummary summary) {
        if (!fulfilled && targetAmount > currentAmount) {
            summary.add(item, targetAmount - currentAmount);
        }
    }
}

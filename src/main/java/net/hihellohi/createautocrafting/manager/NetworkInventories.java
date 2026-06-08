package net.hihellohi.createautocrafting.manager;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Helpers for reading/extracting items across an entire Create logistics network, rather than from a
 * single packager. A network's packagers are reached through every {@link LogisticallyLinkedBehaviour}
 * registered on the frequency (each linked packager exposes the inventory it is attached to).
 */
public final class NetworkInventories {
    private NetworkInventories() {}

    /** Every packager currently linked on the given network frequency. */
    public static List<PackagerBlockEntity> packagers(ServerLevel level, UUID networkId) {
        List<PackagerBlockEntity> out = new ArrayList<>();
        if (networkId == null) {
            return out;
        }
        for (LogisticallyLinkedBehaviour behaviour : LogisticallyLinkedBehaviour.getAllPresent(networkId, false)) {
            if (behaviour.blockEntity instanceof PackagerLinkBlockEntity link) {
                PackagerBlockEntity packager = link.getPackager();
                if (packager != null && !out.contains(packager)) {
                    out.add(packager);
                }
            }
        }
        return out;
    }

    /** Total count of a matching item available across all the given packagers. */
    public static int count(List<PackagerBlockEntity> packagers, ItemStack match) {
        int total = 0;
        for (PackagerBlockEntity packager : packagers) {
            total += packager.getAvailableItems().getCountOf(match);
        }
        return total;
    }

    /**
     * Physically extracts up to {@code count} of a matching item from across the network's packager
     * inventories. Returns the extracted stacks (their summed count may be less than requested).
     */
    public static List<ItemStack> extract(List<PackagerBlockEntity> packagers, ItemStack match, int count) {
        List<ItemStack> got = new ArrayList<>();
        int remaining = count;
        for (PackagerBlockEntity packager : packagers) {
            if (remaining <= 0) {
                break;
            }
            if (packager.targetInventory == null) {
                continue;
            }
            ItemStack extracted = packager.targetInventory.extract(
                    ItemHelper.ExtractionCountMode.UPTO, remaining,
                    stack -> ItemStack.isSameItemSameTags(stack, match));
            if (extracted != null && !extracted.isEmpty()) {
                got.add(extracted);
                remaining -= extracted.getCount();
            }
        }
        return got;
    }

    public static int total(List<ItemStack> stacks) {
        int sum = 0;
        for (ItemStack stack : stacks) {
            sum += stack.getCount();
        }
        return sum;
    }
}

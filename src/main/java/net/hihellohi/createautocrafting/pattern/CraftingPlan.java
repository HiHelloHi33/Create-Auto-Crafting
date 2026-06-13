package net.hihellohi.createautocrafting.pattern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A computed crafting plan: a tree of items needed to make the requested result, with per-node
 * stock availability, plus overall gating flags. Serialised to the client to drive the preview.
 */
public final class CraftingPlan {
    public final CraftNode root;
    public final boolean satisfiable;  // every leaf has enough raw material
    public final int stepsNeeded;      // crafting operations required by the tree
    public final int coilSteps;        // crafting steps the network's logic coils provide

    public CraftingPlan(CraftNode root, boolean satisfiable, int stepsNeeded, int coilSteps) {
        this.root = root;
        this.satisfiable = satisfiable;
        this.stepsNeeded = stepsNeeded;
        this.coilSteps = coilSteps;
    }

    public boolean enoughCoils() {
        return coilSteps >= stepsNeeded;
    }

    /** How many more logic coils are required to start (0 if enough). */
    public int coilsShort() {
        if (enoughCoils()) {
            return 0;
        }
        int perCoil = Math.max(1, net.hihellohi.createautocrafting.manager.LogicCoilRegistry.STEPS_PER_COIL);
        return Math.max(1, (stepsNeeded - coilSteps + perCoil - 1) / perCoil);
    }

    public boolean canStart() {
        return satisfiable && enoughCoils();
    }

    public void write(RegistryFriendlyByteBuf buf) {
        root.write(buf);
        buf.writeBoolean(satisfiable);
        buf.writeVarInt(stepsNeeded);
        buf.writeVarInt(coilSteps);
    }

    public static CraftingPlan read(RegistryFriendlyByteBuf buf) {
        CraftNode root = CraftNode.read(buf, new int[]{0});
        return new CraftingPlan(root, buf.readBoolean(), buf.readVarInt(), buf.readVarInt());
    }

    /** One item in the plan tree. */
    public static final class CraftNode {
        private static final int MAX_NODES = 512;

        public final ItemStack stack;
        public final int needed;
        public final int available;
        public final boolean craftable;
        public final boolean satisfied;
        public final List<CraftNode> children;

        public CraftNode(ItemStack stack, int needed, int available, boolean craftable,
                         boolean satisfied, List<CraftNode> children) {
            this.stack = stack;
            this.needed = needed;
            this.available = available;
            this.craftable = craftable;
            this.satisfied = satisfied;
            this.children = children;
        }

        /** True when this node is a raw material the network is short on (drawn red). */
        public boolean isMissing() {
            return !craftable && available < needed;
        }

        public void write(RegistryFriendlyByteBuf buf) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
            buf.writeVarInt(needed);
            buf.writeVarInt(available);
            buf.writeBoolean(craftable);
            buf.writeBoolean(satisfied);
            buf.writeVarInt(children.size());
            for (CraftNode child : children) {
                child.write(buf);
            }
        }

        public static CraftNode read(RegistryFriendlyByteBuf buf, int[] budget) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            int needed = buf.readVarInt();
            int available = buf.readVarInt();
            boolean craftable = buf.readBoolean();
            boolean satisfied = buf.readBoolean();
            int childCount = buf.readVarInt();
            List<CraftNode> children = new ArrayList<>();
            for (int i = 0; i < childCount; i++) {
                if (budget[0]++ > MAX_NODES) {
                    // drain remaining without keeping, to keep the buffer consistent
                    CraftNode.read(buf, budget);
                    continue;
                }
                children.add(CraftNode.read(buf, budget));
            }
            return new CraftNode(stack, needed, available, craftable, satisfied, children);
        }
    }
}

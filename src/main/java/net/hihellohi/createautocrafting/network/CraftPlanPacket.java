package net.hihellohi.createautocrafting.network;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.client.ClientPacketHandlers;
import net.hihellohi.createautocrafting.pattern.CraftingPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: the computed plan for a requested craft. Opens the crafting-tree preview screen. */
public record CraftPlanPacket(BlockPos tickerPos, ItemStack result, int amount, CraftingPlan plan)
        implements CustomPacketPayload {

    public static final Type<CraftPlanPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, "craft_plan"));

    private static final StreamCodec<RegistryFriendlyByteBuf, CraftingPlan> PLAN_CODEC =
            StreamCodec.of((buf, plan) -> plan.write(buf), CraftingPlan::read);

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftPlanPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CraftPlanPacket::tickerPos,
                    ItemStack.OPTIONAL_STREAM_CODEC, CraftPlanPacket::result,
                    ByteBufCodecs.VAR_INT, CraftPlanPacket::amount,
                    PLAN_CODEC, CraftPlanPacket::plan,
                    CraftPlanPacket::new);

    @Override
    public Type<CraftPlanPacket> type() {
        return TYPE;
    }

    public static void handle(CraftPlanPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandlers.openCraftPlan(
                packet.tickerPos, packet.result, packet.amount, packet.plan));
    }
}

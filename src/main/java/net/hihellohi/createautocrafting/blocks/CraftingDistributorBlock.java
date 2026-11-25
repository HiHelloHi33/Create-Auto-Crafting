package net.hihellohi.createautocrafting.blocks;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.hihellohi.createautocrafting.items.PatternCardItem;
import net.hihellohi.createautocrafting.menu.CraftingDistributorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Packager that also stores Pattern Cards. It still packages, unpackages and links to a Stock Link
 * exactly like Create's Packager; right-clicking it opens its card screen instead of doing nothing.
 */
public class CraftingDistributorBlock extends PackagerBlock {

    public CraftingDistributorBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.TERRACOTTA_YELLOW)
                .strength(2.0f)
                .sound(SoundType.NETHERITE_BLOCK)
                .noOcclusion());
    }

    @Override
    public Class<PackagerBlockEntity> getBlockEntityClass() {
        return PackagerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PackagerBlockEntity> getBlockEntityType() {
        return ModBlocks.CRAFTING_DISTRIBUTOR_BE.get();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // Same hand-offs as Create's Packager: tools and attachable blocks keep working on us.
        if (AllItems.WRENCH.isIn(stack) || AllBlocks.FACTORY_GAUGE.isIn(stack)
                || AllBlocks.PACKAGE_FROGPORT.isIn(stack)
                || (AllBlocks.STOCK_LINK.isIn(stack) && !state.getValue(LINKED))) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (PackageItem.isPackage(stack)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit); // insert a package by hand
        }
        return open(level, pos, player) ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        return open(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private static boolean open(Level level, BlockPos pos, Player player) {
        if (player.isSecondaryUseActive()) {
            return false; // sneaking: let shift-click place blocks (e.g. a Stock Link) on it
        }
        if (level.isClientSide) {
            return true;
        }
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof CraftingDistributorBlockEntity be) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider((id, inv, p) -> new CraftingDistributorMenu(id, inv, be),
                            Component.translatable("block.createautocrafting.crafting_distributor")),
                    buf -> buf.writeBlockPos(pos));
            return true;
        }
        return false;
    }

    /** Sneak + right-click with a configured Pattern Card files it straight into the distributor. */
    public static boolean quickInsert(Level level, BlockPos pos, Player player, ItemStack held) {
        if (!(level.getBlockEntity(pos) instanceof CraftingDistributorBlockEntity be)
                || !PatternCardItem.isConfigured(held)) {
            return false;
        }
        if (!level.isClientSide && be.addPattern(held)) {
            if (!player.isCreative()) {
                held.shrink(1);
            }
        }
        return true;
    }
}

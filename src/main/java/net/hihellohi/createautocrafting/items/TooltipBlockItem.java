package net.hihellohi.createautocrafting.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** A block item that shows Create-style "Press [Shift]" description lines from the lang file. */
public class TooltipBlockItem extends BlockItem {
    private final String[] descriptionKeys;

    public TooltipBlockItem(Block block, Properties properties, String... descriptionKeys) {
        super(block, properties);
        this.descriptionKeys = descriptionKeys;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CACTooltips.appendShiftInfo(tooltip, descriptionKeys);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}

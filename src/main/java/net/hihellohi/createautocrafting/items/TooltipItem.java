package net.hihellohi.createautocrafting.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A plain item that shows Create-style "Press [Shift]" description lines from the lang file. */
public class TooltipItem extends Item {
    private final String[] descriptionKeys;

    public TooltipItem(Properties properties, String... descriptionKeys) {
        super(properties);
        this.descriptionKeys = descriptionKeys;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CACTooltips.appendShiftInfo(tooltip, descriptionKeys);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}

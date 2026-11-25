package net.hihellohi.createautocrafting.client;

import net.minecraft.network.chat.Component;

/**
 * Which entries the Stock Ticker screen shows. Remembered between openings of the screen (until the
 * game closes) so it behaves like a setting rather than something you re-pick every time.
 */
public enum CraftableFilter {
    BOTH("both"),
    CRAFTABLE("craftable"),
    NON_CRAFTABLE("non_craftable");

    private static CraftableFilter current = BOTH;

    private final String key;

    CraftableFilter(String key) {
        this.key = key;
    }

    public static CraftableFilter current() {
        return current;
    }

    /** Switches to the next filter and returns it. */
    public static CraftableFilter cycle() {
        CraftableFilter[] all = values();
        current = all[(current.ordinal() + 1) % all.length];
        return current;
    }

    public Component label() {
        return Component.translatable("gui.createautocrafting.filter." + key);
    }

    public Component description() {
        return Component.translatable("gui.createautocrafting.filter." + key + ".desc");
    }

    /** Whether an entry should be shown under this filter. */
    public boolean shows(boolean craftable) {
        return switch (this) {
            case BOTH -> true;
            case CRAFTABLE -> craftable;
            case NON_CRAFTABLE -> !craftable;
        };
    }
}

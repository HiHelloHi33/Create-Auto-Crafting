package net.hihellohi.createautocrafting.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Small hand-drawn-in-code icons in Create's brass style, so they need no texture files.
 * Everything is built from solid-colour pixels: a dark outline, a bright top-left edge, a darker
 * bottom-right edge and a brass fill — the same shading Create's brass items use.
 */
public final class BrassIcons {
    private static final int OUTLINE = 0xFF2E1F12;
    private static final int LIGHT = 0xFFFBEBB0;
    private static final int MID = 0xFFE2B552;
    private static final int DARK = 0xFFA66F2B;

    /** A chunky 7x7 plus: arms 3 pixels thick. {@code 1} = part of the plus. */
    private static final int[][] PLUS = {
            {0, 0, 1, 1, 1, 0, 0},
            {0, 0, 1, 1, 1, 0, 0},
            {1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1},
            {0, 0, 1, 1, 1, 0, 0},
            {0, 0, 1, 1, 1, 0, 0},
    };
    /** Total drawn size of {@link #plus}, outline included. */
    public static final int PLUS_SIZE = PLUS.length + 2;

    private BrassIcons() {}

    /** Draws the brass "+" with its top-left outline corner at (x, y). */
    public static void plus(GuiGraphics g, int x, int y) {
        int n = PLUS.length;
        for (int row = -1; row <= n; row++) {
            for (int col = -1; col <= n; col++) {
                boolean filled = at(row, col);
                int px = x + col + 1;
                int py = y + row + 1;
                if (!filled) {
                    // outline: empty pixel touching the plus (4-neighbourhood)
                    if (at(row - 1, col) || at(row + 1, col) || at(row, col - 1) || at(row, col + 1)) {
                        g.fill(px, py, px + 1, py + 1, OUTLINE);
                    }
                    continue;
                }
                int color = MID;
                if (!at(row - 1, col) || !at(row, col - 1)) {
                    color = LIGHT;                       // top / left edge catches the light
                }
                if (!at(row + 1, col) || !at(row, col + 1)) {
                    color = DARK;                        // bottom / right edge is in shade
                }
                g.fill(px, py, px + 1, py + 1, color);
            }
        }
    }

    private static boolean at(int row, int col) {
        return row >= 0 && col >= 0 && row < PLUS.length && col < PLUS.length && PLUS[row][col] == 1;
    }
}

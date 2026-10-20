package com.example.visuals.gui;

import net.minecraft.client.gui.DrawContext;

/** Rounded rectangles drawn with plain fills (no shaders), with 1px edge anti-aliasing. */
public final class RoundedRect {
    private RoundedRect() {}

    public static void fill(DrawContext c, int x, int y, int w, int h, int radius, int argb) {
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            c.fill(x, y, x + w, y + h, argb);
            return;
        }

        // Straight middle part in one call.
        c.fill(x, y + r, x + w, y + h - r, argb);

        for (int row = 0; row < r; row++) {
            double dy = r - row - 0.5;
            double dx = r - Math.sqrt(Math.max(0, r * r - dy * dy));
            int inset = (int) Math.ceil(dx);
            double coverage = inset - dx; // coverage of the outer edge pixel

            // top row + mirrored bottom row
            c.fill(x + inset, y + row, x + w - inset, y + row + 1, argb);
            c.fill(x + inset, y + h - 1 - row, x + w - inset, y + h - row, argb);

            if (inset > 0 && coverage > 0.05) {
                int edge = withAlphaFactor(argb, (float) coverage);
                c.fill(x + inset - 1, y + row, x + inset, y + row + 1, edge);
                c.fill(x + w - inset, y + row, x + w - inset + 1, y + row + 1, edge);
                c.fill(x + inset - 1, y + h - 1 - row, x + inset, y + h - row, edge);
                c.fill(x + w - inset, y + h - 1 - row, x + w - inset + 1, y + h - row, edge);
            }
        }
    }

    public static int withAlphaFactor(int argb, float factor) {
        int a = (int) (((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, factor)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}

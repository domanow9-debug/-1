package com.example.visuals.gui;

import com.example.visuals.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Visuals menu. Opens with Right Shift, fades/scales in smoothly, has rounded corners.
 * Right Shift or Esc closes it with the same animation played backwards.
 */
public class VisualsScreen extends Screen {
    // Layout
    private static final int PANEL_W = 260;
    private static final int HEADER_H = 36;
    private static final int ROW_H = 28;
    private static final int PAD = 10;
    private static final int RADIUS = 10;

    // Colors (ARGB)
    private static final int COL_PANEL = 0xF0161923;
    private static final int COL_HEADER = 0xFF1D212D;
    private static final int COL_ROW_HOVER = 0x26FFFFFF;
    private static final int COL_ACCENT = 0xFF7C6CF0;
    private static final int COL_OFF = 0xFF3A3F4D;
    private static final int COL_TEXT = 0xFFE8EAF0;
    private static final int COL_TEXT_DIM = 0xFF8A90A0;
    private static final int COL_KNOB = 0xFFFFFFFF;

    private static final float ANIM_SECONDS = 0.28f;

    private final List<Module> modules = Module.ALL;
    private final float[] toggleAnim = new float[modules.size()];

    private float progress = 0f;   // 0..1 linear time
    private boolean closing = false;
    private long lastNanos = System.nanoTime();

    public VisualsScreen() {
        super(Text.literal("Visuals"));
        for (int i = 0; i < toggleAnim.length; i++) {
            toggleAnim[i] = modules.get(i).enabled ? 1f : 0f;
        }
    }

    // ---- lifecycle ----

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Esc / close requests start the closing animation instead of closing instantly. */
    @Override
    public void close() {
        closing = true;
    }

    private void finishClose() {
        MinecraftClient.getInstance().setScreen(null);
    }

    // ---- animation helpers ----

    private static float easeOutCubic(float t) {
        float f = 1f - t;
        return 1f - f * f * f;
    }

    private void tick(float dt) {
        float step = dt / ANIM_SECONDS;
        progress = closing ? Math.max(0f, progress - step) : Math.min(1f, progress + step);
        if (closing && progress <= 0f) {
            finishClose();
        }
        for (int i = 0; i < toggleAnim.length; i++) {
            float target = modules.get(i).enabled ? 1f : 0f;
            float k = Math.min(1f, dt * 14f);
            toggleAnim[i] += (target - toggleAnim[i]) * k;
        }
    }

    // ---- geometry ----

    private int panelH() {
        return HEADER_H + modules.size() * ROW_H + PAD;
    }

    private int panelX() {
        return (this.width - PANEL_W) / 2;
    }

    private int panelY() {
        return (this.height - panelH()) / 2;
    }

    private float currentScale() {
        return 0.88f + 0.12f * easeOutCubic(progress);
    }

    /** Maps a screen-space mouse coordinate back into unscaled panel space. */
    private double unscale(double v, double center) {
        return center + (v - center) / currentScale();
    }

    // ---- rendering ----

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Own dim instead of vanilla blur, tied to the animation.
        float e = easeOutCubic(progress);
        context.fill(0, 0, this.width, this.height, RoundedRect.withAlphaFactor(0x90000000, e));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        tick(dt);

        renderBackground(context, mouseX, mouseY, delta);

        float e = easeOutCubic(progress);
        float scale = currentScale();

        int px = panelX();
        int py = panelY();
        int ph = panelH();
        double cx = px + PANEL_W / 2.0;
        double cy = py + ph / 2.0;

        context.getMatrices().push();
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-cx, -cy, 0);

        double mx = unscale(mouseX, cx);
        double my = unscale(mouseY, cy);

        // Panel + header
        RoundedRect.fill(context, px, py, PANEL_W, ph, RADIUS, RoundedRect.withAlphaFactor(COL_PANEL, e));
        RoundedRect.fill(context, px, py, PANEL_W, HEADER_H, RADIUS, RoundedRect.withAlphaFactor(COL_HEADER, e));
        // square off the header's bottom corners so it blends into the panel
        context.fill(px, py + HEADER_H - RADIUS, px + PANEL_W, py + HEADER_H, RoundedRect.withAlphaFactor(COL_HEADER, e));

        int textColor = RoundedRect.withAlphaFactor(COL_TEXT, e);
        int dimColor = RoundedRect.withAlphaFactor(COL_TEXT_DIM, e);

        if (e > 0.05f) {
            context.drawText(this.textRenderer, "Visuals", px + PAD + 2, py + (HEADER_H - 8) / 2, textColor, false);
            String hint = "RSHIFT";
            int hw = this.textRenderer.getWidth(hint);
            context.drawText(this.textRenderer, hint, px + PANEL_W - PAD - hw, py + (HEADER_H - 8) / 2, dimColor, false);
        }

        // Rows
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int rx = px + PAD / 2;
            int ry = py + HEADER_H + i * ROW_H;
            int rw = PANEL_W - PAD;
            int rh = ROW_H - 2;

            boolean hover = mx >= rx && mx < rx + rw && my >= ry && my < ry + rh;
            if (hover) {
                RoundedRect.fill(context, rx, ry, rw, rh, 6, RoundedRect.withAlphaFactor(COL_ROW_HOVER, e));
            }

            if (e > 0.05f) {
                context.drawText(this.textRenderer, m.name, rx + 8, ry + (rh - 8) / 2, textColor, false);
            }

            // Toggle pill with sliding knob
            int tw = 30;
            int th = 14;
            int tx = rx + rw - tw - 8;
            int ty = ry + (rh - th) / 2;
            float t = toggleAnim[i];
            int trackColor = lerpColor(COL_OFF, COL_ACCENT, t);
            RoundedRect.fill(context, tx, ty, tw, th, th / 2, RoundedRect.withAlphaFactor(trackColor, e));
            int knobSize = th - 4;
            int kx = tx + 2 + Math.round((tw - knobSize - 4) * t);
            RoundedRect.fill(context, kx, ty + 2, knobSize, knobSize, knobSize / 2, RoundedRect.withAlphaFactor(COL_KNOB, e));
        }

        context.getMatrices().pop();
    }

    private static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing || button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int px = panelX();
        int py = panelY();
        double cx = px + PANEL_W / 2.0;
        double cy = py + panelH() / 2.0;
        double mx = unscale(mouseX, cx);
        double my = unscale(mouseY, cy);

        for (int i = 0; i < modules.size(); i++) {
            int rx = px + PAD / 2;
            int ry = py + HEADER_H + i * ROW_H;
            int rw = PANEL_W - PAD;
            int rh = ROW_H - 2;
            if (mx >= rx && mx < rx + rw && my >= ry && my < ry + rh) {
                modules.get(i).toggle();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

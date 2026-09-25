package io.github.codefarmerfox.luminaserverlauncher.classes.tools;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public final class UI {

    public static final Color ACCENT = new Color(0.075f, 0.420f, 0.910f, 1f);
    public static final Color ACCENT_HOVER = new Color(0.105f, 0.490f, 1.0f, 1f);
    public static final Color BG_TOP = new Color(0.960f, 0.976f, 0.992f, 1f);
    public static final Color BG_BOTTOM = new Color(0.906f, 0.933f, 0.965f, 1f);
    public static final Color CARD = new Color(1f, 1f, 1f, 0.96f);
    public static final Color CARD_BORDER = new Color(0.85f, 0.88f, 0.92f, 1f);
    public static final Color SHADOW = new Color(0.55f, 0.60f, 0.68f, 0.28f);
    public static final Color TEXT_MAIN = new Color(0.10f, 0.13f, 0.19f, 1f);
    public static final Color TEXT_DIM = new Color(0.43f, 0.48f, 0.56f, 1f);
    public static final Color LIST_BG = new Color(0.965f, 0.972f, 0.982f, 1f);
    public static final Color LIST_HOVER = new Color(0.88f, 0.93f, 1.0f, 1f);
    public static final Color TRACK = new Color(0.88f, 0.90f, 0.94f, 1f);

    private UI() {
    }

    public static Texture white() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    public static void gradient(SpriteBatch batch, Texture white,
                                int width, int height, Color top, Color bottom, int steps) {
        Color tmp = new Color();
        for (int i = 0; i < steps; i++) {
            float t = steps <= 1 ? 0 : i / (float) (steps - 1);
            tmp.set(bottom).lerp(top, t);
            batch.setColor(tmp);
            float h = height / (float) steps;
            batch.draw(white, 0, i * h, width, h + 1);
        }
        batch.setColor(Color.WHITE);
    }

    public static boolean hovered(float x, float y, float w, float h) {
        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        return x <= mx && mx <= x + w && y <= my && my <= y + h;
    }

    public static boolean clicked(float x, float y, float w, float h) {
        return Gdx.input.justTouched() && hovered(x, y, w, h);
    }

    public static void roundedFilled(ShapeRenderer sr, float x, float y, float w, float h, float r) {
        r = Math.min(r, Math.min(w, h) / 2f);
        sr.rect(x + r, y, w - 2 * r, h);
        sr.rect(x, y + r, w, h - 2 * r);
        sr.circle(x + r, y + r, r);
        sr.circle(x + w - r, y + r, r);
        sr.circle(x + r, y + h - r, r);
        sr.circle(x + w - r, y + h - r, r);
    }

    public static void shadow(ShapeRenderer sr, float x, float y, float w, float h, float r) {
        sr.setColor(SHADOW);
        roundedFilled(sr, x - 4, y - 6, w + 8, h + 8, r + 2);
    }

    public static void card(ShapeRenderer sr, float x, float y, float w, float h) {
        shadow(sr, x, y, w, h, 14);
        sr.setColor(CARD);
        roundedFilled(sr, x, y, w, h, 14);
        sr.setColor(CARD_BORDER);
        sr.rect(x, y + h - 1, w, 1);
    }

    public static void buttonShape(ShapeRenderer sr, float x, float y, float w, float h, boolean hover, boolean primary) {
        if (primary) {
            sr.setColor(hover ? ACCENT_HOVER : ACCENT);
            roundedFilled(sr, x, y, w, h, 12);
        } else {
            sr.setColor(CARD);
            roundedFilled(sr, x, y, w, h, 12);
            sr.setColor(hover ? ACCENT : CARD_BORDER);
            sr.rect(x, y, 2, h);
        }
    }

    public static void buttonColor(ShapeRenderer sr, float x, float y, float w, float h,
                                   boolean hover, Color color, Color hoverColor) {
        sr.setColor(hover ? hoverColor : color);
        roundedFilled(sr, x, y, w, h, 12);
    }

    public static void progressBar(ShapeRenderer sr, float x, float y, float w, float h, float t) {
        sr.setColor(TRACK);
        roundedFilled(sr, x, y, w, h, h / 2f);
        if (t > 0) {
            float fw = Math.max(h, w * Math.min(1f, t));
            sr.setColor(ACCENT);
            roundedFilled(sr, x, y, fw, h, h / 2f);
        }
    }

    private static final GlyphLayout LAYOUT = new GlyphLayout();

    public static void text(BitmapFont font, SpriteBatch batch, String text, float centerX, float centerY) {
        text(font, batch, text, centerX, centerY, TEXT_MAIN);
    }

    public static void text(BitmapFont font, SpriteBatch batch, String text, float centerX, float centerY, Color color) {
        LAYOUT.setText(font, text);
        font.setColor(color);
        font.draw(batch, LAYOUT, centerX - LAYOUT.width / 2f, centerY + LAYOUT.height / 2f);
        font.setColor(Color.WHITE);
    }

    public static void textLeft(BitmapFont font, SpriteBatch batch, String text, float x, float centerY, Color color) {
        LAYOUT.setText(font, text);
        font.setColor(color);
        font.draw(batch, LAYOUT, x, centerY + LAYOUT.height / 2f);
        font.setColor(Color.WHITE);
    }

    public static float textWidth(BitmapFont font, String text) {
        LAYOUT.setText(font, text);
        return LAYOUT.width;
    }
}
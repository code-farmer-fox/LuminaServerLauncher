package io.github.codefarmerfox.luminaserverlauncher.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.GetFont;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.Input;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.UI;

public class NewNameInputScreen extends ScreenAdapter implements InputProcessor {

    private final App app;

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private Texture white;
    private BitmapFont titleFont;
    private BitmapFont fieldFont;
    private BitmapFont smallFont;

    private Input nameInput;
    private String error = null;

    public NewNameInputScreen(App app) {
        this.app = app;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        white = UI.white();
        try {
            GetFont.readContent();
            Color c = UI.TEXT_MAIN;
            titleFont = GetFont.getFont(GetFont.getParameter(38, c, true));
            fieldFont = GetFont.getFont(GetFont.getParameter(22, c));
            smallFont = GetFont.getFont(GetFont.getParameter(15, c));
        } catch (Exception e) {
            titleFont = new BitmapFont();
            fieldFont = new BitmapFont();
            smallFont = new BitmapFont();
        }
        nameInput = new Input(fieldFont)
                .setPlaceholder("")
                .setMaxLength(32);
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void render(float delta) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        float cardW = Math.min(560, w - 120);
        float cardH = 240;
        float cardX = (w - cardW) / 2f;
        float cardY = h / 2f - cardH / 2f;

        float fieldW = cardW - 80;
        float fieldH = 56;
        float fieldX = cardX + 40;
        float fieldY = cardY + 90;

        float btnW = 200;
        float btnH = 48;
        float btnX = w / 2f - btnW / 2f;
        boolean hoverNext = UI.hovered(btnX, 36, btnW, btnH);
        boolean hoverBack = UI.hovered(26, 26, 110, 38);

        if (UI.clicked(btnX, 36, btnW, btnH)) {
            submit();
            return;
        }
        if (UI.clicked(26, 26, 110, 38)) {
            app.setScreen(new MenuScreen(app));
            return;
        }

        nameInput.setBounds(fieldX, fieldY, fieldW, fieldH);
        nameInput.update(delta);

        // 1) 背景
        batch.begin();
        UI.gradient(batch, white, w, h, UI.BG_TOP, UI.BG_BOTTOM, 24);
        batch.end();

        // 2) 图形
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UI.card(shapes, cardX, cardY, cardW, cardH);
        nameInput.drawShapes(shapes);
        UI.buttonShape(shapes, btnX, 36, btnW, btnH, hoverNext, !nameInput.isEmpty());
        UI.buttonShape(shapes, 26, 26, 110, 38, hoverBack, false);
        shapes.end();

        // 3) 文字
        batch.begin();
        UI.text(titleFont, batch, "新建服务器", w / 2f, h - 90, UI.TEXT_MAIN);
        UI.text(smallFont, batch, "输入服务器名称 · 下一步选择版本",
                w / 2f, h - 124, UI.TEXT_DIM);

        nameInput.drawText(batch);

        if (error != null) {
            UI.text(smallFont, batch, error, w / 2f, cardY - 16,
                    new Color(0.86f, 0.20f, 0.16f, 1f));
        }

        UI.text(fieldFont, batch, "继续", w / 2f, 36 + btnH / 2f);
        UI.textLeft(smallFont, batch, "< 返回", 40, 45, hoverBack ? UI.ACCENT : UI.TEXT_DIM);
        batch.end();
    }

    private void submit() {
        String clean = nameInput.getSanitized();
        if (clean.isEmpty()) {
            error = "请输入有效名称";
            return;
        }
        app.setScreen(new VersionScreen(app, clean));
    }

    @Override public boolean keyDown(int keycode) {
        if (keycode == com.badlogic.gdx.Input.Keys.ENTER) {
            submit();
            return true;
        }
        if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
            app.setScreen(new MenuScreen(app));
            return true;
        }
        return nameInput.keyDown(keycode);
    }

    @Override public boolean keyTyped(char character) {
        if (nameInput.keyTyped(character)) {
            error = null;
            return true;
        }
        return false;
    }

    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean touchDown(int x, int y, int p, int b) { return false; }
    @Override public boolean touchUp(int x, int y, int p, int b) { return false; }
    @Override public boolean touchDragged(int x, int y, int p) { return false; }
    @Override public boolean mouseMoved(int x, int y) { return false; }
    @Override public boolean scrolled(float ax, float ay) { return false; }
    @Override public boolean touchCancelled(int x, int y, int p, int b) { return false; }

    private void release() {
        if (Gdx.input.getInputProcessor() == this) Gdx.input.setInputProcessor(null);
        if (batch != null) { batch.dispose(); batch = null; }
        if (shapes != null) { shapes.dispose(); shapes = null; }
        if (white != null) { white.dispose(); white = null; }
        if (titleFont != null) { titleFont.dispose(); titleFont = null; }
        if (fieldFont != null) { fieldFont.dispose(); fieldFont = null; }
        if (smallFont != null) { smallFont.dispose(); smallFont = null; }
    }

    @Override
    public void hide() {
        release();
    }

    @Override
    public void dispose() {
        release();
    }
}
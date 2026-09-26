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
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.UI;
import io.github.codefarmerfox.luminaserverlauncher.download.VanillaDownloader;

import java.util.ArrayList;
import java.util.List;

public class VersionScreen extends ScreenAdapter implements InputProcessor {

    private static final float ROW_H = 44f;

    private static final class Row {
        final String label;
        final String version;

        Row(String label, String version) {
            this.label = label;
            this.version = version;
        }
    }

    private final App app;
    private final String serverName;

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private Texture white;
    private BitmapFont titleFont;
    private BitmapFont bodyFont;
    private BitmapFont smallFont;

    private final List<Row> rows = new ArrayList<>();
    private boolean loading = true;
    private String error = null;
    private int scroll = 0;
    private int scrollMax = 0;
    private int visibleRows = 0;
    private float cardX = 0, cardW = 0, cardY = 0, cardH = 0;

    public VersionScreen(App app, String serverName) {
        this.app = app;
        this.serverName = serverName;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        white = UI.white();
        try {
            GetFont.readContent();
            Color c = UI.TEXT_MAIN;
            titleFont = GetFont.getFont(GetFont.getParameter(36, c, true));
            bodyFont = GetFont.getFont(GetFont.getParameter(20, c));
            smallFont = GetFont.getFont(GetFont.getParameter(15, c));
        } catch (Exception e) {
            titleFont = new BitmapFont();
            bodyFont = new BitmapFont();
            smallFont = new BitmapFont();
        }
        Gdx.input.setInputProcessor(this);
        loading = true;
        error = null;
        rows.clear();
        new Thread(this::loadVersions, "version-fetch").start();
    }

    private void loadVersions() {
        try {
            List<String> list = new VanillaDownloader().listRecentReleases(80);
            synchronized (rows) {
                rows.clear();
                rows.add(new Row("最新发布版（默认）", null));
                for (String v : list) rows.add(new Row(v, v));
            }
        } catch (Exception e) {
            synchronized (rows) {
                error = "版本获取失败: " + e.getMessage();
            }
        } finally {
            loading = false;
        }
    }

    @Override
    public void render(float delta) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        cardW = Math.min(520, w - 160);
        cardH = h - 320;
        cardX = (w - cardW) / 2f;
        cardY = 90;

        float topY = cardY + cardH - 60;
        visibleRows = Math.max(0, (int) ((cardH - 100) / ROW_H));
        scrollMax = Math.max(0, rows.size() - visibleRows);
        if (scroll > scrollMax) scroll = scrollMax;

        boolean hoverBack = UI.hovered(26, 26, 110, 38);
        if (UI.clicked(26, 26, 110, 38)) {
            app.setScreen(new MenuScreen(app));
            return;
        }

        batch.begin();
        UI.gradient(batch, white, w, h, UI.BG_TOP, UI.BG_BOTTOM, 24);
        batch.end();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UI.card(shapes, cardX, cardY, cardW, cardH);

        synchronized (rows) {
            for (int i = scroll; i < rows.size() && i < scroll + visibleRows; i++) {
                float ry = topY - (i - scroll) * ROW_H;
                float rowW = cardW - 24;
                boolean hover = UI.hovered(cardX + 12, ry - ROW_H + 4, rowW, ROW_H - 4);
                if (hover) {
                    shapes.setColor(UI.LIST_HOVER);
                    UI.roundedFilled(shapes, cardX + 12, ry - ROW_H + 4, rowW, ROW_H - 4, 6);
                }
            }
        }
        UI.buttonShape(shapes, 26, 26, 110, 38, hoverBack, false);
        shapes.end();

        batch.begin();
        UI.text(titleFont, batch, "选择版本", w / 2f, h - 90, UI.TEXT_MAIN);
        UI.text(smallFont, batch, "服务器: " + serverName + " · 滚轮浏览 · 点击版本下载",
                w / 2f, h - 124, UI.TEXT_DIM);

        synchronized (rows) {
            if (loading) {
                UI.text(bodyFont, batch, "加载中...", w / 2f, cardY + cardH / 2f, UI.TEXT_DIM);
            } else if (error != null) {
                UI.text(bodyFont, batch, error, w / 2f, cardY + cardH / 2f, UI.TEXT_DIM);
            } else if (rows.isEmpty()) {
                UI.text(bodyFont, batch, "暂无版本", w / 2f, cardY + cardH / 2f, UI.TEXT_DIM);
            } else {
                for (int i = scroll; i < rows.size() && i < scroll + visibleRows; i++) {
                    Row row = rows.get(i);
                    float ry = topY - (i - scroll) * ROW_H;
                    boolean latest = row.version == null;
                    UI.textLeft(bodyFont, batch, row.label, cardX + 28, ry - ROW_H / 2f,
                            latest ? UI.ACCENT : UI.TEXT_MAIN);
                    UI.textLeft(smallFont, batch, latest ? "默认" : "release",
                            cardX + cardW - 84, ry - ROW_H / 2f, UI.TEXT_DIM);
                }
            }
        }

        UI.textLeft(smallFont, batch, "< 返回", 40, 45, hoverBack ? UI.ACCENT : UI.TEXT_DIM);
        batch.end();
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        if (Gdx.input.justTouched()) {
            float topY = cardY + cardH - 60;
            synchronized (rows) {
                for (int i = scroll; i < rows.size() && i < scroll + visibleRows; i++) {
                    float ry = topY - (i - scroll) * ROW_H;
                    float rowW = cardW - 24;
                    if (UI.hovered(cardX + 12, ry - ROW_H + 4, rowW, ROW_H - 4)) {
                        Row row = rows.get(i);
                        app.setScreen(new DownloadScreen(app, serverName, row.version));
                        break;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        int step = amountY > 0 ? 1 : -1;
        scroll = Math.max(0, Math.min(scroll + step, scrollMax));
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
            app.setScreen(new MenuScreen(app));
            return true;
        }
        return false;
    }

    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char ch) { return false; }
    @Override public boolean touchUp(int x, int y, int p, int b) { return false; }
    @Override public boolean touchDragged(int x, int y, int p) { return false; }
    @Override public boolean mouseMoved(int x, int y) { return false; }
    @Override public boolean touchCancelled(int x, int y, int p, int b) { return false; }

    private void release() {
        if (Gdx.input.getInputProcessor() == this) Gdx.input.setInputProcessor(null);
        if (batch != null) { batch.dispose(); batch = null; }
        if (shapes != null) { shapes.dispose(); shapes = null; }
        if (white != null) { white.dispose(); white = null; }
        if (titleFont != null) { titleFont.dispose(); titleFont = null; }
        if (bodyFont != null) { bodyFont.dispose(); bodyFont = null; }
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
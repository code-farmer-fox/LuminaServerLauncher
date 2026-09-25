package io.github.codefarmerfox.luminaserverlauncher.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.GetFont;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.UI;
import io.github.codefarmerfox.luminaserverlauncher.download.VanillaDownloader;

import java.util.List;

public class VersionScreen extends ScreenAdapter implements InputProcessor {

    private static final int LIST_LIMIT = 24;
    private static final float ROW_H = 46;
    private static final float ROW_GAP = 6;

    private final App app;
    private final String serverName;

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private Texture white;
    private BitmapFont titleFont;
    private BitmapFont rowFont;
    private BitmapFont smallFont;

    private volatile List<String> versions = null;
    private volatile String error = null;
    private float scroll = 0f;
    private float wheelY = 0f;

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
            com.badlogic.gdx.graphics.Color c = UI.TEXT_MAIN;
            titleFont = GetFont.getFont(GetFont.getParameter(38, c, true));
            rowFont = GetFont.getFont(GetFont.getParameter(20, c));
            smallFont = GetFont.getFont(GetFont.getParameter(15, c));
        } catch (Exception e) {
            titleFont = new BitmapFont();
            rowFont = new BitmapFont();
            smallFont = new BitmapFont();
        }

        Thread worker = new Thread(() -> {
            try {
                versions = new VanillaDownloader().listRecentReleases(LIST_LIMIT);
            } catch (Exception e) {
                error = e.toString();
            }
        }, "version-list");
        worker.setDaemon(true);
        worker.start();
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void render(float delta) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        float cardW = Math.min(560, w - 120);
        float cardH = h - 200;
        float cardX = (w - cardW) / 2f;
        float cardY = 100;

        float maxScroll = (versions == null || versions.isEmpty()) ? 0
                : Math.max(0, versions.size() * (ROW_H + ROW_GAP) - (cardH - 60));
        scroll += wheelY * 1.4f;
        wheelY = 0f;
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        boolean hoverBack = UI.hovered(26, 26, 110, 38);
        if (UI.clicked(26, 26, 110, 38)) {
            app.setScreen(new MenuScreen(app));
            return;
        }

        int clickedIndex = -1;
        if (versions != null && Gdx.input.justTouched()) {
            for (int i = 0; i < versions.size(); i++) {
                float y = rowY(cardY, cardH, i);
                if (UI.hovered(cardX + 18, y, cardW - 36, ROW_H)) {
                    clickedIndex = i;
                    break;
                }
            }
        }
        if (clickedIndex >= 0) {
            app.setScreen(new DownloadScreen(app, serverName, versions.get(clickedIndex)));
            return;
        }

        batch.begin();
        UI.gradient(batch, white, w, h, UI.BG_TOP, UI.BG_BOTTOM, 24);
        batch.end();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UI.card(shapes, cardX, cardY, cardW, cardH);
        UI.buttonShape(shapes, 26, 26, 110, 38, hoverBack, false);
        if (versions != null) {
            float clipY = cardY + 26;
            float clipH = cardH - 52;
            for (int i = 0; i < versions.size(); i++) {
                float y = rowY(cardY, cardH, i);
                if (y + ROW_H < clipY || y > clipY + clipH) continue;
                boolean hover = UI.hovered(cardX + 18, y, cardW - 36, ROW_H);
                shapes.setColor(hover ? UI.LIST_HOVER : UI.LIST_BG);
                UI.roundedFilled(shapes, cardX + 18, y, cardW - 36, ROW_H, 8);
            }
        }
        shapes.end();

        batch.begin();
        UI.text(titleFont, batch, "Select Version", w / 2f, h - 90, UI.TEXT_MAIN);
        UI.text(smallFont, batch, "Server: " + serverName + "  ·  official vanilla releases", w / 2f, h - 124, UI.TEXT_DIM);
        UI.textLeft(smallFont, batch, "< Back", 40, 45, hoverBack ? UI.ACCENT : UI.TEXT_DIM);

        if (error != null) {
            UI.text(rowFont, batch, "Failed to load versions: " + error, w / 2f, h / 2f, UI.TEXT_DIM);
        } else if (versions == null) {
            UI.text(rowFont, batch, "Loading versions...", w / 2f, h / 2f, UI.TEXT_DIM);
        } else {
            float clipY = cardY + 26;
            float clipH = cardH - 52;
            for (int i = 0; i < versions.size(); i++) {
                String version = versions.get(i);
                float y = rowY(cardY, cardH, i);
                if (y + ROW_H < clipY || y > clipY + clipH) continue;
                UI.textLeft(rowFont, batch, version, cardX + 40, y + ROW_H / 2f, UI.TEXT_MAIN);
                if (i == 0) {
                    UI.textLeft(smallFont, batch, "latest", cardX + cardW - 90, y + ROW_H / 2f, UI.ACCENT);
                }
            }
        }
        batch.end();
    }

    private float rowY(float cardY, float cardH, int i) {
        return cardY + cardH - 58 - scroll - (i * (ROW_H + ROW_GAP)) - ROW_H;
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        wheelY += amountY;
        return true;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public void dispose() {
        if (Gdx.input.getInputProcessor() == this) {
            Gdx.input.setInputProcessor(null);
        }
        if (batch != null) batch.dispose();
        if (shapes != null) shapes.dispose();
        if (white != null) white.dispose();
        if (titleFont != null) titleFont.dispose();
        if (rowFont != null) rowFont.dispose();
        if (smallFont != null) smallFont.dispose();
    }
}
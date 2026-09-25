package io.github.codefarmerfox.luminaserverlauncher.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.GetFont;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.UI;

public class MenuScreen extends ScreenAdapter {

    private static final float BTN_W = 300;
    private static final float BTN_H = 54;
    private static final float BTN_GAP = 16;

    private final App app;

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private Texture white;
    private BitmapFont brandFont;
    private BitmapFont buttonFont;
    private BitmapFont smallFont;

    public MenuScreen(App app) {
        this.app = app;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        white = UI.white();
        try {
            GetFont.readContent();
            com.badlogic.gdx.graphics.Color c = UI.TEXT_MAIN;
            brandFont = GetFont.getFont(GetFont.getParameter(46, c, true));
            buttonFont = GetFont.getFont(GetFont.getParameter(22, c));
            smallFont = GetFont.getFont(GetFont.getParameter(16, c));
        } catch (Exception e) {
            brandFont = new BitmapFont();
            buttonFont = new BitmapFont();
            smallFont = new BitmapFont();
        }
    }

    @Override
    public void render(float delta) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        float centerX = w / 2f;
        float y1 = h / 2f - 30;
        float y2 = y1 - BTN_H - BTN_GAP;

        boolean hoverDownload = UI.hovered(centerX - BTN_W / 2f, y1, BTN_W, BTN_H);
        boolean hoverServers = UI.hovered(centerX - BTN_W / 2f, y2, BTN_W, BTN_H);

        if (UI.clicked(centerX - BTN_W / 2f, y1, BTN_W, BTN_H)) {
            app.setScreen(new VersionScreen(app, "vanilla"));
            return;
        }
        if (UI.clicked(centerX - BTN_W / 2f, y2, BTN_W, BTN_H)) {
            app.setScreen(new ServerScreen(app, "vanilla"));
            return;
        }

        batch.begin();
        UI.gradient(batch, white, w, h, UI.BG_TOP, UI.BG_BOTTOM, 24);
        batch.end();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UI.buttonShape(shapes, centerX - BTN_W / 2f, y1, BTN_W, BTN_H, hoverDownload, true);
        UI.buttonShape(shapes, centerX - BTN_W / 2f, y2, BTN_W, BTN_H, hoverServers, false);
        shapes.end();

        batch.begin();
        UI.text(brandFont, batch, "LuminaServerLauncher", centerX, h - 120, UI.TEXT_MAIN);
        UI.text(smallFont, batch, "Minecraft server manager, simplified", centerX, h - 152, UI.TEXT_DIM);
        UI.text(buttonFont, batch, "Download Vanilla Server", centerX, y1 + BTN_H / 2f);
        UI.text(buttonFont, batch, "Open Server", centerX, y2 + BTN_H / 2f);
        UI.text(smallFont, batch, "v0.2", centerX, 16, UI.TEXT_DIM);
        batch.end();
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapes != null) shapes.dispose();
        if (white != null) white.dispose();
        if (brandFont != null) brandFont.dispose();
        if (buttonFont != null) buttonFont.dispose();
        if (smallFont != null) smallFont.dispose();
    }
}
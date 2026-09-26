package io.github.codefarmerfox.luminaserverlauncher.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.GetFont;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.UI;
import io.github.codefarmerfox.luminaserverlauncher.config.Config;
import io.github.codefarmerfox.luminaserverlauncher.config.GetConfig;
import io.github.codefarmerfox.luminaserverlauncher.download.VanillaDownloader;

import java.nio.file.Path;

public class DownloadScreen extends ScreenAdapter {

    private final App app;
    private final String serverName;
    private final String version;

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private Texture white;
    private BitmapFont titleFont;
    private BitmapFont headingFont;
    private BitmapFont bodyFont;
    private BitmapFont smallFont;

    private volatile String resolved = "";
    private volatile int requiredJava = -1;
    private volatile long downloaded = 0;
    private volatile long total = -1;
    private volatile String donePath = null;
    private volatile String error = null;

    public DownloadScreen(App app) {
        this(app, "vanilla", null);
    }

    public DownloadScreen(App app, String serverName, String version) {
        this.app = app;
        this.serverName = serverName;
        this.version = version;
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
            headingFont = GetFont.getFont(GetFont.getParameter(22, c));
            bodyFont = GetFont.getFont(GetFont.getParameter(20, c));
            smallFont = GetFont.getFont(GetFont.getParameter(15, c));
        } catch (Exception e) {
            titleFont = new BitmapFont();
            headingFont = new BitmapFont();
            bodyFont = new BitmapFont();
            smallFont = new BitmapFont();
        }

        Thread worker = new Thread(this::runDownload, "vanilla-download");
        worker.setDaemon(true);
        worker.start();
    }

    private void runDownload() {
        try {
            VanillaDownloader downloader = new VanillaDownloader();
            Path jar = downloader.download(version, Path.of(VanillaDownloader.DEFAULT_SERVER_DIR), serverName,
                    new VanillaDownloader.Listener() {
                        @Override
                        public void onVersionResolved(String v) {
                            resolved = v;
                        }

                        @Override
                        public void onJavaVersion(int major) {
                            requiredJava = major;
                        }

                        @Override
                        public void onStart(long totalBytes) {
                            total = totalBytes;
                        }

                        @Override
                        public void onProgress(long downloadedBytes, long totalBytes) {
                            downloaded = downloadedBytes;
                            total = totalBytes;
                        }

                        @Override
                        public void onDone(Path serverJar, long totalBytes) {
                            donePath = serverJar.toString();
                        }
                    });
            if (donePath == null) {
                donePath = jar.toString();
            }
        } catch (Exception e) {
            error = e.toString();
        } finally {
            if (donePath != null && error == null) {
                Config config = app.getConfig();
                config.lastServer = serverName;
                GetConfig.set(config);
            }
        }
    }

    @Override
    public void render(float delta) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        boolean finished = donePath != null || error != null;
        boolean success = donePath != null && error == null;
        String btnLabel = error != null ? "返回" : finished && success ? "打开服务器" : "返回";

        float btnW = 200;
        float btnH = 48;
        boolean hoverBtn = UI.hovered(w / 2f - btnW / 2f, 36, btnW, btnH);

        if (UI.clicked(w / 2f - btnW / 2f, 36, btnW, btnH)) {
            if (success) {
                app.setScreen(new ServerScreen(app, serverName));
            } else {
                app.setScreen(new MenuScreen(app));
            }
            return;
        }

        float cardW = Math.min(640, w - 160);
        float cardH = 260;
        float cardX = (w - cardW) / 2f;
        float cardY = h / 2f - cardH / 2f - 10;

        float barW = cardW - 140;
        float barH = 14;
        float barX = (w - barW) / 2f;

        float progressT = 0f;
        if (error == null && total > 0) {
            progressT = downloaded / (float) total;
        }

        batch.begin();
        UI.gradient(batch, white, w, h, UI.BG_TOP, UI.BG_BOTTOM, 24);
        batch.end();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UI.card(shapes, cardX, cardY, cardW, cardH);
        if (error == null) {
            UI.progressBar(shapes, barX, cardY + 86, barW, barH, progressT);
        }
        UI.buttonShape(shapes, w / 2f - btnW / 2f, 36, btnW, btnH, hoverBtn, success);
        shapes.end();

        batch.begin();
        UI.text(titleFont, batch, "正在下载 " + serverName, w / 2f, h - 84, UI.TEXT_MAIN);
        String subtitle = version == null ? "最新版" : "版本 " + version;
        UI.text(smallFont, batch, subtitle, w / 2f, h - 118, UI.TEXT_DIM);

        UI.text(headingFont, batch, version == null ? resolved : version, w / 2f, cardY + cardH - 36, UI.TEXT_MAIN);

        String status;
        if (error != null) {
            status = "失败";
        } else if (donePath != null) {
            status = "完成";
        } else if (requiredJava > 0) {
            status = "需要 Java " + requiredJava;
        } else {
            status = "解析中...";
        }
        UI.text(bodyFont, batch, status, w / 2f, cardY + cardH - 76, error != null ? UI.TEXT_DIM : UI.TEXT_MAIN);

        if (error == null) {
            if (total > 0) {
                UI.text(bodyFont, batch,
                        String.format("%.0f%%   %.1f / %.1f MiB",
                                progressT * 100, downloaded / 1048576.0, total / 1048576.0),
                        w / 2f, cardY + 56);
            } else {
                UI.text(bodyFont, batch,
                        String.format("%.1f MiB downloaded", downloaded / 1048576.0),
                        w / 2f, cardY + 56);
            }
        } else {
            UI.text(smallFont, batch, error, w / 2f, cardY + 60, UI.TEXT_DIM);
        }

        if (donePath != null) {
            UI.text(smallFont, batch, donePath, w / 2f, cardY + 26, UI.TEXT_DIM);
        }

        UI.text(bodyFont, batch, btnLabel, w / 2f, 36 + btnH / 2f);
        batch.end();
    }

    private void release() {
        if (batch != null) { batch.dispose(); batch = null; }
        if (shapes != null) { shapes.dispose(); shapes = null; }
        if (white != null) { white.dispose(); white = null; }
        if (titleFont != null) { titleFont.dispose(); titleFont = null; }
        if (headingFont != null) { headingFont.dispose(); headingFont = null; }
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
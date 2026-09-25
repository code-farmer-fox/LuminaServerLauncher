package io.github.codefarmerfox.luminaserverlauncher.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.GetFont;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.UI;
import io.github.codefarmerfox.luminaserverlauncher.download.VanillaDownloader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ServerScreen extends ScreenAdapter {

    private static final Color DANGER = new Color(0.86f, 0.20f, 0.16f, 1f);
    private static final Color DANGER_HOVER = new Color(0.94f, 0.25f, 0.20f, 1f);
    private static final Color RUNNING = new Color(0.10f, 0.72f, 0.42f, 1f);
    private static final float PAD = 46;

    private final App app;
    private final String serverName;
    private final Path serverDir;

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private Texture white;
    private BitmapFont titleFont;
    private BitmapFont headingFont;
    private BitmapFont bodyFont;
    private BitmapFont smallFont;

    private Process process;
    private final List<String> logLines = new ArrayList<>();
    private float logPoll = 0f;
    private String launchError = null;

    private float cardX, cardW, lx;

    public ServerScreen(App app, String serverName) {
        this.app = app;
        this.serverName = serverName;
        this.serverDir = Path.of(VanillaDownloader.DEFAULT_SERVER_DIR, serverName);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        white = UI.white();
        try {
            GetFont.readContent();
            Color c = UI.TEXT_MAIN;
            titleFont = GetFont.getFont(GetFont.getParameter(40, c, true));
            headingFont = GetFont.getFont(GetFont.getParameter(16, c));
            bodyFont = GetFont.getFont(GetFont.getParameter(20, c));
            smallFont = GetFont.getFont(GetFont.getParameter(15, c));
        } catch (Exception e) {
            titleFont = new BitmapFont();
            headingFont = new BitmapFont();
            bodyFont = new BitmapFont();
            smallFont = new BitmapFont();
        }
        readLogTail();
    }

    private boolean hasJar() {
        return Files.exists(serverDir.resolve(VanillaDownloader.SERVER_JAR_NAME));
    }

    private boolean isRunning() {
        return process != null && process.isAlive();
    }

    private void readLogTail() {
        Path log = serverDir.resolve("logs/latest.log");
        if (!Files.exists(log)) return;
        try {
            List<String> all = Files.readAllLines(log, StandardCharsets.UTF_8);
            int from = Math.max(0, all.size() - 5);
            logLines.clear();
            logLines.addAll(all.subList(from, all.size()));
        } catch (IOException ignored) {
        }
    }

    private void toggle() {
        if (isRunning()) {
            process.destroy();
            Thread stopper = new Thread(() -> {
                try {
                    if (!process.waitFor(3, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                    }
                } catch (InterruptedException ignored) {
                }
            });
            stopper.setDaemon(true);
            stopper.start();
            process = null;
            launchError = null;
            return;
        }
        Path jar = serverDir.resolve(VanillaDownloader.SERVER_JAR_NAME);
        if (!Files.exists(jar)) {
            launchError = "server.jar missing - download it first";
            return;
        }
        try {
            Files.createDirectories(serverDir.resolve("logs"));
            ProcessBuilder pb = new ProcessBuilder("java", "-jar", jar.getFileName().toString(), "nogui")
                    .directory(serverDir.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(serverDir.resolve("logs/server.log").toFile()));
            process = pb.start();
            launchError = null;
            logLines.clear();
        } catch (IOException e) {
            launchError = "Failed to launch: " + e.getMessage();
        }
    }

    private void row(float y, String label, String value, Color valueColor) {
        UI.textLeft(headingFont, batch, label, lx, y, UI.TEXT_DIM);
        UI.textLeft(bodyFont, batch, value, lx + 260, y, valueColor);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    @Override
    public void render(float delta) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        boolean running = isRunning();

        if (logPoll <= 0) {
            readLogTail();
            logPoll = 0.5f;
        } else {
            logPoll -= delta;
        }

        cardW = Math.min(760, w - 120);
        float cardH = 330;
        cardX = (w - cardW) / 2f;
        float cardY = h / 2f - cardH / 2f;
        lx = cardX + PAD;

        float btnW = 220;
        float btnH = 50;
        float btnX = w / 2f - btnW / 2f;
        boolean hoverLaunch = UI.hovered(btnX, 36, btnW, btnH);
        boolean hoverBack = UI.hovered(26, 26, 110, 38);

        if (UI.clicked(btnX, 36, btnW, btnH)) {
            toggle();
            return;
        }
        if (UI.clicked(26, 26, 110, 38)) {
            if (running) process.destroy();
            app.setScreen(new MenuScreen(app));
            return;
        }

        batch.begin();
        UI.gradient(batch, white, w, h, UI.BG_TOP, UI.BG_BOTTOM, 24);
        batch.end();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UI.card(shapes, cardX, cardY, cardW, cardH);
        Color statusColor = running ? RUNNING : hasJar() ? UI.ACCENT : UI.TEXT_DIM;
        shapes.setColor(statusColor);
        shapes.circle(lx + 20, cardY + cardH - 44, 9);
        if (running) {
            UI.buttonColor(shapes, btnX, 36, btnW, btnH, hoverLaunch, DANGER, DANGER_HOVER);
        } else {
            UI.buttonShape(shapes, btnX, 36, btnW, btnH, hoverLaunch, hasJar());
        }
        UI.buttonShape(shapes, 26, 26, 110, 38, hoverBack, false);
        shapes.end();

        batch.begin();
        UI.textLeft(smallFont, batch, "< Back", 40, 45, hoverBack ? UI.ACCENT : UI.TEXT_DIM);
        UI.text(headingFont, batch, "Vanilla Server", w / 2f, h - 108, UI.TEXT_DIM);
        UI.text(titleFont, batch, serverName, w / 2f, h - 68, UI.TEXT_MAIN);

        float base = cardY + cardH - 52;
        String statusText = running ? "Running" : launchError != null ? "Error" : hasJar() ? "Ready" : "Missing";
        UI.textLeft(bodyFont, batch, statusText, lx + 46, base, statusColor);

        float y1 = base - 48;
        float y2 = y1 - 54;
        row(y1, "SERVER JAR", hasJar() ? "Ready" : "Missing", hasJar() ? UI.ACCENT : UI.TEXT_DIM);
        row(y2, "JAVA", "21+ required", UI.TEXT_MAIN);
        UI.textLeft(headingFont, batch, "DIRECTORY", lx, y2 - 46, UI.TEXT_DIM);
        UI.textLeft(smallFont, batch, serverDir.toString(), lx, y2 - 70, UI.TEXT_MAIN);

        UI.textLeft(headingFont, batch, "LOG", lx, cardY + 44, UI.TEXT_DIM);
        float logY = cardY + 26;
        for (String line : logLines) {
            UI.textLeft(smallFont, batch, truncate(line, 92), lx, logY, UI.TEXT_DIM);
            logY -= 20;
        }
        if (launchError != null) {
            UI.textLeft(bodyFont, batch, truncate(launchError, 92), lx, cardY + 44, DANGER);
        }

        UI.text(bodyFont, batch, running ? "Stop Server" : "Launch Server",
                w / 2f, 36 + btnH / 2f, running ? Color.WHITE : UI.TEXT_MAIN);
        UI.textLeft(smallFont, batch, "v0.2", w - 60, 16, UI.TEXT_DIM);
        batch.end();
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapes != null) shapes.dispose();
        if (white != null) white.dispose();
        if (titleFont != null) titleFont.dispose();
        if (headingFont != null) headingFont.dispose();
        if (bodyFont != null) bodyFont.dispose();
        if (smallFont != null) smallFont.dispose();
    }
}
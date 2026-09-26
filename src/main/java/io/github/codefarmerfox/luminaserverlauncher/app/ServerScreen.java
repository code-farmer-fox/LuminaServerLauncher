package io.github.codefarmerfox.luminaserverlauncher.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.codefarmerfox.luminaserverlauncher.app.serverscreen.ConsoleScreen;
import io.github.codefarmerfox.luminaserverlauncher.app.serverscreen.FileScreen;
import io.github.codefarmerfox.luminaserverlauncher.app.serverscreen.LaunchScreen;
import io.github.codefarmerfox.luminaserverlauncher.app.serverscreen.PropertiesScreen;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.GetFont;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.Input;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.UI;
import io.github.codefarmerfox.luminaserverlauncher.download.VanillaDownloader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ServerScreen extends ScreenAdapter implements InputProcessor {

    private static final Color TAB_ACTIVE = new Color(0.259f, 0.647f, 0.961f, 1f);
    private static final Color TAB_IDLE = new Color(0.16f, 0.18f, 0.24f, 1f);
    private static final Color TAB_HOVER = new Color(0.24f, 0.27f, 0.34f, 1f);
    private static final Color BACK_HOVER = new Color(0.90f, 0.92f, 0.96f, 1f);
    private static final Color BACK_TEXT = new Color(0.16f, 0.18f, 0.24f, 1f);
    private static final long STOP_WAIT_MS = 2000;

    public static final String[] TABS = {"服务器", "选项", "控制台", "文件"};
    public static final int TAB_SERVER = 0;
    public static final int TAB_OPTIONS = 1;
    public static final int TAB_CONSOLE = 2;
    public static final int TAB_FILES = 3;
    public static final float SIDEBAR_W = 180;
    public static final float TAB_H = 52;
    public static final float PAD = 46;

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
    private BitmapFont monoFont;
    private BitmapFont sideBodyFont;
    private BitmapFont sideSmallFont;

    private Process process;
    private OutputStream processIn;
    private final List<String> logLines = new ArrayList<>();
    private float logPoll = 0f;

    private int currentTab = TAB_SERVER;

    private final Input consoleInput;

    private float cardX, cardW, cardY, cardH, lx;

    private LaunchScreen launchScreen;
    private PropertiesScreen propertiesScreen;
    private ConsoleScreen consoleScreen;
    private FileScreen fileScreen;

    public ServerScreen(App app, String serverName) {
        this.app = app;
        this.serverName = serverName;
        this.serverDir = Path.of(VanillaDownloader.DEFAULT_SERVER_DIR, serverName);
        this.consoleInput = new Input(monoFont);
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
            headingFont = GetFont.getFont(GetFont.getParameter(16, c));
            bodyFont = GetFont.getFont(GetFont.getParameter(20, c));
            smallFont = GetFont.getFont(GetFont.getParameter(15, c));
            monoFont = GetFont.getFont(GetFont.getParameter(15, c));
            sideBodyFont = GetFont.getFont(GetFont.getParameter(20, Color.WHITE));
            sideSmallFont = GetFont.getFont(GetFont.getParameter(15, Color.WHITE));
        } catch (Exception e) {
            titleFont = new BitmapFont();
            headingFont = new BitmapFont();
            bodyFont = new BitmapFont();
            smallFont = new BitmapFont();
            monoFont = new BitmapFont();
            sideBodyFont = new BitmapFont();
            sideSmallFont = new BitmapFont();
        }
        consoleInput.setFont(monoFont)
                .setPlaceholder("输入命令后回车...")
                .setMaxLength(100)
                .setFilterEnabled(false)
                .setFocused(false);

        launchScreen = new LaunchScreen(this);
        propertiesScreen = new PropertiesScreen(this);
        consoleScreen = new ConsoleScreen(this);
        fileScreen = new FileScreen(this);

        launchScreen.show();
        Gdx.input.setInputProcessor(this);
    }

    public App app() { return app; }
    public String serverName() { return serverName; }
    public Path serverDir() { return serverDir; }
    public SpriteBatch batch() { return batch; }
    public ShapeRenderer shapes() { return shapes; }
    public BitmapFont titleFont() { return titleFont; }
    public BitmapFont headingFont() { return headingFont; }
    public BitmapFont bodyFont() { return bodyFont; }
    public BitmapFont smallFont() { return smallFont; }
    public BitmapFont monoFont() { return monoFont; }
    public Input consoleInput() { return consoleInput; }
    public List<String> logLines() { return logLines; }
    public float cardX() { return cardX; }
    public float cardW() { return cardW; }
    public float cardY() { return cardY; }
    public float cardH() { return cardH; }
    public float lx() { return lx; }
    public int currentTab() { return currentTab; }
    public void setCurrentTab(int tab) { this.currentTab = tab; }

    public boolean hasJar() {
        return Files.exists(serverDir.resolve(VanillaDownloader.SERVER_JAR_NAME));
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    public void launch() {
        Path jar = serverDir.resolve(VanillaDownloader.SERVER_JAR_NAME);
        if (!Files.exists(jar)) {
            launchScreen.setLaunchError("缺少 server.jar，请先在主菜单下载");
            return;
        }
        try {
            Files.createDirectories(serverDir.resolve("logs"));
            ProcessBuilder pb = new ProcessBuilder("java", "-jar", jar.getFileName().toString(), "nogui")
                    .directory(serverDir.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(serverDir.resolve("logs/server.log").toFile()));
            process = pb.start();
            processIn = process.getOutputStream();
            launchScreen.setLaunchError(null);
            logLines.clear();
            currentTab = TAB_CONSOLE;
            consoleInput.clear();
            consoleInput.setFocused(true);
            consoleScreen.refreshTail();
        } catch (IOException e) {
            launchScreen.setLaunchError("启动失败: " + e.getMessage());
        }
    }

    public void stopServerBlocking() {
        Process p = process;
        process = null;
        OutputStream in = processIn;
        processIn = null;
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
        if (p == null || !p.isAlive()) return;
        p.destroy();
        Thread killer = new Thread(() -> {
            try {
                if (!p.waitFor(STOP_WAIT_MS, TimeUnit.MILLISECONDS)) {
                    p.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "server-stop");
        killer.setDaemon(true);
        killer.start();
    }

    public void sendCommand(String cmd) {
        if (processIn == null || !isRunning()) return;
        try {
            processIn.write((cmd + "\n").getBytes(StandardCharsets.UTF_8));
            processIn.flush();
        } catch (IOException ignored) {
        }
    }

    public void goBack() {
        stopServerBlocking();
        app.setScreen(new MenuScreen(app));
    }

    @Override
    public void render(float delta) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        boolean running = isRunning();

        if (logPoll <= 0) {
            logPoll = 0.5f;
            if (currentTab == TAB_SERVER) launchScreen.refreshTail();
            else if (currentTab == TAB_CONSOLE) consoleScreen.refreshTail();
        } else {
            logPoll -= delta;
        }

        cardX = SIDEBAR_W + 24;
        cardW = w - cardX - 40;
        cardY = 60;
        cardH = h - 140;
        lx = cardX + PAD * 0.6f;

        consoleInput.setBounds(lx, cardY + 24, cardW - PAD * 1.2f, 44);
        consoleInput.setFocused(currentTab == TAB_CONSOLE && running);
        consoleInput.update(delta);

        handleInput(w, h, running);

        batch.begin();
        UI.gradient(batch, white, w, h, UI.BG_TOP, UI.BG_BOTTOM, 24);
        batch.end();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(TAB_IDLE);
        shapes.rect(0, 0, SIDEBAR_W, h);

        for (int i = 0; i < TABS.length; i++) {
            float ty = h - 100 - i * (TAB_H + 8);
            boolean active = (i == currentTab);
            boolean disabled = (i == TAB_CONSOLE && !hasJar() && !running);
            boolean hover = UI.hovered(0, ty, SIDEBAR_W, TAB_H) && !disabled;
            if (active) shapes.setColor(TAB_ACTIVE);
            else if (hover) shapes.setColor(TAB_HOVER);
            else shapes.setColor(TAB_IDLE);
            shapes.rect(0, ty, SIDEBAR_W, TAB_H);
            if (active) {
                shapes.setColor(Color.WHITE);
                shapes.rect(0, ty, 4, TAB_H);
            }
        }

        UI.card(shapes, cardX, cardY, cardW, cardH);

        if (currentTab == TAB_SERVER) launchScreen.drawShapes(running);
        else if (currentTab == TAB_CONSOLE) consoleScreen.drawShapes(running);
        else if (currentTab == TAB_FILES) fileScreen.drawShapes();

        float backW = 110, backH = 38, backX = 26, backY = 26;
        boolean hoverBack = UI.hovered(backX, backY, backW, backH);
        shapes.setColor(hoverBack ? BACK_HOVER : Color.WHITE);
        UI.roundedFilled(shapes, backX, backY, backW, backH, 8);

        shapes.end();

        batch.begin();
        UI.textLeft(sideSmallFont, batch, "服务器管理", 20, h - 28, Color.WHITE);
        for (int i = 0; i < TABS.length; i++) {
            float ty = h - 100 - i * (TAB_H + 8);
            UI.textLeft(sideBodyFont, batch, TABS[i], 28, ty + TAB_H / 2f, Color.WHITE);
        }
        UI.textLeft(smallFont, batch, "< 返回", 40, 45, BACK_TEXT);

        UI.text(titleFont, batch, serverName, cardX + 24, h - 70, UI.TEXT_MAIN);

        if (currentTab == TAB_SERVER) launchScreen.drawText(running, delta);
        else if (currentTab == TAB_OPTIONS) propertiesScreen.drawText();
        else if (currentTab == TAB_CONSOLE) consoleScreen.drawText(running);
        else if (currentTab == TAB_FILES) fileScreen.drawText();

        batch.end();
    }

    private void handleInput(int w, int h, boolean running) {
        for (int i = 0; i < TABS.length; i++) {
            float ty = h - 100 - i * (TAB_H + 8);
            if (UI.clicked(0, ty, SIDEBAR_W, TAB_H)) {
                if (i == TAB_CONSOLE && !hasJar() && !running) return;
                currentTab = i;
                if (i == TAB_FILES) fileScreen.refreshFiles();
                return;
            }
        }
        if (UI.clicked(0, 0, SIDEBAR_W, 60)) {
            goBack();
            return;
        }

        if (currentTab == TAB_SERVER) launchScreen.handleInput(running);
        else if (currentTab == TAB_FILES) fileScreen.handleInput();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (currentTab == TAB_SERVER) return launchScreen.keyDown(keycode);
        if (currentTab == TAB_CONSOLE) return consoleScreen.keyDown(keycode);
        return false;
    }

    @Override
    public boolean keyTyped(char ch) {
        if (currentTab == TAB_SERVER) return launchScreen.keyTyped(ch);
        if (currentTab == TAB_CONSOLE) return consoleScreen.keyTyped(ch);
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
        stopServerBlocking();
        if (Gdx.input.getInputProcessor() == this) Gdx.input.setInputProcessor(null);
        if (batch != null) { batch.dispose(); batch = null; }
        if (shapes != null) { shapes.dispose(); shapes = null; }
        if (white != null) { white.dispose(); white = null; }
        if (titleFont != null) { titleFont.dispose(); titleFont = null; }
        if (headingFont != null) { headingFont.dispose(); headingFont = null; }
        if (bodyFont != null) { bodyFont.dispose(); bodyFont = null; }
        if (smallFont != null) { smallFont.dispose(); smallFont = null; }
        if (monoFont != null) { monoFont.dispose(); monoFont = null; }
        if (sideBodyFont != null) { sideBodyFont.dispose(); sideBodyFont = null; }
        if (sideSmallFont != null) { sideSmallFont.dispose(); sideSmallFont = null; }
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
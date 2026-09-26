package io.github.codefarmerfox.luminaserverlauncher.app.serverscreen;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import io.github.codefarmerfox.luminaserverlauncher.app.ServerScreen;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.EULA;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.Input;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.UI;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LaunchScreen {

    private static final long MAX_TAIL_BYTES = 16 * 1024;
    private static final Color DANGER = new Color(0.86f, 0.20f, 0.16f, 1f);
    private static final Color DANGER_HOVER = new Color(0.94f, 0.25f, 0.20f, 1f);
    private static final Color RUNNING = new Color(0.10f, 0.72f, 0.42f, 1f);

    private final ServerScreen host;
    private final Input motdInput;
    private final List<String> logLines = new ArrayList<>();

    private String motd = "";
    private boolean motdEditing = false;
    private String motdSavedFlash = null;
    private float flashTimer = 0f;
    private String eulaFlash = null;
    private float eulaFlashTimer = 0f;
    private String launchError = null;

    public LaunchScreen(ServerScreen host) {
        this.host = host;
        this.motdInput = new Input(host.bodyFont()).setMaxLength(80).setFocused(false);
    }

    public void show() {
        loadMotd();
    }

    public void setLaunchError(String err) { this.launchError = err; }

    private Path propsPath() {
        return host.serverDir().resolve("server.properties");
    }

    private void loadMotd() {
        Path p = propsPath();
        motd = "";
        if (!Files.exists(p)) return;
        try {
            for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                if (line.startsWith("motd=")) { motd = line.substring(5); break; }
            }
        } catch (IOException ignored) {
        }
    }

    private void saveMotd(String value) {
        Path p = propsPath();
        try {
            List<String> lines = Files.exists(p)
                    ? new ArrayList<>(Files.readAllLines(p, StandardCharsets.UTF_8))
                    : new ArrayList<>();
            boolean replaced = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("motd=")) {
                    lines.set(i, "motd=" + value);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) lines.add("motd=" + value);
            Files.write(p, lines, StandardCharsets.UTF_8);
            motd = value;
            motdSavedFlash = "MOTD 已保存";
            flashTimer = 2f;
        } catch (IOException e) {
            motdSavedFlash = "保存失败: " + e.getMessage();
            flashTimer = 3f;
        }
    }

    public void refreshTail() {
        Path log = host.serverDir().resolve("logs/latest.log");
        if (!Files.exists(log)) return;
        try (RandomAccessFile raf = new RandomAccessFile(log.toFile(), "r")) {
            long len = raf.length();
            if (len <= 0) return;
            long start = Math.max(0, len - MAX_TAIL_BYTES);
            raf.seek(start);
            int size = (int) (len - start);
            byte[] buf = new byte[size];
            raf.readFully(buf);
            String text = new String(buf, StandardCharsets.UTF_8);
            String[] lines = text.split("\r?\n");
            int from = Math.max(0, lines.length - 5);
            logLines.clear();
            for (int i = from; i < lines.length; i++) logLines.add(lines[i]);
        } catch (IOException ignored) {
        }
    }

    private float launchBtnY() { return host.cardY() + 40; }
    private float eulaBtnY() { return launchBtnY() + 70; }
    private float editY() { return host.cardY() + host.cardH() - 200; }

    public void handleInput(boolean running) {
        float cardX = host.cardX(), cardW = host.cardW();
        float btnW = 220, btnH = 50;
        float btnX = cardX + (cardW - btnW) / 2f;

        if (UI.clicked(btnX, launchBtnY(), btnW, btnH)) {
            if (running) {
                host.stopServerBlocking();
                host.consoleInput().setFocused(false);
            } else {
                host.launch();
            }
            return;
        }
        if (UI.clicked(btnX, eulaBtnY(), btnW, btnH)) {
            try {
                EULA.agree(host.serverDir());
                eulaFlash = "已同意 EULA";
                eulaFlashTimer = 2f;
            } catch (IOException e) {
                eulaFlash = "写入失败: " + e.getMessage();
                eulaFlashTimer = 3f;
            }
            return;
        }

        float editW = 120, editH = 40;
        if (motdEditing) {
            if (UI.clicked(cardX + cardW - 280, editY(), editW, editH)) {
                saveMotd(motdInput.getText());
                motdEditing = false;
                motdInput.setFocused(false);
            } else if (UI.clicked(cardX + cardW - 150, editY(), editW, editH)) {
                motdEditing = false;
                motdInput.setFocused(false);
            }
        } else {
            if (UI.clicked(cardX + cardW - 280, editY(), editW, editH)) {
                motdInput.setText(motd);
                motdInput.setFocused(true);
                motdEditing = true;
            }
        }
    }

    public void drawShapes(boolean running) {
        float cardX = host.cardX(), cardW = host.cardW();
        float btnW = 220, btnH = 50;
        float btnX = cardX + (cardW - btnW) / 2f;
        boolean hoverLaunch = UI.hovered(btnX, launchBtnY(), btnW, btnH);
        if (running) {
            UI.buttonColor(host.shapes(), btnX, launchBtnY(), btnW, btnH, hoverLaunch, DANGER, DANGER_HOVER);
        } else {
            UI.buttonShape(host.shapes(), btnX, launchBtnY(), btnW, btnH, hoverLaunch, host.hasJar());
        }

        boolean hoverEula = UI.hovered(btnX, eulaBtnY(), btnW, btnH);
        UI.buttonShape(host.shapes(), btnX, eulaBtnY(), btnW, btnH, hoverEula, true);

        Color statusColor = running ? RUNNING : host.hasJar() ? UI.ACCENT : UI.TEXT_DIM;
        host.shapes().setColor(statusColor);
        host.shapes().circle(host.lx() + 12, host.cardY() + host.cardH() - 44, 8);

        motdInput.setBounds(host.lx(), editY() - 8, cardW - ServerScreen.PAD * 1.2f, 52);
        motdInput.drawShapes(host.shapes());
    }

    public void drawText(boolean running, float delta) {
        if (flashTimer > 0) flashTimer -= delta; else motdSavedFlash = null;
        if (eulaFlashTimer > 0) eulaFlashTimer -= delta; else eulaFlash = null;

        float cardX = host.cardX(), cardW = host.cardW();
        float base = host.cardY() + host.cardH() - 48;
        Color statusColor = running ? RUNNING : host.hasJar() ? UI.ACCENT : UI.TEXT_DIM;
        String statusText = running ? "运行中" : launchError != null ? "错误" : host.hasJar() ? "就绪" : "缺失";
        UI.textLeft(host.bodyFont(), host.batch(), statusText, host.lx() + 32, base, statusColor);

        float y = base - 50;
        UI.textLeft(host.headingFont(), host.batch(), "服务端核心", host.lx(), y, UI.TEXT_DIM);
        UI.textLeft(host.bodyFont(), host.batch(), host.hasJar() ? "就绪" : "缺失", host.lx() + 200, y,
                host.hasJar() ? UI.ACCENT : UI.TEXT_DIM);

        y -= 40;
        UI.textLeft(host.headingFont(), host.batch(), "Java", host.lx(), y, UI.TEXT_DIM);
        UI.textLeft(host.bodyFont(), host.batch(), "需要 21+", host.lx() + 200, y, UI.TEXT_MAIN);

        for (String line : logLines) {
            y -= 20;
            UI.textLeft(host.monoFont(), host.batch(), truncate(line, 100), host.lx(), y, UI.TEXT_DIM);
        }

        float editW = 120, editH = 40;
        if (motdEditing) {
            UI.text(host.bodyFont(), host.batch(), "保存", cardX + cardW - 280 + editW / 2f, editY() + editH / 2f + 6);
            UI.text(host.bodyFont(), host.batch(), "取消", cardX + cardW - 150 + editW / 2f, editY() + editH / 2f + 6);
        } else {
            UI.text(host.bodyFont(), host.batch(), "编辑", cardX + cardW - 280 + editW / 2f, editY() + editH / 2f + 6);
        }

        if (motdSavedFlash != null) {
            UI.textLeft(host.smallFont(), host.batch(), motdSavedFlash, host.lx(), editY() - 28, UI.ACCENT);
        }
        if (launchError != null) {
            UI.textLeft(host.smallFont(), host.batch(), truncate(launchError, 70), host.lx(), launchBtnY() + 70, DANGER);
        }

        UI.text(host.bodyFont(), host.batch(), running ? "停止服务器" : "启动服务器",
                cardX + cardW / 2f, launchBtnY() + 50 / 2f + 6,
                running ? Color.WHITE : UI.TEXT_MAIN);

        UI.text(host.bodyFont(), host.batch(), "同意协议",
                cardX + cardW / 2f, eulaBtnY() + 50 / 2f + 6, UI.TEXT_MAIN);

        if (eulaFlash != null) {
            UI.textLeft(host.smallFont(), host.batch(), eulaFlash, host.lx(), eulaBtnY() + 70, UI.ACCENT);
        }

        motdInput.drawText(host.batch());
    }

    public boolean keyDown(int keycode) {
        if (motdEditing) {
            if (keycode == Keys.ENTER) {
                saveMotd(motdInput.getText());
                motdEditing = false;
                motdInput.setFocused(false);
                return true;
            }
            if (keycode == Keys.ESCAPE) {
                motdEditing = false;
                motdInput.setFocused(false);
                return true;
            }
            return motdInput.keyDown(keycode);
        }
        return false;
    }

    public boolean keyTyped(char ch) {
        if (motdEditing) return motdInput.keyTyped(ch);
        return false;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
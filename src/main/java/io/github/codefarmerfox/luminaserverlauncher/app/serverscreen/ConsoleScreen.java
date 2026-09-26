package io.github.codefarmerfox.luminaserverlauncher.app.serverscreen;

import com.badlogic.gdx.Input.Keys;
import io.github.codefarmerfox.luminaserverlauncher.app.ServerScreen;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.Input;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.UI;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConsoleScreen {

    private static final long MAX_TAIL_BYTES = 16 * 1024;

    private final ServerScreen host;
    private final List<String> logLines = new ArrayList<>();

    public ConsoleScreen(ServerScreen host) {
        this.host = host;
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
            int from = Math.max(0, lines.length - 24);
            logLines.clear();
            for (int i = from; i < lines.length; i++) logLines.add(lines[i]);
        } catch (IOException ignored) {
        }
    }

    public void drawShapes(boolean running) {
        host.consoleInput().drawShapes(host.shapes());
    }

    public void drawText(boolean running) {
        float lx = host.lx();
        float y = host.cardY() + host.cardH() - 40;
        UI.textLeft(host.headingFont(), host.batch(), "控制台", lx, y, UI.TEXT_DIM);
        y -= 36;

        float inputY = host.cardY() + 24;
        for (String line : logLines) {
            if (y < inputY + 60) break;
            UI.textLeft(host.monoFont(), host.batch(), truncate(line, 100), lx, y, UI.TEXT_DIM);
            y -= 20;
        }

        if (running) {
            host.consoleInput().drawText(host.batch());
        } else {
            UI.textLeft(host.smallFont(), host.batch(), "服务器未运行", lx + 14, inputY + 28, UI.TEXT_DIM);
        }
    }

    public boolean keyDown(int keycode) {
        Input in = host.consoleInput();
        if (keycode == Keys.ENTER) {
            String cmd = in.getText().trim();
            if (!cmd.isEmpty()) host.sendCommand(cmd);
            in.clear();
            return true;
        }
        return in.keyDown(keycode);
    }

    public boolean keyTyped(char ch) {
        if (host.isRunning()) return host.consoleInput().keyTyped(ch);
        return false;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
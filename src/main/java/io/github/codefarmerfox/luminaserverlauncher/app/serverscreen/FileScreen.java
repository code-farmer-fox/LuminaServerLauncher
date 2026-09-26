package io.github.codefarmerfox.luminaserverlauncher.app.serverscreen;

import io.github.codefarmerfox.luminaserverlauncher.app.ServerScreen;
import io.github.codefarmerfox.luminaserverlauncher.classes.tools.UI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FileScreen {

    private final ServerScreen host;
    private Path browseDir;
    private List<Path> fileEntries = new ArrayList<>();
    private int filesScroll = 0;

    public FileScreen(ServerScreen host) {
        this.host = host;
        this.browseDir = host.serverDir();
    }

    public void refreshFiles() {
        fileEntries.clear();
        if (!Files.isDirectory(browseDir)) return;
        try (var stream = Files.list(browseDir)) {
            fileEntries = stream
                    .sorted(Comparator
                            .comparing((Path p) -> !Files.isDirectory(p))
                            .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                    .collect(Collectors.toList());
        } catch (IOException ignored) {
        }
    }

    public void handleInput() {
        float cardX = host.cardX(), cardW = host.cardW(), cardY = host.cardY(), cardH = host.cardH();
        float lx = host.lx();
        float y = cardY + cardH - 70;
        for (int i = filesScroll; i < fileEntries.size(); i++) {
            if (y < cardY + 20) break;
            if (UI.clicked(lx, y - 26, cardW - ServerScreen.PAD, 34)) {
                Path p = fileEntries.get(i);
                if (Files.isDirectory(p)) {
                    browseDir = p;
                    filesScroll = 0;
                    refreshFiles();
                }
                return;
            }
            y -= 34;
        }
        if (UI.clicked(cardX + 24, cardY + cardH - 46, 80, 30)) {
            Path parent = browseDir.getParent();
            if (parent != null && parent.startsWith(host.serverDir())) {
                browseDir = parent;
                filesScroll = 0;
                refreshFiles();
            }
        }
    }

    public void drawShapes() {
        float cardX = host.cardX(), cardW = host.cardW(), cardY = host.cardY(), cardH = host.cardH();
        float lx = host.lx();
        float y = cardY + cardH - 70;
        for (int i = filesScroll; i < fileEntries.size(); i++) {
            if (y < cardY + 20) break;
            boolean hover = UI.hovered(lx, y - 26, cardW - ServerScreen.PAD, 34);
            if (hover) {
                host.shapes().setColor(UI.LIST_HOVER);
                UI.roundedFilled(host.shapes(), lx, y - 26, cardW - ServerScreen.PAD, 34, 6);
            }
            y -= 34;
        }
    }

    public void drawText() {
        float cardX = host.cardX(), cardW = host.cardW(), cardY = host.cardY(), cardH = host.cardH();
        float lx = host.lx();
        float y = cardY + cardH - 40;
        UI.textLeft(host.headingFont(), host.batch(), "文件", lx, y, UI.TEXT_DIM);

        String rel = host.serverDir().relativize(browseDir).toString();
        UI.textLeft(host.smallFont(), host.batch(),
                "/" + host.serverName() + (rel.isEmpty() ? "" : "/" + rel),
                lx + 80, y, UI.TEXT_DIM);

        if (!browseDir.equals(host.serverDir())) {
            UI.textLeft(host.smallFont(), host.batch(), "< 上级", cardX + 34, cardY + cardH - 36, UI.ACCENT);
        }

        y -= 34;
        if (fileEntries.isEmpty()) {
            UI.textLeft(host.bodyFont(), host.batch(), "(空目录)", lx, y, UI.TEXT_DIM);
            return;
        }
        for (int i = filesScroll; i < fileEntries.size(); i++) {
            if (y < cardY + 20) break;
            Path p = fileEntries.get(i);
            String name = p.getFileName().toString();
            boolean dir = Files.isDirectory(p);
            String prefix = dir ? "[DIR] " : "      ";
            UI.textLeft(host.smallFont(), host.batch(), prefix + name, lx + 8, y,
                    dir ? UI.ACCENT : UI.TEXT_MAIN);
            if (!dir) {
                try {
                    UI.textLeft(host.smallFont(), host.batch(), humanSize(Files.size(p)),
                            cardX + cardW - 160, y, UI.TEXT_DIM);
                } catch (IOException ignored) {
                }
            }
            y -= 34;
        }
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KiB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MiB", bytes / 1048576.0);
        return String.format("%.1f GiB", bytes / 1073741824.0);
    }
}
package io.github.codefarmerfox.luminaserverlauncher.classes.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EULA {

    public static void agree(Path serverDir) throws IOException {
        Files.createDirectories(serverDir);
        Files.writeString(serverDir.resolve("eula.txt"), "eula=true", StandardCharsets.UTF_8);
    }
}
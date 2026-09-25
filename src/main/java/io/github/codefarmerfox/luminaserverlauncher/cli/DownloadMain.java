package io.github.codefarmerfox.luminaserverlauncher.cli;

import io.github.codefarmerfox.luminaserverlauncher.download.VanillaDownloader;

import java.nio.file.Path;

public final class DownloadMain {

    private static final String USAGE =
            "Usage: java -jar LuminaServerLauncher.jar [serverName] [version]\n"
                    + "  serverName  directory under servers/ (default: vanilla)\n"
                    + "  version     Minecraft version, e.g. 1.21.11 (default: latest release)";

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && ("-h".equals(args[0]) || "--help".equals(args[0]))) {
            System.out.println(USAGE);
            return;
        }
        String serverName = args.length > 0 ? args[0] : "vanilla";
        String version = args.length > 1 ? args[1] : null;
        Path serversRoot = Path.of(VanillaDownloader.DEFAULT_SERVER_DIR);

        System.out.println(version == null
                ? "Resolving latest release version..."
                : "Using version " + version);
        VanillaDownloader downloader = new VanillaDownloader();

        long[] lastPrintedPercent = {-1};
        int[] requiredJava = {21};
        VanillaDownloader.Listener listener = new VanillaDownloader.Listener() {
            @Override
            public void onVersionResolved(String version) {
                System.out.println("Version: " + version);
            }

            @Override
            public void onJavaVersion(int majorVersion) {
                requiredJava[0] = majorVersion;
                int running = Runtime.version().feature();
                System.out.println("Requires Java " + majorVersion
                        + " (running Java " + running + ")");
                if (majorVersion > running) {
                    System.out.println("WARNING: this Java is too old to run the server, use Java "
                            + majorVersion + "+");
                }
            }

            @Override
            public void onStart(long totalBytes) {
                System.out.println("Downloading server.jar ("
                        + (totalBytes > 0 ? totalBytes / (1024 * 1024) + " MiB" : "unknown size") + ")...");
            }

            @Override
            public void onProgress(long downloadedBytes, long totalBytes) {
                if (totalBytes <= 0) {
                    System.out.printf("  %.1f MiB%n", downloadedBytes / 1048576.0);
                    return;
                }
                long percent = downloadedBytes * 100 / totalBytes;
                if (percent != lastPrintedPercent[0]) {
                    lastPrintedPercent[0] = percent;
                    System.out.printf("  %3d%%  %.1f / %.1f MiB%n", percent,
                            downloadedBytes / 1048576.0, totalBytes / 1048576.0);
                }
            }

            @Override
            public void onDone(Path serverJar, long totalBytes) {
                System.out.println("Done: " + serverJar.toAbsolutePath()
                        + " (" + totalBytes / (1024 * 1024) + " MiB)");
            }
        };

        Path jar = downloader.download(version, serversRoot, serverName, listener);
        System.out.println("SHA-1 verified: " + jar);
        if (requiredJava[0] > Runtime.version().feature()) {
            System.out.println("NOT runnable with current Java - download a version requiring Java "
                    + Runtime.version().feature() + " or older");
        }
    }
}

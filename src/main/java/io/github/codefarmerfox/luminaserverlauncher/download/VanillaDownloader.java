package io.github.codefarmerfox.luminaserverlauncher.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public final class VanillaDownloader {

    public static final String VERSION_MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    public static final String DEFAULT_SERVER_DIR = "servers";
    public static final String SERVER_JAR_NAME = "server.jar";

    private static final int BUFFER_SIZE = 128 * 1024;
    private static final long PROGRESS_STEP = 4L * 1024 * 1024;

    public interface Listener {
        void onVersionResolved(String version);

        void onJavaVersion(int majorVersion);

        void onStart(long totalBytes);

        void onProgress(long downloadedBytes, long totalBytes);

        void onDone(Path serverJar, long totalBytes);
    }

    public static final Listener NOOP_LISTENER = new Listener() {
        @Override
        public void onVersionResolved(String version) {
        }

        @Override
        public void onJavaVersion(int majorVersion) {
        }

        @Override
        public void onStart(long totalBytes) {
        }

        @Override
        public void onProgress(long downloadedBytes, long totalBytes) {
        }

        @Override
        public void onDone(Path serverJar, long totalBytes) {
        }
    };

    private final HttpClient client;

    public VanillaDownloader() {
        this(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(30))
                .build());
    }

    public VanillaDownloader(HttpClient client) {
        this.client = client;
    }

    public Path downloadLatestRelease(Path serversRoot, String serverName, Listener listener)
            throws IOException, InterruptedException {
        return download(null, serversRoot, serverName, listener);
    }

    public Path download(String version, Path serversRoot, String serverName, Listener listener)
            throws IOException, InterruptedException {
        String target = (version == null || version.isBlank())
                ? resolveLatestReleaseVersion()
                : version.trim();
        listener.onVersionResolved(target);
        return downloadServerJar(target, serversRoot, serverName, listener);
    }

    public String resolveLatestReleaseVersion() throws IOException, InterruptedException {
        JsonObject manifest = JsonParser.parseString(getText(VERSION_MANIFEST_URL)).getAsJsonObject();
        String latestRelease = manifest.getAsJsonObject("latest").get("release").getAsString();

        String versionUrl = null;
        JsonArray versions = manifest.getAsJsonArray("versions");
        for (JsonElement element : versions) {
            JsonObject entry = element.getAsJsonObject();
            if (latestRelease.equals(entry.get("id").getAsString())
                    && "release".equals(entry.get("type").getAsString())) {
                versionUrl = entry.get("url").getAsString();
                break;
            }
        }
        if (versionUrl == null) {
            throw new IOException("No release entry found for version " + latestRelease);
        }
        return latestRelease;
    }

    public List<String> listRecentReleases(int limit) throws IOException, InterruptedException {
        JsonObject manifest = JsonParser.parseString(getText(VERSION_MANIFEST_URL)).getAsJsonObject();
        String latestRelease = manifest.getAsJsonObject("latest").get("release").getAsString();
        List<String> result = new ArrayList<>();
        JsonArray versions = manifest.getAsJsonArray("versions");
        for (JsonElement element : versions) {
            JsonObject entry = element.getAsJsonObject();
            if ("release".equals(entry.get("type").getAsString())) {
                result.add(entry.get("id").getAsString());
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        if (!result.isEmpty() && !result.get(0).equals(latestRelease)) {
            int index = result.indexOf(latestRelease);
            if (index > 0) {
                result.remove(index);
                result.add(0, latestRelease);
            }
        }
        return result;
    }

    public Path downloadServerJar(String version, Path serversRoot, String serverName, Listener listener)
            throws IOException, InterruptedException {
        String versionJson = getText(resolveVersionUrl(version));
        JsonObject versionData = JsonParser.parseString(versionJson).getAsJsonObject();
        if (versionData.has("javaVersion")) {
            listener.onJavaVersion(
                    versionData.getAsJsonObject("javaVersion").get("majorVersion").getAsInt());
        }
        JsonObject server = versionData.getAsJsonObject("downloads").getAsJsonObject("server");
        String downloadUrl = server.get("url").getAsString();
        String expectedSha1 = server.get("sha1").getAsString();

        Path targetDir = serversRoot.resolve(serverName);
        Files.createDirectories(targetDir);
        Path target = targetDir.resolve(SERVER_JAR_NAME);
        Path temp = targetDir.resolve(SERVER_JAR_NAME + ".part");

        long total = downloadToFile(downloadUrl, temp, listener);
        String actualSha1 = sha1(temp);
        if (!actualSha1.equalsIgnoreCase(expectedSha1)) {
            Files.deleteIfExists(temp);
            throw new IOException("SHA-1 mismatch for " + version + ": expected " + expectedSha1
                    + " but got " + actualSha1);
        }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        listener.onDone(target, total);
        return target;
    }

    private String resolveVersionUrl(String version) throws IOException, InterruptedException {
        JsonObject manifest = JsonParser.parseString(getText(VERSION_MANIFEST_URL)).getAsJsonObject();
        JsonArray versions = manifest.getAsJsonArray("versions");
        for (JsonElement element : versions) {
            JsonObject entry = element.getAsJsonObject();
            if (version.equals(entry.get("id").getAsString())) {
                return entry.get("url").getAsString();
            }
        }
        throw new IOException("Unknown Minecraft version: " + version);
    }

    private long downloadToFile(String url, Path target, Listener listener)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("User-Agent", "LuminaServerLauncher")
                .GET()
                .build();
        HttpResponse<InputStream> response =
                client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Download failed with HTTP " + response.statusCode() + " for " + url);
        }

        long total = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        listener.onStart(total);

        long downloaded = 0;
        long lastReported = 0;
        try (InputStream in = response.body();
             OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                downloaded += read;
                if (downloaded - lastReported >= PROGRESS_STEP) {
                    lastReported = downloaded;
                    listener.onProgress(downloaded, total);
                }
            }
        }
        listener.onProgress(downloaded, total);
        return downloaded;
    }

    private String getText(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "LuminaServerLauncher")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Request failed with HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }

    private static String sha1(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 unavailable", e);
        }
    }
}

# LuminaServerLauncher
LuminaServerLauncher is a Minecraft Server Manager.It starts the server using a VPN.It can download server, manage server properties, manage tier and launch server.

## Features

- Launch Minecraft server
- Download Minecraft server
- Manage tier
- Manage server properties
- Manage JVM properties

## Directory Layout

```
LuminaServerLauncher.jar/      - App jar
    assets/                    - App assets
    com/                       - Libs (com)
    io.github.codefarmerfox/   - Source code package
        lwjgl3/                - Lwjgl3 files
        luminaserverlauncher/  - Source code
    javazoom/                  - Libs (javazoom)
    linux/                     - Libs (linux)
    macos/                     - Libs (macos)
    META-INF/                  - Manifest
    org/                       - Libs (org)
    windows/                   - Libs (windows)
    .dll                       - Windows linklibs
    .dylib                     - macOS linklibs
    .so                        - linux linklibs
assets/                        - User assets
    config.json                - Config
servers/                       - Minecraft servers
```

## Requirements

- Java (21+)

## Run

```
java -jar LuminaServerLauncher.jar
```



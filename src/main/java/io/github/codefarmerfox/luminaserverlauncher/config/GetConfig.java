package io.github.codefarmerfox.luminaserverlauncher.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.files.FileHandle;

public class GetConfig {

    private static final String CONFIG_PATH = "assets/config.json";
    private static Json json = new Json();

    public static Config get() {
        FileHandle fileConfig = Gdx.files.local(CONFIG_PATH);
        if (!fileConfig.exists()) {
            Config config = new Config();
            return set(config);
        }
        return json.fromJson(Config.class, fileConfig);
    }
    public static Config set(Config config) {
        json.toJson(config, Gdx.files.local(CONFIG_PATH));
        return get();
    }
}

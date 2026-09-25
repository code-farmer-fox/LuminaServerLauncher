package io.github.codefarmerfox.luminaserverlauncher.app;

import com.badlogic.gdx.*;
import io.github.codefarmerfox.luminaserverlauncher.config.*;

public class App extends Game {

    private Config config;

    @Override
    public void create() {
        config = GetConfig.get();
        setScreen(new MenuScreen(this));
    }

    public Config getConfig() {
        return config;
    }
}

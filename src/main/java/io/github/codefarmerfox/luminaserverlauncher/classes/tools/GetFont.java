package io.github.codefarmerfox.luminaserverlauncher.classes.tools;

import com.badlogic.gdx.files.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.*;
import com.badlogic.gdx.*;

public class GetFont {

    public static final String FONT_BOLD = "assets/fonts/NotoSansCJKsc-Bold.otf";
    public static final String FONT_REGULAR = "assets/fonts/NotoSansCJKsc-Regular.otf";
    public static final String FONT_LEGACY = "assets/fonts/SourceHanSerifCN-Regular.otf";
    public static final String CONTENT_PATH = "assets/fonts/content.txt";

    private static String content;
    private static FreeTypeFontGenerator generator;

    public static void readContent() {
        FileHandle contentFile = Gdx.files.local(CONTENT_PATH);
        if (!contentFile.exists()) {
            contentFile = Gdx.files.internal(CONTENT_PATH);
        }
        content = contentFile.readString();
    }

    private static FileHandle pickFont(boolean bold) {
        String path = bold ? FONT_BOLD : FONT_REGULAR;
        FileHandle fontFile = Gdx.files.local(path);
        if (!fontFile.exists()) {
            fontFile = Gdx.files.internal(path);
        }
        if (!fontFile.exists()) {
            FileHandle legacy = Gdx.files.local(FONT_LEGACY);
            if (!legacy.exists()) {
                legacy = Gdx.files.internal(FONT_LEGACY);
            }
            fontFile = legacy;
        }
        return fontFile;
    }

    public static FreeTypeFontGenerator.FreeTypeFontParameter getParameter(int size, Color color) {
        return getParameter(size, color, false);
    }

    public static FreeTypeFontGenerator.FreeTypeFontParameter getParameter(int size, Color color, boolean bold) {
        if (generator != null) {
            generator.dispose();
            generator = null;
        }
        generator = new FreeTypeFontGenerator(pickFont(bold));

        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.characters = content;
        parameter.size = size;
        if (color != null) {
            parameter.color = color;
        }
        return parameter;
    }

    public static BitmapFont getFont(FreeTypeFontGenerator.FreeTypeFontParameter parameter) {
        return generator.generateFont(parameter);
    }

    public static String getContent() {
        return content;
    }

    public static void setContent(String content) {
        GetFont.content = content;
    }

    public static void dispose() {
        if (generator != null) {
            generator.dispose();
            generator = null;
        }
    }

}

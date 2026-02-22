package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import eb.util.UtilLibGDX;

/**
 * Utility methods for loading and processing textures.
 */
public class TextureUtils {

    private TextureUtils() {}

    /**
     * Loads a texture from the given asset path and converts all white (or near-white)
     * pixels to transparent, so that icons with a white background are displayed
     * correctly on any background colour.
     *
     * <p>Uses {@link UtilLibGDX#newPixmapWhiteTransparent(Pixmap)} to compute per-pixel
     * alpha as the average distance of each pixel's RGB from white, so pixels that are
     * closer to white become more transparent.
     *
     * @param path path to the image file relative to the assets directory
     * @return a new {@link Texture} whose white background has been made transparent;
     *         the caller is responsible for disposing it
     */
    public static Texture makeWhiteTransparent(String path) {
        Pixmap loaded = new Pixmap(Gdx.files.internal(path));
        Pixmap pixmap;
        if (loaded.getFormat() != Pixmap.Format.RGBA8888) {
            pixmap = new Pixmap(loaded.getWidth(), loaded.getHeight(), Pixmap.Format.RGBA8888);
            pixmap.setBlending(Pixmap.Blending.None);
            pixmap.drawPixmap(loaded, 0, 0);
            loaded.dispose();
        } else {
            pixmap = loaded;
        }
        Pixmap result = UtilLibGDX.newPixmapWhiteTransparent(pixmap);
        pixmap.dispose();
        Texture texture = new Texture(result);
        result.dispose();
        return texture;
    }

    /**
     * Loads a texture from the given asset path and inverts the RGB channels of each pixel
     * (negative image), while preserving the alpha channel unchanged.
     *
     * <p>Uses {@link UtilLibGDX#newPixmapNegative(Pixmap)} for the pixel transformation.
     *
     * @param path path to the image file relative to the assets directory
     * @return a new {@link Texture} with inverted RGB; the caller is responsible for disposing it
     */
    public static Texture makeNegative(String path) {
        Pixmap loaded = new Pixmap(Gdx.files.internal(path));
        Pixmap pixmap;
        if (loaded.getFormat() != Pixmap.Format.RGBA8888) {
            pixmap = new Pixmap(loaded.getWidth(), loaded.getHeight(), Pixmap.Format.RGBA8888);
            pixmap.setBlending(Pixmap.Blending.None);
            pixmap.drawPixmap(loaded, 0, 0);
            loaded.dispose();
        } else {
            pixmap = loaded;
        }
        Pixmap result = UtilLibGDX.newPixmapNegative(pixmap);
        pixmap.dispose();
        Texture texture = new Texture(result);
        result.dispose();
        return texture;
    }
}

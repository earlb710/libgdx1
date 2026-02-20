package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/**
 * Utility methods for loading and processing textures.
 */
public class TextureUtils {

    /** Threshold (inclusive) above which each RGB channel is considered "white". */
    private static final int WHITE_THRESHOLD = 240;

    private TextureUtils() {}

    /**
     * Loads a texture from the given asset path and converts all white (or near-white)
     * fully-opaque pixels to fully transparent, so that icons with a white background
     * are displayed correctly on any background colour.
     *
     * <p>A pixel is considered white when its red, green, and blue components are all
     * &ge; {@value #WHITE_THRESHOLD} and its alpha channel is 255 (fully opaque).
     *
     * @param path path to the image file relative to the assets directory
     * @return a new {@link Texture} whose white background has been made transparent;
     *         the caller is responsible for disposing it
     */
    public static Texture makeWhiteTransparent(String path) {
        Pixmap loaded = new Pixmap(Gdx.files.internal(path));

        // Ensure RGBA8888 so getPixel / drawPixel work with a consistent byte layout.
        Pixmap pixmap;
        if (loaded.getFormat() != Pixmap.Format.RGBA8888) {
            pixmap = new Pixmap(loaded.getWidth(), loaded.getHeight(), Pixmap.Format.RGBA8888);
            pixmap.setBlending(Pixmap.Blending.None);
            pixmap.drawPixmap(loaded, 0, 0);
            loaded.dispose();
        } else {
            pixmap = loaded;
        }

        pixmap.setBlending(Pixmap.Blending.None);

        int width  = pixmap.getWidth();
        int height = pixmap.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // libGDX RGBA8888 pixel layout: bits 31-24 = R, 23-16 = G, 15-8 = B, 7-0 = A
                int pixel = pixmap.getPixel(x, y);
                int r = (pixel >>> 24) & 0xFF;
                int g = (pixel >>> 16) & 0xFF;
                int b = (pixel >>>  8) & 0xFF;
                int a =  pixel         & 0xFF;

                if (a == 255 && r >= WHITE_THRESHOLD && g >= WHITE_THRESHOLD && b >= WHITE_THRESHOLD) {
                    pixmap.drawPixel(x, y, 0); // fully transparent
                }
            }
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    /**
     * Loads a texture from the given asset path and inverts the RGB channels of each pixel
     * (negative image), while preserving the alpha channel unchanged.
     *
     * @param path path to the image file relative to the assets directory
     * @return a new {@link Texture} with inverted RGB; the caller is responsible for disposing it
     */
    public static Texture makeNegative(String path) {
        Pixmap loaded = new Pixmap(Gdx.files.internal(path));

        // Ensure RGBA8888 so getPixel / drawPixel work with a consistent byte layout.
        Pixmap pixmap;
        if (loaded.getFormat() != Pixmap.Format.RGBA8888) {
            pixmap = new Pixmap(loaded.getWidth(), loaded.getHeight(), Pixmap.Format.RGBA8888);
            pixmap.setBlending(Pixmap.Blending.None);
            pixmap.drawPixmap(loaded, 0, 0);
            loaded.dispose();
        } else {
            pixmap = loaded;
        }

        pixmap.setBlending(Pixmap.Blending.None);

        int width  = pixmap.getWidth();
        int height = pixmap.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // libGDX RGBA8888 pixel layout: bits 31-24 = R, 23-16 = G, 15-8 = B, 7-0 = A
                int pixel = pixmap.getPixel(x, y);
                int r = (pixel >>> 24) & 0xFF;
                int g = (pixel >>> 16) & 0xFF;
                int b = (pixel >>>  8) & 0xFF;
                int a =  pixel         & 0xFF;

                int inverted = ((255 - r) << 24) | ((255 - g) << 16) | ((255 - b) << 8) | a;
                pixmap.drawPixel(x, y, inverted);
            }
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
}

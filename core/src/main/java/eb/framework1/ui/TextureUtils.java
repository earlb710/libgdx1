package eb.framework1.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.GdxRuntimeException;
import eb.util.UtilLibGDX;

/**
 * Utility methods for loading and processing textures.
 */
public class TextureUtils {

    /**
     * Platform-specific SVG rasterizer. Must be set before calling
     * {@link #loadSvg(String, int, int)}. Each platform launcher is responsible
     * for registering the appropriate implementation at startup.
     */
    private static SvgLoader svgLoader;

    /**
     * Registers the platform-specific SVG loader. Call this once at application
     * startup (before any call to {@link #loadSvg(String, int, int)}).
     *
     * @param loader the SVG loader implementation to use; must not be {@code null}
     * @throws IllegalArgumentException if {@code loader} is {@code null}
     */
    public static void setSvgLoader(SvgLoader loader) {
        if (loader == null) {
            throw new IllegalArgumentException("SvgLoader must not be null");
        }
        svgLoader = loader;
    }

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
     * Loads a texture from the given asset path without any pixel transformation.
     * Use this when the image should be displayed exactly as it is stored on disk.
     *
     * @param path path to the image file relative to the assets directory
     * @return a new {@link Texture} loaded unchanged; the caller is responsible for disposing it
     */
    public static Texture loadAsIs(String path) {
        return new Texture(Gdx.files.internal(path));
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

    /**
     * Loads an SVG file from the given asset path, rasterizes it at the specified pixel
     * dimensions, and returns the result as a {@link Texture}.
     *
     * <p>Requires a platform-specific {@link SvgLoader} to be registered via
     * {@link TextureUtils#setSvgLoader(SvgLoader)} before this method is called. Each platform launcher
     * (e.g. {@code Lwjgl3Launcher}, {@code AndroidLauncher}) registers the appropriate
     * implementation at startup.
     *
     * <p><b>Security note:</b> SVG files should only be loaded from trusted sources
     * (e.g. bundled game assets), never from untrusted user input.
     *
     * @param path   path to the {@code .svg} file relative to the assets directory
     * @param width  target width in pixels (must be &gt; 0)
     * @param height target height in pixels (must be &gt; 0)
     * @return a new {@link Texture} containing the rasterized SVG;
     *         the caller is responsible for disposing it
     * @throws GdxRuntimeException if no {@link SvgLoader} has been registered
     */
    public static Texture loadSvg(String path, int width, int height) {
        if (svgLoader == null) {
            throw new GdxRuntimeException(
                "No SvgLoader registered. Call TextureUtils.setSvgLoader() before loading SVG files.");
        }
        byte[] svgData = Gdx.files.internal(path).readBytes();
        Pixmap pixmap = svgLoader.rasterize(svgData, width, height);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
}

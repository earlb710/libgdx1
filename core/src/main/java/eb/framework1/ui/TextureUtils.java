package eb.framework1.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
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

    /**
     * Loads an SVG file from the given asset path, rasterizes it at the specified pixel
     * dimensions, applies a colour tint, and returns the result as a {@link Texture}.
     *
     * <p>Each output pixel's RGB is blended with the tint by averaging:
     * {@code output = (original + tint) / 2}. The alpha channel is blended the same
     * way, so passing {@link Color#WHITE} (all channels at 1.0) leaves the image
     * unchanged.
     *
     * <p>Requires a platform-specific {@link SvgLoader} to be registered via
     * {@link TextureUtils#setSvgLoader(SvgLoader)} before this method is called.
     *
     * <p><b>Security note:</b> SVG files should only be loaded from trusted sources
     * (e.g. bundled game assets), never from untrusted user input.
     *
     * @param path   path to the {@code .svg} file relative to the assets directory
     * @param width  target width in pixels (must be &gt; 0)
     * @param height target height in pixels (must be &gt; 0)
     * @param tint   the colour to blend into the rasterized image; must not be {@code null}
     * @return a new {@link Texture} containing the tinted, rasterized SVG;
     *         the caller is responsible for disposing it
     * @throws GdxRuntimeException   if no {@link SvgLoader} has been registered
     * @throws IllegalArgumentException if {@code tint} is {@code null}
     */
    public static Texture loadSvg(String path, int width, int height, Color tint) {
        if (tint == null) {
            throw new IllegalArgumentException("tint must not be null");
        }
        if (svgLoader == null) {
            throw new GdxRuntimeException(
                "No SvgLoader registered. Call TextureUtils.setSvgLoader() before loading SVG files.");
        }
        byte[] svgData = Gdx.files.internal(path).readBytes();
        Pixmap pixmap = svgLoader.rasterize(svgData, width, height);
        Pixmap tinted = UtilLibGDX.newPixmapTint(pixmap,
            Math.round(tint.r * 255),
            Math.round(tint.g * 255),
            Math.round(tint.b * 255),
            Math.round(tint.a * 255));
        pixmap.dispose();
        Texture texture = new Texture(tinted);
        tinted.dispose();
        return texture;
    }

    /**
     * Loads a monochrome SVG file from the given asset path, rasterizes it at the specified
     * pixel dimensions, and recolours the result so the ink is drawn in exactly the given
     * colour.
     *
     * <p>Unlike {@link #loadSvg(String, int, int, Color)} (which blends/tints by averaging),
     * this method <em>replaces</em> the RGB of every pixel with the target colour and preserves
     * the source alpha channel unchanged.  This is the correct operation for SVGs that consist
     * of a single ink colour on a transparent background (e.g. monochrome icon sets): the
     * rasterized alpha channel encodes both the shape and anti-aliased edges, so only the
     * colour needs to change.
     *
     * <pre>
     * // Draw a black icon as red
     * Texture icon = TextureUtils.loadSvgMonochrome("icons/eye.svg", 64, 64, Color.RED);
     * </pre>
     *
     * <p>Requires a platform-specific {@link SvgLoader} to be registered via
     * {@link TextureUtils#setSvgLoader(SvgLoader)} before this method is called.
     *
     * <p><b>Security note:</b> SVG files should only be loaded from trusted sources
     * (e.g. bundled game assets), never from untrusted user input.
     *
     * @param path   path to the {@code .svg} file relative to the assets directory
     * @param width  target width in pixels (must be &gt; 0)
     * @param height target height in pixels (must be &gt; 0)
     * @param color  the exact ink colour to use; must not be {@code null}
     * @return a new {@link Texture} containing the recoloured, rasterized SVG;
     *         the caller is responsible for disposing it
     * @throws GdxRuntimeException      if no {@link SvgLoader} has been registered
     * @throws IllegalArgumentException if {@code color} is {@code null}
     */
    public static Texture loadSvgMonochrome(String path, int width, int height, Color color) {
        if (color == null) {
            throw new IllegalArgumentException("color must not be null");
        }
        if (svgLoader == null) {
            throw new GdxRuntimeException(
                "No SvgLoader registered. Call TextureUtils.setSvgLoader() before loading SVG files.");
        }
        byte[] svgData = Gdx.files.internal(path).readBytes();
        Pixmap pixmap = svgLoader.rasterize(svgData, width, height);
        Pixmap recolored = UtilLibGDX.newPixmapRecolor(pixmap,
            Math.round(color.r * 255),
            Math.round(color.g * 255),
            Math.round(color.b * 255));
        pixmap.dispose();
        Texture texture = new Texture(recolored);
        recolored.dispose();
        return texture;
    }
}

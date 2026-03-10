package eb.framework1.ui;

import com.badlogic.gdx.graphics.Pixmap;

/**
 * Platform-specific SVG rasterizer.
 *
 * <p>Each platform (desktop, Android) provides its own implementation. Register the
 * implementation with {@link TextureUtils#setSvgLoader(SvgLoader)} before calling
 * {@link TextureUtils#loadSvg(String, int, int)}.
 *
 * <p><b>Security note:</b> SVG files should only be loaded from trusted sources
 * (e.g. bundled game assets), never from untrusted user input.
 */
public interface SvgLoader {

    /**
     * Rasterizes the given SVG data to a new RGBA8888 {@link Pixmap} at the requested size.
     *
     * @param svgData UTF-8 bytes of the SVG file
     * @param width   target width in pixels (must be &gt; 0)
     * @param height  target height in pixels (must be &gt; 0)
     * @return a newly created {@link Pixmap}; the caller is responsible for disposing it
     */
    Pixmap rasterize(byte[] svgData, int width, int height);
}

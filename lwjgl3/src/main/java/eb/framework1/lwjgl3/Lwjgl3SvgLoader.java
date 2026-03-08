package eb.framework1.lwjgl3;

import com.badlogic.gdx.graphics.Pixmap;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;
import eb.framework1.ui.SvgLoader;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Desktop (LWJGL3) implementation of {@link SvgLoader} that uses the
 * svgSalamander library (included transitively via {@code gdx-tools}) to
 * rasterize SVG files to libGDX {@link Pixmap} instances.
 *
 * <p>Register an instance via {@link eb.framework1.ui.TextureUtils#setSvgLoader} in
 * {@link Lwjgl3Launcher} before the application starts.
 *
 * <p><b>Security note:</b> Only load SVG files from trusted sources (e.g.
 * bundled game assets). The underlying svgSalamander library may follow
 * external references in maliciously crafted SVG files; restricting input
 * to game-owned assets mitigates this risk.
 */
public class Lwjgl3SvgLoader implements SvgLoader {

    @Override
    public Pixmap rasterize(byte[] svgData, int width, int height) {
        try {
            SVGUniverse universe = new SVGUniverse();
            universe.setVerbose(false);
            URI uri;
            try (InputStreamReader reader = new InputStreamReader(
                    new ByteArrayInputStream(svgData), StandardCharsets.UTF_8)) {
                uri = universe.loadSVG(reader, "svg-asset");
            }
            SVGDiagram diagram = universe.getDiagram(uri);
            diagram.setIgnoringClipHeuristic(true);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            double svgWidth  = diagram.getWidth();
            double svgHeight = diagram.getHeight();
            if (svgWidth > 0 && svgHeight > 0) {
                g.setTransform(AffineTransform.getScaleInstance(
                    width  / svgWidth,
                    height / svgHeight));
            }

            diagram.render(g);
            g.dispose();

            return toPixmap(image, width, height);
        } catch (Exception e) {
            throw new RuntimeException("Failed to rasterize SVG: " + e.getMessage(), e);
        }
    }

    private static Pixmap toPixmap(BufferedImage image, int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        ByteBuffer buffer = pixmap.getPixels();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                buffer.put((byte) ((argb >> 16) & 0xFF)); // R
                buffer.put((byte) ((argb >>  8) & 0xFF)); // G
                buffer.put((byte) ( argb        & 0xFF)); // B
                buffer.put((byte) ((argb >> 24) & 0xFF)); // A
            }
        }
        buffer.flip();
        return pixmap;
    }
}

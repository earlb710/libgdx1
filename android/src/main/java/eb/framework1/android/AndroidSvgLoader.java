package eb.framework1.android;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import com.badlogic.gdx.graphics.Pixmap;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import eb.framework1.ui.SvgLoader;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

/**
 * Android implementation of {@link SvgLoader} that uses the
 * <a href="https://bigbadaboom.github.io/androidsvg/">androidsvg</a> library to
 * rasterize SVG files to libGDX {@link Pixmap} instances.
 *
 * <p>Register an instance via {@link eb.framework1.ui.TextureUtils#setSvgLoader} in
 * {@link AndroidLauncher} before the application starts.
 */
public class AndroidSvgLoader implements SvgLoader {

    @Override
    public Pixmap rasterize(byte[] svgData, int width, int height) {
        try {
            SVG svg = SVG.getFromInputStream(new ByteArrayInputStream(svgData));
            svg.setDocumentWidth(width);
            svg.setDocumentHeight(height);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            try {
                Canvas canvas = new Canvas(bitmap);
                svg.renderToCanvas(canvas, new RectF(0, 0, width, height));
                return toPixmap(bitmap, width, height);
            } finally {
                bitmap.recycle();
            }
        } catch (SVGParseException e) {
            throw new RuntimeException("Failed to rasterize SVG: " + e.getMessage(), e);
        }
    }

    private static Pixmap toPixmap(Bitmap bitmap, int width, int height) {
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        ByteBuffer buffer = pixmap.getPixels();
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >>  8) & 0xFF)); // G
            buffer.put((byte) ( pixel        & 0xFF)); // B
            buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
        }
        buffer.flip();
        return pixmap;
    }
}

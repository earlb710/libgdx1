package eb.framework1;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

/**
 * Utility for measuring the screen area required to display text.
 *
 * <p>All methods use LibGDX's {@link GlyphLayout} for accurate per-glyph
 * measurement so the results match what {@code SpriteBatch} will actually render.
 *
 * <p>Multi-line text is fully supported: any {@code \n} characters in the input
 * string are handled natively by {@link GlyphLayout}.
 *
 * <p>Usage examples:
 * <pre>{@code
 * // Uniform padding
 * TextMeasurer.TextBounds b = TextMeasurer.measure(font, "Hello\nWorld", 8f);
 * float boxW = b.width;   // text width  + 16px (8 left + 8 right)
 * float boxH = b.height;  // text height + 16px (8 top  + 8 bottom)
 *
 * // Separate h/v padding
 * TextMeasurer.TextBounds b2 = TextMeasurer.measure(font, "Line1\nLine2", 12f, 6f);
 *
 * // Temporary scale override (bold at a different size)
 * TextMeasurer.TextBounds b3 = TextMeasurer.measureScaled(boldFont, 1.25f, "Big text", 10f);
 * }</pre>
 */
public final class TextMeasurer {

    private TextMeasurer() {} // static utility – not instantiable

    // -------------------------------------------------------------------------
    // Result type
    // -------------------------------------------------------------------------

    /**
     * Immutable result of a text-measurement call.
     *
     * <p>{@link #textWidth} / {@link #textHeight} are the raw glyph extents.
     * {@link #width} / {@link #height} add the requested padding on all sides.
     */
    public static final class TextBounds {
        /** Measured text width, in pixels (no padding). */
        public final float textWidth;
        /** Measured text height, in pixels (no padding). */
        public final float textHeight;
        /** Total area width  = textWidth  + 2 × paddingH */
        public final float width;
        /** Total area height = textHeight + 2 × paddingV */
        public final float height;

        TextBounds(float textW, float textH, float paddingH, float paddingV) {
            this.textWidth  = textW;
            this.textHeight = textH;
            this.width      = textW + 2f * paddingH;
            this.height     = textH + 2f * paddingV;
        }
    }

    // -------------------------------------------------------------------------
    // measure – uses the font at its current scale
    // -------------------------------------------------------------------------

    /**
     * Measures the bounding box required for {@code text} rendered with {@code font}
     * at the font's current scale, adding equal padding on all four sides.
     *
     * <p>Multi-line text (separated by {@code \n}) is handled correctly.
     *
     * @param font    {@link BitmapFont} to use (regular or bold variant)
     * @param text    Text to measure; may contain {@code \n} for line breaks
     * @param padding Uniform padding in pixels added on each of the four sides
     * @return {@link TextBounds} with raw and padded dimensions
     */
    public static TextBounds measure(BitmapFont font, String text, float padding) {
        return measure(font, text, padding, padding);
    }

    /**
     * Measures the bounding box with separate horizontal and vertical padding.
     *
     * @param font     {@link BitmapFont} to use
     * @param text     Text to measure; may contain {@code \n} for line breaks
     * @param paddingH Horizontal padding (left and right each), in pixels
     * @param paddingV Vertical padding (top and bottom each), in pixels
     * @return {@link TextBounds} with raw and padded dimensions
     */
    public static TextBounds measure(BitmapFont font, String text, float paddingH, float paddingV) {
        GlyphLayout layout = new GlyphLayout(font, text);
        return new TextBounds(layout.width, layout.height, paddingH, paddingV);
    }

    /**
     * Measures using a caller-supplied {@link GlyphLayout} to avoid per-call allocation.
     * Useful in render loops where GC pressure should be minimised.
     *
     * @param font     {@link BitmapFont} to use
     * @param layout   Reusable {@link GlyphLayout} instance (will be overwritten)
     * @param text     Text to measure; may contain {@code \n} for line breaks
     * @param padding  Uniform padding in pixels added on each side
     * @return {@link TextBounds} with raw and padded dimensions
     */
    public static TextBounds measure(BitmapFont font, GlyphLayout layout, String text, float padding) {
        return measure(font, layout, text, padding, padding);
    }

    /**
     * Measures using a caller-supplied {@link GlyphLayout} with separate h/v padding.
     *
     * @param font     {@link BitmapFont} to use
     * @param layout   Reusable {@link GlyphLayout} instance (will be overwritten)
     * @param text     Text to measure; may contain {@code \n} for line breaks
     * @param paddingH Horizontal padding (left and right each), in pixels
     * @param paddingV Vertical padding (top and bottom each), in pixels
     * @return {@link TextBounds} with raw and padded dimensions
     */
    public static TextBounds measure(BitmapFont font, GlyphLayout layout, String text,
                                     float paddingH, float paddingV) {
        layout.setText(font, text);
        return new TextBounds(layout.width, layout.height, paddingH, paddingV);
    }

    // -------------------------------------------------------------------------
    // measureScaled – temporarily overrides the font's scale
    // -------------------------------------------------------------------------

    /**
     * Measures the bounding box after temporarily setting the font's uniform scale
     * to {@code scale}.  The font's original scale is restored before returning.
     *
     * <p>This is useful when a single {@link BitmapFont} (e.g. a bold variant)
     * should be measured at a different size without permanently altering it.
     *
     * @param font    {@link BitmapFont} to use (may be a bold variant)
     * @param scale   Uniform scale factor (1.0 = no change, 1.5 = 50% larger, etc.)
     * @param text    Text to measure; may contain {@code \n} for line breaks
     * @param padding Uniform padding in pixels added on each side
     * @return {@link TextBounds} with raw and padded dimensions
     */
    public static TextBounds measureScaled(BitmapFont font, float scale, String text, float padding) {
        return measureScaled(font, scale, text, padding, padding);
    }

    /**
     * Measures the bounding box at an explicit scale with separate h/v padding.
     *
     * @param font     {@link BitmapFont} to use
     * @param scale    Uniform scale factor applied temporarily
     * @param text     Text to measure; may contain {@code \n} for line breaks
     * @param paddingH Horizontal padding (left and right each), in pixels
     * @param paddingV Vertical padding (top and bottom each), in pixels
     * @return {@link TextBounds} with raw and padded dimensions
     */
    public static TextBounds measureScaled(BitmapFont font, float scale,
                                           String text, float paddingH, float paddingV) {
        float prevX = font.getScaleX();
        float prevY = font.getScaleY();
        font.getData().setScale(scale);
        try {
            GlyphLayout layout = new GlyphLayout(font, text);
            return new TextBounds(layout.width, layout.height, paddingH, paddingV);
        } finally {
            font.getData().setScale(prevX, prevY);
        }
    }
}

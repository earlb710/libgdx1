package eb.gmodel1.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Greedy word-wrap utility.
 *
 * <p>Splits a paragraph of text into lines so that no line exceeds a given
 * pixel width.  The width of each candidate string is measured via a
 * caller-supplied {@link WidthMeasurer}, making the class usable both in
 * LibGDX render code (where a {@link com.badlogic.gdx.graphics.g2d.GlyphLayout}
 * provides accurate per-glyph measurements) and in plain unit tests (where a
 * simple {@code text -> text.length() * charWidth} approximation suffices).
 *
 * <p>Usage in render code:
 * <pre>{@code
 * GlyphLayout layout = new GlyphLayout();
 * List<String> lines = WordWrapper.wrap(text, maxPx, t -> {
 *     layout.setText(font, t);
 *     return layout.width;
 * });
 * }</pre>
 */
public final class WordWrapper {

    private WordWrapper() {} // static utility

    /**
     * Functional interface for measuring the rendered pixel width of a string.
     * Implementations must be consistent: the same string must always return the
     * same value within a single wrap call.
     */
    @FunctionalInterface
    public interface WidthMeasurer {
        public float measure(String text);
    }

    /**
     * Wraps {@code text} into lines, each no wider than {@code maxWidth} pixels.
     *
     * <p>The algorithm is greedy: words are accumulated left-to-right until
     * adding the next word would exceed {@code maxWidth}, at which point a new
     * line is started.  A single word that is already wider than {@code maxWidth}
     * is placed on its own line without further splitting.
     *
     * <p>Existing {@code \n} characters in the source text are honoured:
     * each segment between {@code \n} characters is wrapped independently.
     *
     * @param text      Text to wrap (may be {@code null} or empty)
     * @param maxWidth  Maximum line width in pixels (must be &gt; 0)
     * @param measurer  Function that returns the pixel width of a string
     * @return Unmodifiable list of wrapped lines; empty if {@code text} is
     *         {@code null} or blank
     */
    public static List<String> wrap(String text, float maxWidth, WidthMeasurer measurer) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        if (maxWidth <= 0) return Collections.singletonList(text);

        List<String> result = new ArrayList<>();

        // Honour existing hard line-breaks by processing each paragraph separately
        for (String paragraph : text.split("\n", -1)) {
            wrapParagraph(paragraph, maxWidth, measurer, result);
        }

        return Collections.unmodifiableList(result);
    }

    /** Wraps a single paragraph (no embedded newlines) into {@code out}. */
    private static void wrapParagraph(String paragraph, float maxWidth,
                                      WidthMeasurer measurer, List<String> out) {
        if (paragraph.isEmpty()) {
            out.add("");
            return;
        }

        String[] words = paragraph.split(" ", -1);
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() == 0) {
                // First word on a new line — always append even if wider than maxWidth
                current.append(word);
            } else {
                String candidate = current + " " + word;
                if (measurer.measure(candidate) <= maxWidth) {
                    current.append(' ').append(word);
                } else {
                    out.add(current.toString());
                    current = new StringBuilder(word);
                }
            }
        }

        if (current.length() > 0) {
            out.add(current.toString());
        }
    }
}

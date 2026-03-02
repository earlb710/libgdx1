package eb.gmodel1.generator;

import eb.gmodel1.ui.*;


import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

/**
 * Unit tests for {@link WordWrapper}.
 *
 * Width is approximated as {@code text.length() * 8} pixels so that no
 * LibGDX runtime is needed.
 */
public class WordWrapperTest {

    /** Fixed-width measurer: 8 px per character. */
    private static final WordWrapper.WidthMeasurer PX8 = text -> text.length() * 8f;

    // -------------------------------------------------------------------------
    // Null / empty / blank
    // -------------------------------------------------------------------------

    @Test
    public void wrap_null_returnsEmpty() {
        assertTrue(WordWrapper.wrap(null, 200f, PX8).isEmpty());
    }

    @Test
    public void wrap_emptyString_returnsEmpty() {
        assertTrue(WordWrapper.wrap("", 200f, PX8).isEmpty());
    }

    // -------------------------------------------------------------------------
    // Single-line text that fits
    // -------------------------------------------------------------------------

    @Test
    public void wrap_textFits_returnsSingleLine() {
        // "Hello" = 5 chars × 8 = 40 px; max = 200
        List<String> lines = WordWrapper.wrap("Hello", 200f, PX8);
        assertEquals(1, lines.size());
        assertEquals("Hello", lines.get(0));
    }

    @Test
    public void wrap_exactFit_noBreak() {
        // "Hello World" = 11 chars × 8 = 88 px; max = 88
        List<String> lines = WordWrapper.wrap("Hello World", 88f, PX8);
        assertEquals(1, lines.size());
        assertEquals("Hello World", lines.get(0));
    }

    // -------------------------------------------------------------------------
    // Wrapping at word boundary
    // -------------------------------------------------------------------------

    @Test
    public void wrap_twoWords_splitAtBoundary() {
        // "Hello World" = 88 px; max = 87 → must split
        List<String> lines = WordWrapper.wrap("Hello World", 87f, PX8);
        assertEquals(2, lines.size());
        assertEquals("Hello", lines.get(0));
        assertEquals("World", lines.get(1));
    }

    @Test
    public void wrap_multipleWords_greedySplit() {
        // "one two three four" — 8 px/char, max 96 px (= 12 chars)
        // "one two" = 7 × 8 = 56 ≤ 96
        // "one two three" = 13 × 8 = 104 > 96 → line break after "two"
        // "three four" = 10 × 8 = 80 ≤ 96
        List<String> lines = WordWrapper.wrap("one two three four", 96f, PX8);
        assertEquals(2, lines.size());
        assertEquals("one two", lines.get(0));
        assertEquals("three four", lines.get(1));
    }

    @Test
    public void wrap_threeLines() {
        // max = 96 px (12 chars × 8)
        // "alpha beta" = 10 × 8 = 80 ≤ 96 → first line
        // "alpha beta gamma" = 16 × 8 = 128 > 96 → break
        // "gamma delta" = 11 × 8 = 88 ≤ 96 → second line
        // "gamma delta epsilon" = 19 × 8 = 152 > 96 → break → "epsilon" alone
        List<String> lines = WordWrapper.wrap("alpha beta gamma delta epsilon", 96f, PX8);
        assertEquals(3, lines.size());
        assertEquals("alpha beta", lines.get(0));
        assertEquals("gamma delta", lines.get(1));
        assertEquals("epsilon", lines.get(2));
    }

    // -------------------------------------------------------------------------
    // Single word wider than max
    // -------------------------------------------------------------------------

    @Test
    public void wrap_singleWordTooLong_keepOnOneLine() {
        // "Supercalifragilistic" = 20 × 8 = 160 px; max = 80 → no split possible
        List<String> lines = WordWrapper.wrap("Supercalifragilistic", 80f, PX8);
        assertEquals(1, lines.size());
        assertEquals("Supercalifragilistic", lines.get(0));
    }

    @Test
    public void wrap_firstWordTooLong_thenNormal() {
        // "Supercali next" — max 80 px
        // "Supercali" = 9×8=72 ≤ 80 fits as first word, "Supercali next" = 14×8=112 > 80
        // so "Supercali" | "next"
        List<String> lines = WordWrapper.wrap("Supercali next", 80f, PX8);
        assertEquals(2, lines.size());
        assertEquals("Supercali", lines.get(0));
        assertEquals("next", lines.get(1));
    }

    // -------------------------------------------------------------------------
    // Existing hard line-breaks
    // -------------------------------------------------------------------------

    @Test
    public void wrap_existingNewline_eachParagraphWrapped() {
        // "Hello\nWorld" → two paragraphs, each fits on one line
        List<String> lines = WordWrapper.wrap("Hello\nWorld", 200f, PX8);
        assertEquals(2, lines.size());
        assertEquals("Hello", lines.get(0));
        assertEquals("World", lines.get(1));
    }

    @Test
    public void wrap_existingNewline_paragraphsWrappedIndependently() {
        // "one two\nthree four" with max 80 px
        // "one two" = 7×8=56 ≤ 80 → single line
        // "three four" = 10×8=80 ≤ 80 → single line
        List<String> lines = WordWrapper.wrap("one two\nthree four", 80f, PX8);
        assertEquals(2, lines.size());
        assertEquals("one two", lines.get(0));
        assertEquals("three four", lines.get(1));
    }

    // -------------------------------------------------------------------------
    // Zero / negative maxWidth → no wrap
    // -------------------------------------------------------------------------

    @Test
    public void wrap_zeroMaxWidth_returnsWholeTextAsSingleLine() {
        List<String> lines = WordWrapper.wrap("Hello World", 0f, PX8);
        assertEquals(1, lines.size());
        assertEquals("Hello World", lines.get(0));
    }

    // -------------------------------------------------------------------------
    // Result is unmodifiable
    // -------------------------------------------------------------------------

    @Test(expected = UnsupportedOperationException.class)
    public void wrap_result_isUnmodifiable() {
        List<String> lines = WordWrapper.wrap("Hello", 200f, PX8);
        lines.add("extra"); // must throw
    }
}

package eb.framework1;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Popup explaining that the character was too tired to act.
 *
 * <p>Shown (full-width at the bottom of the screen, non-blocking of the map) whenever
 * the player attempts a stamina-costing action while stamina is at 2 or below.
 * The popup describes what automatically happened: the player was sent home and
 * either rested (daytime) or slept until 06:00 (nighttime).
 *
 * <p>Dismiss via the OK button.
 */
class TirednessPopup {

    private static final Color BG_COLOR     = new Color(0.18f, 0.04f, 0.04f, 1f);
    private static final Color BORDER_COLOR = new Color(0.85f, 0.25f, 0.25f, 1f);
    private static final Color TITLE_COLOR  = new Color(1.00f, 0.40f, 0.40f, 1f);
    private static final Color BTN_COLOR    = new Color(0.10f, 0.50f, 0.15f, 1f);

    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyph;

    private boolean      visible = false;
    private final List<String> lines = new ArrayList<>();

    // OK button bounds (written during draw, read by onTap)
    private float okX, okY, okW, okH;

    TirednessPopup(SpriteBatch batch, ShapeRenderer sr,
                   BitmapFont font, BitmapFont smallFont, GlyphLayout glyph) {
        this.batch     = batch;
        this.sr        = sr;
        this.font      = font;
        this.smallFont = smallFont;
        this.glyph     = glyph;
    }

    // -------------------------------------------------------------------------
    // State queries
    // -------------------------------------------------------------------------

    boolean isVisible() { return visible; }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    /** Shows the popup with the given descriptive message lines. */
    void show(List<String> messageLines) {
        lines.clear();
        lines.addAll(messageLines);
        visible = true;
        okW = 0;
    }

    /** Dismiss the popup if the OK button was tapped. */
    void onTap(int screenX, int flippedY) {
        if (visible && okW > 0
                && screenX >= okX && screenX <= okX + okW
                && flippedY >= okY && flippedY <= okY + okH) {
            visible = false;
            lines.clear();
        }
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    void draw(int screenW, int screenH) {
        if (!visible) return;

        final float PAD    = 24f;
        final float GAP    = 8f;   // extra inter-line spacing

        // Measure all text via TextMeasurer
        TextMeasurer.TextBounds titleBounds = TextMeasurer.measure(font, glyph, "Too Tired!", 0f, 0f);
        float titleH     = titleBounds.textHeight;
        float titleLineH = titleH + GAP;

        TextMeasurer.TextBounds linesBounds =
                TextMeasurer.measureLines(smallFont, glyph, lines, GAP, 0f, 0f);
        float linesH   = linesBounds.textHeight;
        float maxLineW = linesBounds.textWidth;

        TextMeasurer.TextBounds okBounds = TextMeasurer.measure(font, glyph, "OK", 24f, 10f);
        float okBtnW = okBounds.width;
        float okBtnH = okBounds.height;

        // Height = PAD + titleLine + titleH (char-size gap) + linesH + PAD (gap) + okBtn + PAD
        float dialogH = PAD + titleLineH + titleH + linesH + PAD + okBtnH + PAD;
        // Width = max(title, widest line, OK button text) + 2*PAD, capped at screenW
        float rawW = Math.max(titleBounds.textWidth, Math.max(maxLineW, okBounds.textWidth));
        float dialogW = Math.min(screenW, rawW + 2 * PAD);
        float dialogX = 0f;
        float dialogY = 0f;

        // --- Shapes ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(dialogX, dialogY, dialogW, dialogH);
        okX = dialogX + (dialogW - okBtnW) / 2f;
        okY = dialogY + PAD;
        okW = okBtnW;
        okH = okBtnH;
        sr.setColor(BTN_COLOR);
        sr.rect(okX, okY, okW, okH);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(dialogX,     dialogY,     dialogW,     dialogH);
        sr.rect(dialogX + 1, dialogY + 1, dialogW - 2, dialogH - 2);
        sr.rect(okX,     okY,     okW,     okH);
        sr.rect(okX + 1, okY + 1, okW - 2, okH - 2);
        sr.end();

        // --- Text ---
        batch.begin();
        float ty = dialogY + dialogH - PAD - titleH;

        font.setColor(TITLE_COLOR);
        glyph.setText(font, "Too Tired!");
        font.draw(batch, "Too Tired!", dialogX + (dialogW - glyph.width) / 2f, ty);
        ty -= titleLineH + titleH;

        smallFont.setColor(Color.WHITE);
        for (String line : lines) {
            glyph.setText(smallFont, line);
            smallFont.draw(batch, line, dialogX + (dialogW - glyph.width) / 2f, ty);
            ty -= glyph.height + GAP;
        }

        font.setColor(Color.WHITE);
        glyph.setText(font, "OK");
        font.draw(batch, "OK",
                okX + (okW - glyph.width) / 2f,
                okY + (okH + glyph.height) / 2f);
        batch.end();
    }
}

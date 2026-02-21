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

        glyph.setText(font, "Hg");
        float fontH      = glyph.height;
        float fontLineH  = fontH * 1.6f;
        glyph.setText(smallFont, "Hg");
        float smallH     = glyph.height;
        float smallLineH = smallH * 1.5f;

        glyph.setText(font, "OK");
        float okBtnW = glyph.width + 48f;
        float okBtnH = glyph.height + 20f;

        final float PAD = 24f;
        float dialogH = PAD
                + fontLineH                      // "Too Tired!" title
                + lines.size() * smallLineH      // message lines
                + PAD + okBtnH                   // OK button + gap
                + PAD;
        float dialogW = screenW;
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
        float ty = dialogY + dialogH - PAD - fontH;

        font.setColor(TITLE_COLOR);
        glyph.setText(font, "Too Tired!");
        font.draw(batch, "Too Tired!", dialogX + (dialogW - glyph.width) / 2f, ty);
        ty -= fontLineH;

        smallFont.setColor(Color.WHITE);
        for (String line : lines) {
            glyph.setText(smallFont, line);
            smallFont.draw(batch, line, dialogX + (dialogW - glyph.width) / 2f, ty);
            ty -= smallLineH;
        }

        font.setColor(Color.WHITE);
        glyph.setText(font, "OK");
        font.draw(batch, "OK",
                okX + (okW - glyph.width) / 2f,
                okY + (okH + glyph.height) / 2f);
        batch.end();
    }
}

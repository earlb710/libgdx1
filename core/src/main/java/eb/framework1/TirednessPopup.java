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

        final float PAD    = 20f;
        final float GAP    = 8f;

        // --- Font metrics ---
        glyph.setText(font, "Hg");
        float fontH  = glyph.height;
        glyph.setText(smallFont, "Hg");
        float smallH = glyph.height;

        // --- Button dimensions ---
        final float BTN_PAD_X = 24f;
        final float BTN_PAD_Y = 10f;
        glyph.setText(font, "OK");
        float okBtnW = glyph.width + 2 * BTN_PAD_X;
        float okBtnH = fontH + 2 * BTN_PAD_Y;

        // --- Content widths ---
        glyph.setText(font, "Too Tired!");
        float titleW = glyph.width;
        float maxLineW = 0f;
        for (String line : lines) {
            glyph.setText(smallFont, line);
            if (glyph.width > maxLineW) maxLineW = glyph.width;
        }

        // --- Dialog dimensions ---
        float contentW = Math.max(titleW, Math.max(maxLineW, okBtnW));
        float dialogW  = Math.min(screenW * 0.92f, contentW + 2 * PAD);
        okBtnW = Math.min(okBtnW, dialogW - 2 * PAD);

        float linesH = lines.size() * (smallH + GAP);
        float dialogH = PAD
                + fontH  + GAP          // title
                + linesH               // message lines
                + GAP                  // spacer before button
                + okBtnH               // OK button
                + PAD;
        dialogH = Math.min(dialogH, screenH * 0.90f);

        float dialogX = (screenW - dialogW) / 2f;
        float dialogY = (screenH - dialogH) / 2f;

        // --- Single top-down layout pass ---
        float curY = dialogY + dialogH - PAD;

        float titleY = curY;
        curY -= fontH + GAP;

        float[] lineYs = new float[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            lineYs[i] = curY;
            curY -= smallH + GAP;
        }
        curY -= GAP; // spacer before button

        okX = dialogX + (dialogW - okBtnW) / 2f;
        okY = curY - okBtnH;
        okW = okBtnW;
        okH = okBtnH;

        // --- Shapes ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(dialogX, dialogY, dialogW, dialogH);
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
        glyph.setText(font, "Too Tired!");
        font.setColor(TITLE_COLOR);
        font.draw(batch, "Too Tired!", dialogX + (dialogW - glyph.width) / 2f, titleY);

        smallFont.setColor(Color.WHITE);
        for (int i = 0; i < lines.size(); i++) {
            glyph.setText(smallFont, lines.get(i));
            smallFont.draw(batch, lines.get(i), dialogX + (dialogW - glyph.width) / 2f, lineYs[i]);
        }

        glyph.setText(font, "OK");
        font.setColor(Color.WHITE);
        font.draw(batch, "OK",
                okX + (okW - glyph.width) / 2f,
                okY + (okH + glyph.height) / 2f);
        batch.end();
    }
}

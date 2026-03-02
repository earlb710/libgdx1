package eb.framework1.popup;

import eb.framework1.ui.*;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple modal popup used to display the outcome of using a building service
 * (e.g. "You rented a room and rested", "You worked out and gained +1 Strength").
 *
 * <p>Pattern matches the existing {@link TirednessPopup}: centred on screen,
 * coloured title, body lines in white smallFont, single OK button.</p>
 */
public class ServiceResultPopup {

    private static final Color BG_COLOR     = new Color(0.08f, 0.14f, 0.22f, 1f);
    private static final Color BORDER_COLOR = new Color(0.40f, 0.70f, 1.00f, 1f);
    private static final Color TITLE_COLOR  = new Color(0.60f, 1.00f, 0.70f, 1f);
    private static final Color BTN_COLOR    = new Color(0.10f, 0.50f, 0.15f, 1f);

    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyph;

    private boolean           visible = false;
    private String            title   = "";
    private final List<String> lines  = new ArrayList<>();

    // OK button bounds (written during draw, read by onTap)
    private float okX, okY, okW, okH;

    // -------------------------------------------------------------------------

    public ServiceResultPopup(SpriteBatch batch, ShapeRenderer sr,
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

    public boolean isVisible() { return visible; }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    /** Shows the popup with the given title and message lines. */
    public void show(String title, List<String> messageLines) {
        this.title = title != null ? title : "";
        lines.clear();
        lines.addAll(messageLines);
        visible = true;
        okW     = 0f;
        Gdx.app.log("ServiceResultPopup", "Showing: " + this.title);
    }

    /** Dismisses the popup if the OK button was tapped. */
    public void onTap(int screenX, int flippedY) {
        if (visible && okW > 0
                && screenX >= okX && screenX <= okX + okW
                && flippedY >= okY && flippedY <= okY + okH) {
            visible = false;
            lines.clear();
            Gdx.app.log("ServiceResultPopup", "Dismissed");
        }
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    public void draw(int screenW, int screenH) {
        if (!visible) return;

        final float PAD = 24f;
        final float GAP = 8f;

        // Font metrics
        glyph.setText(font, "Hg");
        float titleH     = glyph.height;
        float titleLineH = titleH + GAP;

        glyph.setText(smallFont, "Hg");
        float smallH     = glyph.height;
        float smallLineH = smallH + GAP;

        // OK button
        TextMeasurer.TextBounds okBounds = TextMeasurer.measure(font, glyph, "OK", 24f, 10f);
        float okBtnW = okBounds.width;
        float okBtnH = okBounds.height;

        // Measure content
        glyph.setText(font, title);
        float titleW = glyph.width;
        float maxW   = titleW;
        for (String line : lines) {
            glyph.setText(smallFont, line);
            if (glyph.width > maxW) maxW = glyph.width;
        }
        maxW = Math.max(maxW, okBounds.textWidth);

        float dialogW = Math.min(screenW * 0.9f, maxW + 2 * PAD);
        float btnGap  = Math.max(PAD, titleLineH);
        float dialogH = PAD + titleLineH + titleH
                + lines.size() * smallLineH
                + btnGap + okBtnH + PAD;

        float dialogX = (screenW - dialogW) / 2f;
        float dialogY = (screenH - dialogH) / 2f;

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
        glyph.setText(font, title);
        font.draw(batch, title, dialogX + (dialogW - glyph.width) / 2f, ty);
        ty -= titleLineH + titleH;

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

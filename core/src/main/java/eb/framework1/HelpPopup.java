package eb.framework1;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.Gdx;

/**
 * A self-contained help popup anchored to the lower-right corner of the screen.
 *
 * <p>Displays two sections:
 * <ul>
 *   <li><b>Controls</b> — keyboard and gesture hints</li>
 *   <li><b>Map Legend</b> — a colour swatch and name for each terrain/cell type</li>
 * </ul>
 *
 * <p>A "?" close button sits in the bottom-right corner of the popup itself;
 * clicking it (or clicking the same "?" button in the info bar) dismisses the popup.
 * The caller is responsible for toggling {@link MapViewState#helpVisible} and for
 * routing taps to {@link #onTap(int, int)}.
 */
class HelpPopup {

    // -------------------------------------------------------------------------
    // Layout constants
    // -------------------------------------------------------------------------
    private static final float PAD          = 16f;
    private static final float SECTION_GAP  = 12f;
    private static final float LINE_GAP     = 6f;
    private static final float SWATCH_SIZE  = 14f;
    private static final float SWATCH_GAP   = 8f;
    private static final float MARGIN       = 8f; // gap between popup and screen edge

    // -------------------------------------------------------------------------
    // Colours (popup chrome)
    // -------------------------------------------------------------------------
    private static final Color BG_COLOR     = new Color(0.1f, 0.1f, 0.18f, 0.97f);
    private static final Color BORDER_COLOR = new Color(0.45f, 0.5f, 0.65f, 1f);
    private static final Color TITLE_COLOR  = new Color(0f, 1f, 0f, 1f);
    private static final Color BTN_BG_ACTIVE= new Color(0.1f, 0.35f, 0.1f, 1f);
    private static final Color BTN_BG_IDLE  = new Color(0.15f, 0.15f, 0.25f, 1f);

    // -------------------------------------------------------------------------
    // Map-legend entries: label, R, G, B
    // Terrain colours match CityMap constants; building-category colours are the
    // hex values from assets/text/category_en.json converted to [0,1] floats.
    // Keep these in sync with that file when category colours are changed.
    // -------------------------------------------------------------------------
    private static final Object[][] LEGEND = {
        { "Mountain",        0.4f,   0.35f,  0.3f   },   // CityMap.MOUNTAIN_*
        { "Beach",           0.95f,  0.9f,   0.6f   },   // CityMap.BEACH_*
        { "Residential",     0.298f, 0.686f, 0.314f },   // #4CAF50
        { "Commercial",      0.129f, 0.588f, 0.953f },   // #2196F3
        { "Office",          0.612f, 0.153f, 0.690f },   // #9C27B0
        { "Industrial",      1.0f,   0.757f, 0.027f },   // #FFC107
        { "Infrastructure",  0.376f, 0.490f, 0.545f },   // #607D8B
        { "Medical",         0.957f, 0.263f, 0.212f },   // #F44336
        { "Education",       1.0f,   0.596f, 0.0f   },   // #FF9800
        { "Public Services", 0.0f,   0.737f, 0.831f },   // #00BCD4
        { "Government",      0.247f, 0.318f, 0.710f },   // #3F51B5
        { "Hospitality",     0.914f, 0.118f, 0.388f },   // #E91E63
        { "Entertainment",   1.0f,   0.922f, 0.231f },   // #FFEB3B
        { "Religious",       0.620f, 0.620f, 0.620f },   // #9E9E9E
    };

    // -------------------------------------------------------------------------
    // Controls help lines  (no padding — proportional font can't align colons with spaces)
    // -------------------------------------------------------------------------
    private static final String[] CONTROLS = {
        "Scroll / +- keys: zoom in/out",
        "Drag / Arrow keys: pan map",
        "Click cell: select / info",
        "Double-click cell: move / menu",
        "ESC: quit",
    };

    // -------------------------------------------------------------------------
    // Rendering resources
    // -------------------------------------------------------------------------
    private final SpriteBatch   batch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyphLayout;

    // Close-button bounds (written during draw, read by onTap)
    private float closeBtnX, closeBtnY, closeBtnW, closeBtnH;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------
    HelpPopup(SpriteBatch batch, ShapeRenderer shapeRenderer,
              BitmapFont font, BitmapFont smallFont, GlyphLayout glyphLayout) {
        this.batch         = batch;
        this.shapeRenderer = shapeRenderer;
        this.font          = font;
        this.smallFont     = smallFont;
        this.glyphLayout   = glyphLayout;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    /**
     * Called when the user taps while the popup is visible.
     * Returns {@code true} if the tap hit the close button (caller should hide popup).
     *
     * @param screenX  x coordinate (pixels, left = 0)
     * @param flippedY y coordinate (pixels, bottom = 0 / libGDX convention)
     * @return {@code true} if the "?" close button was tapped
     */
    boolean onTap(int screenX, int flippedY) {
        if (closeBtnW <= 0) return false;
        return screenX >= closeBtnX && screenX <= closeBtnX + closeBtnW
                && flippedY >= closeBtnY && flippedY <= closeBtnY + closeBtnH;
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    /**
     * Draws the popup.  Should be called every frame while the popup is visible,
     * after the map and info-panel have been drawn.
     *
     * @param screenW      screen width in pixels
     * @param screenH      screen height in pixels
     * @param infoAreaH    height of the bottom info panel
     */
    void draw(int screenW, int screenH, int infoAreaH) {

        // --- Measure text ---
        glyphLayout.setText(smallFont, "Hg");
        float smallH    = glyphLayout.height;
        float smallLine = smallH + LINE_GAP;

        glyphLayout.setText(font, "Hg");
        float titleH    = glyphLayout.height;
        float titleLine = titleH + LINE_GAP;

        // Measure close button
        TextMeasurer.TextBounds qBounds =
                TextMeasurer.measure(font, glyphLayout, "?", 14f, 8f);
        float qBtnW = qBounds.width;
        float qBtnH = qBounds.height;

        // Measure widest control line
        float maxControlW = 0f;
        for (String line : CONTROLS) {
            glyphLayout.setText(smallFont, line);
            maxControlW = Math.max(maxControlW, glyphLayout.width);
        }
        // Measure widest legend line (swatch + gap + label)
        float maxLegendW = 0f;
        for (Object[] entry : LEGEND) {
            glyphLayout.setText(smallFont, (String) entry[0]);
            maxLegendW = Math.max(maxLegendW, SWATCH_SIZE + SWATCH_GAP + glyphLayout.width);
        }

        // Section title widths
        glyphLayout.setText(font, "Controls");
        float controlTitleW = glyphLayout.width;
        glyphLayout.setText(font, "Map Legend");
        float legendTitleW = glyphLayout.width;

        // Content width = max of all lines + 2*PAD
        float contentW = Math.max(maxControlW, Math.max(maxLegendW,
                Math.max(controlTitleW, legendTitleW)));
        float popupW = contentW + 2 * PAD;
        // Ensure enough room for close button row
        popupW = Math.max(popupW, qBtnW + 2 * PAD + 4f);
        // Cap at 90% screen width
        popupW = Math.min(popupW, screenW * 0.9f);

        // Content height
        float controlsH   = titleLine                          // "Controls" title
                + CONTROLS.length * smallLine;                  // control lines
        float legendH     = titleLine                          // "Map Legend" title
                + LEGEND.length * (Math.max(SWATCH_SIZE, smallH) + LINE_GAP); // legend rows
        float popupH      = PAD
                + controlsH + SECTION_GAP
                + legendH
                + PAD + qBtnH + PAD;                           // close button at bottom

        // Cap at info panel height (popup overlays the info panel, not the map)
        popupH = Math.min(popupH, infoAreaH - 2 * MARGIN);

        // Anchor: lower-right, within the info panel area (overlays it)
        float popupX = screenW - popupW - MARGIN;
        float popupY = MARGIN;

        // Close button bounds — computed before drawing so clipY can use them
        closeBtnW = qBtnW;
        closeBtnH = qBtnH;
        closeBtnX = popupX + popupW - qBtnW - PAD;
        closeBtnY = popupY + PAD;

        // ---- Shapes: background + close button (no scissor) ----
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BG_COLOR);
        shapeRenderer.rect(popupX, popupY, popupW, popupH);
        shapeRenderer.setColor(BTN_BG_ACTIVE);
        shapeRenderer.rect(closeBtnX, closeBtnY, closeBtnW, closeBtnH);
        shapeRenderer.end();

        // Content clip boundary: just above the close button (prevents content bleeding over it)
        float clipY = closeBtnY + closeBtnH + PAD;
        float clipH = Math.max(0f, popupY + popupH - 1 - clipY);

        // ---- Shapes: colour swatches (scissored to content area) ----
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor((int)(popupX + 1), (int)clipY, (int)(popupW - 2), (int)clipH);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float swatchY = popupY + popupH - PAD - titleLine    // below "Controls" title
                - CONTROLS.length * smallLine - SECTION_GAP  // controls lines + gap
                - titleLine;                                   // "Map Legend" title
        for (Object[] entry : LEGEND) {
            float r = (float) entry[1];
            float g = (float) entry[2];
            float b = (float) entry[3];
            shapeRenderer.setColor(r, g, b, 1f);
            float swatchDrawY = swatchY - (Math.max(SWATCH_SIZE, smallH) - SWATCH_SIZE) / 2f;
            shapeRenderer.rect(popupX + PAD, swatchDrawY, SWATCH_SIZE, SWATCH_SIZE);
            swatchY -= Math.max(SWATCH_SIZE, smallH) + LINE_GAP;
        }
        shapeRenderer.end();

        // Swatch borders (scissored)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        float sbY = popupY + popupH - PAD - titleLine
                - CONTROLS.length * smallLine - SECTION_GAP - titleLine;
        shapeRenderer.setColor(BORDER_COLOR);
        for (int i = 0; i < LEGEND.length; i++) {
            float rowH = Math.max(SWATCH_SIZE, smallH) + LINE_GAP;
            float sd   = sbY - (Math.max(SWATCH_SIZE, smallH) - SWATCH_SIZE) / 2f;
            shapeRenderer.rect(popupX + PAD, sd, SWATCH_SIZE, SWATCH_SIZE);
            sbY -= rowH;
        }
        shapeRenderer.end();

        // ---- Content text (scissored to content area) ----
        batch.begin();

        float ty = popupY + popupH - PAD;

        // --- "Controls" section ---
        font.setColor(TITLE_COLOR);
        glyphLayout.setText(font, "Controls");
        font.draw(batch, "Controls",
                popupX + (popupW - glyphLayout.width) / 2f, ty);
        ty -= titleLine;

        smallFont.setColor(Color.WHITE);
        for (String line : CONTROLS) {
            smallFont.draw(batch, line, popupX + PAD, ty);
            ty -= smallLine;
        }

        ty -= SECTION_GAP;

        // --- "Map Legend" section ---
        font.setColor(TITLE_COLOR);
        glyphLayout.setText(font, "Map Legend");
        font.draw(batch, "Map Legend",
                popupX + (popupW - glyphLayout.width) / 2f, ty);
        ty -= titleLine;

        smallFont.setColor(Color.WHITE);
        for (Object[] entry : LEGEND) {
            float rowH  = Math.max(SWATCH_SIZE, smallH) + LINE_GAP;
            float labelY = ty - (Math.max(SWATCH_SIZE, smallH) - smallH) / 2f;
            smallFont.draw(batch, (String) entry[0],
                    popupX + PAD + SWATCH_SIZE + SWATCH_GAP, labelY);
            ty -= rowH;
        }

        batch.end();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        // ---- Popup and button borders (no scissor) ----
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(BORDER_COLOR);
        shapeRenderer.rect(popupX,     popupY,     popupW,     popupH);
        shapeRenderer.rect(popupX + 1, popupY + 1, popupW - 2, popupH - 2);
        shapeRenderer.rect(closeBtnX,     closeBtnY,     closeBtnW,     closeBtnH);
        shapeRenderer.rect(closeBtnX + 1, closeBtnY + 1, closeBtnW - 2, closeBtnH - 2);
        shapeRenderer.end();

        // ---- Close button label (no scissor — always visible) ----
        batch.begin();
        font.setColor(Color.YELLOW);
        glyphLayout.setText(font, "?");
        font.draw(batch, "?",
                closeBtnX + (closeBtnW - glyphLayout.width) / 2f,
                closeBtnY + (closeBtnH + glyphLayout.height) / 2f);
        batch.end();
    }
}

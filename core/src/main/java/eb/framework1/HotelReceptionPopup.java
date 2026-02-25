package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.List;

/**
 * Modal popup shown when the player "Talks to Reception" at a hotel.
 *
 * <p>Displays the hotel name, room type, nightly rate, and the stamina bonus
 * the hotel provides when sleeping a full 8 hours.  The player chooses between
 * 1, 2, or 3 nights (each costing {@code nights × nightly rate}), or cancels.
 *
 * <h3>Usage (MainScreen)</h3>
 * <pre>
 *   receptionPopup.show(hotelName, roomType, nightly, staminaBonus);
 *   // each frame:
 *   receptionPopup.draw(screenW, screenH);
 *   // on tap:
 *   int nights = receptionPopup.onTap(screenX, flippedY);
 *   if (nights >= 1) handleHotelCheckIn(nights,
 *           receptionPopup.getNightlyCost(), receptionPopup.getStaminaBonus());
 * </pre>
 */
class HotelReceptionPopup {

    private static final Color BG_COLOR      = new Color(0.06f, 0.12f, 0.22f, 1f);
    private static final Color BORDER_COLOR  = new Color(0.70f, 0.85f, 1.00f, 1f);
    private static final Color TITLE_COLOR   = new Color(1.00f, 0.90f, 0.50f, 1f);
    private static final Color LABEL_COLOR   = new Color(0.60f, 0.85f, 1.00f, 1f);
    private static final Color OPTION_COLOR  = new Color(0.10f, 0.45f, 0.12f, 1f);
    private static final Color CANCEL_COLOR  = new Color(0.40f, 0.10f, 0.10f, 1f);

    private static final int   NUM_OPTIONS   = 3;
    private static final int[] OPTION_NIGHTS = { 1, 2, 3 };

    // --- Rendering resources ---
    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyph;

    // --- State ---
    private boolean visible      = false;
    private String  hotelName    = "";
    private String  roomType     = "";
    private String  roomDesc     = "";
    private int     nightly      = 0;
    private int     staminaBonus = 0;

    // Button bounds (written during draw, read by onTap)
    private final float[] optBtnX = new float[NUM_OPTIONS];
    private final float[] optBtnY = new float[NUM_OPTIONS];
    private final float[] optBtnW = new float[NUM_OPTIONS];
    private float          optBtnH  = 0f;
    private float cancelX, cancelY, cancelW, cancelH;

    // -------------------------------------------------------------------------

    HotelReceptionPopup(SpriteBatch batch, ShapeRenderer sr,
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

    boolean isVisible()      { return visible; }
    int     getNightlyCost() { return nightly; }
    int     getStaminaBonus(){ return staminaBonus; }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    /**
     * Opens the popup for the given hotel.
     *
     * @param hotelName    display name of the hotel building
     * @param roomType     e.g. "Budget Room", "Comfortable Room", "Luxury Suite"
     * @param nightly      nightly rate in in-game currency
     * @param staminaBonus extra stamina added on a full 8-hour sleep while checked in
     */
    void show(String hotelName, String roomType, int nightly, int staminaBonus) {
        show(hotelName, roomType, nightly, staminaBonus, "");
    }

    /**
     * Opens the popup for the given hotel with an optional room description.
     *
     * @param hotelName    display name of the hotel building
     * @param roomType     e.g. "Budget Room", "Comfortable Room", "Luxury Suite"
     * @param nightly      nightly rate in in-game currency
     * @param staminaBonus extra stamina added on a full 8-hour sleep while checked in
     * @param roomDesc     contextual room description from {@code description_en.json}; may be null
     */
    void show(String hotelName, String roomType, int nightly, int staminaBonus, String roomDesc) {
        this.hotelName    = hotelName    != null ? hotelName    : "Hotel";
        this.roomType     = roomType     != null ? roomType     : "Standard Room";
        this.nightly      = nightly;
        this.staminaBonus = staminaBonus;
        this.roomDesc     = roomDesc     != null ? roomDesc     : "";
        this.visible      = true;
        this.optBtnH      = 0f;
        this.cancelH      = 0f;
        Gdx.app.log("HotelReceptionPopup", "Showing " + hotelName
                + " nightly=$" + nightly + " bonus=+" + staminaBonus);
    }

    /**
     * Handles a tap on this popup.
     *
     * @return the number of nights selected (1, 2, or 3) when an option button
     *         was tapped; {@code -1} when Cancel was tapped (popup auto-dismissed);
     *         {@code 0} when no button was hit (popup stays open).
     */
    int onTap(int screenX, int flippedY) {
        if (!visible) return 0;

        // Night option buttons
        for (int i = 0; i < NUM_OPTIONS; i++) {
            if (optBtnW[i] > 0
                    && screenX >= optBtnX[i] && screenX <= optBtnX[i] + optBtnW[i]
                    && flippedY >= optBtnY[i] && flippedY <= optBtnY[i] + optBtnH) {
                visible = false;
                return OPTION_NIGHTS[i];
            }
        }
        // Cancel
        if (cancelW > 0
                && screenX >= cancelX && screenX <= cancelX + cancelW
                && flippedY >= cancelY && flippedY <= cancelY + cancelH) {
            visible = false;
            return -1;
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    void draw(int screenW, int screenH) {
        if (!visible) return;

        final float PAD      = 24f;
        final float GAP      = 10f;
        final float BTN_SPACING = 12f;
        final float MIN_W    = 320f;
        final float MAX_W    = screenW * 0.85f;

        // --- Font metrics ---
        glyph.setText(font, "Hg");
        float fontH     = glyph.height;
        float fontLineH = fontH + GAP;
        glyph.setText(smallFont, "Hg");
        float smallH     = glyph.height;
        float smallLineH = smallH + GAP;

        // --- Button sizing ---
        TextMeasurer.TextBounds cancelBounds = TextMeasurer.measure(font, glyph, "Cancel", 28f, 12f);
        float cancelBtnW = cancelBounds.width;
        float cancelBtnH = cancelBounds.height;

        // Measure option button labels: "1 Night  ($XX)"
        float maxOptW = cancelBtnW;
        for (int n : OPTION_NIGHTS) {
            String lbl = optionLabel(n);
            TextMeasurer.TextBounds b = TextMeasurer.measure(font, glyph, lbl, 28f, 12f);
            if (b.width > maxOptW) maxOptW = b.width;
        }
        float optW = maxOptW;
        float optH = cancelBtnH;

        optBtnH = optH;

        // --- Content lines ---
        String titleLine   = hotelName + " — Reception";
        String roomLine    = "Room:       " + roomType;
        String rateLine    = "Nightly:    $" + nightly;
        String bonusLine   = "Sleep bonus: +" + staminaBonus + " stamina (full 8h)";

        // Width from widest line
        float maxLineW = 0f;
        for (String ln : new String[]{ titleLine, roomLine, rateLine, bonusLine }) {
            glyph.setText(ln.startsWith(hotelName) ? font : smallFont, ln);
            if (glyph.width > maxLineW) maxLineW = glyph.width;
        }
        maxLineW = Math.max(maxLineW, optW);

        float dialogW = Math.min(MAX_W, Math.max(MIN_W, maxLineW + 2 * PAD));

        // Compute room description lines (word-wrapped to fit dialogW - 2*PAD)
        float descAreaW = dialogW - 2 * PAD;
        final BitmapFont descFont = smallFont;
        List<String> descLines = roomDesc != null && !roomDesc.isEmpty()
                ? WordWrapper.wrap(roomDesc, descAreaW, t -> { glyph.setText(descFont, t); return glyph.width; })
                : java.util.Collections.<String>emptyList();
        float descH = descLines.isEmpty() ? 0f : descLines.size() * smallLineH + GAP;

        // Height: PAD + title + char-size gap + 3 info lines + desc + spacer + 3 option btns + cancel + PAD
        float dialogH = PAD
                + fontLineH + fontH     // title + character-size gap
                + 3 * smallLineH        // room / rate / bonus
                + descH                 // optional room description
                + GAP                   // spacer before buttons
                + NUM_OPTIONS * (optH + BTN_SPACING)
                + cancelBtnH + BTN_SPACING
                + PAD;

        float dialogX = (screenW - dialogW) / 2f;
        float dialogY = (screenH - dialogH) / 2f;

        // --- Layout buttons bottom-up ---
        float cy = dialogY + PAD;
        cancelX = dialogX + (dialogW - cancelBtnW) / 2f;
        cancelY = cy;
        cancelW = cancelBtnW;
        cancelH = cancelBtnH;
        cy += cancelBtnH + BTN_SPACING;

        for (int i = NUM_OPTIONS - 1; i >= 0; i--) {
            optBtnX[i] = dialogX + (dialogW - optW) / 2f;
            optBtnY[i] = cy;
            optBtnW[i] = optW;
            cy += optH + BTN_SPACING;
        }

        // --- Shapes ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(dialogX, dialogY, dialogW, dialogH);
        // Option buttons
        sr.setColor(OPTION_COLOR);
        for (int i = 0; i < NUM_OPTIONS; i++) {
            sr.rect(optBtnX[i], optBtnY[i], optBtnW[i], optBtnH);
        }
        // Cancel button
        sr.setColor(CANCEL_COLOR);
        sr.rect(cancelX, cancelY, cancelW, cancelH);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(dialogX,     dialogY,     dialogW,     dialogH);
        sr.rect(dialogX + 1, dialogY + 1, dialogW - 2, dialogH - 2);
        for (int i = 0; i < NUM_OPTIONS; i++) {
            sr.rect(optBtnX[i],     optBtnY[i],     optBtnW[i],     optBtnH);
            sr.rect(optBtnX[i] + 1, optBtnY[i] + 1, optBtnW[i] - 2, optBtnH - 2);
        }
        sr.rect(cancelX,     cancelY,     cancelW,     cancelH);
        sr.rect(cancelX + 1, cancelY + 1, cancelW - 2, cancelH - 2);
        sr.end();

        // --- Text ---
        batch.begin();
        float ty = dialogY + dialogH - PAD - fontH;

        // Title
        font.setColor(TITLE_COLOR);
        glyph.setText(font, titleLine);
        font.draw(batch, titleLine, dialogX + (dialogW - glyph.width) / 2f, ty);
        ty -= fontLineH + fontH;

        // Info lines
        for (String ln : new String[]{ roomLine, rateLine, bonusLine }) {
            glyph.setText(smallFont, ln);
            smallFont.setColor(LABEL_COLOR);
            smallFont.draw(batch, ln, dialogX + PAD, ty);
            ty -= smallLineH;
        }

        // Room description (italic-style dimmed colour)
        if (!descLines.isEmpty()) {
            smallFont.setColor(new Color(0.75f, 0.85f, 0.95f, 1f));
            for (String line : descLines) {
                smallFont.draw(batch, line, dialogX + PAD, ty);
                ty -= smallLineH;
            }
            ty -= GAP;
        }

        // Option button labels
        for (int i = 0; i < NUM_OPTIONS; i++) {
            String lbl = optionLabel(OPTION_NIGHTS[i]);
            glyph.setText(font, lbl);
            font.setColor(Color.WHITE);
            font.draw(batch, lbl,
                    optBtnX[i] + (optBtnW[i] - glyph.width) / 2f,
                    optBtnY[i] + (optBtnH + glyph.height) / 2f);
        }

        // Cancel label
        glyph.setText(font, "Cancel");
        font.setColor(Color.WHITE);
        font.draw(batch, "Cancel",
                cancelX + (cancelW - glyph.width) / 2f,
                cancelY + (cancelH + glyph.height) / 2f);

        batch.end();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private String optionLabel(int nights) {
        int total = nights * nightly;
        return nights + (nights == 1 ? " Night" : " Nights") + "  ($" + total + ")";
    }
}

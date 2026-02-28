package eb.framework1;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Custom in-game popup for adding a case-file note.
 *
 * <p>Shows a title, two toggle-able checkboxes ("Include current time" and
 * "Include current location"), a "Write Note" action button, and a "Cancel"
 * button.  The caller shows the OS text-input dialog only after the user taps
 * "Write Note", so the checkbox selections are available up front.
 *
 * <p>Usage:
 * <pre>{@code
 * // Show the popup, seeding checkbox state from saved prefs
 * notePopup.show(state.noteIncludeTime, state.noteIncludeLocation);
 *
 * // In touchUp / render loop:
 * int r = notePopup.onTap(screenX, flippedY);
 * if (r == NotePopup.RESULT_CONFIRM) {
 *     // user tapped "Write Note"
 *     boolean time = notePopup.isIncludeTime();
 *     boolean loc  = notePopup.isIncludeLocation();
 *     // … open Gdx.input.getTextInput(…)
 * }
 * }</pre>
 */
class NotePopup {

    static final int RESULT_NONE    = -1;
    static final int RESULT_CANCEL  =  0;
    static final int RESULT_CONFIRM =  1;

    private static final Color BG_COLOR          = new Color(0.08f, 0.08f, 0.18f, 1f);
    private static final Color BORDER_COLOR      = new Color(0.80f, 0.60f, 0.20f, 1f);
    private static final Color TITLE_COLOR       = new Color(1.00f, 0.80f, 0.20f, 1f);
    private static final Color CONFIRM_BTN_COLOR = new Color(0.15f, 0.35f, 0.15f, 1f);
    private static final Color CANCEL_BTN_COLOR  = new Color(0.35f, 0.15f, 0.15f, 1f);
    private static final Color CB_FILL_COLOR     = new Color(0.55f, 0.75f, 1.00f, 1f);
    private static final Color CB_BORDER_COLOR   = new Color(0.60f, 0.60f, 0.70f, 1f);

    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyph;

    private boolean visible         = false;
    private boolean includeTime     = true;
    private boolean includeLocation = true;

    // Hit areas written during draw(), read by onTap()
    private float confirmX, confirmY, confirmW, confirmH;
    private float cancelX,  cancelY,  cancelW,  cancelH;
    private float timeCbX,  timeCbY,  timeCbW,  timeCbH;
    private float locCbX,   locCbY,   locCbW,   locCbH;

    NotePopup(SpriteBatch batch, ShapeRenderer sr,
              BitmapFont font, BitmapFont smallFont, GlyphLayout glyph) {
        this.batch     = batch;
        this.sr        = sr;
        this.font      = font;
        this.smallFont = smallFont;
        this.glyph     = glyph;
    }

    boolean isVisible()         { return visible; }
    boolean isIncludeTime()     { return includeTime; }
    boolean isIncludeLocation() { return includeLocation; }

    /** Opens the popup, seeding the checkboxes with their last-saved values. */
    void show(boolean includeTime, boolean includeLocation) {
        this.includeTime     = includeTime;
        this.includeLocation = includeLocation;
        this.visible         = true;
        this.confirmW        = 0f; // invalidate hit areas until first draw()
    }

    void dismiss() { visible = false; }

    /**
     * Handles a tap.  Returns {@link #RESULT_CONFIRM}, {@link #RESULT_CANCEL},
     * or {@link #RESULT_NONE}.  Toggling a checkbox returns {@link #RESULT_NONE}
     * so the caller can re-draw without further action.  Dismisses the popup on
     * Confirm or Cancel.
     */
    int onTap(int screenX, int flippedY) {
        if (!visible || confirmW <= 0f) return RESULT_NONE;

        // Time checkbox toggle
        if (screenX >= timeCbX && screenX <= timeCbX + timeCbW
                && flippedY >= timeCbY && flippedY <= timeCbY + timeCbH) {
            includeTime = !includeTime;
            return RESULT_NONE;
        }
        // Location checkbox toggle
        if (screenX >= locCbX && screenX <= locCbX + locCbW
                && flippedY >= locCbY && flippedY <= locCbY + locCbH) {
            includeLocation = !includeLocation;
            return RESULT_NONE;
        }
        // Confirm button
        if (screenX >= confirmX && screenX <= confirmX + confirmW
                && flippedY >= confirmY && flippedY <= confirmY + confirmH) {
            visible = false;
            return RESULT_CONFIRM;
        }
        // Cancel button
        if (screenX >= cancelX && screenX <= cancelX + cancelW
                && flippedY >= cancelY && flippedY <= cancelY + cancelH) {
            visible = false;
            return RESULT_CANCEL;
        }
        return RESULT_NONE;
    }

    void draw(int screenW, int screenH) {
        if (!visible) return;

        final float PAD      = 24f;
        final float GAP      = 12f;
        final float CB_SIZE  = 16f;
        final float CB_GAP   = 8f;   // gap between checkbox box and its label
        final float ROW_GAP  = 10f;  // gap between successive rows
        final float BTN_PAD_H = 24f;
        final float BTN_PAD_V = 10f;
        final float BTN_SPACING = 20f;

        // ── Measure text ─────────────────────────────────────────────────────
        glyph.setText(font, "Hg");
        float fontH    = glyph.height;
        float fontLineH = fontH + ROW_GAP;

        glyph.setText(smallFont, "Hg");
        float smallH = glyph.height;

        final String TITLE_TEXT   = "Add Note";
        final String TIME_LABEL   = "Include current time";
        final String LOC_LABEL    = "Include current location";
        final String CONFIRM_TEXT = "Write Note";
        final String CANCEL_TEXT  = "Cancel";

        TextMeasurer.TextBounds confirmBounds =
                TextMeasurer.measure(font, glyph, CONFIRM_TEXT, BTN_PAD_H, BTN_PAD_V);
        TextMeasurer.TextBounds cancelBounds  =
                TextMeasurer.measure(font, glyph, CANCEL_TEXT,  BTN_PAD_H, BTN_PAD_V);

        float btnH_ = Math.max(confirmBounds.height, cancelBounds.height);
        float confirmW_ = confirmBounds.width;
        float cancelW_  = cancelBounds.width;

        glyph.setText(smallFont, TIME_LABEL);
        float timeLabelW = glyph.width;
        glyph.setText(smallFont, LOC_LABEL);
        float locLabelW  = glyph.width;

        // Width needed for each checkbox row: box + gap + label
        float timeRowW = CB_SIZE + CB_GAP + timeLabelW;
        float locRowW  = CB_SIZE + CB_GAP + locLabelW;

        // Width needed for the buttons row
        float btnRowW = confirmW_ + BTN_SPACING + cancelW_;

        glyph.setText(font, TITLE_TEXT);
        float titleW = glyph.width;

        float contentW_ = Math.max(titleW, Math.max(Math.max(timeRowW, locRowW), btnRowW));
        float dialogW   = Math.min(screenW * 0.90f, contentW_ + PAD * 2f);
        float contentW  = dialogW - PAD * 2f;

        // Height: PAD + title + GAP + timeRow + ROW_GAP + locRow + GAP + buttons + PAD
        float cbRowH = Math.max(CB_SIZE, smallH);
        float dialogH = PAD + fontH + GAP
                + cbRowH + ROW_GAP
                + cbRowH + GAP
                + btnH_ + PAD;

        float dialogX = (screenW - dialogW) / 2f;
        float dialogY = (screenH - dialogH) / 2f;

        // Layout (y increases upward in libGDX)
        float btnRowY  = dialogY + PAD;
        float locRowY  = btnRowY + btnH_ + GAP;
        float timeRowY = locRowY + cbRowH + ROW_GAP;

        // Buttons horizontally centred
        float totalBtnW = confirmW_ + BTN_SPACING + cancelW_;
        float btnStartX = dialogX + (dialogW - totalBtnW) / 2f;

        confirmX = btnStartX;                      confirmY = btnRowY; confirmW = confirmW_; confirmH = btnH_;
        cancelX  = btnStartX + confirmW_ + BTN_SPACING; cancelY = btnRowY; cancelW  = cancelW_;  cancelH  = btnH_;

        // Checkbox hit boxes (tap anywhere in the CB_SIZE square)
        timeCbX = dialogX + PAD; timeCbY = timeRowY + (cbRowH - CB_SIZE) / 2f; timeCbW = CB_SIZE; timeCbH = CB_SIZE;
        locCbX  = dialogX + PAD; locCbY  = locRowY  + (cbRowH - CB_SIZE) / 2f; locCbW  = CB_SIZE; locCbH  = CB_SIZE;

        // ── Draw background + buttons ─────────────────────────────────────────
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(dialogX, dialogY, dialogW, dialogH);
        sr.setColor(CONFIRM_BTN_COLOR);
        sr.rect(confirmX, confirmY, confirmW, confirmH);
        sr.setColor(CANCEL_BTN_COLOR);
        sr.rect(cancelX, cancelY, cancelW, cancelH);
        // Time checkbox fill
        if (includeTime) {
            sr.setColor(CB_FILL_COLOR);
            float inset = 3f;
            sr.rect(timeCbX + inset, timeCbY + inset, CB_SIZE - inset * 2f, CB_SIZE - inset * 2f);
        }
        // Location checkbox fill
        if (includeLocation) {
            sr.setColor(CB_FILL_COLOR);
            float inset = 3f;
            sr.rect(locCbX + inset, locCbY + inset, CB_SIZE - inset * 2f, CB_SIZE - inset * 2f);
        }
        sr.end();

        // ── Draw borders ─────────────────────────────────────────────────────
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(dialogX,     dialogY,     dialogW,     dialogH);
        sr.rect(dialogX + 1, dialogY + 1, dialogW - 2, dialogH - 2);
        sr.rect(confirmX, confirmY, confirmW, confirmH);
        sr.rect(cancelX,  cancelY,  cancelW,  cancelH);
        sr.setColor(CB_BORDER_COLOR);
        sr.rect(timeCbX, timeCbY, CB_SIZE, CB_SIZE);
        sr.rect(locCbX,  locCbY,  CB_SIZE, CB_SIZE);
        sr.end();

        // ── Draw text ─────────────────────────────────────────────────────────
        batch.begin();

        // Title
        glyph.setText(font, TITLE_TEXT);
        font.setColor(TITLE_COLOR);
        font.draw(batch, TITLE_TEXT,
                dialogX + (dialogW - glyph.width) / 2f,
                dialogY + dialogH - PAD);

        // Checkbox labels (vertically centred on the checkbox row)
        smallFont.setColor(Color.WHITE);
        smallFont.draw(batch, TIME_LABEL,
                timeCbX + CB_SIZE + CB_GAP,
                timeCbY + CB_SIZE - 2f);
        smallFont.draw(batch, LOC_LABEL,
                locCbX + CB_SIZE + CB_GAP,
                locCbY + CB_SIZE - 2f);

        // Button labels
        font.setColor(Color.WHITE);
        glyph.setText(font, CONFIRM_TEXT);
        font.draw(batch, CONFIRM_TEXT,
                confirmX + (confirmW - glyph.width) / 2f,
                confirmY + (confirmH + glyph.height) / 2f);
        glyph.setText(font, CANCEL_TEXT);
        font.draw(batch, CANCEL_TEXT,
                cancelX + (cancelW - glyph.width) / 2f,
                cancelY + (cancelH + glyph.height) / 2f);

        batch.end();
    }
}

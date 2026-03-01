package eb.framework1;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.Collections;
import java.util.List;

/**
 * Custom in-game popup for adding a case-file note.
 *
 * <p>Shows a title, an inline multi-line text area (~4 lines, full popup width),
 * two toggle-able checkboxes ("Include current time" / "Include current location"),
 * a "Save" button and a "Cancel" button.
 *
 * <p>Keyboard input is routed here by MainScreen via {@link #keyTyped(char)} and
 * {@link #keyDown(int)}.  Call {@link #update(float)} each frame for cursor blinking.
 *
 * <p>Usage:
 * <pre>{@code
 * notePopup.show(state.noteIncludeTime, state.noteIncludeLocation);
 * Gdx.input.setOnscreenKeyboardVisible(true);
 *
 * // each frame
 * notePopup.update(delta);
 *
 * // in touchUp
 * int r = notePopup.onTap(screenX, flippedY);
 * if (r == NotePopup.RESULT_CONFIRM) {
 *     String text = notePopup.getNoteText();
 *     // … append time/location prefix and add to case file
 * }
 * }</pre>
 */
class NotePopup {

    static final int RESULT_NONE    = -1;
    static final int RESULT_CANCEL  =  0;
    static final int RESULT_CONFIRM =  1;

    private static final int TEXT_AREA_LINES = 4;

    private static final Color BG_COLOR          = new Color(0.08f, 0.08f, 0.18f, 1f);
    private static final Color BORDER_COLOR      = new Color(0.80f, 0.60f, 0.20f, 1f);
    private static final Color TITLE_COLOR       = new Color(1.00f, 0.80f, 0.20f, 1f);
    private static final Color CONFIRM_BTN_COLOR = new Color(0.15f, 0.35f, 0.15f, 1f);
    private static final Color CANCEL_BTN_COLOR  = new Color(0.35f, 0.15f, 0.15f, 1f);
    private static final Color CB_FILL_COLOR     = new Color(0.55f, 0.75f, 1.00f, 1f);
    private static final Color CB_BORDER_COLOR   = new Color(0.60f, 0.60f, 0.70f, 1f);
    private static final Color TEXT_AREA_BG      = new Color(0.04f, 0.04f, 0.12f, 1f);
    private static final Color CURSOR_COLOR      = new Color(0.90f, 0.90f, 0.90f, 1f);
    private static final Color PLACEHOLDER_COLOR = new Color(0.45f, 0.45f, 0.55f, 1f);

    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyph;

    private boolean       visible         = false;
    private boolean       includeTime     = true;
    private boolean       includeLocation = true;
    private final StringBuilder noteText  = new StringBuilder();

    // Cursor blink
    private float   cursorTimer   = 0f;
    private boolean cursorVisible = true;
    private static final float CURSOR_BLINK_PERIOD = 0.5f;

    // Hit areas written during draw(), read by onTap()
    private float confirmX, confirmY, confirmW, confirmH;
    private float cancelX,  cancelY,  cancelW,  cancelH;
    private float timeCbX,  timeCbY,  timeCbW,  timeCbH;
    private float locCbX,   locCbY,   locCbW,   locCbH;

    // Text-area bounds (stored so we can draw the cursor)
    private float textAreaX, textAreaY, textAreaW, textAreaH;
    private float textPad;

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
    /** Returns the trimmed text the user has typed. */
    String  getNoteText()       { return noteText.toString().trim(); }

    /** Opens the popup, seeding checkboxes and clearing the text area. */
    void show(boolean includeTime, boolean includeLocation) {
        this.includeTime     = includeTime;
        this.includeLocation = includeLocation;
        this.visible         = true;
        this.confirmW        = 0f; // invalidate hit areas until first draw()
        this.noteText.setLength(0);
        this.cursorTimer     = 0f;
        this.cursorVisible   = true;
    }

    void dismiss() { visible = false; }

    /** Advances cursor blink timer; call once per frame. */
    void update(float delta) {
        if (!visible) return;
        cursorTimer += delta;
        if (cursorTimer >= CURSOR_BLINK_PERIOD) {
            cursorTimer  -= CURSOR_BLINK_PERIOD;
            cursorVisible = !cursorVisible;
        }
    }

    /**
     * Handles a printable character or backspace typed by the user.
     * @return {@code true} if the event was consumed.
     */
    boolean keyTyped(char c) {
        if (!visible) return false;
        if (c == '\b') {
            if (noteText.length() > 0) noteText.deleteCharAt(noteText.length() - 1);
            cursorVisible = true; cursorTimer = 0f;
            return true;
        }
        if (c >= 32 && c != 127) { // printable, exclude DEL
            noteText.append(c);
            cursorVisible = true; cursorTimer = 0f;
            return true;
        }
        return false;
    }

    /**
     * Handles key-down events (for Delete key).
     * @return {@code true} if the event was consumed.
     */
    boolean keyDown(int keyCode) {
        if (!visible) return false;
        if (keyCode == Input.Keys.FORWARD_DEL) {
            if (noteText.length() > 0) noteText.deleteCharAt(noteText.length() - 1);
            cursorVisible = true; cursorTimer = 0f;
            return true;
        }
        return false;
    }

    /**
     * Handles a tap.  Returns {@link #RESULT_CONFIRM}, {@link #RESULT_CANCEL},
     * or {@link #RESULT_NONE}.  Toggling a checkbox returns {@link #RESULT_NONE}.
     * Dismisses the popup on Save or Cancel.
     */
    int onTap(int screenX, int flippedY) {
        if (!visible || confirmW <= 0f) return RESULT_NONE;

        if (screenX >= timeCbX && screenX <= timeCbX + timeCbW
                && flippedY >= timeCbY && flippedY <= timeCbY + timeCbH) {
            includeTime = !includeTime;
            return RESULT_NONE;
        }
        if (screenX >= locCbX && screenX <= locCbX + locCbW
                && flippedY >= locCbY && flippedY <= locCbY + locCbH) {
            includeLocation = !includeLocation;
            return RESULT_NONE;
        }
        if (screenX >= confirmX && screenX <= confirmX + confirmW
                && flippedY >= confirmY && flippedY <= confirmY + confirmH) {
            visible = false;
            return RESULT_CONFIRM;
        }
        if (screenX >= cancelX && screenX <= cancelX + cancelW
                && flippedY >= cancelY && flippedY <= cancelY + cancelH) {
            visible = false;
            return RESULT_CANCEL;
        }
        return RESULT_NONE;
    }

    void draw(int screenW, int screenH) {
        if (!visible) return;

        final float PAD         = 16f;
        final float GAP         = 10f;
        final float BTN_TOP_GAP = 20f; // extra space above Save/Cancel buttons
        final float CB_SIZE     = 16f;
        final float CB_GAP      = 8f;
        final float ROW_GAP     = 8f;
        final float BTN_PAD_H   = 22f;
        final float BTN_PAD_V   = 10f;
        final float BTN_SPACING = 20f;
        final float TEXT_PAD    = 8f;  // inner padding inside text area

        // ── Measure metrics ──────────────────────────────────────────────────
        glyph.setText(font, "Hg");
        float fontH     = glyph.height;

        glyph.setText(smallFont, "Hg");
        float smallH    = glyph.height;
        float smallLineH = smallH * 1.35f;

        final String TITLE_TEXT   = "Add Note";
        final String TIME_LABEL   = "Include current time";
        final String LOC_LABEL    = "Include current location";
        final String CONFIRM_TEXT = "Save";
        final String CANCEL_TEXT  = "Cancel";

        TextMeasurer.TextBounds confirmBounds =
                TextMeasurer.measure(font, glyph, CONFIRM_TEXT, BTN_PAD_H, BTN_PAD_V);
        TextMeasurer.TextBounds cancelBounds  =
                TextMeasurer.measure(font, glyph, CANCEL_TEXT,  BTN_PAD_H, BTN_PAD_V);

        float btnH_     = Math.max(confirmBounds.height, cancelBounds.height);
        float confirmW_ = confirmBounds.width;
        float cancelW_  = cancelBounds.width;

        // Popup uses almost full screen width so the text area has maximum space
        float dialogW   = screenW * 0.96f;
        float contentW  = dialogW - PAD * 2f;

        // Text area: full content width, TEXT_AREA_LINES lines tall
        float textAreaH_ = TEXT_AREA_LINES * smallLineH + TEXT_PAD * 2f;

        // Checkbox row height
        float cbRowH = Math.max(CB_SIZE, smallH);

        // Total dialog height (BTN_TOP_GAP gives breathing room above Save/Cancel)
        float dialogH = PAD + fontH + GAP
                + textAreaH_ + GAP
                + cbRowH + ROW_GAP
                + cbRowH + BTN_TOP_GAP
                + btnH_ + PAD;

        float dialogX = (screenW - dialogW) / 2f;
        float dialogY = (screenH - dialogH) / 2f;

        // ── Layout (y increases upward in libGDX) ────────────────────────────
        float btnRowY   = dialogY + PAD;
        float locRowY   = btnRowY  + btnH_    + BTN_TOP_GAP;
        float timeRowY  = locRowY  + cbRowH   + ROW_GAP;
        float taY       = timeRowY + cbRowH   + GAP;

        textAreaX = dialogX + PAD;
        textAreaY = taY;
        textAreaW = contentW;
        textAreaH = textAreaH_;
        textPad   = TEXT_PAD;

        // Buttons centred
        float totalBtnW = confirmW_ + BTN_SPACING + cancelW_;
        float btnStartX = dialogX + (dialogW - totalBtnW) / 2f;
        confirmX = btnStartX;
        confirmY = btnRowY; confirmW = confirmW_; confirmH = btnH_;
        cancelX  = btnStartX + confirmW_ + BTN_SPACING;
        cancelY  = btnRowY;  cancelW  = cancelW_;  cancelH  = btnH_;

        // Checkbox hit boxes
        timeCbX = dialogX + PAD; timeCbY = timeRowY + (cbRowH - CB_SIZE) / 2f;
        timeCbW = CB_SIZE;       timeCbH = CB_SIZE;
        locCbX  = dialogX + PAD; locCbY  = locRowY  + (cbRowH - CB_SIZE) / 2f;
        locCbW  = CB_SIZE;       locCbH  = CB_SIZE;

        // ── Pre-compute text-area content (needed for cursor position) ────────
        float taWrapW = textAreaW - TEXT_PAD * 2f;
        String typed  = noteText.toString();
        // Wrapped lines computed once; reused for both drawing and cursor coords
        List<String> wrappedLines = typed.isEmpty()
                ? Collections.singletonList("")
                : WordWrapper.wrap(typed, taWrapW, t -> {
                    glyph.setText(smallFont, t);
                    return glyph.width;
                  });

        // Cursor position (computed before any render calls to avoid sr/batch mixing)
        boolean drawCursor = cursorVisible;
        float cursorRectX = 0f, cursorRectY = 0f;
        if (drawCursor) {
            int lastIdx = Math.min(wrappedLines.size() - 1, TEXT_AREA_LINES - 1);
            if (lastIdx == wrappedLines.size() - 1) {
                String lastLine = wrappedLines.get(lastIdx);
                glyph.setText(smallFont, lastLine);
                cursorRectX = textAreaX + TEXT_PAD + (typed.isEmpty() ? 0f : glyph.width) + 1f;
                cursorRectY = textAreaY + textAreaH - TEXT_PAD - lastIdx * smallLineH - smallH;
            } else {
                drawCursor = false; // text overflows visible area; suppress cursor
            }
        }

        // ── Draw filled rects (ShapeRenderer only — batch not active) ────────
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(dialogX, dialogY, dialogW, dialogH);
        sr.setColor(TEXT_AREA_BG);
        sr.rect(textAreaX, textAreaY, textAreaW, textAreaH);
        sr.setColor(CONFIRM_BTN_COLOR);
        sr.rect(confirmX, confirmY, confirmW, confirmH);
        sr.setColor(CANCEL_BTN_COLOR);
        sr.rect(cancelX, cancelY, cancelW, cancelH);
        if (includeTime) {
            sr.setColor(CB_FILL_COLOR);
            float inset = 3f;
            sr.rect(timeCbX + inset, timeCbY + inset, CB_SIZE - inset * 2f, CB_SIZE - inset * 2f);
        }
        if (includeLocation) {
            sr.setColor(CB_FILL_COLOR);
            float inset = 3f;
            sr.rect(locCbX + inset, locCbY + inset, CB_SIZE - inset * 2f, CB_SIZE - inset * 2f);
        }
        // Blinking cursor drawn here with sr, BEFORE batch.begin(), to avoid gl state corruption
        if (drawCursor) {
            sr.setColor(CURSOR_COLOR);
            sr.rect(cursorRectX, cursorRectY, 2f, smallH);
        }
        sr.end();

        // ── Draw borders (ShapeRenderer only — batch not active) ─────────────
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(dialogX,     dialogY,     dialogW,     dialogH);
        sr.rect(dialogX + 1, dialogY + 1, dialogW - 2, dialogH - 2);
        sr.rect(textAreaX, textAreaY, textAreaW, textAreaH);
        sr.rect(confirmX, confirmY, confirmW, confirmH);
        sr.rect(cancelX,  cancelY,  cancelW,  cancelH);
        sr.setColor(CB_BORDER_COLOR);
        sr.rect(timeCbX, timeCbY, CB_SIZE, CB_SIZE);
        sr.rect(locCbX,  locCbY,  CB_SIZE, CB_SIZE);
        sr.end();

        // ── Draw all text (SpriteBatch only — ShapeRenderer not active) ───────
        batch.begin();

        // Title
        glyph.setText(font, TITLE_TEXT);
        font.setColor(TITLE_COLOR);
        font.draw(batch, TITLE_TEXT,
                dialogX + (dialogW - glyph.width) / 2f,
                dialogY + dialogH - PAD);

        // Text area content
        if (typed.isEmpty()) {
            smallFont.setColor(PLACEHOLDER_COLOR);
            smallFont.draw(batch, "Type your note here\u2026",
                    textAreaX + TEXT_PAD,
                    textAreaY + textAreaH - TEXT_PAD);
        } else {
            smallFont.setColor(Color.WHITE);
            float ty = textAreaY + textAreaH - TEXT_PAD;
            for (int i = 0; i < wrappedLines.size() && i < TEXT_AREA_LINES; i++) {
                smallFont.draw(batch, wrappedLines.get(i), textAreaX + TEXT_PAD, ty);
                ty -= smallLineH;
            }
        }

        // Checkbox labels
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

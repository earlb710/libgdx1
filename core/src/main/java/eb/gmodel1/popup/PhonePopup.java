package eb.gmodel1.popup;

import eb.gmodel1.phone.*;
import eb.gmodel1.screen.*;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Modal popup that displays the player's phone contact list.
 *
 * <p>Contacts are derived from the active profile's open case files: each open
 * case contributes its client name and subject name as contacts.  Contacts from
 * open cases are marked with a star (★).  When a case ends the associated
 * contacts are no longer shown.
 *
 * <p>Each contact row may be tapped to record a phone call (marked
 * "CALLED").  A round power button at the bottom dismisses the popup.
 *
 * <h3>Usage (MainScreen)</h3>
 * <pre>
 *   phonePopup.show(buildPhoneContacts(profile));
 *   // each frame:
 *   phonePopup.draw(screenW, screenH);
 *   // on tap:
 *   int result = phonePopup.onTap(screenX, flippedY);
 *   if (result >= 0) handleContactPhoned(result);
 * </pre>
 */
public class PhonePopup {

    /** Returned by {@link #onTap} when the power button was tapped. */
    public static final int RESULT_CLOSED = -1;
    /** Returned by {@link #onTap} when no interactive area was hit. */
    public static final int RESULT_MISS   = -2;

    // ---- Colours ----
    private static final Color BG_COLOR        = new Color(0.05f, 0.05f, 0.12f, 1f);
    private static final Color BORDER_COLOR    = new Color(0.35f, 0.35f, 0.55f, 1f);
    private static final Color TITLE_COLOR     = new Color(0.75f, 0.85f, 1.00f, 1f);
    private static final Color ROW_EVEN        = new Color(0.10f, 0.10f, 0.20f, 1f);
    private static final Color ROW_ODD         = new Color(0.07f, 0.07f, 0.15f, 1f);
    private static final Color CONTACT_COLOR   = new Color(0.90f, 0.90f, 0.90f, 1f);
    private static final Color STAR_COLOR      = new Color(1.00f, 0.85f, 0.20f, 1f);
    private static final Color CALLED_COLOR    = new Color(0.40f, 0.90f, 0.50f, 1f);
    private static final Color EMPTY_COLOR     = new Color(0.55f, 0.55f, 0.55f, 1f);
    private static final Color POWER_FILL      = new Color(0.55f, 0.10f, 0.10f, 1f);
    private static final Color POWER_BORDER    = new Color(0.80f, 0.30f, 0.30f, 1f);
    private static final Color FRIENDLY_COLOR  = new Color(0.30f, 0.85f, 0.40f, 1f);
    private static final Color NEUTRAL_COLOR   = new Color(0.70f, 0.70f, 0.70f, 1f);
    private static final Color UNFRIENDLY_COLOR = new Color(0.90f, 0.30f, 0.30f, 1f);

    // ---- Rendering resources ----
    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyph;

    // ---- State ----
    private boolean              visible  = false;
    private final List<PhoneContact> contacts = new ArrayList<>();

    // ---- Hit-test bounds (written each frame during draw, read by onTap) ----
    /** Centre X of the round power button. */
    private float powerBtnCX;
    /** Centre Y of the round power button. */
    private float powerBtnCY;
    /** Radius of the round power button. */
    private float powerBtnR;
    /** X of the dialog rectangle (used for contact row hit-testing). */
    private float dialogX;
    /** Width of the dialog rectangle. */
    private float dialogW;
    /** Y of the lowest contact row (row 0). */
    private float contactRowsStartY;
    /** Height of each contact row. */
    private float contactRowH;

    // -------------------------------------------------------------------------

    public PhonePopup(SpriteBatch batch, ShapeRenderer sr,
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

    /** Returns the contact at the given index, or {@code null} if out of range. */
    public PhoneContact getContactAt(int index) {
        return (index >= 0 && index < contacts.size()) ? contacts.get(index) : null;
    }

    /** Returns the number of contacts currently shown. */
    public int getContactCount() { return contacts.size(); }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    /**
     * Opens the phone popup with the supplied contact list.
     * Contacts are typically built from the profile's open case files via
     * {@link MainScreen#buildPhoneContacts}.
     *
     * @param contactList the contacts to display; {@code null} is treated as empty
     */
    public void show(List<PhoneContact> contactList) {
        contacts.clear();
        if (contactList != null) contacts.addAll(contactList);
        visible = true;
    }

    /** Hides the popup. */
    public void hide() { visible = false; }

    // -------------------------------------------------------------------------
    // Input handling
    // -------------------------------------------------------------------------

    /**
     * Handles a tap on this popup.
     *
     * @param screenX  x coordinate (screen space, 0 = left)
     * @param flippedY y coordinate (OpenGL flipped: 0 = bottom)
     * @return index (≥ 0) of the tapped contact, {@link #RESULT_CLOSED}, or
     *         {@link #RESULT_MISS}
     */
    public int onTap(int screenX, int flippedY) {
        if (!visible) return RESULT_MISS;

        // Round power button — hit-test by distance from centre
        float dx = screenX - powerBtnCX;
        float dy = flippedY - powerBtnCY;
        if (powerBtnR > 0 && dx * dx + dy * dy <= powerBtnR * powerBtnR) {
            visible = false;
            return RESULT_CLOSED;
        }

        // Contact rows
        if (contactRowH > 0 && !contacts.isEmpty()) {
            for (int i = 0; i < contacts.size(); i++) {
                float rowY = contactRowsStartY + (contacts.size() - 1 - i) * contactRowH;
                if (screenX >= dialogX && screenX <= dialogX + dialogW
                        && flippedY >= rowY && flippedY <= rowY + contactRowH) {
                    return i;
                }
            }
        }

        return RESULT_MISS;
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    /**
     * Draws the phone popup centred on screen.
     * Does nothing when {@link #isVisible()} is {@code false}.
     *
     * @param screenW screen width in pixels
     * @param screenH screen height in pixels
     */
    public void draw(int screenW, int screenH) {
        if (!visible) return;

        final float PAD   = 20f;
        final float GAP   = 8f;
        final float POWER_R = 24f;   // radius of the round power button

        // Font metrics
        glyph.setText(font, "Hg");
        float fontH = glyph.height;
        glyph.setText(smallFont, "Hg");
        float smallH     = glyph.height;
        float smallLineH = smallH + GAP;

        // Dialog dimensions
        float dW = Math.min(360f, screenW * 0.85f);
        dialogW = dW;

        float rowH      = smallLineH + 4f;
        contactRowH     = rowH;
        int nContacts   = contacts.size();
        float contactsH = nContacts > 0 ? nContacts * rowH : smallLineH;

        float dH = PAD                // top padding
                + fontH + GAP         // title
                + GAP                 // spacer below title
                + contactsH           // contact rows (or "No contacts" label)
                + GAP * 2             // spacer above power area
                + POWER_R * 2         // power button diameter
                + PAD;                // bottom padding

        dH = MathUtils.clamp(dH, 180f, screenH * 0.88f);

        dialogX = (screenW - dW) / 2f;
        float dialogY = (screenH - dH) / 2f;
        float cx = dialogX + dW / 2f;

        // Power button: centred horizontally, near the bottom
        powerBtnCX = cx;
        powerBtnCY = dialogY + PAD + POWER_R;
        powerBtnR  = POWER_R;

        // Contact rows start just above the power area
        float abovePower  = dialogY + PAD + POWER_R * 2 + GAP;
        contactRowsStartY = abovePower + GAP;

        // ---- Shapes ----
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Dialog background
        sr.setColor(BG_COLOR);
        sr.rect(dialogX, dialogY, dW, dH);

        // Alternating row backgrounds
        for (int i = 0; i < nContacts; i++) {
            float rowY = contactRowsStartY + (nContacts - 1 - i) * rowH;
            sr.setColor(i % 2 == 0 ? ROW_EVEN : ROW_ODD);
            sr.rect(dialogX + 2f, rowY, dW - 4f, rowH);
        }

        // Round power button (filled circle)
        sr.setColor(POWER_FILL);
        sr.circle(powerBtnCX, powerBtnCY, POWER_R, 32);

        sr.end();

        // Border + power button outline
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(dialogX,     dialogY,     dW,     dH);
        sr.rect(dialogX + 1, dialogY + 1, dW - 2, dH - 2);
        sr.setColor(POWER_BORDER);
        sr.circle(powerBtnCX, powerBtnCY, POWER_R,     32);
        sr.circle(powerBtnCX, powerBtnCY, POWER_R - 1, 32);
        sr.end();

        // ---- Text ----
        batch.begin();

        // Title
        float ty = dialogY + dH - PAD;
        glyph.setText(font, "Phone");
        font.setColor(TITLE_COLOR);
        font.draw(batch, "Phone", cx - glyph.width / 2f, ty);

        // Contact rows (row 0 at top, row n-1 at bottom, drawn top-down)
        if (nContacts == 0) {
            glyph.setText(smallFont, "No contacts");
            smallFont.setColor(EMPTY_COLOR);
            float noContactY = contactRowsStartY + smallH + GAP;
            smallFont.draw(batch, "No contacts", cx - glyph.width / 2f, noContactY);
        } else {
            for (int i = 0; i < nContacts; i++) {
                PhoneContact c   = contacts.get(i);
                float rowY       = contactRowsStartY + (nContacts - 1 - i) * rowH;
                float textY      = rowY + (rowH + smallH) / 2f;

                // Star prefix (★ for open-case contacts, spaces for others)
                String starStr = c.caseOpen ? "\u2605 " : "  ";
                smallFont.setColor(STAR_COLOR);
                smallFont.draw(batch, starStr, dialogX + PAD, textY);
                glyph.setText(smallFont, starStr);
                float starW = glyph.width;

                // Contact name
                smallFont.setColor(CONTACT_COLOR);
                smallFont.draw(batch, c.name, dialogX + PAD + starW, textY);

                // "CALLED [RATING]" badge on the right if already phoned
                if (c.phoned) {
                    String ratingLabel = ratingLabel(c.rating);
                    glyph.setText(smallFont, ratingLabel);
                    smallFont.setColor(ratingColor(c.rating));
                    smallFont.draw(batch, ratingLabel,
                            dialogX + dW - PAD - glyph.width, textY);
                }
            }
        }

        // Power symbol (⏻) centred on the power button
        String powerSym = "\u23FB";
        glyph.setText(smallFont, powerSym);
        smallFont.setColor(new Color(0.90f, 0.90f, 0.90f, 1f));
        smallFont.draw(batch, powerSym,
                powerBtnCX - glyph.width / 2f,
                powerBtnCY + glyph.height / 2f);

        batch.end();
    }

    // -------------------------------------------------------------------------
    // Rating helpers
    // -------------------------------------------------------------------------

    /** Returns the display label for a rating (or "CALLED" when rating is null). */
    private static String ratingLabel(PhoneMessageRating rating) {
        if (rating == null) return "CALLED";
        switch (rating) {
            case FRIENDLY:   return "FRIENDLY";
            case UNFRIENDLY: return "UNFRIENDLY";
            default:         return "NEUTRAL";
        }
    }

    /** Returns the display colour for a rating. */
    private static Color ratingColor(PhoneMessageRating rating) {
        if (rating == null) return CALLED_COLOR;
        switch (rating) {
            case FRIENDLY:   return FRIENDLY_COLOR;
            case UNFRIENDLY: return UNFRIENDLY_COLOR;
            default:         return NEUTRAL_COLOR;
        }
    }
}

package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Modal popup for the player's email inbox.
 *
 * <p>Displays one email at a time.  The player can Accept (adds a calendar
 * entry), Decline, or Close.  After Accept or Decline the popup advances
 * automatically to the next email; it closes itself when all emails are
 * processed or when Close is tapped.
 *
 * <h3>Usage (MainScreen)</h3>
 * <pre>
 *   emailPopup.show(emails);
 *   // each frame:
 *   emailPopup.draw(screenW, screenH);
 *   // on tap:
 *   int result = emailPopup.onTap(screenX, flippedY);
 *   if (result >= 0) handleEmailAccepted(result);
 * </pre>
 */
class EmailPopup {

    /** An email to show in the popup. */
    static class EmailData {
        final String from;
        final String subject;
        final String body;
        final String calendarTitle;
        final String calendarDateTime;
        final String calendarLocation;

        EmailData(String from, String subject, String body,
                  String calendarTitle, String calendarDateTime, String calendarLocation) {
            this.from             = from             != null ? from             : "";
            this.subject          = subject          != null ? subject          : "";
            this.body             = body             != null ? body             : "";
            this.calendarTitle    = calendarTitle    != null ? calendarTitle    : "";
            this.calendarDateTime = calendarDateTime != null ? calendarDateTime : "";
            this.calendarLocation = calendarLocation != null ? calendarLocation : "";
        }
    }

    /** Returned by {@link #onTap} when the Accept button was tapped (index of accepted email ≥ 0). */
    static final int RESULT_DECLINED = -2;
    /** Returned by {@link #onTap} when the Close button was tapped. */
    static final int RESULT_CLOSED   = -1;
    /** Returned by {@link #onTap} when no button was hit. */
    static final int RESULT_MISS     = -3;

    // --- Colors ---
    private static final Color BG_COLOR      = new Color(0.06f, 0.10f, 0.16f, 1f);
    private static final Color BORDER_COLOR  = new Color(0.40f, 0.65f, 0.90f, 1f);
    private static final Color TITLE_COLOR   = new Color(0.80f, 0.90f, 1.00f, 1f);
    private static final Color FROM_COLOR    = new Color(0.60f, 0.80f, 1.00f, 1f);
    private static final Color BODY_COLOR    = new Color(0.85f, 0.85f, 0.85f, 1f);
    private static final Color APPT_COLOR    = new Color(0.50f, 1.00f, 0.65f, 1f);
    private static final Color ACCEPT_COLOR  = new Color(0.10f, 0.45f, 0.12f, 1f);
    private static final Color DECLINE_COLOR = new Color(0.40f, 0.15f, 0.15f, 1f);
    private static final Color CLOSE_COLOR   = new Color(0.20f, 0.20f, 0.30f, 1f);

    // --- Rendering resources ---
    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyph;

    // --- State ---
    private boolean         visible = false;
    private List<EmailData> emails  = new ArrayList<>();
    private int             current = 0;

    // Button bounds (written during draw, read by onTap)
    private float acceptX,  acceptY,  acceptW,  acceptH;
    private float declineX, declineY, declineW, declineH;
    private float closeX,   closeY,   closeW,   closeH;

    // -------------------------------------------------------------------------

    EmailPopup(SpriteBatch batch, ShapeRenderer sr,
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

    /** Returns the email at {@code index} in the original list, or {@code null}. */
    EmailData getEmailAt(int index) {
        return (index >= 0 && index < emails.size()) ? emails.get(index) : null;
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    void show(List<EmailData> list) {
        this.emails  = new ArrayList<>(list);
        this.current = 0;
        this.visible = !this.emails.isEmpty();
        this.closeW  = 0f;
        Gdx.app.log("EmailPopup", "Opened with " + this.emails.size() + " email(s)");
    }

    /**
     * Handles a tap on this popup.
     *
     * @return index (≥ 0) of the accepted email, {@link #RESULT_DECLINED},
     *         {@link #RESULT_CLOSED}, or {@link #RESULT_MISS}
     */
    int onTap(int screenX, int flippedY) {
        if (!visible || emails.isEmpty()) return RESULT_MISS;

        // Accept
        if (acceptW > 0
                && screenX >= acceptX && screenX <= acceptX + acceptW
                && flippedY >= acceptY && flippedY <= acceptY + acceptH) {
            int idx = current;
            advance();
            return idx;
        }

        // Decline
        if (declineW > 0
                && screenX >= declineX && screenX <= declineX + declineW
                && flippedY >= declineY && flippedY <= declineY + declineH) {
            advance();
            return RESULT_DECLINED;
        }

        // Close
        if (closeW > 0
                && screenX >= closeX && screenX <= closeX + closeW
                && flippedY >= closeY && flippedY <= closeY + closeH) {
            visible = false;
            return RESULT_CLOSED;
        }

        return RESULT_MISS;
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    void draw(int screenW, int screenH) {
        if (!visible || current >= emails.size()) return;
        EmailData email = emails.get(current);

        final float PAD      = 24f;
        final float GAP      = 10f;
        final float BTN_GAP  = 16f;
        final float MIN_W    = 340f;
        final float MAX_W    = screenW * 0.88f;

        // Font metrics
        glyph.setText(font, "Hg");
        float fontH     = glyph.height;
        float fontLineH = fontH + GAP;
        glyph.setText(smallFont, "Hg");
        float smallH     = glyph.height;
        float smallLineH = smallH + GAP;

        // Title line
        String titleStr = "Inbox  [" + (current + 1) + " / " + emails.size() + "]";

        // Measure for dialog width
        TextMeasurer.TextBounds acceptBounds  = TextMeasurer.measure(font, glyph, "Accept",  28f, 12f);
        TextMeasurer.TextBounds declineBounds = TextMeasurer.measure(font, glyph, "Decline", 28f, 12f);
        TextMeasurer.TextBounds closeBounds   = TextMeasurer.measure(font, glyph, "Close",   28f, 12f);
        float btnH = acceptBounds.height;

        glyph.setText(font, titleStr);
        float minLineW = glyph.width;
        glyph.setText(smallFont, "From: " + email.from);
        minLineW = Math.max(minLineW, glyph.width);
        glyph.setText(smallFont, "Subject: " + email.subject);
        minLineW = Math.max(minLineW, glyph.width);

        float dialogW = MathUtils.clamp(minLineW + 2 * PAD, MIN_W, MAX_W);
        float wrapW   = dialogW - 2 * PAD;

        // Wrap body and appointment lines
        List<String> bodyLines = WordWrapper.wrap(email.body, wrapW, t -> {
            glyph.setText(smallFont, t);
            return glyph.width;
        });
        String apptStr = "Appointment:  " + email.calendarDateTime
                + "  \u2014  " + email.calendarLocation;
        List<String> apptLines = WordWrapper.wrap(apptStr, wrapW, t -> {
            glyph.setText(smallFont, t);
            return glyph.width;
        });

        // Compute dialog height
        float dialogH = PAD
                + fontLineH                          // title
                + smallLineH                         // From
                + smallLineH                         // Subject
                + GAP                                // spacer
                + bodyLines.size() * smallLineH      // body
                + GAP                                // spacer
                + apptLines.size() * smallLineH      // appointment
                + GAP                                // spacer before buttons
                + btnH + BTN_GAP                     // Accept + Decline row
                + closeBounds.height                 // Close
                + PAD;

        dialogH = MathUtils.clamp(dialogH, 200f, screenH * 0.90f);

        float dialogX = (screenW - dialogW) / 2f;
        float dialogY = (screenH - dialogH) / 2f;

        // Layout buttons bottom-up
        float cy = dialogY + PAD;
        closeX = dialogX + (dialogW - closeBounds.width) / 2f;
        closeY = cy;
        closeW = closeBounds.width;
        closeH = closeBounds.height;
        cy += closeBounds.height + BTN_GAP;

        float totalBtnW = acceptBounds.width + BTN_GAP + declineBounds.width;
        acceptX  = dialogX + (dialogW - totalBtnW) / 2f;
        acceptY  = cy;
        acceptW  = acceptBounds.width;
        acceptH  = btnH;
        declineX = acceptX + acceptBounds.width + BTN_GAP;
        declineY = cy;
        declineW = declineBounds.width;
        declineH = btnH;

        // Shapes
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(dialogX, dialogY, dialogW, dialogH);
        sr.setColor(ACCEPT_COLOR);
        sr.rect(acceptX, acceptY, acceptW, acceptH);
        sr.setColor(DECLINE_COLOR);
        sr.rect(declineX, declineY, declineW, declineH);
        sr.setColor(CLOSE_COLOR);
        sr.rect(closeX, closeY, closeW, closeH);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(dialogX,      dialogY,      dialogW,      dialogH);
        sr.rect(dialogX + 1,  dialogY + 1,  dialogW - 2,  dialogH - 2);
        sr.rect(acceptX,      acceptY,      acceptW,      acceptH);
        sr.rect(acceptX  + 1, acceptY  + 1, acceptW  - 2, acceptH  - 2);
        sr.rect(declineX,     declineY,     declineW,     declineH);
        sr.rect(declineX + 1, declineY + 1, declineW - 2, declineH - 2);
        sr.rect(closeX,       closeY,       closeW,       closeH);
        sr.rect(closeX  + 1,  closeY  + 1,  closeW  - 2,  closeH  - 2);
        sr.end();

        // Text
        batch.begin();
        float ty = dialogY + dialogH - PAD;

        font.setColor(TITLE_COLOR);
        glyph.setText(font, titleStr);
        font.draw(batch, titleStr, dialogX + (dialogW - glyph.width) / 2f, ty);
        ty -= fontLineH;

        smallFont.setColor(FROM_COLOR);
        smallFont.draw(batch, "From: " + email.from, dialogX + PAD, ty);
        ty -= smallLineH;

        smallFont.setColor(FROM_COLOR);
        smallFont.draw(batch, "Subject: " + email.subject, dialogX + PAD, ty);
        ty -= smallLineH + GAP;

        smallFont.setColor(BODY_COLOR);
        for (String line : bodyLines) {
            smallFont.draw(batch, line, dialogX + PAD, ty);
            ty -= smallLineH;
        }
        ty -= GAP;

        smallFont.setColor(APPT_COLOR);
        for (String line : apptLines) {
            smallFont.draw(batch, line, dialogX + PAD, ty);
            ty -= smallLineH;
        }

        font.setColor(Color.WHITE);
        glyph.setText(font, "Accept");
        font.draw(batch, "Accept",
                acceptX  + (acceptW  - glyph.width) / 2f,
                acceptY  + (acceptH  + glyph.height) / 2f);

        glyph.setText(font, "Decline");
        font.draw(batch, "Decline",
                declineX + (declineW - glyph.width) / 2f,
                declineY + (declineH + glyph.height) / 2f);

        glyph.setText(font, "Close");
        font.draw(batch, "Close",
                closeX + (closeW - glyph.width) / 2f,
                closeY + (closeH + glyph.height) / 2f);

        batch.end();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void advance() {
        current++;
        if (current >= emails.size()) {
            visible = false;
        }
    }
}

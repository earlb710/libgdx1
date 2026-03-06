package eb.framework1.popup;

import eb.framework1.generator.*;
import eb.framework1.schedule.*;
import eb.framework1.screen.*;
import eb.framework1.ui.*;


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
public class EmailPopup {

    /** An email to show in the popup. */
    public static class EmailData {
        public final String from;
        public final String subject;
        public final String body;
        public final String calendarTitle;
        public final String calendarDateTime;
        public final String calendarLocation;
        public final int    rewardMoney;       // 0 = no money reward
        public final String rewardItemName;    // null = no item reward
        /** Map cell of the appointment location; -1 if unknown. */
        public final int    locationCellX;
        public final int    locationCellY;
        /**
         * Full name of the person to meet (e.g. "Alice Smith").
         * Empty string when there is no named contact (e.g. NYPD crime scene).
         * Forwarded to {@link CalendarEntry#contactName} when the email is accepted.
         */
        public final String calendarContactName;
        /**
         * Gender of the contact ({@code "M"} or {@code "F"}).
         * Forwarded to {@link CalendarEntry#contactGender} when the email is accepted.
         * Used by {@link ClientIntroductionGenerator} to tailor the Meet introduction.
         */
        public final String contactGender;

        public EmailData(String from, String subject, String body,
                  String calendarTitle, String calendarDateTime, String calendarLocation) {
            this(from, subject, body, calendarTitle, calendarDateTime, calendarLocation, 0, null, -1, -1, "", "M");
        }

        public EmailData(String from, String subject, String body,
                  String calendarTitle, String calendarDateTime, String calendarLocation,
                  int rewardMoney, String rewardItemName) {
            this(from, subject, body, calendarTitle, calendarDateTime, calendarLocation,
                    rewardMoney, rewardItemName, -1, -1, "", "M");
        }

        public EmailData(String from, String subject, String body,
                  String calendarTitle, String calendarDateTime, String calendarLocation,
                  int rewardMoney, String rewardItemName,
                  int locationCellX, int locationCellY) {
            this(from, subject, body, calendarTitle, calendarDateTime, calendarLocation,
                    rewardMoney, rewardItemName, locationCellX, locationCellY, "", "M");
        }

        public EmailData(String from, String subject, String body,
                  String calendarTitle, String calendarDateTime, String calendarLocation,
                  int rewardMoney, String rewardItemName,
                  int locationCellX, int locationCellY,
                  String calendarContactName) {
            this(from, subject, body, calendarTitle, calendarDateTime, calendarLocation,
                    rewardMoney, rewardItemName, locationCellX, locationCellY, calendarContactName, "M");
        }

        public EmailData(String from, String subject, String body,
                  String calendarTitle, String calendarDateTime, String calendarLocation,
                  int rewardMoney, String rewardItemName,
                  int locationCellX, int locationCellY,
                  String calendarContactName, String contactGender) {
            this.from                 = from             != null ? from             : "";
            this.subject              = subject          != null ? subject          : "";
            this.body                 = body             != null ? body             : "";
            this.calendarTitle        = calendarTitle    != null ? calendarTitle    : "";
            this.calendarDateTime     = calendarDateTime != null ? calendarDateTime : "";
            this.calendarLocation     = calendarLocation != null ? calendarLocation : "";
            this.rewardMoney          = Math.max(0, rewardMoney);
            this.rewardItemName       = rewardItemName;
            this.locationCellX        = locationCellX;
            this.locationCellY        = locationCellY;
            this.calendarContactName  = calendarContactName != null ? calendarContactName : "";
            this.contactGender        = (contactGender != null && !contactGender.trim().isEmpty())
                                        ? contactGender.trim().toUpperCase() : "M";
        }
    }

    /** Returned by {@link #onTap} when the Accept button was tapped (index of accepted email ≥ 0). */
    public static final int RESULT_DECLINED = -2;
    /** Returned by {@link #onTap} when the Close button was tapped. */
    public static final int RESULT_CLOSED   = -1;
    /** Returned by {@link #onTap} when no button was hit. */
    public static final int RESULT_MISS     = -3;
    /** Returned by {@link #onTap} when a navigation (prev/next) button was tapped. */
    public static final int RESULT_NAV      = -4;

    // Email status constants
    private static final int STATUS_UNREAD   = 0;
    private static final int STATUS_ACCEPTED = 1;
    private static final int STATUS_DECLINED = 2;

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
    private static final Color NAV_COLOR     = new Color(0.18f, 0.28f, 0.42f, 1f);
    private static final Color CHECK_COLOR   = new Color(0.20f, 0.85f, 0.30f, 1f);
    private static final Color CROSS_COLOR   = new Color(0.85f, 0.25f, 0.25f, 1f);

    private static final String CHECK_SYMBOL = "[OK]" + " "; // trailing space is intentional padding
    private static final String CROSS_SYMBOL = "[X]"  + "  "; // two spaces to match CHECK_SYMBOL length

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
    private int[]           status  = new int[0]; // STATUS_UNREAD/ACCEPTED/DECLINED per email

    // Button bounds (written during draw, read by onTap)
    private float acceptX,  acceptY,  acceptW,  acceptH;
    private float declineX, declineY, declineW, declineH;
    private float closeX,   closeY,   closeW,   closeH;
    private float prevX,    prevY,    prevW,    prevH;
    private float nextX,    nextY,    nextW,    nextH;

    // -------------------------------------------------------------------------

    public EmailPopup(SpriteBatch batch, ShapeRenderer sr,
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

    /** Returns the email at {@code index} in the original list, or {@code null}. */
    public EmailData getEmailAt(int index) {
        return (index >= 0 && index < emails.size()) ? emails.get(index) : null;
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    public void show(List<EmailData> list) {
        this.emails  = new ArrayList<>(list);
        this.current = 0;
        this.status  = new int[this.emails.size()]; // all STATUS_UNREAD (0)
        this.visible = !this.emails.isEmpty();
        this.closeW  = 0f;
        Gdx.app.log("EmailPopup", "Opened with " + this.emails.size() + " email(s)");
    }

    /**
     * Re-opens the popup restoring previously recorded accept/decline statuses.
     * Use this when the same day's emails are shown again after being closed.
     *
     * @param list     The same email list passed to the original {@link #show} call.
     * @param statuses Status array from {@link #getStatuses()}; a length mismatch
     *                 is handled gracefully (unmatched entries default to unread).
     */
    public void showWithStatus(List<EmailData> list, int[] statuses) {
        this.emails  = new ArrayList<>(list);
        this.current = 0;
        this.status  = new int[this.emails.size()];
        if (statuses != null) {
            int len = Math.min(statuses.length, this.status.length);
            System.arraycopy(statuses, 0, this.status, 0, len);
        }
        this.visible = !this.emails.isEmpty();
        this.closeW  = 0f;
        Gdx.app.log("EmailPopup", "Re-opened with " + this.emails.size() + " email(s) (statuses restored)");
    }

    /**
     * Returns a snapshot of the current per-email status array.
     * Save this before closing and pass it to {@link #showWithStatus} on re-open.
     */
    public int[] getStatuses() {
        if (status == null) return new int[0];
        return java.util.Arrays.copyOf(status, status.length);
    }

    /**
     * Handles a tap on this popup.
     *
     * @return index (≥ 0) of the accepted email, {@link #RESULT_DECLINED},
     *         {@link #RESULT_CLOSED}, {@link #RESULT_NAV}, or {@link #RESULT_MISS}
     */
    public int onTap(int screenX, int flippedY) {
        if (!visible || emails.isEmpty()) return RESULT_MISS;

        // Prev nav
        if (prevW > 0
                && screenX >= prevX && screenX <= prevX + prevW
                && flippedY >= prevY && flippedY <= prevY + prevH) {
            if (current > 0) current--;
            return RESULT_NAV;
        }

        // Next nav
        if (nextW > 0
                && screenX >= nextX && screenX <= nextX + nextW
                && flippedY >= nextY && flippedY <= nextY + nextH) {
            if (current < emails.size() - 1) current++;
            return RESULT_NAV;
        }

        // Accept (only for unread emails)
        if (acceptW > 0
                && screenX >= acceptX && screenX <= acceptX + acceptW
                && flippedY >= acceptY && flippedY <= acceptY + acceptH) {
            int idx = current;
            status[idx] = STATUS_ACCEPTED;
            return idx; // caller uses ≥ 0 to detect acceptance
        }

        // Decline (only for unread emails)
        if (declineW > 0
                && screenX >= declineX && screenX <= declineX + declineW
                && flippedY >= declineY && flippedY <= declineY + declineH) {
            status[current] = STATUS_DECLINED;
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

    public void draw(int screenW, int screenH) {
        if (!visible || current >= emails.size()) return;
        EmailData email  = emails.get(current);
        int       st     = (status != null && current < status.length) ? status[current] : STATUS_UNREAD;

        final float PAD      = 24f;
        final float GAP      = 10f;
        final float BTN_GAP  = 16f;
        final float MIN_W    = 340f;
        final float MAX_W    = screenW;

        // Font metrics
        glyph.setText(font, "Hg");
        float fontH     = glyph.height;
        float fontLineH = fontH + GAP;
        glyph.setText(smallFont, "Hg");
        float smallH     = glyph.height;
        float smallLineH = smallH + GAP;

        // Nav button labels
        String prevLabel = "<<";
        String nextLabel = ">>";
        TextMeasurer.TextBounds prevBounds = TextMeasurer.measure(smallFont, glyph, prevLabel, 14f, 8f);
        TextMeasurer.TextBounds nextBounds = TextMeasurer.measure(smallFont, glyph, nextLabel, 14f, 8f);

        String titleStr = "Inbox  [" + (current + 1) + " / " + emails.size() + "]";

        // Measure for dialog width
        TextMeasurer.TextBounds acceptBounds  = TextMeasurer.measure(font, glyph, "Accept",  28f, 12f);
        TextMeasurer.TextBounds declineBounds = TextMeasurer.measure(font, glyph, "Decline", 28f, 12f);
        TextMeasurer.TextBounds closeBounds   = TextMeasurer.measure(font, glyph, "Close",   28f, 12f);
        float btnH = acceptBounds.height;

        glyph.setText(font, titleStr);
        float minLineW = glyph.width + prevBounds.width + BTN_GAP * 2 + nextBounds.width;
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

        // Reward line (only if reward > 0 or item present)
        String rewardLine = null;
        if (email.rewardMoney > 0 && email.rewardItemName != null) {
            rewardLine = "Reward:  $" + email.rewardMoney + "  +  " + email.rewardItemName;
        } else if (email.rewardMoney > 0) {
            rewardLine = "Reward:  $" + email.rewardMoney;
        } else if (email.rewardItemName != null) {
            rewardLine = "Reward:  " + email.rewardItemName;
        }
        List<String> rewardLines = rewardLine != null
                ? WordWrapper.wrap(rewardLine, wrapW, t -> { glyph.setText(smallFont, t); return glyph.width; })
                : java.util.Collections.emptyList();

        // Action buttons height — hidden when already processed
        float actionBtnH = (st == STATUS_UNREAD) ? (btnH + BTN_GAP) : 0f;

        // Compute dialog height
        float dialogH = PAD
                + fontLineH + fontH                  // title row (inc nav buttons) + char-size gap
                + smallLineH                         // From
                + smallLineH                         // Subject
                + GAP                                // spacer
                + bodyLines.size() * smallLineH      // body
                + GAP                                // spacer
                + apptLines.size() * smallLineH      // appointment
                + (!rewardLines.isEmpty() ? GAP + rewardLines.size() * smallLineH : 0f)
                + GAP                                // spacer before buttons
                + actionBtnH                         // Accept + Decline (when unread)
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

        if (st == STATUS_UNREAD) {
            float totalBtnW = acceptBounds.width + BTN_GAP + declineBounds.width;
            acceptX  = dialogX + (dialogW - totalBtnW) / 2f;
            acceptY  = cy;
            acceptW  = acceptBounds.width;
            acceptH  = btnH;
            declineX = acceptX + acceptBounds.width + BTN_GAP;
            declineY = cy;
            declineW = declineBounds.width;
            declineH = btnH;
        } else {
            acceptW = declineW = 0f; // not clickable
        }

        // Nav buttons in title row (written below during text layout)
        float titleRowY = dialogY + dialogH - PAD - fontH;
        prevX = dialogX + PAD;
        prevY = titleRowY - (prevBounds.height - fontH) / 2f;
        prevW = prevBounds.width;
        prevH = prevBounds.height;
        nextX = dialogX + dialogW - PAD - nextBounds.width;
        nextY = prevY;
        nextW = nextBounds.width;
        nextH = nextBounds.height;

        // Shapes
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(dialogX, dialogY, dialogW, dialogH);
        // Nav buttons
        sr.setColor(current > 0 ? NAV_COLOR : new Color(0.10f, 0.15f, 0.22f, 1f));
        sr.rect(prevX, prevY, prevW, prevH);
        sr.setColor(current < emails.size() - 1 ? NAV_COLOR : new Color(0.10f, 0.15f, 0.22f, 1f));
        sr.rect(nextX, nextY, nextW, nextH);
        if (st == STATUS_UNREAD) {
            sr.setColor(ACCEPT_COLOR);
            sr.rect(acceptX, acceptY, acceptW, acceptH);
            sr.setColor(DECLINE_COLOR);
            sr.rect(declineX, declineY, declineW, declineH);
        }
        sr.setColor(CLOSE_COLOR);
        sr.rect(closeX, closeY, closeW, closeH);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(dialogX,      dialogY,      dialogW,      dialogH);
        sr.rect(dialogX + 1,  dialogY + 1,  dialogW - 2,  dialogH - 2);
        sr.rect(prevX,        prevY,        prevW,        prevH);
        sr.rect(nextX,        nextY,        nextW,        nextH);
        if (st == STATUS_UNREAD) {
            sr.rect(acceptX,      acceptY,      acceptW,      acceptH);
            sr.rect(acceptX  + 1, acceptY  + 1, acceptW  - 2, acceptH  - 2);
            sr.rect(declineX,     declineY,     declineW,     declineH);
            sr.rect(declineX + 1, declineY + 1, declineW - 2, declineH - 2);
        }
        sr.rect(closeX,       closeY,       closeW,       closeH);
        sr.rect(closeX  + 1,  closeY  + 1,  closeW  - 2,  closeH  - 2);
        sr.end();

        // Text
        batch.begin();
        float ty = dialogY + dialogH - PAD;

        // Title row: [<<]  Inbox [n/m]  [>>]
        smallFont.setColor(current > 0 ? TITLE_COLOR : FROM_COLOR);
        smallFont.draw(batch, prevLabel, prevX + (prevW - prevBounds.width + 14f) / 2f,
                prevY + (prevH + prevBounds.height) / 2f);
        smallFont.setColor(current < emails.size() - 1 ? TITLE_COLOR : FROM_COLOR);
        smallFont.draw(batch, nextLabel, nextX + (nextW - nextBounds.width + 14f) / 2f,
                nextY + (nextH + nextBounds.height) / 2f);

        font.setColor(TITLE_COLOR);
        String inboxStr = "Inbox  [" + (current + 1) + " / " + emails.size() + "]";
        glyph.setText(font, inboxStr);
        float titleX = dialogX + (dialogW - glyph.width) / 2f;
        font.draw(batch, inboxStr, titleX, ty);
        ty -= fontLineH + fontH;

        smallFont.setColor(FROM_COLOR);
        smallFont.draw(batch, "From: " + email.from, dialogX + PAD, ty);
        ty -= smallLineH;

        // Subject line
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

        if (!rewardLines.isEmpty()) {
            ty -= GAP;
            smallFont.setColor(new Color(1.00f, 0.85f, 0.20f, 1f)); // gold
            for (String line : rewardLines) {
                smallFont.draw(batch, line, dialogX + PAD, ty);
                ty -= smallLineH;
            }
        }

        if (st == STATUS_UNREAD) {
            font.setColor(Color.WHITE);
            glyph.setText(font, "Accept");
            font.draw(batch, "Accept",
                    acceptX  + (acceptW  - glyph.width) / 2f,
                    acceptY  + (acceptH  + glyph.height) / 2f);
            glyph.setText(font, "Decline");
            font.draw(batch, "Decline",
                    declineX + (declineW - glyph.width) / 2f,
                    declineY + (declineH + glyph.height) / 2f);
        }

        font.setColor(Color.WHITE);
        glyph.setText(font, "Close");
        font.draw(batch, "Close",
                closeX + (closeW - glyph.width) / 2f,
                closeY + (closeH + glyph.height) / 2f);

        // Status symbol in the lower-right corner of the dialog
        if (st == STATUS_ACCEPTED || st == STATUS_DECLINED) {
            String sym = (st == STATUS_ACCEPTED) ? CHECK_SYMBOL.trim() : CROSS_SYMBOL.trim();
            font.setColor(st == STATUS_ACCEPTED ? CHECK_COLOR : CROSS_COLOR);
            glyph.setText(font, sym);
            float symW = glyph.width;
            float symH = glyph.height;
            font.draw(batch, sym,
                    dialogX + dialogW - PAD - symW,
                    dialogY + PAD + (closeH + symH) / 2f);
            font.setColor(Color.WHITE);
        }

        batch.end();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------
}

package eb.framework1;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Draws the unit-interior overlay over the info panel when the player has
 * entered a known unit (e.g. their home office).
 *
 * <p>The popup shows:
 * <ul>
 *   <li>Unit title (e.g. "Your Office — 3rd Floor Unit 3C")</li>
 *   <li>Rest button (daytime 05:00–20:00)</li>
 *   <li>Sleep button (nighttime 20:00–05:00)</li>
 *   <li>Exit button</li>
 * </ul>
 *
 * Button bounds are written to {@link MapViewState} so that {@code MainScreen}
 * can perform hit-testing without knowing layout details.
 */
class UnitInteriorPopup {

    private static final Color BG_COLOR             = new Color(0.08f, 0.08f, 0.18f, 1f);
    private static final Color BORDER_COLOR         = new Color(0.5f,  0.5f,  0.8f,  1f);
    private static final Color REST_BTN_COLOR       = new Color(0.4f,  0.25f, 0.05f, 1f);
    private static final Color SLEEP_BTN_COLOR      = new Color(0.08f, 0.08f, 0.45f, 1f);
    private static final Color SLEEP_DISABLED_COLOR = new Color(0.12f, 0.12f, 0.18f, 1f);
    private static final Color STASH_BTN_COLOR      = new Color(0.35f, 0.15f, 0.50f, 1f);
    private static final Color EMAIL_BTN_COLOR      = new Color(0.10f, 0.30f, 0.50f, 1f);
    private static final Color EXIT_BTN_COLOR       = new Color(0.35f, 0.05f, 0.05f, 1f);
    private static final Color DISABLED_TEXT_COLOR  = new Color(0.40f, 0.40f, 0.40f, 1f);
    private static final Color DISABLED_BORDER_COLOR= new Color(0.25f, 0.25f, 0.35f, 1f);

    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyph;
    private final Profile       profile;

    UnitInteriorPopup(SpriteBatch batch, ShapeRenderer sr, BitmapFont font,
                      BitmapFont smallFont, GlyphLayout glyph, Profile profile) {
        this.batch     = batch;
        this.sr        = sr;
        this.font      = font;
        this.smallFont = smallFont;
        this.glyph     = glyph;
        this.profile   = profile;
    }

    /**
     * Draws the unit interior panel and writes all button bounds into {@code s}.
     * Does nothing if {@code s.unitInteriorOpen} is {@code false}.
     */
    void draw(MapViewState s) {
        if (!s.unitInteriorOpen) return;

        final float PAD_X = 24f, PAD_Y = 10f, BTN_SPACING = 20f;
        final float panelH = s.infoAreaHeight;
        final float panelW = s.screenWidth;

        // Button bounds via TextMeasurer (reuses glyph GlyphLayout – no per-frame allocation)
        TextMeasurer.TextBounds restBounds   = TextMeasurer.measure(font, glyph, "Rest",        PAD_X, PAD_Y);
        TextMeasurer.TextBounds sleepBounds  = TextMeasurer.measure(font, glyph, "Sleep",       PAD_X, PAD_Y);
        TextMeasurer.TextBounds stashBounds  = TextMeasurer.measure(font, glyph, "Open Stash",  PAD_X, PAD_Y);
        TextMeasurer.TextBounds emailBounds  = TextMeasurer.measure(font, glyph, "Check Emails",PAD_X, PAD_Y);
        TextMeasurer.TextBounds exitBounds   = TextMeasurer.measure(font, glyph, "Exit",        PAD_X, PAD_Y);
        final float BTN_H    = restBounds.height;
        final float REST_W   = restBounds.width;
        final float SLEEP_W  = sleepBounds.width;
        final float STASH_W  = stashBounds.width;
        final float EMAIL_W  = emailBounds.width;
        final float EXIT_W   = exitBounds.width;
        final float fontCapH = restBounds.textHeight;

        // Measure "1 hr" label so Sleep button is placed immediately after it
        glyph.setText(smallFont, "1 hr");
        final float ONE_HR_W = glyph.width;
        final float ONE_HR_GAP = 10f;  // gap between Rest btn and "1 hr" text
        final float SLEEP_GAP  = 16f;  // gap between "1 hr" text and Sleep btn

        // Both Rest and Sleep are always visible
        // Layout: title first, then [Rest] 1 hr [Sleep] until 6:00, then stash/email/exit
        // Sleep is disabled (greyed out) during daytime (05:00–19:59)
        // Stash and email are hidden for hotel rooms (unitIsHotelRoom).
        int curHour = profile.getCurrentHour();
        boolean isNight = curHour >= 20 || curHour < 5;
        boolean showStashEmail = !s.unitIsHotelRoom;

        final float btnX = 20f;
        float titleY = panelH - PAD_Y - fontCapH;
        float curY   = titleY - fontCapH - fontCapH - BTN_H; // first row: char-size gap after title

        // Rest and Sleep share the same row
        s.restBtnX  = btnX; s.restBtnW  = REST_W;  s.restBtnH = BTN_H;
        s.restBtnY  = curY;

        float sleepBtnX = btnX + REST_W + ONE_HR_GAP + ONE_HR_W + SLEEP_GAP;
        s.sleepBtnX = sleepBtnX; s.sleepBtnW = isNight ? SLEEP_W : 0f; s.sleepBtnH = BTN_H;
        s.sleepBtnY = curY;
        curY -= BTN_H + BTN_SPACING;

        // Open Stash and Check Emails — only inside office (not hotel room)
        s.openStashBtnX  = btnX; s.openStashBtnH  = BTN_H;
        s.openStashBtnW  = showStashEmail ? STASH_W : 0f;
        s.openStashBtnY  = curY;
        if (showStashEmail) curY -= BTN_H + BTN_SPACING;

        s.checkEmailsBtnX = btnX; s.checkEmailsBtnH = BTN_H;
        s.checkEmailsBtnW = showStashEmail ? EMAIL_W : 0f;
        s.checkEmailsBtnY = curY;
        if (showStashEmail) curY -= BTN_H + BTN_SPACING;

        s.unitExitBtnX = btnX; s.unitExitBtnW = EXIT_W; s.unitExitBtnH = BTN_H;
        s.unitExitBtnY = curY;

        // --- Draw background ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(0, 0, panelW, panelH);
        sr.setColor(REST_BTN_COLOR);
        sr.rect(s.restBtnX, s.restBtnY, REST_W, BTN_H);
        sr.setColor(isNight ? SLEEP_BTN_COLOR : SLEEP_DISABLED_COLOR);
        sr.rect(s.sleepBtnX, s.sleepBtnY, SLEEP_W, BTN_H);
        if (showStashEmail) {
            sr.setColor(STASH_BTN_COLOR);
            sr.rect(s.openStashBtnX, s.openStashBtnY, STASH_W, BTN_H);
            sr.setColor(EMAIL_BTN_COLOR);
            sr.rect(s.checkEmailsBtnX, s.checkEmailsBtnY, EMAIL_W, BTN_H);
        }
        sr.setColor(EXIT_BTN_COLOR);
        sr.rect(s.unitExitBtnX, s.unitExitBtnY, EXIT_W, BTN_H);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.line(0, panelH, panelW, panelH);
        sr.rect(s.restBtnX,     s.restBtnY,     REST_W,     BTN_H);
        sr.rect(s.restBtnX + 1, s.restBtnY + 1, REST_W - 2, BTN_H - 2);
        sr.setColor(isNight ? BORDER_COLOR : DISABLED_BORDER_COLOR);
        sr.rect(s.sleepBtnX,     s.sleepBtnY,     SLEEP_W,     BTN_H);
        sr.rect(s.sleepBtnX + 1, s.sleepBtnY + 1, SLEEP_W - 2, BTN_H - 2);
        sr.setColor(BORDER_COLOR);
        if (showStashEmail) {
            sr.rect(s.openStashBtnX,     s.openStashBtnY,     STASH_W,     BTN_H);
            sr.rect(s.openStashBtnX + 1, s.openStashBtnY + 1, STASH_W - 2, BTN_H - 2);
            sr.rect(s.checkEmailsBtnX,     s.checkEmailsBtnY,     EMAIL_W,     BTN_H);
            sr.rect(s.checkEmailsBtnX + 1, s.checkEmailsBtnY + 1, EMAIL_W - 2, BTN_H - 2);
        }
        sr.rect(s.unitExitBtnX,     s.unitExitBtnY,     EXIT_W,     BTN_H);
        sr.rect(s.unitExitBtnX + 1, s.unitExitBtnY + 1, EXIT_W - 2, BTN_H - 2);
        sr.end();

        // --- Draw text ---
        batch.begin();
        // Unit title
        font.setColor(Color.YELLOW);
        font.draw(batch, s.unitInteriorLabel != null ? s.unitInteriorLabel : "", 20f, titleY);

        // Rest button
        glyph.setText(font, "Rest");
        font.setColor(Color.WHITE);
        font.draw(batch, "Rest",
                s.restBtnX + (REST_W - glyph.width) / 2,
                s.restBtnY + (BTN_H + glyph.height) / 2);
        smallFont.setColor(Color.WHITE);
        float oneHrY = s.restBtnY + (BTN_H + smallFont.getLineHeight() * 0.5f) / 2;
        smallFont.draw(batch, "1 hr", s.restBtnX + REST_W + ONE_HR_GAP, oneHrY);

        // Sleep button (same row, right after "1 hr" text) — greyed out during daytime
        glyph.setText(font, "Sleep");
        font.setColor(isNight ? Color.WHITE : DISABLED_TEXT_COLOR);
        font.draw(batch, "Sleep",
                s.sleepBtnX + (SLEEP_W - glyph.width) / 2,
                s.sleepBtnY + (BTN_H + glyph.height) / 2);
        smallFont.setColor(isNight ? Color.WHITE : DISABLED_TEXT_COLOR);
        smallFont.draw(batch, "until 6:00", s.sleepBtnX + SLEEP_W + ONE_HR_GAP,
                s.sleepBtnY + (BTN_H + smallFont.getLineHeight() * 0.5f) / 2);
        if (showStashEmail) {
            glyph.setText(font, "Open Stash");
            font.setColor(Color.WHITE);
            font.draw(batch, "Open Stash",
                    s.openStashBtnX + (STASH_W - glyph.width) / 2,
                    s.openStashBtnY + (BTN_H + glyph.height) / 2);
            glyph.setText(font, "Check Emails");
            font.setColor(Color.WHITE);
            font.draw(batch, "Check Emails",
                    s.checkEmailsBtnX + (EMAIL_W - glyph.width) / 2,
                    s.checkEmailsBtnY + (BTN_H + glyph.height) / 2);
        }
        glyph.setText(font, "Exit");
        font.setColor(Color.WHITE);
        font.draw(batch, "Exit",
                s.unitExitBtnX + (EXIT_W - glyph.width) / 2,
                s.unitExitBtnY + (BTN_H + glyph.height) / 2);
        smallFont.setColor(Color.WHITE);
        batch.end();
    }
}

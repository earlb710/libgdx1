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

    private static final Color BG_COLOR        = new Color(0.08f, 0.08f, 0.18f, 1f);
    private static final Color BORDER_COLOR    = new Color(0.5f,  0.5f,  0.8f,  1f);
    private static final Color REST_BTN_COLOR  = new Color(0.4f,  0.25f, 0.05f, 1f);
    private static final Color SLEEP_BTN_COLOR = new Color(0.08f, 0.08f, 0.45f, 1f);
    private static final Color STASH_BTN_COLOR = new Color(0.35f, 0.15f, 0.50f, 1f);
    private static final Color EMAIL_BTN_COLOR = new Color(0.10f, 0.30f, 0.50f, 1f);
    private static final Color EXIT_BTN_COLOR  = new Color(0.35f, 0.05f, 0.05f, 1f);

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

        int curHour = profile.getCurrentHour();
        boolean showRest  = curHour >= 5 && curHour < 20;
        boolean showSleep = !showRest;

        // Layout: title first, then buttons below it
        final float btnX = 20f;
        float titleY = panelH - PAD_Y - fontCapH;   // top of title text
        float curY   = titleY - fontCapH - BTN_SPACING - BTN_H; // first button below title

        s.restBtnX  = btnX; s.restBtnW  = showRest  ? REST_W  : 0f; s.restBtnH  = BTN_H;
        s.restBtnY  = curY;
        if (showRest)  curY -= BTN_H + BTN_SPACING;

        s.sleepBtnX = btnX; s.sleepBtnW = showSleep ? SLEEP_W : 0f; s.sleepBtnH = BTN_H;
        s.sleepBtnY = curY;
        if (showSleep) curY -= BTN_H + BTN_SPACING;

        // Open Stash and Check Emails — always visible when inside office
        s.openStashBtnX  = btnX; s.openStashBtnW  = STASH_W; s.openStashBtnH  = BTN_H;
        s.openStashBtnY  = curY;
        curY -= BTN_H + BTN_SPACING;

        s.checkEmailsBtnX = btnX; s.checkEmailsBtnW = EMAIL_W; s.checkEmailsBtnH = BTN_H;
        s.checkEmailsBtnY = curY;
        curY -= BTN_H + BTN_SPACING;

        s.unitExitBtnX = btnX; s.unitExitBtnW = EXIT_W; s.unitExitBtnH = BTN_H;
        s.unitExitBtnY = curY;

        // --- Draw background ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(0, 0, panelW, panelH);
        if (showRest) {
            sr.setColor(REST_BTN_COLOR);
            sr.rect(s.restBtnX, s.restBtnY, REST_W, BTN_H);
        }
        if (showSleep) {
            sr.setColor(SLEEP_BTN_COLOR);
            sr.rect(s.sleepBtnX, s.sleepBtnY, SLEEP_W, BTN_H);
        }
        sr.setColor(STASH_BTN_COLOR);
        sr.rect(s.openStashBtnX, s.openStashBtnY, STASH_W, BTN_H);
        sr.setColor(EMAIL_BTN_COLOR);
        sr.rect(s.checkEmailsBtnX, s.checkEmailsBtnY, EMAIL_W, BTN_H);
        sr.setColor(EXIT_BTN_COLOR);
        sr.rect(s.unitExitBtnX, s.unitExitBtnY, EXIT_W, BTN_H);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.line(0, panelH, panelW, panelH);
        if (showRest) {
            sr.rect(s.restBtnX,     s.restBtnY,     REST_W,     BTN_H);
            sr.rect(s.restBtnX + 1, s.restBtnY + 1, REST_W - 2, BTN_H - 2);
        }
        if (showSleep) {
            sr.rect(s.sleepBtnX,     s.sleepBtnY,     SLEEP_W,     BTN_H);
            sr.rect(s.sleepBtnX + 1, s.sleepBtnY + 1, SLEEP_W - 2, BTN_H - 2);
        }
        sr.rect(s.openStashBtnX,     s.openStashBtnY,     STASH_W,     BTN_H);
        sr.rect(s.openStashBtnX + 1, s.openStashBtnY + 1, STASH_W - 2, BTN_H - 2);
        sr.rect(s.checkEmailsBtnX,     s.checkEmailsBtnY,     EMAIL_W,     BTN_H);
        sr.rect(s.checkEmailsBtnX + 1, s.checkEmailsBtnY + 1, EMAIL_W - 2, BTN_H - 2);
        sr.rect(s.unitExitBtnX,     s.unitExitBtnY,     EXIT_W,     BTN_H);
        sr.rect(s.unitExitBtnX + 1, s.unitExitBtnY + 1, EXIT_W - 2, BTN_H - 2);
        sr.end();

        // --- Draw text ---
        batch.begin();
        // Unit title (uses same titleY computed above)
        font.setColor(Color.YELLOW);
        font.draw(batch, s.unitInteriorLabel != null ? s.unitInteriorLabel : "", 20f, titleY);

        if (showRest) {
            glyph.setText(font, "Rest");
            font.setColor(Color.WHITE);
            font.draw(batch, "Rest",
                    s.restBtnX + (REST_W - glyph.width) / 2,
                    s.restBtnY + (BTN_H + glyph.height) / 2);
            smallFont.setColor(Color.WHITE);
            smallFont.draw(batch, "1 hr", s.restBtnX + REST_W + 10f,
                    s.restBtnY + (BTN_H + smallFont.getLineHeight() * 0.5f) / 2);
        }
        if (showSleep) {
            glyph.setText(font, "Sleep");
            font.setColor(Color.WHITE);
            font.draw(batch, "Sleep",
                    s.sleepBtnX + (SLEEP_W - glyph.width) / 2,
                    s.sleepBtnY + (BTN_H + glyph.height) / 2);
            smallFont.setColor(Color.WHITE);
            smallFont.draw(batch, "until 6:00", s.sleepBtnX + SLEEP_W + 10f,
                    s.sleepBtnY + (BTN_H + smallFont.getLineHeight() * 0.5f) / 2);
        }
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
        glyph.setText(font, "Exit");
        font.setColor(Color.WHITE);
        font.draw(batch, "Exit",
                s.unitExitBtnX + (EXIT_W - glyph.width) / 2,
                s.unitExitBtnY + (BTN_H + glyph.height) / 2);
        smallFont.setColor(Color.WHITE);
        batch.end();
    }
}

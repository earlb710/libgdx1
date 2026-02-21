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
        TextMeasurer.TextBounds restBounds  = TextMeasurer.measure(font, glyph, "Rest",  PAD_X, PAD_Y);
        TextMeasurer.TextBounds sleepBounds = TextMeasurer.measure(font, glyph, "Sleep", PAD_X, PAD_Y);
        TextMeasurer.TextBounds exitBounds  = TextMeasurer.measure(font, glyph, "Exit",  PAD_X, PAD_Y);
        final float BTN_H   = restBounds.height;
        final float REST_W  = restBounds.width;
        final float SLEEP_W = sleepBounds.width;
        final float EXIT_W  = exitBounds.width;
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
        glyph.setText(font, "Exit");
        font.setColor(Color.WHITE);
        font.draw(batch, "Exit",
                s.unitExitBtnX + (EXIT_W - glyph.width) / 2,
                s.unitExitBtnY + (BTN_H + glyph.height) / 2);
        smallFont.setColor(Color.WHITE);
        batch.end();

        // Exit confirmation overlay (drawn on top of normal panel)
        if (s.exitConfirming) {
            drawExitConfirmation(s);
        }
    }

    /** Draws a modal "Exit : Are you sure?" confirmation over the unit interior panel. */
    private void drawExitConfirmation(MapViewState s) {
        final float PAD_X = 24f, PAD_Y = 10f, BTN_SPACING = 20f;
        final float panelH = s.infoAreaHeight;
        final float panelW = s.screenWidth;

        // Semi-transparent dark overlay
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(new Color(0f, 0f, 0f, 0.82f));
        sr.rect(0, 0, panelW, panelH);
        sr.end();

        // Measure buttons
        TextMeasurer.TextBounds yesBounds = TextMeasurer.measure(font, glyph, "Yes", PAD_X, PAD_Y);
        TextMeasurer.TextBounds noBounds  = TextMeasurer.measure(font, glyph, "No",  PAD_X, PAD_Y);
        final float BTN_H = yesBounds.height;
        final float YES_W = yesBounds.width;
        final float NO_W  = noBounds.width;

        // Measure question text
        glyph.setText(font, "Exit : Are you sure?");
        final float qW = glyph.width;
        final float qH = glyph.height;

        // Layout: centre both question and buttons
        float midY   = panelH / 2f;
        float btnY   = midY - BTN_H / 2f - 8f;
        float textY  = midY + qH + BTN_H / 2f + 14f;
        float totalBtnW = YES_W + BTN_SPACING + NO_W;
        float btnStartX = (panelW - totalBtnW) / 2f;

        s.exitYesBtnX = btnStartX;           s.exitYesBtnY = btnY;
        s.exitYesBtnW = YES_W;               s.exitYesBtnH = BTN_H;
        s.exitNoBtnX  = btnStartX + YES_W + BTN_SPACING; s.exitNoBtnY = btnY;
        s.exitNoBtnW  = NO_W;                s.exitNoBtnH  = BTN_H;

        // Button fills
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(new Color(0.1f, 0.45f, 0.1f, 1f));
        sr.rect(s.exitYesBtnX, s.exitYesBtnY, YES_W, BTN_H);
        sr.setColor(new Color(0.5f, 0.05f, 0.05f, 1f));
        sr.rect(s.exitNoBtnX,  s.exitNoBtnY,  NO_W,  BTN_H);
        sr.end();

        // Button borders (2px)
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(Color.WHITE);
        sr.rect(s.exitYesBtnX,     s.exitYesBtnY,     YES_W,     BTN_H);
        sr.rect(s.exitYesBtnX + 1, s.exitYesBtnY + 1, YES_W - 2, BTN_H - 2);
        sr.rect(s.exitNoBtnX,      s.exitNoBtnY,      NO_W,      BTN_H);
        sr.rect(s.exitNoBtnX  + 1, s.exitNoBtnY  + 1, NO_W  - 2, BTN_H - 2);
        sr.end();

        // Text
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Exit : Are you sure?", (panelW - qW) / 2f, textY);

        glyph.setText(font, "Yes");
        font.setColor(Color.WHITE);
        font.draw(batch, "Yes",
                s.exitYesBtnX + (YES_W - glyph.width) / 2f,
                s.exitYesBtnY + (BTN_H + glyph.height) / 2f);

        glyph.setText(font, "No");
        font.draw(batch, "No",
                s.exitNoBtnX + (NO_W - glyph.width) / 2f,
                s.exitNoBtnY + (BTN_H + glyph.height) / 2f);
        batch.end();
    }
}

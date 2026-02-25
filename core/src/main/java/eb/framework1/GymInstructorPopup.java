package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Modal popup shown when the player "Talks to Instructor" at the gym.
 *
 * <p>Presents four training options grouped by goal and session type:
 * <ol>
 *   <li>Strength – Self-guided  ($15, 90 min, 35 % chance)</li>
 *   <li>Strength – Personal Trainer  ($60, 60 min, 70 % chance)</li>
 *   <li>Stamina  – Self-guided  ($15, 90 min, 35 % chance)</li>
 *   <li>Stamina  – Personal Trainer  ($60, 60 min, 70 % chance)</li>
 * </ol>
 * A Cancel button is shown at the bottom.
 *
 * <h3>Usage (MainScreen)</h3>
 * <pre>
 *   gymInstructorPopup.show();
 *   // each frame:
 *   gymInstructorPopup.draw(screenW, screenH);
 *   // on tap:
 *   int option = gymInstructorPopup.onTap(screenX, flippedY);
 *   if (option >= 0) handleGymTraining(option);
 * </pre>
 *
 * <p>{@code option} values match the {@code BuildingServices.GYM_OPT_*} constants.
 * A return value of {@code -1} means Cancel was tapped; {@code -2} is a miss.
 */
class GymInstructorPopup {

    private static final Color BG_COLOR     = new Color(0.08f, 0.12f, 0.08f, 1f);
    private static final Color BORDER_COLOR = new Color(0.40f, 0.80f, 0.40f, 1f);
    private static final Color TITLE_COLOR  = new Color(0.70f, 1.00f, 0.70f, 1f);
    private static final Color LABEL_COLOR  = new Color(0.70f, 0.90f, 1.00f, 1f);
    private static final Color SELF_COLOR   = new Color(0.10f, 0.40f, 0.12f, 1f);
    private static final Color PT_COLOR     = new Color(0.20f, 0.10f, 0.50f, 1f);
    private static final Color CANCEL_COLOR = new Color(0.40f, 0.10f, 0.10f, 1f);

    private static final int NUM_OPTIONS = 4;

    // Displayed names for each option (index = GYM_OPT_* constant)
    private static final String[] OPTION_NAMES = {
        "Strength — Self-guided",
        "Strength — Personal Trainer",
        "Stamina  — Self-guided",
        "Stamina  — Personal Trainer",
    };
    private static final int[]   OPTION_COST  = {
        BuildingServices.GYM_COST_SELF,
        BuildingServices.GYM_COST_PT,
        BuildingServices.GYM_COST_SELF,
        BuildingServices.GYM_COST_PT,
    };
    private static final int[]   OPTION_TIME  = {
        BuildingServices.GYM_TIME_SELF,
        BuildingServices.GYM_TIME_PT,
        BuildingServices.GYM_TIME_SELF,
        BuildingServices.GYM_TIME_PT,
    };
    private static final float[] OPTION_CHANCE = {
        BuildingServices.GYM_CHANCE_SELF,
        BuildingServices.GYM_CHANCE_PT,
        BuildingServices.GYM_CHANCE_SELF,
        BuildingServices.GYM_CHANCE_PT,
    };
    private static final boolean[] IS_PT = { false, true, false, true };

    // --- Rendering resources ---
    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyph;

    // --- State ---
    private boolean visible = false;

    // Button bounds (written during draw, read by onTap)
    private final float[] optBtnX = new float[NUM_OPTIONS];
    private final float[] optBtnY = new float[NUM_OPTIONS];
    private final float[] optBtnW = new float[NUM_OPTIONS];
    private float          optBtnH  = 0f;
    private float cancelX, cancelY, cancelW, cancelH;

    // -------------------------------------------------------------------------

    GymInstructorPopup(SpriteBatch batch, ShapeRenderer sr,
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

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    /** Opens the popup. */
    void show() {
        visible  = true;
        optBtnH  = 0f;
        cancelH  = 0f;
        Gdx.app.log("GymInstructorPopup", "Showing");
    }

    /**
     * Handles a tap on this popup.
     *
     * @return the training option index (0–3, matching {@code BuildingServices.GYM_OPT_*})
     *         when an option button was tapped; {@code -1} when Cancel was tapped
     *         (popup auto-dismissed); {@code -2} for a miss (popup stays open).
     */
    int onTap(int screenX, int flippedY) {
        if (!visible) return -2;

        for (int i = 0; i < NUM_OPTIONS; i++) {
            if (optBtnW[i] > 0
                    && screenX >= optBtnX[i] && screenX <= optBtnX[i] + optBtnW[i]
                    && flippedY >= optBtnY[i] && flippedY <= optBtnY[i] + optBtnH) {
                visible = false;
                return i;
            }
        }
        if (cancelW > 0
                && screenX >= cancelX && screenX <= cancelX + cancelW
                && flippedY >= cancelY && flippedY <= cancelY + cancelH) {
            visible = false;
            return -1;
        }
        return -2;
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    void draw(int screenW, int screenH) {
        if (!visible) return;

        final float PAD         = 24f;
        final float GAP         = 10f;
        final float BTN_SPACING = 12f;
        final float MIN_W       = 340f;
        final float MAX_W       = screenW * 0.90f;

        // --- Font metrics ---
        glyph.setText(font, "Hg");
        float fontH     = glyph.height;
        float fontLineH = fontH + GAP;
        glyph.setText(smallFont, "Hg");
        float smallH     = glyph.height;
        float smallLineH = smallH + GAP;

        // --- Button and label sizing ---
        TextMeasurer.TextBounds cancelBounds = TextMeasurer.measure(font, glyph, "Cancel", 28f, 12f);
        float cancelBtnW = cancelBounds.width;
        float cancelBtnH = cancelBounds.height;

        // Widest option label (name + cost + time suffix)
        float maxOptLabelW = cancelBtnW;
        for (int i = 0; i < NUM_OPTIONS; i++) {
            String label = optionButtonLabel(i);
            glyph.setText(font, label);
            if (glyph.width > maxOptLabelW) maxOptLabelW = glyph.width;
        }
        // Widest chance line
        float maxChanceW = 0f;
        for (int i = 0; i < NUM_OPTIONS; i++) {
            glyph.setText(smallFont, chanceLine(i));
            if (glyph.width > maxChanceW) maxChanceW = glyph.width;
        }

        // Each option occupies one main-font button row + one smallFont chance line
        float singleOptH = cancelBtnH + smallLineH;

        float optW = Math.max(maxOptLabelW + 2 * PAD, maxChanceW + 2 * PAD);
        optBtnH = cancelBtnH;

        // Header lines
        String title     = "Fitness Instructor";
        String subTitle  = "Choose your training session:";
        glyph.setText(font, title);
        float titleW = glyph.width;
        glyph.setText(smallFont, subTitle);
        float subW   = glyph.width;

        float contentW = Math.max(optW, Math.max(titleW, subW));
        float dialogW  = Math.min(MAX_W, Math.max(MIN_W, contentW + 2 * PAD));

        // Height
        float dialogH = PAD
                + fontLineH + fontH                          // title + character-size gap
                + smallLineH                                 // subtitle
                + GAP                                        // spacer
                + NUM_OPTIONS * (singleOptH + BTN_SPACING)  // 4 option rows
                + cancelBtnH + BTN_SPACING                   // cancel
                + PAD;

        float dialogX = (screenW - dialogW) / 2f;
        float dialogY = (screenH - dialogH) / 2f;

        // Layout buttons bottom-up, storing positions for hit-testing
        float cy = dialogY + PAD;

        cancelX = dialogX + (dialogW - cancelBtnW) / 2f;
        cancelY = cy;
        cancelW = cancelBtnW;
        cancelH = cancelBtnH;
        cy += cancelBtnH + BTN_SPACING;

        // Option rows are stored in reverse (bottom up) but we draw them top-down later
        // Store in reverse order of visual top-to-bottom display:
        for (int i = NUM_OPTIONS - 1; i >= 0; i--) {
            // chance-line occupies the space below the button during layout pass
            cy += smallLineH;  // skip chance-line height (drawn below button)
            optBtnX[i] = dialogX + (dialogW - optW) / 2f;
            optBtnY[i] = cy;
            optBtnW[i] = optW;
            cy += cancelBtnH + BTN_SPACING;
        }

        // --- Shapes ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(dialogX, dialogY, dialogW, dialogH);
        for (int i = 0; i < NUM_OPTIONS; i++) {
            sr.setColor(IS_PT[i] ? PT_COLOR : SELF_COLOR);
            sr.rect(optBtnX[i], optBtnY[i], optBtnW[i], optBtnH);
        }
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
        glyph.setText(font, title);
        font.draw(batch, title, dialogX + (dialogW - glyph.width) / 2f, ty);
        ty -= fontLineH + fontH;

        // Subtitle
        smallFont.setColor(LABEL_COLOR);
        glyph.setText(smallFont, subTitle);
        smallFont.draw(batch, subTitle, dialogX + (dialogW - glyph.width) / 2f, ty);
        ty -= smallLineH + GAP;

        // Option button labels and chance lines
        for (int i = 0; i < NUM_OPTIONS; i++) {
            // Button label
            String label = optionButtonLabel(i);
            glyph.setText(font, label);
            font.setColor(Color.WHITE);
            font.draw(batch, label,
                    optBtnX[i] + (optBtnW[i] - glyph.width) / 2f,
                    optBtnY[i] + (optBtnH + glyph.height) / 2f);
            // Chance line below button
            String cLine = chanceLine(i);
            glyph.setText(smallFont, cLine);
            smallFont.setColor(LABEL_COLOR);
            smallFont.draw(batch, cLine,
                    optBtnX[i] + (optBtnW[i] - glyph.width) / 2f,
                    optBtnY[i] - smallH);
        }

        // Cancel
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

    /** Returns the button label, e.g. "Strength — Self-guided  ($15, 1h 30 min)". */
    private static String optionButtonLabel(int i) {
        int cost = OPTION_COST[i];
        int mins = OPTION_TIME[i];
        int h = mins / 60, m = mins % 60;
        String timeStr = (h > 0 && m > 0) ? h + "h " + m + " min"
                       : (h > 0)           ? h + "h"
                       :                     m + " min";
        return OPTION_NAMES[i] + "  ($" + cost + ", " + timeStr + ")";
    }

    /** Returns the descriptive chance line, e.g. "35% chance of +1 Strength". */
    static String chanceLine(int i) {
        String attr = (i == BuildingServices.GYM_OPT_STRENGTH_SELF
                    || i == BuildingServices.GYM_OPT_STRENGTH_PT) ? "Strength" : "Stamina";
        int pct = Math.round(OPTION_CHANCE[i] * 100f);
        return pct + "% chance of +1 " + attr + " (first session today)";
    }
}

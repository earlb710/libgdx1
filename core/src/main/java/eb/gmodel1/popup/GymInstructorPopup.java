package eb.gmodel1.popup;

import eb.gmodel1.city.*;
import eb.gmodel1.screen.*;


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
public class GymInstructorPopup {

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

    public GymInstructorPopup(SpriteBatch batch, ShapeRenderer sr,
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

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    /** Opens the popup. */
    public void show() {
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
    public int onTap(int screenX, int flippedY) {
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

    public void draw(int screenW, int screenH) {
        if (!visible) return;

        final float PAD         = 16f;
        final float GAP         = 8f;
        final float BTN_PAD_X   = 20f;
        final float BTN_PAD_Y   = 10f;
        final float SECTION_GAP = 12f;

        // --- Font metrics ---
        glyph.setText(font, "Hg");
        float fontH  = glyph.height;
        glyph.setText(smallFont, "Hg");
        float smallH = glyph.height;

        // --- Measure all content widths ---
        final String title    = "Fitness Instructor";
        final String subTitle = "Choose your training session:";
        glyph.setText(font,      title);    float titleW = glyph.width;
        glyph.setText(smallFont, subTitle); float subW   = glyph.width;

        float maxOptLabelW = 0f;
        for (int i = 0; i < NUM_OPTIONS; i++) {
            glyph.setText(font, optionButtonLabel(i));
            if (glyph.width > maxOptLabelW) maxOptLabelW = glyph.width;
        }
        float maxChanceW = 0f;
        for (int i = 0; i < NUM_OPTIONS; i++) {
            glyph.setText(smallFont, chanceLine(i));
            if (glyph.width > maxChanceW) maxChanceW = glyph.width;
        }
        glyph.setText(font, "Cancel");
        float cancelTextW = glyph.width;

        // --- Button dimensions ---
        float optBtnW_val    = Math.max(maxOptLabelW + 2 * BTN_PAD_X,
                                        maxChanceW   + 2 * BTN_PAD_X);
        float optBtnH_val    = fontH  + 2 * BTN_PAD_Y;
        float cancelBtnW_val = cancelTextW + 2 * BTN_PAD_X;
        float cancelBtnH_val = fontH  + 2 * BTN_PAD_Y;

        // --- Dialog dimensions ---
        float contentW = Math.max(titleW, Math.max(subW,
                         Math.max(optBtnW_val, cancelBtnW_val)));
        float dialogW  = Math.min(screenW * 0.92f, contentW + 2 * PAD);

        // Clamp buttons so they always fit within the dialog
        optBtnW_val    = Math.min(optBtnW_val,    dialogW - 2 * PAD);
        cancelBtnW_val = Math.min(cancelBtnW_val, dialogW - 2 * PAD);

        float chanceLineH = smallH + GAP;
        float dialogH = PAD
                + fontH       + GAP                                          // title
                + smallH      + GAP + SECTION_GAP                           // subtitle
                + NUM_OPTIONS * (optBtnH_val + chanceLineH + SECTION_GAP)   // 4 option blocks
                + cancelBtnH_val                                             // cancel
                + PAD;
        dialogH = Math.min(dialogH, screenH * 0.95f);

        float dialogX    = (screenW - dialogW) / 2f;
        float dialogY    = (screenH - dialogH) / 2f;
        float btnCenterX = dialogX + dialogW / 2f;

        // --- Single top-down layout pass ---
        // curY is the Y of the TOP edge of the next element (Y increases upward in libgdx).
        float curY = dialogY + dialogH - PAD;

        // Title
        float titleY = curY;
        curY -= fontH + GAP;

        // Subtitle
        float subY = curY;
        curY -= smallH + GAP + SECTION_GAP;

        // Option buttons + chance lines
        for (int i = 0; i < NUM_OPTIONS; i++) {
            optBtnX[i] = btnCenterX - optBtnW_val / 2f;
            optBtnY[i] = curY - optBtnH_val;          // bottom-left Y of button rect
            optBtnW[i] = optBtnW_val;
            curY -= optBtnH_val + chanceLineH + SECTION_GAP;
        }
        optBtnH = optBtnH_val;

        // Cancel
        cancelX = btnCenterX - cancelBtnW_val / 2f;
        cancelY = curY - cancelBtnH_val;
        cancelW = cancelBtnW_val;
        cancelH = cancelBtnH_val;

        // --- Draw shapes ---
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

        // --- Draw text ---
        batch.begin();

        // Title (centered)
        glyph.setText(font, title);
        font.setColor(TITLE_COLOR);
        font.draw(batch, title, dialogX + (dialogW - glyph.width) / 2f, titleY);

        // Subtitle (centered)
        glyph.setText(smallFont, subTitle);
        smallFont.setColor(LABEL_COLOR);
        smallFont.draw(batch, subTitle, dialogX + (dialogW - glyph.width) / 2f, subY);

        // Option labels + chance lines
        for (int i = 0; i < NUM_OPTIONS; i++) {
            String label = optionButtonLabel(i);
            glyph.setText(font, label);
            font.setColor(Color.WHITE);
            font.draw(batch, label,
                    optBtnX[i] + (optBtnW[i] - glyph.width) / 2f,
                    optBtnY[i] + (optBtnH + glyph.height) / 2f);

            // Chance line sits just below the button (within reserved chanceLineH space)
            String cLine = chanceLine(i);
            glyph.setText(smallFont, cLine);
            smallFont.setColor(LABEL_COLOR);
            smallFont.draw(batch, cLine,
                    optBtnX[i] + (optBtnW[i] - glyph.width) / 2f,
                    optBtnY[i] - GAP);
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
    public static String chanceLine(int i) {
        String attr = (i == BuildingServices.GYM_OPT_STRENGTH_SELF
                    || i == BuildingServices.GYM_OPT_STRENGTH_PT) ? "Strength" : "Stamina";
        int pct = Math.round(OPTION_CHANCE[i] * 100f);
        return pct + "% chance of +1 " + attr + " (first session today)";
    }
}

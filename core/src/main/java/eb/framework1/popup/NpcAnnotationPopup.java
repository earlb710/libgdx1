package eb.framework1.popup;

import eb.framework1.character.*;
import eb.framework1.ui.*;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.List;

/**
 * Modal popup that shows an NPC's profile with their skills annotated by
 * {@link SkillCategory}.
 *
 * <h3>Layout (top → bottom)</h3>
 * <ol>
 *   <li><strong>Title bar</strong> — NPC full name</li>
 *   <li><strong>Identity row</strong> — age and occupation</li>
 *   <li><strong>Skills section</strong> — one row per skill; each row shows a
 *       colour-coded category badge ({@code [Work]}, {@code [Hobbies]}, or
 *       {@code [General]}) followed by the skill's display name.</li>
 *   <li><strong>Close button</strong></li>
 * </ol>
 *
 * <h3>Skill-category colours</h3>
 * <ul>
 *   <li>{@link SkillCategory#WORK} — steel-blue badge</li>
 *   <li>{@link SkillCategory#HOBBIES} — forest-green badge</li>
 *   <li>{@link SkillCategory#GENERAL} — warm-gray badge</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Construct once
 * NpcAnnotationPopup pop = new NpcAnnotationPopup(batch, sr, font, smallFont, glyph);
 *
 * // Open when the player taps an NPC on the map
 * pop.show(npc);
 *
 * // Each frame — inside render()
 * if (pop.isVisible()) pop.draw(screenW, screenH);
 *
 * // On tap — inside touchUp
 * if (pop.isVisible() && pop.onTap(screenX, flippedY) == NpcAnnotationPopup.RESULT_CLOSE) {
 *     // popup dismissed
 * }
 * }</pre>
 */
public class NpcAnnotationPopup {

    // -------------------------------------------------------------------------
    // Result codes
    // -------------------------------------------------------------------------

    /** Returned by {@link #onTap} when the Close button was tapped. */
    public static final int RESULT_CLOSE = 1;
    /** Returned by {@link #onTap} when no interactive area was hit. */
    public static final int RESULT_NONE  = 0;

    // -------------------------------------------------------------------------
    // Colours — popup chrome
    // -------------------------------------------------------------------------

    private static final Color BG_COLOR        = new Color(0.07f, 0.09f, 0.16f, 1f);
    private static final Color BORDER_COLOR    = new Color(0.55f, 0.70f, 0.90f, 1f);
    private static final Color TITLE_COLOR     = new Color(0.85f, 0.92f, 1.00f, 1f);
    private static final Color LABEL_COLOR     = new Color(0.55f, 0.70f, 0.85f, 1f);
    private static final Color VALUE_COLOR     = Color.WHITE;
    private static final Color SECTION_COLOR   = new Color(0.70f, 0.80f, 0.95f, 1f);
    private static final Color DIVIDER_COLOR   = new Color(0.25f, 0.35f, 0.55f, 1f);
    private static final Color CLOSE_BTN_COLOR = new Color(0.35f, 0.10f, 0.10f, 1f);

    // -------------------------------------------------------------------------
    // Skill-category badge colours
    // -------------------------------------------------------------------------

    /** Badge fill colour for {@link SkillCategory#WORK}. */
    private static final Color BADGE_WORK_FILL    = new Color(0.10f, 0.30f, 0.58f, 1f);
    /** Badge fill colour for {@link SkillCategory#HOBBIES}. */
    private static final Color BADGE_HOBBIES_FILL = new Color(0.12f, 0.40f, 0.18f, 1f);
    /** Badge fill colour for {@link SkillCategory#GENERAL}. */
    private static final Color BADGE_GENERAL_FILL = new Color(0.30f, 0.28f, 0.22f, 1f);
    /** Badge text colour (shared across all categories). */
    private static final Color BADGE_TEXT_COLOR   = new Color(0.88f, 0.94f, 1.00f, 1f);

    // -------------------------------------------------------------------------
    // Badge / skill-row layout constants
    // -------------------------------------------------------------------------

    /** Vertical padding inside each category badge (pixels above and below text). */
    private static final float BADGE_PAD_V    = 3f;
    /** Horizontal padding inside each category badge (pixels left and right of text). */
    private static final float BADGE_PAD_H    = 5f;
    /** Extra height added to the badge to form the skill-row height. */
    private static final float SKILL_ROW_MARGIN = 2f;
    /** Gap in pixels between the badge right edge and the skill display name. */
    private static final float BADGE_NAME_GAP  = 8f;

    // -------------------------------------------------------------------------
    // Rendering resources
    // -------------------------------------------------------------------------

    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyph;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private boolean      visible = false;
    private NpcCharacter npc     = null;

    /** Scroll offset (y, pixels, applied to the skills section). */
    private float scrollY    = 0f;
    private float maxScrollY = 0f;

    // Hit areas — written during draw(), read by onTap()
    private float closeBtnX, closeBtnY, closeBtnW, closeBtnH;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a new {@link NpcAnnotationPopup}.
     *
     * @param batch     the sprite batch used for text rendering
     * @param sr        the shape renderer used for fills and borders
     * @param font      the default font (used for title and Close button)
     * @param smallFont the small font (used for skill rows and identity line)
     * @param glyph     the shared {@link GlyphLayout} instance for text measurement
     */
    public NpcAnnotationPopup(SpriteBatch batch, ShapeRenderer sr,
                               BitmapFont font, BitmapFont smallFont, GlyphLayout glyph) {
        this.batch     = batch;
        this.sr        = sr;
        this.font      = font;
        this.smallFont = smallFont;
        this.glyph     = glyph;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns {@code true} while the popup is open. */
    public boolean isVisible() { return visible; }

    /**
     * Opens the popup for the given NPC.
     *
     * @param npc the NPC whose profile and skills should be displayed;
     *            must not be {@code null}
     */
    public void show(NpcCharacter npc) {
        if (npc == null) return;
        this.npc     = npc;
        this.visible = true;
        this.scrollY = 0f;
        this.closeBtnW = 0f; // invalidate hit areas until first draw()
    }

    /** Closes the popup. */
    public void dismiss() {
        visible = false;
        npc     = null;
    }

    /**
     * Handles a scroll gesture for the skills list.
     * Call from the host screen's {@code scrolled()} callback.
     *
     * @param amountY scroll amount in "notches" (positive = scroll down)
     */
    public void scroll(float amountY) {
        if (!visible) return;
        scrollY = MathUtils.clamp(scrollY - amountY * 30f, 0f, maxScrollY);
    }

    /**
     * Handles a tap.  Returns {@link #RESULT_CLOSE} if the Close button was
     * tapped (the popup is also dismissed), or {@link #RESULT_NONE} otherwise.
     *
     * @param screenX  tap x in screen pixels (origin at left)
     * @param flippedY tap y in screen pixels (origin at bottom, libGDX y-up)
     */
    public int onTap(int screenX, int flippedY) {
        if (!visible || closeBtnW <= 0f) return RESULT_NONE;

        if (screenX >= closeBtnX && screenX <= closeBtnX + closeBtnW
                && flippedY >= closeBtnY && flippedY <= closeBtnY + closeBtnH) {
            dismiss();
            return RESULT_CLOSE;
        }
        return RESULT_NONE;
    }

    /**
     * Draws the popup centred on screen.
     *
     * <p>Must be called with neither the {@link SpriteBatch} nor the
     * {@link ShapeRenderer} active (this method manages both).
     *
     * @param screenW screen width in pixels
     * @param screenH screen height in pixels
     */
    public void draw(int screenW, int screenH) {
        if (!visible || npc == null) return;

        final float PAD       = 16f;
        final float GAP       = 8f;
        final float BTN_PAD_H = 28f;
        final float BTN_PAD_V = 10f;
        final float SB        = MapViewState.SCROLLBAR_THICKNESS;

        // ── Measure font metrics ─────────────────────────────────────────────
        glyph.setText(font, "Hg");
        float fontCapH  = glyph.height;
        float fontLineH = fontCapH * 1.4f;

        glyph.setText(smallFont, "Hg");
        float smallCapH  = glyph.height;
        float smallLineH = smallCapH * 1.35f;

        // Badge height = small-cap height + vertical padding on each side
        float badgeH    = smallCapH + BADGE_PAD_V * 2f;
        float skillRowH = badgeH + SKILL_ROW_MARGIN; // skill row is badge-height + a tiny margin

        // ── Popup geometry ───────────────────────────────────────────────────
        float dialogW = screenW * 0.90f;

        // Skills list virtual height
        List<NpcSkill> skills = npc.getSkills();
        float skillsVirtualH = skills.isEmpty()
                ? smallLineH
                : skills.size() * skillRowH;

        TextMeasurer.TextBounds closeBounds =
                TextMeasurer.measure(font, glyph, "Close", BTN_PAD_H, BTN_PAD_V);
        float closeBtnH_ = closeBounds.height;
        float closeBtnW_ = closeBounds.width;

        // Maximum skill viewport height (cap to avoid a very tall dialog)
        float maxViewportH = Math.min(skillsVirtualH, screenH * 0.35f);
        maxScrollY = Math.max(0f, skillsVirtualH - maxViewportH);
        scrollY    = MathUtils.clamp(scrollY, 0f, maxScrollY);

        float dialogH = PAD
                + fontLineH                        // title
                + GAP + smallLineH                 // identity
                + GAP + 1f                         // divider
                + GAP + smallLineH                 // "Skills" header
                + GAP + maxViewportH               // skill rows
                + GAP + closeBtnH_                 // Close button
                + PAD;

        float dialogX = (screenW - dialogW) / 2f;
        float dialogY = (screenH - dialogH) / 2f;

        // ── Compute layout (y increases upward) ─────────────────────────────
        float closeBtnX_  = dialogX + (dialogW - closeBtnW_) / 2f;
        float closeBtnY_  = dialogY + PAD;

        float viewportBottom = closeBtnY_ + closeBtnH_ + GAP;
        float viewportTop    = viewportBottom + maxViewportH;

        float skillsHeaderY = viewportTop + GAP;
        float dividerY      = skillsHeaderY + smallLineH + GAP;
        float identityY     = dividerY + 1f + GAP;

        // Store close button hit area
        closeBtnX = closeBtnX_; closeBtnY = closeBtnY_; closeBtnW = closeBtnW_; closeBtnH = closeBtnH_;

        float skillX = dialogX + PAD;

        // ── Pre-compute per-row badge data (shared by all rendering passes) ──
        int n = skills.size();
        String[]        badgeTexts  = new String[n];
        float[]         badgeWidths = new float[n];
        float[]         badgeFillYs = new float[n];
        float[]         rowYs       = new float[n];
        SkillCategory[] categories  = new SkillCategory[n];
        float baseY = viewportTop - skillRowH + scrollY; // screen-space Y of first row
        for (int i = 0; i < n; i++) {
            NpcSkill skill = skills.get(i);
            categories[i]  = skill.getSkillCategory();
            badgeTexts[i]  = "[" + (categories[i] != null ? categories[i].getDisplayName() : "?") + "]";
            glyph.setText(smallFont, badgeTexts[i]);
            badgeWidths[i] = glyph.width + BADGE_PAD_H * 2f;
            rowYs[i]       = baseY - i * skillRowH;
            badgeFillYs[i] = rowYs[i] + (skillRowH - badgeH) / 2f;
        }

        // ── ShapeRenderer: popup background and chrome ───────────────────────
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(dialogX, dialogY, dialogW, dialogH);
        sr.setColor(CLOSE_BTN_COLOR);
        sr.rect(closeBtnX, closeBtnY, closeBtnW, closeBtnH);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(dialogX,     dialogY,     dialogW,     dialogH);
        sr.rect(dialogX + 1, dialogY + 1, dialogW - 2, dialogH - 2);
        sr.setColor(DIVIDER_COLOR);
        sr.rect(dialogX + PAD, dividerY, dialogW - PAD * 2f, 1f);
        sr.setColor(BORDER_COLOR);
        sr.rect(closeBtnX,     closeBtnY,     closeBtnW,     closeBtnH);
        sr.rect(closeBtnX + 1, closeBtnY + 1, closeBtnW - 2, closeBtnH - 2);
        sr.end();

        // ── ShapeRenderer: skill badge fills and borders (scissor-clipped) ──
        if (n > 0) {
            int scissorX = Math.max(0, (int)(dialogX + PAD));
            int scissorY = Math.max(0, (int)viewportBottom);
            int scissorW = Math.max(0, (int)(dialogW - PAD * 2f - (maxScrollY > 0 ? SB : 0)));
            int scissorH = Math.max(0, (int)maxViewportH);
            com.badlogic.gdx.Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
            com.badlogic.gdx.Gdx.gl.glScissor(scissorX, scissorY, scissorW, scissorH);

            // Pass 1: badge fills
            sr.begin(ShapeRenderer.ShapeType.Filled);
            for (int i = 0; i < n; i++) {
                sr.setColor(categoryBadgeColor(categories[i]));
                sr.rect(skillX, badgeFillYs[i], badgeWidths[i], badgeH);
            }
            sr.end();

            // Pass 2: badge borders
            sr.begin(ShapeRenderer.ShapeType.Line);
            for (int i = 0; i < n; i++) {
                sr.setColor(BORDER_COLOR);
                sr.rect(skillX, badgeFillYs[i], badgeWidths[i], badgeH);
            }
            sr.end();

            com.badlogic.gdx.Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        }

        // ── Scrollbar ────────────────────────────────────────────────────────
        if (maxScrollY > 0f) {
            float sbX        = dialogX + dialogW - PAD / 2f - SB;
            float trackH     = maxViewportH;
            float thumbH     = Math.max(SB * 2f, trackH * trackH / (trackH + maxScrollY));
            float scrollRatio = scrollY / maxScrollY;
            float thumbY     = viewportBottom + (1f - scrollRatio) * (trackH - thumbH);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.2f, 0.2f, 0.3f, 1f);
            sr.rect(sbX, viewportBottom, SB, trackH);
            sr.setColor(0.5f, 0.5f, 0.7f, 1f);
            sr.rect(sbX, thumbY, SB, thumbH);
            sr.end();
        }

        // ── SpriteBatch: all text ────────────────────────────────────────────
        batch.begin();

        // Title — NPC name
        glyph.setText(font, npc.getFullName());
        font.setColor(TITLE_COLOR);
        font.draw(batch, npc.getFullName(),
                dialogX + (dialogW - glyph.width) / 2f,
                dialogY + dialogH - PAD);

        // Identity: "Age: X  ·  Occupation: Y"
        String identity = "Age: " + npc.getAge() + "  \u00B7  " + npc.getOccupation();
        glyph.setText(smallFont, identity);
        smallFont.setColor(VALUE_COLOR);
        smallFont.draw(batch, identity,
                dialogX + (dialogW - glyph.width) / 2f,
                identityY + smallCapH);

        // "Skills" section header
        smallFont.setColor(SECTION_COLOR);
        smallFont.draw(batch, "Skills", dialogX + PAD, skillsHeaderY + smallCapH);

        // Skill rows — scissor-clipped to viewport
        int scissorX = Math.max(0, (int)(dialogX + PAD));
        int scissorY = Math.max(0, (int)viewportBottom);
        int scissorW = Math.max(0, (int)(dialogW - PAD * 2f - (maxScrollY > 0 ? SB : 0)));
        int scissorH = Math.max(0, (int)maxViewportH);
        com.badlogic.gdx.Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        com.badlogic.gdx.Gdx.gl.glScissor(scissorX, scissorY, scissorW, scissorH);

        if (n == 0) {
            smallFont.setColor(new Color(0.50f, 0.50f, 0.60f, 1f));
            smallFont.draw(batch, "(no skills recorded)", dialogX + PAD, viewportTop + smallCapH);
        } else {
            // Pass 3: badge text + skill name (uses pre-computed arrays)
            for (int i = 0; i < n; i++) {
                float textBaseline = rowYs[i] + (skillRowH + smallCapH) / 2f;
                smallFont.setColor(BADGE_TEXT_COLOR);
                smallFont.draw(batch, badgeTexts[i], skillX + BADGE_PAD_H, textBaseline);
                float nameX = skillX + badgeWidths[i] + BADGE_NAME_GAP;
                smallFont.setColor(VALUE_COLOR);
                smallFont.draw(batch, skills.get(i).getDisplayName(), nameX, textBaseline);
            }
        }

        com.badlogic.gdx.Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        // Close button text
        font.setColor(Color.WHITE);
        glyph.setText(font, "Close");
        font.draw(batch, "Close",
                closeBtnX + (closeBtnW - glyph.width) / 2f,
                closeBtnY + (closeBtnH + glyph.height) / 2f);

        batch.end();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the badge fill colour for the given skill category.
     * Falls back to {@link #BADGE_GENERAL_FILL} for {@code null}.
     */
    private static Color categoryBadgeColor(SkillCategory cat) {
        if (cat == null) return BADGE_GENERAL_FILL;
        switch (cat) {
            case WORK:    return BADGE_WORK_FILL;
            case HOBBIES: return BADGE_HOBBIES_FILL;
            case GENERAL: return BADGE_GENERAL_FILL;
            default:      return BADGE_GENERAL_FILL;
        }
    }
}

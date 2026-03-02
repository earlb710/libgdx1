package eb.gmodel1.popup;

import eb.gmodel1.character.*;
import eb.gmodel1.screen.*;
import eb.gmodel1.ui.*;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Modal popup that lists every item the character is currently carrying and
 * lets them put individual items into the stash.
 *
 * <p>Items are presented in the same flat order used by
 * {@code MainScreen.handleEquipDrop}: main-slot items first (WEAPON, BODY,
 * LEGS, FEET) then utility items.  Case-locked items show a grey [Locked]
 * label instead of a button.
 *
 * <h3>Usage</h3>
 * <pre>
 *   putInStashPopup.show();
 *   // each frame:
 *   putInStashPopup.draw(screenW, screenH);
 *   // on tap:
 *   int result = putInStashPopup.onTap(screenX, flippedY);
 *   if (result >= 0) handleEquipDrop(result); // MainScreen stashes the item
 * </pre>
 */
public class PutInStashPopup {

    /** Returned by {@link #onTap} when the Close button is tapped. */
    public static final int RESULT_CLOSE = -1;
    /** Returned by {@link #onTap} when no button was hit. */
    public static final int RESULT_MISS  = -2;

    // --- Colors ---
    private static final Color BG_COLOR      = new Color(0.08f, 0.06f, 0.16f, 1f);
    private static final Color BORDER_COLOR  = new Color(0.55f, 0.40f, 0.80f, 1f);
    private static final Color TITLE_COLOR   = new Color(0.85f, 0.70f, 1.00f, 1f);
    private static final Color STASH_COLOR   = new Color(0.80f, 0.55f, 1.00f, 1f);
    private static final Color STASH_BTN_COLOR = new Color(0.35f, 0.15f, 0.50f, 1f);
    private static final Color CLOSE_BTN_COLOR = new Color(0.10f, 0.50f, 0.15f, 1f);
    private static final Color LOCKED_COLOR  = new Color(0.45f, 0.45f, 0.45f, 1f);
    private static final float SCROLLBAR_W   = 8f;

    // --- Rendering resources ---
    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyph;
    private final Profile       profile;

    // --- State ---
    private boolean visible   = false;
    private float   scrollY   = 0f;
    private float   maxScrollY = 0f;

    // Close button bounds
    private float closeX, closeY, closeW, closeH;

    // Per-item [Put in Stash] button bounds (one per carried item)
    private static final int MAX_ITEMS = 20;
    private final float[] stashBtnX = new float[MAX_ITEMS];
    private final float[] stashBtnY = new float[MAX_ITEMS];
    private final float[] stashBtnW = new float[MAX_ITEMS];
    private float stashBtnH = 0f;
    private int   itemCount  = 0;

    // -------------------------------------------------------------------------

    public PutInStashPopup(SpriteBatch batch, ShapeRenderer sr,
                    BitmapFont font, BitmapFont smallFont, GlyphLayout glyph,
                    Profile profile) {
        this.batch     = batch;
        this.sr        = sr;
        this.font      = font;
        this.smallFont = smallFont;
        this.glyph     = glyph;
        this.profile   = profile;
    }

    // -------------------------------------------------------------------------
    // State queries
    // -------------------------------------------------------------------------

    public boolean isVisible() { return visible; }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    public void show() {
        visible   = true;
        scrollY   = 0f;
        closeW    = 0f;
        Gdx.app.log("PutInStashPopup", "Opened");
    }

    public void scroll(float delta) {
        if (visible) scrollY = MathUtils.clamp(scrollY + delta, 0f, maxScrollY);
    }

    /**
     * Handles a tap on the popup.
     *
     * @return Flat index (≥ 0) of the item whose [Put in Stash] button was
     *         tapped (same ordering as {@code MainScreen.handleEquipDrop}),
     *         {@link #RESULT_CLOSE} if the Close button was tapped,
     *         or {@link #RESULT_MISS} if no button was hit.
     */
    public int onTap(int screenX, int flippedY) {
        if (!visible) return RESULT_MISS;

        // Close button
        if (closeW > 0
                && screenX >= closeX && screenX <= closeX + closeW
                && flippedY >= closeY && flippedY <= closeY + closeH) {
            visible = false;
            Gdx.app.log("PutInStashPopup", "Closed");
            return RESULT_CLOSE;
        }

        // [Put in Stash] buttons
        for (int i = 0; i < itemCount && i < MAX_ITEMS; i++) {
            if (stashBtnW[i] > 0
                    && screenX >= stashBtnX[i] && screenX <= stashBtnX[i] + stashBtnW[i]
                    && flippedY >= stashBtnY[i] && flippedY <= stashBtnY[i] + stashBtnH) {
                visible = false;
                Gdx.app.log("PutInStashPopup", "Stash item " + i);
                return i;
            }
        }
        return RESULT_MISS;
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    public void draw(int screenW, int screenH) {
        if (!visible) return;

        // Build the flat carried-items list (same order as MainScreen.handleEquipDrop)
        EquipmentSlot[] mainSlots = { EquipmentSlot.WEAPON, EquipmentSlot.BODY,
                                      EquipmentSlot.LEGS,   EquipmentSlot.FEET };
        List<EquipItem> items = new ArrayList<>();
        for (EquipmentSlot slot : mainSlots) {
            EquipItem item = profile.getEquipped(slot);
            if (item != null) items.add(item);
        }
        items.addAll(profile.getUtilityItems());
        itemCount = Math.min(items.size(), MAX_ITEMS);

        final float PAD   = 24f;
        final float GAP   = 10f;
        final float MIN_W = 340f;
        final float MAX_W = screenW * 0.9f;
        final float MAX_H = screenH * 0.8f;

        // Font metrics
        glyph.setText(font, "Hg");
        float fontH      = glyph.height;
        float fontLineH  = fontH + GAP;
        glyph.setText(smallFont, "Hg");
        float smallH     = glyph.height;
        float smallLineH = smallH + GAP;

        stashBtnH = smallH + 4f;

        // Button bounds
        TextMeasurer.TextBounds closeBounds = TextMeasurer.measure(font, glyph, "Close",         24f, 10f);
        float closeBtnW = closeBounds.width;
        float closeBtnH = closeBounds.height;
        glyph.setText(smallFont, "[Stash]");
        float stashLabelW = glyph.width;

        // Measure item line widths
        float maxLineW = 0f;
        for (EquipItem item : items) {
            glyph.setText(smallFont, item.getName() + "  (" + item.getSlot().getDisplayName() + ")");
            if (glyph.width > maxLineW) maxLineW = glyph.width;
        }
        glyph.setText(font, "Put in Stash");
        float titleW = glyph.width;

        float rawContentW = Math.max(titleW,
                Math.max(maxLineW + 16f + stashLabelW, closeBounds.textWidth));
        float dialogW = MathUtils.clamp(rawContentW + 2 * PAD + SCROLLBAR_W + 4f, MIN_W, MAX_W);

        // Height
        float scrollableContent = items.isEmpty()
                ? smallLineH                                 // "(nothing carried)" line
                : (fontLineH + itemCount * smallLineH);      // title + items
        float fixedH           = PAD + closeBtnH + PAD;
        float titleAndContent  = fontLineH + fontH + scrollableContent + GAP;
        float maxScrollableH   = MAX_H - fixedH - PAD;
        boolean needsScroll    = titleAndContent > maxScrollableH;
        float usedScrollH      = needsScroll ? maxScrollableH : titleAndContent;
        float dialogH          = PAD + usedScrollH + fixedH;
        maxScrollY = needsScroll ? Math.max(0f, titleAndContent - maxScrollableH) : 0f;

        float dialogX = (screenW - dialogW) / 2f;
        float dialogY = (screenH - dialogH) / 2f;

        // --- Shapes ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(dialogX, dialogY, dialogW, dialogH);

        closeX = dialogX + (dialogW - closeBtnW - SCROLLBAR_W) / 2f;
        closeY = dialogY + PAD;
        closeW = closeBtnW;
        closeH = closeBtnH;
        sr.setColor(CLOSE_BTN_COLOR);
        sr.rect(closeX, closeY, closeW, closeH);

        if (needsScroll) {
            float sbX    = dialogX + dialogW - SCROLLBAR_W - 2f;
            float trackY = dialogY + PAD + closeBtnH + PAD;
            float trackH = dialogH - 2 * PAD - closeBtnH - PAD;
            sr.setColor(0.3f, 0.3f, 0.35f, 1f);
            sr.rect(sbX, trackY, SCROLLBAR_W, trackH);
            float thumbH = MathUtils.clamp(trackH * (trackH / titleAndContent), 12f, trackH);
            float thumbY = trackY + (trackH - thumbH) * (maxScrollY > 0 ? 1f - scrollY / maxScrollY : 1f);
            sr.setColor(0.6f, 0.65f, 0.75f, 1f);
            sr.rect(sbX, thumbY, SCROLLBAR_W, thumbH);
        }
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(dialogX,     dialogY,     dialogW,     dialogH);
        sr.rect(dialogX + 1, dialogY + 1, dialogW - 2, dialogH - 2);
        sr.rect(closeX,     closeY,     closeW,     closeH);
        sr.rect(closeX + 1, closeY + 1, closeW - 2, closeH - 2);
        sr.end();

        // --- Scissor clip ---
        float clipX = dialogX + 1;
        float clipY = dialogY + PAD + closeBtnH + PAD;
        float clipW = dialogW - SCROLLBAR_W - 4f - 2f;
        float clipH = dialogH - (clipY - dialogY) - PAD;
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor((int) clipX, (int) clipY, (int) clipW, (int) Math.max(0, clipH));

        // --- Scrollable text ---
        batch.begin();
        float ty = dialogY + dialogH - PAD + scrollY;

        font.setColor(TITLE_COLOR);
        glyph.setText(font, "Put in Stash");
        font.draw(batch, "Put in Stash",
                dialogX + (dialogW - SCROLLBAR_W - glyph.width) / 2f, ty);
        ty -= fontLineH + fontH;

        // Reset stash button widths
        for (int i = 0; i < MAX_ITEMS; i++) stashBtnW[i] = 0f;

        if (items.isEmpty()) {
            smallFont.setColor(new Color(0.55f, 0.55f, 0.65f, 1f));
            glyph.setText(smallFont, "Nothing carried.");
            smallFont.draw(batch, "Nothing carried.",
                    dialogX + (dialogW - SCROLLBAR_W - glyph.width) / 2f, ty);
        } else {
            float itemAreaW = dialogW - SCROLLBAR_W - 4f;
            for (int i = 0; i < itemCount; i++) {
                EquipItem item = items.get(i);
                float iy = ty;

                // Item name + slot
                String label = item.getName() + "  (" + item.getSlot().getDisplayName() + ")";
                smallFont.setColor(Color.WHITE);
                smallFont.draw(batch, label, dialogX + PAD, iy);

                // [Stash] or [Locked] button — right-aligned
                float btnX = dialogX + itemAreaW - stashLabelW - 4f;
                float btnY = iy - smallH - 2f;

                if (iy >= clipY && iy <= clipY + clipH + smallLineH) {
                    if (item.isCaseItem()) {
                        smallFont.setColor(LOCKED_COLOR);
                        smallFont.draw(batch, "[Locked]", btnX, iy);
                        // stashBtnW[i] stays 0 — not clickable
                    } else {
                        smallFont.setColor(STASH_COLOR);
                        smallFont.draw(batch, "[Stash]", btnX, iy);
                        if (i < MAX_ITEMS) {
                            stashBtnX[i] = btnX;
                            stashBtnY[i] = btnY;
                            stashBtnW[i] = stashLabelW;
                        }
                    }
                }

                ty -= smallLineH;
            }
        }
        batch.end();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        // Close button text (always visible)
        batch.begin();
        font.setColor(Color.WHITE);
        glyph.setText(font, "Close");
        font.draw(batch, "Close",
                closeX + (closeW - glyph.width) / 2f,
                closeY + (closeH + glyph.height) / 2f);
        batch.end();
    }
}

package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * Modal popup that shows the player's stash (items stored at home).
 *
 * <p>Each stash item is listed with a {@code [Take]} button that removes the item
 * from the stash and returns it to the character's inventory.
 * A {@code Close} button at the bottom dismisses the popup.
 *
 * <h3>Usage</h3>
 * <pre>
 *   stashPopup.show();
 *   // each frame:
 *   stashPopup.draw(screenW, screenH);
 *   // on tap:
 *   int result = stashPopup.onTap(screenX, flippedY);
 *   if (result >= 0) handleTakeFromStash(result);
 * </pre>
 */
class StashPopup {

    private static final Color BG_COLOR     = new Color(0.10f, 0.08f, 0.18f, 1f);
    private static final Color BORDER_COLOR = new Color(0.55f, 0.40f, 0.80f, 1f);
    private static final Color TITLE_COLOR  = new Color(0.85f, 0.70f, 1.00f, 1f);
    private static final Color TAKE_COLOR   = new Color(0.30f, 0.70f, 0.30f, 1f);
    private static final Color BTN_COLOR    = new Color(0.10f, 0.50f, 0.15f, 1f);
    private static final float SCROLLBAR_W  = 8f;

    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyph;
    private final Profile       profile;

    private boolean visible  = false;
    private float   scrollY  = 0f;
    private float   maxScrollY = 0f;

    // Close button bounds
    private float closeX, closeY, closeW, closeH;

    // Take button bounds (screen-space Y, updated each frame)
    private static final int MAX_ITEMS = 20;
    private final float[] takeBtnX = new float[MAX_ITEMS];
    private final float[] takeBtnY = new float[MAX_ITEMS];
    private final float[] takeBtnW = new float[MAX_ITEMS];
    private float takeBtnH = 0f;
    private int   stashCount = 0;

    // -------------------------------------------------------------------------

    StashPopup(SpriteBatch batch, ShapeRenderer sr,
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

    boolean isVisible() { return visible; }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    void show() {
        visible  = true;
        scrollY  = 0f;
        closeW   = 0f;
        Gdx.app.log("StashPopup", "Opened, items=" + profile.getStash().size());
    }

    void scroll(float delta) {
        if (visible) scrollY = MathUtils.clamp(scrollY + delta, 0f, maxScrollY);
    }

    /**
     * Handles a tap on the popup.
     *
     * @return index of the stash item whose {@code [Take]} button was tapped (≥ 0),
     *         {@code -1} if the Close button was tapped (popup auto-dismissed),
     *         or {@code -2} for a miss (popup stays open).
     */
    int onTap(int screenX, int flippedY) {
        if (!visible) return -2;

        // Close button
        if (closeW > 0
                && screenX >= closeX && screenX <= closeX + closeW
                && flippedY >= closeY && flippedY <= closeY + closeH) {
            visible = false;
            Gdx.app.log("StashPopup", "Closed");
            return -1;
        }

        // [Take] buttons
        for (int i = 0; i < stashCount && i < MAX_ITEMS; i++) {
            if (takeBtnW[i] > 0
                    && screenX >= takeBtnX[i] && screenX <= takeBtnX[i] + takeBtnW[i]
                    && flippedY >= takeBtnY[i] && flippedY <= takeBtnY[i] + takeBtnH) {
                visible = false;
                Gdx.app.log("StashPopup", "Take item " + i);
                return i;
            }
        }
        return -2;
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    void draw(int screenW, int screenH) {
        if (!visible) return;

        final float PAD   = 24f;
        final float GAP   = 10f;
        final float MIN_W = 320f;
        final float MAX_W = screenW * 0.9f;
        final float MAX_H = screenH * 0.8f;

        // Font metrics
        glyph.setText(font, "Hg");
        float fontH      = glyph.height;
        float fontLineH  = fontH + GAP;
        glyph.setText(smallFont, "Hg");
        float smallH     = glyph.height;
        float smallLineH = smallH + GAP;

        takeBtnH = smallH + 4f;

        // OK / Close button
        TextMeasurer.TextBounds closeBounds = TextMeasurer.measure(font, glyph, "Close", 24f, 10f);
        float closeBtnW = closeBounds.width;
        float closeBtnH = closeBounds.height;

        java.util.List<EquipItem> items = profile.getStash();
        stashCount = Math.min(items.size(), MAX_ITEMS);

        // Measure "[Take]" label
        glyph.setText(smallFont, "[Take]");
        float takeLabelW = glyph.width;
        // Max item line width (name + slot)
        float maxLineW = 0f;
        for (EquipItem item : items) {
            glyph.setText(smallFont, item.getName() + "  (" + item.getSlot().getDisplayName() + ")");
            if (glyph.width > maxLineW) maxLineW = glyph.width;
        }
        glyph.setText(font, "Your Stash");
        float titleW = glyph.width;

        float rawContentW = Math.max(titleW, Math.max(maxLineW + 16f + takeLabelW, closeBounds.textWidth));
        float dialogW = MathUtils.clamp(rawContentW + 2 * PAD + SCROLLBAR_W + 4f, MIN_W, MAX_W);

        // Height
        String emptyMsg = items.isEmpty() ? "Stash is empty." : null;
        float scrollableContent = items.isEmpty()
                ? smallLineH                          // "(empty)" line
                : (fontLineH + stashCount * smallLineH);  // title + items
        // title line is part of scrollable area so it scrolls with items when overflowing
        float fixedH = PAD + closeBtnH + PAD;
        float titleAndContent = fontLineH + scrollableContent + GAP;
        float maxScrollable = MAX_H - fixedH - PAD;   // PAD for top border
        boolean needsScroll = titleAndContent > maxScrollable;
        float usedScrollH = needsScroll ? maxScrollable : titleAndContent;
        float dialogH = PAD + usedScrollH + fixedH;
        maxScrollY = needsScroll ? Math.max(0f, titleAndContent - maxScrollable) : 0f;

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
        sr.setColor(BTN_COLOR);
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
        glyph.setText(font, "Your Stash");
        font.draw(batch, "Your Stash",
                dialogX + (dialogW - SCROLLBAR_W - glyph.width) / 2f, ty);
        ty -= fontLineH;

        // Reset all take button widths
        for (int i = 0; i < MAX_ITEMS; i++) takeBtnW[i] = 0f;

        if (items.isEmpty()) {
            smallFont.setColor(new Color(0.55f, 0.55f, 0.65f, 1f));
            glyph.setText(smallFont, "Stash is empty.");
            smallFont.draw(batch, "Stash is empty.",
                    dialogX + (dialogW - SCROLLBAR_W - glyph.width) / 2f, ty);
        } else {
            float itemAreaW = dialogW - SCROLLBAR_W - 4f;
            float takeLblW  = takeLabelW;
            for (int i = 0; i < stashCount; i++) {
                EquipItem item = items.get(i);
                float iy = ty;   // screen Y of this item's text baseline

                // Item name + slot
                String label = item.getName() + "  (" + item.getSlot().getDisplayName() + ")";
                smallFont.setColor(Color.WHITE);
                smallFont.draw(batch, label, dialogX + PAD, iy);

                // [Take] button — right-aligned inside dialog (before scrollbar)
                float takeX = dialogX + itemAreaW - takeLblW - 4f;
                float takeY = iy - smallH - 2f;   // button bottom is below baseline

                if (iy >= clipY && iy <= clipY + clipH + smallLineH) {
                    smallFont.setColor(TAKE_COLOR);
                    smallFont.draw(batch, "[Take]", takeX, iy);
                    if (i < MAX_ITEMS) {
                        takeBtnX[i] = takeX;
                        takeBtnY[i] = takeY;
                        takeBtnW[i] = takeLblW;
                    }
                }

                ty -= smallLineH;
            }
        }
        batch.end();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        // Close button text (outside scissor — always visible)
        batch.begin();
        font.setColor(Color.WHITE);
        glyph.setText(font, "Close");
        font.draw(batch, "Close",
                closeX + (closeW - glyph.width) / 2f,
                closeY + (closeH + glyph.height) / 2f);
        batch.end();
    }
}

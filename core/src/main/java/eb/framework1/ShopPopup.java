package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.List;

/**
 * Modal popup shown when the player browses a shop.
 *
 * <p>Displays a list of available items.  Next to each item the price is shown
 * in yellow, followed by a green {@code [Buy]} button.  Consumable items also
 * show {@code [-]} / {@code [+]} quantity buttons so the player can choose how
 * many units to purchase in a single transaction.
 *
 * <h3>Usage (MainScreen)</h3>
 * <pre>
 *   shopPopup.show("Security Shop", items);
 *   // each frame:
 *   shopPopup.draw(screenW, screenH);
 *   // on tap:
 *   int idx = shopPopup.onTap(screenX, flippedY);
 *   if (idx >= 0) handleShopPurchase(items.get(idx), shopPopup.getLastQuantity());
 * </pre>
 *
 * <p>{@code onTap} returns the index of the purchased item (≥ 0) when a
 * {@code [Buy]} button is tapped (popup auto-dismissed), {@code -1} when the
 * {@code Close} button is tapped (also auto-dismissed), or {@code -2} for a
 * miss (popup stays open).
 */
class ShopPopup {

    private static final Color BG_COLOR      = new Color(0.06f, 0.10f, 0.20f, 1f);
    private static final Color BORDER_COLOR  = new Color(0.70f, 0.85f, 1.00f, 1f);
    private static final Color TITLE_COLOR   = new Color(1.00f, 0.90f, 0.50f, 1f);
    private static final Color PRICE_COLOR   = new Color(1.00f, 0.90f, 0.10f, 1f);
    private static final Color BUY_COLOR     = new Color(0.10f, 0.45f, 0.12f, 1f);
    private static final Color QTY_BTN_COLOR = new Color(0.20f, 0.30f, 0.50f, 1f);
    private static final Color CLOSE_COLOR   = new Color(0.40f, 0.10f, 0.10f, 1f);
    private static final Color ROW_ALT_COLOR = new Color(0.08f, 0.14f, 0.26f, 1f);
    private static final Color DESC_COLOR    = new Color(0.65f, 0.80f, 0.95f, 1f);
    private static final Color EMPTY_COLOR   = new Color(0.60f, 0.65f, 0.75f, 1f);

    private static final int MAX_ITEMS   = 20;
    private static final int MIN_QTY     = 1;
    private static final int MAX_QTY     = 99;
    private static final float SCROLLBAR_W = 8f;

    // --- Rendering resources ---
    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyph;

    // --- State ---
    private boolean      visible   = false;
    private String       title     = "";
    private List<ShopItem> items;
    private final int[]  quantities = new int[MAX_ITEMS];
    private int          lastQty    = 1;
    private float        scrollY    = 0f;
    private float        maxScrollY = 0f;

    // --- Button bounds (written during draw, read by onTap) ---
    private final float[] buyBtnX   = new float[MAX_ITEMS];
    private final float[] buyBtnY   = new float[MAX_ITEMS];
    private float          buyBtnW   = 0f;
    private float          buyBtnH   = 0f;

    private final float[] minusBtnX = new float[MAX_ITEMS];
    private final float[] plusBtnX  = new float[MAX_ITEMS];
    private float          qtyBtnY[] = new float[MAX_ITEMS]; // bottom-left Y
    private float          qtyBtnW   = 0f;
    private float          qtyBtnH   = 0f;

    private float closeX, closeY, closeW, closeH;

    // -------------------------------------------------------------------------

    ShopPopup(SpriteBatch batch, ShapeRenderer sr,
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

    boolean isVisible()      { return visible; }
    /** Returns the quantity chosen for the last purchased item. */
    int     getLastQuantity() { return lastQty; }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    /**
     * Opens the shop popup.
     *
     * @param title  heading shown at the top (e.g. "Security Shop")
     * @param items  items to display; must not be null
     */
    void show(String title, List<ShopItem> items) {
        this.title   = title != null ? title : "Shop";
        this.items   = items;
        this.visible = true;
        this.scrollY = 0f;
        this.buyBtnW = 0f;
        this.closeW  = 0f;
        int count = Math.min(items.size(), MAX_ITEMS);
        for (int i = 0; i < count; i++) quantities[i] = 1;
        Gdx.app.log("ShopPopup", "Showing '" + this.title + "' with " + items.size() + " items");
    }

    /** Scrolls the item list by {@code delta} pixels (positive = scroll down). */
    void scroll(float delta) {
        if (visible) scrollY = MathUtils.clamp(scrollY + delta, 0f, maxScrollY);
    }

    /**
     * Handles a tap on this popup.
     *
     * @return the index of the item whose {@code [Buy]} button was tapped (≥ 0,
     *         popup auto-dismissed); {@code -1} when the Close button was tapped
     *         (popup auto-dismissed); {@code -2} for a miss (popup stays open).
     */
    int onTap(int screenX, int flippedY) {
        if (!visible) return -2;

        // Close button
        if (closeW > 0
                && screenX >= closeX && screenX <= closeX + closeW
                && flippedY >= closeY && flippedY <= closeY + closeH) {
            visible = false;
            Gdx.app.log("ShopPopup", "Closed");
            return -1;
        }

        int count = items == null ? 0 : Math.min(items.size(), MAX_ITEMS);

        // Buy buttons
        if (buyBtnW > 0) {
            for (int i = 0; i < count; i++) {
                if (screenX >= buyBtnX[i] && screenX <= buyBtnX[i] + buyBtnW
                        && flippedY >= buyBtnY[i] && flippedY <= buyBtnY[i] + buyBtnH) {
                    lastQty = quantities[i];
                    visible = false;
                    Gdx.app.log("ShopPopup", "Buy item " + i
                            + " (" + items.get(i).name + ") qty=" + lastQty);
                    return i;
                }
            }
        }

        // Quantity [-] and [+] buttons (consumables only)
        if (qtyBtnW > 0) {
            for (int i = 0; i < count; i++) {
                if (items.get(i).consumable
                        && flippedY >= qtyBtnY[i] && flippedY <= qtyBtnY[i] + qtyBtnH) {
                    if (screenX >= minusBtnX[i] && screenX <= minusBtnX[i] + qtyBtnW) {
                        quantities[i] = Math.max(MIN_QTY, quantities[i] - 1);
                        return -2;
                    }
                    if (screenX >= plusBtnX[i] && screenX <= plusBtnX[i] + qtyBtnW) {
                        quantities[i] = Math.min(MAX_QTY, quantities[i] + 1);
                        return -2;
                    }
                }
            }
        }

        return -2;
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    void draw(int screenW, int screenH) {
        if (!visible) return;

        final float PAD       = 20f;
        final float GAP       = 8f;
        final float ROW_PAD_V = 6f;
        final float BTN_PAD_X = 14f;
        final float BTN_PAD_Y = 6f;
        final float MIN_W     = 340f;
        final float MAX_W     = screenW * 0.92f;
        final float MAX_H     = screenH * 0.85f;

        // --- Font metrics ---
        glyph.setText(font, "Hg");
        float fontH      = glyph.height;
        float fontLineH  = fontH + GAP;
        glyph.setText(smallFont, "Hg");
        float smallH     = glyph.height;
        float smallLineH = smallH + GAP;

        int count = items == null ? 0 : Math.min(items.size(), MAX_ITEMS);

        // --- Fixed-width columns ---
        // BUY button
        glyph.setText(font, "Buy");
        buyBtnW = glyph.width + 2 * BTN_PAD_X;
        buyBtnH = fontH + 2 * BTN_PAD_Y;

        // Quantity control: [-] [nn] [+]
        glyph.setText(font, "-");
        qtyBtnW = glyph.width + 2 * BTN_PAD_X;
        qtyBtnH = buyBtnH;
        glyph.setText(font, "99");
        float qtyNumW = glyph.width + 8f;
        float qtyTotalW = 2 * qtyBtnW + qtyNumW + 4f; // [-] + gap + [nn] + gap + [+]

        // Price column (widest price label)
        float maxPriceW = 0f;
        for (int i = 0; i < count; i++) {
            glyph.setText(smallFont, "$" + items.get(i).price);
            if (glyph.width > maxPriceW) maxPriceW = glyph.width;
        }
        maxPriceW += 8f; // padding

        // Close button
        glyph.setText(font, "Close");
        float closeBtnW = glyph.width + 2 * BTN_PAD_X;
        float closeBtnH = fontH + 2 * BTN_PAD_Y;
        closeW = closeBtnW;
        closeH = closeBtnH;

        // --- Item name column (takes remaining width) ---
        // Title row
        glyph.setText(font, title);
        float titleW = glyph.width;

        // Dialog width: PAD + nameCol + priceCol + [qtyTotal for consumables] + buyBtnW + PAD + scrollbarW
        // We must fit at least one consumable row with qty controls.
        float rightCols  = maxPriceW + qtyTotalW + 8f + buyBtnW;
        float minContent = Math.max(titleW, Math.max(closeBtnW, rightCols + 80f));
        float dialogW    = MathUtils.clamp(minContent + 2 * PAD + SCROLLBAR_W, MIN_W, MAX_W);
        float nameColW   = dialogW - 2 * PAD - SCROLLBAR_W - rightCols - 8f;
        nameColW         = Math.max(nameColW, 60f);

        // --- Row heights ---
        // Each row: item name line + description line (smaller) + vertical padding
        float rowH       = fontH + smallLineH + 2 * ROW_PAD_V;

        // --- Scrollable area height ---
        float scrollContentH = count > 0 ? count * rowH : smallLineH;
        float fixedH = PAD + fontLineH + GAP   // title
                     + closeBtnH + GAP          // close button at bottom
                     + PAD;
        float maxVisibleScrollH = MAX_H - fixedH;
        float visibleScrollH    = Math.min(scrollContentH, maxVisibleScrollH);
        float dialogH           = fixedH + visibleScrollH;
        maxScrollY = Math.max(0f, scrollContentH - visibleScrollH);

        float dialogX = (screenW - dialogW) / 2f;
        float dialogY = (screenH - dialogH) / 2f;

        // --- Close button (bottom) ---
        closeX = dialogX + (dialogW - closeBtnW) / 2f;
        closeY = dialogY + PAD;

        // --- Scrollable content area ---
        float scrollAreaX = dialogX + PAD;
        float scrollAreaY = dialogY + PAD + closeBtnH + GAP;
        float scrollAreaW = dialogW - 2 * PAD - SCROLLBAR_W - 4f;
        float scrollAreaH = visibleScrollH;

        // --- Background ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(dialogX, dialogY, dialogW, dialogH);

        // Row backgrounds (alternating) — drawn in scroll space
        float rowTop = scrollAreaY + scrollAreaH + scrollY; // Y of top of first row content
        for (int i = 0; i < count; i++) {
            float rowY = rowTop - (i + 1) * rowH;
            if (rowY + rowH < scrollAreaY || rowY > scrollAreaY + scrollAreaH) continue;
            if (i % 2 == 1) {
                sr.setColor(ROW_ALT_COLOR);
                sr.rect(scrollAreaX, Math.max(rowY, scrollAreaY),
                        scrollAreaW, Math.min(rowH, rowY + rowH - scrollAreaY));
            }
        }
        sr.end();

        // --- Clip to scroll area using scissor ---
        boolean scissorActive = applyScissor(screenH, scrollAreaX, scrollAreaY,
                scrollAreaW, scrollAreaH);

        // --- Compute buy/qty button positions and draw shapes ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < count; i++) {
            float rowY   = rowTop - (i + 1) * rowH;
            float btnY   = rowY + ROW_PAD_V + (rowH - 2 * ROW_PAD_V - buyBtnH) / 2f;
            float rightX = scrollAreaX + scrollAreaW; // right edge of content

            // BUY button
            buyBtnX[i] = rightX - buyBtnW;
            buyBtnY[i] = btnY;
            if (isRowVisible(rowY, rowH, scrollAreaY, scrollAreaH)) {
                sr.setColor(BUY_COLOR);
                sr.rect(buyBtnX[i], btnY, buyBtnW, buyBtnH);
            }

            // Qty controls (consumables only)
            if (items.get(i).consumable) {
                float qtyRight  = buyBtnX[i] - 8f;
                plusBtnX[i]     = qtyRight - qtyBtnW;
                minusBtnX[i]    = plusBtnX[i] - qtyNumW - 4f - qtyBtnW;
                qtyBtnY[i]     = btnY;
                if (isRowVisible(rowY, rowH, scrollAreaY, scrollAreaH)) {
                    sr.setColor(QTY_BTN_COLOR);
                    sr.rect(minusBtnX[i], btnY, qtyBtnW, qtyBtnH);
                    sr.rect(plusBtnX[i],  btnY, qtyBtnW, qtyBtnH);
                }
            }
        }
        sr.end();

        // Borders
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        for (int i = 0; i < count; i++) {
            if (!isRowVisible(rowTop - (i + 1) * rowH, rowH, scrollAreaY, scrollAreaH)) continue;
            sr.rect(buyBtnX[i],     buyBtnY[i],     buyBtnW,     buyBtnH);
            sr.rect(buyBtnX[i] + 1, buyBtnY[i] + 1, buyBtnW - 2, buyBtnH - 2);
            if (items.get(i).consumable) {
                sr.rect(minusBtnX[i],     qtyBtnY[i],     qtyBtnW,     qtyBtnH);
                sr.rect(minusBtnX[i] + 1, qtyBtnY[i] + 1, qtyBtnW - 2, qtyBtnH - 2);
                sr.rect(plusBtnX[i],      qtyBtnY[i],      qtyBtnW,     qtyBtnH);
                sr.rect(plusBtnX[i] + 1,  qtyBtnY[i] + 1,  qtyBtnW - 2, qtyBtnH - 2);
            }
        }
        sr.end();

        // --- Text in scroll area ---
        batch.begin();
        for (int i = 0; i < count; i++) {
            float rowY  = rowTop - (i + 1) * rowH;
            if (!isRowVisible(rowY, rowH, scrollAreaY, scrollAreaH)) continue;

            ShopItem item    = items.get(i);
            float    textTop = rowY + rowH - ROW_PAD_V;

            // Item name (left, main font)
            font.setColor(Color.WHITE);
            glyph.setText(font, item.name);
            String nameStr = item.name;
            // Truncate name if too long for column
            while (glyph.width > nameColW && nameStr.length() > 1) {
                nameStr = nameStr.substring(0, nameStr.length() - 1);
                glyph.setText(font, nameStr + "…");
            }
            if (!nameStr.equals(item.name)) nameStr = nameStr + "…";
            font.draw(batch, nameStr, scrollAreaX, textTop);

            // Description (second line, small font)
            if (!item.description.isEmpty()) {
                String desc = item.description;
                glyph.setText(smallFont, desc);
                float maxDescW = nameColW + maxPriceW;
                while (glyph.width > maxDescW && desc.length() > 1) {
                    desc = desc.substring(0, desc.length() - 1);
                    glyph.setText(smallFont, desc + "…");
                }
                if (!desc.equals(item.description)) desc = desc + "…";
                smallFont.setColor(DESC_COLOR);
                smallFont.draw(batch, desc, scrollAreaX, textTop - fontH - 2f);
            }

            // Price in yellow (right of name column)
            String priceStr = "$" + item.price;
            glyph.setText(smallFont, priceStr);
            float priceX = scrollAreaX + nameColW + 4f;
            smallFont.setColor(PRICE_COLOR);
            smallFont.draw(batch, priceStr, priceX,
                    buyBtnY[i] + (buyBtnH + glyph.height) / 2f);

            // BUY label
            glyph.setText(font, "Buy");
            font.setColor(Color.WHITE);
            font.draw(batch, "Buy",
                    buyBtnX[i] + (buyBtnW - glyph.width) / 2f,
                    buyBtnY[i] + (buyBtnH + glyph.height) / 2f);

            // Qty controls for consumables
            if (item.consumable) {
                // [-]
                glyph.setText(font, "-");
                font.setColor(Color.WHITE);
                font.draw(batch, "-",
                        minusBtnX[i] + (qtyBtnW - glyph.width) / 2f,
                        qtyBtnY[i] + (qtyBtnH + glyph.height) / 2f);
                // [+]
                glyph.setText(font, "+");
                font.draw(batch, "+",
                        plusBtnX[i] + (qtyBtnW - glyph.width) / 2f,
                        qtyBtnY[i] + (qtyBtnH + glyph.height) / 2f);
                // quantity number (between - and +)
                String qtyStr = String.valueOf(quantities[i]);
                glyph.setText(font, qtyStr);
                float qtyNumX = minusBtnX[i] + qtyBtnW + (qtyNumW - glyph.width) / 2f + 2f;
                font.setColor(Color.WHITE);
                font.draw(batch, qtyStr,
                        qtyNumX,
                        qtyBtnY[i] + (qtyBtnH + glyph.height) / 2f);
            }
        }

        // Empty message
        if (count == 0) {
            glyph.setText(smallFont, "Nothing available.");
            smallFont.setColor(EMPTY_COLOR);
            smallFont.draw(batch, "Nothing available.",
                    scrollAreaX + (scrollAreaW - glyph.width) / 2f,
                    scrollAreaY + scrollAreaH / 2f + smallH / 2f);
        }
        batch.end();

        if (scissorActive) {
            com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_SCISSOR_TEST);
        }

        // --- Elements outside scroll area ---
        // Scrollbar
        if (maxScrollY > 0f) {
            float sbX   = dialogX + dialogW - PAD / 2f - SCROLLBAR_W;
            float sbH   = scrollAreaH;
            float sbY   = scrollAreaY;
            float thumbH = Math.max(20f, sbH * (visibleScrollH / (visibleScrollH + maxScrollY)));
            float thumbY = sbY + (sbH - thumbH) * (1f - scrollY / maxScrollY);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(new Color(0.30f, 0.35f, 0.50f, 1f));
            sr.rect(sbX, sbY, SCROLLBAR_W, sbH);
            sr.setColor(new Color(0.55f, 0.65f, 0.85f, 1f));
            sr.rect(sbX, thumbY, SCROLLBAR_W, thumbH);
            sr.end();
        }

        // Dialog border + title (outside scissor)
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(CLOSE_COLOR);
        sr.rect(closeX, closeY, closeW, closeH);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(dialogX,     dialogY,     dialogW,     dialogH);
        sr.rect(dialogX + 1, dialogY + 1, dialogW - 2, dialogH - 2);
        sr.rect(closeX,     closeY,     closeW,     closeH);
        sr.rect(closeX + 1, closeY + 1, closeW - 2, closeH - 2);
        sr.end();

        batch.begin();
        // Title
        glyph.setText(font, title);
        font.setColor(TITLE_COLOR);
        font.draw(batch, title,
                dialogX + (dialogW - glyph.width) / 2f,
                dialogY + dialogH - PAD);
        // Close label
        glyph.setText(font, "Close");
        font.setColor(Color.WHITE);
        font.draw(batch, "Close",
                closeX + (closeW - glyph.width) / 2f,
                closeY + (closeH + glyph.height) / 2f);
        batch.end();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static boolean isRowVisible(float rowY, float rowH,
                                        float areaY, float areaH) {
        return rowY + rowH > areaY && rowY < areaY + areaH;
    }

    /**
     * Applies an OpenGL scissor rectangle so that only the scroll area is drawn.
     * In LibGDX the viewport Y starts at the bottom, so we must convert.
     *
     * @return {@code true} if the scissor test was enabled (caller must disable it).
     */
    private boolean applyScissor(int screenH,
                                  float areaX, float areaY,
                                  float areaW, float areaH) {
        int sx = (int) areaX;
        int sy = (int) areaY;
        int sw = (int) (areaW + 1);
        int sh = (int) (areaH + 1);
        if (sw <= 0 || sh <= 0) return false;
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_SCISSOR_TEST);
        com.badlogic.gdx.Gdx.gl.glScissor(sx, sy, sw, sh);
        return true;
    }
}

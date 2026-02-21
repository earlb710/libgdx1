package eb.framework1;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

import java.util.ArrayList;
import java.util.List;

/**
 * A lightweight context menu that renders at an arbitrary screen position (y-up coordinates).
 * Items are provided as label strings; actions are handled externally via {@link #onTap}.
 *
 * <p>Typical usage:
 * <pre>
 *   // On right-click:
 *   contextMenu.show(screenX, flippedY, items, font, gl, screenW, screenH);
 *
 *   // Each frame:
 *   if (contextMenu.isVisible()) contextMenu.draw(batch, sr, font, gl);
 *
 *   // On left-click:
 *   int idx = contextMenu.onTap(screenX, flippedY);
 *   contextMenu.dismiss();
 * </pre>
 */
class ContextMenu {

    // --- Colors ---
    private static final Color BG_COLOR     = new Color(0.12f, 0.12f, 0.20f, 1f);
    private static final Color BORDER_COLOR = new Color(0.50f, 0.50f, 0.65f, 1f);
    private static final Color TEXT_COLOR   = Color.WHITE;

    // --- Layout constants ---
    private static final float PAD_X       = 26f;
    private static final float PAD_Y       = 14f;
    private static final float ITEM_GAP    = 8f;   // vertical gap between items

    // --- State ---
    private boolean visible = false;
    private float   menuX, menuY;
    private float   menuW, itemH;
    private float   screenW, screenH;
    private final Matrix4 projMatrix = new Matrix4();

    private final List<String> items = new ArrayList<>();

    // -------------------------------------------------------------------------

    /**
     * Shows the menu at the given screen position (y-up).
     * The menu is automatically shifted to stay fully inside the screen bounds.
     *
     * @param screenX     x pixel of the right-click (screen coords)
     * @param flippedY    y pixel of the right-click (y-up)
     * @param newItems    item labels to display
     * @param font        font used for measurement and drawing
     * @param gl          reusable GlyphLayout for measurement
     * @param screenWidth screen pixel width
     * @param screenHeight screen pixel height
     */
    void show(float screenX, float flippedY,
              List<String> newItems,
              BitmapFont font, GlyphLayout gl,
              float screenWidth, float screenHeight) {
        items.clear();
        items.addAll(newItems);
        if (items.isEmpty()) { visible = false; return; }

        // Measure cap height and widest item
        gl.setText(font, "Hg");
        itemH = gl.height + PAD_Y * 2;
        float maxW = 0f;
        for (String item : items) {
            gl.setText(font, item);
            maxW = Math.max(maxW, gl.width);
        }
        menuW = maxW + PAD_X * 2;
        float menuH = totalHeight();

        // Position: prefer just below-right of the click; clamp to screen
        float mx = screenX + 4f;
        float my = flippedY - menuH - 4f;   // anchor just below click
        if (mx + menuW > screenWidth  - 2f) mx = screenWidth  - menuW - 2f;
        if (my < 2f)                         my = flippedY + 4f;           // flip above click
        if (my + menuH > screenHeight - 2f) my = screenHeight - menuH - 2f;
        mx = Math.max(2f, mx);
        my = Math.max(2f, my);

        menuX   = mx;
        menuY   = my;
        this.screenW = screenWidth;
        this.screenH = screenHeight;
        visible = true;
    }

    boolean isVisible()  { return visible; }
    void    dismiss()    { visible = false; }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    /**
     * Draws the context menu.  Handles its own {@code begin}/{@code end} calls
     * so the caller just needs to ensure neither batch nor shapeRenderer is
     * currently open.
     */
    void draw(SpriteBatch batch, ShapeRenderer sr, BitmapFont font, GlyphLayout gl) {
        if (!visible || items.isEmpty()) return;
        float menuH = totalHeight();

        // Ensure the ShapeRenderer uses screen-pixel coordinates.
        // This guards against any earlier resize that didn't update projView.
        sr.setProjectionMatrix(projMatrix.setToOrtho2D(0, 0, screenW, screenH));

        // Background
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(menuX, menuY, menuW, menuH);
        sr.end();

        // Border (double-line)
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(menuX,     menuY,     menuW,     menuH);
        sr.rect(menuX + 1, menuY + 1, menuW - 2, menuH - 2);
        sr.end();

        // Item text (items drawn top-to-bottom)
        batch.setProjectionMatrix(projMatrix);  // ensure same ortho2D as ShapeRenderer
        batch.begin();
        float iy = menuY + menuH;
        for (String item : items) {
            iy -= itemH + ITEM_GAP;
            gl.setText(font, item);
            font.setColor(TEXT_COLOR);
            font.draw(batch, item, menuX + PAD_X, iy + (itemH + gl.height) / 2f);
        }
        font.setColor(Color.WHITE);
        batch.end();
    }

    // -------------------------------------------------------------------------
    // Hit-testing
    // -------------------------------------------------------------------------

    /**
     * Returns the 0-based index of the item that was tapped, or {@code -1} if
     * the tap was outside the menu.
     *
     * @param screenX  x pixel of the tap (screen coords)
     * @param flippedY y pixel of the tap (y-up)
     */
    int onTap(float screenX, float flippedY) {
        if (!visible || items.isEmpty()) return -1;
        float menuH = totalHeight();
        if (screenX < menuX || screenX > menuX + menuW) return -1;
        if (flippedY < menuY || flippedY > menuY + menuH) return -1;

        // Items are drawn top-to-bottom inside [menuY+menuH … menuY]
        float relFromTop = (menuY + menuH) - flippedY;
        int idx = (int)(relFromTop / (itemH + ITEM_GAP));
        return (idx >= 0 && idx < items.size()) ? idx : -1;
    }

    /** Returns true if the given point (y-up) is within the menu bounds. */
    boolean contains(float screenX, float flippedY) {
        if (!visible || items.isEmpty()) return false;
        float menuH = totalHeight();
        return screenX >= menuX && screenX <= menuX + menuW
                && flippedY >= menuY && flippedY <= menuY + menuH;
    }

    // -------------------------------------------------------------------------

    private float totalHeight() {
        return items.size() * itemH + Math.max(0, items.size() - 1) * ITEM_GAP;
    }
}

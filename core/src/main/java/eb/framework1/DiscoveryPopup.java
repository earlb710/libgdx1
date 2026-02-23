package eb.framework1;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.List;

/**
 * Popup displayed when the player first arrives at a building (new location discovered).
 *
 * <p>Shows the building name, its description, and the list of improvements
 * that are automatically discovered upon arrival (hiddenValue == 0).
 * Dismissed via the OK button.
 */
class DiscoveryPopup {

    // --- Colours ---
    private static final Color BG_COLOR     = new Color(0.08f, 0.12f, 0.20f, 1f);
    private static final Color BORDER_COLOR = new Color(0.40f, 0.60f, 0.90f, 1f);
    private static final Color TITLE_COLOR  = new Color(1.00f, 0.85f, 0.30f, 1f);
    private static final Color BTN_COLOR    = new Color(0.10f, 0.50f, 0.15f, 1f);

    private static final float SCROLLBAR_W = 8f;

    // --- Rendering resources ---
    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyph;

    // --- State ---
    private boolean      visible = false;
    private String       buildingName;
    private String       description;
    private final List<String> improvementLines = new ArrayList<>();

    // OK button bounds (written during draw, read by onTap)
    private float okX, okY, okW, okH;

    // Scroll state
    private float scrollY   = 0f;
    private float maxScrollY = 0f;

    // -------------------------------------------------------------------------

    DiscoveryPopup(SpriteBatch batch, ShapeRenderer sr,
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

    /**
     * Shows the popup for a newly discovered building.
     *
     * @param buildingName   Name of the discovered building
     * @param description    Building description (may be null or empty)
     * @param improvementLines Formatted names of auto-discovered improvements
     */
    void show(String buildingName, String description, List<String> improvementLines) {
        this.buildingName = buildingName;
        this.description  = (description != null && !description.isEmpty()) ? description : null;
        this.improvementLines.clear();
        this.improvementLines.addAll(improvementLines);
        this.scrollY  = 0f;
        this.okW      = 0f;
        this.visible  = true;
        Gdx.app.log("DiscoveryPopup", "Showing for: " + buildingName
                + " imps=" + improvementLines.size());
    }

    /** Dismiss the popup if the OK button was tapped. */
    void onTap(int screenX, int flippedY) {
        if (visible && okW > 0
                && screenX >= okX && screenX <= okX + okW
                && flippedY >= okY && flippedY <= okY + okH) {
            visible  = false;
            scrollY  = 0f;
            Gdx.app.log("DiscoveryPopup", "Dismissed");
        }
    }

    /** Scroll the content by delta pixels (positive = scroll down). */
    void scroll(float delta) {
        if (visible) {
            scrollY = MathUtils.clamp(scrollY + delta, 0f, maxScrollY);
        }
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

        // --- Font metrics ---
        glyph.setText(font, "Hg");
        float fontH     = glyph.height;
        float fontLineH = fontH + GAP;

        glyph.setText(smallFont, "Hg");
        float smallH     = glyph.height;
        float smallLineH = smallH + GAP;

        // --- OK button ---
        TextMeasurer.TextBounds okBounds = TextMeasurer.measure(font, glyph, "OK", 24f, 10f);
        float okBtnW = okBounds.width;
        float okBtnH = okBounds.height;

        // --- Title (building name) ---
        String titleText = "Discovered: " + buildingName;
        glyph.setText(font, titleText);
        float titleW = glyph.width;

        // --- Collect scrollable lines: description + improvements ---
        List<String> scrollLines = new ArrayList<>();
        if (description != null) {
            scrollLines.add(description);
        }
        if (!improvementLines.isEmpty()) {
            scrollLines.add("Improvements found:");
            scrollLines.addAll(improvementLines);
        }

        // Measure scrollable content
        float maxLineW = titleW;
        for (String line : scrollLines) {
            glyph.setText(smallFont, line);
            maxLineW = Math.max(maxLineW, glyph.width);
        }

        float dialogW = MathUtils.clamp(maxLineW + 2 * PAD + SCROLLBAR_W + 4f, MIN_W, MAX_W);

        // Height layout:
        //   PAD + titleLine + scrollableArea + PAD + okBtnH + PAD
        float scrollableContent = scrollLines.size() * smallLineH;
        float fixedH = PAD + fontLineH + PAD + okBtnH + PAD;
        float maxScrollable = MAX_H - fixedH;
        boolean needsScroll = scrollableContent > maxScrollable;
        float usedScrollH = needsScroll ? maxScrollable : scrollableContent;
        float dialogH = fixedH + usedScrollH;
        maxScrollY = needsScroll ? Math.max(0f, scrollableContent - maxScrollable) : 0f;

        // Centre on screen
        float dialogX = (screenW - dialogW) / 2f;
        float dialogY = (screenH - dialogH) / 2f;

        // --- Shapes: background + OK button ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(dialogX, dialogY, dialogW, dialogH);

        okX = dialogX + (dialogW - okBtnW - SCROLLBAR_W) / 2f;
        okY = dialogY + PAD;
        okW = okBtnW;
        okH = okBtnH;
        sr.setColor(BTN_COLOR);
        sr.rect(okX, okY, okW, okH);

        // Vertical scrollbar
        if (needsScroll) {
            float sbX    = dialogX + dialogW - SCROLLBAR_W - 2f;
            float trackY = dialogY + PAD + okBtnH + PAD;
            float trackH = dialogH - 2 * PAD - okBtnH - PAD - fontLineH - PAD;
            sr.setColor(0.3f, 0.3f, 0.35f, 1f);
            sr.rect(sbX, trackY, SCROLLBAR_W, trackH);
            float thumbH = MathUtils.clamp(trackH * (trackH / scrollableContent), 12f, trackH);
            float thumbY = trackY + (trackH - thumbH) * (maxScrollY > 0 ? 1f - scrollY / maxScrollY : 1f);
            sr.setColor(0.6f, 0.65f, 0.75f, 1f);
            sr.rect(sbX, thumbY, SCROLLBAR_W, thumbH);
        }
        sr.end();

        // Borders
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(dialogX,     dialogY,     dialogW,     dialogH);
        sr.rect(dialogX + 1, dialogY + 1, dialogW - 2, dialogH - 2);
        sr.rect(okX,     okY,     okW,     okH);
        sr.rect(okX + 1, okY + 1, okW - 2, okH - 2);
        sr.end();

        // --- Scissor clip for scrollable text area ---
        float clipX = dialogX + 1;
        float clipY = dialogY + PAD + okBtnH + PAD;
        float clipW = dialogW - SCROLLBAR_W - 4f - 2f;
        float clipH = dialogH - (clipY - dialogY) - PAD;
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor((int) clipX, (int) clipY, (int) clipW, (int) Math.max(0, clipH));

        // --- Scrollable text ---
        batch.begin();
        float ty = dialogY + dialogH - PAD + scrollY;

        // Title line
        font.setColor(TITLE_COLOR);
        glyph.setText(font, titleText);
        font.draw(batch, titleText,
                dialogX + (dialogW - SCROLLBAR_W - glyph.width) / 2f, ty);
        ty -= fontLineH;

        // Content lines
        smallFont.setColor(Color.WHITE);
        for (String line : scrollLines) {
            glyph.setText(smallFont, line);
            smallFont.draw(batch, line, dialogX + PAD, ty);
            ty -= smallLineH;
        }
        batch.end();

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        // OK button text (always visible, outside scissor)
        batch.begin();
        font.setColor(Color.WHITE);
        glyph.setText(font, "OK");
        font.draw(batch, "OK",
                okX + (okW - glyph.width) / 2f,
                okY + (okH + glyph.height) / 2f);
        batch.end();
    }
}

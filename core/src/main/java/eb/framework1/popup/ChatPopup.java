package eb.framework1.popup;

import eb.framework1.character.NpcCharacter;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.Gdx;

import java.util.Collections;
import java.util.List;

/**
 * Modal popup that allows the player to initiate a conversation with an
 * unknown NPC they are standing next to.
 *
 * <p>For now this popup shows an introductory greeting from the unknown person
 * and a Close button.  Future versions will add dialogue choices and
 * relationship-building mechanics.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * ChatPopup popup = new ChatPopup(batch, sr, font, smallFont, glyph);
 * popup.show(npc);
 * if (popup.isVisible()) popup.draw(screenW, screenH);
 * if (popup.isVisible()) popup.onTap(screenX, flippedY);
 * }</pre>
 */
public class ChatPopup {

    // -------------------------------------------------------------------------
    // Colours
    // -------------------------------------------------------------------------

    private static final Color BG_COLOR        = new Color(0.07f, 0.12f, 0.10f, 1f);
    private static final Color BORDER_COLOR    = new Color(0.40f, 0.75f, 0.55f, 1f);
    private static final Color TITLE_COLOR     = new Color(0.85f, 1.00f, 0.90f, 1f);
    private static final Color CLOSE_BTN_COLOR = new Color(0.20f, 0.35f, 0.15f, 1f);

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

    private boolean      visible  = false;
    private NpcCharacter npc      = null;
    /** Pre-wrapped text lines for the current NPC conversation. */
    private List<String> textLines = Collections.emptyList();

    // Hit areas — written during draw(), read by onTap()
    private float closeBtnX, closeBtnY, closeBtnW, closeBtnH;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ChatPopup(SpriteBatch batch, ShapeRenderer sr,
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
     * Opens the popup showing a conversation prompt for the given NPC.
     *
     * @param npc the character to converse with; must not be {@code null}
     */
    public void show(NpcCharacter npc) {
        if (npc == null) return;
        this.npc      = npc;
        this.visible  = true;
        this.closeBtnW = 0f;
        this.textLines = Collections.emptyList();
        Gdx.app.log("ChatPopup", "Opening conversation with NPC " + npc.getId());
    }

    /** Closes the popup. */
    public void dismiss() {
        visible   = false;
        npc       = null;
        textLines = Collections.emptyList();
    }

    /**
     * Handles a tap.  If the Close button is tapped the popup is dismissed.
     *
     * @param screenX  tap x in screen pixels (left origin)
     * @param flippedY tap y in screen pixels (bottom origin, libGDX y-up)
     */
    public void onTap(int screenX, int flippedY) {
        if (!visible || closeBtnW <= 0f) return;
        if (screenX >= closeBtnX && screenX <= closeBtnX + closeBtnW
                && flippedY >= closeBtnY && flippedY <= closeBtnY + closeBtnH) {
            dismiss();
        }
    }

    /**
     * Draws the popup centred on screen.
     *
     * @param screenW screen width in pixels
     * @param screenH screen height in pixels
     */
    public void draw(int screenW, int screenH) {
        if (!visible || npc == null) return;

        final float PAD       = 18f;
        final float BTN_PAD_H = 28f;
        final float BTN_PAD_V = 10f;

        // ── Font metrics ──────────────────────────────────────────────────────
        glyph.setText(font, "Hg");
        float fontCapH  = glyph.height;
        float fontLineH = fontCapH * 1.4f;

        glyph.setText(smallFont, "Hg");
        float smallCapH  = glyph.height;
        float smallLineH = smallCapH * 1.4f;

        // ── Generate text (first draw only) ───────────────────────────────────
        float maxTextW = Math.min(screenW * 0.7f, 500f);

        if (textLines.isEmpty()) {
            boolean isFemale = "F".equalsIgnoreCase(npc.getGender());
            String pronoun = isFemale ? "woman" : "man";
            String greeting = "You approach the unknown " + pronoun
                    + " and try to start a conversation.";

            eb.framework1.ui.WordWrapper.WidthMeasurer measurer =
                    new eb.framework1.ui.WordWrapper.WidthMeasurer() {
                @Override public float measure(String text) {
                    glyph.setText(smallFont, text);
                    return glyph.width;
                }
            };
            textLines = eb.framework1.ui.WordWrapper.wrap(greeting, maxTextW, measurer);
        }

        // ── Layout ────────────────────────────────────────────────────────────
        String title = "Conversation";
        glyph.setText(font, title);
        float titleW = glyph.width;

        float bodyH = textLines.size() * smallLineH + 10f;

        glyph.setText(font, "Close");
        float closeTxtW = glyph.width;
        closeBtnW = closeTxtW + BTN_PAD_H * 2;
        closeBtnH = fontCapH + BTN_PAD_V * 2;

        float popW = Math.max(titleW + PAD * 2, maxTextW + PAD * 2);
        popW = Math.max(popW, closeBtnW + PAD * 2);
        float popH = PAD + fontLineH + bodyH + 10f + closeBtnH + PAD;

        float popX = (screenW - popW) / 2f;
        float popY = (screenH - popH) / 2f;

        // ── Dim overlay ───────────────────────────────────────────────────────
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0f, 0f, 0f, 0.55f);
        sr.rect(0, 0, screenW, screenH);
        sr.end();

        // ── Background + border ───────────────────────────────────────────────
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(popX, popY, popW, popH);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(popX, popY, popW, popH);
        sr.end();

        // ── Title ─────────────────────────────────────────────────────────────
        batch.begin();
        font.setColor(TITLE_COLOR);
        float titleX = popX + (popW - titleW) / 2f;
        float titleY = popY + popH - PAD;
        font.draw(batch, title, titleX, titleY);

        // ── Body text ─────────────────────────────────────────────────────────
        smallFont.setColor(Color.WHITE);
        float bodyY = titleY - fontLineH - 4f;
        for (String line : textLines) {
            smallFont.draw(batch, line, popX + PAD, bodyY);
            bodyY -= smallLineH;
        }
        batch.end();

        // ── Close button ──────────────────────────────────────────────────────
        closeBtnX = popX + (popW - closeBtnW) / 2f;
        closeBtnY = popY + PAD;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(CLOSE_BTN_COLOR);
        sr.rect(closeBtnX, closeBtnY, closeBtnW, closeBtnH);
        sr.end();

        batch.begin();
        font.setColor(Color.WHITE);
        glyph.setText(font, "Close");
        font.draw(batch, "Close",
                closeBtnX + (closeBtnW - glyph.width) / 2f,
                closeBtnY + closeBtnH - BTN_PAD_V);
        batch.end();
    }
}

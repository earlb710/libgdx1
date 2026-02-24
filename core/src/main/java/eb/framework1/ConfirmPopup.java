package eb.framework1;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;

/**
 * Simple Yes/No confirmation popup.
 *
 * <p>Call {@link #show(String, String, int)} to display it with a pending action
 * index. {@link #onTap(int, int)} returns {@link #RESULT_YES}, {@link #RESULT_NO},
 * or {@link #RESULT_NONE} if no button was hit.</p>
 */
class ConfirmPopup {

    static final int RESULT_NONE = -1;
    static final int RESULT_NO   =  0;
    static final int RESULT_YES  =  1;

    private static final Color BG_COLOR      = new Color(0.08f, 0.08f, 0.18f, 1f);
    private static final Color BORDER_COLOR  = new Color(0.80f, 0.60f, 0.20f, 1f);
    private static final Color TITLE_COLOR   = new Color(1.00f, 0.80f, 0.20f, 1f);
    private static final Color YES_BTN_COLOR = new Color(0.60f, 0.10f, 0.10f, 1f);
    private static final Color NO_BTN_COLOR  = new Color(0.10f, 0.35f, 0.10f, 1f);

    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyph;

    private boolean visible    = false;
    private String  title      = "";
    private String  message    = "";
    private int     pendingIdx = -1;

    // Button bounds (written during draw, read by onTap)
    private float yesX, yesY, yesW, yesH;
    private float noX,  noY,  noW,  noH;

    ConfirmPopup(SpriteBatch batch, ShapeRenderer sr,
                 BitmapFont font, BitmapFont smallFont, GlyphLayout glyph) {
        this.batch     = batch;
        this.sr        = sr;
        this.font      = font;
        this.smallFont = smallFont;
        this.glyph     = glyph;
    }

    boolean isVisible()    { return visible; }
    int     getPendingIdx(){ return pendingIdx; }

    /** Shows the popup with a title, single message line, and pending action index. */
    void show(String title, String message, int pendingIdx) {
        this.title      = title   != null ? title   : "";
        this.message    = message != null ? message : "";
        this.pendingIdx = pendingIdx;
        this.visible    = true;
        this.yesW       = 0f;
    }

    /**
     * Returns {@link #RESULT_YES}, {@link #RESULT_NO}, or {@link #RESULT_NONE}.
     * Dismisses the popup on Yes or No.
     */
    int onTap(int screenX, int flippedY) {
        if (!visible || yesW <= 0) return RESULT_NONE;
        if (screenX >= yesX && screenX <= yesX + yesW
                && flippedY >= yesY && flippedY <= yesY + yesH) {
            visible = false;
            return RESULT_YES;
        }
        if (noW > 0 && screenX >= noX && screenX <= noX + noW
                && flippedY >= noY && flippedY <= noY + noH) {
            visible = false;
            return RESULT_NO;
        }
        return RESULT_NONE;
    }

    void draw(int screenW, int screenH) {
        if (!visible) return;

        final float PAD         = 24f;
        final float GAP         = 10f;
        final float BTN_SPACING = 20f;
        final float BTN_GAP     = 18f; // extra space between message block and buttons

        glyph.setText(font, "Hg");
        float titleH     = glyph.height;
        float titleLineH = titleH + GAP;

        TextMeasurer.TextBounds yesBounds = TextMeasurer.measure(font, glyph, "Yes", 24f, 10f);
        TextMeasurer.TextBounds noBounds  = TextMeasurer.measure(font, glyph, "No",  24f, 10f);
        float btnH  = yesBounds.height;
        float yesW_ = yesBounds.width;
        float noW_  = noBounds.width;

        // Dialog width: wide enough for title and both buttons, capped at 85% screen width
        glyph.setText(font, title);
        float minW = Math.max(glyph.width, yesW_ + BTN_SPACING + noW_);
        float dialogW = Math.min(screenW * 0.85f, minW + 2 * PAD);
        float contentW = dialogW - 2 * PAD;

        // Word-wrap message to content width; measure real wrapped height
        glyph.setText(smallFont, message, Color.WHITE, contentW, Align.center, true);
        float msgH = glyph.height;

        float dialogH = PAD + titleLineH + msgH + BTN_GAP + btnH + PAD;

        float dialogX = (screenW - dialogW) / 2f;
        float dialogY = (screenH - dialogH) / 2f;

        float totalBtnW = yesW_ + BTN_SPACING + noW_;
        float btnStartX = dialogX + (dialogW - totalBtnW) / 2f;

        yesX = btnStartX;                        yesY = dialogY + PAD; yesW = yesW_; yesH = btnH;
        noX  = btnStartX + yesW_ + BTN_SPACING;  noY  = dialogY + PAD; noW  = noW_;  noH  = btnH;

        // --- Shapes ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(dialogX, dialogY, dialogW, dialogH);
        sr.setColor(YES_BTN_COLOR);
        sr.rect(yesX, yesY, yesW, yesH);
        sr.setColor(NO_BTN_COLOR);
        sr.rect(noX, noY, noW, noH);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(dialogX,     dialogY,     dialogW,     dialogH);
        sr.rect(dialogX + 1, dialogY + 1, dialogW - 2, dialogH - 2);
        sr.rect(yesX,     yesY,     yesW,     yesH);
        sr.rect(yesX + 1, yesY + 1, yesW - 2, yesH - 2);
        sr.rect(noX,     noY,     noW,     noH);
        sr.rect(noX + 1, noY + 1, noW - 2, noH - 2);
        sr.end();

        // --- Text ---
        batch.begin();
        float ty = dialogY + dialogH - PAD - titleH;

        font.setColor(TITLE_COLOR);
        glyph.setText(font, title);
        font.draw(batch, title, dialogX + (dialogW - glyph.width) / 2f, ty);
        ty -= titleLineH;

        // Draw the pre-wrapped message (GlyphLayout already set for contentW + center align)
        glyph.setText(smallFont, message, Color.WHITE, contentW, Align.center, true);
        smallFont.setColor(Color.WHITE);
        smallFont.draw(batch, glyph, dialogX + PAD, ty);

        font.setColor(Color.WHITE);
        glyph.setText(font, "Yes");
        font.draw(batch, "Yes",
                yesX + (yesW - glyph.width) / 2f,
                yesY + (yesH + glyph.height) / 2f);

        glyph.setText(font, "No");
        font.draw(batch, "No",
                noX + (noW - glyph.width) / 2f,
                noY + (noH + glyph.height) / 2f);
        batch.end();
    }
}

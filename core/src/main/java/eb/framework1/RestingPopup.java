package eb.framework1;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Animated "Resting…" popup.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li><b>ANIMATING</b> — displays "Resting" followed by 1–5 dots, one new
 *       dot every {@value #DOT_INTERVAL_SEC}s.  When the 5th dot is shown the
 *       popup automatically advances to the next phase.</li>
 *   <li><b>RESULT</b> (optional) — shows a single result message ("It became
 *       night" or "It became morning") and an OK button.  Skipped when there
 *       is no message to show.</li>
 *   <li><b>IDLE</b> — invisible; no-op.</li>
 * </ol>
 *
 * <h3>Integration with MainScreen</h3>
 * <pre>
 *   // Show when the player taps Rest:
 *   restingPopup.start("It became night");  // or null for no result message
 *
 *   // Each frame:
 *   restingPopup.update(delta);
 *   restingPopup.draw(screenW, screenH);
 *
 *   // On tap while visible:
 *   restingPopup.onTap(screenX, flippedY);
 * </pre>
 */
class RestingPopup {

    private static final float DOT_INTERVAL_SEC = 0.25f;
    private static final int   MAX_DOTS         = 5;

    private enum State { IDLE, ANIMATING, RESULT }

    // --- Colors ---
    private static final Color BG_COLOR     = new Color(0.08f, 0.10f, 0.20f, 1f);
    private static final Color BORDER_COLOR = new Color(0.40f, 0.55f, 0.85f, 1f);
    private static final Color TITLE_COLOR  = new Color(0.70f, 0.85f, 1.00f, 1f);
    private static final Color MSG_COLOR    = new Color(1.00f, 0.90f, 0.55f, 1f);
    private static final Color BTN_COLOR    = new Color(0.10f, 0.50f, 0.15f, 1f);

    // --- Rendering resources ---
    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final GlyphLayout   glyph;

    // --- State ---
    private State  state   = State.IDLE;
    private float  timer   = 0f;
    private int    dots    = 0;
    private int    maxDots = MAX_DOTS;
    private String label   = "Resting";
    private String resultMsg = null;   // null → skip RESULT phase

    // OK button bounds (RESULT phase only)
    private float okX, okY, okW, okH;

    // -------------------------------------------------------------------------

    RestingPopup(SpriteBatch batch, ShapeRenderer sr, BitmapFont font, GlyphLayout glyph) {
        this.batch = batch;
        this.sr    = sr;
        this.font  = font;
        this.glyph = glyph;
    }

    // -------------------------------------------------------------------------
    // State queries
    // -------------------------------------------------------------------------

    boolean isVisible()   { return state != State.IDLE; }
    boolean isAnimating() { return state == State.ANIMATING; }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    /**
     * Starts the resting animation (Rest button — 5 dots, label "Resting").
     *
     * @param resultMessage Optional message shown after the animation, or {@code null}.
     */
    void start(String resultMessage) {
        start(resultMessage, MAX_DOTS, "Resting");
    }

    /**
     * Starts the animation with a custom dot count and label (used for Sleep).
     *
     * @param resultMessage Optional message shown after the animation, or {@code null}.
     * @param numDots       Number of dots to display (e.g. hoursSlept × 2 for sleep).
     * @param animLabel     Text shown during the animation (e.g. "Sleeping").
     */
    void start(String resultMessage, int numDots, String animLabel) {
        this.resultMsg = resultMessage;
        this.maxDots   = Math.max(1, numDots);
        this.label     = animLabel != null ? animLabel : "Resting";
        this.timer     = 0f;
        this.dots      = 0;
        this.okW       = 0f;
        this.state     = State.ANIMATING;
    }

    /**
     * Advances the animation timer.  Call once per frame from
     * {@code MainScreen.render()}.
     */
    void update(float delta) {
        if (state != State.ANIMATING) return;
        timer += delta;
        dots = Math.min(maxDots, (int) (timer / DOT_INTERVAL_SEC) + 1);
        if (dots >= maxDots && timer >= maxDots * DOT_INTERVAL_SEC) {
            // Animation complete — advance to result or close
            if (resultMsg != null && !resultMsg.isEmpty()) {
                state = State.RESULT;
                okW   = 0f;
            } else {
                state = State.IDLE;
            }
        }
    }

    /**
     * Handles a tap; dismisses the RESULT phase when OK is tapped.
     */
    void onTap(int screenX, int flippedY) {
        if (state == State.RESULT && okW > 0
                && screenX >= okX && screenX <= okX + okW
                && flippedY >= okY && flippedY <= okY + okH) {
            state = State.IDLE;
        }
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    void draw(int screenW, int screenH) {
        if (state == State.IDLE) return;

        final float PAD = 32f;
        final float GAP = 12f;

        glyph.setText(font, "Hg");
        float fontH     = glyph.height;
        float fontLineH = fontH + GAP;

        // 24f = horizontal padding, 10f = vertical padding inside the OK button
        TextMeasurer.TextBounds okBounds = TextMeasurer.measure(font, glyph, "OK", 24f, 10f);
        float okBtnW = okBounds.width;
        float okBtnH = okBounds.height;

        String mainText;
        if (state == State.ANIMATING) {
            StringBuilder sb = new StringBuilder(label);
            for (int i = 0; i < dots; i++) sb.append('.');
            mainText = sb.toString();
        } else {
            mainText = resultMsg;
        }

        glyph.setText(font, mainText);
        float textW = glyph.width;
        // Also measure the widest possible animated text to prevent dialog resizing mid-animation
        if (state == State.ANIMATING) {
            StringBuilder maxSb = new StringBuilder(label);
            for (int i = 0; i < maxDots; i++) maxSb.append('.');
            glyph.setText(font, maxSb.toString());
            textW = Math.max(textW, glyph.width);
        }
        float contentW = Math.max(textW, okBounds.textWidth);
        float dialogW  = Math.min(screenW * 0.75f, contentW + 2 * PAD);
        float dialogH;
        if (state == State.RESULT) {
            dialogH = PAD + fontLineH + PAD + okBtnH + PAD;
        } else {
            dialogH = PAD + fontH + PAD;
        }

        float dialogX = (screenW - dialogW) / 2f;
        float dialogY = (screenH - dialogH) / 2f;

        // --- Shapes ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(dialogX, dialogY, dialogW, dialogH);
        if (state == State.RESULT) {
            okX = dialogX + (dialogW - okBtnW) / 2f;
            okY = dialogY + PAD;
            okW = okBtnW;
            okH = okBtnH;
            sr.setColor(BTN_COLOR);
            sr.rect(okX, okY, okW, okH);
        } else {
            okW = 0f;
        }
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(dialogX,     dialogY,     dialogW,     dialogH);
        sr.rect(dialogX + 1, dialogY + 1, dialogW - 2, dialogH - 2);
        if (state == State.RESULT) {
            sr.rect(okX,     okY,     okW,     okH);
            sr.rect(okX + 1, okY + 1, okW - 2, okH - 2);
        }
        sr.end();

        // --- Text ---
        batch.begin();
        float ty = dialogY + dialogH - PAD;
        font.setColor(state == State.ANIMATING ? TITLE_COLOR : MSG_COLOR);
        glyph.setText(font, mainText);
        font.draw(batch, mainText, dialogX + (dialogW - glyph.width) / 2f, ty);

        if (state == State.RESULT) {
            font.setColor(Color.WHITE);
            glyph.setText(font, "OK");
            font.draw(batch, "OK",
                    okX + (okW - glyph.width) / 2f,
                    okY + (okH + glyph.height) / 2f);
        }
        batch.end();
    }
}

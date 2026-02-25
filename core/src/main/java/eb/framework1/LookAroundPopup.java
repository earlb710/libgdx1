package eb.framework1;

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
import java.util.Random;

/**
 * Self-contained "Look around" feature.
 * Manages the state machine (IDLE → ANIMATING → RESULTS → IDLE) and draws the
 * modal popup overlay. MainScreen routes input to this class and reads
 * {@link #isVisible()} / {@link #isAnimating()} to gate other interactions.
 */
class LookAroundPopup {

    private enum State { IDLE, ANIMATING, RESULTS }

    // Timing
    private static final float DOT_INTERVAL = 0.5f;
    private static final int   MAX_DOTS     = 3;

    // Colours
    private static final Color BG_COLOR       = new Color(0.1f, 0.1f, 0.18f, 1f);
    private static final Color BORDER_COLOR   = new Color(0.5f, 0.6f, 0.8f,  1f);
    private static final Color BTN_COLOR      = new Color(0.1f, 0.5f, 0.15f, 1f);

    // Rendering resources
    private final SpriteBatch   batch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyphLayout;
    private final CityMap       cityMap;
    private final Profile       profile;
    private final NovelTextEngine novelTextEngine;

    // State
    private State        state = State.IDLE;
    private float        timer = 0f;
    private int          lookCharX, lookCharY;
    private List<String> foundItems = new ArrayList<>();
    private String       novelDesc  = null;

    // OK button bounds (written during draw, read by onTap)
    private float okX, okY, okW, okH;

    // Scroll state for results list
    private float scrollY     = 0f;
    private float maxScrollY  = 0f;
    private static final float SCROLLBAR_W = 8f;

    // RNG for probability-based discovery rolls
    private final Random rng = new Random();

    LookAroundPopup(SpriteBatch batch, ShapeRenderer shapeRenderer,
                    BitmapFont font, BitmapFont smallFont, GlyphLayout glyphLayout,
                    CityMap cityMap, Profile profile, NovelTextEngine novelTextEngine) {
        this.batch           = batch;
        this.shapeRenderer   = shapeRenderer;
        this.font            = font;
        this.smallFont       = smallFont;
        this.glyphLayout     = glyphLayout;
        this.cityMap         = cityMap;
        this.profile         = profile;
        this.novelTextEngine = novelTextEngine;
    }

    // -------------------------------------------------------------------------
    // State queries
    // -------------------------------------------------------------------------

    boolean isVisible()   { return state != State.IDLE; }
    boolean isAnimating() { return state == State.ANIMATING; }
    boolean isResults()   { return state == State.RESULTS; }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    /** Start the look-around animation for the given character cell. */
    void start(int charX, int charY) {
        lookCharX = charX;
        lookCharY = charY;
        foundItems.clear();
        novelDesc = null;
        timer   = 0f;
        scrollY = 0f;
        state   = State.ANIMATING;
        Gdx.app.log("LookAroundPopup", "Started at " + charX + "," + charY);
    }

    /** Advance animation timer; call once per frame while visible. */
    void update(float delta) {
        if (state == State.ANIMATING) {
            timer += delta;
            if (timer >= DOT_INTERVAL * MAX_DOTS) {
                runDiscovery();
            }
        }
    }

    /**
     * Handle a tap while the popup is visible.
     * During RESULTS: dismisses the popup if the OK button is hit.
     */
    void onTap(int screenX, int flippedY) {
        if (state == State.RESULTS && okW > 0
                && screenX >= okX && screenX <= okX + okW
                && flippedY >= okY && flippedY <= okY + okH) {
            state = State.IDLE;
            scrollY = 0f;
            foundItems.clear();
            Gdx.app.log("LookAroundPopup", "Dismissed");
        }
    }

    /** Scroll the results list by delta pixels (positive = scroll down). */
    void scroll(float delta) {
        if (state == State.RESULTS) {
            scrollY = MathUtils.clamp(scrollY + delta, 0f, maxScrollY);
        }
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    void draw(int screenW, int screenH, int infoAreaHeight) {
        final float PAD    = 24f;
        final float GAP    = 14f;  // extra inter-line spacing added to each text height
        final float MIN_W  = 320f;
        final float MAX_W  = screenW * 0.9f;
        final float MAX_H  = screenH * 0.8f;

        // --- Measure OK button (fixed, always visible) ---
        TextMeasurer.TextBounds okBounds = TextMeasurer.measure(font, glyphLayout, "OK", 24f, 10f);
        float okBtnW = okBounds.width;
        float okBtnH = okBounds.height;

        // --- Measure all text elements via TextMeasurer ---
        // Pre-compute smallLineH so it's available in both sizing and scrollbar sections.
        glyphLayout.setText(smallFont, "Hg");
        float smallLineH = glyphLayout.height + GAP;

        // Heading line
        TextMeasurer.TextBounds headBounds;
        float headingH, headingLineH;
        float dialogW, dialogH;
        boolean needsScroll;
        List<String> novelLines = java.util.Collections.emptyList();

        if (state == State.ANIMATING) {
            headBounds   = TextMeasurer.measure(font, glyphLayout, "Looking around...", 0f, 0f);
            headingH     = headBounds.textHeight;
            headingLineH = headingH + GAP;
            // Width: heading + 2*PAD (no scrollbar needed for animation state)
            dialogW = MathUtils.clamp(headBounds.textWidth + 2 * PAD, MIN_W, MAX_W);
            // Height: exactly PAD top + text + PAD bottom so text is always PAD from each border
            dialogH      = 2 * PAD + headingH;
            needsScroll  = false;
            maxScrollY   = 0f;
        } else {
            // RESULTS
            headBounds   = TextMeasurer.measure(font, glyphLayout, "Found:", 0f, 0f);
            headingH     = headBounds.textHeight;
            headingLineH = headingH + GAP;
            // Measure items: total height and max width
            TextMeasurer.TextBounds itemsBounds =
                    TextMeasurer.measureLines(smallFont, glyphLayout, foundItems, GAP, 0f, 0f);
            // measureLines gives N*lineH + (N-1)*GAP but draw loop uses N*(lineH+GAP).
            // Add one trailing GAP so the last item is never clipped.
            float itemsH   = itemsBounds.textHeight + (foundItems.isEmpty() ? 0f : GAP);
            float maxItemW = itemsBounds.textWidth;

            // Width = widest of (heading, items, ok-button-text) + 2*PAD + scrollbar margin
            float rawContentW = Math.max(headBounds.textWidth,
                                Math.max(maxItemW, okBounds.textWidth));
            dialogW = MathUtils.clamp(rawContentW + 2 * PAD + SCROLLBAR_W + 4f, MIN_W, MAX_W);

            // Wrap novel description text to the available content width and add to height.
            if (novelDesc != null) {
                float wrapWidth = dialogW - 2 * PAD;
                novelLines = WordWrapper.wrap(novelDesc, wrapWidth, t -> {
                    glyphLayout.setText(smallFont, t);
                    return glyphLayout.width;
                });
                if (!novelLines.isEmpty()) {
                    itemsH += novelLines.size() * smallLineH;
                }
            }

            // Height formula:
            //   PAD (top border)
            //   + headingLineH  (heading text + gap)
            //   + headingH      (character-size space after heading)
            //   + itemsH        (all item lines + inter-line gaps + trailing gap)
            //   + PAD           (spacing between items and OK button)
            //   + okBtnH        (OK button)
            //   + PAD           (bottom border)
            // The heading+items section is scrollable; the OK button is fixed.
            float scrollableH   = headingLineH + headingH + itemsH;
            float fixedBottomH  = PAD + okBtnH;          // always visible
            float maxScrollableH = MAX_H - 2 * PAD - fixedBottomH;
            needsScroll  = scrollableH > maxScrollableH;
            float usedScrollH = needsScroll ? maxScrollableH : scrollableH;
            dialogH      = PAD + usedScrollH + fixedBottomH + PAD;
            maxScrollY   = needsScroll ? Math.max(0f, scrollableH - maxScrollableH) : 0f;
        }

        // For drawing the text we still need fontLineH
        glyphLayout.setText(font, "Hg");
        float fontH      = glyphLayout.height;
        float fontLineH  = fontH + GAP;

        // Centred on screen
        float dialogX = (screenW - dialogW) / 2f;
        float dialogY = (screenH - dialogH) / 2f;

        // --- Shapes ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BG_COLOR);
        shapeRenderer.rect(dialogX, dialogY, dialogW, dialogH);
        if (state == State.RESULTS) {
            float okAreaY = dialogY + PAD;
            okX = dialogX + (dialogW - okBtnW - SCROLLBAR_W) / 2f;
            okY = okAreaY;
            okW = okBtnW; okH = okBtnH;
            shapeRenderer.setColor(BTN_COLOR);
            shapeRenderer.rect(okX, okY, okW, okH);
        } else {
            okW = 0;
        }
        // Vertical scrollbar track + thumb
        if (needsScroll) {
            float sbX    = dialogX + dialogW - SCROLLBAR_W - 2f;
            float trackY = dialogY + PAD + okBtnH + PAD;
            float trackH = dialogH - 2 * PAD - okBtnH - PAD;
            shapeRenderer.setColor(0.3f, 0.3f, 0.35f, 1f);
            shapeRenderer.rect(sbX, trackY, SCROLLBAR_W, trackH);
            // scrollableH = headingLineH + headingH (char gap) + itemsH; visible area = trackH
            float scrollableH = headBounds.textHeight + GAP + headBounds.textHeight
                    + TextMeasurer.measureLines(smallFont, glyphLayout, foundItems, GAP, 0f, 0f).textHeight
                    + novelLines.size() * smallLineH;
            float thumbH  = MathUtils.clamp(trackH * (trackH / scrollableH), 12f, trackH);
            float thumbY  = trackY + (trackH - thumbH) * (maxScrollY > 0 ? 1f - scrollY / maxScrollY : 1f);
            shapeRenderer.setColor(0.6f, 0.65f, 0.75f, 1f);
            shapeRenderer.rect(sbX, thumbY, SCROLLBAR_W, thumbH);
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(BORDER_COLOR);
        shapeRenderer.rect(dialogX,     dialogY,     dialogW,     dialogH);
        shapeRenderer.rect(dialogX + 1, dialogY + 1, dialogW - 2, dialogH - 2);
        if (state == State.RESULTS) {
            shapeRenderer.rect(okX,     okY,     okW,     okH);
            shapeRenderer.rect(okX + 1, okY + 1, okW - 2, okH - 2);
        }
        shapeRenderer.end();

        // --- Scissor clip for text area ---
        float clipX = dialogX + 1;
        float clipY = state == State.RESULTS ? dialogY + PAD + okBtnH + PAD : dialogY + 1;
        float clipW = dialogW - SCROLLBAR_W - 4f - 2f;
        float clipH = dialogH - (clipY - dialogY) - PAD;
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor((int) clipX, (int) clipY, (int) clipW, (int) Math.max(0, clipH));

        // --- Text ---
        batch.begin();
        float ty = dialogY + dialogH - PAD + scrollY;

        if (state == State.ANIMATING) {
            int dots = Math.min(MAX_DOTS, (int)(timer / DOT_INTERVAL) + 1);
            StringBuilder sb = new StringBuilder("Looking around");
            for (int i = 0; i < dots; i++) sb.append('.');
            font.setColor(Color.WHITE);
            glyphLayout.setText(font, sb.toString());
            font.draw(batch, sb.toString(), dialogX + (dialogW - SCROLLBAR_W - glyphLayout.width) / 2f, ty);
        } else {
            // RESULTS — "Found:" header
            font.setColor(InfoPanelRenderer.LABEL_COLOR);
            glyphLayout.setText(font, "Found:");
            font.draw(batch, "Found:", dialogX + (dialogW - SCROLLBAR_W - glyphLayout.width) / 2f, ty);
            ty -= fontLineH + fontH;
            smallFont.setColor(Color.WHITE);
            for (String item : foundItems) {
                glyphLayout.setText(smallFont, item);
                smallFont.draw(batch, item,
                        dialogX + PAD,
                        ty);
                ty -= smallLineH;
            }
            if (!novelLines.isEmpty()) {
                smallFont.setColor(InfoPanelRenderer.NOVEL_COLOR);
                for (String nLine : novelLines) {
                    smallFont.draw(batch, nLine, dialogX + PAD, ty);
                    ty -= smallLineH;
                }
            }
        }
        batch.end();

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        // OK button text drawn outside scissor (always visible)
        if (state == State.RESULTS) {
            batch.begin();
            font.setColor(Color.WHITE);
            glyphLayout.setText(font, "OK");
            font.draw(batch, "OK",
                    okX + (okW - glyphLayout.width) / 2f,
                    okY + (okH + glyphLayout.height) / 2f);
            batch.end();
        }
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private void runDiscovery() {
        // Advance game time and deduct stamina for the look-around action
        profile.advanceGameTime(10);
        profile.useStamina(1);

        Cell cell = cityMap.getCell(lookCharX, lookCharY);
        if (cell.hasBuilding()) {
            // Attribute key matches how CharacterAttributeScreen saves it (enum .name())
            int perception = profile.getAttribute(CharacterAttribute.PERCEPTION.name());

            // Always test against the easiest (lowest hiddenValue > 0) undiscovered improvement
            Improvement easiest = null;
            for (Improvement imp : cell.getBuilding().getImprovements()) {
                if (imp.isDiscovered() || imp.getHiddenValue() == 0) continue;
                if (easiest == null || imp.getHiddenValue() < easiest.getHiddenValue()) {
                    easiest = imp;
                }
            }
            if (easiest != null) {
                // Sliding probability: chance = min(1.0, perception / hiddenValue)
                // e.g. perception=5, hiddenValue=5 → 100%; perception=3, hiddenValue=5 → 60%
                float chance = Math.min(1.0f, (float) perception / easiest.getHiddenValue());
                if (rng.nextFloat() < chance) {
                    easiest.discover();
                    String mod = InfoPanelRenderer.formatAttributeModifiers(easiest.getAttributeModifiers());
                    String entry = easiest.getName() + " (Lvl " + easiest.getLevel() + ")"
                            + (mod.isEmpty() ? "" : " " + mod);
                    foundItems.add(entry);
                    if (novelTextEngine != null) {
                        String desc = novelTextEngine.getImprovementDescription(
                                easiest.getName(), profile.getGender());
                        if (desc != null && !desc.isEmpty()) {
                            novelDesc = desc;
                        }
                    }
                }
            }
        }
        if (foundItems.isEmpty()) foundItems.add("Nothing new found.");
        state = State.RESULTS;
        Gdx.app.log("LookAroundPopup", "Discovery done: " + foundItems.size() + " item(s)");
    }
}

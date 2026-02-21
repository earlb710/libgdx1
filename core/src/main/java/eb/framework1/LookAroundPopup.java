package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

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

    // State
    private State        state = State.IDLE;
    private float        timer = 0f;
    private int          lookCharX, lookCharY;
    private List<String> foundItems = new ArrayList<>();

    // OK button bounds (written during draw, read by onTap)
    private float okX, okY, okW, okH;

    // RNG for probability-based discovery rolls
    private final Random rng = new Random();

    LookAroundPopup(SpriteBatch batch, ShapeRenderer shapeRenderer,
                    BitmapFont font, BitmapFont smallFont, GlyphLayout glyphLayout,
                    CityMap cityMap, Profile profile) {
        this.batch         = batch;
        this.shapeRenderer = shapeRenderer;
        this.font          = font;
        this.smallFont     = smallFont;
        this.glyphLayout   = glyphLayout;
        this.cityMap       = cityMap;
        this.profile       = profile;
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
        timer = 0f;
        state = State.ANIMATING;
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
            foundItems.clear();
            Gdx.app.log("LookAroundPopup", "Dismissed");
        }
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    void draw(int screenW, int screenH, int infoAreaHeight) {
        glyphLayout.setText(font, "Hg");
        float fontH   = glyphLayout.height;
        float fontLineH  = fontH * 1.5f;
        glyphLayout.setText(smallFont, "Hg");
        float smallH  = glyphLayout.height;
        float smallLineH = smallH * 1.5f;

        final float PAD   = 24f;

        TextMeasurer.TextBounds okBounds = TextMeasurer.measure(font, glyphLayout, "OK", 24f, 10f);
        float okBtnW = okBounds.width;
        float okBtnH = okBounds.height;

        int contentLines = (state == State.RESULTS)
                ? 1 + foundItems.size()
                : 1;
        float dialogH = PAD
                + fontLineH
                + contentLines * smallLineH
                + (state == State.RESULTS ? PAD + okBtnH : 0)
                + PAD;

        // Full-width, anchored to the bottom (info panel area) – does not cover the map
        float dialogW = screenW;
        float dialogX = 0;
        float dialogY = 0;

        // --- Shapes ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BG_COLOR);
        shapeRenderer.rect(dialogX, dialogY, dialogW, dialogH);
        if (state == State.RESULTS) {
            okX = dialogX + (dialogW - okBtnW) / 2f;
            okY = dialogY + PAD;
            okW = okBtnW; okH = okBtnH;
            shapeRenderer.setColor(BTN_COLOR);
            shapeRenderer.rect(okX, okY, okW, okH);
        } else {
            okW = 0;
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

        // --- Text ---
        batch.begin();
        float ty = dialogY + dialogH - PAD - fontH;

        if (state == State.ANIMATING) {
            int dots = Math.min(MAX_DOTS, (int)(timer / DOT_INTERVAL) + 1);
            StringBuilder sb = new StringBuilder("Looking around");
            for (int i = 0; i < dots; i++) sb.append('.');
            font.setColor(Color.WHITE);
            glyphLayout.setText(font, sb.toString());
            font.draw(batch, sb.toString(), dialogX + (dialogW - glyphLayout.width) / 2f, ty);
        } else {
            // RESULTS
            font.setColor(InfoPanelRenderer.LABEL_COLOR);
            glyphLayout.setText(font, "Found:");
            font.draw(batch, "Found:", dialogX + (dialogW - glyphLayout.width) / 2f, ty);
            ty -= smallLineH;
            smallFont.setColor(Color.WHITE);
            for (String item : foundItems) {
                glyphLayout.setText(smallFont, item);
                smallFont.draw(batch, item, dialogX + (dialogW - glyphLayout.width) / 2f, ty);
                ty -= smallLineH;
            }
            font.setColor(Color.WHITE);
            glyphLayout.setText(font, "OK");
            font.draw(batch, "OK",
                    okX + (okW - glyphLayout.width) / 2f,
                    okY + (okH + glyphLayout.height) / 2f);
        }
        batch.end();
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
            for (Improvement imp : cell.getBuilding().getImprovements()) {
                if (imp.isDiscovered()) continue;
                int hv = imp.getHiddenValue();
                if (hv == 0) {
                    // hiddenValue 0 is auto-discovered on arrival; skip here
                    continue;
                }
                // Sliding probability: chance = min(1.0, perception / hiddenValue)
                // e.g. perception=5, hiddenValue=5 → 100%; perception=3, hiddenValue=5 → 60%
                float chance = Math.min(1.0f, (float) perception / hv);
                if (rng.nextFloat() < chance) {
                    imp.discover();
                    String mod = InfoPanelRenderer.formatAttributeModifiers(imp.getAttributeModifiers());
                    String entry = imp.getName() + " (Lvl " + imp.getLevel() + ")"
                            + (mod.isEmpty() ? "" : " " + mod);
                    foundItems.add(entry);
                }
            }
        }
        if (foundItems.isEmpty()) foundItems.add("Nothing new found.");
        state = State.RESULTS;
        Gdx.app.log("LookAroundPopup", "Discovery done: " + foundItems.size() + " item(s)");
    }
}

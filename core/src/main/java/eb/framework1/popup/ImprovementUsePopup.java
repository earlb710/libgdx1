package eb.framework1.popup;

import eb.framework1.character.*;
import eb.framework1.city.*;
import eb.framework1.ui.*;


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
 * Modal popup that lists every discovered improvement at the player's current
 * building that has a {@code function} (e.g. "rest", "exercise", "study").
 *
 * <p>Each row shows the improvement name, its function, and a <em>Use</em>
 * button.  If the improvement has access restrictions ({@code restrict} map)
 * that the player does not meet, the button is drawn in a disabled colour and
 * a plain-English reason is shown next to it instead of being clickable.
 *
 * <p>Possible restriction keys:
 * <ul>
 *   <li>{@code gender} – {@code "male"} or {@code "female"}</li>
 *   <li>{@code strength} – minimum STRENGTH attribute value (integer)</li>
 * </ul>
 *
 * <p>Example failure messages:
 * <ul>
 *   <li><em>you are not male and not strong enough</em></li>
 *   <li><em>you are not strong enough</em></li>
 *   <li><em>you are not female</em></li>
 * </ul>
 */
public class ImprovementUsePopup {

    // ── Colours ──────────────────────────────────────────────────────────────
    private static final Color BG_COLOR        = new Color(0.08f, 0.10f, 0.18f, 1f);
    private static final Color BORDER_COLOR    = new Color(0.45f, 0.60f, 0.90f, 1f);
    private static final Color TITLE_COLOR     = new Color(1.00f, 0.85f, 0.30f, 1f);
    private static final Color BTN_USE_COLOR   = new Color(0.10f, 0.50f, 0.15f, 1f);
    private static final Color BTN_DISABLED    = new Color(0.35f, 0.35f, 0.35f, 1f);
    private static final Color BTN_CLOSE_COLOR = new Color(0.50f, 0.10f, 0.10f, 1f);
    private static final Color FUNC_COLOR      = new Color(0.55f, 0.85f, 1.00f, 1f);
    private static final Color DENY_COLOR      = new Color(1.00f, 0.50f, 0.30f, 1f);

    private static final float PAD         = 18f;
    private static final float ROW_GAP     = 8f;
    private static final float SCROLLBAR_W = 8f;

    // ── Rendering resources ───────────────────────────────────────────────────
    private final SpriteBatch   batch;
    private final ShapeRenderer sr;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyph;
    private final Profile       profile;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean visible = false;
    private String  buildingName = "";

    /** One entry per discoverable functional improvement shown in the popup. */
    private final List<Row> rows = new ArrayList<>();

    /** Index of the row whose Use button was just tapped; -1 = none; -2 = close. */
    private int tappedRow = -2;

    // Scroll state
    private float scrollY    = 0f;
    private float maxScrollY = 0f;

    // Close-button bounds
    private float closeBtnX, closeBtnY, closeBtnW, closeBtnH;

    // ── Inner types ───────────────────────────────────────────────────────────

    private static final class Row {
        final Improvement imp;
        final boolean     allowed;
        final String      denyReason;   // non-null when !allowed

        // Use-button bounds (written during draw, read by onTap)
        float btnX, btnY, btnW, btnH;

        Row(Improvement imp, boolean allowed, String denyReason) {
            this.imp        = imp;
            this.allowed    = allowed;
            this.denyReason = denyReason;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    public ImprovementUsePopup(SpriteBatch batch, ShapeRenderer sr,
                               BitmapFont font, BitmapFont smallFont,
                               GlyphLayout glyph, Profile profile) {
        this.batch     = batch;
        this.sr        = sr;
        this.font      = font;
        this.smallFont = smallFont;
        this.glyph     = glyph;
        this.profile   = profile;
    }

    // ── State queries ─────────────────────────────────────────────────────────

    public boolean isVisible() { return visible; }

    /**
     * Returns the index of the row that was used (≥ 0), {@code -1} if the
     * close button was tapped, or {@code -2} if nothing was tapped since last
     * call.  Resets to {@code -2} after each call.
     */
    public int pollTapped() {
        int r = tappedRow;
        tappedRow = -2;
        return r;
    }

    /**
     * Returns the {@link Improvement} for a given row index, or {@code null}.
     */
    public Improvement getRowImprovement(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) return null;
        return rows.get(rowIndex).imp;
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    /**
     * Opens the popup for the given building, listing every discovered
     * improvement that has a {@code function}.
     *
     * @param building     the building whose improvements to show
     * @param buildingName display name of the building
     */
    public void show(Building building, String buildingName) {
        this.buildingName = buildingName != null ? buildingName : "";
        rows.clear();
        scrollY    = 0f;
        maxScrollY = 0f;
        tappedRow  = -2;

        if (building == null) { visible = false; return; }

        for (Improvement imp : building.getImprovements()) {
            if (!imp.isDiscovered() || !imp.hasFunction()) continue;
            String deny   = buildDenyReason(imp);
            boolean ok    = (deny == null);
            rows.add(new Row(imp, ok, deny));
        }

        visible = !rows.isEmpty();
        Gdx.app.log("ImprovementUsePopup", "show: " + rows.size() + " rows for " + buildingName);
    }

    /** Dismiss the popup without using anything. */
    public void dismiss() {
        visible = false;
        tappedRow = -1;
    }

    /** Handle a tap.  Call every frame if the popup is visible. */
    public void onTap(int screenX, int flippedY) {
        if (!visible) return;

        // Close button
        if (closeBtnW > 0
                && screenX >= closeBtnX && screenX <= closeBtnX + closeBtnW
                && flippedY >= closeBtnY && flippedY <= closeBtnY + closeBtnH) {
            visible   = false;
            tappedRow = -1;
            return;
        }

        // Use buttons
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (row.allowed && row.btnW > 0
                    && screenX >= row.btnX && screenX <= row.btnX + row.btnW
                    && flippedY >= row.btnY && flippedY <= row.btnY + row.btnH) {
                tappedRow = i;
                visible   = false;
                return;
            }
        }
    }

    /** Scroll the list (positive = down). */
    public void scroll(float delta) {
        if (visible) scrollY = MathUtils.clamp(scrollY + delta, 0f, maxScrollY);
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    public void draw(int screenW, int screenH) {
        if (!visible) return;

        final float MAX_W = screenW * 0.92f;
        final float MAX_H = screenH * 0.80f;
        final float MIN_W = 320f;

        // ── Measure text metrics ──────────────────────────────────────────────
        glyph.setText(font, "Hg");
        final float fontH     = glyph.height;
        final float fontLineH = fontH + ROW_GAP;

        glyph.setText(smallFont, "Hg");
        final float smallH     = glyph.height;
        final float smallLineH = smallH + ROW_GAP;

        final String useLabel  = "Use";
        final String closeLabel= "Close";

        TextMeasurer.TextBounds useBounds   = TextMeasurer.measure(font, glyph, useLabel,   14f, 8f);
        TextMeasurer.TextBounds closeBounds = TextMeasurer.measure(font, glyph, closeLabel, 18f, 8f);
        final float useW  = useBounds.width;
        final float useH  = useBounds.height;

        // ── Estimate dialog width ─────────────────────────────────────────────
        glyph.setText(font, buildingName.isEmpty() ? "Use Improvements" : buildingName);
        float maxContentW = glyph.width;
        for (Row row : rows) {
            String label = row.imp.getName() + " [" + row.imp.getFunction() + "]";
            glyph.setText(smallFont, label);
            float needed = glyph.width + PAD + useW + PAD
                    + (row.denyReason != null ? measureSmall(row.denyReason) + PAD : 0f);
            if (needed > maxContentW) maxContentW = needed;
        }
        float dialogW = MathUtils.clamp(maxContentW + 2 * PAD + SCROLLBAR_W + 4f, MIN_W, MAX_W);

        // ── Height ───────────────────────────────────────────────────────────
        // Fixed: title + separator + close button
        float fixedH = PAD + fontLineH + PAD / 2f + PAD + closeBounds.height + PAD;
        // Scrollable: rows
        float rowH = smallLineH + ROW_GAP;
        float scrollable = rows.size() * rowH;
        float maxScrollH = MAX_H - fixedH;
        boolean needsScroll = scrollable > maxScrollH;
        float usedScrollH  = needsScroll ? maxScrollH : scrollable;
        float dialogH      = fixedH + usedScrollH;
        maxScrollY = needsScroll ? Math.max(0f, scrollable - maxScrollH) : 0f;

        float dialogX = (screenW - dialogW) / 2f;
        float dialogY = (screenH - dialogH) / 2f;

        // ── Draw background ───────────────────────────────────────────────────
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(dialogX, dialogY, dialogW, dialogH);

        // Close button (bottom centre)
        closeBtnW = closeBounds.width;
        closeBtnH = closeBounds.height;
        closeBtnX = dialogX + (dialogW - closeBtnW) / 2f;
        closeBtnY = dialogY + PAD;
        sr.setColor(BTN_CLOSE_COLOR);
        sr.rect(closeBtnX, closeBtnY, closeBtnW, closeBtnH);

        // Scrollbar
        if (needsScroll) {
            float sbX    = dialogX + dialogW - SCROLLBAR_W - 2f;
            float trackY = dialogY + PAD + closeBtnH + PAD;
            float trackH = dialogH - 2 * PAD - closeBtnH - PAD - fontLineH - PAD / 2f;
            sr.setColor(0.3f, 0.3f, 0.35f, 1f);
            sr.rect(sbX, trackY, SCROLLBAR_W, trackH);
            float thumbH = MathUtils.clamp(trackH * (trackH / scrollable), 12f, trackH);
            float thumbY = trackY + (trackH - thumbH)
                    * (maxScrollY > 0 ? 1f - scrollY / maxScrollY : 1f);
            sr.setColor(0.6f, 0.65f, 0.75f, 1f);
            sr.rect(sbX, thumbY, SCROLLBAR_W, thumbH);
        }

        // Row backgrounds (alternating)
        float rowAreaTop = dialogY + dialogH - PAD - fontLineH - PAD / 2f;
        float rowAreaBot = dialogY + PAD + closeBtnH + PAD;
        float clippedScrollH = rowAreaTop - rowAreaBot;
        for (int i = 0; i < rows.size(); i++) {
            float rowTop = rowAreaTop - i * rowH - scrollY;
            float rowBot = rowTop - smallLineH;
            if (rowBot > rowAreaTop || rowTop < rowAreaBot) continue;
            if (i % 2 == 1) {
                sr.setColor(0.10f, 0.13f, 0.22f, 1f);
                sr.rect(dialogX + 1, rowBot, dialogW - SCROLLBAR_W - 4f - 2f, smallLineH);
            }
            // Use button
            Row row = rows.get(i);
            row.btnX = dialogX + dialogW - SCROLLBAR_W - 4f - useW - PAD;
            row.btnY = rowBot + (smallLineH - useH) / 2f;
            row.btnW = useW;
            row.btnH = useH;
            sr.setColor(row.allowed ? BTN_USE_COLOR : BTN_DISABLED);
            sr.rect(row.btnX, row.btnY, row.btnW, row.btnH);
        }
        sr.end();

        // ── Borders ───────────────────────────────────────────────────────────
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(dialogX,     dialogY,     dialogW,     dialogH);
        sr.rect(dialogX + 1, dialogY + 1, dialogW - 2, dialogH - 2);
        sr.rect(closeBtnX,     closeBtnY,     closeBtnW,     closeBtnH);
        sr.rect(closeBtnX + 1, closeBtnY + 1, closeBtnW - 2, closeBtnH - 2);
        for (Row row : rows) {
            if (row.btnW > 0) {
                sr.rect(row.btnX,     row.btnY,     row.btnW,     row.btnH);
                sr.rect(row.btnX + 1, row.btnY + 1, row.btnW - 2, row.btnH - 2);
            }
        }
        sr.end();

        // ── Scissor for row area ───────────────────────────────────────────────
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor((int) (dialogX + 1), (int) rowAreaBot,
                (int) (dialogW - SCROLLBAR_W - 4f - 2f),
                (int) Math.max(0, clippedScrollH));

        batch.begin();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            float rowTop = rowAreaTop - i * rowH - scrollY;
            float textY  = rowTop - (smallLineH - smallH) / 2f;

            if (textY < rowAreaBot - smallLineH || textY > rowAreaTop + smallLineH) continue;

            // Name [function]
            String nameLabel = row.imp.getName();
            smallFont.setColor(Color.WHITE);
            smallFont.draw(batch, nameLabel, dialogX + PAD, textY);

            glyph.setText(smallFont, nameLabel);
            float afterName = dialogX + PAD + glyph.width + PAD / 2f;
            smallFont.setColor(FUNC_COLOR);
            smallFont.draw(batch, "[" + row.imp.getFunction() + "]", afterName, textY);

            // Deny reason (right of Use button, or if no space, below)
            if (!row.allowed && row.denyReason != null) {
                float reasonX = row.btnX - measureSmall(row.denyReason) - PAD / 2f;
                if (reasonX < dialogX + PAD) {
                    // fall back to just showing it at default indented position
                    reasonX = dialogX + PAD * 2;
                }
                smallFont.setColor(DENY_COLOR);
                smallFont.draw(batch, row.denyReason, reasonX, textY);
            }

            // Use button label
            font.setColor(Color.WHITE);
            glyph.setText(font, useLabel);
            font.draw(batch, useLabel,
                    row.btnX + (row.btnW - glyph.width) / 2f,
                    row.btnY + (row.btnH + glyph.height) / 2f);
        }
        batch.end();

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        // ── Title (above scissor area, always visible) ────────────────────────
        batch.begin();
        String title = buildingName.isEmpty() ? "Use Improvements" : buildingName;
        font.setColor(TITLE_COLOR);
        glyph.setText(font, title);
        font.draw(batch, title,
                dialogX + (dialogW - glyph.width) / 2f,
                dialogY + dialogH - PAD);

        // Close button label
        font.setColor(Color.WHITE);
        glyph.setText(font, closeLabel);
        font.draw(batch, closeLabel,
                closeBtnX + (closeBtnW - glyph.width) / 2f,
                closeBtnY + (closeBtnH + glyph.height) / 2f);
        batch.end();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Returns the plain-English reason why the player cannot use the
     * improvement, or {@code null} if all restrictions are satisfied.
     */
    private String buildDenyReason(Improvement imp) {
        ImprovementData data = imp.getData();
        if (data == null || !data.hasRestrict()) return null;

        List<String> failures = new ArrayList<>();

        // Gender check
        String reqGender = data.getRequiredGender();
        if (reqGender != null && !reqGender.isEmpty()) {
            String playerGender = profile.getGender();
            if (!reqGender.equalsIgnoreCase(playerGender)) {
                failures.add("you are not " + reqGender);
            }
        }

        // Strength check
        int reqStr = data.getRequiredStrength();
        if (reqStr > 0) {
            int playerStr = profile.getAttribute(CharacterAttribute.STRENGTH.name());
            if (playerStr < reqStr) {
                failures.add("not strong enough");
            }
        }

        if (failures.isEmpty()) return null;

        if (failures.size() == 1) return failures.get(0);

        // Multiple failures: join with " and "
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < failures.size(); i++) {
            if (i > 0) sb.append(" and ");
            sb.append(failures.get(i));
        }
        return sb.toString();
    }

    private float measureSmall(String text) {
        glyph.setText(smallFont, text);
        return glyph.width;
    }
}

package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.Map;

/**
 * Draws the info panel (bottom strip) and the top info bar (date/money).
 * Button bounds are written into {@link MapViewState} after each draw so that
 * MainScreen can perform hit-testing without knowing layout details.
 *
 * <p>The cell-info content area supports vertical and horizontal scrolling via
 * GL scissor clipping.  {@link MapViewState#infoScrollX} / {@link MapViewState#infoScrollY}
 * are clamped here each frame; {@link MapViewState#infoMaxScrollX} /
 * {@link MapViewState#infoMaxScrollY} are also written here for MainScreen drag handling.
 *
 * <p>Typography hierarchy:
 * <ul>
 *   <li>Labels ("Cell:", "Terrain:", …) – {@code font}, {@link #LABEL_COLOR}</li>
 *   <li>Values (coordinates, terrain name, …) – {@code font}, white</li>
 *   <li>Building name value – {@code smallFont}, white, vertically centred with label</li>
 *   <li>Improvement name – {@code font}, white</li>
 *   <li>Improvement attribute modifier [STR+2] – {@code smallFont}, bright white,
 *       vertically centred with improvement name</li>
 * </ul>
 */
class InfoPanelRenderer {

    // --- Colors ---
    private static final Color INFO_BG_COLOR         = new Color(0.15f, 0.15f, 0.2f,  1f);
    private static final Color INFO_BORDER_COLOR      = new Color(0.4f,  0.4f,  0.5f,  1f);
    private static final Color MOVE_TO_BUTTON_COLOR   = new Color(0.1f,  0.5f,  0.15f, 1f);
    private static final Color LOOK_AROUND_BTN_COLOR  = new Color(0.1f,  0.3f,  0.6f,  1f);
    private static final Color SCROLLBAR_TRACK_COLOR  = new Color(0.2f,  0.2f,  0.3f,  1f);
    private static final Color SCROLLBAR_THUMB_COLOR  = new Color(0.5f,  0.5f,  0.7f,  1f);
    static final Color         LABEL_COLOR            = new Color(0f,    1f,    0f,    1f);

    // --- Rendering resources ---
    private final SpriteBatch   batch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyphLayout;
    private final CityMap       cityMap;
    private final Profile       profile;

    // Active scroll offsets during the clipped content draw pass (reset to 0 outside it)
    private float drawScrollX, drawScrollY;

    InfoPanelRenderer(SpriteBatch batch, ShapeRenderer shapeRenderer,
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
    // Top info bar (date + money)
    // -------------------------------------------------------------------------

    void drawInfoBar(MapViewState s) {
        float barY = s.screenHeight - MapViewState.INFO_BAR_HEIGHT;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(INFO_BG_COLOR);
        shapeRenderer.rect(0, barY, s.screenWidth, MapViewState.INFO_BAR_HEIGHT);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(INFO_BORDER_COLOR);
        shapeRenderer.line(0, barY, s.screenWidth, barY);
        shapeRenderer.end();

        batch.begin();
        glyphLayout.setText(smallFont, "Hg");
        float textY = barY + (MapViewState.INFO_BAR_HEIGHT + glyphLayout.height) / 2;

        // Date / time (left) — smallFont
        String dateTime = profile.getGameDateTime();
        int spaceIdx = dateTime.indexOf(' ');
        String datePart = spaceIdx >= 0 ? dateTime.substring(0, spaceIdx) : dateTime;
        String timePart = spaceIdx >= 0 ? dateTime.substring(spaceIdx) : "";

        smallFont.setColor(Color.GREEN);
        smallFont.draw(batch, datePart, 10, textY);
        glyphLayout.setText(smallFont, datePart);
        smallFont.setColor(Color.WHITE);
        smallFont.draw(batch, timePart, 10 + glyphLayout.width, textY);

        // Money (right) — smallFont
        String moneyText = "$" + profile.getMoney();
        glyphLayout.setText(smallFont, moneyText);
        smallFont.setColor(Color.YELLOW);
        smallFont.draw(batch, moneyText, s.screenWidth - glyphLayout.width - 10, textY);

        // Stamina (centre) — label white, value cyan/blue
        String staminaLabel = "Stamina ";
        String staminaValue = profile.getCurrentStamina() + " / " + profile.getMaxStamina();
        glyphLayout.setText(smallFont, staminaLabel + staminaValue);
        float staminaTotalW = glyphLayout.width;
        float staminaX = (s.screenWidth - staminaTotalW) / 2f;
        smallFont.setColor(Color.WHITE);
        smallFont.draw(batch, staminaLabel, staminaX, textY);
        glyphLayout.setText(smallFont, staminaLabel);
        smallFont.setColor(new Color(0.4f, 0.7f, 1.0f, 1f));
        smallFont.draw(batch, staminaValue, staminaX + glyphLayout.width, textY);

        smallFont.setColor(Color.WHITE);
        batch.end();
    }

    // -------------------------------------------------------------------------
    // Info block (bottom strip)
    // -------------------------------------------------------------------------

    void drawInfoBlock(MapViewState s, boolean lookAroundIdle) {
        boolean showMoveToButton = s.selectedCellX >= 0 && s.selectedCellY >= 0
                && (s.selectedCellX != s.charCellX || s.selectedCellY != s.charCellY);
        boolean canMove = showMoveToButton && s.currentRoute != null && s.currentRoute.isReachable();

        boolean showLookAroundButton = !showMoveToButton
                && s.selectedCellX >= 0 && s.selectedCellY >= 0
                && s.selectedCellX == s.charCellX && s.selectedCellY == s.charCellY
                && lookAroundIdle
                && cityMap.getCell(s.selectedCellX, s.selectedCellY).hasBuilding()
                && cityMap.getCell(s.selectedCellX, s.selectedCellY).getBuilding().isDiscovered()
                && hasUndiscoveredImprovements(
                        cityMap.getCell(s.selectedCellX, s.selectedCellY).getBuilding());

        // --- Font metrics ---
        glyphLayout.setText(font, "Hg");
        float fontCapH  = glyphLayout.height;
        float fontLineH = fontCapH * 1.4f;
        glyphLayout.setText(smallFont, "Hg");
        float smallCapH  = glyphLayout.height;
        float smallLineH = smallCapH * 1.4f;
        // Vertical offsets for smallFont relative to a font-height row
        float valCenterOff = (fontCapH - smallCapH) / 2f;  // centre-aligns small with big
        float valBottomOff = fontCapH - smallCapH;          // bottom-aligns small with big

        // --- Button sizing ---
        final float PAD_X = 24f, PAD_Y = 10f, BTN_PAD = 14f;
        glyphLayout.setText(font, "Move to");
        final float BTN_W = glyphLayout.width + PAD_X * 2;
        final float BTN_H = glyphLayout.height + PAD_Y * 2;
        glyphLayout.setText(font, "Look around");
        final float LA_W  = glyphLayout.width + PAD_X * 2;

        float btnX = 20f;
        float btnY = s.infoAreaHeight - BTN_PAD - BTN_H;

        // Write button bounds for hit-testing
        s.moveToButtonX = btnX; s.moveToButtonY = btnY;
        s.moveToButtonW = showMoveToButton ? BTN_W : 0f;
        s.moveToButtonH = BTN_H;
        s.lookAroundBtnX = btnX; s.lookAroundBtnY = btnY;
        s.lookAroundBtnW = showLookAroundButton ? LA_W : 0f;
        s.lookAroundBtnH = BTN_H;

        boolean hasButton = showMoveToButton || showLookAroundButton;

        // --- Content area ---
        final float SB = MapViewState.SCROLLBAR_THICKNESS;
        // contentStartY: virtual Y of the first text line (y-up, no scroll applied)
        float contentStartY = hasButton
                ? btnY - BTN_PAD - fontLineH
                : s.infoAreaHeight - fontLineH;
        float contentAreaBottom = SB;                        // above horizontal scrollbar
        float contentAreaH      = contentStartY - contentAreaBottom;
        float contentAreaW      = s.screenWidth - SB;        // left of vertical scrollbar

        // --- Measure content and update max-scroll bounds ---
        float totalContentH = computeContentHeight(s, fontLineH);
        float totalContentW = computeContentWidth(s, 20f);
        s.infoMaxScrollY = Math.max(0f, totalContentH - contentAreaH);
        s.infoMaxScrollX = Math.max(0f, totalContentW - contentAreaW);
        s.infoScrollY = MathUtils.clamp(s.infoScrollY, 0f, s.infoMaxScrollY);
        s.infoScrollX = MathUtils.clamp(s.infoScrollX, 0f, s.infoMaxScrollX);

        // --- Shapes (background + buttons) ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(INFO_BG_COLOR);
        shapeRenderer.rect(0, 0, s.screenWidth, s.infoAreaHeight);
        if (showMoveToButton) {
            shapeRenderer.setColor(canMove ? MOVE_TO_BUTTON_COLOR : Color.DARK_GRAY);
            shapeRenderer.rect(btnX, btnY, BTN_W, BTN_H);
        }
        if (showLookAroundButton) {
            shapeRenderer.setColor(LOOK_AROUND_BTN_COLOR);
            shapeRenderer.rect(btnX, btnY, LA_W, BTN_H);
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(INFO_BORDER_COLOR);
        shapeRenderer.line(0, s.infoAreaHeight, s.screenWidth, s.infoAreaHeight);
        if (showMoveToButton) {
            shapeRenderer.rect(btnX,     btnY,     BTN_W,     BTN_H);
            shapeRenderer.rect(btnX + 1, btnY + 1, BTN_W - 2, BTN_H - 2);
        }
        if (showLookAroundButton) {
            shapeRenderer.rect(btnX,     btnY,     LA_W,     BTN_H);
            shapeRenderer.rect(btnX + 1, btnY + 1, LA_W - 2, BTN_H - 2);
        }
        shapeRenderer.end();

        // --- Text ---
        batch.begin();
        float textX = 20f;

        // Button text (no scroll)
        if (showMoveToButton) {
            glyphLayout.setText(font, "Move to");
            font.setColor(Color.WHITE);
            font.draw(batch, "Move to",
                    btnX + (BTN_W - glyphLayout.width) / 2,
                    btnY + (BTN_H + glyphLayout.height) / 2);
            String timeStr = canMove ? s.currentRoute.formatTime() : "Unreachable";
            glyphLayout.setText(smallFont, timeStr);
            smallFont.setColor(canMove ? Color.WHITE : Color.RED);
            smallFont.draw(batch, timeStr, btnX + BTN_W + 10f,
                    btnY + (BTN_H + glyphLayout.height) / 2);
            smallFont.setColor(Color.WHITE);
        }
        if (showLookAroundButton) {
            glyphLayout.setText(font, "Look around");
            font.setColor(Color.WHITE);
            font.draw(batch, "Look around",
                    btnX + (LA_W - glyphLayout.width) / 2,
                    btnY + (BTN_H + glyphLayout.height) / 2);
            smallFont.setColor(Color.WHITE);
            smallFont.draw(batch, "10 min", btnX + LA_W + 10f,
                    btnY + (BTN_H + smallFont.getLineHeight() * 0.5f) / 2);
        }

        // GL scissor: clips content to its area (below buttons, above horizontal scrollbar)
        batch.flush();
        float scissorTop = hasButton ? btnY : s.infoAreaHeight;
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, (int) contentAreaBottom,
                (int) contentAreaW, Math.max(0, (int)(scissorTop - contentAreaBottom)));

        // Activate scroll offsets for drawLabelValue and direct draws
        drawScrollX = s.infoScrollX;
        drawScrollY = s.infoScrollY;

        // Content text (virtual textY; actual draw = textY + drawScrollY, x - drawScrollX)
        float textY = contentStartY;
        if (s.selectedCellX >= 0 && s.selectedCellY >= 0) {
            Cell cell = cityMap.getCell(s.selectedCellX, s.selectedCellY);
            textY = drawLabelValue(font, "Cell: ",
                    s.selectedCellX + ", " + s.selectedCellY, textX, textY);
            textY -= fontLineH;
            textY = drawLabelValue(font, "Terrain: ",
                    cell.getTerrainType().getDisplayName(), textX, textY);
            textY -= fontLineH;

            if (cell.hasBuilding()) {
                Building building = cell.getBuilding();
                if (building.isDiscovered()) {
                    // Building: label = font/green; value = smallFont/white, vertically centred
                    String bMod = formatAttributeModifiers(building.getAttributeModifiers());
                    String bDisplay = building.getName() + (bMod.isEmpty() ? "" : " " + bMod);
                    float dx = textX - drawScrollX;
                    float dy = textY + drawScrollY;
                    font.setColor(LABEL_COLOR);
                    font.draw(batch, "Building: ", dx, dy);
                    glyphLayout.setText(font, "Building: ");
                    smallFont.setColor(Color.WHITE);
                    smallFont.draw(batch, bDisplay, dx + glyphLayout.width, dy - valCenterOff);
                    textY -= fontLineH;

                    if (building.getDefinition() != null) {
                        textY = drawLabelValue(font, "Category: ",
                                building.getCategory(), textX, textY);
                        textY -= fontLineH;
                        textY = drawLabelValue(font, "Floors: ",
                                String.valueOf(building.getFloors()), textX, textY);
                        textY -= fontLineH;
                    }

                    // Improvements: header font/green; name font/white;
                    // modifier smallFont/bright-white vertically centred with name
                    font.setColor(LABEL_COLOR);
                    font.draw(batch, "Improvements:", textX - drawScrollX, textY + drawScrollY);
                    textY -= fontLineH;
                    for (Improvement imp : building.getImprovements()) {
                        float idy = textY + drawScrollY;
                        if (idy < contentAreaBottom - fontLineH) break; // off-screen below
                        float idx = textX - drawScrollX;
                        if (imp.isDiscovered()) {
                            String modStr = formatAttributeModifiers(imp.getAttributeModifiers());
                            String namePart = "  - " + imp.getName();
                            font.setColor(Color.WHITE);
                            font.draw(batch, namePart, idx, idy);
                            if (!modStr.isEmpty()) {
                                glyphLayout.setText(font, namePart);
                                smallFont.setColor(Color.WHITE);
                                smallFont.draw(batch, " " + modStr,
                                        idx + glyphLayout.width, idy - valBottomOff);
                            }
                        } else {
                            font.setColor(Color.WHITE);
                            font.draw(batch, "  - ???", idx, idy);
                        }
                        textY -= fontLineH;
                    }
                } else {
                    drawLabelValue(font, "Building: ", "???", textX, textY);
                }
            }
        } else {
            font.setColor(Color.WHITE);
            font.draw(batch, "Click on a cell to see details",
                    textX - drawScrollX, textY + drawScrollY);
        }

        // Remove scissor before drawing fixed overlays
        batch.flush();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        drawScrollX = 0f;
        drawScrollY = 0f;

        // Zoom text (top-right, not scrolled)
        if (s.zoomLevel != s.lastZoomLevel) {
            s.cachedZoomText = "Zoom: " + String.format("%.1fx", s.zoomLevel);
            s.lastZoomLevel = s.zoomLevel;
        }
        glyphLayout.setText(smallFont, s.cachedZoomText);
        smallFont.setColor(Color.WHITE);
        smallFont.draw(batch, s.cachedZoomText,
                s.screenWidth - glyphLayout.width - 20, s.infoAreaHeight - smallLineH);

        // Controls hint (above horizontal scrollbar)
        String hint = "Scroll to zoom | Drag to pan | +/- keys | Arrow keys";
        glyphLayout.setText(smallFont, hint);
        smallFont.setColor(Color.WHITE);
        smallFont.draw(batch, hint,
                (s.screenWidth - glyphLayout.width) / 2f, SB + smallLineH + 2f);

        batch.end();

        // Scrollbars drawn after batch.end() to avoid interleaving
        drawScrollbars(s, contentAreaH, contentAreaW, SB);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Draws a label (font/green) + value (font/white) pair at the given virtual position.
     * The actual draw position is offset by the active {@link #drawScrollX}/{@link #drawScrollY}.
     */
    private float drawLabelValue(BitmapFont fnt, String label, String value,
                                 float textX, float textY) {
        float dx = textX - drawScrollX;
        float dy = textY + drawScrollY;
        fnt.setColor(LABEL_COLOR);
        fnt.draw(batch, label, dx, dy);
        glyphLayout.setText(fnt, label);
        fnt.setColor(Color.WHITE);
        fnt.draw(batch, value, dx + glyphLayout.width, dy);
        return textY;
    }

    /**
     * Computes total virtual height of the content section (sum of all line advances).
     * Used to determine whether vertical scrolling is needed.
     */
    private float computeContentHeight(MapViewState s, float fontLineH) {
        if (s.selectedCellX < 0) return fontLineH; // "Click on a cell…"
        Cell cell = cityMap.getCell(s.selectedCellX, s.selectedCellY);
        float h = fontLineH * 2; // Cell (advance) + Terrain (advance)
        if (!cell.hasBuilding()) return h;
        Building b = cell.getBuilding();
        if (!b.isDiscovered()) return h + fontLineH; // Building: ??? (last line)
        h += fontLineH; // Building (advance)
        if (b.getDefinition() != null) h += fontLineH * 2; // Category + Floors
        h += fontLineH; // "Improvements:" header (advance)
        h += b.getImprovements().size() * fontLineH; // one row per improvement
        return h;
    }

    /**
     * Computes the maximum text width of any content line (including textX offset).
     * Used to determine whether horizontal scrolling is needed.
     */
    private float computeContentWidth(MapViewState s, float textX) {
        if (s.selectedCellX < 0) {
            glyphLayout.setText(font, "Click on a cell to see details");
            return textX + glyphLayout.width;
        }
        Cell cell = cityMap.getCell(s.selectedCellX, s.selectedCellY);
        float maxW = 0f;
        glyphLayout.setText(font, "Cell: " + s.selectedCellX + ", " + s.selectedCellY);
        maxW = Math.max(maxW, glyphLayout.width);
        glyphLayout.setText(font, "Terrain: " + cell.getTerrainType().getDisplayName());
        maxW = Math.max(maxW, glyphLayout.width);
        if (cell.hasBuilding()) {
            Building b = cell.getBuilding();
            if (b.isDiscovered()) {
                String bMod = formatAttributeModifiers(b.getAttributeModifiers());
                glyphLayout.setText(font, "Building: ");
                float labW = glyphLayout.width;
                glyphLayout.setText(smallFont, b.getName() + (bMod.isEmpty() ? "" : " " + bMod));
                maxW = Math.max(maxW, labW + glyphLayout.width);
                if (b.getDefinition() != null) {
                    glyphLayout.setText(font, "Category: " + b.getCategory());
                    maxW = Math.max(maxW, glyphLayout.width);
                    glyphLayout.setText(font, "Floors: " + b.getFloors());
                    maxW = Math.max(maxW, glyphLayout.width);
                }
                glyphLayout.setText(font, "Improvements:");
                maxW = Math.max(maxW, glyphLayout.width);
                for (Improvement imp : b.getImprovements()) {
                    String namePart = "  - " + (imp.isDiscovered() ? imp.getName() : "???");
                    glyphLayout.setText(font, namePart);
                    float lineW = glyphLayout.width;
                    if (imp.isDiscovered()) {
                        String modStr = formatAttributeModifiers(imp.getAttributeModifiers());
                        if (!modStr.isEmpty()) {
                            glyphLayout.setText(smallFont, " " + modStr);
                            lineW += glyphLayout.width;
                        }
                    }
                    maxW = Math.max(maxW, lineW);
                }
            } else {
                glyphLayout.setText(font, "Building: ???");
                maxW = Math.max(maxW, glyphLayout.width);
            }
        }
        return textX + maxW;
    }

    /** Draws V and H scrollbar track + thumb after the main batch has ended. */
    private void drawScrollbars(MapViewState s, float contentAreaH, float contentAreaW, float SB) {
        boolean showV = s.infoMaxScrollY > 0f;
        boolean showH = s.infoMaxScrollX > 0f;
        if (!showV && !showH) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (showV) {
            float trackX = s.screenWidth - SB;
            float trackY = SB;
            float trackH = contentAreaH;
            float totalH = trackH + s.infoMaxScrollY;
            float thumbH = Math.max(SB * 2f, trackH * trackH / totalH);
            float scrollRatio = s.infoScrollY / s.infoMaxScrollY;
            float thumbY = trackY + (1f - scrollRatio) * (trackH - thumbH);
            shapeRenderer.setColor(SCROLLBAR_TRACK_COLOR);
            shapeRenderer.rect(trackX, trackY, SB, trackH);
            shapeRenderer.setColor(SCROLLBAR_THUMB_COLOR);
            shapeRenderer.rect(trackX, thumbY, SB, thumbH);
        }

        if (showH) {
            float trackW = contentAreaW;
            float totalW = trackW + s.infoMaxScrollX;
            float thumbW = Math.max(SB * 2f, trackW * trackW / totalW);
            float scrollRatio = s.infoScrollX / s.infoMaxScrollX;
            float thumbX = scrollRatio * (trackW - thumbW);
            shapeRenderer.setColor(SCROLLBAR_TRACK_COLOR);
            shapeRenderer.rect(0, 0, trackW, SB);
            shapeRenderer.setColor(SCROLLBAR_THUMB_COLOR);
            shapeRenderer.rect(thumbX, 0, thumbW, SB);
        }

        shapeRenderer.end();
    }

    static String formatAttributeModifiers(Map<CharacterAttribute, Integer> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<CharacterAttribute, Integer> e : modifiers.entrySet()) {
            if (!first) sb.append(' ');
            first = false;
            String name = e.getKey().getDisplayName();
            String abbr = name.length() >= 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase();
            int val = e.getValue();
            sb.append(abbr);
            if (val > 0) sb.append('+');
            sb.append(val);
        }
        return sb.append(']').toString();
    }

    /** Returns true if the building has at least one improvement not yet discovered. */
    private static boolean hasUndiscoveredImprovements(Building building) {
        for (Improvement imp : building.getImprovements()) {
            if (!imp.isDiscovered()) return true;
        }
        return false;
    }
}

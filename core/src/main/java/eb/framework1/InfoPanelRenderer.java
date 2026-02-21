package eb.framework1;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.Map;

/**
 * Draws the info panel (bottom strip) and the top info bar (date/money).
 * Button bounds are written into {@link MapViewState} after each draw so that
 * MainScreen can perform hit-testing without knowing layout details.
 */
class InfoPanelRenderer {

    // --- Colors ---
    private static final Color INFO_BG_COLOR         = new Color(0.15f, 0.15f, 0.2f,  1f);
    private static final Color INFO_BORDER_COLOR      = new Color(0.4f,  0.4f,  0.5f,  1f);
    private static final Color MOVE_TO_BUTTON_COLOR   = new Color(0.1f,  0.5f,  0.15f, 1f);
    private static final Color LOOK_AROUND_BTN_COLOR  = new Color(0.1f,  0.3f,  0.6f,  1f);
    static final Color         LABEL_COLOR            = new Color(0f,    1f,    0f,    1f);

    // --- Rendering resources ---
    private final SpriteBatch   batch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final GlyphLayout   glyphLayout;
    private final CityMap       cityMap;
    private final Profile       profile;

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
        glyphLayout.setText(font, "Hg");
        float textY = barY + (MapViewState.INFO_BAR_HEIGHT + glyphLayout.height) / 2;

        String dateTime = profile.getGameDateTime();
        int spaceIdx = dateTime.indexOf(' ');
        String datePart = spaceIdx >= 0 ? dateTime.substring(0, spaceIdx) : dateTime;
        String timePart = spaceIdx >= 0 ? dateTime.substring(spaceIdx) : "";

        font.setColor(Color.GREEN);
        font.draw(batch, datePart, 10, textY);
        glyphLayout.setText(font, datePart);
        font.setColor(Color.WHITE);
        font.draw(batch, timePart, 10 + glyphLayout.width, textY);

        String moneyText = "$" + profile.getMoney();
        glyphLayout.setText(font, moneyText);
        font.setColor(Color.YELLOW);
        font.draw(batch, moneyText, s.screenWidth - glyphLayout.width - 10, textY);

        font.setColor(Color.WHITE);
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

        // --- Shapes ---
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
        glyphLayout.setText(font, "Hg");
        float fontLineH = glyphLayout.height * 1.4f;
        glyphLayout.setText(smallFont, "Hg");
        float smallLineH = glyphLayout.height * 1.4f;
        float textX = 20;

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
            // Show time cost next to button
            smallFont.setColor(Color.WHITE);
            smallFont.draw(batch, "10 min", btnX + LA_W + 10f,
                    btnY + (BTN_H + smallFont.getLineHeight() * 0.5f) / 2);
        }

        float textY = (showMoveToButton || showLookAroundButton)
                ? btnY - BTN_PAD - fontLineH
                : s.infoAreaHeight - fontLineH;

        if (s.selectedCellX >= 0 && s.selectedCellY >= 0) {
            Cell cell = cityMap.getCell(s.selectedCellX, s.selectedCellY);
            textY = drawLabelValue(font, "Cell: ", s.selectedCellX + ", " + s.selectedCellY, textX, textY);
            textY -= fontLineH;
            textY = drawLabelValue(font, "Terrain: ", cell.getTerrainType().getDisplayName(), textX, textY);
            textY -= fontLineH;

            if (cell.hasBuilding()) {
                Building building = cell.getBuilding();
                if (building.isDiscovered()) {
                    String bMod = formatAttributeModifiers(building.getAttributeModifiers());
                    String bDisplay = building.getName() + (bMod.isEmpty() ? "" : " " + bMod);
                    // Label in body font (green), value in smaller font (white)
                    font.setColor(LABEL_COLOR);
                    font.draw(batch, "Building: ", textX, textY);
                    glyphLayout.setText(font, "Building: ");
                    smallFont.setColor(Color.WHITE);
                    smallFont.draw(batch, bDisplay, textX + glyphLayout.width, textY);
                    textY -= fontLineH;
                    if (building.getDefinition() != null) {
                        textY = drawLabelValue(font, "Category: ", building.getCategory(), textX, textY);
                        textY -= fontLineH;
                        textY = drawLabelValue(font, "Floors: ", String.valueOf(building.getFloors()), textX, textY);
                        textY -= fontLineH;
                    }
                    if (textY > smallLineH * 6) {
                        // Whole improvements section in smallFont for a clear visual distinction
                        smallFont.setColor(LABEL_COLOR);
                        smallFont.draw(batch, "Improvements:", textX, textY);
                        textY -= smallLineH;
                        for (Improvement imp : building.getImprovements()) {
                            if (textY < smallLineH * 2) break;
                            if (imp.isDiscovered()) {
                                String modStr = formatAttributeModifiers(imp.getAttributeModifiers());
                                String namePart = "  - " + imp.getName();
                                smallFont.setColor(Color.WHITE);
                                smallFont.draw(batch, namePart, textX, textY);
                                if (!modStr.isEmpty()) {
                                    glyphLayout.setText(smallFont, namePart);
                                    smallFont.setColor(Color.WHITE);
                                    smallFont.draw(batch, " " + modStr,
                                            textX + glyphLayout.width, textY);
                                }
                            } else {
                                smallFont.setColor(Color.WHITE);
                                smallFont.draw(batch, "  - ???", textX, textY);
                            }
                            textY -= smallLineH;
                        }
                    }
                } else {
                    drawLabelValue(font, "Building: ", "???", textX, textY);
                }
            }
        } else {
            font.draw(batch, "Click on a cell to see details", textX, textY);
        }

        // Zoom text (top-right of info panel)
        if (s.zoomLevel != s.lastZoomLevel) {
            s.cachedZoomText = "Zoom: " + String.format("%.1fx", s.zoomLevel);
            s.lastZoomLevel = s.zoomLevel;
        }
        glyphLayout.setText(smallFont, s.cachedZoomText);
        smallFont.draw(batch, s.cachedZoomText,
                s.screenWidth - glyphLayout.width - 20, s.infoAreaHeight - smallLineH);

        // Controls hint
        String hint = "Scroll to zoom | Drag to pan | +/- keys | Arrow keys";
        glyphLayout.setText(smallFont, hint);
        smallFont.draw(batch, hint, (s.screenWidth - glyphLayout.width) / 2, smallLineH + 10);

        batch.end();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private float drawLabelValue(BitmapFont fnt, String label, String value, float textX, float textY) {
        fnt.setColor(LABEL_COLOR);
        fnt.draw(batch, label, textX, textY);
        glyphLayout.setText(fnt, label);
        fnt.setColor(Color.WHITE);
        fnt.draw(batch, value, textX + glyphLayout.width, textY);
        return textY;
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

    /** Returns true if the building has at least one improvement that has not yet been discovered. */
    private static boolean hasUndiscoveredImprovements(Building building) {
        for (Improvement imp : building.getImprovements()) {
            if (!imp.isDiscovered()) return true;
        }
        return false;
    }
}

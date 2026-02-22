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
 *   <li>Building attribute modifier [STR+2] – {@code tinyFont}, bright white, bottom-aligned</li>
 *   <li>Improvement name – {@code font}, white</li>
 *   <li>Improvement attribute modifier [STR+2] – {@code tinyFont}, bright white,
 *       bottom-aligned with improvement name</li>
 * </ul>
 */
class InfoPanelRenderer {

    // --- Colors ---
    private static final Color INFO_BG_COLOR         = new Color(0.15f, 0.15f, 0.2f,  1f);
    private static final Color INFO_BORDER_COLOR      = new Color(0.4f,  0.4f,  0.5f,  1f);
    private static final Color MOVE_TO_BUTTON_COLOR   = new Color(0.1f,  0.5f,  0.15f, 1f);
    private static final Color LOOK_AROUND_BTN_COLOR  = new Color(0.1f,  0.3f,  0.6f,  1f);
    private static final Color OFFICE_BTN_COLOR         = new Color(0.45f, 0.35f, 0.0f,  1f);
    private static final Color SCROLLBAR_TRACK_COLOR  = new Color(0.2f,  0.2f,  0.3f,  1f);
    private static final Color SCROLLBAR_THUMB_COLOR  = new Color(0.5f,  0.5f,  0.7f,  1f);
    static final Color         LABEL_COLOR            = new Color(0f,    1f,    0f,    1f);

    // --- Rendering resources ---
    private final SpriteBatch   batch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final BitmapFont    tinyFont;
    private final GlyphLayout   glyphLayout;
    private final CityMap       cityMap;
    private final Profile       profile;

    // Active scroll offsets during the clipped content draw pass (reset to 0 outside it)
    private float drawScrollX, drawScrollY;

    InfoPanelRenderer(SpriteBatch batch, ShapeRenderer shapeRenderer,
                      BitmapFont font, BitmapFont smallFont, BitmapFont tinyFont,
                      GlyphLayout glyphLayout,
                      CityMap cityMap, Profile profile) {
        this.batch         = batch;
        this.shapeRenderer = shapeRenderer;
        this.font          = font;
        this.smallFont     = smallFont;
        this.tinyFont      = tinyFont;
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

        // Stamina (centre) — value only, cyan/blue, e.g. "20/20"
        String staminaValue = profile.getCurrentStamina() + "/" + profile.getMaxStamina();
        glyphLayout.setText(smallFont, staminaValue);
        float staminaX = (s.screenWidth - glyphLayout.width) / 2f;
        smallFont.setColor(new Color(0.4f, 0.7f, 1.0f, 1f));
        smallFont.draw(batch, staminaValue, staminaX, textY);

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

        boolean atCurrentBuilding = !showMoveToButton
                && s.selectedCellX >= 0 && s.selectedCellY >= 0
                && s.selectedCellX == s.charCellX && s.selectedCellY == s.charCellY
                && lookAroundIdle
                && cityMap.getCell(s.selectedCellX, s.selectedCellY).hasBuilding()
                && cityMap.getCell(s.selectedCellX, s.selectedCellY).getBuilding().isDiscovered();

        boolean atHome = s.selectedCellX == s.homeCellX && s.selectedCellY == s.homeCellY;
        Building selBuilding = atCurrentBuilding
                ? cityMap.getCell(s.selectedCellX, s.selectedCellY).getBuilding() : null;

        boolean showLookAroundButton = selBuilding != null && selBuilding.hasUndiscoveredImprovements();

        boolean showOfficeButton = atHome && selBuilding != null
                && selBuilding.getFloors() > 1;

        // --- Font metrics ---
        glyphLayout.setText(font, "Hg");
        float fontCapH  = glyphLayout.height;
        float fontLineH = fontCapH * 1.4f;
        glyphLayout.setText(smallFont, "Hg");
        float smallCapH  = glyphLayout.height;
        float smallLineH = smallCapH * 1.4f;
        glyphLayout.setText(tinyFont, "Hg");
        float tinyCapH  = glyphLayout.height;
        // Vertical offsets for smallFont / tinyFont relative to a font-height row
        float valCenterOff      = (fontCapH - smallCapH) / 2f;   // centre-aligns small with big
        float valTinyBottomOff  = fontCapH - tinyCapH;            // bottom-aligns tiny with big (font row)
        float valSmallTinyOff   = smallCapH - tinyCapH;           // bottom-aligns tiny with small row

        // --- Button sizing ---
        final float PAD_X = 24f, PAD_Y = 10f, BTN_PAD = 14f, BTN_SPACING = 16f;
        TextMeasurer.TextBounds moveBounds = TextMeasurer.measure(font, glyphLayout, "Move to",     PAD_X, PAD_Y);
        TextMeasurer.TextBounds laBounds   = TextMeasurer.measure(font, glyphLayout, "Look around", PAD_X, PAD_Y);
        final float BTN_W = moveBounds.width;
        final float BTN_H = moveBounds.height;
        final float LA_W  = laBounds.width;
        // office button label width computed dynamically below

        // Buttons stack vertically from the top of the info panel downward
        final float btnX = 20f;
        float curRowBottom = s.infoAreaHeight - BTN_PAD - BTN_H;
        float lowestBtnBottom = -1f; // bottom Y of last visible button

        s.moveToButtonX = btnX; s.moveToButtonH = BTN_H; s.moveToButtonW = showMoveToButton ? BTN_W : 0f;
        s.moveToButtonY = curRowBottom;
        if (showMoveToButton) { lowestBtnBottom = curRowBottom; curRowBottom -= BTN_H + BTN_SPACING; }

        s.lookAroundBtnX = btnX; s.lookAroundBtnH = BTN_H; s.lookAroundBtnW = showLookAroundButton ? LA_W : 0f;
        s.lookAroundBtnY = curRowBottom;
        if (showLookAroundButton) { lowestBtnBottom = curRowBottom; curRowBottom -= BTN_H + BTN_SPACING; }

        // Rest/Sleep buttons are handled by UnitInteriorPopup; zero them out here
        s.restBtnW = 0f;
        s.sleepBtnW = 0f;

        // Office button — width based on the actual label text
        String officeBtnLabel = showOfficeButton
                ? "Your Office: " + floorOrdinal(s.homeFloor) + " Fl Unit " + s.homeFloor + s.homeUnitLetter
                : "";
        float OFFICE_W = 0f;
        if (showOfficeButton) {
            OFFICE_W = TextMeasurer.measure(font, glyphLayout, officeBtnLabel, PAD_X, PAD_Y).width;
        }
        s.goToOfficeBtnX = btnX; s.goToOfficeBtnH = BTN_H; s.goToOfficeBtnW = showOfficeButton ? OFFICE_W : 0f;
        s.goToOfficeBtnY = curRowBottom;
        if (showOfficeButton) { lowestBtnBottom = curRowBottom; }

        boolean hasButton = showMoveToButton || showLookAroundButton || showOfficeButton;

        // --- Content area ---
        final float SB = MapViewState.SCROLLBAR_THICKNESS;
        // contentStartY: virtual Y of the first text line (y-up, no scroll applied)
        float contentStartY = hasButton
                ? lowestBtnBottom - BTN_PAD - fontLineH
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
            shapeRenderer.rect(s.moveToButtonX, s.moveToButtonY, BTN_W, BTN_H);
        }
        if (showLookAroundButton) {
            shapeRenderer.setColor(LOOK_AROUND_BTN_COLOR);
            shapeRenderer.rect(s.lookAroundBtnX, s.lookAroundBtnY, LA_W, BTN_H);
        }
        if (showOfficeButton) {
            shapeRenderer.setColor(OFFICE_BTN_COLOR);
            shapeRenderer.rect(s.goToOfficeBtnX, s.goToOfficeBtnY, OFFICE_W, BTN_H);
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(INFO_BORDER_COLOR);
        shapeRenderer.line(0, s.infoAreaHeight, s.screenWidth, s.infoAreaHeight);
        if (showMoveToButton) {
            shapeRenderer.rect(s.moveToButtonX,     s.moveToButtonY,     BTN_W,     BTN_H);
            shapeRenderer.rect(s.moveToButtonX + 1, s.moveToButtonY + 1, BTN_W - 2, BTN_H - 2);
        }
        if (showLookAroundButton) {
            shapeRenderer.rect(s.lookAroundBtnX,     s.lookAroundBtnY,     LA_W,     BTN_H);
            shapeRenderer.rect(s.lookAroundBtnX + 1, s.lookAroundBtnY + 1, LA_W - 2, BTN_H - 2);
        }
        if (showOfficeButton) {
            shapeRenderer.rect(s.goToOfficeBtnX,     s.goToOfficeBtnY,     OFFICE_W,     BTN_H);
            shapeRenderer.rect(s.goToOfficeBtnX + 1, s.goToOfficeBtnY + 1, OFFICE_W - 2, BTN_H - 2);
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
                    s.moveToButtonX + (BTN_W - glyphLayout.width) / 2,
                    s.moveToButtonY + (BTN_H + glyphLayout.height) / 2);
            String timeStr = canMove ? s.currentRoute.formatTime() : "Unreachable";
            glyphLayout.setText(smallFont, timeStr);
            smallFont.setColor(canMove ? Color.WHITE : Color.RED);
            smallFont.draw(batch, timeStr, s.moveToButtonX + BTN_W + 10f,
                    s.moveToButtonY + (BTN_H + glyphLayout.height) / 2);
            smallFont.setColor(Color.WHITE);
        }
        if (showLookAroundButton) {
            glyphLayout.setText(font, "Look around");
            font.setColor(Color.WHITE);
            font.draw(batch, "Look around",
                    s.lookAroundBtnX + (LA_W - glyphLayout.width) / 2,
                    s.lookAroundBtnY + (BTN_H + glyphLayout.height) / 2);
            smallFont.setColor(Color.WHITE);
            smallFont.draw(batch, "10 min", s.lookAroundBtnX + LA_W + 10f,
                    s.lookAroundBtnY + (BTN_H + smallFont.getLineHeight() * 0.5f) / 2);
        }
        if (showOfficeButton) {
            glyphLayout.setText(font, officeBtnLabel);
            font.setColor(Color.WHITE);
            font.draw(batch, officeBtnLabel,
                    s.goToOfficeBtnX + (OFFICE_W - glyphLayout.width) / 2,
                    s.goToOfficeBtnY + (BTN_H + glyphLayout.height) / 2);
        }

        // GL scissor: clips content to its area (below buttons, above horizontal scrollbar)
        batch.flush();
        float scissorTop = hasButton ? lowestBtnBottom : s.infoAreaHeight;
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
            textY = drawLabelValue(font, "Terrain: ",
                    cell.getTerrainType().getDisplayName(), textX, textY);
            textY -= fontLineH;

            if (cell.hasBuilding()) {
                Building building = cell.getBuilding();
                if (building.isDiscovered()) {
                    // Building: label = font/green; name = smallFont/white, centred;
                    //           modifier = tinyFont/white, bottom-aligned with name row
                    String bMod = formatAttributeModifiers(building.getAttributeModifiers());
                    float dx = textX - drawScrollX;
                    float dy = textY + drawScrollY;
                    font.setColor(LABEL_COLOR);
                    font.draw(batch, "Building: ", dx, dy);
                    glyphLayout.setText(font, "Building: ");
                    float nameX = dx + glyphLayout.width;
                    String bName = building.getName();
                    smallFont.setColor(Color.WHITE);
                    smallFont.draw(batch, bName, nameX, dy - valCenterOff);
                    if (!bMod.isEmpty()) {
                        glyphLayout.setText(smallFont, bName);
                        tinyFont.setColor(Color.WHITE);
                        tinyFont.draw(batch, " " + bMod,
                                nameX + glyphLayout.width, dy - valCenterOff - valSmallTinyOff);
                    }
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
                                tinyFont.setColor(Color.WHITE);
                                tinyFont.draw(batch, " " + modStr,
                                        idx + glyphLayout.width, idy - valTinyBottomOff);
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

        batch.end();

        // Scrollbars drawn after batch.end() to avoid interleaving
        drawScrollbars(s, contentAreaH, contentAreaW, SB);

        // "?" toggle button — lower-right corner of info panel
        TextMeasurer.TextBounds qb = TextMeasurer.measure(font, glyphLayout, "?", 14f, 8f);
        s.helpBtnX = s.screenWidth - qb.width - 6f;
        s.helpBtnY = 6f;
        s.helpBtnW = qb.width;
        s.helpBtnH = qb.height;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(s.helpVisible
                ? new Color(0.1f, 0.35f, 0.1f, 1f)
                : new Color(0.15f, 0.15f, 0.25f, 1f));
        shapeRenderer.rect(s.helpBtnX, s.helpBtnY, s.helpBtnW, s.helpBtnH);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(INFO_BORDER_COLOR);
        shapeRenderer.rect(s.helpBtnX,     s.helpBtnY,     s.helpBtnW,     s.helpBtnH);
        shapeRenderer.rect(s.helpBtnX + 1, s.helpBtnY + 1, s.helpBtnW - 2, s.helpBtnH - 2);
        shapeRenderer.end();

        batch.begin();
        glyphLayout.setText(font, "?");
        font.setColor(Color.YELLOW);
        font.draw(batch, "?",
                s.helpBtnX + (s.helpBtnW - glyphLayout.width) / 2f,
                s.helpBtnY + (s.helpBtnH + glyphLayout.height) / 2f);
        batch.end();
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
        float h = fontLineH; // Terrain (advance)
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
        glyphLayout.setText(font, "Terrain: " + cell.getTerrainType().getDisplayName());
        maxW = Math.max(maxW, glyphLayout.width);
        if (cell.hasBuilding()) {
            Building b = cell.getBuilding();
            if (b.isDiscovered()) {
                String bMod = formatAttributeModifiers(b.getAttributeModifiers());
                glyphLayout.setText(font, "Building: ");
                float labW = glyphLayout.width;
                glyphLayout.setText(smallFont, b.getName());
                float nameW = glyphLayout.width;
                if (!bMod.isEmpty()) {
                    glyphLayout.setText(tinyFont, " " + bMod);
                    nameW += glyphLayout.width;
                }
                maxW = Math.max(maxW, labW + nameW);
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
                            glyphLayout.setText(tinyFont, " " + modStr);
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

    /** Ordinal string for a 1-based floor number: 1→"1st", 2→"2nd", 3→"3rd", etc. */
    static String floorOrdinal(int n) {
        if (n <= 0) return n + "th";
        int mod100 = n % 100;
        if (mod100 >= 11 && mod100 <= 13) return n + "th";
        switch (n % 10) {
            case 1:  return n + "st";
            case 2:  return n + "nd";
            case 3:  return n + "rd";
            default: return n + "th";
        }
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

}

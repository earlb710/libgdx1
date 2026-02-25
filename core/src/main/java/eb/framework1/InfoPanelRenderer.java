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
    private static final Color STASH_BTN_COLOR           = new Color(0.35f, 0.15f, 0.50f, 1f);
    private static final Color EMAIL_BTN_COLOR           = new Color(0.10f, 0.30f, 0.50f, 1f);
    private static final Color SCROLLBAR_TRACK_COLOR  = new Color(0.2f,  0.2f,  0.3f,  1f);
    private static final Color SCROLLBAR_THUMB_COLOR  = new Color(0.5f,  0.5f,  0.7f,  1f);
    static final Color         LABEL_COLOR            = new Color(0f,    1f,    0f,    1f);
    private static final Color ATTR_TOTAL_COLOR       = new Color(0.5f,  1.0f,  0.5f,  1f);
    static final Color         NOVEL_COLOR            = new Color(0.70f, 0.90f, 1.00f, 1f);
    private static final Color ADD_NOTE_BTN_COLOR     = new Color(0.3f,  0.3f,  0.55f, 1f);
    private static final Color NOTE_COLOR             = new Color(0.85f, 0.85f, 0.70f, 1f);
    private static final Color APPOINTMENT_BTN_COLOR  = new Color(0.6f,  0.45f, 0.0f,  1f);
    private static final Color SERVICE_BTN_COLOR       = new Color(0.15f, 0.45f, 0.45f, 1f);

    // --- Rendering resources ---
    private final SpriteBatch   batch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont    font;
    private final BitmapFont    smallFont;
    private final BitmapFont    boldSmallFont;
    private final BitmapFont    tinyFont;
    private final BitmapFont    noteFont;
    private final GlyphLayout   glyphLayout;
    private final CityMap       cityMap;
    private final Profile       profile;
    private final NovelTextEngine novelTextEngine;

    // Active scroll offsets during the clipped content draw pass (reset to 0 outside it)
    private float drawScrollX, drawScrollY;

    InfoPanelRenderer(SpriteBatch batch, ShapeRenderer shapeRenderer,
                      BitmapFont font, BitmapFont smallFont, BitmapFont boldSmallFont,
                      BitmapFont tinyFont, BitmapFont noteFont,
                      GlyphLayout glyphLayout,
                      CityMap cityMap, Profile profile, NovelTextEngine novelTextEngine) {
        this.batch           = batch;
        this.shapeRenderer   = shapeRenderer;
        this.font            = font;
        this.smallFont       = smallFont;
        this.boldSmallFont   = boldSmallFont;
        this.tinyFont        = tinyFont;
        this.noteFont        = noteFont;
        this.glyphLayout     = glyphLayout;
        this.cityMap         = cityMap;
        this.profile         = profile;
        this.novelTextEngine = novelTextEngine;
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

        // Stamina (centre) — "cur / effectiveMax"
        // effectiveMax mirrors Profile.getMaxStamina() but includes the location STAMINA modifier:
        //   max(10, (staminaAttr + locationMod) × 10)
        int staminaMod   = locationModFor(s.charCellX, s.charCellY, CharacterAttribute.STAMINA);
        int effectiveMax = Math.max(10,
                (profile.getAttribute(CharacterAttribute.STAMINA.name()) + staminaMod) * 10);
        String curStr = profile.getCurrentStamina() + "/";
        String maxStr = String.valueOf(effectiveMax);
        glyphLayout.setText(smallFont, curStr + maxStr);
        float staminaX = (s.screenWidth - glyphLayout.width) / 2f;
        smallFont.setColor(new Color(0.4f, 0.7f, 1.0f, 1f));
        smallFont.draw(batch, curStr, staminaX, textY);
        glyphLayout.setText(smallFont, curStr);
        // max: brighter cyan = bonus, orange = penalty, normal cyan = no modifier
        Color maxColor = staminaMod > 0 ? new Color(0.6f, 1.0f, 1.0f, 1f)
                       : staminaMod < 0 ? Color.ORANGE
                       :                  new Color(0.4f, 0.7f, 1.0f, 1f);
        smallFont.setColor(maxColor);
        smallFont.draw(batch, maxStr, staminaX + glyphLayout.width, textY);

        smallFont.setColor(Color.WHITE);
        batch.end();
    }

    // -------------------------------------------------------------------------
    // Info block (bottom strip)
    // -------------------------------------------------------------------------

    void drawInfoBlock(MapViewState s, boolean lookAroundIdle) {

        // =====================================================================
        // TAB BAR — drawn at the top of the info panel
        // =====================================================================
        final String[] TAB_LABELS = { "Info", "Character", "Calendar", "Case File" };
        // Use smallFont for tabs so they fit at any info-panel size
        glyphLayout.setText(smallFont, "Hg");
        float tabFontH  = glyphLayout.height;
        float tabPadX   = 18f;
        float tabPadY   = 7f;
        float tabH      = tabFontH + tabPadY * 2f;
        float tabBarTop = s.infoAreaHeight;          // top of info panel (y-up)
        float tabBarY   = tabBarTop - tabH;          // bottom edge of tab bar

        s.tabBarHeight = tabH;
        s.tabH         = tabH;

        // Compute tab widths
        float[] tabWidths = new float[TAB_LABELS.length];
        for (int i = 0; i < TAB_LABELS.length; i++) {
            glyphLayout.setText(smallFont, TAB_LABELS[i]);
            tabWidths[i] = glyphLayout.width + tabPadX * 2f;
        }

        // The active tab is 3px taller at the top to appear "raised".
        // Inactive tabs are shifted up by that same amount so the bottom of all tabs
        // aligns with the separator, but the active tab sits 3px higher.
        final float TAB_RAISE = 3f;

        // Draw panel background first, then tab fills on top
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(INFO_BG_COLOR);
        shapeRenderer.rect(0, 0, s.screenWidth, s.infoAreaHeight);

        float tx = 0f;
        for (int i = 0; i < TAB_LABELS.length; i++) {
            boolean active = TAB_LABELS[i].equalsIgnoreCase(s.activeInfoTab);
            s.tabX[i] = tx;
            s.tabW[i] = tabWidths[i];
            if (active) {
                // Active tab: same bg as panel, raised, sits flush with content below
                s.tabY[i] = tabBarY;
                shapeRenderer.setColor(INFO_BG_COLOR);
                shapeRenderer.rect(tx, tabBarY, tabWidths[i], tabH + TAB_RAISE);
            } else {
                // Inactive tab: darker, not raised; sits on the separator line
                s.tabY[i] = tabBarY;
                shapeRenderer.setColor(new Color(0.10f, 0.10f, 0.14f, 1f));
                shapeRenderer.rect(tx, tabBarY, tabWidths[i], tabH);
            }
            tx += tabWidths[i];
        }
        shapeRenderer.end();

        // Borders and separator
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(INFO_BORDER_COLOR);
        tx = 0f;
        float activeTabX = 0f, activeTabW = 0f;
        for (int i = 0; i < TAB_LABELS.length; i++) {
            boolean active = TAB_LABELS[i].equalsIgnoreCase(s.activeInfoTab);
            if (active) {
                activeTabX = tx;
                activeTabW = tabWidths[i];
                // Active tab: bright accent border, left + top + right only (no bottom)
                shapeRenderer.setColor(new Color(0.55f, 0.75f, 1.00f, 1f));
                shapeRenderer.line(tx,               tabBarY,             tx,               tabBarY + tabH + TAB_RAISE);
                shapeRenderer.line(tx,               tabBarY + tabH + TAB_RAISE, tx + tabWidths[i], tabBarY + tabH + TAB_RAISE);
                shapeRenderer.line(tx + tabWidths[i], tabBarY,            tx + tabWidths[i], tabBarY + tabH + TAB_RAISE);
                // Second inner line for a thicker look
                shapeRenderer.line(tx + 1f,           tabBarY,            tx + 1f,           tabBarY + tabH + TAB_RAISE - 1f);
                shapeRenderer.line(tx + 1f,           tabBarY + tabH + TAB_RAISE - 1f, tx + tabWidths[i] - 1f, tabBarY + tabH + TAB_RAISE - 1f);
                shapeRenderer.line(tx + tabWidths[i] - 1f, tabBarY,       tx + tabWidths[i] - 1f, tabBarY + tabH + TAB_RAISE - 1f);
            } else {
                // Inactive tab: dimmer border on all four sides
                shapeRenderer.setColor(new Color(0.35f, 0.35f, 0.45f, 1f));
                shapeRenderer.rect(tx, tabBarY, tabWidths[i], tabH);
            }
            tx += tabWidths[i];
        }
        // Separator line across the full width except under the active tab
        shapeRenderer.setColor(new Color(0.55f, 0.75f, 1.00f, 1f));
        shapeRenderer.line(0,                         tabBarY, activeTabX,              tabBarY);
        shapeRenderer.line(activeTabX + activeTabW,   tabBarY, s.screenWidth,           tabBarY);
        // Top panel border
        shapeRenderer.setColor(INFO_BORDER_COLOR);
        shapeRenderer.line(0, tabBarTop, s.screenWidth, tabBarTop);
        shapeRenderer.end();

        // Tab labels — active tab: bold white; inactive: dimmed regular
        batch.begin();
        tx = 0f;
        for (int i = 0; i < TAB_LABELS.length; i++) {
            boolean active = TAB_LABELS[i].equalsIgnoreCase(s.activeInfoTab);
            BitmapFont tabFont = active ? boldSmallFont : smallFont;
            glyphLayout.setText(tabFont, TAB_LABELS[i]);
            tabFont.setColor(active ? Color.WHITE : new Color(0.55f, 0.55f, 0.65f, 1f));
            float labelY = active
                    ? tabBarY + tabPadY + tabFontH + TAB_RAISE / 2f  // vertically centred in raised tab
                    : tabBarY + tabPadY + tabFontH;
            tabFont.draw(batch, TAB_LABELS[i],
                    tx + (tabWidths[i] - glyphLayout.width) / 2f,
                    labelY);
            tx += tabWidths[i];
        }
        batch.end();

        // The panel area available for tab content is below the tab bar
        int contentPanelH = (int) tabBarY; // pixels from y=0 up to tab bar bottom

        // =====================================================================
        // DISPATCH to active tab
        // =====================================================================
        if ("CHARACTER".equalsIgnoreCase(s.activeInfoTab)) {
            drawCharacterTab(s, contentPanelH);
        } else if ("CALENDAR".equalsIgnoreCase(s.activeInfoTab)) {
            drawCalendarTab(s, contentPanelH);
        } else if ("CASE FILE".equalsIgnoreCase(s.activeInfoTab)) {
            drawCaseFileTab(s, contentPanelH);
        } else {
            drawInfoTab(s, lookAroundIdle, contentPanelH);
        }
    }

    // -------------------------------------------------------------------------
    // Info tab content (original cell-info display)
    // -------------------------------------------------------------------------

    private void drawInfoTab(MapViewState s, boolean lookAroundIdle, int panelH) {
        s.addNoteBtnW = 0f; // Add Note button and checkboxes are only on the Case File tab
        s.noteTimeCbW = 0f;
        s.noteLocCbW  = 0f;
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
        float valCenterOff      = (fontCapH - smallCapH) / 2f;
        float valTinyBottomOff  = fontCapH - tinyCapH;
        float valSmallTinyOff   = smallCapH - tinyCapH;

        // --- Button sizing ---
        final float PAD_X = 24f, PAD_Y = 10f, BTN_PAD = 14f, BTN_SPACING = 16f;
        TextMeasurer.TextBounds moveBounds = TextMeasurer.measure(font, glyphLayout, "Move to",     PAD_X, PAD_Y);
        TextMeasurer.TextBounds laBounds   = TextMeasurer.measure(font, glyphLayout, "Look around", PAD_X, PAD_Y);
        final float BTN_W = moveBounds.width;
        final float BTN_H = moveBounds.height;
        final float LA_W  = laBounds.width;

        // Buttons stack from top of content area downward (top = tabBarY bottom)
        final float btnX = 20f;
        float curRowBottom = panelH - BTN_PAD - BTN_H;
        float lowestBtnBottom = -1f;

        s.moveToButtonX = btnX; s.moveToButtonH = BTN_H; s.moveToButtonW = showMoveToButton ? BTN_W : 0f;
        s.moveToButtonY = curRowBottom;
        if (showMoveToButton) { lowestBtnBottom = curRowBottom; curRowBottom -= BTN_H + BTN_SPACING; }

        s.lookAroundBtnX = btnX; s.lookAroundBtnH = BTN_H; s.lookAroundBtnW = showLookAroundButton ? LA_W : 0f;
        s.lookAroundBtnY = curRowBottom;
        if (showLookAroundButton) { lowestBtnBottom = curRowBottom; curRowBottom -= BTN_H + BTN_SPACING; }

        s.restBtnW = 0f;
        s.sleepBtnW = 0f;

        String officeBtnLabel = showOfficeButton
                ? "Your Office: " + floorOrdinal(s.homeFloor) + " Fl Unit " + s.homeFloor + s.homeUnitLetter
                : "";
        float OFFICE_W = 0f;
        if (showOfficeButton) {
            OFFICE_W = TextMeasurer.measure(font, glyphLayout, officeBtnLabel, PAD_X, PAD_Y).width;
        }
        s.goToOfficeBtnX = btnX; s.goToOfficeBtnH = BTN_H; s.goToOfficeBtnW = showOfficeButton ? OFFICE_W : 0f;
        s.goToOfficeBtnY = curRowBottom;
        if (showOfficeButton) { lowestBtnBottom = curRowBottom; curRowBottom -= BTN_H + BTN_SPACING; }

        // "Go to Room" button — shown when player is standing at a hotel where they have nights booked.
        boolean isHotel = selBuilding != null && BuildingServices.getHotelNightlyCost(selBuilding) > 0;
        boolean showHotelRoomButton = isHotel
                && profile.getAttribute(BuildingServices.ATTR_HOTEL_NIGHTS) > 0;
        int hotelRoomNum = profile.getAttribute(BuildingServices.ATTR_HOTEL_ROOM);
        String hotelRoomBtnLabel = showHotelRoomButton ? "Go to Room " + hotelRoomNum : "";
        float HOTEL_ROOM_W = 0f;
        if (showHotelRoomButton) {
            HOTEL_ROOM_W = TextMeasurer.measure(font, glyphLayout, hotelRoomBtnLabel, PAD_X, PAD_Y).width;
        }
        s.goToHotelRoomBtnX = btnX; s.goToHotelRoomBtnH = BTN_H;
        s.goToHotelRoomBtnW = showHotelRoomButton ? HOTEL_ROOM_W : 0f;
        s.goToHotelRoomBtnY = curRowBottom;
        if (showHotelRoomButton) { lowestBtnBottom = curRowBottom; curRowBottom -= BTN_H + BTN_SPACING; }

        // Open Stash button – shown inside the office (UnitInteriorPopup), not here
        boolean showStashButton = false;
        s.openStashBtnX = btnX; s.openStashBtnH = BTN_H; s.openStashBtnW = 0f;
        s.openStashBtnY = curRowBottom;

        // Check Emails button – shown inside the office (UnitInteriorPopup), not here
        boolean showCheckEmailsButton = false;
        s.checkEmailsBtnX = btnX; s.checkEmailsBtnH = BTN_H; s.checkEmailsBtnW = 0f;
        s.checkEmailsBtnY = curRowBottom;

        // Appointment button – shown when an upcoming appointment (≤ 3 h) is at the
        // player's current location and the player is viewing their own cell.
        CalendarEntry upcomingAppt = findUpcomingAppointmentAtLocation(s);
        boolean showAppointmentButton = upcomingAppt != null
                && s.selectedCellX == s.charCellX && s.selectedCellY == s.charCellY;
        String apptBtnLabel = showAppointmentButton ? "Appointment: " + upcomingAppt.title : "";
        float APPT_W = 0f;
        if (showAppointmentButton) {
            APPT_W = TextMeasurer.measure(font, glyphLayout, apptBtnLabel, PAD_X, PAD_Y).width;
        }
        s.appointmentBtnX = btnX; s.appointmentBtnH = BTN_H; s.appointmentBtnW = showAppointmentButton ? APPT_W : 0f;
        s.appointmentBtnY = curRowBottom;
        if (showAppointmentButton) { lowestBtnBottom = curRowBottom; curRowBottom -= BTN_H + BTN_SPACING; }

        // Service buttons – shown when the player is standing at a discovered building that
        // has services (and is not heading elsewhere).
        java.util.List<BuildingService> svcList =
                (atCurrentBuilding && selBuilding != null)
                        ? BuildingServices.getServices(selBuilding)
                        : java.util.Collections.<BuildingService>emptyList();
        s.svcBtnCount = 0;
        s.svcBtnH = BTN_H;
        for (int si = 0; si < svcList.size() && si < MapViewState.MAX_SVC_BTNS; si++) {
            String svcLabel = svcList.get(si).menuLabel();
            float svcW = TextMeasurer.measure(font, glyphLayout, svcLabel, PAD_X, PAD_Y).width;
            s.svcBtnX[si] = btnX;
            s.svcBtnY[si] = curRowBottom;
            s.svcBtnW[si] = svcW;
            lowestBtnBottom = curRowBottom;
            curRowBottom -= BTN_H + BTN_SPACING;
            s.svcBtnCount++;
        }

        boolean hasButton = showMoveToButton || showLookAroundButton || showOfficeButton || showHotelRoomButton || showStashButton || showCheckEmailsButton || showAppointmentButton || s.svcBtnCount > 0;

        // --- Content area ---
        final float SB = MapViewState.SCROLLBAR_THICKNESS;
        float contentStartY = hasButton
                ? lowestBtnBottom - fontLineH - fontLineH
                : panelH - fontLineH;
        float contentAreaBottom = SB;
        float contentAreaH      = contentStartY - contentAreaBottom;
        float contentAreaW      = s.screenWidth - SB;

        // --- Measure content and update max-scroll bounds ---
        final float TEXT_X = 20f;
        float wrapWidth = contentAreaW - TEXT_X;
        float totalContentH = computeContentHeight(s, fontLineH, smallLineH, wrapWidth);
        float totalContentW = computeContentWidth(s, TEXT_X);
        s.infoMaxScrollY = Math.max(0f, totalContentH - contentAreaH);
        s.infoMaxScrollX = Math.max(0f, totalContentW - contentAreaW);
        s.infoScrollY = MathUtils.clamp(s.infoScrollY, 0f, s.infoMaxScrollY);
        s.infoScrollX = MathUtils.clamp(s.infoScrollX, 0f, s.infoMaxScrollX);

        // --- Shapes (buttons) ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
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
        if (showHotelRoomButton) {
            shapeRenderer.setColor(OFFICE_BTN_COLOR);
            shapeRenderer.rect(s.goToHotelRoomBtnX, s.goToHotelRoomBtnY, HOTEL_ROOM_W, BTN_H);
        }
        if (showAppointmentButton) {
            shapeRenderer.setColor(APPOINTMENT_BTN_COLOR);
            shapeRenderer.rect(s.appointmentBtnX, s.appointmentBtnY, APPT_W, BTN_H);
        }
        for (int si = 0; si < s.svcBtnCount; si++) {
            shapeRenderer.setColor(SERVICE_BTN_COLOR);
            shapeRenderer.rect(s.svcBtnX[si], s.svcBtnY[si], s.svcBtnW[si], BTN_H);
        }

        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(INFO_BORDER_COLOR);
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
        if (showHotelRoomButton) {
            shapeRenderer.rect(s.goToHotelRoomBtnX,     s.goToHotelRoomBtnY,     HOTEL_ROOM_W,     BTN_H);
            shapeRenderer.rect(s.goToHotelRoomBtnX + 1, s.goToHotelRoomBtnY + 1, HOTEL_ROOM_W - 2, BTN_H - 2);
        }
        if (showAppointmentButton) {
            shapeRenderer.rect(s.appointmentBtnX,     s.appointmentBtnY,     APPT_W,     BTN_H);
            shapeRenderer.rect(s.appointmentBtnX + 1, s.appointmentBtnY + 1, APPT_W - 2, BTN_H - 2);
        }
        for (int si = 0; si < s.svcBtnCount; si++) {
            shapeRenderer.rect(s.svcBtnX[si],     s.svcBtnY[si],     s.svcBtnW[si],     BTN_H);
            shapeRenderer.rect(s.svcBtnX[si] + 1, s.svcBtnY[si] + 1, s.svcBtnW[si] - 2, BTN_H - 2);
        }

        shapeRenderer.end();
        batch.begin();
        float textX = 20f;

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
        if (showHotelRoomButton) {
            glyphLayout.setText(font, hotelRoomBtnLabel);
            font.setColor(Color.WHITE);
            font.draw(batch, hotelRoomBtnLabel,
                    s.goToHotelRoomBtnX + (HOTEL_ROOM_W - glyphLayout.width) / 2,
                    s.goToHotelRoomBtnY + (BTN_H + glyphLayout.height) / 2);
        }
        if (showAppointmentButton) {
            glyphLayout.setText(font, apptBtnLabel);
            font.setColor(Color.WHITE);
            font.draw(batch, apptBtnLabel,
                    s.appointmentBtnX + (APPT_W - glyphLayout.width) / 2,
                    s.appointmentBtnY + (BTN_H + glyphLayout.height) / 2);
        }
        for (int si = 0; si < s.svcBtnCount && si < svcList.size(); si++) {
            String svcLabel = svcList.get(si).menuLabel();
            glyphLayout.setText(font, svcLabel);
            font.setColor(Color.WHITE);
            font.draw(batch, svcLabel,
                    s.svcBtnX[si] + (s.svcBtnW[si] - glyphLayout.width) / 2,
                    s.svcBtnY[si] + (BTN_H + glyphLayout.height) / 2);
        }


        // GL scissor
        batch.flush();
        float scissorTop = hasButton ? lowestBtnBottom : panelH;
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, (int) contentAreaBottom,
                (int) contentAreaW, Math.max(0, (int)(scissorTop - contentAreaBottom)));

        drawScrollX = s.infoScrollX;
        drawScrollY = s.infoScrollY;

        float textY = contentStartY;
        if (s.selectedCellX >= 0 && s.selectedCellY >= 0) {
            Cell cell = cityMap.getCell(s.selectedCellX, s.selectedCellY);
            textY = drawLabelValue(font, "Terrain: ",
                    cell.getTerrainType().getDisplayName(), textX, textY);
            textY -= fontLineH;

            if (cell.hasBuilding()) {
                Building building = cell.getBuilding();
                if (building.isDiscovered()) {
                    String bMod = formatAttributeModifiers(building.getAttributeModifiers());
                    float dx = textX - drawScrollX;
                    float dy = textY + drawScrollY;
                    font.setColor(LABEL_COLOR);
                    font.draw(batch, "Building: ", dx, dy);
                    glyphLayout.setText(font, "Building: ");
                    float nameX = dx + glyphLayout.width;
                    String bName = building.getDisplayName();
                    smallFont.setColor(Color.WHITE);
                    smallFont.draw(batch, bName, nameX, dy - valCenterOff);
                    if (!bMod.isEmpty()) {
                        glyphLayout.setText(smallFont, bName);
                        tinyFont.setColor(Color.WHITE);
                        tinyFont.draw(batch, " " + formatAttributeModifiersMarkup(building.getAttributeModifiers()),
                                nameX + glyphLayout.width, dy - valCenterOff - valSmallTinyOff);
                    }
                    textY -= fontLineH;

                    if (building.getDefinition() != null) {
                        textY = drawLabelValue(font, "Type: ",
                                building.getName(), textX, textY);
                        textY -= fontLineH;
                        textY = drawLabelValue(font, "Floors: ",
                                String.valueOf(building.getFloors()), textX, textY);
                        textY -= fontLineH;
                    }

                    // Multi-tenant: show all company names
                    List<String> tenants = building.getTenants();
                    if (tenants.size() > 1) {
                        font.setColor(LABEL_COLOR);
                        font.draw(batch, "Tenants:", textX - drawScrollX, textY + drawScrollY);
                        textY -= fontLineH;
                        for (String tenant : tenants) {
                            float tdy = textY + drawScrollY;
                            if (tdy < contentAreaBottom - fontLineH) break;
                            smallFont.setColor(Color.WHITE);
                            smallFont.draw(batch, "  " + tenant, textX - drawScrollX, tdy);
                            textY -= smallLineH;
                        }
                    }

                    font.setColor(LABEL_COLOR);
                    font.draw(batch, "Improvements:", textX - drawScrollX, textY + drawScrollY);
                    textY -= fontLineH;
                    for (Improvement imp : building.getImprovements()) {
                        float idy = textY + drawScrollY;
                        if (idy < contentAreaBottom - fontLineH) break;
                        float idx = textX - drawScrollX;
                        if (imp.isDiscovered()) {
                            String modStr = formatAttributeModifiers(imp.getAttributeModifiers());
                            String namePart = "  - " + imp.getName();
                            font.setColor(Color.WHITE);
                            font.draw(batch, namePart, idx, idy);
                            if (!modStr.isEmpty()) {
                                glyphLayout.setText(font, namePart);
                                tinyFont.setColor(Color.WHITE);
                                tinyFont.draw(batch, " " + formatAttributeModifiersMarkup(imp.getAttributeModifiers()),
                                        idx + glyphLayout.width, idy - valTinyBottomOff);
                            }
                            textY -= fontLineH;
                        } else {
                            font.setColor(Color.WHITE);
                            font.draw(batch, "  - ???", idx, idy);
                            textY -= fontLineH;
                        }
                    }

                    // Building description from description_en.json (word-wrapped, novel colour)
                    List<String> novelLines = buildingNovelLines(building, contentAreaW - textX);
                    if (!novelLines.isEmpty()) {
                        textY -= smallLineH;
                        smallFont.setColor(NOVEL_COLOR);
                        for (String nLine : novelLines) {
                            smallFont.draw(batch, nLine, textX - drawScrollX, textY + drawScrollY);
                            textY -= smallLineH;
                        }
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

        batch.flush();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        drawScrollX = 0f;
        drawScrollY = 0f;

        // Zoom text (top-right of content area)
        if (s.zoomLevel != s.lastZoomLevel) {
            s.cachedZoomText = "Zoom: " + String.format("%.1fx", s.zoomLevel);
            s.lastZoomLevel = s.zoomLevel;
        }
        glyphLayout.setText(smallFont, s.cachedZoomText);
        smallFont.setColor(Color.WHITE);
        smallFont.draw(batch, s.cachedZoomText,
                s.screenWidth - glyphLayout.width - 20,
                panelH - (glyphLayout.height * 1.4f));

        batch.end();

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
    // Character tab content
    // -------------------------------------------------------------------------

    private void drawCharacterTab(MapViewState s, int panelH) {
        // Disable action buttons when character tab is active
        s.moveToButtonW      = 0f;
        s.lookAroundBtnW     = 0f;
        s.restBtnW           = 0f;
        s.sleepBtnW          = 0f;
        s.goToOfficeBtnW     = 0f;
        s.goToHotelRoomBtnW  = 0f;
        s.openStashBtnW      = 0f;
        s.checkEmailsBtnW    = 0f;
        s.addNoteBtnW        = 0f;
        s.noteTimeCbW        = 0f;
        s.noteLocCbW         = 0f;
        s.infoMaxScrollX     = 0f;
        s.infoScrollX        = 0f;

        glyphLayout.setText(font, "Hg");
        float fontCapH  = glyphLayout.height;
        float fontLineH = fontCapH * 1.4f;
        glyphLayout.setText(smallFont, "Hg");
        float smallCapH  = glyphLayout.height;
        float smallLineH = smallCapH * 1.4f;

        final float SB  = MapViewState.SCROLLBAR_THICKNESS;
        final float PAD = 16f;

        CharacterAttribute[] attrs = CharacterAttribute.values();
        EquipmentSlot[] mainSlots = { EquipmentSlot.WEAPON, EquipmentSlot.BODY,
                                      EquipmentSlot.LEGS,   EquipmentSlot.FEET };

        // Count total carried items for equipment section height
        int equippedItemCount = 0;
        for (EquipmentSlot slot : mainSlots) {
            if (profile.getEquipped(slot) != null) equippedItemCount++;
        }
        equippedItemCount += profile.getUtilityItems().size();
        int equipRows = Math.max(1, equippedItemCount); // at least 1 for "(none)" row

        // Total virtual content height (dynamic, based on actual item count)
        float equipHeaderH = fontLineH;
        float totalH = fontLineH                            // character name
                + attrs.length * smallLineH                 // attributes
                + smallLineH                                // blank separator
                + equipHeaderH                              // "Equipment" header
                + equipRows * smallLineH                    // one row per carried item
                + smallLineH;                               // weight row
        float contentAreaH = panelH - SB;
        s.infoMaxScrollY = Math.max(0f, totalH - contentAreaH);
        s.infoScrollY    = MathUtils.clamp(s.infoScrollY, 0f, s.infoMaxScrollY);

        // Scissor to content area (full height; scrollbar track is drawn separately on the right)
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, 0, (int)(s.screenWidth - SB), Math.max(0, panelH));

        batch.begin();
        drawScrollY = s.infoScrollY;

        float ty = panelH - PAD; // virtual top

        // Character name
        font.setColor(Color.YELLOW);
        font.draw(batch, profile.getCharacterName(), PAD, ty + drawScrollY);
        ty -= fontLineH;

        // Attributes — centred-[total] layout:
        //   Name (right-aligned left of bracket)  |  [total] (centred)  |  base ±loc ±equip ±body (right of centre)
        float contentW  = s.screenWidth - SB;           // usable width (no scrollbar)
        float centerX   = contentW / 2f;                // X centre of the [total] token
        glyphLayout.setText(smallFont, " ");
        float spaceW = glyphLayout.width;               // measured once; reused per attribute row

        for (CharacterAttribute attr : attrs) {
            int base     = profile.getAttribute(attr.name());
            int locMod   = locationModFor(s.charCellX, s.charCellY, attr);
            int equipMod = profile.getEquipmentModifier(attr);
            int bodyMod  = (attr == CharacterAttribute.STRENGTH)
                           ? profile.getMuscleFatStrengthModifier() : 0;
            float ay = ty + drawScrollY;
            int    total    = base + locMod + equipMod + bodyMod;
            String totalStr = String.valueOf(total);

            // Measure "[total]" bracket token width for centering
            String bracketStr = "[" + totalStr + "]";
            glyphLayout.setText(smallFont, bracketStr);
            float bracketW  = glyphLayout.width;
            float bracketX  = centerX - bracketW / 2f;  // left edge of "[total]"
            float afterX    = centerX + bracketW / 2f;  // first pixel right of "]"

            // Attribute name — white, right-aligned immediately left of bracket (one space gap)
            String nameStr = attr.getDisplayName();
            glyphLayout.setText(smallFont, nameStr);
            float nameW = glyphLayout.width;
            float nameX = bracketX - spaceW - nameW;
            smallFont.setColor(Color.WHITE);
            smallFont.draw(batch, nameStr, nameX, ay);

            // "[" — white
            smallFont.setColor(Color.WHITE);
            smallFont.draw(batch, "[", bracketX, ay);
            glyphLayout.setText(smallFont, "[");
            float cx = bracketX + glyphLayout.width;

            // total value — bright green
            smallFont.setColor(ATTR_TOTAL_COLOR);
            smallFont.draw(batch, totalStr, cx, ay);
            glyphLayout.setText(smallFont, totalStr);
            cx += glyphLayout.width;

            // "]" — white
            smallFont.setColor(Color.WHITE);
            smallFont.draw(batch, "]", cx, ay);

            // Additions to the right of the bracket (start at afterX)
            cx = afterX;
            String baseStr = String.valueOf(base);
            boolean hasAdditions = locMod != 0 || equipMod != 0 || bodyMod != 0;
            if (hasAdditions) {
                // " base" — white (space before base, colon removed)
                String spaceBase = " " + baseStr;
                smallFont.setColor(Color.WHITE);
                smallFont.draw(batch, spaceBase, cx, ay);
                glyphLayout.setText(smallFont, spaceBase);
                cx += glyphLayout.width;
            }

            if (locMod != 0) {
                // " ±loc" — bright green (building / improvement bonus)
                String locStr = " " + (locMod > 0 ? "+" : "-") + Math.abs(locMod);
                smallFont.setColor(Color.GREEN);
                smallFont.draw(batch, locStr, cx, ay);
                glyphLayout.setText(smallFont, locStr);
                cx += glyphLayout.width;
            }

            if (equipMod != 0) {
                // " ±equip" — cyan
                String eqStr = " " + (equipMod > 0 ? "+" : "-") + Math.abs(equipMod);
                smallFont.setColor(Color.CYAN);
                smallFont.draw(batch, eqStr, cx, ay);
                glyphLayout.setText(smallFont, eqStr);
                cx += glyphLayout.width;
            }

            if (bodyMod != 0) {
                // " ±body" — yellow
                String bdStr = " " + (bodyMod > 0 ? "+" : "-") + Math.abs(bodyMod);
                smallFont.setColor(Color.YELLOW);
                smallFont.draw(batch, bdStr, cx, ay);
            }

            ty -= smallLineH;
        }

        // --- Equipment section ---
        ty -= smallLineH; // blank separator

        // "Equipment" header
        font.setColor(LABEL_COLOR);
        font.draw(batch, "Equipment", PAD, ty + drawScrollY);
        ty -= equipHeaderH;

        // Build flat list of all carried items (main slots first, then utility)
        List<EquipItem> allItems = new ArrayList<>();
        for (EquipmentSlot slot : mainSlots) {
            EquipItem item = profile.getEquipped(slot);
            if (item != null) allItems.add(item);
        }
        allItems.addAll(profile.getUtilityItems());

        // "Drop" vs "Stash" label: "Stash" when player is inside their home office
        boolean inOffice = s.unitInteriorOpen
                && s.charCellX == s.homeCellX && s.charCellY == s.homeCellY;
        String dropLabel = inOffice ? "[Stash]" : "[Drop]";
        Color  dropColor = inOffice ? new Color(1.0f, 0.65f, 0.2f, 1f)
                                    : new Color(1.0f, 0.35f, 0.35f, 1f);

        // Pre-measure the drop button label (same width for all rows)
        glyphLayout.setText(smallFont, dropLabel);
        float dropLabelW = glyphLayout.width;
        float dropLabelX = contentW - 8f - dropLabelW; // right-aligned, 8px from scrollbar edge

        // Clear old drop button bounds
        for (int i = 0; i < MapViewState.MAX_EQUIP_BTNS; i++) s.equipDropBtnW[i] = 0f;
        s.equipDropBtnH = smallCapH + 4f;

        if (allItems.isEmpty()) {
            smallFont.setColor(new Color(0.45f, 0.45f, 0.45f, 1f));
            smallFont.draw(batch, "(none)", PAD, ty + drawScrollY);
            ty -= smallLineH;
            s.equipDropBtnCount = 0;
        } else {
            s.equipDropBtnCount = allItems.size();
            for (int i = 0; i < allItems.size(); i++) {
                EquipItem item = allItems.get(i);
                float iy = ty + drawScrollY;
                float cx = PAD;

                // Item name — white
                smallFont.setColor(Color.WHITE);
                smallFont.draw(batch, item.getName(), cx, iy);
                glyphLayout.setText(smallFont, item.getName());
                cx += glyphLayout.width;

                // "  (Slot)" — dimmed
                String slotLabel = "  (" + item.getSlot().getDisplayName() + ")";
                smallFont.setColor(new Color(0.6f, 0.6f, 0.6f, 1f));
                smallFont.draw(batch, slotLabel, cx, iy);
                glyphLayout.setText(smallFont, slotLabel);
                cx += glyphLayout.width;

                // Attribute modifiers — cyan
                if (!item.getModifiers().isEmpty()) {
                    StringBuilder modBuf = new StringBuilder();
                    for (Map.Entry<CharacterAttribute, Integer> e :
                            item.getModifiers().entrySet()) {
                        if (modBuf.length() > 0) modBuf.append(' ');
                        int v = e.getValue();
                        modBuf.append(v > 0 ? '+' : '-').append(Math.abs(v))
                              .append(' ').append(e.getKey().getDisplayName());
                    }
                    smallFont.setColor(Color.CYAN);
                    smallFont.draw(batch, "  " + modBuf, cx, iy);
                }

                // [Drop] / [Stash] button — right-aligned, only when visible in panel
                if (i < MapViewState.MAX_EQUIP_BTNS && iy > 0 && iy <= panelH + smallCapH) {
                    if (item.isCaseItem()) {
                        // Case items: show a locked [Case] label — no drop button
                        smallFont.setColor(Color.YELLOW);
                        smallFont.draw(batch, "[Case]", dropLabelX, iy);
                        // equipDropBtnW[i] stays 0 (already cleared above)
                    } else {
                        smallFont.setColor(dropColor);
                        smallFont.draw(batch, dropLabel, dropLabelX, iy);
                        s.equipDropBtnX[i] = dropLabelX;
                        s.equipDropBtnY[i] = iy - smallCapH - 2f; // button bottom
                        s.equipDropBtnW[i] = dropLabelW;
                    }
                } // else W stays 0 (set above) — not clickable when scrolled off-screen

                ty -= smallLineH;
            }
        }

        // Weight row  "Weight: 2.0 / 5.0"  — green / orange / red depending on load
        float carried  = profile.getTotalCarriedWeight();
        float capacity = profile.getWeightCapacity();
        float wy = ty + drawScrollY;
        smallFont.setColor(LABEL_COLOR);
        smallFont.draw(batch, "Weight: ", PAD, wy);
        glyphLayout.setText(smallFont, "Weight: ");
        String weightStr = String.format("%.1f / %.1f kg", carried, capacity);
        Color weightColor;
        if (carried > capacity) {
            weightColor = Color.RED;
        } else if (carried > capacity * 0.8f) {
            weightColor = Color.ORANGE;
        } else {
            weightColor = Color.GREEN;
        }
        smallFont.setColor(weightColor);
        smallFont.draw(batch, weightStr, PAD + glyphLayout.width, wy);
        ty -= smallLineH;

        batch.end();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        drawScrollY = 0f;

        // Scrollbar — always draw track; thumb only when scrollable
        float sbX    = s.screenWidth - SB;
        float trackH = contentAreaH;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 1f);
        shapeRenderer.rect(sbX, SB, SB, trackH);
        if (s.infoMaxScrollY > 0f) {
            float thumbH      = Math.max(SB * 2f, trackH * trackH / totalH);
            float scrollRatio = s.infoScrollY / s.infoMaxScrollY;
            float thumbY      = SB + (1f - scrollRatio) * (trackH - thumbH);
            shapeRenderer.setColor(0.5f, 0.5f, 0.7f, 1f);
            shapeRenderer.rect(sbX, thumbY, SB, thumbH);
        }
        shapeRenderer.end();
    }

    // -------------------------------------------------------------------------
    // Appointment helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the first calendar entry whose {@code location} matches the
     * building the character is currently standing in, AND whose date/time is
     * between now and 3 hours (180 minutes) in the future.
     *
     * <p>Special case: a calendar location of {@code "Your Office"} matches
     * whenever the character is at {@code homeCellX / homeCellY}.
     *
     * @return the matching entry, or {@code null} if none qualifies
     */
    private CalendarEntry findUpcomingAppointmentAtLocation(MapViewState s) {
        if (s.charCellX < 0 || s.charCellY < 0) return null;

        Cell cell = cityMap.getCell(s.charCellX, s.charCellY);
        String buildingName = (cell != null && cell.hasBuilding()
                && cell.getBuilding().isDiscovered())
                ? cell.getBuilding().getName() : null;
        boolean atHome = s.charCellX == s.homeCellX && s.charCellY == s.homeCellY;

        if (buildingName == null && !atHome) return null;

        long nowMinutes = dateTimeToMinutes(profile.getGameDateTime());
        for (CalendarEntry entry : profile.getCalendarEntries()) {
            boolean locationMatches;
            if ("Your Office".equalsIgnoreCase(entry.location)) {
                locationMatches = atHome;
            } else {
                locationMatches = buildingName != null
                        && buildingName.equalsIgnoreCase(entry.location);
            }
            if (!locationMatches) continue;

            // Show when the appointment is between now and 3 hours in the future
            long diff = dateTimeToMinutes(entry.dateTime) - nowMinutes;
            if (diff >= 0 && diff <= 180) return entry;
        }
        return null;
    }

    /**
     * Converts a {@code "YYYY-MM-DD HH:MM"} game date/time to total minutes
     * using the standard 365-day year with per-month day counts.
     * Returns {@link Long#MAX_VALUE} / 2 on malformed input so that a bad
     * date can never accidentally appear within the 3-hour appointment window.
     */
    private static long dateTimeToMinutes(String dt) {
        final int[] MONTH_DAYS = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        try {
            String[] halves = dt.split(" ");
            String[] d = halves[0].split("-");
            String[] t = halves[1].split(":");
            int year  = Integer.parseInt(d[0]);
            int month = Integer.parseInt(d[1]);
            int day   = Integer.parseInt(d[2]);
            int hour  = Integer.parseInt(t[0]);
            int min   = Integer.parseInt(t[1]);
            long totalDays = (long)(year - 2050) * 365L;
            for (int m = 1; m < month; m++) totalDays += MONTH_DAYS[m - 1];
            totalDays += day;
            return totalDays * 24L * 60L + hour * 60L + min;
        } catch (Exception e) {
            return Long.MAX_VALUE / 2;
        }
    }

    // -------------------------------------------------------------------------
    // Calendar tab content
    // -------------------------------------------------------------------------

    private void drawCalendarTab(MapViewState s, int panelH) {
        // Disable action buttons when calendar tab is active
        s.moveToButtonW = 0f;
        s.lookAroundBtnW = 0f;
        s.restBtnW = 0f;
        s.sleepBtnW = 0f;
        s.goToOfficeBtnW = 0f;
        s.goToHotelRoomBtnW = 0f;
        s.openStashBtnW = 0f;
        s.checkEmailsBtnW = 0f;
        s.infoMaxScrollX = 0f;
        s.infoScrollX = 0f;

        glyphLayout.setText(font, "Hg");
        float fontCapH  = glyphLayout.height;
        float fontLineH = fontCapH * 1.4f;
        glyphLayout.setText(smallFont, "Hg");
        float smallCapH = glyphLayout.height;
        float smallLineH = smallCapH * 1.4f;

        final float SB = MapViewState.SCROLLBAR_THICKNESS;
        final float PAD = 16f;

        java.util.List<CalendarEntry> entries = profile.getCalendarEntries();

        // Compute virtual height
        float totalH = fontLineH;  // "Calendar" header
        if (entries.isEmpty()) {
            totalH += smallLineH;
        } else {
            for (CalendarEntry e : entries) {
                totalH += smallLineH + fontLineH + smallLineH; // date + title + location
                if (e.rewardMoney > 0 || e.rewardItemName != null) totalH += smallLineH;
                totalH += smallLineH * 0.4f; // entry gap
            }
        }

        float contentAreaH = panelH - SB;
        s.infoMaxScrollY = Math.max(0f, totalH - contentAreaH);
        s.infoScrollY = MathUtils.clamp(s.infoScrollY, 0f, s.infoMaxScrollY);

        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, 0, (int) (s.screenWidth - SB), Math.max(0, panelH));

        batch.begin();
        drawScrollY = s.infoScrollY;

        float ty = panelH - PAD;

        font.setColor(LABEL_COLOR);
        font.draw(batch, "Calendar", PAD, ty + drawScrollY);
        ty -= fontLineH;

        if (entries.isEmpty()) {
            smallFont.setColor(new Color(0.45f, 0.45f, 0.45f, 1f));
            smallFont.draw(batch, "(no appointments)", PAD, ty + drawScrollY);
        } else {
            for (CalendarEntry entry : entries) {
                // Date/time
                smallFont.setColor(new Color(0.50f, 0.80f, 1.00f, 1f));
                smallFont.draw(batch, entry.dateTime, PAD, ty + drawScrollY);
                ty -= smallLineH;
                // Title
                font.setColor(Color.WHITE);
                font.draw(batch, entry.title, PAD + 8f, ty + drawScrollY);
                ty -= fontLineH;
                // Location + cell + travel time
                smallFont.setColor(new Color(0.65f, 0.65f, 0.65f, 1f));
                String locLine = entry.location;
                if (entry.locationCellX >= 0 && entry.locationCellY >= 0) {
                    CityMap.RouteResult route = cityMap.findFastestRoute(
                            s.charCellX, s.charCellY,
                            entry.locationCellX, entry.locationCellY);
                    String travelStr = route.formatTime(); // returns "Unreachable" when not reachable
                    locLine += "  (" + MapRenderer.cellLabel(entry.locationCellX, entry.locationCellY)
                             + " \u00b7 " + travelStr + ")";
                }
                smallFont.draw(batch, locLine, PAD + 8f, ty + drawScrollY);
                ty -= smallLineH;
                // Reward (if any)
                if (entry.rewardMoney > 0 || entry.rewardItemName != null) {
                    String rewardTxt;
                    if (entry.rewardMoney > 0 && entry.rewardItemName != null)
                        rewardTxt = "Reward: $" + entry.rewardMoney + " + " + entry.rewardItemName;
                    else if (entry.rewardMoney > 0)
                        rewardTxt = "Reward: $" + entry.rewardMoney;
                    else
                        rewardTxt = "Reward: " + entry.rewardItemName;
                    smallFont.setColor(new Color(1.00f, 0.85f, 0.20f, 1f));
                    smallFont.draw(batch, rewardTxt, PAD + 8f, ty + drawScrollY);
                    ty -= smallLineH;
                }
                ty -= smallLineH * 0.4f; // entry spacing
            }
        }

        batch.end();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        drawScrollY = 0f;

        // Vertical scrollbar
        float sbX = s.screenWidth - SB;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(SCROLLBAR_TRACK_COLOR);
        shapeRenderer.rect(sbX, SB, SB, contentAreaH);
        if (s.infoMaxScrollY > 0f) {
            float thumbH = Math.max(SB * 2f, contentAreaH * contentAreaH / totalH);
            float scrollRatio = s.infoScrollY / s.infoMaxScrollY;
            float thumbY = SB + (1f - scrollRatio) * (contentAreaH - thumbH);
            shapeRenderer.setColor(SCROLLBAR_THUMB_COLOR);
            shapeRenderer.rect(sbX, thumbY, SB, thumbH);
        }
        shapeRenderer.end();
    }


    // Case File tab content
    // -------------------------------------------------------------------------

    private void drawCaseFileTab(MapViewState s, int panelH) {
        // Disable action buttons when case file tab is active
        s.moveToButtonW      = 0f;
        s.lookAroundBtnW     = 0f;
        s.restBtnW           = 0f;
        s.sleepBtnW          = 0f;
        s.goToOfficeBtnW     = 0f;
        s.goToHotelRoomBtnW  = 0f;
        s.infoMaxScrollX     = 0f;
        s.infoScrollX        = 0f;
        s.addNoteBtnW        = 0f;
        s.noteTimeCbW        = 0f;
        s.noteLocCbW         = 0f;

        final float PAD = 12f;

        glyphLayout.setText(font, "Hg");
        float fontCapH  = glyphLayout.height;
        float fontLineH = fontCapH * 1.4f;

        // --- Poplist header: open cases ---
        List<CaseFile> openCases = profile.getOpenCases();
        String selectedCase;
        if (openCases.isEmpty()) {
            selectedCase = "None";
        } else {
            CaseFile active = profile.getActiveCaseFile();
            selectedCase = active != null ? active.getName() : openCases.get(0).getName();
        }

        // Draw poplist-style selector at the top
        float poplistY = panelH - PAD;
        float poplistH = fontCapH + 10f;
        float poplistW = s.screenWidth - PAD * 2f;
        float poplistX = PAD;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 1f);
        shapeRenderer.rect(poplistX, poplistY - poplistH, poplistW, poplistH);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(INFO_BORDER_COLOR);
        shapeRenderer.rect(poplistX, poplistY - poplistH, poplistW, poplistH);
        shapeRenderer.end();

        batch.begin();
        font.setColor(LABEL_COLOR);
        font.draw(batch, "Case: ", poplistX + 6f, poplistY - 4f);
        glyphLayout.setText(font, "Case: ");
        float labelW = glyphLayout.width;
        font.setColor(Color.WHITE);
        font.draw(batch, selectedCase, poplistX + 6f + labelW, poplistY - 4f);
        batch.end();

        // --- Case details below the poplist ---
        float contentY = poplistY - poplistH - PAD;

        if (!openCases.isEmpty()) {
            CaseFile active = profile.getActiveCaseFile();
            if (active == null) active = openCases.get(0);

            batch.begin();
            font.setColor(LABEL_COLOR);
            font.draw(batch, "Status: ", PAD, contentY);
            glyphLayout.setText(font, "Status: ");
            font.setColor(Color.WHITE);
            font.draw(batch, active.getStatus().name(), PAD + glyphLayout.width, contentY);
            contentY -= fontLineH;

            font.setColor(LABEL_COLOR);
            font.draw(batch, "Opened: ", PAD, contentY);
            glyphLayout.setText(font, "Opened: ");
            font.setColor(Color.WHITE);
            font.draw(batch, active.getDateOpened(), PAD + glyphLayout.width, contentY);
            contentY -= fontLineH;

            if (!active.getDescription().isEmpty()) {
                font.setColor(LABEL_COLOR);
                font.draw(batch, "Description:", PAD, contentY);
                contentY -= fontLineH;
                smallFont.setColor(NOVEL_COLOR);
                smallFont.draw(batch, active.getDescription(), PAD + 8f, contentY);
                smallFont.setColor(Color.WHITE);
                contentY -= fontLineH;
            }

            // Clue count
            font.setColor(LABEL_COLOR);
            font.draw(batch, "Clues: ", PAD, contentY);
            glyphLayout.setText(font, "Clues: ");
            font.setColor(Color.WHITE);
            font.draw(batch, String.valueOf(active.getClues().size()), PAD + glyphLayout.width, contentY);
            contentY -= fontLineH;

            // List clues
            List<String> clues = active.getClues();
            for (int i = 0; i < clues.size(); i++) {
                smallFont.setColor(Color.WHITE);
                smallFont.draw(batch, "\u2022 " + clues.get(i), PAD + 8f, contentY);
                contentY -= fontLineH * 0.9f;
            }

            // Evidence count
            font.setColor(LABEL_COLOR);
            font.draw(batch, "Evidence: ", PAD, contentY);
            glyphLayout.setText(font, "Evidence: ");
            font.setColor(Color.WHITE);
            font.draw(batch, String.valueOf(active.getEvidence().size()), PAD + glyphLayout.width, contentY);
            contentY -= fontLineH;

            // List evidence
            List<String> evidenceList = active.getEvidence();
            for (int i = 0; i < evidenceList.size(); i++) {
                smallFont.setColor(Color.WHITE);
                smallFont.draw(batch, "\u2022 " + evidenceList.get(i), PAD + 8f, contentY);
                contentY -= fontLineH * 0.9f;
            }

            // Notes count
            font.setColor(LABEL_COLOR);
            font.draw(batch, "Notes: ", PAD, contentY);
            glyphLayout.setText(font, "Notes: ");
            font.setColor(Color.WHITE);
            font.draw(batch, String.valueOf(active.getNotes().size()), PAD + glyphLayout.width, contentY);
            contentY -= fontLineH;

            // List notes
            List<String> notesList = active.getNotes();
            for (int i = 0; i < notesList.size(); i++) {
                noteFont.setColor(NOTE_COLOR);
                noteFont.draw(batch, "\u2022 " + notesList.get(i), PAD + 8f, contentY);
                contentY -= fontLineH * 0.9f;
            }
            noteFont.setColor(Color.WHITE);

            batch.end();

            // --- Checkboxes: Include current time / Include current location ---
            final float CB_LABEL_GAP   = 6f;   // gap between checkbox square and label text
            final float CB_ROW_PADDING = 4f;   // vertical padding around each checkbox row
            float cbSize = fontCapH;
            float cbRowH = cbSize + CB_ROW_PADDING;

            // "Include current time" checkbox
            float timeCbX = PAD;
            float timeCbY = contentY - cbSize - 2f;

            s.noteTimeCbX = timeCbX;
            s.noteTimeCbY = timeCbY;
            s.noteTimeCbW = cbSize;
            s.noteTimeCbH = cbSize;

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(INFO_BORDER_COLOR);
            shapeRenderer.rect(timeCbX, timeCbY, cbSize, cbSize);
            shapeRenderer.end();

            if (s.noteIncludeTime) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(LABEL_COLOR);
                float inset = 3f;
                shapeRenderer.rect(timeCbX + inset, timeCbY + inset,
                        cbSize - inset * 2, cbSize - inset * 2);
                shapeRenderer.end();
            }

            batch.begin();
            smallFont.setColor(Color.WHITE);
            smallFont.draw(batch, "Include current time",
                    timeCbX + cbSize + CB_LABEL_GAP, timeCbY + cbSize - 2f);
            batch.end();

            contentY -= cbRowH;

            // "Include current location" checkbox
            float locCbX = PAD;
            float locCbY = contentY - cbSize - 2f;

            s.noteLocCbX = locCbX;
            s.noteLocCbY = locCbY;
            s.noteLocCbW = cbSize;
            s.noteLocCbH = cbSize;

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(INFO_BORDER_COLOR);
            shapeRenderer.rect(locCbX, locCbY, cbSize, cbSize);
            shapeRenderer.end();

            if (s.noteIncludeLocation) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(LABEL_COLOR);
                float inset = 3f;
                shapeRenderer.rect(locCbX + inset, locCbY + inset,
                        cbSize - inset * 2, cbSize - inset * 2);
                shapeRenderer.end();
            }

            batch.begin();
            smallFont.setColor(Color.WHITE);
            smallFont.draw(batch, "Include current location",
                    locCbX + cbSize + CB_LABEL_GAP, locCbY + cbSize - 2f);
            batch.end();

            contentY -= cbRowH;

            // --- "Add Note" button ---
            float btnW = 120f;
            float btnH = fontCapH + 12f;
            float btnX = PAD;
            float btnY = contentY - btnH - 4f;

            s.addNoteBtnX = btnX;
            s.addNoteBtnY = btnY;
            s.addNoteBtnW = btnW;
            s.addNoteBtnH = btnH;

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(ADD_NOTE_BTN_COLOR);
            shapeRenderer.rect(btnX, btnY, btnW, btnH);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(INFO_BORDER_COLOR);
            shapeRenderer.rect(btnX, btnY, btnW, btnH);
            shapeRenderer.end();

            batch.begin();
            font.setColor(Color.WHITE);
            glyphLayout.setText(font, "Add Note");
            font.draw(batch, "Add Note",
                    btnX + (btnW - glyphLayout.width) / 2f,
                    btnY + (btnH + glyphLayout.height) / 2f);
            batch.end();
        } else {
            batch.begin();
            smallFont.setColor(new Color(0.5f, 0.5f, 0.6f, 1f));
            smallFont.draw(batch, "No open cases.", PAD, contentY);
            smallFont.setColor(Color.WHITE);
            batch.end();
        }

        // Scroll — set max scroll to 0 for now (content fits without scroll initially)
        s.infoMaxScrollY = 0f;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the total attribute modifier contributed by the building (and all discovered
     * improvements) at the character's current cell, or 0 if no building/undiscovered.
     */
    private int locationModFor(int charCellX, int charCellY, CharacterAttribute attr) {
        if (charCellX < 0 || charCellY < 0) return 0;
        Cell cell = cityMap.getCell(charCellX, charCellY);
        if (!cell.hasBuilding() || !cell.getBuilding().isDiscovered()) return 0;
        Building b = cell.getBuilding();
        int total = b.getAttributeModifiers().getOrDefault(attr, 0);
        for (Improvement imp : b.getImprovements()) {
            if (imp.isDiscovered()) {
                total += imp.getAttributeModifiers().getOrDefault(attr, 0);
            }
        }
        return total;
    }

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
     * Returns the building description from {@code description_en.json} via the novel text engine,
     * contextualised to the current time of day, the character's attributes and gender.
     * When the building has a state ("good", "normal", "bad"), the state-specific description
     * is preferred; otherwise the default contextual description is used.
     * Returns {@code null} if no description is available for this building.
     */
    private String buildingNovelText(Building b) {
        if (novelTextEngine == null || b.getDefinition() == null) return null;
        String key   = b.getDefinition().getId();
        String state = b.getState();
        String text;
        if (state != null) {
            text = novelTextEngine.getStateDescription(key, state);
            // Fall back to time/attribute/gender-aware description if no state variant found
            if (text == null || text.isEmpty()) {
                text = novelTextEngine.getDescription(
                        key, profile.getCurrentHour(), profile.getAttributes(), profile.getGender());
            }
        } else {
            text = novelTextEngine.getDescription(
                    key, profile.getCurrentHour(), profile.getAttributes(), profile.getGender());
        }
        return (text != null && !text.isEmpty()) ? text : null;
    }

    /**
     * Returns the building description from {@code description_en.json}, word-wrapped to
     * {@code wrapWidth} pixels. Returns an empty list when no description is available.
     */
    private List<String> buildingNovelLines(Building b, float wrapWidth) {
        String novel = buildingNovelText(b);
        if (novel == null) return java.util.Collections.emptyList();
        return WordWrapper.wrap(novel, wrapWidth, t -> {
            glyphLayout.setText(font, t);
            return glyphLayout.width;
        });
    }

    /**
     * Computes total virtual height of the content section (sum of all line advances).
     * Used to determine whether vertical scrolling is needed.
     *
     * @param fontLineH  line advance for the regular font
     * @param smallLineH line advance for the smaller font (used by descriptions and novel text)
     * @param wrapWidth  pixel width at which novel text should be wrapped
     */
    private float computeContentHeight(MapViewState s, float fontLineH, float smallLineH,
                                       float wrapWidth) {
        if (s.selectedCellX < 0) return fontLineH; // "Click on a cell…"
        Cell cell = cityMap.getCell(s.selectedCellX, s.selectedCellY);
        float h = fontLineH; // Terrain (advance)
        if (!cell.hasBuilding()) return h;
        Building b = cell.getBuilding();
        if (!b.isDiscovered()) return h + fontLineH; // Building: ??? (last line)
        h += fontLineH; // Building (advance)
        if (b.getDefinition() != null) h += fontLineH * 2; // Type + Floors
        // Multi-tenant: Tenants header + one line per extra tenant
        List<String> tenants = b.getTenants();
        if (tenants.size() > 1) {
            h += fontLineH;               // "Tenants:" header
            h += smallLineH * tenants.size();
        }
        h += fontLineH; // "Improvements:" header (advance)
        for (Improvement imp : b.getImprovements()) {
            h += fontLineH; // improvement name row
        }
        int novelLineCount = buildingNovelLines(b, wrapWidth).size();
        if (novelLineCount > 0) {
            h += smallLineH * (1 + novelLineCount); // blank gap + description lines
        }
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
                glyphLayout.setText(smallFont, b.getDisplayName());
                float nameW = glyphLayout.width;
                if (!bMod.isEmpty()) {
                    glyphLayout.setText(tinyFont, " " + bMod);
                    nameW += glyphLayout.width;
                }
                maxW = Math.max(maxW, labW + nameW);
                if (b.getDefinition() != null) {
                    glyphLayout.setText(font, "Type: " + b.getName());
                    maxW = Math.max(maxW, glyphLayout.width);
                    glyphLayout.setText(font, "Floors: " + b.getFloors());
                    maxW = Math.max(maxW, glyphLayout.width);
                }
                // Multi-tenant width
                List<String> tenants = b.getTenants();
                if (tenants.size() > 1) {
                    for (String tenant : tenants) {
                        glyphLayout.setText(smallFont, "  " + tenant);
                        maxW = Math.max(maxW, glyphLayout.width);
                    }
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
                // Novel/description text is always word-wrapped to the content area width,
                // so it never contributes to horizontal scroll.
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

    /**
     * Same as {@link #formatAttributeModifiers} but wraps each numeric value in a
     * LibGDX colour-markup tag so it renders in bright green when drawn with a
     * {@link com.badlogic.gdx.graphics.g2d.BitmapFont} that has
     * {@code markupEnabled = true}.
     *
     * <p>Example output: {@code [[STR[#00EE44FF]+2[]]}</p>
     */
    static String formatAttributeModifiersMarkup(Map<CharacterAttribute, Integer> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("[["); // "[" rendered as "[[" in markup
        boolean first = true;
        for (Map.Entry<CharacterAttribute, Integer> e : modifiers.entrySet()) {
            if (!first) sb.append(' ');
            first = false;
            String name = e.getKey().getDisplayName();
            String abbr = name.length() >= 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase();
            int val = e.getValue();
            sb.append(abbr);
            // Colour only the numeric part in bright green
            sb.append("[#00EE44FF]");
            if (val > 0) sb.append('+');
            sb.append(val);
            sb.append("[]"); // reset colour
        }
        sb.append(']');
        return sb.toString();
    }

}

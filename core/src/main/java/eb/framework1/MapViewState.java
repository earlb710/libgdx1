package eb.framework1;

import com.badlogic.gdx.math.MathUtils;

/**
 * Holds all mutable map-view state shared between MainScreen and the rendering helpers.
 * Also provides the geometry helper methods (getCellSize, getVisibleCells, clampMapOffset)
 * so that both the screen logic and renderers compute layout consistently.
 */
class MapViewState {

    // --- Constants (layout) ---
    static final float RULER_WIDTH  = 45f;
    static final float RULER_GAP    = 1f;
    static final int   INFO_BAR_HEIGHT = 70;
    static final float DEFAULT_INFO_PANEL_RATIO = 0.33f;
    static final float MIN_INFO_PANEL_RATIO = 0.15f;
    static final float MAX_INFO_PANEL_RATIO = 0.50f;

    // --- Map pan / zoom ---
    float mapOffsetX = 0f;
    float mapOffsetY = 0f;
    float zoomLevel  = 2.0f;
    float lastZoomLevel = 2.0f;
    String cachedZoomText = "Zoom: 2.0x";

    // --- Screen layout (set by resize) ---
    int screenWidth;
    int screenHeight;
    int mapAreaHeight;
    int infoAreaHeight;
    float infoPanelRatio = DEFAULT_INFO_PANEL_RATIO;

    // --- Selection / character ---
    int selectedCellX = -1;
    int selectedCellY = -1;
    int charCellX = -1;
    int charCellY = -1;
    int cursorCellX = -1;
    int cursorCellY = -1;
    int homeCellX = -1;
    int homeCellY = -1;
    int homeFloor      = 1;    // floor number (1-based) the player's office is on
    char homeUnitLetter = 'A'; // unit letter (A-Z) within the floor; unit label = floor + letter (e.g. 3C)
    CityMap.RouteResult currentRoute = null;

    // --- Button bounds (written by InfoPanelRenderer, read by MainScreen for hit-testing) ---
    float moveToButtonX, moveToButtonY, moveToButtonW, moveToButtonH;
    float lookAroundBtnX, lookAroundBtnY, lookAroundBtnW, lookAroundBtnH;
    float restBtnX, restBtnY, restBtnW, restBtnH;       // written by UnitInteriorPopup
    float sleepBtnX, sleepBtnY, sleepBtnW, sleepBtnH;   // written by UnitInteriorPopup
    float goToOfficeBtnX, goToOfficeBtnY, goToOfficeBtnW, goToOfficeBtnH;
    float goToHotelRoomBtnX, goToHotelRoomBtnY, goToHotelRoomBtnW, goToHotelRoomBtnH;
    float openStashBtnX, openStashBtnY, openStashBtnW, openStashBtnH;
    float checkEmailsBtnX, checkEmailsBtnY, checkEmailsBtnW, checkEmailsBtnH;
    float openPhoneBtnX, openPhoneBtnY, openPhoneBtnW, openPhoneBtnH;
    float appointmentBtnX, appointmentBtnY, appointmentBtnW, appointmentBtnH;

    // Drop/Stash buttons in character tab (one per carried item; screen-space Y, updated each frame)
    static final int MAX_EQUIP_BTNS = 20;
    float[] equipDropBtnX = new float[MAX_EQUIP_BTNS];
    float[] equipDropBtnY = new float[MAX_EQUIP_BTNS];
    float[] equipDropBtnW = new float[MAX_EQUIP_BTNS];
    float   equipDropBtnH = 0f;
    int     equipDropBtnCount = 0;

    // Service buttons in info tab (one per service the current building offers)
    static final int MAX_SVC_BTNS = 4;
    float[] svcBtnX = new float[MAX_SVC_BTNS];
    float[] svcBtnY = new float[MAX_SVC_BTNS];
    float[] svcBtnW = new float[MAX_SVC_BTNS];
    float   svcBtnH = 0f;
    int     svcBtnCount = 0;
    float addNoteBtnX, addNoteBtnY, addNoteBtnW, addNoteBtnH;
    boolean noteIncludeTime     = true;
    boolean noteIncludeLocation = true;
    float noteTimeCbX, noteTimeCbY, noteTimeCbW, noteTimeCbH;
    float noteLocCbX,  noteLocCbY,  noteLocCbW,  noteLocCbH;

    // --- Unit interior popup ---
    boolean unitInteriorOpen    = false;
    boolean unitIsHotelRoom     = false;   // true when interior is a rented hotel room (no stash/email)
    String  unitInteriorLabel   = "";
    String  unitInteriorDescription = null;
    float unitExitBtnX, unitExitBtnY, unitExitBtnW, unitExitBtnH;
    float saveBtnX, saveBtnY, saveBtnW, saveBtnH;  // written by UnitInteriorPopup

    // --- Walk animation ---
    boolean isWalking   = false;
    java.util.List<int[]> walkPath = null;
    int   walkStepIdx   = 0;
    float walkTimer     = 0f;
    static final float WALK_STEP_SECONDS = 0.4f;

    /** Destination cell stored at walk-start; used to set charCellX/Y on arrival. */
    int walkDestCellX = -1;
    int walkDestCellY = -1;

    /**
     * Current junction position while walking (junction coords, e.g. 3.0 = exact junction 3,
     * or −1 when not walking).  Used by MapRenderer to draw the portrait ON the road.
     */
    float charJuncX = -1f;
    float charJuncY = -1f;

    // --- Traveled road path (accumulates every junction visited while walking) ---
    java.util.List<int[]> traveledPath = new java.util.ArrayList<>();

    // --- Developer mode toggle ---
    /** When {@code true} developer-mode features are active. Default ON. */
    boolean developerMode = true;
    /** Bounds of the "D" toggle button in the top info bar (written by InfoPanelRenderer). */
    float devModeBtnX, devModeBtnY, devModeBtnW, devModeBtnH;

    // --- Help toggle (info panel "?" button) ---
    boolean helpVisible = false;
    float helpBtnX, helpBtnY, helpBtnW, helpBtnH;

    // --- Info panel tab bar ---
    /** Currently active tab: {@code "INFO"}, {@code "CHARACTER"}, {@code "CALENDAR"}, or {@code "CASE FILE"}. */
    String activeInfoTab = "INFO";
    /** Tab bar height (written by InfoPanelRenderer, read by MainScreen). */
    float tabBarHeight = 0f;
    /** Tab bounds: [0] = Info tab, [1] = Character tab, [2] = Calendar tab, [3] = Case File tab (x, y, w, h). */
    float[] tabX = new float[4];
    float[] tabY = new float[4];
    float[] tabW = new float[4];
    float   tabH = 0f;

    // --- Info panel scroll (written by InfoPanelRenderer each frame) ---
    static final float SCROLLBAR_THICKNESS = 8f;
    float infoScrollY    = 0f;   // pixels content has scrolled up   (0 = top)
    float infoScrollX    = 0f;   // pixels content has scrolled right (0 = left)
    float infoMaxScrollY = 0f;   // maximum infoScrollY (set by InfoPanelRenderer)
    float infoMaxScrollX = 0f;   // maximum infoScrollX (set by InfoPanelRenderer)

    // --- Geometry helpers ---

    float getCellSize() {
        int base = getBaseVisibleCells();
        float availableWidth = screenWidth - RULER_WIDTH - RULER_GAP;
        return availableWidth / (float) base;
    }

    int getBaseVisibleCells() {
        return Math.max(3, Math.round(CityMap.MAP_SIZE / zoomLevel));
    }

    int getVisibleCellsX() {
        return getBaseVisibleCells();
    }

    int getVisibleCellsY() {
        float cellSize = getCellSize();
        return (int) ((mapAreaHeight - RULER_WIDTH - RULER_GAP) / cellSize);
    }

    void clampMapOffset() {
        int visX = getVisibleCellsX();
        int visY = getVisibleCellsY();
        mapOffsetX = MathUtils.clamp(mapOffsetX, 0, Math.max(0, CityMap.MAP_SIZE - visX));
        mapOffsetY = MathUtils.clamp(mapOffsetY, 0, Math.max(0, CityMap.MAP_SIZE - visY));
    }
}

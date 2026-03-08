package eb.framework1.ui;

import eb.framework1.city.*;
import eb.framework1.popup.*;
import eb.framework1.screen.*;


import com.badlogic.gdx.math.MathUtils;

/**
 * Holds all mutable map-view state shared between MainScreen and the rendering helpers.
 * Also provides the geometry helper methods (getCellSize, getVisibleCells, clampMapOffset)
 * so that both the screen logic and renderers compute layout consistently.
 */
public class MapViewState {

    // --- Constants (layout) ---
    public static final float RULER_WIDTH  = 45f;
    public static final float RULER_GAP    = 1f;
    public static final int   INFO_BAR_HEIGHT = 70;
    public static final float DEFAULT_INFO_PANEL_RATIO = 0.33f;
    public static final float MIN_INFO_PANEL_RATIO = 0.15f;
    public static final float MAX_INFO_PANEL_RATIO = 0.50f;

    // --- Map pan / zoom ---
    public float mapOffsetX = 0f;
    public float mapOffsetY = 0f;
    public float zoomLevel  = 2.0f;
    public float lastZoomLevel = 2.0f;
    public String cachedZoomText = "Zoom: 2.0x";

    // --- Screen layout (set by resize) ---
    public int screenWidth;
    public int screenHeight;
    public int mapAreaHeight;
    public int infoAreaHeight;
    public float infoPanelRatio = DEFAULT_INFO_PANEL_RATIO;

    // --- Selection / character ---
    public int selectedCellX = -1;
    public int selectedCellY = -1;
    public int charCellX = -1;
    public int charCellY = -1;
    public int cursorCellX = -1;
    public int cursorCellY = -1;
    public int homeCellX = -1;
    public int homeCellY = -1;
    public int homeFloor      = 1;    // floor number (1-based) the player's office is on
    public char homeUnitLetter = 'A'; // unit letter (A-Z) within the floor; unit label = floor + letter (e.g. 3C)
    public CityMap.RouteResult currentRoute = null;

    // --- Button bounds (written by InfoPanelRenderer, read by MainScreen for hit-testing) ---
    public float moveToButtonX, moveToButtonY, moveToButtonW, moveToButtonH;
    public float lookAroundBtnX, lookAroundBtnY, lookAroundBtnW, lookAroundBtnH;
    public float restBtnX, restBtnY, restBtnW, restBtnH;       // written by UnitInteriorPopup
    public float sleepBtnX, sleepBtnY, sleepBtnW, sleepBtnH;   // written by UnitInteriorPopup
    public float goToOfficeBtnX, goToOfficeBtnY, goToOfficeBtnW, goToOfficeBtnH;
    public float goToHotelRoomBtnX, goToHotelRoomBtnY, goToHotelRoomBtnW, goToHotelRoomBtnH;
    public float openStashBtnX, openStashBtnY, openStashBtnW, openStashBtnH;
    public float checkEmailsBtnX, checkEmailsBtnY, checkEmailsBtnW, checkEmailsBtnH;
    public float openPhoneBtnX, openPhoneBtnY, openPhoneBtnW, openPhoneBtnH;
    public float appointmentBtnX, appointmentBtnY, appointmentBtnW, appointmentBtnH;

    // Drop/Stash buttons in character tab (one per carried item; screen-space Y, updated each frame)
    public static final int MAX_EQUIP_BTNS = 20;
    public float[] equipDropBtnX = new float[MAX_EQUIP_BTNS];
    public float[] equipDropBtnY = new float[MAX_EQUIP_BTNS];
    public float[] equipDropBtnW = new float[MAX_EQUIP_BTNS];
    public float   equipDropBtnH = 0f;
    public int     equipDropBtnCount = 0;

    // Service buttons in info tab (one per service the current building offers)
    public static final int MAX_SVC_BTNS = 4;
    public float[] svcBtnX = new float[MAX_SVC_BTNS];
    public float[] svcBtnY = new float[MAX_SVC_BTNS];
    public float[] svcBtnW = new float[MAX_SVC_BTNS];
    public float   svcBtnH = 0f;
    public int     svcBtnCount = 0;

    // Inline improvement action buttons in scrollable content area of info panel
    public static final int MAX_IMP_BTNS = 8;
    public float[]       impBtnX   = new float[MAX_IMP_BTNS];
    public float[]       impBtnY   = new float[MAX_IMP_BTNS];
    public float[]       impBtnW   = new float[MAX_IMP_BTNS];
    public float         impBtnH   = 0f;
    public int           impBtnCount = 0;
    public Improvement[] impBtnImp = new Improvement[MAX_IMP_BTNS];
    public float addNoteBtnX, addNoteBtnY, addNoteBtnW, addNoteBtnH;
    public boolean noteIncludeTime     = true;
    public boolean noteIncludeLocation = true;
    public float noteTimeCbX, noteTimeCbY, noteTimeCbW, noteTimeCbH;
    public float noteLocCbX,  noteLocCbY,  noteLocCbW,  noteLocCbH;

    // --- Unit interior popup ---
    public boolean unitInteriorOpen    = false;
    public boolean unitIsHotelRoom     = false;   // true when interior is a rented hotel room (no stash/email)
    public String  unitInteriorLabel   = "";
    public String  unitInteriorDescription = null;
    public float unitExitBtnX, unitExitBtnY, unitExitBtnW, unitExitBtnH;
    public float saveBtnX, saveBtnY, saveBtnW, saveBtnH;  // written by UnitInteriorPopup

    // --- Walk animation ---
    public boolean isWalking   = false;
    public java.util.List<int[]> walkPath = null;
    public int   walkStepIdx   = 0;
    public float walkTimer     = 0f;
    public static final float WALK_STEP_SECONDS = 0.4f;

    /** Destination cell stored at walk-start; used to set charCellX/Y on arrival. */
    public int walkDestCellX = -1;
    public int walkDestCellY = -1;

    /**
     * Current junction position while walking (junction coords, e.g. 3.0 = exact junction 3,
     * or −1 when not walking).  Used by MapRenderer to draw the portrait ON the road.
     */
    public float charJuncX = -1f;
    public float charJuncY = -1f;

    // --- Traveled road path (accumulates every junction visited while walking) ---
    public java.util.List<int[]> traveledPath = new java.util.ArrayList<>();

    // --- Developer mode toggle ---
    /** When {@code true} developer-mode features are active. Default ON. */
    public boolean developerMode = true;
    /** Bounds of the "D" toggle button in the top info bar (written by InfoPanelRenderer). */
    public float devModeBtnX, devModeBtnY, devModeBtnW, devModeBtnH;

    // --- NPC tracking overlay ---
    /**
     * List of all NPC characters known to the current session.  Populated by
     * {@code MainScreen} whenever NPCs are generated or loaded.  In developer
     * mode all NPCs in this list are rendered as stick figures on the map;
     * otherwise only those with {@link eb.framework1.character.NpcCharacter#isTracked()}
     * equal to {@code true} are shown.
     */
    public java.util.List<eb.framework1.character.NpcCharacter> allNpcs =
            new java.util.ArrayList<>();

    /**
     * IDs of NPCs the player has already met (has a {@link eb.framework1.character.Relationship}
     * entry for).  Updated by {@code MainScreen} each frame.  Used by
     * {@code InfoPanelRenderer} to decide whether to draw an eye icon next to an NPC.
     */
    public java.util.Set<String> knownNpcIds = new java.util.HashSet<>();

    // Eye-icon hit areas in the info panel (one per visible unknown NPC at the player's cell)
    public static final int MAX_EYE_ICONS = 50;
    public float[] eyeIconX   = new float[MAX_EYE_ICONS];
    public float[] eyeIconY   = new float[MAX_EYE_ICONS];
    public float[] eyeIconW   = new float[MAX_EYE_ICONS];
    public float   eyeIconH   = 0f;
    public int     eyeIconCount = 0;
    /** NPC corresponding to each eye icon (parallel array). */
    public eb.framework1.character.NpcCharacter[] eyeIconNpc =
            new eb.framework1.character.NpcCharacter[MAX_EYE_ICONS];

    /**
     * Current in-game hour (0–23).  Used by {@code MapRenderer} to determine
     * which schedule entry is active and where each NPC should be drawn.
     * Updated by {@code MainScreen} each frame from the profile's
     * {@code gameDateTime} string.
     */
    public int currentHour = 8;

    // --- Help toggle (info panel "?" button) ---
    public boolean helpVisible = false;
    public float helpBtnX, helpBtnY, helpBtnW, helpBtnH;

    // --- Info panel tab bar ---
    /** Currently active tab: {@code "INFO"}, {@code "CHARACTER"}, {@code "CALENDAR"}, or {@code "CASE FILE"}. */
    public String activeInfoTab = "INFO";
    /** Tab bar height (written by InfoPanelRenderer, read by MainScreen). */
    float tabBarHeight = 0f;
    /** Tab bounds: [0] = Info tab, [1] = Character tab, [2] = Calendar tab, [3] = Case File tab (x, y, w, h). */
    public float[] tabX = new float[4];
    public float[] tabY = new float[4];
    public float[] tabW = new float[4];
    public float   tabH = 0f;

    // --- Expand/collapse button (▲/▼ after the last tab) ---
    /** When {@code true} the info panel covers the full screen (minus the top info bar). */
    public boolean panelExpanded = false;
    /** Bounds of the expand/collapse button (written by InfoPanelRenderer, read by MainScreen). */
    public float expandBtnX, expandBtnY, expandBtnW, expandBtnH;

    // --- Info panel scroll (written by InfoPanelRenderer each frame) ---
    public static final float SCROLLBAR_THICKNESS = 8f;
    public float infoScrollY    = 0f;   // pixels content has scrolled up   (0 = top)
    public float infoScrollX    = 0f;   // pixels content has scrolled right (0 = left)
    public float infoMaxScrollY = 0f;   // maximum infoScrollY (set by InfoPanelRenderer)
    public float infoMaxScrollX = 0f;   // maximum infoScrollX (set by InfoPanelRenderer)

    // --- Geometry helpers ---

    public float getCellSize() {
        int base = getBaseVisibleCells();
        float availableWidth = screenWidth - RULER_WIDTH - RULER_GAP;
        return availableWidth / (float) base;
    }

    public int getBaseVisibleCells() {
        return Math.max(3, Math.round(CityMap.MAP_SIZE / zoomLevel));
    }

    public int getVisibleCellsX() {
        return getBaseVisibleCells();
    }

    public int getVisibleCellsY() {
        float cellSize = getCellSize();
        return (int) ((mapAreaHeight - RULER_WIDTH - RULER_GAP) / cellSize);
    }

    public void clampMapOffset() {
        int visX = getVisibleCellsX();
        int visY = getVisibleCellsY();
        mapOffsetX = MathUtils.clamp(mapOffsetX, 0, Math.max(0, CityMap.MAP_SIZE - visX));
        mapOffsetY = MathUtils.clamp(mapOffsetY, 0, Math.max(0, CityMap.MAP_SIZE - visY));
    }
}

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
    CityMap.RouteResult currentRoute = null;

    // --- Button bounds (written by InfoPanelRenderer, read by MainScreen for hit-testing) ---
    float moveToButtonX, moveToButtonY, moveToButtonW, moveToButtonH;
    float lookAroundBtnX, lookAroundBtnY, lookAroundBtnW, lookAroundBtnH;

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

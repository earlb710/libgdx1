package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Main game screen displaying the city map and info panel.
 * Layout: Top 2/3 is the map view, Bottom 1/3 is the info block.
 * 
 * Features:
 * - Displays 16x16 city map with colored cells by category
 * - Zoomable from full map view to 3x3 cell view
 * - Draggable/scrollable map
 */
public class MainScreen implements Screen {
    private Main game;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private BitmapFont smallFont;
    private GlyphLayout glyphLayout;
    private boolean initialized = false;
    
    // Map data
    private CityMap cityMap;
    private Profile profile;
    
    // Map view parameters
    private float mapOffsetX = 0;  // Pan offset in cells
    private float mapOffsetY = 0;
    private float zoomLevel = 2.0f; // 1.0 = full map, higher = more zoom
    private float lastZoomLevel = 2.0f; // For caching zoom text
    private String cachedZoomText = "Zoom: 2.0x";
    
    // Tuning constants
    private static final float MIN_ZOOM = 1.0f;  // Full map view (16x16 visible)
    private static final float MAX_ZOOM = 5.33f; // 3x3 cells visible (16/3 ≈ 5.33)
    private static final float ZOOM_SPEED = 0.15f;
    private static final float SCROLL_SPEED = 0.5f;
    private static final float TAP_THRESHOLD_PIXELS = 10f;
    private static final float DEFAULT_INFO_PANEL_RATIO = 0.33f; // Default info panel takes 1/3 of screen
    private static final float MIN_INFO_PANEL_RATIO = 0.15f;     // Minimum 15% of screen
    private static final float MAX_INFO_PANEL_RATIO = 0.50f;     // Maximum 50% of screen
    
    // Selected cell
    private int selectedCellX = -1;
    private int selectedCellY = -1;
    
    // Cursor hover position (cell coordinates, -1 if not over map)
    private int cursorCellX = -1;
    private int cursorCellY = -1;
    
    // Drag state
    private boolean isDragging = false;
    private float dragStartX, dragStartY;
    private float dragStartOffsetX, dragStartOffsetY;
    
    // Ruler constants
    private static final float RULER_WIDTH = 45f;  // Width of ruler strip
    private static final float RULER_GAP = 1f;     // Gap between rulers and map
    private static final Color RULER_BG_COLOR = new Color(0.1f, 0.1f, 0.15f, 1f);
    private static final Color RULER_MARKER_COLOR = new Color(1f, 0.5f, 0f, 1f); // Orange marker
    private static final String[] HEX_DIGITS = {"0", "1", "2", "3", "4", "5", "6", "7", 
                                                  "8", "9", "A", "B", "C", "D", "E", "F"};
    
    // Input handling
    private InputProcessor previousInputProcessor;
    
    // Color cache for categories (avoid allocations during render)
    private Map<String, Color> categoryColorCache;
    
    // Icon texture cache for building icons
    private Map<String, Texture> iconTextureCache;
    
    // Character portrait texture
    private Texture characterIconTexture;
    
    // Layout dimensions (calculated in resize)
    private int screenWidth, screenHeight;
    private int mapAreaHeight;    // Height of map area (full height minus info panel)
    private int infoAreaHeight;   // Height of info panel
    private int infoAreaY;        // Y position of info area top
    private float infoPanelRatio = DEFAULT_INFO_PANEL_RATIO; // Configurable info panel height ratio
    
    // Colors
    private static final Color MOUNTAIN_COLOR = new Color(0.4f, 0.35f, 0.3f, 1f);
    private static final Color BEACH_COLOR = new Color(0.95f, 0.9f, 0.6f, 1f);
    private static final Color GRID_COLOR = new Color(0.2f, 0.2f, 0.25f, 1f);
    private static final Color INFO_BG_COLOR = new Color(0.15f, 0.15f, 0.2f, 1f);
    private static final Color INFO_BORDER_COLOR = new Color(0.4f, 0.4f, 0.5f, 1f);
    private static final Color SELECTION_COLOR = new Color(1f, 1f, 0f, 1f);
    private static final Color LABEL_COLOR = new Color(0f, 1f, 0f, 1f); // Bright green for all labels
    private static final int SELECTION_THICKNESS = 5; // Thickness of selection border in pixels
    private static final int INFO_BAR_HEIGHT = 70;    // Height of the top info bar (date + money)
    
    // Floor-based brightness constants
    private static final float MIN_BRIGHTNESS = 0.55f; // Brightness for 1-floor buildings
    private static final int MAX_FLOORS = 10;          // Maximum floors for brightness calculation
    
    // Reusable color object to avoid allocations during render
    private final Color tempColor = new Color();
    
    public MainScreen(Main game, Profile profile) {
        this.game = game;
        this.profile = profile;
    }
    
    @Override
    public void show() {
        Gdx.app.log("MainScreen", "show() called");
        
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        this.glyphLayout = new GlyphLayout();
        
        // Get fonts from FontManager
        this.font = game.getFontManager().getBodyFont();
        this.smallFont = game.getFontManager().getSmallFont();
        
        // Generate the city map using profile seed and game data
        GameDataManager gameData = game.getGameDataManager();
        this.cityMap = new CityMap(profile, gameData);
        
        Gdx.app.log("MainScreen", "CityMap generated: " + cityMap);
        
        // Choose a random building cell as the character's starting location
        List<Cell> buildingCells = new ArrayList<>();
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = cityMap.getCell(x, y);
                if (cell.getTerrainType() == TerrainType.BUILDING) {
                    buildingCells.add(cell);
                }
            }
        }
        if (!buildingCells.isEmpty()) {
            Random rand = new Random(profile.getRandSeed() + 7);
            Cell startCell = buildingCells.get(rand.nextInt(buildingCells.size()));
            selectedCellX = startCell.getX();
            selectedCellY = startCell.getY();
            Gdx.app.log("MainScreen", "Character starting location: " + selectedCellX + "," + selectedCellY);
        }
        
        // Initialize category color cache to avoid allocations during render
        initColorCache(gameData);
        
        // Load building icon textures
        loadBuildingIcons();
        
        // Load character portrait icon texture
        String iconName = profile.getCharacterIcon();
        if (iconName != null && !iconName.isEmpty()) {
            characterIconTexture = TextureUtils.makeNegative("character/" + iconName + ".png");
            characterIconTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        
        // Set up input processing (save previous processor for restoration)
        previousInputProcessor = Gdx.input.getInputProcessor();
        setupInput();
        
        // Calculate layout
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        
        // Center the map on the character's starting cell
        if (selectedCellX >= 0 && selectedCellY >= 0) {
            int visibleCellsX = getVisibleCellsX();
            int visibleCellsY = getVisibleCellsY();
            mapOffsetX = selectedCellX - visibleCellsX / 2.0f;
            mapOffsetY = selectedCellY - visibleCellsY / 2.0f;
            clampMapOffset();
        }
        
        initialized = true;
        Gdx.app.log("MainScreen", "Initialization complete");
    }
    
    private void setupInput() {
        InputAdapter inputAdapter = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                int flippedY = screenHeight - screenY;
                
                // Check if touch is in map area
                if (flippedY > infoAreaHeight) {
                    isDragging = true;
                    dragStartX = screenX;
                    dragStartY = screenY;
                    dragStartOffsetX = mapOffsetX;
                    dragStartOffsetY = mapOffsetY;
                    return true;
                }
                return false;
            }
            
            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                int flippedY = screenHeight - screenY;
                
                // If it was a tap (not a drag), select a cell
                if (isDragging && flippedY > infoAreaHeight) {
                    float dragDistance = Vector2.len(screenX - dragStartX, screenY - dragStartY);
                    if (dragDistance < TAP_THRESHOLD_PIXELS) { // Tap, not drag
                        selectCellAt(screenX, flippedY);
                    }
                }
                
                isDragging = false;
                return true;
            }
            
            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (isDragging) {
                    // Calculate drag delta in screen pixels
                    float deltaX = screenX - dragStartX;
                    float deltaY = screenY - dragStartY; // Not flipped for drag
                    
                    // Convert to cell units based on current zoom and cell size
                    float cellSize = getCellSize();
                    float cellDeltaX = -deltaX / cellSize;
                    float cellDeltaY = -deltaY / cellSize; // Negate to swap scroll direction
                    
                    // Apply to offset
                    mapOffsetX = dragStartOffsetX + cellDeltaX;
                    mapOffsetY = dragStartOffsetY + cellDeltaY;
                    
                    // Clamp offsets
                    clampMapOffset();
                    return true;
                }
                return false;
            }
            
            @Override
            public boolean scrolled(float amountX, float amountY) {
                // Zoom with mouse scroll
                float oldZoom = zoomLevel;
                zoomLevel -= amountY * ZOOM_SPEED;
                zoomLevel = MathUtils.clamp(zoomLevel, MIN_ZOOM, MAX_ZOOM);
                
                // Adjust offset to zoom toward center
                if (oldZoom != zoomLevel) {
                    clampMapOffset();
                }
                return true;
            }
            
            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                updateCursorCell(screenX, screenHeight - screenY);
                return false;
            }
        };
        
        // Set input processor
        Gdx.input.setInputProcessor(inputAdapter);
    }
    
    private void updateCursorCell(int screenX, int flippedY) {
        // Check if cursor is in map area
        if (flippedY <= infoAreaHeight) {
            cursorCellX = -1;
            cursorCellY = -1;
            return;
        }
        
        // Convert screen coordinates to cell coordinates
        float cellSize = getCellSize();
        int visibleCellsX = getVisibleCellsX();
        int visibleCellsY = getVisibleCellsY();
        
        float mapAreaX = RULER_WIDTH + RULER_GAP;
        float mapAreaY = infoAreaHeight;
        
        float relX = screenX - mapAreaX;
        float relY = flippedY - mapAreaY;
        
        // Check if within map bounds
        if (relX < 0 || relX >= cellSize * visibleCellsX || 
            relY < 0 || relY >= cellSize * visibleCellsY) {
            cursorCellX = -1;
            cursorCellY = -1;
            return;
        }
        
        int cellX = (int)(mapOffsetX + relX / cellSize);
        // Invert Y because screen Y increases upward but row 0 is at top of map
        int cellY = (int)(mapOffsetY + visibleCellsY - relY / cellSize);
        
        // Validate cell is within map
        if (cellX >= 0 && cellX < CityMap.MAP_SIZE && cellY >= 0 && cellY < CityMap.MAP_SIZE) {
            cursorCellX = cellX;
            cursorCellY = cellY;
        } else {
            cursorCellX = -1;
            cursorCellY = -1;
        }
    }
    
    private void selectCellAt(int screenX, int screenY) {
        // Convert screen coordinates to cell coordinates
        float cellSize = getCellSize();
        int visibleCellsY = getVisibleCellsY();
        
        // Map area starts after left ruler + gap at infoAreaHeight
        float mapAreaX = RULER_WIDTH + RULER_GAP;
        float mapAreaY = infoAreaHeight;
        
        // Calculate which cell was clicked
        float relX = screenX - mapAreaX;
        float relY = screenY - mapAreaY;
        
        int cellX = (int)(mapOffsetX + relX / cellSize);
        // Invert Y because screen Y increases upward but row 0 is at top of map
        int cellY = (int)(mapOffsetY + visibleCellsY - relY / cellSize);
        
        // Check bounds
        if (cellX >= 0 && cellX < CityMap.MAP_SIZE && cellY >= 0 && cellY < CityMap.MAP_SIZE) {
            selectedCellX = cellX;
            selectedCellY = cellY;
            Gdx.app.log("MainScreen", "Selected cell: " + cellX + "," + cellY);
        }
    }
    
    private float getCellSize() {
        // Cell size is determined by available width (minus ruler and gap) divided by base visible cells
        // This ensures the map fills the available width after the left ruler
        int baseVisibleCells = getBaseVisibleCells();
        float availableWidth = screenWidth - RULER_WIDTH - RULER_GAP;
        return availableWidth / (float)baseVisibleCells;
    }
    
    private int getBaseVisibleCells() {
        // At zoom 1.0, show all 16 cells horizontally. At max zoom, show 3 cells.
        return Math.max(3, Math.round(CityMap.MAP_SIZE / zoomLevel));
    }
    
    private int getVisibleCellsX() {
        // Horizontal visible cells based on width
        return getBaseVisibleCells();
    }
    
    private int getVisibleCellsY() {
        // Vertical visible cells - subtract ruler width and gap from available height
        float cellSize = getCellSize();
        return (int)((mapAreaHeight - RULER_WIDTH - RULER_GAP) / cellSize);
    }
    
    private void clampMapOffset() {
        int visibleCellsX = getVisibleCellsX();
        int visibleCellsY = getVisibleCellsY();
        float maxOffsetX = CityMap.MAP_SIZE - visibleCellsX;
        float maxOffsetY = CityMap.MAP_SIZE - visibleCellsY;
        mapOffsetX = MathUtils.clamp(mapOffsetX, 0, Math.max(0, maxOffsetX));
        mapOffsetY = MathUtils.clamp(mapOffsetY, 0, Math.max(0, maxOffsetY));
    }
    
    @Override
    public void render(float delta) {
        if (!initialized) {
            ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
            return;
        }
        
        // Handle keyboard input for zoom
        handleKeyboardInput();
        
        // Clear screen
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
        
        // Draw the map in top 2/3
        drawMap();
        
        // Draw rulers on sides of map
        drawRulers();
        
        // Draw info bar above map (top of screen)
        drawInfoBar();
        
        // Draw info block in bottom 1/3
        drawInfoBlock();
    }
    
    private void handleKeyboardInput() {
        // Zoom with +/- keys
        if (Gdx.input.isKeyJustPressed(Input.Keys.PLUS) || Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)) {
            zoomLevel = MathUtils.clamp(zoomLevel + ZOOM_SPEED * 2, MIN_ZOOM, MAX_ZOOM);
            clampMapOffset();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
            zoomLevel = MathUtils.clamp(zoomLevel - ZOOM_SPEED * 2, MIN_ZOOM, MAX_ZOOM);
            clampMapOffset();
        }
        
        // Arrow keys for scrolling
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            mapOffsetX -= SCROLL_SPEED;
            clampMapOffset();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            mapOffsetX += SCROLL_SPEED;
            clampMapOffset();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            mapOffsetY += SCROLL_SPEED;
            clampMapOffset();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            mapOffsetY -= SCROLL_SPEED;
            clampMapOffset();
        }
    }
    
    private void drawMap() {
        float cellSize = getCellSize();
        int visibleCellsX = getVisibleCellsX();
        int visibleCellsY = getVisibleCellsY();
        
        // Border size scales with cell size for visibility
        float borderSize = Math.max(2, cellSize * 0.06f); // 6% of cell size, minimum 2px
        float pathwaySize = Math.max(1, borderSize * 0.25f); // Pathway is 1/4 the width of a road
        
        // Map starts after left ruler + gap and at info panel top
        float mapStartX = RULER_WIDTH + RULER_GAP;
        float mapStartY = infoAreaHeight;
        
        // Calculate which cells to draw
        int startCellX = (int)mapOffsetX;
        int startCellY = (int)mapOffsetY;
        int endCellX = Math.min(startCellX + visibleCellsX + 1, CityMap.MAP_SIZE);
        int endCellY = Math.min(startCellY + visibleCellsY + 1, CityMap.MAP_SIZE);
        
        // Fractional offset for smooth scrolling
        float fracOffsetX = mapOffsetX - startCellX;
        float fracOffsetY = mapOffsetY - startCellY;
        
        // Draw black background for grid (creates black borders between cells)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(mapStartX, mapStartY, cellSize * visibleCellsX, cellSize * visibleCellsY);
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Draw cells with per-side border gaps based on road access.
        // A border (gap) is drawn on sides where road access exists (representing the road).
        // No border is drawn where there is no road access (building extends to edge).
        // Y axis is inverted: row 0 at top, row F at bottom.
        // Map NORTH (y+1) corresponds to screen bottom; Map SOUTH (y-1) to screen top.
        for (int cx = startCellX; cx < endCellX; cx++) {
            for (int cy = startCellY; cy < endCellY; cy++) {
                float drawX = mapStartX + (cx - startCellX - fracOffsetX) * cellSize;
                // Invert Y: row 0 at top (higher screen Y), row F at bottom (lower screen Y)
                float drawY = mapStartY + (visibleCellsY - 1 - (cy - startCellY - fracOffsetY)) * cellSize;
                
                // Skip only if completely outside visible area (allow partial drawing)
                if (drawX + cellSize < mapStartX - cellSize || drawX > mapStartX + cellSize * visibleCellsX + cellSize || 
                    drawY + cellSize < mapStartY - cellSize || drawY > mapStartY + cellSize * visibleCellsY + cellSize) {
                    continue;
                }
                
                // Use pre-computed render data for color and border flags
                CellRenderData rd = cityMap.getCellRenderData(cx, cy);
                shapeRenderer.setColor(rd.getR(), rd.getG(), rd.getB(), rd.getA());
                
                // Calculate per-side border insets based on road type.
                // ROAD = full borderSize, PATHWAY = pathwaySize (1/4 of road), NONE = 0
                // Map directions to screen edges (Y axis is inverted):
                //   Map WEST  -> screen LEFT,   Map EAST  -> screen RIGHT
                //   Map NORTH -> screen BOTTOM,  Map SOUTH -> screen TOP
                float leftInset   = borderInset(rd.getBorderTypeWest(),  borderSize, pathwaySize);
                float rightInset  = borderInset(rd.getBorderTypeEast(),  borderSize, pathwaySize);
                float bottomInset = borderInset(rd.getBorderTypeNorth(), borderSize, pathwaySize);
                float topInset    = borderInset(rd.getBorderTypeSouth(), borderSize, pathwaySize);
                
                shapeRenderer.rect(drawX + leftInset, drawY + bottomInset,
                                   cellSize - leftInset - rightInset,
                                   cellSize - bottomInset - topInset);
            }
        }
        
        shapeRenderer.end();
        
        // Draw building icons on cells (with inverted Y)
        batch.begin();
        for (int cx = startCellX; cx < endCellX; cx++) {
            for (int cy = startCellY; cy < endCellY; cy++) {
                CellRenderData rd = cityMap.getCellRenderData(cx, cy);
                String iconPath = rd.getIconPath();
                if (iconPath == null) continue;
                Texture iconTex = iconTextureCache.get(iconPath);
                if (iconTex == null) continue;
                
                float drawX = mapStartX + (cx - startCellX - fracOffsetX) * cellSize;
                float drawY = mapStartY + (visibleCellsY - 1 - (cy - startCellY - fracOffsetY)) * cellSize;
                
                // Scale icon to fit within cell (with border insets), centered
                float leftInset   = borderInset(rd.getBorderTypeWest(),  borderSize, pathwaySize);
                float rightInset  = borderInset(rd.getBorderTypeEast(),  borderSize, pathwaySize);
                float bottomInset = borderInset(rd.getBorderTypeNorth(), borderSize, pathwaySize);
                float topInset    = borderInset(rd.getBorderTypeSouth(), borderSize, pathwaySize);
                float availW = cellSize - leftInset - rightInset;
                float availH = cellSize - bottomInset - topInset;
                float iconSize = Math.min(availW, availH) * 0.7f; // 70% of available space
                float iconX = drawX + leftInset + (availW - iconSize) / 2;
                float iconY = drawY + bottomInset + (availH - iconSize) / 2;
                
                // Tint not needed - icons have transparent bg with black lines
                batch.setColor(Color.WHITE);
                batch.draw(iconTex, iconX, iconY, iconSize, iconSize);
            }
        }
        
        // Draw character portrait icon in the lower-right of the selected cell
        if (characterIconTexture != null && selectedCellX >= 0 && selectedCellY >= 0) {
            if (selectedCellX >= startCellX && selectedCellX < endCellX &&
                selectedCellY >= startCellY && selectedCellY < endCellY) {
                float drawX = mapStartX + (selectedCellX - startCellX - fracOffsetX) * cellSize;
                float drawY = mapStartY + (visibleCellsY - 1 - (selectedCellY - startCellY - fracOffsetY)) * cellSize;
                float portraitSize = cellSize * 0.4f;
                float portraitX = drawX + cellSize - portraitSize - borderSize;
                float portraitY = drawY + borderSize;
                batch.setColor(Color.WHITE);
                batch.draw(characterIconTexture, portraitX, portraitY, portraitSize, portraitSize);
            }
        }
        
        batch.end();
        
        // Draw selection highlight (with inverted Y)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        if (selectedCellX >= 0 && selectedCellY >= 0) {
            if (selectedCellX >= startCellX && selectedCellX < endCellX &&
                selectedCellY >= startCellY && selectedCellY < endCellY) {
                float drawX = mapStartX + (selectedCellX - startCellX - fracOffsetX) * cellSize;
                float drawY = mapStartY + (visibleCellsY - 1 - (selectedCellY - startCellY - fracOffsetY)) * cellSize;
                shapeRenderer.setColor(SELECTION_COLOR);
                // Draw thick selection border
                for (int i = 0; i < SELECTION_THICKNESS; i++) {
                    shapeRenderer.rect(drawX + i, drawY + i, cellSize - i * 2, cellSize - i * 2);
                }
            }
        }
        
        shapeRenderer.end();
        
        // Draw cell coordinates when zoomed in enough (with inverted Y)
        if (zoomLevel >= 2.0f) {
            batch.begin();
            // Use smaller font for cell coordinates
            smallFont.getData().setScale(0.7f);
            for (int cx = startCellX; cx < endCellX; cx++) {
                for (int cy = startCellY; cy < endCellY; cy++) {
                    float drawX = mapStartX + (cx - startCellX - fracOffsetX) * cellSize;
                    float drawY = mapStartY + (visibleCellsY - 1 - (cy - startCellY - fracOffsetY)) * cellSize;
                    
                    String coords = Integer.toHexString(cx).toUpperCase() + Integer.toHexString(cy).toUpperCase();
                    glyphLayout.setText(smallFont, coords);
                    smallFont.draw(batch, coords, drawX + borderSize + 2, drawY + cellSize - borderSize - 2);
                }
            }
            // Restore font scale
            smallFont.getData().setScale(1.0f);
            batch.end();
        }
    }
    
    /**
     * Returns the border inset size for a given road type.
     * ROAD uses full borderSize, PATHWAY uses pathwaySize (1/4 of road), NONE uses 0.
     */
    private static float borderInset(RoadType type, float borderSize, float pathwaySize) {
        switch (type) {
            case ROAD:    return borderSize;
            case PATHWAY: return pathwaySize;
            default:      return 0;
        }
    }
    
    private void drawRulers() {
        float cellSize = getCellSize();
        int visibleCellsX = getVisibleCellsX();
        int visibleCellsY = getVisibleCellsY();
        
        // Map area positioning (same as drawMap)
        float mapStartX = RULER_WIDTH + RULER_GAP;
        float mapStartY = infoAreaHeight;
        
        // Calculate which cells are visible
        int startCellX = (int)mapOffsetX;
        int startCellY = (int)mapOffsetY;
        float fracOffsetX = mapOffsetX - startCellX;
        float fracOffsetY = mapOffsetY - startCellY;
        
        // Draw ruler backgrounds and cursor markers (all shape rendering first)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(RULER_BG_COLOR);
        
        // Left vertical ruler background (at left edge x=0)
        shapeRenderer.rect(0, mapStartY, RULER_WIDTH, cellSize * visibleCellsY);
        
        // Top horizontal ruler background (just below the info bar)
        float topRulerY = screenHeight - INFO_BAR_HEIGHT - RULER_WIDTH;
        shapeRenderer.rect(mapStartX, topRulerY, cellSize * visibleCellsX, RULER_WIDTH);
        
        // Draw cursor markers on rulers (if cursor is over map) - with inverted Y
        if (cursorCellY >= startCellY && cursorCellY < startCellY + visibleCellsY) {
            float rulerY = mapStartY + (visibleCellsY - 1 - (cursorCellY - startCellY - fracOffsetY)) * cellSize;
            shapeRenderer.setColor(RULER_MARKER_COLOR);
            shapeRenderer.rect(0, rulerY, RULER_WIDTH, cellSize);
        }
        if (cursorCellX >= startCellX && cursorCellX < startCellX + visibleCellsX) {
            float rulerX = mapStartX + (cursorCellX - startCellX - fracOffsetX) * cellSize;
            shapeRenderer.setColor(RULER_MARKER_COLOR);
            shapeRenderer.rect(rulerX, topRulerY, cellSize, RULER_WIDTH);
        }
        
        shapeRenderer.end();;
        
        // Draw hex numbers (all text rendering)
        batch.begin();
        
        // Draw left ruler hex numbers (Y axis - cell rows) - inverted: 0 at top, F at bottom
        for (int i = 0; i < visibleCellsY; i++) {
            int cellY = startCellY + i;
            if (cellY >= 0 && cellY < CityMap.MAP_SIZE) {
                // Invert Y position: row 0 at top (higher screen Y), row F at bottom
                float rulerY = mapStartY + (visibleCellsY - 1 - (i - fracOffsetY)) * cellSize;
                
                String hex = HEX_DIGITS[cellY];
                glyphLayout.setText(smallFont, hex);
                float textX = (RULER_WIDTH - glyphLayout.width) / 2;
                float textY = rulerY + (cellSize + glyphLayout.height) / 2;
                smallFont.setColor(cursorCellY == cellY ? Color.BLACK : Color.WHITE);
                smallFont.draw(batch, hex, textX, textY);
            }
        }
        
        // Draw top ruler hex numbers (X axis - cell columns)
        for (int i = 0; i < visibleCellsX; i++) {
            int cellX = startCellX + i;
            if (cellX >= 0 && cellX < CityMap.MAP_SIZE) {
                float rulerX = mapStartX + (i - fracOffsetX) * cellSize;
                
                String hex = HEX_DIGITS[cellX];
                glyphLayout.setText(smallFont, hex);
                float textX = rulerX + (cellSize - glyphLayout.width) / 2;
                float textY = topRulerY + RULER_WIDTH - (RULER_WIDTH - glyphLayout.height) / 2;
                smallFont.setColor(cursorCellX == cellX ? Color.BLACK : Color.WHITE);
                smallFont.draw(batch, hex, textX, textY);
            }
        }
        
        batch.end();
    }
    
    private Color getCellColor(Cell cell) {
        switch (cell.getTerrainType()) {
            case MOUNTAIN:
                return MOUNTAIN_COLOR;
            case BEACH:
                return BEACH_COLOR;
            case BUILDING:
                // Get base color from cached category colors, then apply floor-based brightness
                if (cell.hasBuilding() && cell.getBuilding().getDefinition() != null) {
                    Building building = cell.getBuilding();
                    String categoryId = building.getDefinition().getCategory();
                    Color baseColor = categoryColorCache.get(categoryId);
                    if (baseColor != null) {
                        // Calculate brightness based on floors (more floors = brighter)
                        // Brightness ranges from 0.4 (1 floor) to 1.0 (MAX_FLOORS)
                        int floors = building.getFloors();
                        float brightness = MIN_BRIGHTNESS + (1.0f - MIN_BRIGHTNESS) * (floors / (float) MAX_FLOORS);
                        // Apply brightness to base color (reuse tempColor to avoid allocations)
                        tempColor.set(
                            Math.min(1.0f, baseColor.r * brightness),
                            Math.min(1.0f, baseColor.g * brightness),
                            Math.min(1.0f, baseColor.b * brightness),
                            1.0f
                        );
                        return tempColor;
                    }
                }
                // Fallback to gray
                return Color.GRAY;
            default:
                return Color.GRAY;
        }
    }
    
    /**
     * Initialize the color cache for building categories to avoid allocations during render.
     */
    private void initColorCache(GameDataManager gameData) {
        categoryColorCache = new HashMap<>();
        for (CategoryDefinition cat : gameData.getCategories()) {
            float[] rgb = cat.getColorFloats();
            categoryColorCache.put(cat.getId(), new Color(rgb[0], rgb[1], rgb[2], 1f));
        }
        // Add fallback gray color
        categoryColorCache.put(null, Color.GRAY);
        Gdx.app.log("MainScreen", "Cached " + categoryColorCache.size() + " category colors");
    }
    
    /**
     * Loads building icon textures from the pre-computed render data.
     * Only loads unique icon paths to avoid duplicate texture loading.
     */
    private void loadBuildingIcons() {
        iconTextureCache = new HashMap<>();
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                CellRenderData rd = cityMap.getCellRenderData(x, y);
                String iconPath = rd.getIconPath();
                if (iconPath != null && !iconTextureCache.containsKey(iconPath)) {
                    try {
                        Texture tex = TextureUtils.makeWhiteTransparent(iconPath);
                        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                        iconTextureCache.put(iconPath, tex);
                    } catch (Exception e) {
                        Gdx.app.log("MainScreen", "Could not load icon: " + iconPath + " - " + e.getMessage());
                    }
                }
            }
        }
        Gdx.app.log("MainScreen", "Loaded " + iconTextureCache.size() + " building icons");
    }
    
    /**
     * Helper method to draw a label in bright green and value in white.
     * @return the textY position (unchanged, caller should subtract lineHeight)
     */
    private float drawLabelValue(SpriteBatch batch, BitmapFont font, String label, String value, float textX, float textY) {
        // Draw label in bright green
        font.setColor(LABEL_COLOR);
        font.draw(batch, label, textX, textY);
        glyphLayout.setText(font, label);
        float labelWidth = glyphLayout.width;
        // Draw value in white
        font.setColor(Color.WHITE);
        font.draw(batch, value, textX + labelWidth, textY);
        return textY;
    }
    
    /**
     * Formats attribute modifiers as a compact string, e.g. "[INT+2 PER-1]".
     */
    private String formatAttributeModifiers(Map<CharacterAttribute, Integer> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<CharacterAttribute, Integer> entry : modifiers.entrySet()) {
            if (!first) sb.append(' ');
            first = false;
            String displayName = entry.getKey().getDisplayName();
            String abbrev = displayName.length() >= 3
                ? displayName.substring(0, 3).toUpperCase()
                : displayName.toUpperCase();
            int val = entry.getValue();
            sb.append(abbrev);
            if (val > 0) sb.append('+');
            sb.append(val);
        }
        sb.append(']');
        return sb.toString();
    }
    
    private void drawInfoBlock() {
        // Draw info area background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(INFO_BG_COLOR);
        shapeRenderer.rect(0, 0, screenWidth, infoAreaHeight);
        shapeRenderer.end();
        
        // Draw border at top of info area
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(INFO_BORDER_COLOR);
        shapeRenderer.line(0, infoAreaHeight, screenWidth, infoAreaHeight);
        shapeRenderer.end();
        
        // Draw info text
        batch.begin();
        
        // Calculate proper line heights based on actual font metrics
        glyphLayout.setText(font, "Hg"); // Use characters with ascenders/descenders
        float fontLineHeight = glyphLayout.height * 1.4f; // Add 40% for spacing
        
        glyphLayout.setText(smallFont, "Hg");
        float smallFontLineHeight = glyphLayout.height * 1.4f;
        
        float textX = 20;
        float textY = infoAreaHeight - fontLineHeight; // Start one line height from top
        
        // Show selected cell info (no title heading)
        if (selectedCellX >= 0 && selectedCellY >= 0) {
            Cell cell = cityMap.getCell(selectedCellX, selectedCellY);
            
            // Draw "Cell:" label in bright green, then coordinates in white
            textY = drawLabelValue(batch, font, "Cell: ", selectedCellX + ", " + selectedCellY, textX, textY);
            textY -= fontLineHeight;
            
            // Draw "Terrain:" label in bright green, then value in white
            textY = drawLabelValue(batch, font, "Terrain: ", cell.getTerrainType().getDisplayName(), textX, textY);
            textY -= fontLineHeight;
            
            if (cell.hasBuilding()) {
                Building building = cell.getBuilding();
                
                if (building.isDiscovered()) {
                    // Draw "Building:" label in bright green, then value in white
                    String buildingModStr = formatAttributeModifiers(building.getAttributeModifiers());
                    String buildingDisplay = building.getName();
                    if (!buildingModStr.isEmpty()) {
                        buildingDisplay += " " + buildingModStr;
                    }
                    textY = drawLabelValue(batch, font, "Building: ", buildingDisplay, textX, textY);
                    textY -= fontLineHeight;
                    
                    if (building.getDefinition() != null) {
                        // Draw "Category:" label in bright green, then value in white
                        textY = drawLabelValue(batch, font, "Category: ", building.getCategory(), textX, textY);
                        textY -= fontLineHeight;
                        
                        // Draw "Floors:" label in bright green, then value in white
                        textY = drawLabelValue(batch, font, "Floors: ", String.valueOf(building.getFloors()), textX, textY);
                        textY -= fontLineHeight;
                    }
                    
                    // Show improvements (only if there's space)
                    if (textY > smallFontLineHeight * 6) { // Need space for footer + improvements
                        // Draw "Improvements:" label in bright green (no value)
                        font.setColor(LABEL_COLOR);
                        font.draw(batch, "Improvements:", textX, textY);
                        font.setColor(Color.WHITE);
                        textY -= fontLineHeight;
                        
                        for (Improvement imp : building.getImprovements()) {
                            if (textY < smallFontLineHeight * 2) break; // Stop before footer area
                            if (imp.isDiscovered()) {
                                String modStr = formatAttributeModifiers(imp.getAttributeModifiers());
                                String display = "  - " + imp.getName() + " (Lvl " + imp.getLevel() + ")";
                                if (!modStr.isEmpty()) {
                                    display += " " + modStr;
                                }
                                smallFont.draw(batch, display, textX, textY);
                            } else {
                                smallFont.draw(batch, "  - ???", textX, textY);
                            }
                            textY -= smallFontLineHeight;
                        }
                    }
                } else {
                    // Building not yet discovered - show placeholder
                    textY = drawLabelValue(batch, font, "Building: ", "???", textX, textY);
                    textY -= fontLineHeight;
                }
            }
        } else {
            font.draw(batch, "Click on a cell to see details", textX, textY);
        }
        
        // Show zoom level in corner (cache the formatted string)
        if (zoomLevel != lastZoomLevel) {
            cachedZoomText = "Zoom: " + String.format("%.1fx", zoomLevel);
            lastZoomLevel = zoomLevel;
        }
        glyphLayout.setText(smallFont, cachedZoomText);
        smallFont.draw(batch, cachedZoomText, screenWidth - glyphLayout.width - 20, infoAreaHeight - smallFontLineHeight);
        
        // Show controls hint - position with proper margin from bottom
        String controlsHint = "Scroll to zoom | Drag to pan | +/- keys | Arrow keys";
        glyphLayout.setText(smallFont, controlsHint);
        float footerY = smallFontLineHeight + 10; // Proper margin from bottom
        smallFont.draw(batch, controlsHint, (screenWidth - glyphLayout.width) / 2, footerY);
        
        batch.end();
    }

    private void drawInfoBar() {
        float barY = screenHeight - INFO_BAR_HEIGHT;

        // Background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(INFO_BG_COLOR);
        shapeRenderer.rect(0, barY, screenWidth, INFO_BAR_HEIGHT);
        shapeRenderer.end();

        // Bottom border of bar
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(INFO_BORDER_COLOR);
        shapeRenderer.line(0, barY, screenWidth, barY);
        shapeRenderer.end();

        batch.begin();

        // Measure a sample string to get a reliable text height for vertical centering
        glyphLayout.setText(font, "Hg");
        float textY = barY + (INFO_BAR_HEIGHT + glyphLayout.height) / 2;

        // Date/time on the left in white (bold via bodyFont)
        String dateText = profile.getGameDateTime();
        font.setColor(Color.WHITE);
        font.draw(batch, dateText, 10, textY);

        // Money on the right in yellow (bold via bodyFont)
        String moneyText = "$" + profile.getMoney();
        glyphLayout.setText(font, moneyText);
        font.setColor(Color.YELLOW);
        font.draw(batch, moneyText, screenWidth - glyphLayout.width - 10, textY);

        font.setColor(Color.WHITE);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.infoAreaHeight = (int)(height * infoPanelRatio);  // Configurable info panel height
        this.mapAreaHeight = height - infoAreaHeight - INFO_BAR_HEIGHT;  // Map uses remaining height minus info bar
        
        // Calculate actual map display size (rectangular, uses full available space)
        float cellSize = getCellSize();
        int visibleCellsX = getVisibleCellsX();
        int visibleCellsY = getVisibleCellsY();
        float mapDisplayWidth = cellSize * visibleCellsX;
        float mapDisplayHeight = cellSize * visibleCellsY;
        
        Gdx.app.log("MainScreen", "=== SCREEN LAYOUT DEBUG ===");
        Gdx.app.log("MainScreen", "Screen size: " + width + "x" + height + " pixels");
        Gdx.app.log("MainScreen", "Map area available: " + screenWidth + "x" + mapAreaHeight + " pixels");
        Gdx.app.log("MainScreen", "Info panel height: " + infoAreaHeight + " pixels (ratio=" + infoPanelRatio + ")");
        Gdx.app.log("MainScreen", "Actual map display: " + mapDisplayWidth + "x" + mapDisplayHeight + " pixels");
        Gdx.app.log("MainScreen", "Cell size: " + cellSize + " pixels, Visible cells: " + visibleCellsX + "x" + visibleCellsY);
        Gdx.app.log("MainScreen", "Zoom level: " + zoomLevel);
        Gdx.app.log("MainScreen", "===========================");
    }
    
    /**
     * Set the info panel height ratio (0.0 to 1.0).
     * Clamped to MIN_INFO_PANEL_RATIO and MAX_INFO_PANEL_RATIO.
     * @param ratio The ratio of screen height for info panel
     */
    public void setInfoPanelRatio(float ratio) {
        this.infoPanelRatio = MathUtils.clamp(ratio, MIN_INFO_PANEL_RATIO, MAX_INFO_PANEL_RATIO);
        // Recalculate layout only if dimensions are initialized
        if (screenWidth > 0 && screenHeight > 0) {
            resize(screenWidth, screenHeight);
        }
    }
    
    /**
     * Get the current info panel height ratio.
     * @return The ratio of screen height used by info panel
     */
    public float getInfoPanelRatio() {
        return infoPanelRatio;
    }
    
    @Override
    public void pause() {
    }
    
    @Override
    public void resume() {
    }
    
    @Override
    public void hide() {
        Gdx.app.log("MainScreen", "hide() called");
        // Restore previous input processor
        if (previousInputProcessor != null) {
            Gdx.input.setInputProcessor(previousInputProcessor);
        }
    }
    
    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        // Dispose building icon textures
        if (iconTextureCache != null) {
            for (Texture tex : iconTextureCache.values()) {
                tex.dispose();
            }
            iconTextureCache.clear();
        }
        // Dispose character portrait texture
        if (characterIconTexture != null) {
            characterIconTexture.dispose();
            characterIconTexture = null;
        }
        // Restore previous input processor on dispose
        if (previousInputProcessor != null) {
            Gdx.input.setInputProcessor(previousInputProcessor);
        }
        // Fonts are managed by FontManager
    }
}

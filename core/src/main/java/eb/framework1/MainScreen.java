package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.HashMap;
import java.util.Map;

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
    private float zoomLevel = 1.0f; // 1.0 = full map, higher = more zoom
    private float lastZoomLevel = 1.0f; // For caching zoom text
    private String cachedZoomText = "Zoom: 1.0x";
    
    // Tuning constants
    private static final float MIN_ZOOM = 1.0f;  // Full map view (16x16 visible)
    private static final float MAX_ZOOM = 5.33f; // 3x3 cells visible (16/3 ≈ 5.33)
    private static final float ZOOM_SPEED = 0.15f;
    private static final float SCROLL_SPEED = 0.5f;
    private static final float TAP_THRESHOLD_PIXELS = 10f;
    
    // Selected cell
    private int selectedCellX = -1;
    private int selectedCellY = -1;
    
    // Drag state
    private boolean isDragging = false;
    private float dragStartX, dragStartY;
    private float dragStartOffsetX, dragStartOffsetY;
    
    // Input handling
    private InputProcessor previousInputProcessor;
    
    // Color cache for categories (avoid allocations during render)
    private Map<String, Color> categoryColorCache;
    
    // Layout dimensions (calculated in resize)
    private int screenWidth, screenHeight;
    private int mapAreaHeight;    // Top 2/3
    private int infoAreaHeight;   // Bottom 1/3
    private int infoAreaY;        // Y position of info area top
    
    // Colors
    private static final Color MOUNTAIN_COLOR = new Color(0.4f, 0.35f, 0.3f, 1f);
    private static final Color BEACH_COLOR = new Color(0.95f, 0.9f, 0.6f, 1f);
    private static final Color GRID_COLOR = new Color(0.2f, 0.2f, 0.25f, 1f);
    private static final Color INFO_BG_COLOR = new Color(0.15f, 0.15f, 0.2f, 1f);
    private static final Color INFO_BORDER_COLOR = new Color(0.4f, 0.4f, 0.5f, 1f);
    private static final Color SELECTION_COLOR = new Color(1f, 1f, 0f, 1f);
    private static final Color CELL_LABEL_COLOR = new Color(0f, 1f, 0f, 1f); // Bright green for "Cell:" label
    private static final int SELECTION_THICKNESS = 5; // Thickness of selection border in pixels
    
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
        
        // Initialize category color cache to avoid allocations during render
        initColorCache(gameData);
        
        // Set up input processing (save previous processor for restoration)
        previousInputProcessor = Gdx.input.getInputProcessor();
        setupInput();
        
        // Calculate layout
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        
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
                    float cellDeltaY = deltaY / cellSize; // Flip Y for coordinate system
                    
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
        };
        
        // Set input processor
        Gdx.input.setInputProcessor(inputAdapter);
    }
    
    private void selectCellAt(int screenX, int screenY) {
        // Convert screen coordinates to cell coordinates
        float cellSize = getCellSize();
        int visibleCells = getVisibleCells();
        
        // Map area starts at infoAreaHeight
        float mapAreaX = (screenWidth - cellSize * visibleCells) / 2;
        float mapAreaY = infoAreaHeight + (mapAreaHeight - cellSize * visibleCells) / 2;
        
        // Calculate which cell was clicked
        float relX = screenX - mapAreaX;
        float relY = screenY - mapAreaY;
        
        int cellX = (int)(mapOffsetX + relX / cellSize);
        int cellY = (int)(mapOffsetY + relY / cellSize);
        
        // Check bounds
        if (cellX >= 0 && cellX < CityMap.MAP_SIZE && cellY >= 0 && cellY < CityMap.MAP_SIZE) {
            selectedCellX = cellX;
            selectedCellY = cellY;
            Gdx.app.log("MainScreen", "Selected cell: " + cellX + "," + cellY);
        }
    }
    
    private float getCellSize() {
        // At zoom=1, all 16 cells fit. At max zoom, 3 cells fit.
        int visibleCells = getVisibleCells();
        float maxCellSize = Math.min(screenWidth, mapAreaHeight) / (float)visibleCells;
        return maxCellSize * 0.95f; // 95% to leave some margin
    }
    
    private int getVisibleCells() {
        // At zoom 1.0, show all 16 cells. At max zoom, show 3 cells.
        return Math.max(3, Math.round(CityMap.MAP_SIZE / zoomLevel));
    }
    
    private void clampMapOffset() {
        int visibleCells = getVisibleCells();
        float maxOffset = CityMap.MAP_SIZE - visibleCells;
        mapOffsetX = MathUtils.clamp(mapOffsetX, 0, Math.max(0, maxOffset));
        mapOffsetY = MathUtils.clamp(mapOffsetY, 0, Math.max(0, maxOffset));
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
        int visibleCells = getVisibleCells();
        
        // Border size scales with cell size for visibility
        float borderSize = Math.max(2, cellSize * 0.06f); // 6% of cell size, minimum 2px
        
        // Center the map in the map area
        float mapStartX = (screenWidth - cellSize * visibleCells) / 2;
        float mapStartY = infoAreaHeight + (mapAreaHeight - cellSize * visibleCells) / 2;
        
        // Calculate which cells to draw
        int startCellX = (int)mapOffsetX;
        int startCellY = (int)mapOffsetY;
        int endCellX = Math.min(startCellX + visibleCells + 1, CityMap.MAP_SIZE);
        int endCellY = Math.min(startCellY + visibleCells + 1, CityMap.MAP_SIZE);
        
        // Fractional offset for smooth scrolling
        float fracOffsetX = mapOffsetX - startCellX;
        float fracOffsetY = mapOffsetY - startCellY;
        
        // Draw black background for grid (creates black borders between cells)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(mapStartX, mapStartY, cellSize * visibleCells, cellSize * visibleCells);
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Draw cells with border gap (partial cells at edges are allowed)
        for (int cx = startCellX; cx < endCellX; cx++) {
            for (int cy = startCellY; cy < endCellY; cy++) {
                Cell cell = cityMap.getCell(cx, cy);
                
                float drawX = mapStartX + (cx - startCellX - fracOffsetX) * cellSize;
                float drawY = mapStartY + (cy - startCellY - fracOffsetY) * cellSize;
                
                // Skip only if completely outside visible area (allow partial drawing)
                if (drawX + cellSize < mapStartX - cellSize || drawX > mapStartX + cellSize * visibleCells + cellSize || 
                    drawY + cellSize < mapStartY - cellSize || drawY > mapStartY + cellSize * visibleCells + cellSize) {
                    continue;
                }
                
                // Get cell color
                Color cellColor = getCellColor(cell);
                shapeRenderer.setColor(cellColor);
                // Draw cell with border gap on all sides
                shapeRenderer.rect(drawX + borderSize, drawY + borderSize, 
                                   cellSize - borderSize * 2, cellSize - borderSize * 2);
            }
        }
        
        shapeRenderer.end();
        
        // Draw selection highlight
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        if (selectedCellX >= 0 && selectedCellY >= 0) {
            if (selectedCellX >= startCellX && selectedCellX < endCellX &&
                selectedCellY >= startCellY && selectedCellY < endCellY) {
                float drawX = mapStartX + (selectedCellX - startCellX - fracOffsetX) * cellSize;
                float drawY = mapStartY + (selectedCellY - startCellY - fracOffsetY) * cellSize;
                shapeRenderer.setColor(SELECTION_COLOR);
                // Draw thick selection border
                for (int i = 0; i < SELECTION_THICKNESS; i++) {
                    shapeRenderer.rect(drawX + i, drawY + i, cellSize - i * 2, cellSize - i * 2);
                }
            }
        }
        
        shapeRenderer.end();
        
        // Draw cell coordinates when zoomed in enough
        if (zoomLevel >= 2.0f) {
            batch.begin();
            for (int cx = startCellX; cx < endCellX; cx++) {
                for (int cy = startCellY; cy < endCellY; cy++) {
                    float drawX = mapStartX + (cx - startCellX - fracOffsetX) * cellSize;
                    float drawY = mapStartY + (cy - startCellY - fracOffsetY) * cellSize;
                    
                    String coords = cx + "," + cy;
                    glyphLayout.setText(smallFont, coords);
                    smallFont.draw(batch, coords, drawX + borderSize + 2, drawY + cellSize - borderSize - 2);
                }
            }
            batch.end();
        }
    }
    
    private Color getCellColor(Cell cell) {
        switch (cell.getTerrainType()) {
            case MOUNTAIN:
                return MOUNTAIN_COLOR;
            case BEACH:
                return BEACH_COLOR;
            case BUILDING:
                // Get color from cached category colors
                if (cell.hasBuilding() && cell.getBuilding().getDefinition() != null) {
                    String categoryId = cell.getBuilding().getDefinition().getCategory();
                    Color cachedColor = categoryColorCache.get(categoryId);
                    if (cachedColor != null) {
                        return cachedColor;
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
        
        // Title
        font.setColor(Color.WHITE);
        font.draw(batch, "INFO PANEL", textX, textY);
        textY -= fontLineHeight * 1.3f;
        
        // Show selected cell info
        if (selectedCellX >= 0 && selectedCellY >= 0) {
            Cell cell = cityMap.getCell(selectedCellX, selectedCellY);
            
            // Draw "Cell:" label in bright green, then coordinates in white
            font.setColor(CELL_LABEL_COLOR);
            font.draw(batch, "Cell: ", textX, textY);
            glyphLayout.setText(font, "Cell: ");
            float cellLabelWidth = glyphLayout.width;
            font.setColor(Color.WHITE);
            font.draw(batch, selectedCellX + ", " + selectedCellY, textX + cellLabelWidth, textY);
            textY -= fontLineHeight;
            
            font.draw(batch, "Terrain: " + cell.getTerrainType().getDisplayName(), textX, textY);
            textY -= fontLineHeight;
            
            if (cell.hasBuilding()) {
                Building building = cell.getBuilding();
                font.draw(batch, "Building: " + building.getName(), textX, textY);
                textY -= fontLineHeight;
                
                if (building.getDefinition() != null) {
                    font.draw(batch, "Category: " + building.getCategory(), textX, textY);
                    textY -= fontLineHeight;
                    
                    font.draw(batch, "Floors: " + building.getFloors(), textX, textY);
                    textY -= fontLineHeight;
                }
                
                // Show improvements (only if there's space)
                if (textY > smallFontLineHeight * 6) { // Need space for footer + improvements
                    font.draw(batch, "Improvements:", textX, textY);
                    textY -= fontLineHeight;
                    
                    for (Improvement imp : building.getImprovements()) {
                        if (textY < smallFontLineHeight * 2) break; // Stop before footer area
                        smallFont.draw(batch, "  - " + imp.getName() + " (Lvl " + imp.getLevel() + ")", textX, textY);
                        textY -= smallFontLineHeight;
                    }
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
    
    @Override
    public void resize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.infoAreaHeight = height / 3;  // Bottom 1/3
        this.mapAreaHeight = height - infoAreaHeight;  // Top 2/3
        
        Gdx.app.log("MainScreen", "Resized: " + width + "x" + height + 
                    ", mapArea=" + mapAreaHeight + ", infoArea=" + infoAreaHeight);
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
        // Restore previous input processor on dispose
        if (previousInputProcessor != null) {
            Gdx.input.setInputProcessor(previousInputProcessor);
        }
        // Fonts are managed by FontManager
    }
}

package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

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
    private static final float MIN_ZOOM = 1.0f;  // Full map view (16x16 visible)
    private static final float MAX_ZOOM = 5.33f; // 3x3 cells visible (16/3 ≈ 5.33)
    private static final float ZOOM_SPEED = 0.15f;
    
    // Selected cell
    private int selectedCellX = -1;
    private int selectedCellY = -1;
    
    // Drag state
    private boolean isDragging = false;
    private float dragStartX, dragStartY;
    private float dragStartOffsetX, dragStartOffsetY;
    
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
        
        // Set up input processing
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
                    if (dragDistance < 10) { // Tap, not drag
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
        float scrollSpeed = 0.5f;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            mapOffsetX -= scrollSpeed;
            clampMapOffset();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            mapOffsetX += scrollSpeed;
            clampMapOffset();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            mapOffsetY += scrollSpeed;
            clampMapOffset();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            mapOffsetY -= scrollSpeed;
            clampMapOffset();
        }
    }
    
    private void drawMap() {
        float cellSize = getCellSize();
        int visibleCells = getVisibleCells();
        
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
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Draw cells
        for (int cx = startCellX; cx < endCellX; cx++) {
            for (int cy = startCellY; cy < endCellY; cy++) {
                Cell cell = cityMap.getCell(cx, cy);
                
                float drawX = mapStartX + (cx - startCellX - fracOffsetX) * cellSize;
                float drawY = mapStartY + (cy - startCellY - fracOffsetY) * cellSize;
                
                // Skip if outside visible area
                if (drawX + cellSize < 0 || drawX > screenWidth || 
                    drawY + cellSize < infoAreaHeight || drawY > screenHeight) {
                    continue;
                }
                
                // Get cell color
                Color cellColor = getCellColor(cell);
                shapeRenderer.setColor(cellColor);
                shapeRenderer.rect(drawX + 1, drawY + 1, cellSize - 2, cellSize - 2);
            }
        }
        
        shapeRenderer.end();
        
        // Draw grid lines
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(GRID_COLOR);
        
        for (int cx = startCellX; cx <= endCellX; cx++) {
            float drawX = mapStartX + (cx - startCellX - fracOffsetX) * cellSize;
            shapeRenderer.line(drawX, infoAreaHeight, drawX, screenHeight);
        }
        for (int cy = startCellY; cy <= endCellY; cy++) {
            float drawY = mapStartY + (cy - startCellY - fracOffsetY) * cellSize;
            shapeRenderer.line(0, drawY, screenWidth, drawY);
        }
        
        // Draw selection highlight
        if (selectedCellX >= 0 && selectedCellY >= 0) {
            if (selectedCellX >= startCellX && selectedCellX < endCellX &&
                selectedCellY >= startCellY && selectedCellY < endCellY) {
                float drawX = mapStartX + (selectedCellX - startCellX - fracOffsetX) * cellSize;
                float drawY = mapStartY + (selectedCellY - startCellY - fracOffsetY) * cellSize;
                shapeRenderer.setColor(SELECTION_COLOR);
                shapeRenderer.rect(drawX, drawY, cellSize, cellSize);
                shapeRenderer.rect(drawX + 2, drawY + 2, cellSize - 4, cellSize - 4);
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
                    smallFont.draw(batch, coords, drawX + 3, drawY + cellSize - 3);
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
                // Get color from building category
                if (cell.hasBuilding() && cell.getBuilding().getDefinition() != null) {
                    BuildingDefinition def = cell.getBuilding().getDefinition();
                    CategoryDefinition cat = game.getGameDataManager().getCategoryById(def.getCategory());
                    if (cat != null) {
                        float[] rgb = cat.getColorFloats();
                        return new Color(rgb[0], rgb[1], rgb[2], 1f);
                    }
                }
                // Fallback to gray
                return new Color(0.5f, 0.5f, 0.5f, 1f);
            default:
                return Color.GRAY;
        }
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
        
        float textX = 20;
        float textY = infoAreaHeight - 20;
        float lineHeight = 25;
        
        // Title
        font.setColor(Color.WHITE);
        font.draw(batch, "INFO PANEL", textX, textY);
        textY -= lineHeight * 1.5f;
        
        // Show selected cell info
        if (selectedCellX >= 0 && selectedCellY >= 0) {
            Cell cell = cityMap.getCell(selectedCellX, selectedCellY);
            
            font.draw(batch, "Cell: " + selectedCellX + ", " + selectedCellY, textX, textY);
            textY -= lineHeight;
            
            font.draw(batch, "Terrain: " + cell.getTerrainType().getDisplayName(), textX, textY);
            textY -= lineHeight;
            
            if (cell.hasBuilding()) {
                Building building = cell.getBuilding();
                font.draw(batch, "Building: " + building.getName(), textX, textY);
                textY -= lineHeight;
                
                if (building.getDefinition() != null) {
                    font.draw(batch, "Category: " + building.getCategory(), textX, textY);
                    textY -= lineHeight;
                    
                    font.draw(batch, "Floors: " + building.getFloors(), textX, textY);
                    textY -= lineHeight;
                }
                
                // Show improvements
                font.draw(batch, "Improvements:", textX, textY);
                textY -= lineHeight;
                
                for (Improvement imp : building.getImprovements()) {
                    smallFont.draw(batch, "  - " + imp.getName() + " (Lvl " + imp.getLevel() + ")", textX, textY);
                    textY -= lineHeight * 0.8f;
                }
            }
        } else {
            font.draw(batch, "Click on a cell to see details", textX, textY);
        }
        
        // Show zoom level in corner
        String zoomText = "Zoom: " + String.format("%.1fx", zoomLevel);
        glyphLayout.setText(smallFont, zoomText);
        smallFont.draw(batch, zoomText, screenWidth - glyphLayout.width - 20, infoAreaHeight - 20);
        
        // Show controls hint
        String controlsHint = "Scroll to zoom | Drag to pan | +/- keys | Arrow keys";
        glyphLayout.setText(smallFont, controlsHint);
        smallFont.draw(batch, controlsHint, (screenWidth - glyphLayout.width) / 2, 20);
        
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
    }
    
    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        // Fonts are managed by FontManager
    }
}

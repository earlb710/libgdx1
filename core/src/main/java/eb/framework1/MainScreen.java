package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Main game screen.  Orchestrates map rendering, info panel, look-around popup,
 * input handling, and game-state updates.
 *
 * Rendering is delegated to:
 *   {@link MapRenderer}        – map tiles, route, icons, rulers
 *   {@link InfoPanelRenderer}  – bottom info strip and top date/money bar
 *   {@link LookAroundPopup}    – modal look-around animation and results
 *
 * Shared layout / selection state lives in {@link MapViewState}.
 */
public class MainScreen implements Screen {

    private Main game;
    private SpriteBatch   batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont    font;
    private BitmapFont    smallFont;
    private GlyphLayout   glyphLayout;
    private boolean initialized = false;

    // Data
    private CityMap  cityMap;
    private Profile  profile;

    // Shared view state (layout, pan, zoom, selection, button bounds)
    private final MapViewState state = new MapViewState();

    // Rendering helpers
    private MapRenderer       mapRenderer;
    private InfoPanelRenderer infoPanelRenderer;
    private LookAroundPopup   lookAroundPopup;

    // Input state
    private InputProcessor previousInputProcessor;
    private boolean infoAreaPressed = false;
    private float   infoTouchStartX, infoTouchStartY;
    private float   infoScrollDragStartScrollY, infoScrollDragStartScrollX;
    private boolean isDragging = false;
    private float   dragStartX, dragStartY;
    private float   dragStartOffsetX, dragStartOffsetY;

    // Tuning constants
    private static final float MIN_ZOOM             = 1.0f;
    private static final float MAX_ZOOM             = 5.33f;
    private static final float ZOOM_SPEED           = 0.15f;
    private static final float SCROLL_SPEED         = 0.5f;
    private static final float TAP_THRESHOLD_PIXELS = 10f;

    // -------------------------------------------------------------------------

    public MainScreen(Main game, Profile profile) {
        this.game    = game;
        this.profile = profile;
    }

    // -------------------------------------------------------------------------
    // Screen lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void show() {
        Gdx.app.log("MainScreen", "show() called");

        batch         = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        glyphLayout   = new GlyphLayout();
        font          = game.getFontManager().getBodyFont();
        smallFont     = game.getFontManager().getSmallFont();

        GameDataManager gameData = game.getGameDataManager();
        cityMap = new CityMap(profile, gameData);
        Gdx.app.log("MainScreen", "CityMap generated: " + cityMap);

        // Pick a random building as the starting cell
        List<Cell> buildingCells = new ArrayList<>();
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                if (cityMap.getCell(x, y).getTerrainType() == TerrainType.BUILDING)
                    buildingCells.add(cityMap.getCell(x, y));
            }
        }
        if (!buildingCells.isEmpty()) {
            Cell start = buildingCells.get(new Random(profile.getRandSeed() + 7)
                    .nextInt(buildingCells.size()));
            state.selectedCellX = start.getX();
            state.selectedCellY = start.getY();
            state.charCellX = state.selectedCellX;
            state.charCellY = state.selectedCellY;
            discoverCell(state.charCellX, state.charCellY);
            Gdx.app.log("MainScreen", "Start: " + state.charCellX + "," + state.charCellY);
        }

        // Build rendering helpers
        mapRenderer = new MapRenderer(batch, shapeRenderer, font, smallFont, glyphLayout, cityMap);
        mapRenderer.loadBuildingIcons();

        String iconName = profile.getCharacterIcon();
        if (iconName != null && !iconName.isEmpty()) {
            Texture charTex = TextureUtils.makeNegative("character/" + iconName + ".png");
            charTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            mapRenderer.setCharacterIconTexture(charTex);
        }

        infoPanelRenderer = new InfoPanelRenderer(batch, shapeRenderer, font, smallFont,
                glyphLayout, cityMap, profile);

        lookAroundPopup = new LookAroundPopup(batch, shapeRenderer, font, smallFont,
                glyphLayout, cityMap, profile);

        // Input + layout
        previousInputProcessor = Gdx.input.getInputProcessor();
        setupInput();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Centre on starting cell
        if (state.selectedCellX >= 0) {
            state.mapOffsetX = state.selectedCellX - state.getVisibleCellsX() / 2.0f;
            state.mapOffsetY = state.selectedCellY - state.getVisibleCellsY() / 2.0f;
            state.clampMapOffset();
        }

        initialized = true;
        Gdx.app.log("MainScreen", "Initialisation complete");
    }

    @Override
    public void render(float delta) {
        if (!initialized) {
            ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
            return;
        }

        handleKeyboardInput();
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);

        mapRenderer.drawMap(state);
        mapRenderer.drawRulers(state);
        infoPanelRenderer.drawInfoBar(state);
        infoPanelRenderer.drawInfoBlock(state, !lookAroundPopup.isVisible());

        if (lookAroundPopup.isVisible()) {
            lookAroundPopup.update(delta);
            lookAroundPopup.draw(state.screenWidth, state.screenHeight, state.infoAreaHeight);
        }
    }

    @Override
    public void resize(int width, int height) {
        state.screenWidth    = width;
        state.screenHeight   = height;
        state.infoAreaHeight = (int)(height * state.infoPanelRatio)
                + (int)(MapViewState.SCROLLBAR_THICKNESS);
        state.mapAreaHeight  = height - state.infoAreaHeight - MapViewState.INFO_BAR_HEIGHT;
        Gdx.app.log("MainScreen", "Resized to " + width + "x" + height
                + " (info=" + state.infoAreaHeight + " map=" + state.mapAreaHeight + ")");
    }

    @Override public void pause()  {}
    @Override public void resume() {}

    @Override
    public void hide() {
        Gdx.app.log("MainScreen", "hide() called");
        if (previousInputProcessor != null)
            Gdx.input.setInputProcessor(previousInputProcessor);
    }

    @Override
    public void dispose() {
        if (batch         != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (mapRenderer   != null) mapRenderer.dispose();
        if (previousInputProcessor != null)
            Gdx.input.setInputProcessor(previousInputProcessor);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void setInfoPanelRatio(float ratio) {
        state.infoPanelRatio = MathUtils.clamp(ratio,
                MapViewState.MIN_INFO_PANEL_RATIO, MapViewState.MAX_INFO_PANEL_RATIO);
        if (state.screenWidth > 0) resize(state.screenWidth, state.screenHeight);
    }

    public float getInfoPanelRatio() {
        return state.infoPanelRatio;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private void setupInput() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                // Any popup blocks normal interaction – capture for later tap detection
                if (lookAroundPopup.isVisible()) {
                    infoAreaPressed  = true;
                    infoTouchStartX  = screenX;
                    infoTouchStartY  = screenY;
                    isDragging       = false;
                    return true;
                }
                int flippedY = state.screenHeight - screenY;
                if (flippedY > state.infoAreaHeight) {
                    isDragging       = true;
                    dragStartX       = screenX;
                    dragStartY       = screenY;
                    dragStartOffsetX = state.mapOffsetX;
                    dragStartOffsetY = state.mapOffsetY;
                    infoAreaPressed  = false;
                } else {
                    infoAreaPressed = true;
                    infoTouchStartX = screenX;
                    infoTouchStartY = screenY;
                    infoScrollDragStartScrollY = state.infoScrollY;
                    infoScrollDragStartScrollX = state.infoScrollX;
                    isDragging      = false;
                }
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                int flippedY = state.screenHeight - screenY;

                if (lookAroundPopup.isResults()) {
                    if (infoAreaPressed) {
                        float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                        if (d < TAP_THRESHOLD_PIXELS) lookAroundPopup.onTap(screenX, flippedY);
                        infoAreaPressed = false;
                    }
                    isDragging = false;
                    return true;
                }
                if (lookAroundPopup.isAnimating()) {
                    infoAreaPressed = false;
                    isDragging = false;
                    return true;
                }

                if (isDragging && flippedY > state.infoAreaHeight) {
                    float d = Vector2.len(screenX - dragStartX, screenY - dragStartY);
                    if (d < TAP_THRESHOLD_PIXELS) selectCellAt(screenX, flippedY);
                }
                if (infoAreaPressed) {
                    float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                    if (d < TAP_THRESHOLD_PIXELS) {
                        checkMoveToButtonClick(screenX, flippedY);
                        checkLookAroundButtonClick(screenX, flippedY);
                    }
                    infoAreaPressed = false;
                }
                isDragging = false;
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (isDragging) {
                    float cs = state.getCellSize();
                    state.mapOffsetX = dragStartOffsetX - (screenX - dragStartX) / cs;
                    state.mapOffsetY = dragStartOffsetY - (screenY - dragStartY) / cs;
                    state.clampMapOffset();
                    return true;
                }
                if (infoAreaPressed && !lookAroundPopup.isVisible()) {
                    // screenY is 0 at top, increases downward
                    // Drag up (screenY decreases) → reveal content below → increase infoScrollY
                    float dy = infoTouchStartY - screenY;
                    float dx = infoTouchStartX - screenX;
                    state.infoScrollY = MathUtils.clamp(
                            infoScrollDragStartScrollY + dy, 0f, state.infoMaxScrollY);
                    state.infoScrollX = MathUtils.clamp(
                            infoScrollDragStartScrollX + dx, 0f, state.infoMaxScrollX);
                    return true;
                }
                return false;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                float old = state.zoomLevel;
                state.zoomLevel = MathUtils.clamp(state.zoomLevel - amountY * ZOOM_SPEED, MIN_ZOOM, MAX_ZOOM);
                if (old != state.zoomLevel) state.clampMapOffset();
                return true;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                updateCursorCell(screenX, state.screenHeight - screenY);
                return false;
            }
        });
    }

    private void handleKeyboardInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.PLUS) || Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)) {
            state.zoomLevel = MathUtils.clamp(state.zoomLevel + ZOOM_SPEED * 2, MIN_ZOOM, MAX_ZOOM);
            state.clampMapOffset();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
            state.zoomLevel = MathUtils.clamp(state.zoomLevel - ZOOM_SPEED * 2, MIN_ZOOM, MAX_ZOOM);
            state.clampMapOffset();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))  { state.mapOffsetX -= SCROLL_SPEED; state.clampMapOffset(); }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) { state.mapOffsetX += SCROLL_SPEED; state.clampMapOffset(); }
        if (Gdx.input.isKeyPressed(Input.Keys.UP))    { state.mapOffsetY += SCROLL_SPEED; state.clampMapOffset(); }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN))  { state.mapOffsetY -= SCROLL_SPEED; state.clampMapOffset(); }
    }

    private void updateCursorCell(int screenX, int flippedY) {
        float mapAreaX = MapViewState.RULER_WIDTH + MapViewState.RULER_GAP;
        if (flippedY <= state.infoAreaHeight) {
            state.cursorCellX = state.cursorCellY = -1;
            return;
        }
        float cs = state.getCellSize();
        int visY = state.getVisibleCellsY();
        float relX = screenX - mapAreaX;
        float relY = flippedY - state.infoAreaHeight;
        if (relX < 0 || relX >= cs * state.getVisibleCellsX() || relY < 0 || relY >= cs * visY) {
            state.cursorCellX = state.cursorCellY = -1;
            return;
        }
        int cx = (int)(state.mapOffsetX + relX / cs);
        int cy = (int)(state.mapOffsetY + visY - relY / cs);
        if (cx >= 0 && cx < CityMap.MAP_SIZE && cy >= 0 && cy < CityMap.MAP_SIZE) {
            state.cursorCellX = cx;
            state.cursorCellY = cy;
        } else {
            state.cursorCellX = state.cursorCellY = -1;
        }
    }

    private void selectCellAt(int screenX, int screenY) {
        float cs = state.getCellSize();
        int visY = state.getVisibleCellsY();
        float relX = screenX - (MapViewState.RULER_WIDTH + MapViewState.RULER_GAP);
        float relY = screenY - state.infoAreaHeight;
        int cx = (int)(state.mapOffsetX + relX / cs);
        int cy = (int)(state.mapOffsetY + visY - relY / cs);
        if (cx >= 0 && cx < CityMap.MAP_SIZE && cy >= 0 && cy < CityMap.MAP_SIZE) {
            state.selectedCellX = cx;
            state.selectedCellY = cy;
            state.infoScrollY = 0f;
            state.infoScrollX = 0f;
            recalculateRoute();
            Gdx.app.log("MainScreen", "Selected: " + cx + "," + cy);
        }
    }

    // -------------------------------------------------------------------------
    // Button hit-testing
    // -------------------------------------------------------------------------

    private void checkMoveToButtonClick(int screenX, int flippedY) {
        if (state.moveToButtonW <= 0) return;
        if (screenX >= state.moveToButtonX && screenX <= state.moveToButtonX + state.moveToButtonW
                && flippedY >= state.moveToButtonY && flippedY <= state.moveToButtonY + state.moveToButtonH) {
            handleMoveToClick();
        }
    }

    private void checkLookAroundButtonClick(int screenX, int flippedY) {
        if (state.lookAroundBtnW <= 0) return;
        if (screenX >= state.lookAroundBtnX && screenX <= state.lookAroundBtnX + state.lookAroundBtnW
                && flippedY >= state.lookAroundBtnY && flippedY <= state.lookAroundBtnY + state.lookAroundBtnH) {
            lookAroundPopup.start(state.charCellX, state.charCellY);
        }
    }

    // -------------------------------------------------------------------------
    // Game logic
    // -------------------------------------------------------------------------

    private void recalculateRoute() {
        if (state.selectedCellX < 0 || state.charCellX < 0
                || (state.selectedCellX == state.charCellX && state.selectedCellY == state.charCellY)) {
            state.currentRoute = null;
            return;
        }
        state.currentRoute = cityMap.findFastestRoute(
                state.charCellX, state.charCellY,
                state.selectedCellX, state.selectedCellY);
    }

    private void handleMoveToClick() {
        if (state.currentRoute == null || !state.currentRoute.isReachable()) return;
        if (state.selectedCellX < 0) return;

        profile.advanceGameTime(state.currentRoute.totalMinutes);
        state.charCellX = state.selectedCellX;
        state.charCellY = state.selectedCellY;
        discoverCell(state.charCellX, state.charCellY);
        state.currentRoute = null;

        state.mapOffsetX = state.charCellX - state.getVisibleCellsX() / 2.0f;
        state.mapOffsetY = state.charCellY - state.getVisibleCellsY() / 2.0f;
        state.clampMapOffset();
        Gdx.app.log("MainScreen", "Moved to " + state.charCellX + "," + state.charCellY);
    }

    /**
     * Discovers the building and any hiddenValue==0 improvements at the given cell.
     * Called on arrival (game start or Move To).
     */
    private void discoverCell(int x, int y) {
        Cell cell = cityMap.getCell(x, y);
        if (!cell.hasBuilding()) return;
        Building building = cell.getBuilding();
        building.discover();
        for (Improvement imp : building.getImprovements()) {
            if (imp.getHiddenValue() == 0) imp.discover();
        }
        Gdx.app.log("MainScreen", "Discovered " + x + "," + y + ": " + building.getName());
    }
}

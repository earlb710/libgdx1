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
    private BitmapFont    tinyFont;
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
    private UnitInteriorPopup unitInteriorPopup;
    private TirednessPopup    tirednessPopup;
    private HelpPopup         helpPopup;

    // Input state
    private InputProcessor previousInputProcessor;
    private boolean infoAreaPressed = false;
    private float   infoTouchStartX, infoTouchStartY;
    private float   infoScrollDragStartScrollY, infoScrollDragStartScrollX;
    private boolean isDragging = false;
    private float   dragStartX, dragStartY;
    private float   dragStartOffsetX, dragStartOffsetY;
    // Context menu (double-click)
    private final ContextMenu      contextMenu        = new ContextMenu();
    private final List<String>     contextMenuItems   = new ArrayList<>();
    private final List<Runnable>   contextMenuActions = new ArrayList<>();
    private long  lastMapTapTimeMs = 0L;
    private float lastMapTapX      = -1f;
    private float lastMapTapY      = -1f;

    // Quit confirmation (ESC key)
    private boolean quitConfirming   = false;
    private float   quitYesBtnX, quitYesBtnY, quitYesBtnW, quitYesBtnH;
    private float   quitNoBtnX,  quitNoBtnY,  quitNoBtnW,  quitNoBtnH;

    // Tuning constants
    private static final float MIN_ZOOM             = 1.0f;
    private static final float MAX_ZOOM             = 5.33f;
    private static final float ZOOM_SPEED           = 0.15f;
    private static final float SCROLL_SPEED         = 0.5f;
    private static final float TAP_THRESHOLD_PIXELS = 10f;
    private static final long  DOUBLE_CLICK_MS      = 400L;
    private static final float DOUBLE_CLICK_PX      = 20f;

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
        tinyFont      = game.getFontManager().getTinyFont();

        GameDataManager gameData = game.getGameDataManager();
        cityMap = new CityMap(profile, gameData);
        Gdx.app.log("MainScreen", "CityMap generated: " + cityMap);

        // Pick a random building as the starting cell
        List<Cell> buildingCells = new ArrayList<>();
        List<Cell> officeCells   = new ArrayList<>();
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell c = cityMap.getCell(x, y);
                if (c.getTerrainType() == TerrainType.BUILDING) {
                    buildingCells.add(c);
                    if (c.hasBuilding() && c.getBuilding().getDefinition() != null
                            && "office_building_small".equals(c.getBuilding().getDefinition().getId())) {
                        officeCells.add(c);
                    }
                }
            }
        }

        // Mark a random small office building as home + owned, and use it as the start position
        List<Cell> homeSrc = officeCells.isEmpty() ? buildingCells : officeCells;
        if (!homeSrc.isEmpty()) {
            Cell homeCell = homeSrc.get(new Random(profile.getRandSeed() + 13)
                    .nextInt(homeSrc.size()));
            state.homeCellX = homeCell.getX();
            state.homeCellY = homeCell.getY();
            Building homeBuilding = homeCell.getBuilding();
            homeBuilding.setHome(true);
            homeBuilding.setOwned(true);
            discoverCell(state.homeCellX, state.homeCellY);
            // Assign a random floor and unit letter for the player's office
            Random officeRng = new Random(profile.getRandSeed() + 17);
            int floors = homeBuilding.getFloors();
            state.homeFloor      = floors > 1 ? 1 + officeRng.nextInt(floors) : 1;
            state.homeUnitLetter = (char) ('A' + officeRng.nextInt(26));
            state.charCellX     = state.homeCellX;
            state.charCellY     = state.homeCellY;
            state.selectedCellX = state.homeCellX;
            state.selectedCellY = state.homeCellY;
            Gdx.app.log("MainScreen", "Home/Start: " + state.homeCellX + "," + state.homeCellY
                    + " Floor " + state.homeFloor + " Unit " + state.homeFloor + state.homeUnitLetter);
        } else if (!buildingCells.isEmpty()) {
            // Fallback: no home found, use any building cell
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
        mapRenderer = new MapRenderer(batch, shapeRenderer, font, smallFont, tinyFont, glyphLayout, cityMap);
        mapRenderer.loadBuildingIcons();

        String iconName = profile.getCharacterIcon();
        if (iconName != null && !iconName.isEmpty()) {
            Texture charTex = TextureUtils.makeNegative("character/" + iconName + ".png");
            charTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            mapRenderer.setCharacterIconTexture(charTex);
        }

        infoPanelRenderer = new InfoPanelRenderer(batch, shapeRenderer, font, smallFont, tinyFont,
                glyphLayout, cityMap, profile);

        lookAroundPopup = new LookAroundPopup(batch, shapeRenderer, font, smallFont,
                glyphLayout, cityMap, profile);

        unitInteriorPopup = new UnitInteriorPopup(batch, shapeRenderer, font, smallFont,
                glyphLayout, profile);

        tirednessPopup = new TirednessPopup(batch, shapeRenderer, font, smallFont, glyphLayout);

        helpPopup = new HelpPopup(batch, shapeRenderer, font, smallFont, glyphLayout);

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
        if (state.isWalking) updateWalk(delta);
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);

        mapRenderer.drawMap(state);
        mapRenderer.drawRulers(state);
        infoPanelRenderer.drawInfoBar(state);
        infoPanelRenderer.drawInfoBlock(state, !lookAroundPopup.isVisible());

        if (lookAroundPopup.isVisible()) {
            lookAroundPopup.update(delta);
            lookAroundPopup.draw(state.screenWidth, state.screenHeight, state.infoAreaHeight);
        }

        if (state.unitInteriorOpen && !lookAroundPopup.isVisible()) {
            unitInteriorPopup.draw(state);
        }

        if (tirednessPopup.isVisible()) {
            tirednessPopup.draw(state.screenWidth, state.screenHeight);
        }

        if (contextMenu.isVisible()) {
            contextMenu.draw(batch, shapeRenderer, font, glyphLayout);
        }

        if (state.helpVisible) {
            helpPopup.draw(state.screenWidth, state.screenHeight, state.infoAreaHeight);
        }

        if (quitConfirming) {
            drawQuitConfirmation();
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
    // Quit confirmation overlay
    // -------------------------------------------------------------------------

    private void drawQuitConfirmation() {
        int sw = state.screenWidth;
        int sh = state.screenHeight;
        final float PAD = 32f;
        final Color BG      = new Color(0.08f, 0.08f, 0.12f, 0.96f);
        final Color BORDER  = new Color(0.9f,  0.3f,  0.3f,  1f);

        TextMeasurer.TextBounds yesBounds = TextMeasurer.measure(font, glyphLayout, "YES", 40f, 18f);
        TextMeasurer.TextBounds noBounds  = TextMeasurer.measure(font, glyphLayout, "NO",  40f, 18f);

        glyphLayout.setText(font, "Exit : Are you sure?");
        float msgW  = glyphLayout.width;
        float msgH  = glyphLayout.height;
        float gap   = 24f;
        float btnGap = 20f;
        float totalBtnW = yesBounds.width + btnGap + noBounds.width;
        float boxW  = Math.max(msgW + 2 * PAD, totalBtnW + 2 * PAD);
        float boxH  = PAD + msgH + gap + yesBounds.height + PAD;
        float boxX  = (sw - boxW) / 2f;
        float boxY  = (sh - boxH) / 2f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BG);
        shapeRenderer.rect(boxX, boxY, boxW, boxH);

        // Compute and store button rects
        float btnY   = boxY + PAD;
        float btnX0  = boxX + (boxW - totalBtnW) / 2f;
        quitYesBtnX = btnX0;               quitYesBtnY = btnY;
        quitYesBtnW = yesBounds.width;     quitYesBtnH = yesBounds.height;
        quitNoBtnX  = btnX0 + yesBounds.width + btnGap; quitNoBtnY = btnY;
        quitNoBtnW  = noBounds.width;      quitNoBtnH  = noBounds.height;

        shapeRenderer.setColor(0.2f, 0.6f, 0.2f, 1f);
        shapeRenderer.rect(quitYesBtnX, quitYesBtnY, quitYesBtnW, quitYesBtnH);
        shapeRenderer.setColor(0.6f, 0.15f, 0.15f, 1f);
        shapeRenderer.rect(quitNoBtnX,  quitNoBtnY,  quitNoBtnW,  quitNoBtnH);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(BORDER);
        shapeRenderer.rect(boxX,     boxY,     boxW,     boxH);
        shapeRenderer.rect(boxX + 1, boxY + 1, boxW - 2, boxH - 2);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(quitYesBtnX, quitYesBtnY, quitYesBtnW, quitYesBtnH);
        shapeRenderer.rect(quitNoBtnX,  quitNoBtnY,  quitNoBtnW,  quitNoBtnH);
        shapeRenderer.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Exit : Are you sure?",
                boxX + (boxW - msgW) / 2f,
                boxY + boxH - PAD);
        font.draw(batch, "YES",
                quitYesBtnX + (quitYesBtnW - yesBounds.textWidth) / 2f,
                quitYesBtnY + (quitYesBtnH + yesBounds.textHeight) / 2f);
        font.draw(batch, "NO",
                quitNoBtnX + (quitNoBtnW - noBounds.textWidth) / 2f,
                quitNoBtnY + (quitNoBtnH + noBounds.textHeight) / 2f);
        batch.end();
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private void setupInput() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                int flippedY = state.screenHeight - screenY;

                // Walking animation blocks all input
                if (state.isWalking) { return true; }

                // Quit confirmation overlay blocks all interaction
                if (quitConfirming) { return true; }

                // Any look-around popup blocks normal interaction
                if (lookAroundPopup.isVisible()) {
                    infoAreaPressed  = true;
                    infoTouchStartX  = screenX;
                    infoTouchStartY  = screenY;
                    isDragging       = false;
                    return true;
                }

                // Tiredness popup blocks all normal interaction until dismissed
                if (tirednessPopup.isVisible()) {
                    infoAreaPressed = true;
                    infoTouchStartX = screenX;
                    infoTouchStartY = screenY;
                    isDragging      = false;
                    return true;
                }

                // Left-click with context menu visible – record position for tap detection
                if (contextMenu.isVisible()) {
                    dragStartX = screenX;
                    dragStartY = screenY;
                    return true;
                }

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

                // Walking animation blocks all input
                if (state.isWalking) {
                    infoAreaPressed = false;
                    isDragging = false;
                    return true;
                }

                // Quit confirmation overlay – handle Yes/No only
                if (quitConfirming) {
                    if (screenX >= quitYesBtnX && screenX <= quitYesBtnX + quitYesBtnW
                            && flippedY >= quitYesBtnY && flippedY <= quitYesBtnY + quitYesBtnH) {
                        Gdx.app.log("MainScreen", "Quit confirmed");
                        Gdx.app.exit();
                    } else if (screenX >= quitNoBtnX && screenX <= quitNoBtnX + quitNoBtnW
                            && flippedY >= quitNoBtnY && flippedY <= quitNoBtnY + quitNoBtnH) {
                        quitConfirming = false;
                    }
                    infoAreaPressed = false;
                    isDragging = false;
                    return true;
                }

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

                // Tiredness popup: only the OK button can dismiss it
                if (tirednessPopup.isVisible()) {
                    if (infoAreaPressed) {
                        float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                        if (d < TAP_THRESHOLD_PIXELS) tirednessPopup.onTap(screenX, flippedY);
                        infoAreaPressed = false;
                    }
                    isDragging = false;
                    return true;
                }

                // Left-click: handle context menu first
                if (contextMenu.isVisible()) {
                    float d = Vector2.len(screenX - dragStartX, screenY - dragStartY);
                    if (d < TAP_THRESHOLD_PIXELS) {
                        int idx = contextMenu.onTap(screenX, flippedY);
                        if (idx >= 0 && idx < contextMenuActions.size()) {
                            contextMenuActions.get(idx).run();
                        }
                    }
                    contextMenu.dismiss();
                    isDragging = false;
                    infoAreaPressed = false;
                    return true;
                }

                if (isDragging && flippedY > state.infoAreaHeight) {
                    float d = Vector2.len(screenX - dragStartX, screenY - dragStartY);
                    if (d < TAP_THRESHOLD_PIXELS) {
                        selectCellAt(screenX, flippedY);
                        // Double-click detection: two taps close together in time and space
                        long  now = System.currentTimeMillis();
                        float dd  = Vector2.len(screenX - lastMapTapX, screenY - lastMapTapY);
                        if (now - lastMapTapTimeMs < DOUBLE_CLICK_MS && dd < DOUBLE_CLICK_PX
                                && !lookAroundPopup.isVisible()) {
                            contextMenu.dismiss();
                            buildContextMenu(screenX, flippedY);
                            Gdx.app.log("MainScreen", "Double-click menu at " + screenX + "," + flippedY
                                    + " items=" + contextMenuItems.size());
                            lastMapTapTimeMs = 0L; // reset so triple-click doesn't re-trigger
                        } else {
                            lastMapTapTimeMs = now;
                            lastMapTapX      = screenX;
                            lastMapTapY      = screenY;
                        }
                    }
                }
                if (infoAreaPressed) {
                    float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                    if (d < TAP_THRESHOLD_PIXELS) {
                        checkTabClick(screenX, flippedY);
                        checkUnitExitButtonClick(screenX, flippedY);
                        checkMoveToButtonClick(screenX, flippedY);
                        checkLookAroundButtonClick(screenX, flippedY);
                        checkRestButtonClick(screenX, flippedY);
                        checkSleepButtonClick(screenX, flippedY);
                        checkGoToOfficeButtonClick(screenX, flippedY);
                        checkHelpButtonClick(screenX, flippedY);
                    }
                    infoAreaPressed = false;
                }
                isDragging = false;
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (contextMenu.isVisible()) {
                    contextMenu.dismiss();
                    return true;
                }
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
                contextMenu.dismiss();
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
        // ESC → show quit confirmation (second ESC cancels)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            quitConfirming = !quitConfirming;
        }
        if (quitConfirming) return; // block all other keys while confirming
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
    // Context menu
    // -------------------------------------------------------------------------

    /**
     * Builds and shows the context menu for the currently selected cell.
     * Must be called after {@link #selectCellAt} (which sets selectedCellX/Y and route).
     *
     * @param menuScreenX x pixel where the menu should appear
     * @param menuFlippedY y pixel where the menu should appear (y-up)
     */
    private void buildContextMenu(float menuScreenX, float menuFlippedY) {
        int cx = state.selectedCellX;
        int cy = state.selectedCellY;
        if (cx < 0 || cy < 0) return;

        contextMenuItems.clear();
        contextMenuActions.clear();

        boolean isCharCell = cx == state.charCellX && cy == state.charCellY;

        if (!isCharCell) {
            // "Move To" — always show; label differs based on reachability (mirrors info panel)
            boolean reachable = state.currentRoute != null && state.currentRoute.isReachable();
            String label = reachable
                    ? "Move To (" + state.currentRoute.formatTime() + ")"
                    : "Move To (Unreachable)";
            contextMenuItems.add(label);
            contextMenuActions.add(this::handleMoveToClick);  // always; handleMoveToClick re-checks at call time
        } else {
            // Actions available at the current location
            boolean atHome = cx == state.homeCellX && cy == state.homeCellY;
            Cell cell = cityMap.getCell(cx, cy);
            if (cell.hasBuilding() && cell.getBuilding().isDiscovered()) {
                Building b = cell.getBuilding();
                if (b.hasUndiscoveredImprovements()) {
                    contextMenuItems.add("Look Around (10 min)");
                    contextMenuActions.add(() -> lookAroundPopup.start(state.charCellX, state.charCellY));
                }
                if (atHome && b.getFloors() > 1) {
                    String officeLabel = "Go to your office : "
                            + floorOrdinal(state.homeFloor) + " Floor"
                            + " Unit " + state.homeFloor + state.homeUnitLetter;
                    contextMenuItems.add(officeLabel);
                    contextMenuActions.add(() -> {
                        state.unitInteriorLabel = "Your Office \u2014 " + floorOrdinal(state.homeFloor)
                                + " Floor  Unit " + state.homeFloor + state.homeUnitLetter;
                        state.unitInteriorOpen = true;
                    });
                }
            }
        }

        if (!contextMenuItems.isEmpty()) {
            contextMenu.show(menuScreenX, menuFlippedY, contextMenuItems,
                    font, glyphLayout, state.screenWidth, state.screenHeight);
        }
    }

    /** Returns the ordinal string for a 1-based floor number: 1→"1st", 2→"2nd", 3→"3rd", etc. */
    private static String floorOrdinal(int n) {
        return InfoPanelRenderer.floorOrdinal(n);
    }

    // -------------------------------------------------------------------------
    // Button hit-testing
    // -------------------------------------------------------------------------

    private void checkTabClick(int screenX, int flippedY) {
        if (state.tabH <= 0) return;
        String[] tabIds = { "INFO", "CHARACTER" };
        for (int i = 0; i < tabIds.length; i++) {
            if (state.tabW[i] <= 0) continue;
            if (screenX >= state.tabX[i] && screenX <= state.tabX[i] + state.tabW[i]
                    && flippedY >= state.tabY[i] && flippedY <= state.tabY[i] + state.tabH) {
                if (!tabIds[i].equals(state.activeInfoTab)) {
                    state.activeInfoTab = tabIds[i];
                    state.infoScrollY = 0f;
                    state.infoScrollX = 0f;
                    Gdx.app.log("MainScreen", "Info tab switched to " + tabIds[i]);
                }
                return;
            }
        }
    }

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
            if (checkTirednessBeforeAction()) return;
            lookAroundPopup.start(state.charCellX, state.charCellY);
        }
    }

    private void checkRestButtonClick(int screenX, int flippedY) {
        if (state.restBtnW <= 0) return;
        if (screenX >= state.restBtnX && screenX <= state.restBtnX + state.restBtnW
                && flippedY >= state.restBtnY && flippedY <= state.restBtnY + state.restBtnH) {
            handleRestClick();
        }
    }

    private void checkSleepButtonClick(int screenX, int flippedY) {
        if (state.sleepBtnW <= 0) return;
        if (screenX >= state.sleepBtnX && screenX <= state.sleepBtnX + state.sleepBtnW
                && flippedY >= state.sleepBtnY && flippedY <= state.sleepBtnY + state.sleepBtnH) {
            handleSleepClick();
        }
    }

    private void checkGoToOfficeButtonClick(int screenX, int flippedY) {
        if (state.goToOfficeBtnW <= 0) return;
        if (screenX >= state.goToOfficeBtnX && screenX <= state.goToOfficeBtnX + state.goToOfficeBtnW
                && flippedY >= state.goToOfficeBtnY && flippedY <= state.goToOfficeBtnY + state.goToOfficeBtnH) {
            state.unitInteriorLabel = "Your Office \u2014 " + floorOrdinal(state.homeFloor)
                    + " Floor  Unit " + state.homeFloor + state.homeUnitLetter;
            state.unitInteriorOpen = true;
            Gdx.app.log("MainScreen", "Entered office: " + state.unitInteriorLabel);
        }
    }

    private void checkUnitExitButtonClick(int screenX, int flippedY) {
        if (!state.unitInteriorOpen) return;
        if (state.unitExitBtnW > 0
                && screenX >= state.unitExitBtnX && screenX <= state.unitExitBtnX + state.unitExitBtnW
                && flippedY >= state.unitExitBtnY && flippedY <= state.unitExitBtnY + state.unitExitBtnH) {
            state.unitInteriorOpen = false;
            Gdx.app.log("MainScreen", "Exited unit");
        }
    }

    private void checkHelpButtonClick(int screenX, int flippedY) {
        // If the popup is visible, let it handle the tap first (its own "?" close button).
        if (state.helpVisible && helpPopup.onTap(screenX, flippedY)) {
            state.helpVisible = false;
            return;
        }
        if (state.helpBtnW <= 0) return;
        if (screenX >= state.helpBtnX && screenX <= state.helpBtnX + state.helpBtnW
                && flippedY >= state.helpBtnY && flippedY <= state.helpBtnY + state.helpBtnH) {
            state.helpVisible = !state.helpVisible;
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

    private void handleRestClick() {
        profile.addStamina(2);
        profile.advanceGameTime(60);
        Gdx.app.log("MainScreen", "Rested 1 hour, +2 stamina");
    }

    private void handleSleepClick() {
        int hour   = profile.getCurrentHour();
        int minute = profile.getCurrentMinute();
        int minutesSleep;
        if (hour >= 20) {
            // e.g. 22:30 → sleep until 06:00 next day
            minutesSleep = (24 - hour) * 60 - minute + 6 * 60;
        } else {
            // hour < 5 → sleep until 06:00 same day
            minutesSleep = (6 - hour) * 60 - minute;
        }
        if (minutesSleep <= 0) minutesSleep = 1;
        float hoursSlept  = minutesSleep / 60.0f;
        float fraction    = Math.min(1.0f, hoursSlept / 8.0f);
        int   staminaGain = Math.round(profile.getMaxStamina() * fraction);
        profile.addStamina(staminaGain);
        profile.advanceGameTime(minutesSleep);
        Gdx.app.log("MainScreen", "Slept " + minutesSleep + " min (to 6:00), +" + staminaGain + " stamina");
    }

    private void handleMoveToClick() {
        if (state.currentRoute == null || !state.currentRoute.isReachable()) return;
        if (state.selectedCellX < 0) return;
        if (checkTirednessBeforeAction()) return;

        // Advance game time and stamina immediately
        profile.advanceGameTime(state.currentRoute.totalMinutes);
        profile.useStamina(2);

        // Start walk animation along the route path
        state.walkPath     = state.currentRoute.path;
        state.walkStepIdx  = 1;  // index 0 is the current (start) cell – skip it
        state.walkTimer    = MapViewState.WALK_STEP_SECONDS;
        state.isWalking    = true;
        state.currentRoute = null;
        state.unitInteriorOpen = false;
        contextMenu.dismiss();
        Gdx.app.log("MainScreen", "Walk started, steps=" + state.walkPath.size());
    }

    /** Called every frame while {@code state.isWalking} is true. */
    private void updateWalk(float delta) {
        state.walkTimer -= delta;
        if (state.walkTimer > 0f) return;

        // Advance to next cell in path
        if (state.walkStepIdx >= state.walkPath.size()) {
            // Shouldn't normally reach here, but guard anyway
            state.isWalking = false;
            return;
        }

        int[] cell = state.walkPath.get(state.walkStepIdx);
        state.charCellX = cell[0];
        state.charCellY = cell[1];

        // Centre the map on the current walk cell
        state.mapOffsetX = state.charCellX - state.getVisibleCellsX() / 2.0f;
        state.mapOffsetY = state.charCellY - state.getVisibleCellsY() / 2.0f;
        state.clampMapOffset();

        state.walkStepIdx++;

        if (state.walkStepIdx >= state.walkPath.size()) {
            // Reached destination – always discover
            state.isWalking = false;
            state.walkPath  = null;
            discoverCell(state.charCellX, state.charCellY);
            Gdx.app.log("MainScreen", "Walk complete, arrived at "
                    + state.charCellX + "," + state.charCellY);
        } else {
            // 10% chance of discovering an intermediate cell while passing through
            if (MathUtils.random() < 0.10f) {
                discoverCell(state.charCellX, state.charCellY);
            }
            state.walkTimer = MapViewState.WALK_STEP_SECONDS;
        }
    }

    /**
     * Checks if the character is too tired (stamina &lt;= 2) to perform a stamina-using action.
     * If too tired: automatically routes the player home, advances game time by the travel
     * cost, then applies rest (+2 stamina, +60 min) or sleep (until 06:00) depending on the
     * time of day.  Shows the tiredness popup to explain what happened.
     *
     * @return {@code true} if the action should be blocked (player was too tired);
     *         {@code false} if the action can proceed normally.
     */
    private boolean checkTirednessBeforeAction() {
        if (profile.getCurrentStamina() > 2) return false;
        if (state.homeCellX < 0) return false; // no home assigned yet

        // Calculate travel time from current position to home
        int travelMinutes = 0;
        if (state.charCellX != state.homeCellX || state.charCellY != state.homeCellY) {
            CityMap.RouteResult homeRoute = cityMap.findFastestRoute(
                    state.charCellX, state.charCellY, state.homeCellX, state.homeCellY);
            if (homeRoute != null && homeRoute.isReachable()) {
                travelMinutes = homeRoute.totalMinutes;
            }
        }

        // Advance time for travel and teleport home
        if (travelMinutes > 0) profile.advanceGameTime(travelMinutes);
        state.charCellX     = state.homeCellX;
        state.charCellY     = state.homeCellY;
        state.selectedCellX = state.homeCellX;
        state.selectedCellY = state.homeCellY;
        state.unitInteriorOpen = false;
        state.currentRoute  = null;
        discoverCell(state.charCellX, state.charCellY);

        // Centre map on home
        state.mapOffsetX = state.charCellX - state.getVisibleCellsX() / 2.0f;
        state.mapOffsetY = state.charCellY - state.getVisibleCellsY() / 2.0f;
        state.clampMapOffset();

        // Determine rest or sleep based on the time AFTER travel
        int hour = profile.getCurrentHour();
        boolean nighttime = (hour >= 20 || hour < 5);

        List<String> msgLines = new ArrayList<>();
        if (travelMinutes > 0) {
            msgLines.add("You collapsed on the way home.");
            msgLines.add("Travel time lost: " + formatMinutes(travelMinutes) + ".");
        } else {
            msgLines.add("You are too exhausted to continue.");
        }

        if (nighttime) {
            int curHour = profile.getCurrentHour();
            int curMin  = profile.getCurrentMinute();
            int minutesSleep = (curHour >= 20)
                    ? (24 - curHour) * 60 - curMin + 6 * 60
                    : (6 - curHour) * 60 - curMin;
            if (minutesSleep <= 0) minutesSleep = 1;
            float fraction    = Math.min(1.0f, minutesSleep / (8f * 60f));
            int   staminaGain = Math.round(profile.getMaxStamina() * fraction);
            profile.addStamina(staminaGain);
            profile.advanceGameTime(minutesSleep);
            msgLines.add("You slept until 06:00.");
            msgLines.add("+" + staminaGain + " stamina restored.");
        } else {
            profile.addStamina(2);
            profile.advanceGameTime(60);
            msgLines.add("You rested for 1 hour at home.");
            msgLines.add("+2 stamina restored.");
        }

        tirednessPopup.show(msgLines);
        Gdx.app.log("MainScreen", "Tiredness triggered: travel=" + travelMinutes + "min");
        return true;
    }

    /** Formats a minute count as a compact string, e.g. "1h 30min" or "45 min". */
    private static String formatMinutes(int totalMinutes) {
        int hours = totalMinutes / 60;
        int mins  = totalMinutes % 60;
        if (hours == 0) return mins + " min";
        if (mins  == 0) return hours + "h";
        return hours + "h " + mins + "min";
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

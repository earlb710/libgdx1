package eb.framework1.screen;

import eb.framework1.*;
import eb.framework1.character.*;
import eb.framework1.city.*;
import eb.framework1.generator.*;
import eb.framework1.investigation.*;
import eb.framework1.phone.*;
import eb.framework1.popup.*;
import eb.framework1.save.*;
import eb.framework1.schedule.*;
import eb.framework1.shop.*;
import eb.framework1.ui.*;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
    private BitmapFont    boldSmallFont;
    private BitmapFont    tinyFont;
    private BitmapFont    noteFont;
    private GlyphLayout   glyphLayout;
    private boolean initialized = false;

    // Data
    private CityMap          cityMap;
    private Profile          profile;
    private NovelTextEngine  novelTextEngine;

    // Shared view state (layout, pan, zoom, selection, button bounds)
    private final MapViewState state = new MapViewState();

    // Rendering helpers
    private MapRenderer       mapRenderer;
    private InfoPanelRenderer infoPanelRenderer;
    private LookAroundPopup   lookAroundPopup;
    private UnitInteriorPopup unitInteriorPopup;
    private TirednessPopup    tirednessPopup;
    private RestingPopup      restingPopup;
    private HelpPopup         helpPopup;
    private DiscoveryPopup    discoveryPopup;
    private ServiceResultPopup serviceResultPopup;
    private StashPopup         stashPopup;
    private PutInStashPopup    putInStashPopup;
    private HotelReceptionPopup hotelReceptionPopup;
    private GymInstructorPopup  gymInstructorPopup;
    private ShopPopup            shopPopup;
    private List<ShopItem>       shopItems = new ArrayList<>();
    private EmailPopup           emailPopup;
    private PhonePopup           phonePopup;
    private ConfirmPopup         confirmDropPopup;
    private NotePopup            notePopup;
    private MeetPopup            meetPopup;
    private NpcAnnotationPopup   npcAnnotationPopup;
    /** The appointment currently shown in meetPopup; null when no meeting is open. */
    private CalendarEntry        currentMeetAppt;
    /**
     * Case pre-generated when the meeting popup is opened, before the player
     * has tapped Accept or Reject.  Stored here so its meeting dialogue can be
     * shown during the appointment and so it can be added to the profile on
     * Accept without generating a second, different case.  Cleared on
     * Accept (transferred to profile) or Reject/Close (discarded).
     */
    private CaseFile             pendingCase;
    /** The emails generated for today's inbox; null or empty = none generated yet today. */
    private java.util.List<EmailPopup.EmailData> todaysEmails = new java.util.ArrayList<>();
    /** Per-email accept/decline statuses; kept in sync with todaysEmails so re-opens restore marks. */
    private int[] todaysEmailStatuses = new int[0];

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
    private long lastMapTapTimeMs  = 0L;
    private int  lastMapTapCellX   = -1;
    private int  lastMapTapCellY   = -1;

    /** Services offered by the building at the player's current cell; refreshed each frame by InfoPanelRenderer
     *  via svcBtnCount.  Kept here so tap-handling can look up the correct BuildingService by index. */
    private List<BuildingService> currentCellServices = new ArrayList<>();

    // Quit confirmation (ESC key)
    private boolean quitConfirming   = false;
    private float   quitYesBtnX, quitYesBtnY, quitYesBtnW, quitYesBtnH;
    private float   quitNoBtnX,  quitNoBtnY,  quitNoBtnW,  quitNoBtnH;

    // Save-done notification overlay (dismissed by the OK button)
    private boolean saveDoneVisible = false;
    private float   saveDoneOkBtnX, saveDoneOkBtnY, saveDoneOkBtnW, saveDoneOkBtnH;

    private final SaveGameManager saveGameManager = new SaveGameManager();

    // Tuning constants
    private static final float MIN_ZOOM             = 1.0f;
    private static final float MAX_ZOOM             = 5.33f;
    private static final float ZOOM_SPEED           = 0.15f;
    private static final float SCROLL_SPEED         = 0.5f;
    private static final float TAP_THRESHOLD_PIXELS = 10f;
    private static final long  DOUBLE_CLICK_MS      = 400L;
    private static final String BUILDING_ID_COFFEE_SHOP   = "coffee_shop";
    private static final String BUILDING_ID_SECURITY_SHOP = "security_shop";

    // Prices for purchasable gear at the security shop (item name → cost in $)
    private static final java.util.Map<String, Integer> GEAR_PRICES;
    static {
        java.util.Map<String, Integer> m = new java.util.LinkedHashMap<>();
        m.put("Pistol",       350);
        m.put("Binoculars",   120);
        m.put("Camera",       200);
        m.put("Pepper Spray",  50);
        GEAR_PRICES = java.util.Collections.unmodifiableMap(m);
    }

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
        boldSmallFont = game.getFontManager().getBoldSmallFont();
        tinyFont      = game.getFontManager().getTinyFont();
        noteFont      = game.getFontManager().getNoteFont();
        tinyFont.getData().markupEnabled = true;  // enables colour markup tags in tinyFont draw calls

        GameDataManager gameData = game.getGameDataManager();
        novelTextEngine = gameData.getNovelTextEngine();
        cityMap = new CityMap(profile, gameData);
        Gdx.app.log("MainScreen", "CityMap generated: " + cityMap);

        // Resume from the last save if one exists, otherwise start fresh
        GameSave save = saveGameManager.loadGame(profile.getCharacterName());
        if (save != null) {
            save.applyToProfile(profile);
            save.applyToMap(cityMap);
            state.charCellX     = save.getCharCellX();
            state.charCellY     = save.getCharCellY();
            state.homeCellX     = save.getHomeCellX();
            state.homeCellY     = save.getHomeCellY();
            state.selectedCellX = state.charCellX;
            state.selectedCellY = state.charCellY;
            // Restore in-memory home-building flags and compute floor/unit from seed
            Cell homeCell = cityMap.getCell(state.homeCellX, state.homeCellY);
            if (homeCell.hasBuilding()) {
                Building homeBuilding = homeCell.getBuilding();
                homeBuilding.setHome(true);
                Random officeRng = new Random(profile.getRandSeed() + 17);
                int floors = homeBuilding.getFloors();
                state.homeFloor      = floors > 1 ? 1 + officeRng.nextInt(floors) : 1;
                state.homeUnitLetter = (char) ('A' + officeRng.nextInt(26));
            }
            Gdx.app.log("MainScreen", "Resumed from save: char=" + state.charCellX + "," + state.charCellY
                    + " home=" + state.homeCellX + "," + state.homeCellY);
        } else {
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

            discoverStartingBuildings();

            // Generate world-population NPCs for a fresh game
            PersonNameGenerator png = (gameData != null) ? gameData.getPersonNameGenerator() : null;
            if (png != null) {
                NpcGenerator npcGen = new NpcGenerator(png, new Random(profile.getRandSeed() + 31));
                for (int i = 0; i < 20; i++) {
                    NpcCharacter npc = npcGen.generateWorldNpc(cityMap);
                    profile.addWorldNpc(npc);
                }
                Gdx.app.log("MainScreen", "Generated " + profile.getWorldNpcs().size() + " world NPCs");
            }
        }

        // Populate allNpcs from the profile's world NPC list (restored from save or freshly generated)
        state.allNpcs.clear();
        state.allNpcs.addAll(profile.getWorldNpcs());

        // Build rendering helpers
        mapRenderer = new MapRenderer(batch, shapeRenderer, font, smallFont, tinyFont, glyphLayout, cityMap);
        mapRenderer.loadBuildingIcons();

        String iconName = profile.getCharacterIcon();
        if (iconName != null && !iconName.isEmpty()) {
            Texture charTex = TextureUtils.loadAsIs("character/" + iconName + ".png");
            charTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            mapRenderer.setCharacterIconTexture(charTex);
        }

        infoPanelRenderer = new InfoPanelRenderer(batch, shapeRenderer, font, smallFont, boldSmallFont, tinyFont, noteFont,
                glyphLayout, cityMap, profile, novelTextEngine);

        lookAroundPopup = new LookAroundPopup(batch, shapeRenderer, font, smallFont,
                glyphLayout, cityMap, profile, novelTextEngine);

        unitInteriorPopup = new UnitInteriorPopup(batch, shapeRenderer, font, smallFont,
                glyphLayout, profile);

        tirednessPopup = new TirednessPopup(batch, shapeRenderer, font, smallFont, glyphLayout);

        restingPopup = new RestingPopup(batch, shapeRenderer, font, glyphLayout);

        helpPopup = new HelpPopup(batch, shapeRenderer, font, smallFont, glyphLayout);

        discoveryPopup = new DiscoveryPopup(batch, shapeRenderer, font, smallFont, boldSmallFont, glyphLayout);

        serviceResultPopup = new ServiceResultPopup(batch, shapeRenderer, font, smallFont, glyphLayout);

        stashPopup = new StashPopup(batch, shapeRenderer, font, smallFont, glyphLayout, profile);

        putInStashPopup = new PutInStashPopup(batch, shapeRenderer, font, smallFont, glyphLayout, profile);

        hotelReceptionPopup = new HotelReceptionPopup(batch, shapeRenderer, font, smallFont, glyphLayout);

        gymInstructorPopup = new GymInstructorPopup(batch, shapeRenderer, font, smallFont, glyphLayout);

        shopPopup = new ShopPopup(batch, shapeRenderer, font, smallFont, glyphLayout);

        emailPopup = new EmailPopup(batch, shapeRenderer, font, smallFont, glyphLayout);

        phonePopup = new PhonePopup(batch, shapeRenderer, font, smallFont, glyphLayout);

        confirmDropPopup = new ConfirmPopup(batch, shapeRenderer, font, smallFont, glyphLayout);

        notePopup = new NotePopup(batch, shapeRenderer, font, smallFont, glyphLayout);

        meetPopup = new MeetPopup(batch, shapeRenderer, font, smallFont, glyphLayout);

        npcAnnotationPopup = new NpcAnnotationPopup(batch, shapeRenderer, font, smallFont, glyphLayout);

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

        // Keep the NPC-overlay hour in sync with the current in-game time so that
        // stick figures are repositioned on the map every time the clock advances.
        state.currentHour = profile.getCurrentHour();

        mapRenderer.drawMap(state);
        mapRenderer.drawRulers(state);
        infoPanelRenderer.drawInfoBar(state);
        infoPanelRenderer.drawInfoBlock(state, !lookAroundPopup.isVisible());

        // Keep currentCellServices in sync with the service buttons just rendered.
        if (state.charCellX >= 0 && state.charCellY >= 0
                && state.selectedCellX == state.charCellX
                && state.selectedCellY == state.charCellY) {
            Cell svcCell = cityMap.getCell(state.charCellX, state.charCellY);
            if (svcCell.hasBuilding() && svcCell.getBuilding().isDiscovered()) {
                currentCellServices = BuildingServices.getServices(svcCell.getBuilding());
            } else {
                currentCellServices = java.util.Collections.emptyList();
            }
        } else {
            currentCellServices = java.util.Collections.emptyList();
        }

        if (isAnyPopupVisible()) {
            drawDimOverlay(state.screenWidth, state.screenHeight);
        }

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

        if (restingPopup.isVisible()) {
            restingPopup.update(delta);
            restingPopup.draw(state.screenWidth, state.screenHeight);
        }

        if (discoveryPopup.isVisible()) {
            discoveryPopup.draw(state.screenWidth, state.screenHeight);
        }

        if (serviceResultPopup.isVisible()) {
            serviceResultPopup.draw(state.screenWidth, state.screenHeight);
        }

        if (stashPopup.isVisible()) {
            stashPopup.draw(state.screenWidth, state.screenHeight);
        }

        if (putInStashPopup.isVisible()) {
            putInStashPopup.draw(state.screenWidth, state.screenHeight);
        }

        if (hotelReceptionPopup.isVisible()) {
            hotelReceptionPopup.draw(state.screenWidth, state.screenHeight);
        }

        if (gymInstructorPopup.isVisible()) {
            gymInstructorPopup.draw(state.screenWidth, state.screenHeight);
        }

        if (shopPopup.isVisible()) {
            shopPopup.draw(state.screenWidth, state.screenHeight);
        }

        if (emailPopup.isVisible()) {
            emailPopup.draw(state.screenWidth, state.screenHeight);
        }

        if (phonePopup.isVisible()) {
            phonePopup.draw(state.screenWidth, state.screenHeight);
        }

        if (confirmDropPopup.isVisible()) {
            confirmDropPopup.draw(state.screenWidth, state.screenHeight);
        }

        if (notePopup.isVisible()) {
            notePopup.update(delta);
            notePopup.draw(state.screenWidth, state.screenHeight);
        }

        if (meetPopup.isVisible()) {
            meetPopup.draw(state.screenWidth, state.screenHeight);
        }

        if (npcAnnotationPopup.isVisible()) {
            npcAnnotationPopup.draw(state.screenWidth, state.screenHeight);
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

        if (saveDoneVisible) {
            drawSaveDonePopup();
        }
    }

    @Override
    public void resize(int width, int height) {
        state.screenWidth    = width;
        state.screenHeight   = height;
        if (state.panelExpanded) {
            state.infoAreaHeight = height - MapViewState.INFO_BAR_HEIGHT;
            state.mapAreaHeight  = 0;
        } else {
            state.infoAreaHeight = (int)(height * state.infoPanelRatio)
                    + (int)(MapViewState.SCROLLBAR_THICKNESS);
            state.mapAreaHeight  = height - state.infoAreaHeight - MapViewState.INFO_BAR_HEIGHT;
        }
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
    // Popup dim overlay
    // -------------------------------------------------------------------------

    /** Returns true if any popup or overlay that should dim the background is visible. */
    private boolean isAnyPopupVisible() {
        return lookAroundPopup.isVisible()
            || state.unitInteriorOpen
            || tirednessPopup.isVisible()
            || restingPopup.isVisible()
            || discoveryPopup.isVisible()
            || serviceResultPopup.isVisible()
            || stashPopup.isVisible()
            || putInStashPopup.isVisible()
            || hotelReceptionPopup.isVisible()
            || gymInstructorPopup.isVisible()
            || shopPopup.isVisible()
            || emailPopup.isVisible()
            || phonePopup.isVisible()
            || confirmDropPopup.isVisible()
            || meetPopup.isVisible()
            || npcAnnotationPopup.isVisible()
            || contextMenu.isVisible()
            || state.helpVisible
            || quitConfirming;
    }

    /** Draws a 50% transparent black overlay over the full screen to dim background content. */
    private void drawDimOverlay(int screenW, int screenH) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
        shapeRenderer.rect(0, 0, screenW, screenH);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
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

    private void drawSaveDonePopup() {
        final float PAD    = 28f;
        final float BTN_GAP = 16f;
        final Color BG     = new Color(0.06f, 0.18f, 0.06f, 0.95f);
        final Color BORDER = new Color(0.3f,  0.8f,  0.3f,  1f);

        TextMeasurer.TextBounds okBounds = TextMeasurer.measure(font, glyphLayout, "OK", 32f, 14f);

        glyphLayout.setText(font, "Game Saved!");
        float msgW = glyphLayout.width;
        float msgH = glyphLayout.height;

        float boxW = Math.max(msgW, okBounds.width) + 2 * PAD;
        float boxH = PAD + msgH + BTN_GAP + okBounds.height + PAD;
        float boxX = (state.screenWidth  - boxW) / 2f;
        float boxY = (state.screenHeight - boxH) / 2f;

        // Store OK button bounds for hit-testing
        saveDoneOkBtnX = boxX + (boxW - okBounds.width) / 2f;
        saveDoneOkBtnY = boxY + PAD;
        saveDoneOkBtnW = okBounds.width;
        saveDoneOkBtnH = okBounds.height;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BG);
        shapeRenderer.rect(boxX, boxY, boxW, boxH);
        shapeRenderer.setColor(0.15f, 0.50f, 0.15f, 1f);
        shapeRenderer.rect(saveDoneOkBtnX, saveDoneOkBtnY, saveDoneOkBtnW, saveDoneOkBtnH);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(BORDER);
        shapeRenderer.rect(boxX,     boxY,     boxW,     boxH);
        shapeRenderer.rect(boxX + 1, boxY + 1, boxW - 2, boxH - 2);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(saveDoneOkBtnX,     saveDoneOkBtnY,     saveDoneOkBtnW,     saveDoneOkBtnH);
        shapeRenderer.rect(saveDoneOkBtnX + 1, saveDoneOkBtnY + 1, saveDoneOkBtnW - 2, saveDoneOkBtnH - 2);
        shapeRenderer.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Game Saved!",
                boxX + (boxW - msgW) / 2f,
                boxY + boxH - PAD);
        font.draw(batch, "OK",
                saveDoneOkBtnX + (saveDoneOkBtnW - okBounds.textWidth) / 2f,
                saveDoneOkBtnY + (saveDoneOkBtnH + okBounds.textHeight) / 2f);
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

                // Save-done popup blocks all interaction until OK is tapped
                if (saveDoneVisible) {
                    infoAreaPressed = true;
                    infoTouchStartX = screenX;
                    infoTouchStartY = screenY;
                    isDragging      = false;
                    return true;
                }

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

                // Resting popup blocks all normal interaction while visible
                if (restingPopup.isVisible()) {
                    infoAreaPressed = true;
                    infoTouchStartX = screenX;
                    infoTouchStartY = screenY;
                    isDragging      = false;
                    return true;
                }

                // Discovery popup blocks all normal interaction until dismissed
                if (discoveryPopup.isVisible()) {
                    infoAreaPressed = true;
                    infoTouchStartX = screenX;
                    infoTouchStartY = screenY;
                    isDragging      = false;
                    return true;
                }

                // Service result popup blocks all normal interaction until dismissed
                if (serviceResultPopup.isVisible()) {
                    infoAreaPressed = true;
                    infoTouchStartX = screenX;
                    infoTouchStartY = screenY;
                    isDragging      = false;
                    return true;
                }

                // Stash popup blocks all normal interaction until dismissed
                if (stashPopup.isVisible()) {
                    infoAreaPressed = true;
                    infoTouchStartX = screenX;
                    infoTouchStartY = screenY;
                    isDragging      = false;
                    return true;
                }

                // Put-in-stash popup blocks all normal interaction until dismissed
                if (putInStashPopup.isVisible()) {
                    infoAreaPressed = true;
                    infoTouchStartX = screenX;
                    infoTouchStartY = screenY;
                    isDragging      = false;
                    return true;
                }

                // Confirm drop popup blocks all normal interaction until dismissed
                if (confirmDropPopup.isVisible()) {
                    infoAreaPressed = true;
                    infoTouchStartX = screenX;
                    infoTouchStartY = screenY;
                    isDragging      = false;
                    return true;
                }

                // Hotel reception popup blocks all normal interaction until dismissed
                if (hotelReceptionPopup.isVisible()) {
                    infoAreaPressed = true;
                    infoTouchStartX = screenX;
                    infoTouchStartY = screenY;
                    isDragging      = false;
                    return true;
                }

                // Gym instructor popup blocks all normal interaction until dismissed
                if (gymInstructorPopup.isVisible()) {
                    infoAreaPressed = true;
                    infoTouchStartX = screenX;
                    infoTouchStartY = screenY;
                    isDragging      = false;
                    return true;
                }

                // Shop popup blocks all normal interaction until dismissed
                if (shopPopup.isVisible()) {
                    infoAreaPressed = true;
                    infoTouchStartX = screenX;
                    infoTouchStartY = screenY;
                    isDragging      = false;
                    return true;
                }

                // Email popup blocks all normal interaction until dismissed
                if (emailPopup.isVisible()) {
                    infoAreaPressed = true;
                    infoTouchStartX = screenX;
                    infoTouchStartY = screenY;
                    isDragging      = false;
                    return true;
                }

                // Phone popup blocks all normal interaction until dismissed
                if (phonePopup.isVisible()) {
                    infoAreaPressed = true;
                    infoTouchStartX = screenX;
                    infoTouchStartY = screenY;
                    isDragging      = false;
                    return true;
                }

                // Note popup blocks all normal interaction until dismissed
                if (notePopup.isVisible()) {
                    infoAreaPressed = true;
                    infoTouchStartX = screenX;
                    infoTouchStartY = screenY;
                    isDragging      = false;
                    return true;
                }

                // Meet popup blocks all normal interaction until dismissed
                if (meetPopup.isVisible()) {
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

                if (flippedY > state.screenHeight - MapViewState.INFO_BAR_HEIGHT) {
                    // Top info bar — treat as tap target (not map drag)
                    infoAreaPressed = true;
                    infoTouchStartX = screenX;
                    infoTouchStartY = screenY;
                    isDragging      = false;
                } else if (flippedY > state.infoAreaHeight) {
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

                // Save-done popup – only the OK button dismisses it
                if (saveDoneVisible) {
                    if (infoAreaPressed) {
                        float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                        if (d < TAP_THRESHOLD_PIXELS
                                && screenX >= saveDoneOkBtnX && screenX <= saveDoneOkBtnX + saveDoneOkBtnW
                                && flippedY >= saveDoneOkBtnY && flippedY <= saveDoneOkBtnY + saveDoneOkBtnH) {
                            saveDoneVisible = false;
                        }
                        infoAreaPressed = false;
                    }
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

                // Resting popup: only the OK button (RESULT phase) can dismiss it
                if (restingPopup.isVisible()) {
                    if (infoAreaPressed && !restingPopup.isAnimating()) {
                        float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                        if (d < TAP_THRESHOLD_PIXELS) restingPopup.onTap(screenX, flippedY);
                        infoAreaPressed = false;
                    }
                    isDragging = false;
                    return true;
                }

                // Discovery popup: only the OK button can dismiss it
                if (discoveryPopup.isVisible()) {
                    if (infoAreaPressed) {
                        float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                        if (d < TAP_THRESHOLD_PIXELS) discoveryPopup.onTap(screenX, flippedY);
                        infoAreaPressed = false;
                    }
                    isDragging = false;
                    return true;
                }

                // Service result popup: only the OK button can dismiss it
                if (serviceResultPopup.isVisible()) {
                    if (infoAreaPressed) {
                        float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                        if (d < TAP_THRESHOLD_PIXELS) serviceResultPopup.onTap(screenX, flippedY);
                        infoAreaPressed = false;
                    }
                    isDragging = false;
                    return true;
                }

                // Stash popup: Close, Take, or Put in Stash button
                if (stashPopup.isVisible()) {
                    if (infoAreaPressed) {
                        float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                        if (d < TAP_THRESHOLD_PIXELS) {
                            int result = stashPopup.onTap(screenX, flippedY);
                            if (result >= 0) handleTakeFromStash(result);
                            else if (result == StashPopup.RESULT_PUT_IN_STASH)
                                putInStashPopup.show();
                        }
                        infoAreaPressed = false;
                    }
                    isDragging = false;
                    return true;
                }

                // Put-in-stash popup: stash the selected item or close
                if (putInStashPopup.isVisible()) {
                    if (infoAreaPressed) {
                        float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                        if (d < TAP_THRESHOLD_PIXELS) {
                            int result = putInStashPopup.onTap(screenX, flippedY);
                            if (result >= 0) handleEquipDrop(result);
                        }
                        infoAreaPressed = false;
                    }
                    isDragging = false;
                    return true;
                }

                // Confirm drop popup: Yes drops/stashes the item; No cancels
                if (confirmDropPopup.isVisible()) {
                    if (infoAreaPressed) {
                        float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                        if (d < TAP_THRESHOLD_PIXELS) {
                            int result = confirmDropPopup.onTap(screenX, flippedY);
                            if (result == ConfirmPopup.RESULT_YES) {
                                handleEquipDrop(confirmDropPopup.getPendingIdx());
                            }
                        }
                        infoAreaPressed = false;
                    }
                    isDragging = false;
                    return true;
                }

                // Note popup: checkbox toggles or Save / Cancel
                if (notePopup.isVisible()) {
                    if (infoAreaPressed) {
                        float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                        if (d < TAP_THRESHOLD_PIXELS) {
                            int result = notePopup.onTap(screenX, flippedY);
                            if (result == NotePopup.RESULT_CONFIRM) {
                                state.noteIncludeTime     = notePopup.isIncludeTime();
                                state.noteIncludeLocation = notePopup.isIncludeLocation();
                                Gdx.input.setOnscreenKeyboardVisible(false);
                                submitNoteFromPopup();
                            } else if (result == NotePopup.RESULT_CANCEL) {
                                Gdx.input.setOnscreenKeyboardVisible(false);
                            }
                        }
                        infoAreaPressed = false;
                    }
                    isDragging = false;
                    return true;
                }

                // Hotel reception popup: night selection or cancel
                if (hotelReceptionPopup.isVisible()) {
                    if (infoAreaPressed) {
                        float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                        if (d < TAP_THRESHOLD_PIXELS) {
                            int nights = hotelReceptionPopup.onTap(screenX, flippedY);
                            if (nights >= 1) {
                                handleHotelCheckIn(nights,
                                        hotelReceptionPopup.getDiscountedTotal(nights),
                                        hotelReceptionPopup.getStaminaBonus());
                            }
                        }
                        infoAreaPressed = false;
                    }
                    isDragging = false;
                    return true;
                }

                // Gym instructor popup: training option or cancel
                if (gymInstructorPopup.isVisible()) {
                    if (infoAreaPressed) {
                        float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                        if (d < TAP_THRESHOLD_PIXELS) {
                            int option = gymInstructorPopup.onTap(screenX, flippedY);
                            if (option >= 0) handleGymTraining(option);
                        }
                        infoAreaPressed = false;
                    }
                    isDragging = false;
                    return true;
                }

                // Shop popup: buy an item or close
                if (shopPopup.isVisible()) {
                    if (infoAreaPressed) {
                        float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                        if (d < TAP_THRESHOLD_PIXELS) {
                            int idx = shopPopup.onTap(screenX, flippedY);
                            if (idx >= 0 && idx < shopItems.size()) {
                                handleShopPurchase(shopItems.get(idx), shopPopup.getLastQuantity());
                            }
                        }
                        infoAreaPressed = false;
                    }
                    isDragging = false;
                    return true;
                }

                // Email popup: Accept, Decline, or Close
                if (emailPopup.isVisible()) {
                    if (infoAreaPressed) {
                        float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                        if (d < TAP_THRESHOLD_PIXELS) {
                            int result = emailPopup.onTap(screenX, flippedY);
                            if (result >= 0) handleEmailAccepted(result);
                            // Persist statuses after every interaction so re-opens restore marks
                            todaysEmailStatuses = emailPopup.getStatuses();
                        }
                        infoAreaPressed = false;
                    }
                    isDragging = false;
                    return true;
                }

                // Phone popup: tap a contact to mark as called, or tap power button to close
                if (phonePopup.isVisible()) {
                    if (infoAreaPressed) {
                        float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                        if (d < TAP_THRESHOLD_PIXELS) {
                            int result = phonePopup.onTap(screenX, flippedY);
                            if (result >= 0) handleContactPhoned(result);
                        }
                        infoAreaPressed = false;
                    }
                    isDragging = false;
                    return true;
                }

                // NPC Annotation popup: close button
                if (npcAnnotationPopup.isVisible()) {
                    float d = Vector2.len(screenX - dragStartX, screenY - dragStartY);
                    if (d < TAP_THRESHOLD_PIXELS) {
                        npcAnnotationPopup.onTap(screenX, flippedY);
                    }
                    isDragging = false;
                    infoAreaPressed = false;
                    return true;
                }

                // Meet popup: tap a question button, accept/reject, or close button
                if (meetPopup.isVisible()) {
                    if (infoAreaPressed) {
                        float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                        if (d < TAP_THRESHOLD_PIXELS) {
                            int result = meetPopup.onTap(screenX, flippedY);
                            if (result == MeetPopup.RESULT_ACCEPTED) {
                                handleMeetingAccepted();
                            } else if (result == MeetPopup.RESULT_REJECTED) {
                                handleMeetingRejected();
                            }
                            // RESULT_CLOSED: popup hides but the appointment and
                            // pendingCase are preserved so the player can reopen
                            // the meeting by tapping the Meet button again.
                        }
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
                        if (state.helpVisible) state.helpVisible = false;
                        selectCellAt(screenX, flippedY);
                        // Double-click detection: same cell tapped twice within time window
                        long    now      = System.currentTimeMillis();
                        boolean sameCell = state.selectedCellX == lastMapTapCellX
                                        && state.selectedCellY == lastMapTapCellY;
                        if (now - lastMapTapTimeMs < DOUBLE_CLICK_MS && sameCell
                                && !lookAroundPopup.isVisible()) {
                            contextMenu.dismiss();
                            buildContextMenu(screenX, flippedY);
                            Gdx.app.log("MainScreen", "Double-click menu at " + screenX + "," + flippedY
                                    + " items=" + contextMenuItems.size());
                            lastMapTapTimeMs = 0L;  // reset so triple-click doesn't re-trigger
                            lastMapTapCellX  = -1;
                            lastMapTapCellY  = -1;
                        } else {
                            lastMapTapTimeMs = now;
                            lastMapTapCellX  = state.selectedCellX;
                            lastMapTapCellY  = state.selectedCellY;
                            // Show annotation popup when an untracked NPC is at the tapped cell.
                            // For tracked NPCs the info panel already lists them.
                            NpcCharacter tappedNpc =
                                    npcAtCell(state.selectedCellX, state.selectedCellY);
                            if (tappedNpc != null && !tappedNpc.isTracked()) {
                                npcAnnotationPopup.show(tappedNpc);
                            }
                        }
                    }
                }
                if (infoAreaPressed) {
                    float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                    if (d < TAP_THRESHOLD_PIXELS) {
                        if (state.unitInteriorOpen) {
                            // Office popup covers the info panel — only office buttons are active.
                            // Map-navigation buttons (Look Around, Move To, etc.) must be suppressed
                            // to prevent click-through to the info panel behind the popup.
                            checkUnitExitButtonClick(screenX, flippedY);
                            checkRestButtonClick(screenX, flippedY);
                            checkSleepButtonClick(screenX, flippedY);
                            checkOpenStashButtonClick(screenX, flippedY);
                            checkCheckEmailsButtonClick(screenX, flippedY);
                            checkOpenPhoneButtonClick(screenX, flippedY);
                            checkSaveButtonClick(screenX, flippedY);
                        } else {
                            checkTabClick(screenX, flippedY);
                            checkMoveToButtonClick(screenX, flippedY);
                            checkLookAroundButtonClick(screenX, flippedY);
                            checkGoToOfficeButtonClick(screenX, flippedY);
                            checkGoToHotelRoomButtonClick(screenX, flippedY);
                            checkEquipDropButtonClick(screenX, flippedY);
                            checkHelpButtonClick(screenX, flippedY);
                            checkAddNoteButtonClick(screenX, flippedY);
                            checkServiceButtonClick(screenX, flippedY);
                            checkImprovementButtonClick(screenX, flippedY);
                        }
                        checkTabClick(screenX, flippedY);
                        checkExpandButtonClick(screenX, flippedY);
                        checkAppointmentButtonClick(screenX, flippedY);
                        checkDevModeButtonClick(screenX, flippedY);
                    }
                    infoAreaPressed = false;
                }
                isDragging = false;
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (contextMenu.isVisible()) {
                    // Only dismiss if the finger has moved far enough to be a real drag.
                    // Tiny jitter during a tap should not cancel the menu before touchUp fires.
                    float dragDistance = Vector2.len(screenX - dragStartX, screenY - dragStartY);
                    if (dragDistance >= TAP_THRESHOLD_PIXELS) {
                        contextMenu.dismiss();
                    }
                    return true;
                }
                if (isDragging) {
                    float cs = state.getCellSize();
                    state.mapOffsetX = dragStartOffsetX - (screenX - dragStartX) / cs;
                    state.mapOffsetY = dragStartOffsetY - (screenY - dragStartY) / cs;
                    state.clampMapOffset();
                    return true;
                }
                if (infoAreaPressed && !lookAroundPopup.isVisible() && !discoveryPopup.isVisible()) {
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
                if (state.unitInteriorOpen) {
                    state.infoScrollY = MathUtils.clamp(
                            state.infoScrollY + amountY * 20f, 0f, state.infoMaxScrollY);
                    return true;
                }
                if (discoveryPopup.isVisible()) {
                    discoveryPopup.scroll(amountY * 20f);
                    return true;
                }
                if (shopPopup.isVisible()) {
                    shopPopup.scroll(amountY * 20f);
                    return true;
                }
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

            @Override
            public boolean keyTyped(char character) {
                if (notePopup.isVisible()) return notePopup.keyTyped(character);
                return false;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (notePopup.isVisible()) return notePopup.keyDown(keycode);
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

    /**
     * Returns the first NPC whose current cell matches ({@code cx}, {@code cy}),
     * or {@code null} if no NPC is at that location.
     */
    private NpcCharacter npcAtCell(int cx, int cy) {
        if (state.allNpcs == null || cx < 0 || cy < 0) return null;
        for (NpcCharacter npc : state.allNpcs) {
            if (npc.getCurrentCellX(state.currentHour) == cx
                    && npc.getCurrentCellY(state.currentHour) == cy) {
                return npc;
            }
        }
        return null;
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
                List<BuildingService> services = BuildingServices.getServices(b);
                for (BuildingService svc : services) {
                    final BuildingService captured = svc;
                    contextMenuItems.add(captured.menuLabel());
                    contextMenuActions.add(() -> handleServiceClick(captured));
                }
                if (atHome && b.getFloors() > 1) {
                    String officeLabel = "Go to your office : "
                            + floorOrdinal(state.homeFloor) + " Floor"
                            + " Unit " + state.homeFloor + state.homeUnitLetter;
                    contextMenuItems.add(officeLabel);
                    contextMenuActions.add(() -> {
                        state.unitInteriorLabel = "Your Office \u2014 " + floorOrdinal(state.homeFloor)
                                + " Floor  Unit " + state.homeFloor + state.homeUnitLetter;
                        state.unitInteriorDescription = buildOfficeDescription();
                        state.infoScrollY    = 0f;
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
        String[] tabIds = { "INFO", "CHARACTER", "CALENDAR", "CASE FILE" };
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

    private void checkExpandButtonClick(int screenX, int flippedY) {
        if (state.expandBtnW <= 0) return;
        if (screenX >= state.expandBtnX && screenX <= state.expandBtnX + state.expandBtnW
                && flippedY >= state.expandBtnY && flippedY <= state.expandBtnY + state.expandBtnH) {
            state.panelExpanded = !state.panelExpanded;
            resize(state.screenWidth, state.screenHeight);
            Gdx.app.log("MainScreen", "Panel expanded: " + state.panelExpanded);
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
            state.unitInteriorDescription = buildOfficeDescription();
            state.unitIsHotelRoom = false;
            state.infoScrollY    = 0f;
            state.unitInteriorOpen = true;
            Gdx.app.log("MainScreen", "Entered office: " + state.unitInteriorLabel);
        }
    }

    private void checkGoToHotelRoomButtonClick(int screenX, int flippedY) {
        if (state.goToHotelRoomBtnW <= 0) return;
        if (screenX >= state.goToHotelRoomBtnX && screenX <= state.goToHotelRoomBtnX + state.goToHotelRoomBtnW
                && flippedY >= state.goToHotelRoomBtnY && flippedY <= state.goToHotelRoomBtnY + state.goToHotelRoomBtnH) {
            int roomNum = profile.getAttribute(BuildingServices.ATTR_HOTEL_ROOM);
            Cell cell = cityMap.getCell(state.charCellX, state.charCellY);
            String hotelName = cell.hasBuilding() ? cell.getBuilding().getDisplayName() : "Hotel";
            state.unitInteriorLabel = hotelName + " \u2014 Room " + roomNum;
            state.unitInteriorDescription = buildHotelRoomDescription(cell.hasBuilding() ? cell.getBuilding() : null);
            state.unitIsHotelRoom = true;
            state.infoScrollY    = 0f;
            state.unitInteriorOpen = true;
            Gdx.app.log("MainScreen", "Entered hotel room: " + state.unitInteriorLabel);
        }
    }

    private void checkOpenStashButtonClick(int screenX, int flippedY) {
        if (state.openStashBtnW <= 0) return;
        if (screenX >= state.openStashBtnX && screenX <= state.openStashBtnX + state.openStashBtnW
                && flippedY >= state.openStashBtnY && flippedY <= state.openStashBtnY + state.openStashBtnH) {
            stashPopup.show();
            Gdx.app.log("MainScreen", "Stash opened");
        }
    }

    private void checkCheckEmailsButtonClick(int screenX, int flippedY) {
        if (state.checkEmailsBtnW <= 0) return;
        if (screenX >= state.checkEmailsBtnX && screenX <= state.checkEmailsBtnX + state.checkEmailsBtnW
                && flippedY >= state.checkEmailsBtnY && flippedY <= state.checkEmailsBtnY + state.checkEmailsBtnH) {
            handleCheckEmailsClick();
        }
    }

    private void checkOpenPhoneButtonClick(int screenX, int flippedY) {
        if (state.openPhoneBtnW <= 0) return;
        if (screenX >= state.openPhoneBtnX && screenX <= state.openPhoneBtnX + state.openPhoneBtnW
                && flippedY >= state.openPhoneBtnY && flippedY <= state.openPhoneBtnY + state.openPhoneBtnH) {
            phonePopup.show(buildPhoneContacts());
            Gdx.app.log("MainScreen", "Phone opened with " + phonePopup.getContactCount() + " contact(s)");
        }
    }

    private void checkAppointmentButtonClick(int screenX, int flippedY) {
        if (state.appointmentBtnW <= 0) return;
        if (screenX >= state.appointmentBtnX && screenX <= state.appointmentBtnX + state.appointmentBtnW
                && flippedY >= state.appointmentBtnY && flippedY <= state.appointmentBtnY + state.appointmentBtnH) {
            CalendarEntry appt = findUpcomingAppointment();
            if (appt != null && !appt.contactName.isEmpty()) {
                // Pre-generate the case now (before Accept/Reject) so the meeting dialogue
                // can be shown during the appointment.  The pending case is transferred to
                // the profile on Accept and discarded on Reject/Close.
                if (pendingCase == null
                        || pendingCase.getClientName() == null
                        || !appt.contactName.equalsIgnoreCase(pendingCase.getClientName())) {
                    GameDataManager gdm = game.getGameDataManager();
                    PersonNameGenerator png = (gdm != null) ? gdm.getPersonNameGenerator() : null;
                    if (png != null) {
                        CaseGenerator caseGen = new CaseGenerator(png);
                        pendingCase = caseGen.generate(profile.getGameDateTime());
                        pendingCase.setClientName(appt.contactName);
                        Gdx.app.log("MainScreen", "Pending case pre-generated for: " + appt.contactName);
                    }
                }
                String caseContext = (pendingCase != null && !pendingCase.getObjective().isEmpty())
                        ? pendingCase.getObjective() : "";
                java.util.List<MeetingQA> dialogue = pendingCase != null
                        ? pendingCase.getMeetingDialogue() : java.util.Collections.emptyList();
                currentMeetAppt = appt;
                meetPopup.show(appt, caseContext, dialogue);
                Gdx.app.log("MainScreen", "Meet popup opened for: " + appt.contactName);
            } else {
                // No named contact — fall back to showing the Calendar tab
                state.activeInfoTab = "CALENDAR";
                state.infoScrollY   = 0f;
                state.infoScrollX   = 0f;
                Gdx.app.log("MainScreen", "Appointment button tapped — switching to Calendar tab");
            }
        }
    }

    /**
     * Finds the upcoming appointment at the player's current location within
     * the 3-hour window, mirroring the logic in
     * {@link InfoPanelRenderer#findUpcomingAppointmentAtLocation}.
     */
    private CalendarEntry findUpcomingAppointment() {
        if (state.charCellX < 0 || state.charCellY < 0) return null;
        Cell cell = cityMap.getCell(state.charCellX, state.charCellY);
        String buildingName = (cell != null && cell.hasBuilding() && cell.getBuilding().isDiscovered())
                ? cell.getBuilding().getName() : null;
        boolean atHome = state.charCellX == state.homeCellX && state.charCellY == state.homeCellY;

        long nowMinutes = dateTimeToMinutes(profile.getGameDateTime());
        for (CalendarEntry entry : profile.getCalendarEntries()) {
            boolean locationMatches;
            if ("Your Office".equalsIgnoreCase(entry.location)) {
                locationMatches = atHome;
            } else if (entry.locationCellX >= 0 && entry.locationCellY >= 0) {
                // Match by exact cell coordinates. Works even when the building at
                // those coordinates is undiscovered (e.g. coffee shop not yet visited).
                locationMatches = state.charCellX == entry.locationCellX
                        && state.charCellY == entry.locationCellY;
            } else {
                locationMatches = buildingName != null
                        && buildingName.equalsIgnoreCase(entry.location);
            }
            if (!locationMatches) continue;
            long diff = dateTimeToMinutes(entry.dateTime) - nowMinutes;
            if (diff >= 0 && diff <= 180) return entry;
        }
        return null;
    }

    /**
     * Converts a {@code "YYYY-MM-DD HH:MM"} game date/time string to total
     * minutes, mirroring the helper in {@link InfoPanelRenderer}.
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

    /**
     * Returns the first open {@link CaseFile} whose client name matches
     * {@code contactName}, or {@code null} if none is found.
     */
    private CaseFile findCaseForContact(String contactName) {
        if (contactName == null || contactName.isEmpty()) return null;
        for (CaseFile cf : profile.getCaseFiles()) {
            if (cf.isOpen() && contactName.equalsIgnoreCase(cf.getClientName())) {
                return cf;
            }
        }
        return null;
    }

    /**
     * Returns the objective of the first open {@link CaseFile} whose client
     * name matches {@code contactName}, or an empty string if none is found.
     */
    private String findCaseContextForContact(String contactName) {
        if (contactName == null || contactName.isEmpty()) return "";
        for (CaseFile cf : profile.getCaseFiles()) {
            if (cf.isOpen() && contactName.equalsIgnoreCase(cf.getClientName())) {
                String obj = cf.getObjective();
                return (obj != null) ? obj : "";
            }
        }
        return "";
    }

    /**
     * Called when the player taps "Accept Case".  Uses the pre-generated
     * {@link #pendingCase} (created when the meeting popup was opened) so the
     * accepted case exactly matches the dialogue the player just read.
     * Falls back to generating a fresh case if the pending case is unavailable.
     * Delegates to {@link #handleMeetingClosed()} to record the meeting note
     * and any asked questions.
     */
    private void handleMeetingAccepted() {
        if (pendingCase != null) {
            profile.addCaseFile(pendingCase);
            Gdx.app.log("MainScreen", "Case accepted (pre-generated): " + pendingCase.getName());
            pendingCase = null;
        } else {
            // Fallback: generate a case on-the-spot if no pending case exists
            GameDataManager gdm = game.getGameDataManager();
            PersonNameGenerator png = (gdm != null) ? gdm.getPersonNameGenerator() : null;
            if (png != null) {
                CaseGenerator caseGen = new CaseGenerator(png);
                CaseFile newCase = caseGen.generate(profile.getGameDateTime());
                if (currentMeetAppt != null && !currentMeetAppt.contactName.isEmpty()) {
                    newCase.setClientName(currentMeetAppt.contactName);
                }
                profile.addCaseFile(newCase);
                Gdx.app.log("MainScreen", "Case accepted (fallback-generated): " + newCase.getName());
            } else {
                Gdx.app.log("MainScreen", "Cannot generate case: PersonNameGenerator unavailable");
            }
        }
        handleMeetingClosed();
    }

    /**
     * Called when the player taps "Reject Case".  Discards the pre-generated
     * {@link #pendingCase}, removes the appointment from the calendar (so the
     * Meet button disappears), and clears tracking state.  No meeting notes or
     * clues are recorded.
     */
    private void handleMeetingRejected() {
        pendingCase = null;
        if (currentMeetAppt != null) {
            profile.removeCalendarEntry(currentMeetAppt);
            Gdx.app.log("MainScreen", "Meeting rejected; appointment removed for: "
                    + currentMeetAppt.contactName);
            currentMeetAppt = null;
        }
    }

    /**
     * Called after the player taps "Accept Case" (via {@link #handleMeetingAccepted()}).
     * Finds the newly-added {@link CaseFile} that matches the appointment's contact name
     * and records a meeting note plus a clue entry for every question the
     * player asked during the meeting.  Also removes the completed appointment.
     */
    private void handleMeetingClosed() {
        if (currentMeetAppt == null) return;
        String contactName = currentMeetAppt.contactName;

        // pendingCase is already null here (transferred to the profile in
        // handleMeetingAccepted before this method is called).
        pendingCase = null;

        if (contactName.isEmpty()) {
            profile.removeCalendarEntry(currentMeetAppt);
            currentMeetAppt = null;
            return;
        }

        // Locate the matching open case file (same logic as findCaseContextForContact)
        CaseFile matchingCase = null;
        for (CaseFile cf : profile.getCaseFiles()) {
            if (cf.isOpen() && contactName.equalsIgnoreCase(cf.getClientName())) {
                matchingCase = cf;
                break;
            }
        }
        if (matchingCase == null) {
            Gdx.app.log("MainScreen", "No open case found for contact: " + contactName);
            profile.removeCalendarEntry(currentMeetAppt);
            currentMeetAppt = null;
            return;
        }

        // Add a note recording who was met, when and where
        String gameDateTime = profile.getGameDateTime();
        String location     = currentMeetAppt.location.isEmpty() ? "unknown location" : currentMeetAppt.location;
        matchingCase.addNote("Met " + contactName + " at " + location + " (" + gameDateTime + ")");

        // Add a clue entry for each question the player asked
        for (String question : meetPopup.getAskedQuestions()) {
            matchingCase.addClue("Asked " + contactName + ": \"" + question + "\"");
        }

        // Record a bilateral relationship entry for this first meeting.
        // Try to find the NPC in the world NPC list so the NPC also gets an entry.
        eb.framework1.character.NpcCharacter contactNpc = null;
        for (eb.framework1.character.NpcCharacter npc : state.allNpcs) {
            if (contactName.equalsIgnoreCase(npc.getFullName())) {
                contactNpc = npc;
                break;
            }
        }
        int contactCharisma = (contactNpc != null)
                ? contactNpc.getAttribute(eb.framework1.character.CharacterAttribute.CHARISMA)
                : eb.framework1.character.Relationship.NEUTRAL_CHARISMA;
        String contactId = (contactNpc != null) ? contactNpc.getId() : contactName;
        eb.framework1.character.Relationship.recordMeeting(
                profile, contactId, contactName, contactCharisma, contactNpc);
        Gdx.app.log("MainScreen", "Relationship recorded: player ↔ " + contactName);

        Gdx.app.log("MainScreen", "Case file updated after meeting: " + matchingCase.getName());
        // Remove the completed appointment from the calendar
        profile.removeCalendarEntry(currentMeetAppt);
        currentMeetAppt = null;
    }

    private void checkServiceButtonClick(int screenX, int flippedY) {
        if (state.svcBtnCount <= 0) return;
        for (int i = 0; i < state.svcBtnCount && i < currentCellServices.size(); i++) {
            if (state.svcBtnW[i] <= 0) continue;
            if (screenX >= state.svcBtnX[i] && screenX <= state.svcBtnX[i] + state.svcBtnW[i]
                    && flippedY >= state.svcBtnY[i] && flippedY <= state.svcBtnY[i] + state.svcBtnH) {
                handleServiceClick(currentCellServices.get(i));
                return;
            }
        }
    }

    private void checkImprovementButtonClick(int screenX, int flippedY) {
        if (state.impBtnCount <= 0) return;
        for (int i = 0; i < state.impBtnCount; i++) {
            if (state.impBtnW[i] <= 0 || state.impBtnImp[i] == null) continue;
            if (screenX >= state.impBtnX[i] && screenX <= state.impBtnX[i] + state.impBtnW[i]
                    && flippedY >= state.impBtnY[i] && flippedY <= state.impBtnY[i] + state.impBtnH) {
                handleImprovementUse(state.impBtnImp[i]);
                return;
            }
        }
    }

    private void checkEquipDropButtonClick(int screenX, int flippedY) {
        if (state.equipDropBtnCount <= 0) return;
        for (int i = 0; i < state.equipDropBtnCount && i < MapViewState.MAX_EQUIP_BTNS; i++) {
            if (state.equipDropBtnW[i] <= 0) continue;
            if (screenX >= state.equipDropBtnX[i]
                    && screenX <= state.equipDropBtnX[i] + state.equipDropBtnW[i]
                    && flippedY >= state.equipDropBtnY[i]
                    && flippedY <= state.equipDropBtnY[i] + state.equipDropBtnH) {
                boolean inOffice = state.unitInteriorOpen
                        && state.charCellX == state.homeCellX
                        && state.charCellY == state.homeCellY;
                String action   = inOffice ? "stash" : "drop";
                String itemName = getCarriedItemName(i);
                confirmDropPopup.show("Are you sure?",
                        "Do you want to " + action + ": " + itemName + "?", i);
                return;
            }
        }
    }

    /** Returns the display name of carried item at flat index {@code idx}, or "item" if not found. */
    private String getCarriedItemName(int idx) {
        EquipmentSlot[] mainSlots = { EquipmentSlot.WEAPON, EquipmentSlot.BODY,
                                      EquipmentSlot.LEGS,   EquipmentSlot.FEET };
        List<EquipItem> allItems = new ArrayList<>();
        for (EquipmentSlot slot : mainSlots) {
            EquipItem item = profile.getEquipped(slot);
            if (item != null) allItems.add(item);
        }
        allItems.addAll(profile.getUtilityItems());
        return (idx >= 0 && idx < allItems.size()) ? allItems.get(idx).getName() : "item";
    }

    private void checkUnitExitButtonClick(int screenX, int flippedY) {
        if (!state.unitInteriorOpen) return;
        if (state.unitExitBtnW > 0
                && screenX >= state.unitExitBtnX && screenX <= state.unitExitBtnX + state.unitExitBtnW
                && flippedY >= state.unitExitBtnY && flippedY <= state.unitExitBtnY + state.unitExitBtnH) {
            state.unitInteriorOpen = false;
            state.unitIsHotelRoom  = false;
            Gdx.app.log("MainScreen", "Exited unit");
        }
    }

    private void checkSaveButtonClick(int screenX, int flippedY) {
        if (!state.unitInteriorOpen) return;
        if (state.saveBtnW > 0
                && screenX >= state.saveBtnX && screenX <= state.saveBtnX + state.saveBtnW
                && flippedY >= state.saveBtnY && flippedY <= state.saveBtnY + state.saveBtnH) {
            saveGameManager.saveGame(profile, cityMap,
                    state.charCellX, state.charCellY,
                    state.homeCellX, state.homeCellY);
            saveDoneVisible = true;
            Gdx.app.log("MainScreen", "Game saved from office");
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

    private void checkDevModeButtonClick(int screenX, int flippedY) {
        if (state.devModeBtnW <= 0) return;
        if (screenX >= state.devModeBtnX && screenX <= state.devModeBtnX + state.devModeBtnW
                && flippedY >= state.devModeBtnY && flippedY <= state.devModeBtnY + state.devModeBtnH) {
            state.developerMode = !state.developerMode;
            Gdx.app.log("MainScreen", "Developer mode " + (state.developerMode ? "ON" : "OFF"));
        }
    }

    private void checkAddNoteButtonClick(int screenX, int flippedY) {
        if (state.addNoteBtnW <= 0) return;
        if (screenX >= state.addNoteBtnX && screenX <= state.addNoteBtnX + state.addNoteBtnW
                && flippedY >= state.addNoteBtnY && flippedY <= state.addNoteBtnY + state.addNoteBtnH) {
            CaseFile active = profile.getActiveCaseFile();
            if (active != null) {
                notePopup.show(state.noteIncludeTime, state.noteIncludeLocation);
                Gdx.input.setOnscreenKeyboardVisible(true);
            }
        }
    }

    /** Saves the note typed in the popup to the active case file. */
    private void submitNoteFromPopup() {
        CaseFile active = profile.getActiveCaseFile();
        if (active == null) return;
        String text = notePopup.getNoteText();
        if (text.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        if (state.noteIncludeTime) {
            sb.append("[").append(profile.getGameDateTime()).append("] ");
        }
        if (state.noteIncludeLocation) {
            String loc = getCurrentLocationName();
            if (loc != null && !loc.isEmpty()) {
                sb.append("@ ").append(loc).append(" ");
            }
        }
        sb.append(text);
        active.addNote(sb.toString());
        Gdx.app.log("MainScreen", "Note added to case: " + active.getName());
    }

    /** Returns the building name at the character's current location, or the cell coordinates. */
    private String getCurrentLocationName() {
        if (state.charCellX < 0 || state.charCellY < 0) return "";
        Cell cell = cityMap.getCell(state.charCellX, state.charCellY);
        if (cell != null && cell.hasBuilding() && cell.getBuilding().getName() != null) {
            return cell.getBuilding().getName();
        }
        return "(" + state.charCellX + "," + state.charCellY + ")";
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
        int hourBefore = profile.getCurrentHour();
        int effectiveCap = calculateEffectiveStaminaCap();
        profile.addStaminaUpTo(2, effectiveCap);
        profile.advanceGameTime(60);
        int hourAfter  = profile.getCurrentHour();
        Gdx.app.log("MainScreen", "Rested 1 hour, +2 stamina (cap=" + effectiveCap + ")");

        // Determine if a day-part boundary was crossed during the rest hour
        String resultMsg = null;
        boolean crossedEvening = hourBefore < 18 && hourAfter >= 18;
        boolean crossedMorning = (hourBefore >= 18 || hourBefore < 6) && hourAfter >= 6 && hourAfter < 18;
        if (crossedEvening) {
            resultMsg = "It became night.";
        } else if (crossedMorning) {
            resultMsg = "It became morning.";
        }
        restingPopup.start(resultMsg);
    }

    private void handleSleepClick() {
        int hourBefore = profile.getCurrentHour();
        int hour   = hourBefore;
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

        int effectiveCap = calculateEffectiveStaminaCap();
        int   staminaGain = Math.round(effectiveCap * fraction);

        // 1 dot per hour slept; each dot advances time + fills stamina incrementally
        int animDots        = Math.max(1, Math.round(hoursSlept));
        final int minutesPerDot = minutesSleep / animDots;
        final int staminaPerDot = staminaGain  / animDots;
        final int cap           = effectiveCap;

        // Determine result message without mutating state yet (compute hourAfter arithmetically)
        int hourAfter = ((hour * 60 + minute + minutesSleep) / 60) % 24;
        String resultMsg = null;
        if (hourBefore < 6 && hourAfter >= 6 && hourAfter < 18) {
            resultMsg = "It became morning.";
        } else if (hourBefore >= 20 && hourAfter >= 6 && hourAfter < 18) {
            resultMsg = "It became morning.";
        } else if (hourBefore < 18 && hourAfter >= 18) {
            resultMsg = "It became night.";
        }

        Gdx.app.log("MainScreen", "Slept " + minutesSleep + " min (to 6:00), +"
                + staminaGain + " stamina (cap=" + effectiveCap + ")");

        // Per-dot callback: advance time + fill stamina together so both animate visibly
        restingPopup.start(resultMsg, animDots, "Sleeping", 0.2f, () -> {
            profile.advanceGameTime(minutesPerDot);
            profile.addStaminaUpTo(staminaPerDot, cap);
            profile.removeExpiredCalendarEntries();
        });
    }

    private void handleMoveToClick() {
        if (state.currentRoute == null || !state.currentRoute.isReachable()) return;
        if (state.selectedCellX < 0) return;
        if (checkTirednessBeforeAction()) return;

        // Advance game time and stamina immediately
        profile.advanceGameTime(state.currentRoute.totalMinutes);
        profile.useStamina(2);

        // Seed the traveled path with the starting junction (if not already there)
        if (state.traveledPath == null) state.traveledPath = new java.util.ArrayList<>();
        if (!state.currentRoute.path.isEmpty()) {
            int[] startJunc = state.currentRoute.path.get(0);
            if (state.traveledPath.isEmpty()
                    || state.traveledPath.get(state.traveledPath.size() - 1)[0] != startJunc[0]
                    || state.traveledPath.get(state.traveledPath.size() - 1)[1] != startJunc[1]) {
                state.traveledPath.add(new int[]{startJunc[0], startJunc[1]});
            }
        }

        // Record destination cell so updateWalk can set charCellX/Y on arrival
        state.walkDestCellX = state.selectedCellX;
        state.walkDestCellY = state.selectedCellY;

        // Start walk animation along the route path (junction coordinates)
        state.walkPath     = state.currentRoute.path;
        state.walkStepIdx  = 1;  // index 0 is the start junction – skip it
        state.walkTimer    = MapViewState.WALK_STEP_SECONDS;
        state.isWalking    = true;
        state.currentRoute = null;
        state.unitInteriorOpen = false;
        state.unitIsHotelRoom  = false;
        contextMenu.dismiss();
        Gdx.app.log("MainScreen", "Walk started, steps=" + state.walkPath.size());
    }

    /**
     * Drops or stashes the carried item at the given index in the flat
     * allItems order (main slots first, then utility items, matching the
     * order InfoPanelRenderer uses when drawing the character tab).
     *
     * When the player is inside their home office the item is moved to the
     * stash instead of being permanently deleted.
     */
    private void handleEquipDrop(int idx) {
        EquipmentSlot[] mainSlots = { EquipmentSlot.WEAPON, EquipmentSlot.BODY,
                                      EquipmentSlot.LEGS,   EquipmentSlot.FEET };
        List<EquipItem>      allItems   = new ArrayList<>();
        List<EquipmentSlot>  slotsUsed  = new ArrayList<>();
        for (EquipmentSlot slot : mainSlots) {
            EquipItem item = profile.getEquipped(slot);
            if (item != null) { allItems.add(item); slotsUsed.add(slot); }
        }
        List<EquipItem> utility = new ArrayList<>(profile.getUtilityItems());
        int mainCount = allItems.size();
        allItems.addAll(utility);

        if (idx < 0 || idx >= allItems.size()) return;

        // Case items are locked to active cases and can never be dropped or stashed
        if (allItems.get(idx).isCaseItem()) {
            Gdx.app.log("MainScreen", "Cannot drop case item: " + allItems.get(idx).getName());
            return;
        }

        boolean inOffice = state.unitInteriorOpen
                && state.charCellX == state.homeCellX
                && state.charCellY == state.homeCellY;

        if (idx < mainCount) {
            EquipItem item = allItems.get(idx);
            if (inOffice) profile.addToStash(item);
            profile.unequip(slotsUsed.get(idx));
            Gdx.app.log("MainScreen", (inOffice ? "Stashed" : "Dropped") + " " + item.getName());
        } else {
            int utilIdx = idx - mainCount;
            EquipItem item = utility.get(utilIdx);
            if (inOffice) profile.addToStash(item);
            profile.removeUtilityItemAt(utilIdx);
            Gdx.app.log("MainScreen", (inOffice ? "Stashed" : "Dropped") + " " + item.getName());
        }
    }

    /**
     * Takes the stash item at {@code index} back into the character's inventory.
     * Non-utility items are (re-)equipped in their slot; utility items are added
     * back to the utility list.
     */
    private void handleTakeFromStash(int index) {
        EquipItem item = profile.takeFromStash(index);
        if (item == null) return;
        if (item.getSlot() == EquipmentSlot.UTILITY) {
            profile.addUtilityItem(item);
        } else {
            profile.equip(item);
        }
        Gdx.app.log("MainScreen", "Took from stash: " + item.getName());
    }

    /**
     * Checks the player into a hotel for the given number of nights.
     *
     * <p>Deducts the total cost ({@code nights × nightly}), stores the hotel
     * stamina bonus and remaining nights in the player's profile attributes, and
     * shows a confirmation popup.  If the player can't afford it, an error popup
     * is shown instead.
     *
     * @param nights   number of nights to book (1–3)
     * @param nightly  nightly rate in in-game currency
     * @param bonus    extra stamina awarded per full 8-hour sleep while checked in
     */
    private void handleHotelCheckIn(int nights, int total, int bonus) {
        List<String> resultLines = new ArrayList<>();

        if (total > 0 && profile.getMoney() < total) {
            resultLines.add("You can't afford " + nights
                    + (nights == 1 ? " night" : " nights") + " here.");
            resultLines.add("Required: $" + total
                    + "  |  You have: $" + profile.getMoney());
            serviceResultPopup.show("Not Enough Money", resultLines);
            return;
        }

        if (total > 0) profile.setMoney(profile.getMoney() - total);
        // Check-in time: 15 minutes at the front desk
        profile.advanceGameTime(15);

        profile.setAttribute(BuildingServices.ATTR_HOTEL_BONUS,  bonus);
        profile.setAttribute(BuildingServices.ATTR_HOTEL_NIGHTS, nights);
        int roomNum = (state.charCellX * 7 + state.charCellY * 13) % 99 + 1;
        profile.setAttribute(BuildingServices.ATTR_HOTEL_ROOM, roomNum);

        resultLines.add("You checked in for " + nights
                + (nights == 1 ? " night." : " nights."));
        if (total > 0) resultLines.add("Total cost: $" + total + ".");
        resultLines.add("Sleep bonus: full stamina.");
        resultLines.add("Use the Sleep button when it is time for bed.");
        serviceResultPopup.show("Checked In", resultLines);
        Gdx.app.log("MainScreen", "Hotel check-in: " + nights + "n, $" + total
                + ", bonus=" + bonus);
    }

    /**
     * Executes the chosen gym training option.
     *
     * <p>Training options are identified by the {@code BuildingServices.GYM_OPT_*} constants.
     * The overuse mechanic applies to both strength and stamina training:
     * successive sessions on the same day have reduced chance of success and
     * an increasing risk of a setback.
     *
     * @param option one of {@link BuildingServices#GYM_OPT_STRENGTH_SELF},
     *               {@link BuildingServices#GYM_OPT_STRENGTH_PT},
     *               {@link BuildingServices#GYM_OPT_STAMINA_SELF},
     *               {@link BuildingServices#GYM_OPT_STAMINA_PT}
     */
    private void handleGymTraining(int option) {
        int cost    = (option == BuildingServices.GYM_OPT_STRENGTH_PT
                    || option == BuildingServices.GYM_OPT_STAMINA_PT)
                      ? BuildingServices.GYM_COST_PT : BuildingServices.GYM_COST_SELF;
        int timeMins = (option == BuildingServices.GYM_OPT_STRENGTH_PT
                     || option == BuildingServices.GYM_OPT_STAMINA_PT)
                       ? BuildingServices.GYM_TIME_PT : BuildingServices.GYM_TIME_SELF;

        List<String> resultLines = new ArrayList<>();

        // Affordability check
        if (cost > 0 && profile.getMoney() < cost) {
            resultLines.add("You can't afford this session.");
            resultLines.add("Required: $" + cost + "  |  You have: $" + profile.getMoney());
            serviceResultPopup.show("Not Enough Money", resultLines);
            return;
        }

        // Deduct cost and advance time
        profile.setMoney(profile.getMoney() - cost);
        profile.advanceGameTime(timeMins);

        // Overuse tracking (shared across all gym sessions today)
        int todayDate = BuildingServices.gameDateInt(profile.getGameDateTime());
        int usesToday = BuildingServices.gymUsesToday(profile, todayDate);
        BuildingServices.recordGymUse(profile, todayDate);

        boolean isStrength = (option == BuildingServices.GYM_OPT_STRENGTH_SELF
                           || option == BuildingServices.GYM_OPT_STRENGTH_PT);
        boolean isPT       = (option == BuildingServices.GYM_OPT_STRENGTH_PT
                           || option == BuildingServices.GYM_OPT_STAMINA_PT);
        String attrName  = isStrength ? CharacterAttribute.STRENGTH.name()
                                      : CharacterAttribute.STAMINA.name();
        String attrLabel = isStrength ? "Strength" : "Stamina";
        int    current   = profile.getAttribute(attrName);

        // Base chance from the option, scaled down by overuse
        float baseChance  = isPT ? BuildingServices.GYM_CHANCE_PT : BuildingServices.GYM_CHANCE_SELF;
        float chance;
        float overuseRisk;
        if (usesToday == 0) {
            chance      = baseChance;
            overuseRisk = 0f;
        } else if (usesToday == 1) {
            chance      = baseChance * BuildingServices.GYM_OVERUSE_SECOND_CHANCE_MULT;
            overuseRisk = BuildingServices.GYM_OVERUSE_SECOND_RISK;
        } else {
            chance      = 0f;           // no gain possible on 3rd+ session
            overuseRisk = BuildingServices.GYM_OVERUSE_THIRD_RISK;
        }

        float roll = MathUtils.random();

        if (overuseRisk > 0f && roll < overuseRisk) {
            // Setback from over-training (applies equally to strength and stamina attributes)
            profile.setAttribute(attrName, Math.max(0, current - 1));
            resultLines.add("Over-training! Your body is exhausted.");
            resultLines.add("-1 " + attrLabel + ".");
        } else if (chance > 0f && roll < chance) {
            profile.setAttribute(attrName, current + 1);
            if (isPT) {
                resultLines.add("Excellent session with your trainer!");
            } else {
                resultLines.add("Great workout! Your effort paid off.");
            }
            resultLines.add("+1 " + attrLabel + ".");
        } else if (usesToday >= 2) {
            resultLines.add("You went through the motions, but your body needs rest.");
            resultLines.add("No benefit from additional training today.");
        } else {
            resultLines.add(isPT ? "Good session. Your trainer pushed you hard."
                                 : "Solid workout. Keep the consistency up.");
            resultLines.add("No attribute change this time.");
        }

        resultLines.add("Cost: $" + cost + ".");
        String title = isStrength
                ? (isPT ? "Strength PT Session" : "Strength Training")
                : (isPT ? "Stamina PT Session"  : "Stamina Training");
        serviceResultPopup.show(title, resultLines);
        Gdx.app.log("MainScreen", "Gym training opt=" + option + " cost=$" + cost
                + " uses=" + usesToday + " roll=" + roll);
    }

    /**
     * Executes a building service for the character at the current cell.
     * Deducts money and game-time, applies effects, then shows the result popup.
     */
    private void handleServiceClick(BuildingService svc) {
        List<String> resultLines = new ArrayList<>();

        // Check affordability
        if (svc.cost > 0 && profile.getMoney() < svc.cost) {
            resultLines.add("You can't afford this service.");
            resultLines.add("Required: $" + svc.cost
                    + "  |  You have: $" + profile.getMoney());
            serviceResultPopup.show("Not Enough Money", resultLines);
            return;
        }

        // Deduct cost and advance time
        if (svc.cost > 0) profile.setMoney(profile.getMoney() - svc.cost);
        if (svc.timeCost > 0) profile.advanceGameTime(svc.timeCost);

        String title = svc.name;

        switch (svc.id) {
            // ---- Hotel reception: open the night-selection popup ------------
            case BuildingServices.SVC_HOTEL_RECEPTION: {
                Cell cell = cityMap.getCell(state.charCellX, state.charCellY);
                if (!cell.hasBuilding()) break;
                Building hotel = cell.getBuilding();
                hotelReceptionPopup.show(
                        hotel.getDisplayName(),
                        BuildingServices.getHotelRoomType(hotel),
                        BuildingServices.getHotelNightlyCost(hotel),
                        BuildingServices.getHotelStaminaBonus(hotel),
                        buildHotelRoomDescription(hotel),
                        profile.getAttribute(BuildingServices.ATTR_HOTEL_NIGHTS));
                return;  // popup drives the rest; don't show serviceResultPopup now
            }

            // ---- Gym instructor: open the training-option popup -------------
            case BuildingServices.SVC_GYM_INSTRUCTOR: {
                gymInstructorPopup.show();
                return;  // popup drives the rest
            }

            // ---- Food: restore stamina -----------------------------------
            case BuildingServices.SVC_BUY_MEAL: {
                int gain = 2;
                profile.addStamina(gain);
                resultLines.add("You enjoyed a satisfying meal.");
                if (svc.cost > 0) resultLines.add("Cost: $" + svc.cost + ".");
                resultLines.add("+" + gain + " stamina.");
                break;
            }
            case BuildingServices.SVC_BUY_COFFEE: {
                int gain = 1;
                profile.addStamina(gain);
                resultLines.add("You sipped a hot coffee.");
                if (svc.cost > 0) resultLines.add("Cost: $" + svc.cost + ".");
                resultLines.add("+" + gain + " stamina.");
                break;
            }
            case BuildingServices.SVC_FINE_DINING: {
                int gain = 6;
                profile.addStamina(gain);
                resultLines.add("An exquisite meal. You feel refreshed and energised.");
                if (svc.cost > 0) resultLines.add("Cost: $" + svc.cost + ".");
                resultLines.add("+" + gain + " stamina.");
                break;
            }

            // ---- Medicine / healthcare: show shop popup --------------------
            case BuildingServices.SVC_BUY_MEDICINE: {
                Cell cell = cityMap.getCell(state.charCellX, state.charCellY);
                Building b = cell.hasBuilding() ? cell.getBuilding() : null;
                shopItems = new ArrayList<>(BuildingServices.getShopItems(b));
                if (shopItems.isEmpty()) {
                    shopItems.add(new ShopItem("Medicine", "Over-the-counter supplies.", svc.cost > 0 ? svc.cost : 20, true, 4));
                }
                shopPopup.show(BuildingServices.getShopTitle(b, svc.id), shopItems);
                return;
            }
            case BuildingServices.SVC_DOCTOR: {
                int before = profile.getCurrentStamina();
                profile.addStamina(profile.getMaxStamina());
                int gained = profile.getCurrentStamina() - before;
                resultLines.add("You received full medical attention.");
                if (svc.cost > 0) resultLines.add("Cost: $" + svc.cost + ".");
                resultLines.add("+" + gained + " stamina restored.");
                break;
            }

            // ---- Library / education: chance to gain INTELLIGENCE ----------
            case BuildingServices.SVC_LIBRARY_STUDY:
            case BuildingServices.SVC_ATTEND_CLASS: {
                float gainChance = BuildingServices.SVC_LIBRARY_STUDY.equals(svc.id) ? 0.25f : 0.30f;
                String intAttr = CharacterAttribute.INTELLIGENCE.name();
                int currentInt = profile.getAttribute(intAttr);
                if (MathUtils.random() < gainChance) {
                    profile.setAttribute(intAttr, currentInt + 1);
                    resultLines.add("Something clicked. You feel sharper.");
                    resultLines.add("+1 Intelligence.");
                } else {
                    resultLines.add("A productive session. Knowledge builds slowly.");
                    resultLines.add("No attribute change this time.");
                }
                if (svc.cost > 0) resultLines.add("Cost: $" + svc.cost + ".");
                break;
            }

            // ---- Entertainment: stamina restoration -----------------------
            case BuildingServices.SVC_ENTERTAINMENT: {
                int gain = 3;
                profile.addStamina(gain);
                resultLines.add("You had a great time!");
                if (svc.cost > 0) resultLines.add("Cost: $" + svc.cost + ".");
                resultLines.add("+" + gain + " stamina.");
                break;
            }

            // ---- Religious service: small stamina bonus -------------------
            case BuildingServices.SVC_ATTEND_SERVICE: {
                int gain = 2;
                profile.addStamina(gain);
                resultLines.add("You feel a sense of peace and calm.");
                resultLines.add("+" + gain + " stamina.");
                break;
            }

            // ---- Haircut --------------------------------------------------
            case BuildingServices.SVC_HAIRCUT: {
                resultLines.add("You look sharp and feel confident.");
                if (svc.cost > 0) resultLines.add("Cost: $" + svc.cost + ".");
                break;
            }

            // ---- Security shop: show shop popup ----------------------------
            case BuildingServices.SVC_BUY_GEAR: {
                Cell cell = cityMap.getCell(state.charCellX, state.charCellY);
                Building b = cell.hasBuilding() ? cell.getBuilding() : null;
                shopItems = new ArrayList<>(BuildingServices.getShopItems(b));
                shopPopup.show(BuildingServices.getShopTitle(b, svc.id), shopItems,
                        carriedItemNames());
                return;
            }

            // ---- Supply / Retail: show shop popup --------------------------
            case BuildingServices.SVC_BUY_SNACKS: {
                Cell cell = cityMap.getCell(state.charCellX, state.charCellY);
                Building b = cell.hasBuilding() ? cell.getBuilding() : null;
                shopItems = new ArrayList<>(BuildingServices.getShopItems(b));
                if (shopItems.isEmpty()) {
                    shopItems.add(new ShopItem("Snack", "Quick bite to eat.", svc.cost > 0 ? svc.cost : 4, true, 1));
                }
                shopPopup.show(BuildingServices.getShopTitle(b, svc.id), shopItems);
                return;
            }
            case BuildingServices.SVC_BUY_SUPPLIES: {
                Cell cell = cityMap.getCell(state.charCellX, state.charCellY);
                Building b = cell.hasBuilding() ? cell.getBuilding() : null;
                shopItems = new ArrayList<>(BuildingServices.getShopItems(b));
                if (shopItems.isEmpty()) {
                    shopItems.add(new ShopItem("Supplies", "Everyday essentials.", svc.cost > 0 ? svc.cost : 10, true, 1));
                }
                shopPopup.show(BuildingServices.getShopTitle(b, svc.id), shopItems);
                return;
            }

            // ---- Laundromat -----------------------------------------------
            case BuildingServices.SVC_LAUNDRY: {
                int gain = 2;
                profile.addStamina(gain);
                resultLines.add("Your clothes are clean and fresh.");
                if (svc.cost > 0) resultLines.add("Cost: $" + svc.cost + ".");
                resultLines.add("+" + gain + " stamina.");
                break;
            }

            default:
                resultLines.add("Service completed.");
                break;
        }

        serviceResultPopup.show(title, resultLines);
        Gdx.app.log("MainScreen", "Service used: " + svc.id);
    }

    /**
     * Processes a gear purchase at the security shop.
     *
     * <p>Iterates the {@link #GEAR_PRICES} catalogue.  For each item the player
     * can afford and does not already carry, the first affordable item is
     * purchased automatically and feedback is added to {@code resultLines}.
     * If the player already owns all available items the result says so.</p>
     *
     * <p>This is intentionally simple (auto-buys first affordable item).
     * A full shop UI can be added later if needed.</p>
     */
    private void handleBuyGear(List<String> resultLines) {
        // Build a set of names already carried (main slots + utility)
        java.util.Set<String> carried = new java.util.HashSet<>();
        EquipmentSlot[] mainSlots = { EquipmentSlot.WEAPON, EquipmentSlot.BODY,
                                      EquipmentSlot.LEGS,   EquipmentSlot.FEET };
        for (EquipmentSlot slot : mainSlots) {
            EquipItem item = profile.getEquipped(slot);
            if (item != null) carried.add(item.getName());
        }
        for (EquipItem item : profile.getUtilityItems()) carried.add(item.getName());

        // Find purchasable items (not already carried, in catalogue)
        List<String> available = new ArrayList<>();
        for (String name : GEAR_PRICES.keySet()) {
            if (!carried.contains(name)) available.add(name);
        }

        if (available.isEmpty()) {
            resultLines.add("You already own everything in stock.");
            return;
        }

        // List what's for sale along with prices
        resultLines.add("Items available:");
        for (String name : available) {
            int price = GEAR_PRICES.get(name);
            resultLines.add("  " + name + " — $" + price);
        }
        resultLines.add("");

        // Auto-purchase first affordable item
        for (String name : available) {
            int price = GEAR_PRICES.get(name);
            if (profile.getMoney() >= price) {
                // Find the catalogue item
                EquipItem bought = null;
                for (EquipmentSlot slot : mainSlots) {
                    EquipItem candidate = EquipItem.findByName(name, slot);
                    if (candidate != null) { bought = candidate; break; }
                }
                if (bought == null) bought = EquipItem.findByName(name, EquipmentSlot.UTILITY);
                if (bought == null) {
                    Gdx.app.log("MainScreen", "WARN: gear '" + name + "' in GEAR_PRICES not found in EquipItem catalogue");
                    continue;
                }

                profile.setMoney(profile.getMoney() - price);
                if (bought.getSlot() == EquipmentSlot.UTILITY) {
                    profile.addUtilityItem(bought);
                } else {
                    profile.equip(bought);
                }
                resultLines.add("Purchased: " + name + " for $" + price + ".");
                resultLines.add("Remaining balance: $" + profile.getMoney() + ".");
                Gdx.app.log("MainScreen", "Gear purchased: " + name + " for $" + price);
                return;
            }
        }

        // Can't afford anything
        resultLines.add("You can't afford any items right now.");
        resultLines.add("You have: $" + profile.getMoney() + ".");
    }

    /**
     * Handles a "Buy Supplies" service at retail/convenience stores.
     * Currently shows a list of purchasable supply items, or
     * "Nothing of interest at the moment" when none are available.
     */
    private void handleBuySupplies(BuildingService svc, List<String> resultLines) {
        // No supply catalogue items defined yet — show placeholder
        resultLines.add("Nothing of interest at the moment.");
    }

    /**
     * Returns the set of item names currently carried by the player
     * (equipped main-slot items plus utility items).  Used to gray out
     * already-owned items in the shop popup.
     */
    private Set<String> carriedItemNames() {
        Set<String> names = new HashSet<>();
        EquipmentSlot[] mainSlots = { EquipmentSlot.WEAPON, EquipmentSlot.BODY,
                                      EquipmentSlot.LEGS,   EquipmentSlot.FEET };
        for (EquipmentSlot slot : mainSlots) {
            EquipItem equipped = profile.getEquipped(slot);
            if (equipped != null) names.add(equipped.getName());
        }
        for (EquipItem item : profile.getUtilityItems()) {
            if (item != null) names.add(item.getName());
        }
        return names;
    }

    /**
     * Processes a purchase made through the shop popup.
     *
     * <p>For gear items the player does not yet carry, the item is equipped and
     * money is deducted.  For consumable items the selected quantity is purchased
     * all at once, stamina is added, and money is deducted.  If the player cannot
     * afford the purchase a result popup is shown instead.
     *
     * @param item     the {@link ShopItem} the player chose to buy
     * @param quantity number of units (always 1 for non-consumables)
     */
    private void handleShopPurchase(ShopItem item, int quantity) {
        int qty        = item.consumable ? Math.max(1, quantity) : 1;
        int totalCost  = item.price * qty;

        if (profile.getMoney() < totalCost) {
            List<String> lines = new ArrayList<>();
            lines.add("You can't afford " + item.name + ".");
            lines.add("Cost: $" + totalCost + "  |  You have: $" + profile.getMoney());
            serviceResultPopup.show("Not Enough Money", lines);
            return;
        }

        profile.setMoney(profile.getMoney() - totalCost);

        if (item.consumable) {
            int totalStamina = item.staminaGain * qty;
            if (totalStamina > 0) profile.addStamina(totalStamina);
            List<String> lines = new ArrayList<>();
            lines.add("You bought " + qty + "× " + item.name + " for $" + totalCost + ".");
            if (totalStamina > 0) lines.add("+" + totalStamina + " stamina.");
            lines.add("Remaining balance: $" + profile.getMoney() + ".");
            serviceResultPopup.show("Purchase Complete", lines);
        } else {
            // Non-consumable: treat as gear / equipment
            EquipmentSlot[] mainSlots = { EquipmentSlot.WEAPON, EquipmentSlot.BODY,
                                          EquipmentSlot.LEGS,   EquipmentSlot.FEET };
            EquipItem bought = null;
            for (EquipmentSlot slot : mainSlots) {
                EquipItem candidate = EquipItem.findByName(item.name, slot);
                if (candidate != null) { bought = candidate; break; }
            }
            if (bought == null) bought = EquipItem.findByName(item.name, EquipmentSlot.UTILITY);

            List<String> lines = new ArrayList<>();
            if (bought != null) {
                if (bought.getSlot() == EquipmentSlot.UTILITY) {
                    profile.addUtilityItem(bought);
                } else {
                    profile.equip(bought);
                }
                lines.add("Purchased: " + item.name + " for $" + totalCost + ".");
            } else {
                // Unknown gear item — still deduct money but don't add to inventory
                lines.add("Purchased: " + item.name + " for $" + totalCost + ".");
                Gdx.app.log("MainScreen", "WARN: shop item '" + item.name
                        + "' not found in EquipItem catalogue");
            }
            lines.add("Remaining balance: $" + profile.getMoney() + ".");
            serviceResultPopup.show("Purchase Complete", lines);
        }

        Gdx.app.log("MainScreen", "Shop purchase: " + item.name
                + " x" + qty + " for $" + totalCost);
    }

    /** Called every frame while {@code state.isWalking} is true. */
    private void updateWalk(float delta) {
        state.walkTimer -= delta;
        if (state.walkTimer > 0f) return;

        // Advance to next junction in path
        if (state.walkStepIdx >= state.walkPath.size()) {
            // Shouldn't normally reach here, but guard anyway
            state.isWalking = false;
            return;
        }

        int[] junc = state.walkPath.get(state.walkStepIdx);
        int jx = junc[0], jy = junc[1];

        // Accumulate this junction into the persistent traveled path
        if (state.traveledPath == null) state.traveledPath = new java.util.ArrayList<>();
        state.traveledPath.add(new int[]{jx, jy});

        // Place character icon on the road (junction coordinates)
        state.charJuncX = jx;
        state.charJuncY = jy;

        // Keep charCellX/Y roughly tracking position for other game logic
        state.charCellX = Math.min(jx, CityMap.MAP_SIZE - 1);
        state.charCellY = Math.min(jy, CityMap.MAP_SIZE - 1);

        // Centre the map on the current junction position
        state.mapOffsetX = jx - state.getVisibleCellsX() / 2.0f;
        state.mapOffsetY = jy - state.getVisibleCellsY() / 2.0f;
        state.clampMapOffset();

        state.walkStepIdx++;

        if (state.walkStepIdx >= state.walkPath.size()) {
            // Reached destination – move portrait back onto the cell
            state.isWalking  = false;
            state.walkPath   = null;
            state.charJuncX  = -1f;
            state.charJuncY  = -1f;
            state.traveledPath.clear();
            state.charCellX = state.walkDestCellX;
            state.charCellY = state.walkDestCellY;
            state.mapOffsetX = state.charCellX - state.getVisibleCellsX() / 2.0f;
            state.mapOffsetY = state.charCellY - state.getVisibleCellsY() / 2.0f;
            state.clampMapOffset();
            boolean newlyDiscovered = discoverCell(state.charCellX, state.charCellY);
            applyHotelArrivalBonus(state.charCellX, state.charCellY);
            showDiscoveryPopup(state.charCellX, state.charCellY, newlyDiscovered);
            Gdx.app.log("MainScreen", "Walk complete, arrived at "
                    + state.charCellX + "," + state.charCellY);
        } else {
            // Discover the two cells on either side of the current road segment (10% each).
            // Determine direction from the previous junction to the current one.
            int[] prevJunc = state.walkPath.get(state.walkStepIdx - 2);
            int djx = jx - prevJunc[0];
            int djy = jy - prevJunc[1];
            int side1CX, side1CY, side2CX, side2CY;
            if (djy == 0) {
                // Horizontal movement: side cells are above and below junction row jy
                side1CX = Math.min(jx, CityMap.MAP_SIZE - 1);
                side1CY = Math.min(jy, CityMap.MAP_SIZE - 1);      // cell above road
                side2CX = Math.min(jx, CityMap.MAP_SIZE - 1);
                side2CY = Math.max(jy - 1, 0);                     // cell below road
            } else {
                // Vertical movement: side cells are left and right of junction column jx
                side1CX = Math.min(jx, CityMap.MAP_SIZE - 1);      // cell to the right
                side1CY = Math.min(jy, CityMap.MAP_SIZE - 1);
                side2CX = Math.max(jx - 1, 0);                     // cell to the left
                side2CY = Math.min(jy, CityMap.MAP_SIZE - 1);
            }
            if (side1CX >= 0 && side1CX < CityMap.MAP_SIZE
                    && side1CY >= 0 && side1CY < CityMap.MAP_SIZE) {
                if (MathUtils.random() < 0.10f) discoverCell(side1CX, side1CY);
            }
            if (side2CX >= 0 && side2CX < CityMap.MAP_SIZE
                    && side2CY >= 0 && side2CY < CityMap.MAP_SIZE) {
                if (MathUtils.random() < 0.10f) discoverCell(side2CX, side2CY);
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
        state.unitIsHotelRoom  = false;
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
            profile.removeExpiredCalendarEntries();
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
     * Returns the total STAMINA attribute modifier granted by the building (and
     * its discovered improvements) at the character's current cell, or 0 if the
     * cell has no discovered building.
     *
     * <p>Mirrors {@code InfoPanelRenderer.locationModFor} but lives in MainScreen
     * so rest / sleep handlers can use it without cross-class coupling.</p>
     */
    private int getLocationStaminaMod() {
        if (state.charCellX < 0 || state.charCellY < 0) return 0;
        Cell cell = cityMap.getCell(state.charCellX, state.charCellY);
        if (!cell.hasBuilding() || !cell.getBuilding().isDiscovered()) return 0;
        Building b = cell.getBuilding();
        int total = b.getAttributeModifiers().getOrDefault(CharacterAttribute.STAMINA, 0);
        for (Improvement imp : b.getImprovements()) {
            if (imp.isDiscovered()) {
                total += imp.getAttributeModifiers().getOrDefault(CharacterAttribute.STAMINA, 0);
            }
        }
        return total;
    }

    /**
     * Returns the effective stamina cap at the character's current location:
     * {@code max(10, (staminaAttr + locationMod) × 10)}.
     *
     * <p>The multiplier of 10 converts each STAMINA attribute point (and each
     * location modifier point) into 10 stamina pool points.</p>
     */
    private int calculateEffectiveStaminaCap() {
        int locMod = getLocationStaminaMod();
        // 10 stamina pool points per STAMINA attribute (and per location modifier) point
        return Math.max(10, (profile.getAttribute(CharacterAttribute.STAMINA.name()) + locMod) * 10);
    }

    /**
     * Discovers the building and any hiddenValue==0 improvements at the given cell.
     * Called on arrival (game start or Move To).
     *
     * @return {@code true} if the building at this cell was newly discovered
     *         (it was not discovered before this call); {@code false} otherwise.
     */
    private boolean discoverCell(int x, int y) {
        Cell cell = cityMap.getCell(x, y);
        if (!cell.hasBuilding()) return false;
        Building building = cell.getBuilding();
        boolean wasDiscovered = building.isDiscovered();
        building.discover();
        for (Improvement imp : building.getImprovements()) {
            if (imp.getHiddenValue() == 0) imp.discover();
        }
        Gdx.app.log("MainScreen", "Discovered " + x + "," + y + ": " + building.getName());
        return !wasDiscovered;
    }

    /**
     * Applies the hotel tier stamina bonus when the player arrives at a hotel cell.
     * The bonus (+1/+2/+3 by tier) is granted each time the player walks to the hotel,
     * provided they are currently checked in (ATTR_HOTEL_NIGHTS > 0).
     * One night of stay is consumed per arrival bonus.
     */
    private void applyHotelArrivalBonus(int x, int y) {
        Cell cell = cityMap.getCell(x, y);
        if (!cell.hasBuilding()) return;
        Building building = cell.getBuilding();
        int bonus = BuildingServices.getHotelStaminaBonus(building);
        if (bonus <= 0) return;
        int nights = profile.getAttribute(BuildingServices.ATTR_HOTEL_NIGHTS);
        if (nights <= 0) return;
        profile.addStamina(bonus);
        int remaining = nights - 1;
        profile.setAttribute(BuildingServices.ATTR_HOTEL_NIGHTS, remaining);
        if (remaining == 0) profile.setAttribute(BuildingServices.ATTR_HOTEL_BONUS, 0);
        Gdx.app.log("MainScreen", "Hotel arrival bonus: +" + bonus
                + " stamina, " + remaining + " nights remaining");
    }

    /**
     * Builds and shows the discovery popup for a building arrival.
     * Collects all improvements that were auto-discovered (hiddenValue == 0),
     * along with their novel improvement descriptions.
     *
     * @param newDiscovery {@code true} if this is the first time this building is visited
     */
    private void showDiscoveryPopup(int x, int y, boolean newDiscovery) {
        Cell cell = cityMap.getCell(x, y);
        if (!cell.hasBuilding()) return;
        Building building = cell.getBuilding();
        BuildingDefinition def = building.getDefinition();

        // Use buildings_en.json descriptions as the building description; fall back to the static description field if not found
        String description = null;
        if (novelTextEngine != null && def != null) {
            String state = building.getState();
            String raw;
            if (state != null) {
                raw = novelTextEngine.getStateDescription(def.getId(), state);
                // Fall back to contextual description if no state variant exists for this key
                if (raw == null || raw.isEmpty()) {
                    raw = novelTextEngine.getDescription(
                            def.getId(), profile.getCurrentHour(),
                            profile.getAttributes(), profile.getGender());
                }
            } else {
                raw = novelTextEngine.getDescription(
                        def.getId(), profile.getCurrentHour(),
                        profile.getAttributes(), profile.getGender());
            }
            description = (raw != null && !raw.isEmpty()) ? raw : null;
        }
        if (description == null && def != null) {
            description = def.getDescription();
        }

        // Collect auto-discovered improvements (hiddenValue == 0) with their descriptions
        List<String> improvementLines = new ArrayList<>();
        for (Improvement imp : building.getImprovements()) {
            if (imp.getHiddenValue() != 0) continue;
            improvementLines.add("- " + imp.getName());
            ImprovementData impData = imp.getData();
            if (impData != null && !impData.getDescription().isEmpty()) {
                improvementLines.add("  " + impData.getDescription());
            }
        }

        discoveryPopup.show(building.getDisplayName(), building.getName(), description, null,
                improvementLines, newDiscovery);
    }

    /**
     * Returns the nearest cell (by Manhattan distance from the reference point) whose
     * building definition ID matches one of the supplied IDs, or {@code null} if none found.
     */
    private Cell findNearestBuilding(int refX, int refY, String... buildingIds) {
        Cell nearest = null;
        int minDist = Integer.MAX_VALUE;
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = cityMap.getCell(x, y);
                if (!cell.hasBuilding() || cell.getBuilding().getDefinition() == null) continue;
                String id = cell.getBuilding().getDefinition().getId();
                for (String targetId : buildingIds) {
                    if (targetId.equals(id)) {
                        int dist = Math.abs(x - refX) + Math.abs(y - refY);
                        if (dist < minDist) {
                            minDist = dist;
                            nearest = cell;
                        }
                        break;
                    }
                }
            }
        }
        return nearest;
    }

    /** Discovers the cell's building if the cell is non-null. */
    private void discoverCellIfFound(Cell cell) {
        if (cell != null) {
            discoverCell(cell.getX(), cell.getY());
        }
    }

    /**
     * Generates 1–2 random emails (client requests and/or police cases) and
     * opens the email popup so the player can accept or decline each one.
     * Emails are only generated once per in-game day; subsequent calls on the
     * same day show a "no new emails" notice instead.
     */
    private void handleCheckEmailsClick() {
        // Once-per-day guard
        String today = profile.getGameDateTime().substring(0, 10);
        if (today.equals(profile.getLastEmailCheckDate())) {
            // Same day — re-show the existing emails restoring their accept/decline marks
            if (!todaysEmails.isEmpty()) {
                emailPopup.showWithStatus(todaysEmails, todaysEmailStatuses);
                Gdx.app.log("MainScreen", "Re-opening today's inbox (" + todaysEmails.size() + " email(s))");
            } else {
                java.util.List<String> lines = new java.util.ArrayList<>();
                lines.add("No new emails today.");
                lines.add("Check back tomorrow.");
                serviceResultPopup.show("Inbox", lines);
            }
            return;
        }

        // New day — generate fresh emails
        profile.setLastEmailCheckDate(today);
        todaysEmails.clear();
        todaysEmailStatuses = new int[0];

        java.util.Random rng = new java.util.Random(System.currentTimeMillis());

        PersonNameGenerator png = (game.getGameDataManager() != null)
                ? game.getGameDataManager().getPersonNameGenerator() : null;

        // Seed the used-datetime set from already-accepted calendar entries so
        // newly generated appointments never clash with existing ones.
        java.util.Set<String> usedDTs = new java.util.HashSet<>();
        String latestApptDT = null;   // most recent appointment date in the calendar
        for (CalendarEntry ce : profile.getCalendarEntries()) {
            usedDTs.add(ce.dateTime);
            if (latestApptDT == null || ce.dateTime.compareTo(latestApptDT) > 0) {
                latestApptDT = ce.dateTime;
            }
        }
        // Anchor new appointments from the latest existing one (or today if none).
        // nextAppointmentDT adds daysAhead=1 to find the first free slot, and each
        // chosen slot is added to usedDTs — so successive emails in the same batch
        // automatically chain: email 1 → anchor+1, email 2 → anchor+2, etc.
        String apptAnchor = (latestApptDT != null) ? latestApptDT : profile.getGameDateTime();

        int count = 1 + rng.nextInt(3); // 1–3 emails per day
        for (int i = 0; i < count; i++) {
            boolean isPolice = (i > 0) && rng.nextBoolean();
            if (isPolice) {
                String detective = (png != null) ? png.generateFull("M") : "James Carter";
                String dt = nextAppointmentDT(apptAnchor, 1, usedDTs);
                int reward = 500 + rng.nextInt(6) * 100; // $500–$1000
                todaysEmails.add(new EmailPopup.EmailData(
                        "Det. " + detective + " (NYPD)",
                        "Consulting Request \u2014 Homicide",
                        "Detective,\n\nWe need a private investigator to consult on a homicide case.\n"
                                + "Please report to the crime scene at your earliest convenience.\n\n"
                                + "\u2014 Det. " + detective,
                        "NYPD: Crime Scene",
                        dt,
                        "Crime Scene (TBD)",
                        reward, null,
                        -1, -1   // crime scene location unknown at scheduling time
                ));
            } else {
                String gender = rng.nextBoolean() ? "M" : "F";
                String clientName = (png != null) ? png.generateFull(gender) : "Alex Morgan";
                String dt = nextAppointmentDT(apptAnchor, 1, usedDTs);
                boolean atOffice = rng.nextBoolean();
                String loc = atOffice ? "Your Office" : "Downtown Cafe";
                int reward = 200 + rng.nextInt(5) * 100; // $200–$600
                int locCellX, locCellY;
                if (atOffice) {
                    locCellX = state.homeCellX;
                    locCellY = state.homeCellY;
                } else {
                    // Pick the first discovered coffee shop cell as the meeting point
                    int[] cafeCell = findFirstBuildingCell(BUILDING_ID_COFFEE_SHOP);
                    locCellX = cafeCell != null ? cafeCell[0] : -1;
                    locCellY = cafeCell != null ? cafeCell[1] : -1;
                }
                todaysEmails.add(new EmailPopup.EmailData(
                        clientName,
                        "I need your help urgently",
                        "Dear Detective,\n\nI require your assistance with a matter of great urgency.\n"
                                + "Could we meet "
                                + (atOffice ? "at your office" : "at a coffee shop downtown") + "?\n\n"
                                + "Regards,\n" + clientName,
                        "Meeting: " + clientName,
                        dt,
                        loc,
                        reward, null,
                        locCellX, locCellY,
                        clientName, gender
                ));
            }
        }

        emailPopup.show(todaysEmails);
        todaysEmailStatuses = emailPopup.getStatuses();
        Gdx.app.log("MainScreen", "Check emails: " + todaysEmails.size() + " email(s) generated");
    }

    /**
     * Called when the player accepts an email.  Adds the associated appointment
     * to the profile's calendar.
     *
     * @param emailIndex index in the email popup's list (from {@link EmailPopup#onTap})
     */
    private void handleEmailAccepted(int emailIndex) {
        EmailPopup.EmailData email = emailPopup.getEmailAt(emailIndex);
        if (email == null) return;
        // Guard against duplicate entries (e.g. accept → close → reopen → accept again)
        for (CalendarEntry existing : profile.getCalendarEntries()) {
            if (java.util.Objects.equals(existing.dateTime, email.calendarDateTime)
                    && java.util.Objects.equals(existing.title, email.calendarTitle)) {
                Gdx.app.log("MainScreen", "Duplicate calendar entry ignored: " + email.calendarTitle);
                return;
            }
        }
        profile.addCalendarEntry(
                new CalendarEntry(email.calendarDateTime, email.calendarTitle, email.calendarLocation,
                        email.rewardMoney, email.rewardItemName,
                        email.locationCellX, email.locationCellY,
                        email.calendarContactName, email.contactGender));
        Gdx.app.log("MainScreen", "Calendar entry added: " + email.calendarTitle
                + " @ " + email.calendarDateTime
                + (email.rewardMoney > 0 ? "  reward=$" + email.rewardMoney : ""));
    }

    /**
     * Builds the phone contact list from the profile's open case files.
     *
     * <p>Each open case contributes its {@link CaseFile#getClientName() client name}
     * and {@link CaseFile#getSubjectName() subject name} as contacts, provided they
     * are non-blank.  Contacts from open cases are marked with a star (★).  Duplicate
     * names within the same case are skipped.
     *
     * @return ordered list of {@link PhoneContact}s (never {@code null})
     */
    List<PhoneContact> buildPhoneContacts() {
        List<PhoneContact> result = new ArrayList<>();
        for (CaseFile cf : profile.getCaseFiles()) {
            if (!cf.isOpen()) continue;  // contacts removed when case ends
            String caseId = cf.getId();
            java.util.Set<String> seenNames = new java.util.LinkedHashSet<>();
            // Client name
            String clientName = cf.getClientName();
            if (clientName != null && !clientName.trim().isEmpty()
                    && seenNames.add(clientName.trim())) {
                result.add(new PhoneContact(
                        clientName.trim(), caseId, true,
                        profile.isContactPhoned(caseId, clientName.trim()),
                        profile.getContactMessageRating(caseId, clientName.trim())));
            }
            // Subject name
            String subjectName = cf.getSubjectName();
            if (subjectName != null && !subjectName.trim().isEmpty()
                    && seenNames.add(subjectName.trim())) {
                result.add(new PhoneContact(
                        subjectName.trim(), caseId, true,
                        profile.isContactPhoned(caseId, subjectName.trim()),
                        profile.getContactMessageRating(caseId, subjectName.trim())));
            }
        }
        return result;
    }

    /**
     * Called when the player taps a contact in the phone popup.
     * <ul>
     *   <li>First tap: marks the contact as called with a default
     *       {@link PhoneMessageRating#NEUTRAL} rating.</li>
     *   <li>Subsequent taps: cycles the rating (NEUTRAL → FRIENDLY →
     *       UNFRIENDLY → NEUTRAL).</li>
     * </ul>
     *
     * @param contactIndex index in the phone popup's contact list
     */
    private void handleContactPhoned(int contactIndex) {
        PhoneContact contact = phonePopup.getContactAt(contactIndex);
        if (contact == null) return;
        if (!contact.phoned) {
            // First call – default to NEUTRAL
            contact.phoned  = true;
            contact.rating  = PhoneMessageRating.NEUTRAL;
        } else {
            // Re-tap: cycle through ratings
            contact.rating = (contact.rating != null)
                    ? contact.rating.next()
                    : PhoneMessageRating.NEUTRAL;
        }
        profile.setContactMessageRating(contact.caseId, contact.name, contact.rating);
        Gdx.app.log("MainScreen", "Phoned contact: " + contact.name
                + " (case " + contact.caseId + ") rating=" + contact.rating);
    }

    /**
     * Returns a contextual description of the player's home office, selected from
     * {@code buildings_en.json} (descriptions section) using the office building's current state
     * (good/normal/bad).  Falls back to the time-of-day description when no state
     * variant is defined, and to an empty string when no JSON entry is found.
     */
    String buildOfficeDescription() {
        if (novelTextEngine == null) return "";
        Cell homeCell = cityMap.getCell(state.homeCellX, state.homeCellY);
        String buildingState = (homeCell != null && homeCell.hasBuilding())
                ? homeCell.getBuilding().getState() : null;
        String desc = "";
        if (buildingState != null) {
            desc = novelTextEngine.getStateDescription("your_office", buildingState);
        }
        if (desc == null || desc.isEmpty()) {
            desc = novelTextEngine.getDescription(
                    "your_office", profile.getCurrentHour(),
                    profile.getAttributes(), profile.getGender());
        }
        return desc != null ? desc : "";
    }

    /**
     * Returns a contextual room description for the given hotel building, selected
     * from {@code buildings_en.json} (descriptions section) using the room-specific key derived from the
     * building definition ID ({@code <id>_room}) and the building's state
     * (good/normal/bad).  Falls back to time-of-day then default text.
     *
     * @param hotel the hotel {@link Building} (may be {@code null})
     * @return description text, never {@code null}
     */
    String buildHotelRoomDescription(Building hotel) {
        if (novelTextEngine == null || hotel == null || hotel.getDefinition() == null) return "";
        String roomKey = hotel.getDefinition().getId() + "_room";
        String buildingState = hotel.getState();
        String desc = "";
        if (buildingState != null) {
            desc = novelTextEngine.getStateDescription(roomKey, buildingState);
        }
        if (desc == null || desc.isEmpty()) {
            desc = novelTextEngine.getDescription(
                    roomKey, profile.getCurrentHour(),
                    profile.getAttributes(), profile.getGender());
        }
        return desc != null ? desc : "";
    }

    /**
     * Scans the city map and returns the coordinates {@code [x, y]} of the first
     * cell whose building has the given {@code buildingId}, or {@code null} if none.
     */
    private int[] findFirstBuildingCell(String buildingId) {
        if (buildingId == null) return null;
        Cell[][] cells = cityMap.getCells();
        if (cells == null) return null;
        for (Cell[] row : cells) {
            if (row == null) continue;
            for (Cell cell : row) {
                if (cell == null || !cell.hasBuilding()) continue;
                if (buildingId.equals(cell.getBuilding().getDefinition().getId())) {
                    return new int[]{ cell.getX(), cell.getY() };
                }
            }
        }
        return null;
    }

    /**
     * Returns a game date-time string that is at least {@code daysAhead} days
     * from {@code currentDT}, at 09:00, and not already present in
     * {@code usedDTs}.  The chosen datetime is added to {@code usedDTs} so
     * subsequent calls in the same batch automatically receive a different slot.
     * Format: {@code "YYYY-MM-DD 09:00"}.
     */
    private static String nextAppointmentDT(String currentDT, int daysAhead,
                                             java.util.Set<String> usedDTs) {
        try {
            String[] parts = currentDT.split(" ")[0].split("-");
            int year  = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day   = Integer.parseInt(parts[2]);
            day += daysAhead;
            // Advance until we land on a free slot
            while (true) {
                // Normalise the date (roll over month/year boundaries)
                while (true) {
                    boolean leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
                    int[] dim = {31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
                    if (day <= dim[month - 1]) break;
                    day -= dim[month - 1];
                    if (++month > 12) { month = 1; year++; }
                }
                String candidate = String.format("%04d-%02d-%02d 09:00", year, month, day);
                if (!usedDTs.contains(candidate)) {
                    usedDTs.add(candidate);
                    return candidate;
                }
                day++; // slot taken — try the next day
            }
        } catch (Exception e) {
            String fallback = currentDT.split(" ")[0] + " 09:00";
            usedDTs.add(fallback);
            return fallback;
        }
    }

    /**
     * Pre-discovers civic buildings and attribute-related buildings at game start.
     *
     * <p>Always discovered (public knowledge):
     * <ul>
     *   <li>Nearest police station</li>
     *   <li>Nearest hospital (small or large)</li>
     * </ul>
     *
     * <p>Discovered when the relevant attribute score is &ge; 4:
     * <ul>
     *   <li>STRENGTH or STAMINA &rarr; gym/fitness centre</li>
     *   <li>INTELLIGENCE &rarr; public library</li>
     *   <li>PERCEPTION &rarr; fire station</li>
     *   <li>MEMORY &rarr; post office</li>
     *   <li>INTUITION &rarr; nearest religious building</li>
     *   <li>AGILITY &rarr; sports arena</li>
     *   <li>CHARISMA &rarr; nearest restaurant</li>
     *   <li>EMPATHY &rarr; community centre</li>
     *   <li>INTIMIDATION &rarr; courthouse</li>
     * </ul>
     */
    private void discoverStartingBuildings() {
        int refX = state.homeCellX;
        int refY = state.homeCellY;
        if (refX < 0 || refY < 0) return;

        // Always discover: police station, hospital, and nearest security shop
        discoverCellIfFound(findNearestBuilding(refX, refY, "police_station"));
        discoverCellIfFound(findNearestBuilding(refX, refY, "hospital_small", "hospital_large"));
        discoverCellIfFound(findNearestBuilding(refX, refY, BUILDING_ID_SECURITY_SHOP));

        // STRENGTH or STAMINA >= 4 → gym
        if (profile.getAttribute(CharacterAttribute.STRENGTH.name()) >= 4
                || profile.getAttribute(CharacterAttribute.STAMINA.name()) >= 4) {
            discoverCellIfFound(findNearestBuilding(refX, refY, "gym_fitness_center"));
        }

        // INTELLIGENCE >= 4 → library
        if (profile.getAttribute(CharacterAttribute.INTELLIGENCE.name()) >= 4) {
            discoverCellIfFound(findNearestBuilding(refX, refY, "library"));
        }

        // PERCEPTION >= 4 → fire station
        if (profile.getAttribute(CharacterAttribute.PERCEPTION.name()) >= 4) {
            discoverCellIfFound(findNearestBuilding(refX, refY, "fire_station"));
        }

        // MEMORY >= 4 → post office
        if (profile.getAttribute(CharacterAttribute.MEMORY.name()) >= 4) {
            discoverCellIfFound(findNearestBuilding(refX, refY, "post_office"));
        }

        // INTUITION >= 4 → nearest religious building
        if (profile.getAttribute(CharacterAttribute.INTUITION.name()) >= 4) {
            discoverCellIfFound(findNearestBuilding(refX, refY, "church", "mosque", "synagogue"));
        }

        // AGILITY >= 4 → sports arena
        if (profile.getAttribute(CharacterAttribute.AGILITY.name()) >= 4) {
            discoverCellIfFound(findNearestBuilding(refX, refY, "sports_arena"));
        }

        // CHARISMA >= 4 → nearest restaurant
        if (profile.getAttribute(CharacterAttribute.CHARISMA.name()) >= 4) {
            discoverCellIfFound(findNearestBuilding(refX, refY,
                    "restaurant_casual", "restaurant_fine_dining"));
        }

        // EMPATHY >= 4 → community centre
        if (profile.getAttribute(CharacterAttribute.EMPATHY.name()) >= 4) {
            discoverCellIfFound(findNearestBuilding(refX, refY, "community_center"));
        }

        // INTIMIDATION >= 4 → courthouse
        if (profile.getAttribute(CharacterAttribute.INTIMIDATION.name()) >= 4) {
            discoverCellIfFound(findNearestBuilding(refX, refY, "courthouse"));
        }

        Gdx.app.log("MainScreen", "Starting buildings discovered from home "
                + refX + "," + refY);
    }

    /**
     * Called when the player taps an inline action button for an improvement.
     * Applies a basic stamina effect based on the improvement's function and
     * effective rating, then shows the result popup.
     */
    private void handleImprovementUse(Improvement imp) {
        if (imp == null) return;
        List<String> resultLines = new ArrayList<>();
        String fn = imp.getFunction();
        int eff = imp.getEffective();

        // Advance time (10 min per use of an improvement)
        profile.advanceGameTime(10);
        profile.useStamina(1);

        // Basic effect per function type
        switch (fn) {
            case "rest": {
                int gain = Math.max(1, eff / 20);
                profile.addStamina(gain);
                resultLines.add("You rested at the " + imp.getName() + ".");
                resultLines.add("+" + gain + " stamina.");
                break;
            }
            case "exercise": {
                int staminaCost = 2;
                profile.useStamina(staminaCost);
                resultLines.add("You exercised at the " + imp.getName() + ".");
                resultLines.add("Spent " + staminaCost + " extra stamina working out.");
                break;
            }
            case "study": {
                String intAttr = CharacterAttribute.INTELLIGENCE.name();
                int cur = profile.getAttribute(intAttr);
                float chance = eff / 200f;   // 50% eff → 25% chance
                if (MathUtils.random() < chance) {
                    profile.setAttribute(intAttr, cur + 1);
                    resultLines.add("You studied at the " + imp.getName() + ".");
                    resultLines.add("Your Intelligence improved!");
                } else {
                    resultLines.add("You studied at the " + imp.getName() + ".");
                    resultLines.add("No breakthrough this time.");
                }
                break;
            }
            case "converse": {
                String chaAttr = CharacterAttribute.CHARISMA.name();
                int cur = profile.getAttribute(chaAttr);
                float chance = eff / 200f;
                if (MathUtils.random() < chance) {
                    profile.setAttribute(chaAttr, cur + 1);
                    resultLines.add("You socialised at the " + imp.getName() + ".");
                    resultLines.add("Your Charisma improved!");
                } else {
                    resultLines.add("You socialised at the " + imp.getName() + ".");
                    resultLines.add("A pleasant interaction.");
                }
                break;
            }
            case "draw": {
                String perAttr = CharacterAttribute.PERCEPTION.name();
                int cur = profile.getAttribute(perAttr);
                float chance = eff / 200f;
                if (MathUtils.random() < chance) {
                    profile.setAttribute(perAttr, cur + 1);
                    resultLines.add("You explored the " + imp.getName() + ".");
                    resultLines.add("Your Perception sharpened!");
                } else {
                    resultLines.add("You explored the " + imp.getName() + ".");
                    resultLines.add("An interesting experience.");
                }
                break;
            }
            case "repair": {
                int gain = Math.max(1, eff / 25);
                profile.addStamina(gain);
                resultLines.add("You used the " + imp.getName() + ".");
                resultLines.add("+" + gain + " stamina.");
                break;
            }
            default: {
                resultLines.add("You used the " + imp.getName() + ".");
                break;
            }
        }

        serviceResultPopup.show(imp.getName(), resultLines);
        Gdx.app.log("MainScreen", "ImprovementUse: " + imp.getName() + " fn=" + fn);
    }
}

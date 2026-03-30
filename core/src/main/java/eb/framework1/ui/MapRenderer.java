package eb.framework1.ui;

import eb.framework1.city.*;
import eb.framework1.screen.*;
import eb.framework1.character.*;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the city map: cell fills, road borders, route highlight, building icons,
 * character portrait, selection highlight, coordinate labels, and rulers.
 */
public class MapRenderer {

    // --- Colors ---
    private static final Color RULER_BG_COLOR        = new Color(0.1f,  0.1f,  0.15f, 1f);
    private static final Color RULER_MARKER_COLOR    = new Color(1f,   0.5f,  0f,    1f);
    private static final Color SELECTION_COLOR       = new Color(1f,   1f,    0f,    1f);
    private static final Color REST_INDICATOR_COLOR  = new Color(0f,   0.8f,  0.2f,  1f);
    private static final Color SLEEP_INDICATOR_COLOR = new Color(0.2f, 0.3f,  0.9f,  1f);
    private static final Color TRAVELED_ROAD_COLOR   = new Color(0.3f,  0.75f, 1f,    1f);
    private static final Color BEVEL_LIGHT_COLOR     = new Color(0.85f, 0.85f, 0.85f, 0.7f);
    private static final Color BEVEL_DARK_COLOR      = new Color(0.15f, 0.15f, 0.15f, 0.55f);
    /** Bevel thickness as a fraction of cell size (clamped to a minimum of 2 px). */
    private static final float BEVEL_SIZE_RATIO      = 0.03f;
    /** Minimum bevel thickness in screen pixels regardless of zoom level. */
    private static final float MIN_BEVEL_SIZE_PX     = 2f;

    public static final String[] HEX_DIGITS = {
        "0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"
    };

    /**
     * Returns a map cell label in the same format shown on the rulers, e.g.
     * column 8 + row 10 → {@code "8A"}.
     *
     * @param x column index (0–15)
     * @param y row index (0–15)
     * @return hex-digit label, or {@code "??"} when out of range
     */
    static String cellLabel(int x, int y) {
        if (x < 0 || x >= HEX_DIGITS.length || y < 0 || y >= HEX_DIGITS.length)
            return "??";
        return HEX_DIGITS[x] + HEX_DIGITS[y];
    }
    private static final int SELECTION_THICKNESS = 5;

    // --- Rendering resources ---
    private final SpriteBatch    batch;
    private final ShapeRenderer  shapeRenderer;
    private final BitmapFont     font;
    private final BitmapFont     smallFont;
    private final GlyphLayout    glyphLayout;
    private final CityMap        cityMap;

    // --- Caches ---
    private Map<String, Texture> iconTextureCache;
    private Texture              characterIconTexture;
    /** Small icon textures for NPC markers on the map. */
    private Texture              npcManTexture;
    private Texture              npcWomanTexture;
    /** Native dimensions of the NPC marker textures (cached to avoid per-frame queries). */
    private int                  npcManTexW, npcManTexH;
    private int                  npcWomanTexW, npcWomanTexH;


    public MapRenderer(SpriteBatch batch, ShapeRenderer shapeRenderer,
                BitmapFont font, BitmapFont smallFont, BitmapFont tinyFont,
                GlyphLayout glyphLayout,
                CityMap cityMap) {
        this.batch         = batch;
        this.shapeRenderer = shapeRenderer;
        this.font          = font;
        this.smallFont     = smallFont;
        this.glyphLayout   = glyphLayout;
        this.cityMap       = cityMap;
    }

    // -------------------------------------------------------------------------
    // Initialisation helpers (called once from MainScreen.show())
    // -------------------------------------------------------------------------

    public void loadBuildingIcons() {
        iconTextureCache = new HashMap<>();
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                CellRenderData rd = cityMap.getCellRenderData(x, y);
                String iconPath = rd.getIconPath();
                if (iconPath != null && !iconTextureCache.containsKey(iconPath)) {
                    try {
                        Texture tex = new Texture(Gdx.files.internal(iconPath));
                        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                        iconTextureCache.put(iconPath, tex);
                    } catch (Exception e) {
                        Gdx.app.log("MapRenderer", "Could not load icon: " + iconPath + " – " + e.getMessage());
                    }
                }
            }
        }
        Gdx.app.log("MapRenderer", "Loaded " + iconTextureCache.size() + " building icons");

        // Load NPC gender icons used to mark NPC positions on the map
        try {
            npcManTexture = new Texture(Gdx.files.internal("icons/man_icon_32.png"));
            npcManTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            npcManTexW = npcManTexture.getWidth();
            npcManTexH = npcManTexture.getHeight();
        } catch (Exception e) {
            Gdx.app.log("MapRenderer", "Could not load man_icon_32: " + e.getMessage());
        }
        try {
            npcWomanTexture = new Texture(Gdx.files.internal("icons/woman_icon_32.png"));
            npcWomanTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            npcWomanTexW = npcWomanTexture.getWidth();
            npcWomanTexH = npcWomanTexture.getHeight();
        } catch (Exception e) {
            Gdx.app.log("MapRenderer", "Could not load woman_icon_32: " + e.getMessage());
        }
    }

    public void setCharacterIconTexture(Texture tex) {
        this.characterIconTexture = tex;
    }

    public void dispose() {
        if (iconTextureCache != null) {
            for (Texture tex : iconTextureCache.values()) tex.dispose();
            iconTextureCache.clear();
        }
        if (characterIconTexture != null) {
            characterIconTexture.dispose();
            characterIconTexture = null;
        }
        if (npcManTexture != null) {
            npcManTexture.dispose();
            npcManTexture = null;
        }
        if (npcWomanTexture != null) {
            npcWomanTexture.dispose();
            npcWomanTexture = null;
        }
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    public void drawMap(MapViewState s) {
        float cellSize     = s.getCellSize();
        int visibleCellsX  = s.getVisibleCellsX();
        int visibleCellsY  = s.getVisibleCellsY();
        float borderSize   = Math.max(2, cellSize * 0.06f);
        float pathwaySize  = Math.max(1, borderSize * 0.25f);
        float mapStartX    = MapViewState.RULER_WIDTH + MapViewState.RULER_GAP;
        float mapStartY    = s.infoAreaHeight;

        int startCellX = (int) s.mapOffsetX;
        int startCellY = (int) s.mapOffsetY;
        int endCellX   = Math.min(startCellX + visibleCellsX + 1, CityMap.MAP_SIZE);
        int endCellY   = Math.min(startCellY + visibleCellsY + 1, CityMap.MAP_SIZE);
        float fracOffsetX = s.mapOffsetX - startCellX;
        float fracOffsetY = s.mapOffsetY - startCellY;

        // Black grid background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(mapStartX, mapStartY, cellSize * visibleCellsX, cellSize * visibleCellsY);
        shapeRenderer.end();

        // Cell fills
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int cx = startCellX; cx < endCellX; cx++) {
            for (int cy = startCellY; cy < endCellY; cy++) {
                float drawX = mapStartX + (cx - startCellX - fracOffsetX) * cellSize;
                float drawY = mapStartY + (visibleCellsY - 1 - (cy - startCellY - fracOffsetY)) * cellSize;
                if (drawX + cellSize < mapStartX - cellSize || drawX > mapStartX + cellSize * visibleCellsX + cellSize
                        || drawY + cellSize < mapStartY - cellSize || drawY > mapStartY + cellSize * visibleCellsY + cellSize) {
                    continue;
                }
                CellRenderData rd = cityMap.getCellRenderData(cx, cy);
                shapeRenderer.setColor(rd.getR(), rd.getG(), rd.getB(), rd.getA());
                float li = borderInset(rd.getBorderTypeWest(),  borderSize, pathwaySize);
                float ri = borderInset(rd.getBorderTypeEast(),  borderSize, pathwaySize);
                float bi = borderInset(rd.getBorderTypeNorth(), borderSize, pathwaySize);
                float ti = borderInset(rd.getBorderTypeSouth(), borderSize, pathwaySize);
                shapeRenderer.rect(drawX + li, drawY + bi, cellSize - li - ri, cellSize - bi - ti);
            }
        }
        shapeRenderer.end();

        // Gray beveled rectangle overlay on building squares (raised 3-D look)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float bevelSize = bevelSize(cellSize);
        for (int cx = startCellX; cx < endCellX; cx++) {
            for (int cy = startCellY; cy < endCellY; cy++) {
                if (!cityMap.getCell(cx, cy).hasBuilding()) continue;
                float drawX = mapStartX + (cx - startCellX - fracOffsetX) * cellSize;
                float drawY = mapStartY + (visibleCellsY - 1 - (cy - startCellY - fracOffsetY)) * cellSize;
                CellRenderData rd = cityMap.getCellRenderData(cx, cy);
                float li = borderInset(rd.getBorderTypeWest(),  borderSize, pathwaySize);
                float ri = borderInset(rd.getBorderTypeEast(),  borderSize, pathwaySize);
                float bi = borderInset(rd.getBorderTypeNorth(), borderSize, pathwaySize);
                float ti = borderInset(rd.getBorderTypeSouth(), borderSize, pathwaySize);
                drawBeveledRect(shapeRenderer,
                        drawX + li, drawY + bi,
                        cellSize - li - ri, cellSize - bi - ti,
                        bevelSize);
            }
        }
        shapeRenderer.end();

        // Mountain crosshatch overlay (black "/" and "\" diagonal lines)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.BLACK);
        for (int cx = startCellX; cx < endCellX; cx++) {
            for (int cy = startCellY; cy < endCellY; cy++) {
                if (cityMap.getCell(cx, cy).getTerrainType() != TerrainType.MOUNTAIN) continue;
                float drawX = mapStartX + (cx - startCellX - fracOffsetX) * cellSize;
                float drawY = mapStartY + (visibleCellsY - 1 - (cy - startCellY - fracOffsetY)) * cellSize;
                if (drawX + cellSize < mapStartX || drawX > mapStartX + cellSize * visibleCellsX
                        || drawY + cellSize < mapStartY || drawY > mapStartY + cellSize * visibleCellsY) {
                    continue;
                }
                float step = Math.max(3f, cellSize / 5f);
                // "/" diagonals (bottom-left to top-right family)
                for (float t = -cellSize + step; t < cellSize; t += step) {
                    float x1, y1, x2, y2;
                    if (t >= 0) {
                        x1 = drawX;            y1 = drawY + t;
                        x2 = drawX + cellSize - t; y2 = drawY + cellSize;
                    } else {
                        x1 = drawX - t;        y1 = drawY;
                        x2 = drawX + cellSize; y2 = drawY + cellSize + t;
                    }
                    shapeRenderer.line(x1, y1, x2, y2);
                }
                // "\" diagonals (top-left to bottom-right family)
                for (float t = step; t < 2 * cellSize; t += step) {
                    float x1, y1, x2, y2;
                    if (t <= cellSize) {
                        x1 = drawX;            y1 = drawY + t;
                        x2 = drawX + t;        y2 = drawY;
                    } else {
                        x1 = drawX + t - cellSize; y1 = drawY + cellSize;
                        x2 = drawX + cellSize;     y2 = drawY + t - cellSize;
                    }
                    shapeRenderer.line(x1, y1, x2, y2);
                }
            }
        }
        shapeRenderer.end();

        // Blue road segments: preview for planned route AND accumulated traveled path
        boolean hasPreview  = s.currentRoute != null && s.currentRoute.isReachable()
                && s.currentRoute.path != null && s.currentRoute.path.size() >= 2;
        boolean hasTraveled = s.traveledPath != null && s.traveledPath.size() >= 2;
        if (hasPreview || hasTraveled) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(TRAVELED_ROAD_COLOR);
            if (hasPreview) {
                drawRoadSegments(s.currentRoute.path, shapeRenderer,
                        mapStartX, mapStartY, cellSize, borderSize,
                        startCellX, startCellY, endCellX, endCellY,
                        fracOffsetX, fracOffsetY, visibleCellsY);
            }
            if (hasTraveled) {
                drawRoadSegments(s.traveledPath, shapeRenderer,
                        mapStartX, mapStartY, cellSize, borderSize,
                        startCellX, startCellY, endCellX, endCellY,
                        fracOffsetX, fracOffsetY, visibleCellsY);
            }
            shapeRenderer.end();
        }

        // Building icons (only discovered buildings)
        batch.begin();
        for (int cx = startCellX; cx < endCellX; cx++) {
            for (int cy = startCellY; cy < endCellY; cy++) {
                Cell iconCell = cityMap.getCell(cx, cy);
                if (!iconCell.hasBuilding() || !iconCell.getBuilding().isDiscovered()) continue;
                CellRenderData rd = cityMap.getCellRenderData(cx, cy);
                String iconPath = rd.getIconPath();
                if (iconPath == null) continue;
                Texture iconTex = iconTextureCache.get(iconPath);
                if (iconTex == null) continue;
                float drawX = mapStartX + (cx - startCellX - fracOffsetX) * cellSize;
                float drawY = mapStartY + (visibleCellsY - 1 - (cy - startCellY - fracOffsetY)) * cellSize;
                float li = borderInset(rd.getBorderTypeWest(),  borderSize, pathwaySize);
                float ri = borderInset(rd.getBorderTypeEast(),  borderSize, pathwaySize);
                float bi = borderInset(rd.getBorderTypeNorth(), borderSize, pathwaySize);
                float ti = borderInset(rd.getBorderTypeSouth(), borderSize, pathwaySize);
                float availW = cellSize - li - ri;
                float availH = cellSize - bi - ti;
                float iconSize = Math.min(availW, availH) * 0.7f;
                batch.setColor(Color.WHITE);
                batch.draw(iconTex, drawX + li + (availW - iconSize) / 2, drawY + bi + (availH - iconSize) / 2,
                        iconSize, iconSize);
            }
        }

        // Character portrait
        if (characterIconTexture != null) {
            float portraitSize = cellSize * 0.4f;
            batch.setColor(Color.WHITE);
            if (s.charJuncX >= 0f) {
                // Walking: draw portrait centred on the current junction (road position)
                // Screen X: junction jx lies at the left edge of cell jx
                float jScreenX = mapStartX + (s.charJuncX - startCellX - fracOffsetX) * cellSize;
                // Screen Y: junction jy lies at the top of cell jy = bottom of cell (jy-1)
                float jScreenY = mapStartY + (visibleCellsY - 1 - (s.charJuncY - startCellY - fracOffsetY)) * cellSize
                        + cellSize;
                batch.draw(characterIconTexture,
                        jScreenX - portraitSize / 2f,
                        jScreenY - portraitSize / 2f,
                        portraitSize, portraitSize);
            } else if (s.charCellX >= 0 && s.charCellY >= 0
                    && s.charCellX >= startCellX && s.charCellX < endCellX
                    && s.charCellY >= startCellY && s.charCellY < endCellY) {
                // Idle: draw portrait in the top-right corner of the current cell
                float drawX = mapStartX + (s.charCellX - startCellX - fracOffsetX) * cellSize;
                float drawY = mapStartY + (visibleCellsY - 1 - (s.charCellY - startCellY - fracOffsetY)) * cellSize;
                batch.draw(characterIconTexture,
                        drawX + cellSize - portraitSize - borderSize, drawY + borderSize,
                        portraitSize, portraitSize);
            }
        }
        batch.end();

        // Rest / sleep indicator dots + home-cell yellow star (single Filled pass)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float dotR = Math.max(3f, cellSize * 0.11f);
        for (int cx = startCellX; cx < endCellX; cx++) {
            for (int cy = startCellY; cy < endCellY; cy++) {
                Cell ic = cityMap.getCell(cx, cy);
                if (!ic.hasBuilding() || !ic.getBuilding().isDiscovered()) continue;
                Building ib = ic.getBuilding();
                if (ib.isHome() || (!ib.allowsRest() && !ib.allowsSleep())) continue;
                CellRenderData ird = cityMap.getCellRenderData(cx, cy);
                float drawX = mapStartX + (cx - startCellX - fracOffsetX) * cellSize;
                float drawY = mapStartY + (visibleCellsY - 1 - (cy - startCellY - fracOffsetY)) * cellSize;
                float ti = borderInset(ird.getBorderTypeSouth(), borderSize, pathwaySize);
                float li = borderInset(ird.getBorderTypeWest(),  borderSize, pathwaySize);
                float dotY = drawY + cellSize - ti - dotR - 2f;
                float dotX = drawX + li + dotR + 2f;
                if (ib.allowsRest()) {
                    shapeRenderer.setColor(REST_INDICATOR_COLOR);
                    shapeRenderer.circle(dotX, dotY, dotR, 10);
                    dotX += dotR * 2 + 2f;
                }
                if (ib.allowsSleep()) {
                    shapeRenderer.setColor(SLEEP_INDICATOR_COLOR);
                    shapeRenderer.circle(dotX, dotY, dotR, 10);
                }
            }
        }
        if (s.homeCellX >= startCellX && s.homeCellX < endCellX
                && s.homeCellY >= startCellY && s.homeCellY < endCellY) {
            float drawX = mapStartX + (s.homeCellX - startCellX - fracOffsetX) * cellSize;
            float drawY = mapStartY + (visibleCellsY - 1 - (s.homeCellY - startCellY - fracOffsetY)) * cellSize;
            float starR = Math.max(4f, cellSize * 0.18f);
            shapeRenderer.setColor(Color.YELLOW);
            drawFilledStar(shapeRenderer,
                    drawX + cellSize - starR - borderSize,
                    drawY + cellSize - starR - borderSize,
                    starR, starR * 0.4f);
        }
        shapeRenderer.end();

        // NPC icons (tracked NPCs, or all NPCs in developer / debug mode)
        drawNpcIcons(s, mapStartX, mapStartY, cellSize, borderSize, pathwaySize,
                startCellX, startCellY, endCellX, endCellY,
                fracOffsetX, fracOffsetY, visibleCellsY);

        // Selection highlight
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        if (s.selectedCellX >= 0 && s.selectedCellY >= 0
                && s.selectedCellX >= startCellX && s.selectedCellX < endCellX
                && s.selectedCellY >= startCellY && s.selectedCellY < endCellY) {
            float drawX = mapStartX + (s.selectedCellX - startCellX - fracOffsetX) * cellSize;
            float drawY = mapStartY + (visibleCellsY - 1 - (s.selectedCellY - startCellY - fracOffsetY)) * cellSize;
            shapeRenderer.setColor(SELECTION_COLOR);
            for (int i = 0; i < SELECTION_THICKNESS; i++) {
                shapeRenderer.rect(drawX + i, drawY + i, cellSize - i * 2, cellSize - i * 2);
            }
        }
        shapeRenderer.end();

        // Coordinate labels (when zoomed in)
        if (s.zoomLevel >= 2.0f) {
            batch.begin();
            smallFont.setColor(Color.WHITE);
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
            smallFont.getData().setScale(1.0f);
            batch.end();
        }
    }

    // -------------------------------------------------------------------------
    // NPC icon rendering
    // -------------------------------------------------------------------------

    /**
     * Draws a gender-specific icon at the lower-left corner of each NPC's
     * current building rectangle, inset from the cell border so the icon
     * never overlaps a road.
     *
     * <p>In developer / debug mode ({@link MapViewState#developerMode}) all
     * NPCs in {@link MapViewState#allNpcs} are drawn.  In normal mode only NPCs
     * where {@link NpcCharacter#isTracked()} is {@code true} are drawn.
     *
     * <p>Male NPCs use {@code icons/man_icon_32.png}; female NPCs use
     * {@code icons/woman_icon_32.png}.  Each icon is drawn at its native texture
     * size (32×64 px), pinned to the lower-left corner of the building rectangle
     * (i.e. offset by the West and North border insets of the cell).
     *
     * <p>NPCs with no known cell ({@code -1}) or outside the currently visible
     * map area are silently skipped.
     */
    private void drawNpcIcons(MapViewState s,
            float mapStartX, float mapStartY, float cellSize,
            float borderSize, float pathwaySize,
            int startCellX, int startCellY, int endCellX, int endCellY,
            float fracOffsetX, float fracOffsetY, int visibleCellsY) {

        if (s.allNpcs == null || s.allNpcs.isEmpty()) return;
        if (npcManTexture == null && npcWomanTexture == null) return;

        batch.begin();
        batch.setColor(Color.WHITE);

        for (NpcCharacter npc : s.allNpcs) {
            if (!s.developerMode && !npc.isTracked()) continue;

            int nx = npc.getCurrentCellX(s.currentHour);
            int ny = npc.getCurrentCellY(s.currentHour);
            if (nx < 0 || ny < 0) continue;
            if (nx < startCellX || nx >= endCellX || ny < startCellY || ny >= endCellY) continue;

            // Lower-left corner of the cell in screen coordinates
            float cellX = mapStartX + (nx - startCellX - fracOffsetX) * cellSize;
            float cellY = mapStartY + (visibleCellsY - 1 - (ny - startCellY - fracOffsetY)) * cellSize;

            // Compute road-border insets so the icon sits inside the building rectangle
            CellRenderData rd = cityMap.getCellRenderData(nx, ny);
            float li = borderInset(rd.getBorderTypeWest(),  borderSize, pathwaySize);
            float bi = borderInset(rd.getBorderTypeNorth(), borderSize, pathwaySize);

            boolean isFemale = "F".equalsIgnoreCase(npc.getGender());
            Texture icon = isFemale ? npcWomanTexture : npcManTexture;
            if (icon == null) continue;

            // Pin icon to the lower-left of the building rectangle at native size,
            // inset past the bevel so the icon sits fully inside the building area.
            float iconW = isFemale ? npcWomanTexW : npcManTexW;
            float iconH = isFemale ? npcWomanTexH : npcManTexH;
            float bevelInset = bevelSize(cellSize);
            float npcIconX = cellX + li + bevelInset;
            float npcIconY = cellY + bi + bevelInset;
            batch.draw(icon, npcIconX, npcIconY, iconW, iconH);
        }

        batch.end();
    }

    public void drawRulers(MapViewState s) {
        float cellSize    = s.getCellSize();
        int visibleCellsX = s.getVisibleCellsX();
        int visibleCellsY = s.getVisibleCellsY();
        float mapStartX   = MapViewState.RULER_WIDTH + MapViewState.RULER_GAP;
        float mapStartY   = s.infoAreaHeight;
        int startCellX    = (int) s.mapOffsetX;
        int startCellY    = (int) s.mapOffsetY;
        float fracOffsetX = s.mapOffsetX - startCellX;
        float fracOffsetY = s.mapOffsetY - startCellY;
        float topRulerY   = s.screenHeight - MapViewState.INFO_BAR_HEIGHT - MapViewState.RULER_WIDTH;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(RULER_BG_COLOR);
        shapeRenderer.rect(0, mapStartY, MapViewState.RULER_WIDTH, cellSize * visibleCellsY);
        shapeRenderer.rect(mapStartX, topRulerY, cellSize * visibleCellsX, MapViewState.RULER_WIDTH);
        if (s.cursorCellY >= startCellY && s.cursorCellY < startCellY + visibleCellsY) {
            float ry = mapStartY + (visibleCellsY - 1 - (s.cursorCellY - startCellY - fracOffsetY)) * cellSize;
            shapeRenderer.setColor(RULER_MARKER_COLOR);
            shapeRenderer.rect(0, ry, MapViewState.RULER_WIDTH, cellSize);
        }
        if (s.cursorCellX >= startCellX && s.cursorCellX < startCellX + visibleCellsX) {
            float rx = mapStartX + (s.cursorCellX - startCellX - fracOffsetX) * cellSize;
            shapeRenderer.setColor(RULER_MARKER_COLOR);
            shapeRenderer.rect(rx, topRulerY, cellSize, MapViewState.RULER_WIDTH);
        }
        shapeRenderer.end();

        batch.begin();
        for (int i = 0; i < visibleCellsY; i++) {
            int cellY = startCellY + i;
            if (cellY < 0 || cellY >= CityMap.MAP_SIZE) continue;
            float ry = mapStartY + (visibleCellsY - 1 - (i - fracOffsetY)) * cellSize;
            String hex = HEX_DIGITS[cellY];
            glyphLayout.setText(smallFont, hex);
            smallFont.setColor(s.cursorCellY == cellY ? Color.BLACK : Color.WHITE);
            smallFont.draw(batch, hex,
                    (MapViewState.RULER_WIDTH - glyphLayout.width) / 2,
                    ry + (cellSize + glyphLayout.height) / 2);
        }
        for (int i = 0; i < visibleCellsX; i++) {
            int cellX = startCellX + i;
            if (cellX < 0 || cellX >= CityMap.MAP_SIZE) continue;
            float rx = mapStartX + (i - fracOffsetX) * cellSize;
            String hex = HEX_DIGITS[cellX];
            glyphLayout.setText(smallFont, hex);
            smallFont.setColor(s.cursorCellX == cellX ? Color.BLACK : Color.WHITE);
            smallFont.draw(batch, hex,
                    rx + (cellSize - glyphLayout.width) / 2,
                    topRulerY + MapViewState.RULER_WIDTH - (MapViewState.RULER_WIDTH - glyphLayout.height) / 2);
        }
        batch.end();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    public static float borderInset(RoadType type, float borderSize, float pathwaySize) {
        switch (type) {
            case ROAD:    return borderSize;
            case PATHWAY: return pathwaySize;
            default:      return 0;
        }
    }

    /**
     * Draws blue filled road bars between each consecutive pair of junctions in {@code path}.
     * Each entry in {@code path} is a junction coordinate [jx, jy] ∈ [0..MAP_SIZE].
     * The ShapeRenderer must already be in {@link ShapeRenderer.ShapeType#Filled} mode with the
     * desired colour set by the caller.
     *
     * <ul>
     *   <li>Horizontal segment (jx1,jy)↔(jx1+1,jy): a horizontal bar centred on junction row jy.</li>
     *   <li>Vertical segment (jx,jy1)↔(jx,jy1+1): a vertical bar centred on junction column jx.</li>
     * </ul>
     */
    private static void drawRoadSegments(java.util.List<int[]> path, ShapeRenderer sr,
            float mapStartX, float mapStartY, float cellSize, float borderSize,
            int startCellX, int startCellY, int endCellX, int endCellY,
            float fracOffsetX, float fracOffsetY, int visibleCellsY) {
        float roadW = borderSize * 2f;
        for (int i = 0; i < path.size() - 1; i++) {
            int[] j1 = path.get(i);
            int[] j2 = path.get(i + 1);
            int jx1 = j1[0], jy1 = j1[1];
            int jx2 = j2[0], jy2 = j2[1];
            // Skip if both junctions are outside the visible area
            boolean vis1 = jx1 >= startCellX && jx1 <= endCellX && jy1 >= startCellY && jy1 <= endCellY;
            boolean vis2 = jx2 >= startCellX && jx2 <= endCellX && jy2 >= startCellY && jy2 <= endCellY;
            if (!vis1 && !vis2) continue;
            if (jy1 == jy2) {
                // Horizontal segment: bar centred on junction row jy1.
                // Screen Y of junction jy = barY + cellSize (barY is the bottom of cell jy;
                // the junction itself lies at the top of cell jy = barY + cellSize).
                float barX = mapStartX + (Math.min(jx1, jx2) - startCellX - fracOffsetX) * cellSize;
                float barY = mapStartY + (visibleCellsY - 1 - (jy1 - startCellY - fracOffsetY)) * cellSize;
                sr.rect(barX, barY + cellSize - borderSize, cellSize, roadW);
            } else if (jx1 == jx2) {
                // Vertical segment: bar centred on junction column jx1
                // barY = bottom of cell row min(jy1,jy2); segment spans that cell upward
                float barX = mapStartX + (jx1 - startCellX - fracOffsetX) * cellSize;
                float barY = mapStartY + (visibleCellsY - 1 - (Math.min(jy1, jy2) - startCellY - fracOffsetY)) * cellSize;
                sr.rect(barX - borderSize, barY, roadW, cellSize);
            }
        }
    }

    /**
     * Returns the bevel thickness in screen pixels for the given cell size.
     */
    private static float bevelSize(float cellSize) {
        return Math.max(MIN_BEVEL_SIZE_PX, cellSize * BEVEL_SIZE_RATIO);
    }

    /**
     * Draws a beveled rectangle to give a raised 3-D appearance.
     * Light-gray triangles are drawn on the top and left edges (highlight),
     * and dark-gray triangles are drawn on the bottom and right edges (shadow).
     * The ShapeRenderer must already be in {@link ShapeRenderer.ShapeType#Filled} mode.
     *
     * @param sr  ShapeRenderer in Filled mode
     * @param x   left edge of the rectangle
     * @param y   bottom edge of the rectangle (LibGDX Y-up)
     * @param w   width of the rectangle
     * @param h   height of the rectangle
     * @param b   bevel thickness in pixels
     */
    private void drawBeveledRect(ShapeRenderer sr, float x, float y, float w, float h, float b) {
        // Top edge highlight (light gray)
        sr.setColor(BEVEL_LIGHT_COLOR);
        sr.triangle(x,       y + h,     x + w,     y + h,     x + w - b, y + h - b);
        sr.triangle(x,       y + h,     x + w - b, y + h - b, x + b,     y + h - b);
        // Left edge highlight (light gray)
        sr.triangle(x,       y,         x,         y + h,     x + b,     y + h - b);
        sr.triangle(x,       y,         x + b,     y + h - b, x + b,     y + b);
        // Bottom edge shadow (dark gray)
        sr.setColor(BEVEL_DARK_COLOR);
        sr.triangle(x,       y,         x + w,     y,         x + w - b, y + b);
        sr.triangle(x,       y,         x + w - b, y + b,     x + b,     y + b);
        // Right edge shadow (dark gray)
        sr.triangle(x + w,   y,         x + w,     y + h,     x + w - b, y + h - b);
        sr.triangle(x + w,   y,         x + w - b, y + h - b, x + w - b, y + b);
    }

    /**
     * Draws a filled 5-pointed star centred at (cx, cy) with outer radius R and inner radius r,
     * using the ShapeRenderer already in Filled mode.
     */
    private static void drawFilledStar(ShapeRenderer sr, float cx, float cy, float R, float r) {
        float[] vx = new float[10];
        float[] vy = new float[10];
        for (int i = 0; i < 5; i++) {
            double outerAngle = -Math.PI / 2 + i * 2 * Math.PI / 5;
            double innerAngle = outerAngle + Math.PI / 5;
            vx[i * 2]     = cx + (float)(R * Math.cos(outerAngle));
            vy[i * 2]     = cy + (float)(R * Math.sin(outerAngle));
            vx[i * 2 + 1] = cx + (float)(r * Math.cos(innerAngle));
            vy[i * 2 + 1] = cy + (float)(r * Math.sin(innerAngle));
        }
        for (int i = 0; i < 10; i++) {
            int next = (i + 1) % 10;
            sr.triangle(cx, cy, vx[i], vy[i], vx[next], vy[next]);
        }
    }
}

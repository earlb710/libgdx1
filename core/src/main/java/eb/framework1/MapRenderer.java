package eb.framework1;

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
class MapRenderer {

    // --- Colors ---
    private static final Color RULER_BG_COLOR        = new Color(0.1f,  0.1f,  0.15f, 1f);
    private static final Color RULER_MARKER_COLOR    = new Color(1f,   0.5f,  0f,    1f);
    private static final Color SELECTION_COLOR       = new Color(1f,   1f,    0f,    1f);
    private static final Color ROUTE_HIGHLIGHT_COLOR = new Color(0f,   0.8f,  1f,    1f);
    private static final Color REST_INDICATOR_COLOR  = new Color(0f,   0.8f,  0.2f,  1f);
    private static final Color SLEEP_INDICATOR_COLOR = new Color(0.2f, 0.3f,  0.9f,  1f);

    private static final String[] HEX_DIGITS = {
        "0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"
    };
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

    MapRenderer(SpriteBatch batch, ShapeRenderer shapeRenderer,
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

    void loadBuildingIcons() {
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
                        Gdx.app.log("MapRenderer", "Could not load icon: " + iconPath + " – " + e.getMessage());
                    }
                }
            }
        }
        Gdx.app.log("MapRenderer", "Loaded " + iconTextureCache.size() + " building icons");
    }

    void setCharacterIconTexture(Texture tex) {
        this.characterIconTexture = tex;
    }

    void dispose() {
        if (iconTextureCache != null) {
            for (Texture tex : iconTextureCache.values()) tex.dispose();
            iconTextureCache.clear();
        }
        if (characterIconTexture != null) {
            characterIconTexture.dispose();
            characterIconTexture = null;
        }
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    void drawMap(MapViewState s) {
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

        // Route path highlight (uniform cell-border rect)
        if (s.currentRoute != null && s.currentRoute.isReachable() && s.currentRoute.path != null) {
            int thickness = Math.max(1, Math.round(borderSize));
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(ROUTE_HIGHLIGHT_COLOR);
            for (int[] pathCell : s.currentRoute.path) {
                int cx = pathCell[0], cy = pathCell[1];
                if (cx >= startCellX && cx < endCellX && cy >= startCellY && cy < endCellY) {
                    float drawX = mapStartX + (cx - startCellX - fracOffsetX) * cellSize;
                    float drawY = mapStartY + (visibleCellsY - 1 - (cy - startCellY - fracOffsetY)) * cellSize;
                    for (int i = 0; i < thickness; i++) {
                        shapeRenderer.rect(drawX + i, drawY + i, cellSize - 2 * i, cellSize - 2 * i);
                    }
                }
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
        if (characterIconTexture != null && s.charCellX >= 0 && s.charCellY >= 0
                && s.charCellX >= startCellX && s.charCellX < endCellX
                && s.charCellY >= startCellY && s.charCellY < endCellY) {
            float drawX = mapStartX + (s.charCellX - startCellX - fracOffsetX) * cellSize;
            float drawY = mapStartY + (visibleCellsY - 1 - (s.charCellY - startCellY - fracOffsetY)) * cellSize;
            float portraitSize = cellSize * 0.4f;
            batch.setColor(Color.WHITE);
            batch.draw(characterIconTexture,
                    drawX + cellSize - portraitSize - borderSize, drawY + borderSize,
                    portraitSize, portraitSize);
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

    void drawRulers(MapViewState s) {
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

    static float borderInset(RoadType type, float borderSize, float pathwaySize) {
        switch (type) {
            case ROAD:    return borderSize;
            case PATHWAY: return pathwaySize;
            default:      return 0;
        }
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

package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.Random;

public class MainScreen implements Screen {
    private Main game;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private GlyphLayout glyphLayout;
    private boolean initialized = false;

    // Map dimensions: 15 cols × 17 rows × 32 px = 480 × 544 (window is 480 × 640).
    private static final int MAP_ROWS = 17;
    private static final int MAP_COLS = 15;
    private static final float CELL_SIZE = 32f;
    private static final float BORDER_THICKNESS = 2f;
    // Origins allow the map to be repositioned without changing buildRenderData() call sites.
    private static final float MAP_ORIGIN_X = 0f;
    private static final float MAP_ORIGIN_Y = 0f;

    private CellRenderData[][] renderData;
    private Profile profile;

    public MainScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        try {
            batch = new SpriteBatch();
            shapeRenderer = new ShapeRenderer();
            glyphLayout = new GlyphLayout();
            font = game.getFontManager().getBodyFont();

            profile = game.getProfileManager().getSelectedProfile();
            long seed = (profile != null) ? profile.getRandSeed() : System.currentTimeMillis();

            TerrainType[][] terrain = generateTerrain(MAP_ROWS, MAP_COLS, seed);
            RoadAccessMap roadMap = new RoadAccessMap(terrain);
            roadMap.removeRoads(seed);
            renderData = roadMap.buildRenderData(
                    terrain, MAP_ORIGIN_X, MAP_ORIGIN_Y, CELL_SIZE, CELL_SIZE, BORDER_THICKNESS);

            initialized = true;
            Gdx.app.log("MainScreen", "Map generated and render data built successfully");
        } catch (Exception e) {
            Gdx.app.error("MainScreen", "Error in show(): " + e.getMessage(), e);
        }
    }

    /**
     * Generates a randomised terrain grid using the supplied seed.
     * Distribution: 40% PLAINS, 20% FOREST, 15% WATER, 15% MOUNTAIN, 10% BEACH.
     */
    private TerrainType[][] generateTerrain(int rows, int cols, long seed) {
        Random rng = new Random(seed);
        TerrainType[][] terrain = new TerrainType[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int roll = rng.nextInt(100);
                if (roll < 40) {
                    terrain[r][c] = TerrainType.PLAINS;
                } else if (roll < 60) {
                    terrain[r][c] = TerrainType.FOREST;
                } else if (roll < 75) {
                    terrain[r][c] = TerrainType.WATER;
                } else if (roll < 90) {
                    terrain[r][c] = TerrainType.MOUNTAIN;
                } else {
                    terrain[r][c] = TerrainType.BEACH;
                }
            }
        }
        return terrain;
    }

    @Override
    public void render(float delta) {
        if (!initialized) {
            ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
            return;
        }

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        // Draw cell fills.
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (CellRenderData[] row : renderData) {
            for (CellRenderData cell : row) {
                shapeRenderer.setColor(cell.getFillColor());
                shapeRenderer.rect(cell.getX(), cell.getY(), cell.getWidth(), cell.getHeight());
            }
        }
        shapeRenderer.end();

        // Draw border segments (only sides with road access).
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (CellRenderData[] row : renderData) {
            for (CellRenderData cell : row) {
                shapeRenderer.setColor(cell.getBorderColor());
                for (float[] seg : cell.getBorderSegments()) {
                    shapeRenderer.rect(seg[0], seg[1], seg[2], seg[3]);
                }
            }
        }
        shapeRenderer.end();

        // Draw HUD at the top.
        if (profile != null) {
            batch.begin();
            String hud = profile.getCharacterName()
                    + "  |  " + profile.getDifficulty()
                    + "  |  Year " + profile.getGameDate();
            glyphLayout.setText(font, hud);
            font.draw(batch, hud,
                    (Gdx.graphics.getWidth() - glyphLayout.width) / 2f,
                    Gdx.graphics.getHeight() - 10f);
            batch.end();
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }
    }
}

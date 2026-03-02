package eb.gmodel1.city;

import eb.gmodel1.character.*;
import eb.gmodel1.generator.*;
import eb.gmodel1.save.*;
import eb.gmodel1.ui.*;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Represents a 16x16 city map grid.
 * Generated using the profile's random seed for deterministic map creation.
 * 
 * Features:
 * - 10% of non-beach cells are mountains (not accessible)
 * - One random side is beach
 * - Each non-mountain, non-beach cell has a building with 4 improvements
 * - Buildings are selected using weighted random based on percentage distribution
 * - Pre-computed render data (rectangle + color) stored for each cell for efficient redrawing
 */
public class CityMap {
    public static final int MAP_SIZE = 16;
    private static final double MOUNTAIN_PERCENTAGE = 0.10;

    // Color constants for terrain types (used in render data pre-computation)
    private static final float MOUNTAIN_R = 0.4f, MOUNTAIN_G = 0.35f, MOUNTAIN_B = 0.3f;
    private static final float BEACH_R = 0.95f, BEACH_G = 0.9f, BEACH_B = 0.6f;
    private static final float GRAY_R = 0.5f, GRAY_G = 0.5f, GRAY_B = 0.5f;

    // Floor-based brightness constants for building color calculation
    private static final float MIN_BRIGHTNESS = 0.55f;
    private static final int MAX_FLOORS = 10;
    
    // Fallback building types (used when GameDataManager is not provided)
    private static final String[] FALLBACK_BUILDING_TYPES = {
        "Residential", "Commercial", "Industrial", "Office",
        "Hospital", "School", "Police Station", "Fire Station",
        "Park", "Library", "Mall", "Restaurant"
    };
    
    private static final String[] FALLBACK_IMPROVEMENT_TYPES = {
        "Security System", "Solar Panels", "Water Recycling", "Garden",
        "Parking Garage", "Elevator", "HVAC System", "Internet",
        "Generator", "Storage", "Rooftop Deck", "Fitness Center"
    };

    /**
     * Required building groups: at least one ID in each inner array must appear on the map.
     * Groups with multiple IDs are alternatives (e.g. small or large hospital both satisfy
     * the "hospital" requirement).
     */
    private static final String[][] REQUIRED_BUILDING_GROUPS = {
        { "hospital_small", "hospital_large" },
        { "police_station" },
        { "prison" },
        { "library" },
        { "gym_fitness_center" },
        { "office_building_small" }
    };

    /**
     * Company type IDs that should NOT receive generated company names.
     * These are public/civic/institutional buildings.
     */
    private static final java.util.Set<String> SKIP_COMPANY_TYPES;

    /**
     * Buildings that host multiple companies.
     * Values are [minTenants, maxTenants] (inclusive).
     */
    private static final Map<String, int[]> MULTI_TENANT_RANGE;

    static {
        SKIP_COMPANY_TYPES = new java.util.HashSet<>(Arrays.asList(
                "government", "education", "residential"));

        MULTI_TENANT_RANGE = new HashMap<>();
        MULTI_TENANT_RANGE.put("office_building_small",  new int[]{2, 4});
        MULTI_TENANT_RANGE.put("office_building_medium", new int[]{3, 6});
        MULTI_TENANT_RANGE.put("office_building_large",  new int[]{5, 10});
        MULTI_TENANT_RANGE.put("corporate_headquarters", new int[]{1, 2});
        MULTI_TENANT_RANGE.put("strip_mall",             new int[]{4, 8});
        MULTI_TENANT_RANGE.put("shopping_center",        new int[]{8, 15});
        MULTI_TENANT_RANGE.put("regional_mall",          new int[]{12, 20});
        MULTI_TENANT_RANGE.put("coworking_space",        new int[]{3, 7});
    }

    /**
     * Represents the four sides of the map for beach placement.
     */
    public enum Side {
        NORTH, SOUTH, EAST, WEST
    }

    private final Cell[][] cells;
    private final CellRenderData[][] renderData;
    private final RoadAccessMap roadAccessMap;
    private final Side beachSide;
    private final long seed;
    private final GameDataManager gameData;

    /**
     * Creates a new city map using the provided profile's random seed.
     * Uses fallback building types.
     * 
     * @param profile The profile containing the random seed for map generation
     */
    public CityMap(Profile profile) {
        this(profile.getRandSeed(), null);
    }

    /**
     * Creates a new city map using the provided random seed.
     * Uses fallback building types.
     * 
     * @param seed The random seed for deterministic map generation
     */
    public CityMap(long seed) {
        this(seed, null);
    }

    /**
     * Creates a new city map using the provided profile and game data.
     * Uses building definitions from GameDataManager for weighted distribution.
     * 
     * @param profile The profile containing the random seed for map generation
     * @param gameData The GameDataManager containing building definitions
     */
    public CityMap(Profile profile, GameDataManager gameData) {
        this(profile.getRandSeed(), gameData);
    }

    /**
     * Creates a new city map using the provided random seed and game data.
     * Uses building definitions from GameDataManager for weighted distribution.
     * 
     * @param seed The random seed for deterministic map generation
     * @param gameData The GameDataManager containing building definitions (can be null for fallback)
     */
    public CityMap(long seed, GameDataManager gameData) {
        this.seed = seed;
        this.cells = new Cell[MAP_SIZE][MAP_SIZE];
        this.renderData = new CellRenderData[MAP_SIZE][MAP_SIZE];
        this.gameData = gameData;
        
        Random random = new Random(seed);
        
        // Determine which side is the beach
        Side[] sides = Side.values();
        this.beachSide = sides[random.nextInt(sides.length)];
        
        // Calculate number of mountain cells (10% of total, excluding beach side)
        int totalCells = MAP_SIZE * MAP_SIZE;
        int beachCells = MAP_SIZE;
        int availableCells = totalCells - beachCells;
        int mountainCount = (int) Math.round(availableCells * MOUNTAIN_PERCENTAGE);
        
        // Track which cells are mountains
        boolean[][] isMountain = new boolean[MAP_SIZE][MAP_SIZE];
        int mountainsPlaced = 0;
        
        // Place mountains randomly (excluding beach side)
        while (mountainsPlaced < mountainCount) {
            int x = random.nextInt(MAP_SIZE);
            int y = random.nextInt(MAP_SIZE);
            
            if (!isBeachCell(x, y) && !isMountain[x][y]) {
                isMountain[x][y] = true;
                mountainsPlaced++;
            }
        }
        
        // Create all cells
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                TerrainType terrain;
                Building building = null;
                
                if (isBeachCell(x, y)) {
                    terrain = TerrainType.BEACH;
                } else if (isMountain[x][y]) {
                    terrain = TerrainType.MOUNTAIN;
                } else {
                    terrain = TerrainType.BUILDING;
                    building = generateBuilding(random, x, y);
                }
                
                cells[x][y] = new Cell(x, y, terrain, building);
            }
        }
        
        // Guarantee all required buildings appear at least once
        if (gameData != null) {
            ensureRequiredBuildings(random);
        }
        
        // Build road access map and remove ~30% of roads while preserving connectivity
        this.roadAccessMap = new RoadAccessMap(this);
        this.roadAccessMap.removeRoads(seed);
        
        // Pre-compute render data for all cells to avoid repeated calculations during drawing
        buildRenderData();
    }

    /**
     * Checks if the given coordinates are on the beach side.
     */
    private boolean isBeachCell(int x, int y) {
        switch (beachSide) {
            case NORTH:
                return y == MAP_SIZE - 1;
            case SOUTH:
                return y == 0;
            case EAST:
                return x == MAP_SIZE - 1;
            case WEST:
                return x == 0;
            default:
                return false;
        }
    }

    /**
     * Pre-computes render data (rectangle, color, and border flags) for all cells.
     * This is called once after map generation so that the rendering loop
     * can use the stored data directly without recalculating each frame.
     * Border flags are derived from the road access map: a border is drawn on
     * sides where road access exists (representing the road gap), and no border
     * is drawn where there is no road access (building extends to edge).
     */
    private void buildRenderData() {
        // Build a category color lookup map from GameDataManager
        Map<String, float[]> categoryColors = new HashMap<>();
        if (gameData != null) {
            for (CategoryDefinition cat : gameData.getCategories()) {
                categoryColors.put(cat.getId(), cat.getColorFloats());
            }
        }

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                Cell cell = cells[x][y];
                float r, g, b, a = 1.0f;

                switch (cell.getTerrainType()) {
                    case MOUNTAIN:
                        r = MOUNTAIN_R;
                        g = MOUNTAIN_G;
                        b = MOUNTAIN_B;
                        break;
                    case BEACH:
                        r = BEACH_R;
                        g = BEACH_G;
                        b = BEACH_B;
                        break;
                    case BUILDING:
                        if (cell.hasBuilding() && cell.getBuilding().getDefinition() != null) {
                            Building building = cell.getBuilding();
                            String categoryId = building.getDefinition().getCategory();
                            float[] baseColor = categoryColors.get(categoryId);
                            if (baseColor != null) {
                                float brightness = MIN_BRIGHTNESS
                                    + (1.0f - MIN_BRIGHTNESS) * (building.getFloors() / (float) MAX_FLOORS);
                                r = Math.min(1.0f, baseColor[0] * brightness);
                                g = Math.min(1.0f, baseColor[1] * brightness);
                                b = Math.min(1.0f, baseColor[2] * brightness);
                            } else {
                                r = GRAY_R;
                                g = GRAY_G;
                                b = GRAY_B;
                            }
                        } else {
                            r = GRAY_R;
                            g = GRAY_G;
                            b = GRAY_B;
                        }
                        break;
                    default:
                        r = GRAY_R;
                        g = GRAY_G;
                        b = GRAY_B;
                        break;
                }

                // Get road access for border types
                RoadAccess ra = roadAccessMap.getAccess(x, y);
                // Get icon path for building cells
                String iconPath = null;
                if (cell.getTerrainType() == TerrainType.BUILDING
                        && cell.hasBuilding() && cell.getBuilding().getDefinition() != null) {
                    iconPath = cell.getBuilding().getDefinition().getIconPath();
                }
                renderData[x][y] = new CellRenderData(x, y, 1, 1, r, g, b, a,
                    ra.getNorthType(), ra.getSouthType(), ra.getEastType(), ra.getWestType(),
                    iconPath);
            }
        }
    }

    /**
     * Generates a building using either GameDataManager definitions or fallback.
     */
    private Building generateBuilding(Random random, int x, int y) {
        if (gameData != null && !gameData.getBuildings().isEmpty()) {
            return generateBuildingFromDefinition(random, x, y);
        } else {
            return generateFallbackBuilding(random, x, y);
        }
    }

    /**
     * Generates a building using BuildingDefinition with weighted random selection.
     */
    private Building generateBuildingFromDefinition(Random random, int x, int y) {
        BuildingDefinition definition = selectWeightedBuilding(random);
        return buildingFromDefinition(definition, random, x, y);
    }

    /**
     * Creates a Building instance from an explicit BuildingDefinition.
     * Shared by the normal weighted-random generation path and the
     * required-building guarantee path.
     */
    private Building buildingFromDefinition(BuildingDefinition definition, Random random, int x, int y) {
        // Generate random number of floors within the definition's range
        int minFloors = Math.max(1, definition.getMinFloors());
        int maxFloors = Math.max(minFloors, definition.getMaxFloors());
        int floors = minFloors + random.nextInt(maxFloors - minFloors + 1);
        
        // Building name is the definition type name (no coordinates suffix)
        String buildingName = definition.getName();
        
        // Select 4 random improvements from the definition's improvement list
        List<String> availableImprovements = definition.getImprovements();
        List<Improvement> selectedImprovements = new ArrayList<>();
        
        if (availableImprovements.size() >= 4) {
            // Shuffle and pick first 4
            List<String> shuffled = new ArrayList<>(availableImprovements);
            Collections.shuffle(shuffled, random);
            for (int i = 0; i < 4; i++) {
                int level = random.nextInt(5) + 1; // Levels 1-5
                int hiddenValue = random.nextInt(6); // Hidden value 0-5
                selectedImprovements.add(new Improvement(shuffled.get(i), level, hiddenValue));
            }
        } else if (!availableImprovements.isEmpty()) {
            // Not enough improvements, use what's available and fill with duplicates
            for (int i = 0; i < 4; i++) {
                String impName = availableImprovements.get(random.nextInt(availableImprovements.size()));
                int level = random.nextInt(5) + 1;
                int hiddenValue = random.nextInt(6); // Hidden value 0-5
                selectedImprovements.add(new Improvement(impName, level, hiddenValue));
            }
        } else {
            // No improvements defined, use fallback improvements
            for (int i = 0; i < 4; i++) {
                String impName = FALLBACK_IMPROVEMENT_TYPES[random.nextInt(FALLBACK_IMPROVEMENT_TYPES.length)];
                int level = random.nextInt(5) + 1;
                int hiddenValue = random.nextInt(6); // Hidden value 0-5
                selectedImprovements.add(new Improvement(impName, level, hiddenValue));
            }
        }
        
        Building building = new Building(buildingName, selectedImprovements, definition, floors);
        building.setState(computeBuildingState(random, x, y));

        // Assign company / tenant names for company-type buildings
        List<String> tenants = generateTenants(definition, random);
        if (!tenants.isEmpty()) {
            building.setTenants(tenants);
        }

        return building;
    }

    /**
     * Generates company / tenant names for a building definition.
     *
     * <p>Returns an empty list for non-company buildings (government, education,
     * residential, religious, infrastructure, transit).  Single-company buildings
     * receive exactly 1 name; multi-tenant buildings (office blocks, strip malls,
     * shopping centres, etc.) receive between the defined min and max number of
     * names pulled from the {@link CompanyNameGenerator}.
     *
     * <p>The supplied {@link Random} is the map seed's random instance so that
     * names are fully deterministic for a given map seed.
     */
    private List<String> generateTenants(BuildingDefinition definition, Random random) {
        if (gameData == null) return new ArrayList<>();
        CompanyNameGenerator gen = gameData.getCompanyNameGenerator();
        if (gen == null) return new ArrayList<>();

        String buildingId = definition.getId();
        String typeId = gen.getTypeForBuilding(buildingId);

        // No mapping → not a company building, or explicitly skipped
        if (typeId == null) return new ArrayList<>();
        if (SKIP_COMPANY_TYPES.contains(typeId)) return new ArrayList<>();

        // Determine tenant count
        int[] range = MULTI_TENANT_RANGE.get(buildingId);
        int count;
        if (range != null) {
            count = range[0] + random.nextInt(range[1] - range[0] + 1);
        } else {
            count = 1;
        }

        List<String> tenants = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tenants.add(gen.generateForBuilding(buildingId, random));
        }
        return tenants;
    }

    /**
     * Selects a building definition using weighted random based on percentage.
     */
    private BuildingDefinition selectWeightedBuilding(Random random) {
        List<BuildingDefinition> buildings = gameData.getBuildings();
        
        // Calculate total weight
        double totalWeight = 0;
        for (BuildingDefinition b : buildings) {
            totalWeight += b.getPercentage();
        }
        
        // Handle edge case where all percentages are 0 - use uniform random
        if (totalWeight <= 0) {
            return buildings.get(random.nextInt(buildings.size()));
        }
        
        // Select random value
        double randomValue = random.nextDouble() * totalWeight;
        
        // Find the building that corresponds to this value
        double cumulative = 0;
        for (BuildingDefinition b : buildings) {
            cumulative += b.getPercentage();
            if (randomValue <= cumulative) {
                return b;
            }
        }
        
        // Fallback to last building (shouldn't happen)
        return buildings.get(buildings.size() - 1);
    }

    /**
     * Computes the maintenance/condition state for a building at the given map coordinates.
     *
     * <p>Buildings closer to the centre of the map have a higher probability of being in
     * a "good" state; buildings near the edges are more likely to be in a "bad" state.
     * The three returned values are "good", "normal", and "bad".</p>
     *
     * <p>The algorithm normalises the Euclidean distance from the cell to the map centre
     * ({@code (MAP_SIZE-1)/2.0}) on the range [0, 1], then uses that normalised distance
     * to weight the probability thresholds:</p>
     * <ul>
     *   <li>At centre (dist=0): 60% good, 30% normal, 10% bad</li>
     *   <li>At edge   (dist=1): 10% good, 30% normal, 60% bad</li>
     * </ul>
     */
    static String computeBuildingState(Random random, int x, int y) {
        double cx = (MAP_SIZE - 1) / 2.0;
        double cy = (MAP_SIZE - 1) / 2.0;
        double maxDist = Math.sqrt(cx * cx + cy * cy);
        double dist = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
        double normDist = dist / maxDist; // 0 = centre, 1 = corner

        // Probability of "good" decreases linearly from 0.60 (centre) to 0.10 (edge)
        double goodProb   = 0.60 - normDist * 0.50;
        // Probability of "bad" increases linearly from 0.10 (centre) to 0.60 (edge)
        double badProb    = 0.10 + normDist * 0.50;
        // "normal" takes the remainder (always ~0.30)

        double roll = random.nextDouble();
        if (roll < goodProb) return "good";
        if (roll < goodProb + (1.0 - goodProb - badProb)) return "normal";
        return "bad";
    }

    /**
     * Generates a building using fallback (hardcoded) types.
     */
    private Building generateFallbackBuilding(Random random, int x, int y) {
        String buildingName = FALLBACK_BUILDING_TYPES[random.nextInt(FALLBACK_BUILDING_TYPES.length)] + "-" + x + "," + y;
        
        List<Improvement> improvements = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String improvementName = FALLBACK_IMPROVEMENT_TYPES[random.nextInt(FALLBACK_IMPROVEMENT_TYPES.length)];
            int level = random.nextInt(5) + 1; // Levels 1-5
            int hiddenValue = random.nextInt(6); // Hidden value 0-5
            improvements.add(new Improvement(improvementName, level, hiddenValue));
        }
        
        return new Building(buildingName, improvements);
    }

    /**
     * Ensures that each required building group has at least one representative on the map.
     * For any group that is absent, a random building cell is replaced with the first
     * listed building ID in that group.  Replacement cells are chosen from a shuffled
     * list of all building cells, so required buildings are spread around the map.
     */
    private void ensureRequiredBuildings(Random random) {
        // Collect all building cells
        List<Cell> buildingCells = new ArrayList<>();
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                if (cells[x][y].getTerrainType() == TerrainType.BUILDING) {
                    buildingCells.add(cells[x][y]);
                }
            }
        }
        // Shuffle so replacements are spread around the map rather than clustered
        Collections.shuffle(buildingCells, random);
        int nextReplacementIdx = 0;

        for (String[] group : REQUIRED_BUILDING_GROUPS) {
            // Check whether any building cell already satisfies this group
            boolean found = false;
            outer:
            for (Cell cell : buildingCells) {
                Building b = cell.getBuilding();
                if (b == null || b.getDefinition() == null) continue;
                String defId = b.getDefinition().getId();
                for (String id : group) {
                    if (id.equals(defId)) {
                        found = true;
                        break outer;
                    }
                }
            }

            if (!found) {
                // Use the first ID in the group as the preferred replacement
                BuildingDefinition def = gameData.getBuildingById(group[0]);
                if (def == null) {
                    System.err.println("CityMap: required building definition '" + group[0]
                            + "' not found in game data – cannot guarantee its presence on the map.");
                    continue;
                }
                if (nextReplacementIdx >= buildingCells.size()) {
                    System.err.println("CityMap: not enough building cells to place required building '"
                            + group[0] + "' – map may be too small.");
                    break;
                }
                Cell cell = buildingCells.get(nextReplacementIdx++);
                Building newBuilding = buildingFromDefinition(def, random, cell.getX(), cell.getY());
                cells[cell.getX()][cell.getY()] =
                        new Cell(cell.getX(), cell.getY(), TerrainType.BUILDING, newBuilding);
            }
        }
    }

    /**
     * Gets the cell at the specified coordinates.
     * 
     * @param x The x coordinate (0 to MAP_SIZE-1)
     * @param y The y coordinate (0 to MAP_SIZE-1)
     * @return The cell at the specified position
     * @throws IllegalArgumentException if coordinates are out of bounds
     */
    public Cell getCell(int x, int y) {
        if (x < 0 || x >= MAP_SIZE || y < 0 || y >= MAP_SIZE) {
            throw new IllegalArgumentException("Coordinates out of bounds: (" + x + ", " + y + ")");
        }
        return cells[x][y];
    }

    /**
     * Gets the pre-computed render data for the cell at the specified coordinates.
     * Contains the rectangle (x, y, width, height) and pre-computed color,
     * allowing efficient redrawing without recalculating terrain and building colors.
     *
     * @param x The x coordinate (0 to MAP_SIZE-1)
     * @param y The y coordinate (0 to MAP_SIZE-1)
     * @return The pre-computed render data for the cell
     * @throws IllegalArgumentException if coordinates are out of bounds
     */
    public CellRenderData getCellRenderData(int x, int y) {
        if (x < 0 || x >= MAP_SIZE || y < 0 || y >= MAP_SIZE) {
            throw new IllegalArgumentException("Coordinates out of bounds: (" + x + ", " + y + ")");
        }
        return renderData[x][y];
    }

    /**
     * Gets all cells in the map as a 2D array.
     * 
     * @return A copy of the cells array
     */
    public Cell[][] getCells() {
        Cell[][] copy = new Cell[MAP_SIZE][MAP_SIZE];
        for (int x = 0; x < MAP_SIZE; x++) {
            System.arraycopy(cells[x], 0, copy[x], 0, MAP_SIZE);
        }
        return copy;
    }

    /**
     * Gets the side of the map that has the beach.
     * 
     * @return The beach side
     */
    public Side getBeachSide() {
        return beachSide;
    }

    /**
     * Gets the road access map for this city map.
     * 
     * @return The road access map
     */
    public RoadAccessMap getRoadAccessMap() {
        return roadAccessMap;
    }

    /**
     * Gets the seed used to generate this map.
     * 
     * @return The random seed
     */
    public long getSeed() {
        return seed;
    }

    /**
     * Gets the size of the map (both width and height).
     * 
     * @return The map size (16)
     */
    public int getSize() {
        return MAP_SIZE;
    }

    /**
     * Counts the number of cells with a specific terrain type.
     * 
     * @param terrainType The terrain type to count
     * @return The number of cells with that terrain type
     */
    public int countTerrain(TerrainType terrainType) {
        int count = 0;
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                if (cells[x][y].getTerrainType() == terrainType) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Counts the number of buildings in the map.
     * 
     * @return The number of cells with buildings
     */
    public int countBuildings() {
        int count = 0;
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                if (cells[x][y].hasBuilding()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Holds the result of a pathfinding operation between two cells.
     */
    public static class RouteResult {
        /** Travel time in minutes per road cell. */
        public static final int ROAD_MINUTES_PER_CELL = 5;
        /** Travel time in minutes per path cell (4× slower than road). */
        public static final int PATH_MINUTES_PER_CELL = 20;

        /** Path as a list of {x, y} cell coordinates, from start to end. */
        public final List<int[]> path;
        /** Total travel time in minutes. -1 if unreachable. */
        public final int totalMinutes;

        public RouteResult(List<int[]> path, int totalMinutes) {
            this.path = path;
            this.totalMinutes = totalMinutes;
        }

        public boolean isReachable() {
            return totalMinutes >= 0;
        }

        /** Returns a human-readable travel time string, e.g. "35 min" or "1h 5min". */
        public String formatTime() {
            if (!isReachable()) return "Unreachable";
            if (totalMinutes < 60) return totalMinutes + " min";
            return (totalMinutes / 60) + "h " + (totalMinutes % 60) + "min";
        }
    }

    /**
     * Finds the fastest route between two cells by routing along physically connected road
     * segments on the junction graph.
     *
     * <p>The city grid has junction nodes at every cell corner: (jx, jy) ∈ [0..MAP_SIZE].
     * Road segments connect adjacent junctions:</p>
     * <ul>
     *   <li><b>Horizontal</b> (jx,jy)↔(jx+1,jy): travels east-west along the boundary
     *       between cell rows jy-1 and jy.  Exists when {@code access[jx][jy-1].hasNorth()}.</li>
     *   <li><b>Vertical</b> (jx,jy)↔(jx,jy+1): travels north-south along the boundary
     *       between cell columns jx-1 and jx.  Exists when {@code access[jx-1][jy].hasEast()}.</li>
     * </ul>
     * <p>Two road segments are connected only when they share a junction.  This prevents the
     * character from "jumping" directly from one cell border to a non-adjacent parallel border.</p>
     *
     * <p>The returned {@link RouteResult#path} contains junction coordinates [jx, jy],
     * not cell coordinates.</p>
     *
     * @param fromX Start cell X coordinate
     * @param fromY Start cell Y coordinate
     * @param toX   Destination X coordinate
     * @param toY   Destination Y coordinate
     * @return A {@link RouteResult} with the junction path and total travel time
     */
    public RouteResult findFastestRoute(int fromX, int fromY, int toX, int toY) {
        if (fromX == toX && fromY == toY) {
            List<int[]> p = new ArrayList<>();
            p.add(new int[]{fromX, fromY});
            return new RouteResult(p, 0);
        }

        int N = MAP_SIZE + 1; // junction indices: 0..MAP_SIZE (17 values per axis)
        int[][] dist   = new int[N][N];
        int[][] prevJX = new int[N][N];
        int[][] prevJY = new int[N][N];
        for (int[] row : dist)   Arrays.fill(row, Integer.MAX_VALUE);
        for (int[] row : prevJX) Arrays.fill(row, -1);
        for (int[] row : prevJY) Arrays.fill(row, -1);

        // Multi-source: start at all 4 corners of the source cell with cost 0
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
        for (int djx = 0; djx <= 1; djx++) {
            for (int djy = 0; djy <= 1; djy++) {
                int jx = fromX + djx, jy = fromY + djy;
                if (dist[jx][jy] > 0) {
                    dist[jx][jy] = 0;
                    pq.offer(new int[]{0, jx, jy});
                }
            }
        }

        // Mark destination corners
        boolean[][] isDest = new boolean[N][N];
        for (int djx = 0; djx <= 1; djx++)
            for (int djy = 0; djy <= 1; djy++)
                isDest[toX + djx][toY + djy] = true;

        int bestJX = -1, bestJY = -1;
        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int cost = curr[0], jx = curr[1], jy = curr[2];
            if (cost > dist[jx][jy]) continue;

            // Accept destination only if at least one road segment was travelled (cost > 0).
            // This prevents zero-cost "arrival" when adjacent cells share a corner junction.
            if (isDest[jx][jy] && cost > 0) {
                bestJX = jx;
                bestJY = jy;
                break;
            }

            // Horizontal neighbours (east-west along boundary between rows jy-1 and jy)
            if (jy >= 1 && jy <= MAP_SIZE - 1) {
                // East: (jx,jy) → (jx+1,jy); road = north border of cell (jx, jy-1)
                if (jx < MAP_SIZE) {
                    RoadAccess ra = roadAccessMap.getAccess(jx, jy - 1);
                    if (ra.hasNorth())
                        tryRelaxJunc(jx, jy, jx + 1, jy, cost, ra.getNorthType(), dist, prevJX, prevJY, pq);
                }
                // West: (jx,jy) → (jx-1,jy); road = north border of cell (jx-1, jy-1)
                if (jx > 0) {
                    RoadAccess ra = roadAccessMap.getAccess(jx - 1, jy - 1);
                    if (ra.hasNorth())
                        tryRelaxJunc(jx, jy, jx - 1, jy, cost, ra.getNorthType(), dist, prevJX, prevJY, pq);
                }
            }

            // Vertical neighbours (north-south along boundary between cols jx-1 and jx)
            if (jx >= 1 && jx <= MAP_SIZE - 1) {
                // North: (jx,jy) → (jx,jy+1); road = east border of cell (jx-1, jy)
                if (jy < MAP_SIZE) {
                    RoadAccess ra = roadAccessMap.getAccess(jx - 1, jy);
                    if (ra.hasEast())
                        tryRelaxJunc(jx, jy, jx, jy + 1, cost, ra.getEastType(), dist, prevJX, prevJY, pq);
                }
                // South: (jx,jy) → (jx,jy-1); road = east border of cell (jx-1, jy-1)
                if (jy > 0) {
                    RoadAccess ra = roadAccessMap.getAccess(jx - 1, jy - 1);
                    if (ra.hasEast())
                        tryRelaxJunc(jx, jy, jx, jy - 1, cost, ra.getEastType(), dist, prevJX, prevJY, pq);
                }
            }
        }

        if (bestJX < 0) {
            return new RouteResult(null, -1);
        }

        // Reconstruct junction path by tracing back from destination to source corner
        List<int[]> path = new ArrayList<>();
        int jx = bestJX, jy = bestJY;
        while (prevJX[jx][jy] != -1) {
            path.add(0, new int[]{jx, jy});
            int pjx = prevJX[jx][jy], pjy = prevJY[jx][jy];
            jx = pjx;
            jy = pjy;
        }
        path.add(0, new int[]{jx, jy}); // add source corner
        return new RouteResult(path, dist[bestJX][bestJY]);
    }

    private void tryRelaxJunc(int jx, int jy, int njx, int njy, int cost, RoadType type,
                               int[][] dist, int[][] prevJX, int[][] prevJY, PriorityQueue<int[]> pq) {
        int edgeCost = (type == RoadType.ROAD)
                ? RouteResult.ROAD_MINUTES_PER_CELL
                : RouteResult.PATH_MINUTES_PER_CELL;
        int newCost = cost + edgeCost;
        if (newCost < dist[njx][njy]) {
            dist[njx][njy] = newCost;
            prevJX[njx][njy] = jx;
            prevJY[njx][njy] = jy;
            pq.offer(new int[]{newCost, njx, njy});
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CityMap{size=").append(MAP_SIZE)
          .append(", seed=").append(seed)
          .append(", beachSide=").append(beachSide)
          .append(", mountains=").append(countTerrain(TerrainType.MOUNTAIN))
          .append(", beaches=").append(countTerrain(TerrainType.BEACH))
          .append(", buildings=").append(countBuildings())
          .append("}");
        return sb.toString();
    }

    /**
     * Returns a simple text visualization of the map.
     * M = Mountain, B = Beach, # = Building
     */
    public String toAsciiMap() {
        StringBuilder sb = new StringBuilder();
        for (int y = MAP_SIZE - 1; y >= 0; y--) {
            for (int x = 0; x < MAP_SIZE; x++) {
                switch (cells[x][y].getTerrainType()) {
                    case MOUNTAIN:
                        sb.append('M');
                        break;
                    case BEACH:
                        sb.append('B');
                        break;
                    case BUILDING:
                        sb.append('#');
                        break;
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}

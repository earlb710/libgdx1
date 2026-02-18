package eb.framework1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
 */
public class CityMap {
    public static final int MAP_SIZE = 16;
    private static final double MOUNTAIN_PERCENTAGE = 0.10;
    
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
     * Represents the four sides of the map for beach placement.
     */
    public enum Side {
        NORTH, SOUTH, EAST, WEST
    }

    private final Cell[][] cells;
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
        // Select building definition using weighted random
        BuildingDefinition definition = selectWeightedBuilding(random);
        
        // Generate random number of floors within the definition's range
        int minFloors = Math.max(1, definition.getMinFloors());
        int maxFloors = Math.max(minFloors, definition.getMaxFloors());
        int floors = minFloors + random.nextInt(maxFloors - minFloors + 1);
        
        // Build name with coordinates
        String buildingName = definition.getName() + "-" + x + "," + y;
        
        // Select 4 random improvements from the definition's improvement list
        List<String> availableImprovements = definition.getImprovements();
        List<Improvement> selectedImprovements = new ArrayList<>();
        
        if (availableImprovements.size() >= 4) {
            // Shuffle and pick first 4
            List<String> shuffled = new ArrayList<>(availableImprovements);
            Collections.shuffle(shuffled, random);
            for (int i = 0; i < 4; i++) {
                int level = random.nextInt(5) + 1; // Levels 1-5
                selectedImprovements.add(new Improvement(shuffled.get(i), level));
            }
        } else {
            // Not enough improvements, use what's available and fill with duplicates
            for (int i = 0; i < 4; i++) {
                String impName = availableImprovements.get(random.nextInt(availableImprovements.size()));
                int level = random.nextInt(5) + 1;
                selectedImprovements.add(new Improvement(impName, level));
            }
        }
        
        return new Building(buildingName, selectedImprovements, definition, floors);
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
     * Generates a building using fallback (hardcoded) types.
     */
    private Building generateFallbackBuilding(Random random, int x, int y) {
        String buildingName = FALLBACK_BUILDING_TYPES[random.nextInt(FALLBACK_BUILDING_TYPES.length)] + "-" + x + "," + y;
        
        List<Improvement> improvements = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String improvementName = FALLBACK_IMPROVEMENT_TYPES[random.nextInt(FALLBACK_IMPROVEMENT_TYPES.length)];
            int level = random.nextInt(5) + 1; // Levels 1-5
            improvements.add(new Improvement(improvementName, level));
        }
        
        return new Building(buildingName, improvements);
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

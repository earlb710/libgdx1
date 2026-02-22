package eb.framework1;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Snapshot of all mutable game state required to save and restore a game session.
 *
 * <p>The immutable map layout (terrain, building types, road network) is <em>not</em>
 * stored here because it can always be regenerated deterministically from
 * {@link #getRandSeed()}.  Only discovery/ownership state and the player's position
 * need to be persisted.
 *
 * <h3>Typical usage</h3>
 * <pre>
 *   // --- saving ---
 *   GameSave save = GameSave.from(profile, cityMap, charX, charY, homeX, homeY);
 *   saveGameManager.save(save);
 *
 *   // --- loading ---
 *   GameSave save = saveGameManager.load(characterName);
 *   if (save != null) {
 *       save.applyToProfile(profile);
 *       save.applyToMap(cityMap);
 *   }
 * </pre>
 */
public class GameSave {

    // -------------------------------------------------------------------------
    // Profile snapshot
    // -------------------------------------------------------------------------
    private final String characterName;
    private final String gender;
    private final String difficulty;
    private final String characterIcon;
    private final Map<String, Integer> attributes;
    private final int    gameDate;
    private final long   randSeed;
    private final int    money;
    private final String gameDateTime;
    private final int    currentStamina;

    // -------------------------------------------------------------------------
    // Character / home positions (cell coordinates)
    // -------------------------------------------------------------------------
    private final int charCellX;
    private final int charCellY;
    private final int homeCellX;
    private final int homeCellY;

    // -------------------------------------------------------------------------
    // Map discovery / ownership state
    // Flat arrays indexed by  x * MAP_SIZE + y  (length MAP_SIZE²)
    // Improvement array indexed by  (x * MAP_SIZE + y) * 4 + impIndex  (length MAP_SIZE²×4)
    // -------------------------------------------------------------------------
    private final boolean[] buildingDiscovered;
    private final boolean[] buildingOwned;
    private final boolean[] improvementDiscovered;

    // -------------------------------------------------------------------------
    // Constructor (package-private; use GameSave.from() or SaveGameManager)
    // -------------------------------------------------------------------------
    GameSave(String characterName, String gender, String difficulty, String characterIcon,
             Map<String, Integer> attributes,
             int gameDate, long randSeed, int money, String gameDateTime, int currentStamina,
             int charCellX, int charCellY, int homeCellX, int homeCellY,
             boolean[] buildingDiscovered, boolean[] buildingOwned,
             boolean[] improvementDiscovered) {
        this.characterName       = characterName;
        this.gender              = gender;
        this.difficulty          = difficulty;
        this.characterIcon       = characterIcon;
        this.attributes          = Collections.unmodifiableMap(new HashMap<>(attributes != null ? attributes : new HashMap<>()));
        this.gameDate            = gameDate;
        this.randSeed            = randSeed;
        this.money               = money;
        this.gameDateTime        = gameDateTime;
        this.currentStamina      = currentStamina;
        this.charCellX           = charCellX;
        this.charCellY           = charCellY;
        this.homeCellX           = homeCellX;
        this.homeCellY           = homeCellY;
        this.buildingDiscovered    = buildingDiscovered.clone();
        this.buildingOwned         = buildingOwned.clone();
        this.improvementDiscovered = improvementDiscovered.clone();
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Creates a {@link GameSave} snapshot from the current game state.
     *
     * @param profile the active profile
     * @param map     the current city map
     * @param charX   character X position (cell coordinate)
     * @param charY   character Y position (cell coordinate)
     * @param homeX   home cell X coordinate
     * @param homeY   home cell Y coordinate
     * @return a new {@link GameSave} capturing the current state
     */
    public static GameSave from(Profile profile, CityMap map,
                                int charX, int charY,
                                int homeX, int homeY) {
        int size = CityMap.MAP_SIZE;
        boolean[] bDisc  = new boolean[size * size];
        boolean[] bOwned = new boolean[size * size];
        boolean[] iDisc  = new boolean[size * size * 4];

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int idx  = x * size + y;
                Cell cell = map.getCell(x, y);
                if (cell.hasBuilding()) {
                    Building b = cell.getBuilding();
                    bDisc[idx]  = b.isDiscovered();
                    bOwned[idx] = b.isOwned();
                    List<Improvement> imps = b.getImprovements();
                    for (int i = 0; i < imps.size() && i < 4; i++) {
                        iDisc[idx * 4 + i] = imps.get(i).isDiscovered();
                    }
                }
            }
        }

        return new GameSave(
                profile.getCharacterName(),
                profile.getGender(),
                profile.getDifficulty(),
                profile.getCharacterIcon(),
                profile.getAttributes(),
                profile.getGameDate(),
                profile.getRandSeed(),
                profile.getMoney(),
                profile.getGameDateTime(),
                profile.getCurrentStamina(),
                charX, charY,
                homeX, homeY,
                bDisc, bOwned, iDisc);
    }

    // -------------------------------------------------------------------------
    // Apply
    // -------------------------------------------------------------------------

    /**
     * Applies the saved profile state to {@code profile}.
     * Only mutable fields (money, stamina, game date/time, attributes) are updated;
     * identity fields (name, gender, difficulty, icon, seed) are left for the caller
     * to verify before calling this method.
     *
     * @param profile the profile to update
     */
    public void applyToProfile(Profile profile) {
        profile.setMoney(money);
        profile.setGameDate(gameDate);
        profile.setGameDateTime(gameDateTime);
        profile.setAttributes(new HashMap<>(attributes));
        profile.setCurrentStamina(currentStamina);
    }

    /**
     * Applies the saved map discovery / ownership state to {@code map}.
     * Only buildings that were discovered/owned in the save are flagged; the rest
     * remain in their default (undiscovered) state.
     *
     * @param map the city map to update
     */
    public void applyToMap(CityMap map) {
        int size = CityMap.MAP_SIZE;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int idx  = x * size + y;
                Cell cell = map.getCell(x, y);
                if (cell.hasBuilding()) {
                    Building b = cell.getBuilding();
                    if (buildingDiscovered[idx])  b.discover();
                    b.setOwned(buildingOwned[idx]);
                    List<Improvement> imps = b.getImprovements();
                    for (int i = 0; i < imps.size() && i < 4; i++) {
                        if (improvementDiscovered[idx * 4 + i]) imps.get(i).discover();
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getCharacterName()  { return characterName; }
    public String getGender()         { return gender; }
    public String getDifficulty()     { return difficulty; }
    public String getCharacterIcon()  { return characterIcon; }
    public Map<String, Integer> getAttributes() { return attributes; }
    public int    getGameDate()       { return gameDate; }
    public long   getRandSeed()       { return randSeed; }
    public int    getMoney()          { return money; }
    public String getGameDateTime()   { return gameDateTime; }
    public int    getCurrentStamina() { return currentStamina; }
    public int    getCharCellX()      { return charCellX; }
    public int    getCharCellY()      { return charCellY; }
    public int    getHomeCellX()      { return homeCellX; }
    public int    getHomeCellY()      { return homeCellY; }

    /** Returns a copy of the flat building-discovered array (index = {@code x*MAP_SIZE+y}). */
    public boolean[] getBuildingDiscovered()    { return buildingDiscovered.clone(); }

    /** Returns a copy of the flat building-owned array (index = {@code x*MAP_SIZE+y}). */
    public boolean[] getBuildingOwned()         { return buildingOwned.clone(); }

    /** Returns a copy of the flat improvement-discovered array
     *  (index = {@code (x*MAP_SIZE+y)*4+impIndex}). */
    public boolean[] getImprovementDiscovered() { return improvementDiscovered.clone(); }
}

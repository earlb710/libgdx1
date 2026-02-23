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
    // Equipment snapshot
    // Stored as item names so the save is independent of EquipItem object identity.
    // Non-utility slot → item name (null = empty).
    // Utility items → ordered list of names.
    // -------------------------------------------------------------------------
    /** Non-utility slot → item name; null value means the slot is empty. */
    private final Map<EquipmentSlot, String> equipmentNames;
    /** Ordered names of utility items carried. */
    private final List<String> utilityItemNames;

    // -------------------------------------------------------------------------
    // Constructor (package-private; use GameSave.from() or SaveGameManager)
    // -------------------------------------------------------------------------
    GameSave(String characterName, String gender, String difficulty, String characterIcon,
             Map<String, Integer> attributes,
             int gameDate, long randSeed, int money, String gameDateTime, int currentStamina,
             int charCellX, int charCellY, int homeCellX, int homeCellY,
             boolean[] buildingDiscovered, boolean[] buildingOwned,
             boolean[] improvementDiscovered,
             Map<EquipmentSlot, String> equipmentNames,
             List<String> utilityItemNames) {
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
        this.equipmentNames  = Collections.unmodifiableMap(
                equipmentNames != null ? new java.util.EnumMap<>(equipmentNames) : new java.util.EnumMap<>(EquipmentSlot.class));
        this.utilityItemNames = Collections.unmodifiableList(
                utilityItemNames != null ? new java.util.ArrayList<>(utilityItemNames) : new java.util.ArrayList<>());
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
                bDisc, bOwned, iDisc,
                snapshotEquipment(profile),
                snapshotUtility(profile));
    }

    /** Snapshot non-utility slots as slot→name map. */
    private static java.util.EnumMap<EquipmentSlot, String> snapshotEquipment(Profile profile) {
        java.util.EnumMap<EquipmentSlot, String> map = new java.util.EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot == EquipmentSlot.UTILITY) continue;
            EquipItem item = profile.getEquipped(slot);
            if (item != null) map.put(slot, item.getName());
        }
        return map;
    }

    /** Snapshot utility items as list of names. */
    private static java.util.ArrayList<String> snapshotUtility(Profile profile) {
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        for (EquipItem item : profile.getUtilityItems()) {
            names.add(item.getName());
        }
        return names;
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
        // Restore non-utility equipment from catalogue by name
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot == EquipmentSlot.UTILITY) continue;
            String name = equipmentNames.get(slot);
            if (name == null) {
                profile.unequip(slot);
            } else {
                EquipItem item = EquipItem.findByName(name, slot);
                if (item != null) profile.equip(item);
            }
        }
        // Restore utility items
        for (String name : utilityItemNames) {
            EquipItem item = EquipItem.findByName(name, EquipmentSlot.UTILITY);
            if (item != null) profile.addUtilityItem(item);
        }
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

    /** Returns the non-utility equipment names (slot→name; null value = empty slot). */
    public Map<EquipmentSlot, String> getEquipmentNames() {
        return equipmentNames; // already unmodifiable
    }

    /** Returns the ordered list of utility item names. */
    public List<String> getUtilityItemNames() {
        return utilityItemNames; // already unmodifiable
    }
}

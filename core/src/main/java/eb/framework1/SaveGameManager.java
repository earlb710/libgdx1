package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Persists and restores {@link GameSave} snapshots using libGDX {@link Preferences}.
 *
 * <p>Each save is stored in a dedicated preferences file named
 * {@code "framework1.save.<characterName>"} (lower-cased), keyed by {@value #KEY_SAVE}.
 * The complete game state is serialised as a single JSON string so that a save is
 * atomic – either the whole state is written or nothing is.
 *
 * <h3>Typical usage</h3>
 * <pre>
 *   // Saving
 *   saveGameManager.saveGame(profile, cityMap, charX, charY, homeX, homeY);
 *
 *   // Loading
 *   if (saveGameManager.hasSave(profile.getCharacterName())) {
 *       GameSave save = saveGameManager.loadGame(profile.getCharacterName());
 *       if (save != null) {
 *           save.applyToProfile(profile);
 *           save.applyToMap(cityMap);
 *       }
 *   }
 * </pre>
 */
public class SaveGameManager {

    static final String PREFS_PREFIX = "framework1.save.";
    static final String KEY_SAVE     = "save";

    private final Json json;

    public SaveGameManager() {
        json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Persists the current game state for {@code profile}.
     *
     * @param profile the active profile
     * @param map     the current city map
     * @param charX   character X cell coordinate
     * @param charY   character Y cell coordinate
     * @param homeX   home cell X coordinate
     * @param homeY   home cell Y coordinate
     */
    public void saveGame(Profile profile, CityMap map,
                         int charX, int charY,
                         int homeX, int homeY) {
        GameSave save    = GameSave.from(profile, map, charX, charY, homeX, homeY);
        SaveData data    = toData(save);
        String   jsonStr = json.toJson(data);
        Preferences prefs = prefs(profile.getCharacterName());
        prefs.putString(KEY_SAVE, jsonStr);
        prefs.flush();
        Gdx.app.log("SaveGameManager", "Game saved for '" + profile.getCharacterName() + "'");
    }

    /**
     * Loads the most recent save for the given character name.
     *
     * @param characterName the character name used as the save identifier
     * @return the {@link GameSave}, or {@code null} if no save exists or loading fails
     */
    public GameSave loadGame(String characterName) {
        Preferences prefs   = prefs(characterName);
        String      jsonStr = prefs.getString(KEY_SAVE, null);
        if (jsonStr == null || jsonStr.isEmpty()) {
            return null;
        }
        try {
            SaveData data = json.fromJson(SaveData.class, jsonStr);
            GameSave save = fromData(data);
            Gdx.app.log("SaveGameManager", "Game loaded for '" + characterName + "'");
            return save;
        } catch (Exception e) {
            Gdx.app.error("SaveGameManager",
                    "Failed to load save for '" + characterName + "': " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Returns {@code true} if a save exists for the given character name.
     *
     * @param characterName the character name
     * @return {@code true} if a save file exists
     */
    public boolean hasSave(String characterName) {
        return prefs(characterName).contains(KEY_SAVE);
    }

    /**
     * Deletes the save associated with the given character name.
     *
     * @param characterName the character name
     */
    public void deleteSave(String characterName) {
        Preferences prefs = prefs(characterName);
        prefs.remove(KEY_SAVE);
        prefs.flush();
        Gdx.app.log("SaveGameManager", "Save deleted for '" + characterName + "'");
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Preferences prefs(String characterName) {
        return Gdx.app.getPreferences(PREFS_PREFIX + characterName.toLowerCase());
    }

    /** Converts a {@link GameSave} to the flat, JSON-serialisable {@link SaveData}. */
    static SaveData toData(GameSave save) {
        SaveData d = new SaveData();
        d.characterName       = save.getCharacterName();
        d.gender              = save.getGender();
        d.difficulty          = save.getDifficulty();
        d.characterIcon       = save.getCharacterIcon();
        d.attributes          = new HashMap<>(save.getAttributes());
        d.gameDate            = save.getGameDate();
        d.randSeed            = save.getRandSeed();
        d.money               = save.getMoney();
        d.gameDateTime        = save.getGameDateTime();
        d.currentStamina      = save.getCurrentStamina();
        d.charCellX           = save.getCharCellX();
        d.charCellY           = save.getCharCellY();
        d.homeCellX           = save.getHomeCellX();
        d.homeCellY           = save.getHomeCellY();
        d.buildingDiscovered    = save.getBuildingDiscovered();
        d.buildingOwned         = save.getBuildingOwned();
        d.improvementDiscovered = save.getImprovementDiscovered();
        // Equipment — store as name strings, keyed by slot name
        d.equipmentSlotNames = new HashMap<>();
        for (Map.Entry<EquipmentSlot, String> e : save.getEquipmentNames().entrySet()) {
            d.equipmentSlotNames.put(e.getKey().name(), e.getValue());
        }
        d.utilityItemNames = new ArrayList<>(save.getUtilityItemNames());
        return d;
    }

    /** Converts a {@link SaveData} to an immutable {@link GameSave}. */
    static GameSave fromData(SaveData d) {
        int size = CityMap.MAP_SIZE;
        int cells = size * size;
        boolean[] bDisc  = d.buildingDiscovered    != null ? d.buildingDiscovered    : new boolean[cells];
        boolean[] bOwned = d.buildingOwned         != null ? d.buildingOwned         : new boolean[cells];
        boolean[] iDisc  = d.improvementDiscovered != null ? d.improvementDiscovered : new boolean[cells * 4];
        Map<String, Integer> attrs = d.attributes != null ? d.attributes : new HashMap<>();
        // Rebuild equipment maps
        java.util.EnumMap<EquipmentSlot, String> equipMap = new java.util.EnumMap<>(EquipmentSlot.class);
        if (d.equipmentSlotNames != null) {
            for (Map.Entry<String, String> e : d.equipmentSlotNames.entrySet()) {
                try {
                    equipMap.put(EquipmentSlot.valueOf(e.getKey()), e.getValue());
                } catch (IllegalArgumentException ignored) { /* unknown slot */ }
            }
        }
        java.util.List<String> utilNames = d.utilityItemNames != null
                ? d.utilityItemNames : new ArrayList<>();
        return new GameSave(
                d.characterName,
                d.gender,
                d.difficulty,
                d.characterIcon,
                attrs,
                d.gameDate,
                d.randSeed,
                d.money,
                d.gameDateTime != null ? d.gameDateTime : "2050-01-02 16:00",
                d.currentStamina,
                d.charCellX, d.charCellY,
                d.homeCellX, d.homeCellY,
                bDisc, bOwned, iDisc,
                equipMap, utilNames);
    }

    // -------------------------------------------------------------------------
    // Flat JSON-serialisable data transfer object
    // -------------------------------------------------------------------------

    /**
     * Package-private flat struct used for JSON serialisation.
     * All fields are public so that libGDX {@link Json} can read/write them automatically.
     */
    static class SaveData {
        public String  characterName;
        public String  gender;
        public String  difficulty;
        public String  characterIcon;
        public Map<String, Integer> attributes;
        public int     gameDate;
        public long    randSeed;
        public int     money;
        public String  gameDateTime;
        public int     currentStamina;
        public int     charCellX;
        public int     charCellY;
        public int     homeCellX;
        public int     homeCellY;
        /** Flat array, index = {@code x * MAP_SIZE + y}. */
        public boolean[] buildingDiscovered;
        /** Flat array, index = {@code x * MAP_SIZE + y}. */
        public boolean[] buildingOwned;
        /** Flat array, index = {@code (x * MAP_SIZE + y) * 4 + impIndex}. */
        public boolean[] improvementDiscovered;
        /** Non-utility slot name → item name (null = empty). */
        public Map<String, String> equipmentSlotNames;
        /** Ordered utility item names. */
        public java.util.List<String> utilityItemNames;
    }
}

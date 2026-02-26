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
        // Case files (including story trees)
        d.caseFiles = new ArrayList<>();
        for (CaseFile cf : save.getCaseFiles()) {
            d.caseFiles.add(toCaseFileData(cf));
        }
        d.activeCaseId = save.getActiveCaseId();
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
        // Restore case files (including story trees)
        java.util.List<CaseFile> caseFiles = new ArrayList<>();
        if (d.caseFiles != null) {
            for (CaseFileData cfd : d.caseFiles) {
                if (cfd != null) caseFiles.add(fromCaseFileData(cfd));
            }
        }
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
                equipMap, utilNames,
                caseFiles, d.activeCaseId);
    }

    // -------------------------------------------------------------------------
    // Case-file conversion helpers
    // -------------------------------------------------------------------------

    private static CaseFileData toCaseFileData(CaseFile cf) {
        CaseFileData d = new CaseFileData();
        d.id          = cf.getId();
        d.name        = cf.getName();
        d.description = cf.getDescription();
        d.status      = cf.getStatus().name();
        d.dateOpened  = cf.getDateOpened();
        d.dateClosed  = cf.getDateClosed();
        d.clues       = new ArrayList<>(cf.getClues());
        d.evidence    = new ArrayList<>(cf.getEvidence());
        d.notes       = new ArrayList<>(cf.getNotes());
        d.evidenceItems = new ArrayList<>();
        for (EvidenceItem item : cf.getEvidenceItems()) {
            d.evidenceItems.add(toEvidenceItemData(item));
        }
        d.caseType    = cf.getCaseType() != null ? cf.getCaseType().name() : null;
        d.clientName  = cf.getClientName();
        d.subjectName = cf.getSubjectName();
        d.objective   = cf.getObjective();
        d.leads = new ArrayList<>();
        for (CaseLead lead : cf.getLeads()) {
            d.leads.add(toCaseLeadData(lead));
        }
        d.complexity  = cf.getComplexity();
        d.storyRoot   = cf.getStoryRoot() != null ? toStoryNodeData(cf.getStoryRoot()) : null;
        return d;
    }

    private static CaseFile fromCaseFileData(CaseFileData d) {
        CaseFile.Status status;
        try { status = CaseFile.Status.valueOf(d.status); }
        catch (Exception e) { status = CaseFile.Status.OPEN; }

        java.util.List<EvidenceItem> items = new ArrayList<>();
        if (d.evidenceItems != null) {
            for (EvidenceItemData eid : d.evidenceItems) {
                if (eid != null) {
                    EvidenceItem item = fromEvidenceItemData(eid);
                    if (item != null) items.add(item);
                }
            }
        }

        // Full constructor: id, name, description, status, dateOpened, dateClosed,
        //                   clues, evidence, notes, evidenceItems  (matches CaseFile's 10-arg ctor)
        CaseFile cf = new CaseFile(
                d.id, d.name, d.description, status,
                d.dateOpened, d.dateClosed,
                d.clues, d.evidence, d.notes, items);

        if (d.caseType != null) {
            try { cf.setCaseType(CaseType.valueOf(d.caseType)); }
            catch (IllegalArgumentException ignored) { /* unknown type */ }
        }
        cf.setClientName(d.clientName);
        cf.setSubjectName(d.subjectName);
        cf.setObjective(d.objective);
        if (d.complexity >= 1 && d.complexity <= 3) cf.setComplexity(d.complexity);
        if (d.leads != null) {
            for (CaseLeadData ld : d.leads) {
                if (ld != null) {
                    CaseLead lead = fromCaseLeadData(ld);
                    if (lead != null) cf.addLead(lead);
                }
            }
        }
        if (d.storyRoot != null) {
            cf.setStoryRoot(fromStoryNodeData(d.storyRoot));
        }
        return cf;
    }

    private static EvidenceItemData toEvidenceItemData(EvidenceItem item) {
        EvidenceItemData d = new EvidenceItemData();
        d.name = item.getName();
        d.possibleModifiers = new ArrayList<>();
        for (EvidenceModifier m : item.getPossibleModifiers()) {
            d.possibleModifiers.add(m.name());
        }
        d.submittedModifiers = new ArrayList<>();
        for (EvidenceModifier m : item.getSubmittedModifiers()) {
            d.submittedModifiers.add(m.name());
        }
        return d;
    }

    private static EvidenceItem fromEvidenceItemData(EvidenceItemData d) {
        if (d == null || d.name == null) return null;
        EvidenceItem.Builder b = new EvidenceItem.Builder(d.name);
        if (d.possibleModifiers != null) {
            for (String mn : d.possibleModifiers) {
                try { b.possibleModifier(EvidenceModifier.valueOf(mn)); }
                catch (IllegalArgumentException ignored) { /* unknown modifier */ }
            }
        }
        EvidenceItem item = b.build();
        if (d.submittedModifiers != null) {
            for (String mn : d.submittedModifiers) {
                try {
                    EvidenceModifier mod = EvidenceModifier.valueOf(mn);
                    if (item.getPossibleModifiers().contains(mod)) {
                        item.submitForAnalysis(mod);
                    }
                } catch (IllegalArgumentException | IllegalStateException ignored) { /* skip */ }
            }
        }
        return item;
    }

    private static CaseLeadData toCaseLeadData(CaseLead lead) {
        CaseLeadData d = new CaseLeadData();
        d.id              = lead.getId();
        d.description     = lead.getDescription();
        d.hint            = lead.getHint();
        d.discoveryMethod = lead.getDiscoveryMethod().name();
        d.discovered      = lead.isDiscovered();
        return d;
    }

    private static CaseLead fromCaseLeadData(CaseLeadData d) {
        if (d == null || d.id == null || d.hint == null) return null;
        DiscoveryMethod method;
        try { method = DiscoveryMethod.valueOf(d.discoveryMethod); }
        catch (Exception e) { method = DiscoveryMethod.INTERVIEW; }
        CaseLead lead = new CaseLead(d.id, d.description, d.hint, method);
        if (d.discovered) lead.discover();
        return lead;
    }

    /** Recursively converts a {@link CaseStoryNode} to its serialisable DTO. */
    private static StoryNodeData toStoryNodeData(CaseStoryNode node) {
        if (node == null) return null;
        StoryNodeData d = new StoryNodeData();
        d.id          = node.getId();
        d.title       = node.getTitle();
        d.description = node.getDescription();
        d.nodeType    = node.getNodeType().name();
        d.completed   = node.isCompleted();
        d.children    = new ArrayList<>();
        for (CaseStoryNode child : node.getChildren()) {
            d.children.add(toStoryNodeData(child));
        }
        return d;
    }

    /** Recursively converts a {@link StoryNodeData} DTO back to a {@link CaseStoryNode}. */
    private static CaseStoryNode fromStoryNodeData(StoryNodeData d) {
        if (d == null || d.id == null || d.title == null) return null;
        CaseStoryNode.NodeType type;
        try { type = CaseStoryNode.NodeType.valueOf(d.nodeType); }
        catch (Exception e) { type = CaseStoryNode.NodeType.ACTION; }
        CaseStoryNode node = new CaseStoryNode(d.id, d.title, d.description, type);
        if (d.completed) node.complete();
        if (d.children != null) {
            for (StoryNodeData child : d.children) {
                CaseStoryNode childNode = fromStoryNodeData(child);
                if (childNode != null) node.addChild(childNode);
            }
        }
        return node;
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
        /** Serialised case files (including story trees). */
        public java.util.List<CaseFileData> caseFiles;
        /** ID of the active case file, or null. */
        public String activeCaseId;
    }

    // -------------------------------------------------------------------------
    // Case-file data transfer objects
    // -------------------------------------------------------------------------

    /** Serialisable DTO for a single story-tree node (recursive). */
    static class StoryNodeData {
        public String id;
        public String title;
        public String description;
        /** {@link CaseStoryNode.NodeType#name()} value. */
        public String nodeType;
        public boolean completed;
        public java.util.List<StoryNodeData> children;
    }

    /** Serialisable DTO for a {@link CaseLead}. */
    static class CaseLeadData {
        public String id;
        public String description;
        public String hint;
        /** {@link DiscoveryMethod#name()} value. */
        public String discoveryMethod;
        public boolean discovered;
    }

    /** Serialisable DTO for an {@link EvidenceItem}. */
    static class EvidenceItemData {
        public String name;
        /** {@link EvidenceModifier#name()} values of applicable analyses. */
        public java.util.List<String> possibleModifiers;
        /** {@link EvidenceModifier#name()} values of submitted analyses. */
        public java.util.List<String> submittedModifiers;
    }

    /** Serialisable DTO for a {@link CaseFile} (including story tree). */
    static class CaseFileData {
        public String id;
        public String name;
        public String description;
        /** {@link CaseFile.Status#name()} value. */
        public String status;
        public String dateOpened;
        public String dateClosed;
        public java.util.List<String> clues;
        public java.util.List<String> evidence;
        public java.util.List<String> notes;
        public java.util.List<EvidenceItemData> evidenceItems;
        /** {@link CaseType#name()} value, or null for legacy/manual cases. */
        public String caseType;
        public String clientName;
        public String subjectName;
        public String objective;
        public java.util.List<CaseLeadData> leads;
        public int complexity;
        /** Root of the story-progression tree; null for legacy/manual cases. */
        public StoryNodeData storyRoot;
    }
}

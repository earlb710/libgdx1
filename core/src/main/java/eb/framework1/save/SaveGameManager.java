package eb.framework1.save;

import eb.framework1.character.*;
import eb.framework1.city.*;
import eb.framework1.face.FaceConfig;
import eb.framework1.investigation.*;


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

    public static final String PREFS_PREFIX = "framework1.save.";
    public static final String KEY_SAVE     = "save";

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
    public static SaveData toData(GameSave save) {
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
        d.worldNpcs = new ArrayList<>();
        for (NpcCharacter npc : save.getWorldNpcs()) {
            d.worldNpcs.add(toNpcData(npc));
        }
        d.visionTrait = save.getVisionTrait().name();
        return d;
    }

    /** Converts a {@link SaveData} to an immutable {@link GameSave}. */
    public static GameSave fromData(SaveData d) {
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
        // Restore world NPCs
        java.util.List<NpcCharacter> worldNpcs = new ArrayList<>();
        if (d.worldNpcs != null) {
            for (NpcCharacterData nd : d.worldNpcs) {
                NpcCharacter npc = fromNpcData(nd);
                if (npc != null) worldNpcs.add(npc);
            }
        }
        // Restore vision trait (null in older saves → NONE)
        VisionTrait savedVisionTrait = VisionTrait.NONE;
        if (d.visionTrait != null) {
            try { savedVisionTrait = VisionTrait.valueOf(d.visionTrait); }
            catch (IllegalArgumentException ignored) { /* use default NONE */ }
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
                caseFiles, d.activeCaseId,
                worldNpcs,
                savedVisionTrait);
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
    public static class SaveData {
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
        /** Serialised world-population NPCs. */
        public java.util.List<NpcCharacterData> worldNpcs;
        /** {@link VisionTrait#name()} value; null in older saves → NONE. */
        public String visionTrait;
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

    // -------------------------------------------------------------------------
    // NPC data transfer objects
    // -------------------------------------------------------------------------

    /** Serialisable DTO for a single {@link NpcScheduleEntry}. */
    static class NpcScheduleEntryData {
        public int    startHour;
        public int    endHour;
        public String activityType;
        public String locationName;
        public int    cellX;
        public int    cellY;
    }

    /** Serialisable DTO for a single {@link EquipItem} carried by an NPC. */
    static class NpcItemData {
        /** {@link EquipItem#getName()} value. */
        public String name;
        /** {@link EquipmentSlot#name()} value. */
        public String slot;
    }

    /** Serialisable DTO for an {@link NpcCharacter}. */
    static class NpcCharacterData {
        public String id;
        public String fullName;
        public String gender;
        public int    age;
        public String occupation;
        public String spriteKey;
        public String physicalDescription;
        public String homeAddress;
        public String workplaceAddress;
        public java.util.List<String> frequentLocations;
        public String phoneNumber;
        public String email;
        public int    cooperativeness;
        public int    honesty;
        public int    nervousness;
        /** {@link PersonalityProfile#name()} value. */
        public String personalityProfile;
        /** {@link CharacterAttribute#name()} → value. */
        public Map<String, Integer> attributes;
        /** {@link NpcSkill#name()} values. */
        public java.util.List<String> skills;
        public java.util.List<NpcScheduleEntryData> scheduleEntries;
        public String birthdate;
        public boolean tracked;
        // Appearance attributes (added later; null/0 in older saves → use defaults)
        public String hairType;
        public String hairColor;
        public int    wealthyLevel;
        public String favColor;
        // Body measurements (added later; 0 in older saves → not set)
        public int    heightCm;
        public int    weightKg;
        // Carried items (added later; null in older saves → empty list)
        public java.util.List<NpcItemData> carriedItems;
        // Vision trait (added later; null in older saves → NONE)
        public String visionTrait;
        // Face configuration (added later; null in older saves → no face)
        public FaceConfigData faceConfig;
    }

    /**
     * Lightweight DTO that mirrors {@link FaceConfig} for persistence.
     * Each field corresponds to a feature's key properties.
     * Null DTO means no face was generated.
     */
    static class FaceConfigData {
        public double fatness;
        public String[] teamColors;
        // body
        public String bodyId;
        public String bodyColor;
        public double bodySize;
        // head
        public String headId;
        public String headShave;
        // hair
        public String hairId;
        public String hairColor;
        public boolean hairFlip;
        // hairBg
        public String hairBgId;
        // eye
        public String eyeId;
        public int eyeAngle;
        // eyebrow
        public String eyebrowId;
        public int eyebrowAngle;
        // ear
        public String earId;
        public double earSize;
        // nose
        public String noseId;
        public boolean noseFlip;
        public double noseSize;
        // mouth
        public String mouthId;
        public boolean mouthFlip;
        // eyeLine
        public String eyeLineId;
        // smileLine
        public String smileLineId;
        public double smileLineSize;
        // miscLine
        public String miscLineId;
        // facialHair
        public String facialHairId;
        // glasses
        public String glassesId;
        // accessories
        public String accessoriesId;
        // jersey
        public String jerseyId;
    }

    private static NpcCharacterData toNpcData(NpcCharacter npc) {
        NpcCharacterData d = new NpcCharacterData();
        d.id                  = npc.getId();
        d.fullName            = npc.getFullName();
        d.gender              = npc.getGender();
        d.age                 = npc.getAge();
        d.occupation          = npc.getOccupation();
        d.spriteKey           = npc.getSpriteKey();
        d.physicalDescription = npc.getPhysicalDescription();
        d.homeAddress         = npc.getHomeAddress();
        d.workplaceAddress    = npc.getWorkplaceAddress();
        d.frequentLocations   = new ArrayList<>(npc.getFrequentLocations());
        d.phoneNumber         = npc.getPhoneNumber();
        d.email               = npc.getEmail();
        d.cooperativeness     = npc.getCooperativeness();
        d.honesty             = npc.getHonesty();
        d.nervousness         = npc.getNervousness();
        d.personalityProfile  = npc.getPersonalityProfile().name();
        d.attributes          = new HashMap<>();
        for (Map.Entry<CharacterAttribute, Integer> e : npc.getAttributes().entrySet()) {
            d.attributes.put(e.getKey().name(), e.getValue());
        }
        d.skills = new ArrayList<>();
        for (NpcSkill s : npc.getSkills()) {
            d.skills.add(s.name());
        }
        d.scheduleEntries = new ArrayList<>();
        NpcSchedule schedule = npc.getSchedule();
        if (schedule != null) {
            for (NpcScheduleEntry entry : schedule.getEntries()) {
                NpcScheduleEntryData ed = new NpcScheduleEntryData();
                ed.startHour    = entry.startHour;
                ed.endHour      = entry.endHour;
                ed.activityType = entry.activityType;
                ed.locationName = entry.locationName;
                ed.cellX        = entry.cellX;
                ed.cellY        = entry.cellY;
                d.scheduleEntries.add(ed);
            }
        }
        d.birthdate     = npc.getBirthdate();
        d.tracked       = npc.isTracked();
        d.hairType      = npc.getHairType();
        d.hairColor     = npc.getHairColor();
        d.wealthyLevel  = npc.getWealthyLevel();
        d.favColor      = npc.getFavColor();
        d.heightCm      = npc.getHeightCm();
        d.weightKg      = npc.getWeightKg();
        d.carriedItems  = new ArrayList<>();
        for (EquipItem item : npc.getCarriedItems()) {
            NpcItemData itemData = new NpcItemData();
            itemData.name = item.getName();
            itemData.slot = item.getSlot().name();
            d.carriedItems.add(itemData);
        }
        d.visionTrait   = npc.getVisionTrait().name();
        if (npc.getFaceConfig() != null) {
            d.faceConfig = toFaceConfigData(npc.getFaceConfig());
        }
        return d;
    }

    private static FaceConfigData toFaceConfigData(FaceConfig f) {
        FaceConfigData fd = new FaceConfigData();
        fd.fatness       = f.fatness;
        fd.teamColors    = f.teamColors.clone();
        fd.bodyId        = f.body.id;
        fd.bodyColor     = f.body.color;
        fd.bodySize      = f.body.size;
        fd.headId        = f.head.id;
        fd.headShave     = f.head.shave;
        fd.hairId        = f.hair.id;
        fd.hairColor     = f.hair.color;
        fd.hairFlip      = f.hair.flip;
        fd.hairBgId      = f.hairBg.id;
        fd.eyeId         = f.eye.id;
        fd.eyeAngle      = f.eye.angle;
        fd.eyebrowId     = f.eyebrow.id;
        fd.eyebrowAngle  = f.eyebrow.angle;
        fd.earId         = f.ear.id;
        fd.earSize       = f.ear.size;
        fd.noseId        = f.nose.id;
        fd.noseFlip      = f.nose.flip;
        fd.noseSize      = f.nose.size;
        fd.mouthId       = f.mouth.id;
        fd.mouthFlip     = f.mouth.flip;
        fd.eyeLineId     = f.eyeLine.id;
        fd.smileLineId   = f.smileLine.id;
        fd.smileLineSize = f.smileLine.size;
        fd.miscLineId    = f.miscLine.id;
        fd.facialHairId  = f.facialHair.id;
        fd.glassesId     = f.glasses.id;
        fd.accessoriesId = f.accessories.id;
        fd.jerseyId      = f.jersey.id;
        return fd;
    }

    private static FaceConfig fromFaceConfigData(FaceConfigData fd) {
        if (fd == null) return null;
        return new FaceConfig.Builder()
            .fatness(fd.fatness)
            .teamColors(fd.teamColors != null ? fd.teamColors
                      : new String[]{"#89bfd3","#7a1319","#07364f"})
            .body(new FaceConfig.BodyFeature(fd.bodyId, fd.bodyColor, fd.bodySize))
            .head(new FaceConfig.HeadFeature(fd.headId, fd.headShave))
            .hair(new FaceConfig.HairFeature(fd.hairId, fd.hairColor, fd.hairFlip))
            .hairBg(new FaceConfig.SimpleFeature(fd.hairBgId))
            .eye(new FaceConfig.EyeFeature(fd.eyeId, fd.eyeAngle))
            .eyebrow(new FaceConfig.EyebrowFeature(fd.eyebrowId, fd.eyebrowAngle))
            .ear(new FaceConfig.EarFeature(fd.earId, fd.earSize))
            .nose(new FaceConfig.NoseFeature(fd.noseId, fd.noseFlip, fd.noseSize))
            .mouth(new FaceConfig.MouthFeature(fd.mouthId, fd.mouthFlip))
            .eyeLine(new FaceConfig.SimpleFeature(fd.eyeLineId))
            .smileLine(new FaceConfig.SmileLineFeature(fd.smileLineId, fd.smileLineSize))
            .miscLine(new FaceConfig.SimpleFeature(fd.miscLineId))
            .facialHair(new FaceConfig.SimpleFeature(fd.facialHairId))
            .glasses(new FaceConfig.SimpleFeature(fd.glassesId))
            .accessories(new FaceConfig.SimpleFeature(fd.accessoriesId))
            .jersey(new FaceConfig.SimpleFeature(fd.jerseyId))
            .build();
    }

    /**
     * Clamps a personality trait value (cooperativeness, honesty, nervousness) to the
     * valid 1–10 range.  A stored value of 0 (e.g. from an older save before the field
     * existed) is treated as the neutral default of 5.
     */
    private static int clampTrait(int value) {
        return Math.max(1, Math.min(10, value == 0 ? 5 : value));
    }

    private static NpcCharacter fromNpcData(NpcCharacterData d) {
        if (d == null || d.id == null || d.fullName == null) return null;
        NpcCharacter.Builder b = new NpcCharacter.Builder()
                .id(d.id)
                .fullName(d.fullName)
                .gender(d.gender != null ? d.gender : "M")
                .age(d.age < 1 ? 30 : d.age)
                .occupation(d.occupation != null ? d.occupation : "")
                .spriteKey(d.spriteKey != null ? d.spriteKey : "")
                .physicalDescription(d.physicalDescription != null ? d.physicalDescription : "")
                .homeAddress(d.homeAddress != null ? d.homeAddress : "")
                .workplaceAddress(d.workplaceAddress != null ? d.workplaceAddress : "")
                .phoneNumber(d.phoneNumber != null ? d.phoneNumber : "")
                .email(d.email != null ? d.email : "")
                .cooperativeness(clampTrait(d.cooperativeness))
                .honesty(clampTrait(d.honesty))
                .nervousness(clampTrait(d.nervousness))
                .birthdate(d.birthdate)
                .tracked(d.tracked)
                .hairType(d.hairType != null ? d.hairType : "")
                .hairColor(d.hairColor != null ? d.hairColor : "")
                .wealthyLevel(d.wealthyLevel < 1 ? 5 : d.wealthyLevel)
                .favColor(d.favColor != null ? d.favColor : "")
                .heightCm(d.heightCm)
                .weightKg(d.weightKg);
        if (d.frequentLocations != null) {
            for (String loc : d.frequentLocations) {
                b.addFrequentLocation(loc);
            }
        }
        if (d.personalityProfile != null) {
            try { b.personalityProfile(PersonalityProfile.valueOf(d.personalityProfile)); }
            catch (IllegalArgumentException ignored) { /* use default */ }
        }
        if (d.attributes != null) {
            for (Map.Entry<String, Integer> e : d.attributes.entrySet()) {
                try {
                    b.attribute(CharacterAttribute.valueOf(e.getKey()), e.getValue());
                } catch (IllegalArgumentException ignored) { /* unknown attribute */ }
            }
        }
        if (d.skills != null) {
            for (String sName : d.skills) {
                try { b.addSkill(NpcSkill.valueOf(sName)); }
                catch (IllegalArgumentException ignored) { /* unknown skill */ }
            }
        }
        if (d.scheduleEntries != null && !d.scheduleEntries.isEmpty()) {
            java.util.List<NpcScheduleEntry> entries = new ArrayList<>();
            for (NpcScheduleEntryData ed : d.scheduleEntries) {
                if (ed != null) {
                    entries.add(new NpcScheduleEntry(
                            ed.startHour, ed.endHour,
                            ed.activityType, ed.locationName,
                            ed.cellX, ed.cellY));
                }
            }
            b.schedule(new NpcSchedule(entries));
        }
        if (d.carriedItems != null) {
            for (NpcItemData itemData : d.carriedItems) {
                if (itemData == null || itemData.name == null || itemData.slot == null) continue;
                try {
                    EquipmentSlot slot = EquipmentSlot.valueOf(itemData.slot);
                    EquipItem item = EquipItem.findByName(itemData.name, slot);
                    if (item != null) b.addCarriedItem(item);
                } catch (IllegalArgumentException ignored) { /* unknown slot or item */ }
            }
        }
        if (d.visionTrait != null) {
            try { b.visionTrait(VisionTrait.valueOf(d.visionTrait)); }
            catch (IllegalArgumentException ignored) { /* use default NONE */ }
        }
        if (d.faceConfig != null) {
            FaceConfig fc = fromFaceConfigData(d.faceConfig);
            if (fc != null) b.faceConfig(fc);
        }
        return b.build();
    }
}

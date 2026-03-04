package eb.framework1.character;

import eb.framework1.investigation.*;
import eb.framework1.phone.*;
import eb.framework1.schedule.*;


import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Profile {
    private String characterName;
    private String gender;
    private String difficulty;
    private String characterIcon; // Selected character icon (e.g., "man1", "man2", "woman1", "woman2")
    private Map<String, Integer> attributes;
    private int gameDate;  // Game date starting from 2050
    private long randSeed; // Random seed for procedural generation
    private int money;     // Player's current money
    private String gameDateTime; // Full in-game date/time string (e.g. "2050-01-02 13:20")
    private int currentStamina = -1; // -1 = lazy-initialise from STAMINA attribute on first access

    /**
     * Unique identifier for this character instance.
     * Allows multiple characters to coexist without name-based collisions.
     */
    private final String characterId;

    // Equipment: one item per non-utility slot (null = empty)
    private final Map<EquipmentSlot, EquipItem> equipment;
    // Utility slot allows multiple items
    private final List<EquipItem> utilityItems;
    // Stash: items stored at the player's home office (not carried)
    private final List<EquipItem> stash;
    // Calendar: accepted appointments / case starts
    private final List<CalendarEntry> calendarEntries;
    // Date (YYYY-MM-DD) when emails were last generated; "" = never
    private String lastEmailCheckDate = "";
    // Keys of contacts the player has already phoned, formatted as "caseId|contactName"
    private final java.util.Set<String> phonedContactKeys = new java.util.LinkedHashSet<>();
    // Ratings for phoned contacts, keyed by "caseId|contactName"
    private final Map<String, PhoneMessageRating> contactMessageRatings = new HashMap<>();
    
    public Profile(String characterName, String gender, String difficulty) {
        this(characterName, gender, difficulty, null, new HashMap<>());
    }
    
    public Profile(String characterName, String gender, String difficulty, String characterIcon) {
        this(characterName, gender, difficulty, characterIcon, new HashMap<>());
    }
    
    public Profile(String characterName, String gender, String difficulty, Map<String, Integer> attributes) {
        this(characterName, gender, difficulty, null, attributes, 2050, System.currentTimeMillis());
    }
    
    public Profile(String characterName, String gender, String difficulty, String characterIcon, Map<String, Integer> attributes) {
        this(characterName, gender, difficulty, characterIcon, attributes, 2050, System.currentTimeMillis());
    }
    
    public Profile(String characterName, String gender, String difficulty, Map<String, Integer> attributes, 
                   int gameDate, long randSeed) {
        this(characterName, gender, difficulty, null, attributes, gameDate, randSeed);
    }
    
    public Profile(String characterName, String gender, String difficulty, String characterIcon,
                   Map<String, Integer> attributes, int gameDate, long randSeed) {
        if (characterName == null || characterName.trim().isEmpty()) {
            throw new IllegalArgumentException("Character name cannot be null or empty");
        }
        if (gender == null || gender.trim().isEmpty()) {
            throw new IllegalArgumentException("Gender cannot be null or empty");
        }
        if (difficulty == null || difficulty.trim().isEmpty()) {
            throw new IllegalArgumentException("Difficulty cannot be null or empty");
        }
        
        this.characterName = characterName.trim();
        this.gender = gender.trim();
        this.difficulty = difficulty.trim();
        this.characterIcon = characterIcon;
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
        this.gameDate = gameDate;
        this.randSeed = randSeed;
        this.money = 1000;
        this.gameDateTime = "2050-01-02 16:00";
        this.characterId = UUID.randomUUID().toString();
        this.equipment   = new EnumMap<>(EquipmentSlot.class);
        this.utilityItems = new ArrayList<>();
        this.stash        = new ArrayList<>();
        this.calendarEntries = new ArrayList<>();
        // Default starting weapon
        equipment.put(EquipmentSlot.WEAPON, EquipItem.PISTOL);
    }
    
    // Character name serves as both the profile identifier and in-game name
    public String getName() {
        return characterName;
    }
    
    public String getCharacterName() {
        return characterName;
    }
    
    public String getGender() {
        return gender;
    }
    
    public String getDifficulty() {
        return difficulty;
    }
    
    public String getCharacterIcon() {
        return characterIcon;
    }
    
    public void setCharacterIcon(String characterIcon) {
        this.characterIcon = characterIcon;
    }
    
    public Map<String, Integer> getAttributes() {
        // Always return a non-null map for backwards compatibility
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return new HashMap<>(attributes);
    }
    
    public int getAttribute(String attributeName) {
        return attributes.getOrDefault(attributeName, 0);
    }
    
    public void setAttribute(String attributeName, int value) {
        attributes.put(attributeName, value);
    }
    
    public void setAttributes(Map<String, Integer> attributes) {
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
    }
    
    public int getGameDate() {
        return gameDate;
    }
    
    public void setGameDate(int gameDate) {
        this.gameDate = gameDate;
    }
    
    public long getRandSeed() {
        return randSeed;
    }
    
    public void setRandSeed(long randSeed) {
        this.randSeed = randSeed;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public String getGameDateTime() {
        return gameDateTime;
    }

    public void setGameDateTime(String gameDateTime) {
        this.gameDateTime = gameDateTime;
    }

    /** Maximum stamina pool = STAMINA attribute value × 10, minimum 10. */
    public int getMaxStamina() {
        return Math.max(10, getAttribute(CharacterAttribute.STAMINA.name()) * 10);
    }

    /** Current stamina. Lazy-initialised to {@link #getMaxStamina()} on first call. */
    public int getCurrentStamina() {
        if (currentStamina < 0) {
            currentStamina = getMaxStamina();
        }
        return currentStamina;
    }

    /** Deducts {@code amount} stamina points (floored at 0). */
    public void useStamina(int amount) {
        currentStamina = Math.max(0, getCurrentStamina() - amount);
    }

    public void setCurrentStamina(int stamina) {
        this.currentStamina = Math.max(0, stamina);
    }

    /** Adds {@code amount} stamina points, capped at {@link #getMaxStamina()}. */
    public void addStamina(int amount) {
        currentStamina = Math.min(getMaxStamina(), getCurrentStamina() + amount);
    }

    /**
     * Adds {@code amount} stamina points capped at {@code cap} instead of
     * {@link #getMaxStamina()}.  Use this when a location bonus allows the
     * effective maximum to exceed the base cap.
     *
     * @param amount amount to add (negative values are ignored)
     * @param cap    hard upper limit (e.g. base max + location modifier × 10)
     */
    public void addStaminaUpTo(int amount, int cap) {
        if (amount <= 0) return;
        currentStamina = Math.min(cap, getCurrentStamina() + amount);
    }

    /** Divisor converting body weight (kg) to base carry capacity (kg). */
    private static final float BODY_WEIGHT_CAPACITY_DIVISOR = 4f;

    /** Carry capacity gain (kg) per point of STRENGTH. */
    private static final float STRENGTH_CARRY_KG_PER_POINT = 2f;

    /** Kg of muscle or fat needed to shift STRENGTH by ±1 point. */
    private static final int MUSCLE_FAT_STRENGTH_DIVISOR = 10;

    /** Offset (cm) subtracted from height to derive lean base body weight for males. */
    private static final int BASE_WEIGHT_OFFSET_MALE   = 135;
    /** Offset (cm) subtracted from height to derive lean base body weight for females. */
    private static final int BASE_WEIGHT_OFFSET_FEMALE = 120;

    /** Default heights (cm) used as fallback when the HEIGHT_CM attribute is not stored. */
    private static final int DEFAULT_HEIGHT_MALE   = 175;
    private static final int DEFAULT_HEIGHT_FEMALE = 163;

    // -------------------------------------------------------------------------

    /**
     * Returns the character's lean base body weight (kg) derived from height and gender.
     * <p>Formula: {@code HEIGHT_CM - 135} for males, {@code HEIGHT_CM - 120} for females.
     * A 175 cm male therefore has a base of 40 kg; a 163 cm female has a base of 43 kg.
     */
    public int getBaseBodyWeight() {
        boolean isFemale = "female".equalsIgnoreCase(gender);
        int heightCm = getAttribute(CharacterAttribute.HEIGHT_CM.name());
        if (heightCm == 0) {
            heightCm = isFemale ? DEFAULT_HEIGHT_FEMALE : DEFAULT_HEIGHT_MALE;
        }
        int offset = isFemale ? BASE_WEIGHT_OFFSET_FEMALE : BASE_WEIGHT_OFFSET_MALE;
        return Math.max(1, heightCm - offset);
    }

    /**
     * Returns the character's total body weight in kg:
     * {@code base(height, gender) + MUSCLE_KG + FAT_KG}.
     */
    public int getTotalBodyWeightKg() {
        int muscleKg = getAttribute(CharacterAttribute.MUSCLE_KG.name());
        int fatKg    = getAttribute(CharacterAttribute.FAT_KG.name());
        return getBaseBodyWeight() + muscleKg + fatKg;
    }

    /**
     * Returns a STRENGTH modifier based on the character's body composition:
     * each {@value #MUSCLE_FAT_STRENGTH_DIVISOR} kg of muscle adds +1 STRENGTH;
     * each {@value #MUSCLE_FAT_STRENGTH_DIVISOR} kg of fat subtracts 1 STRENGTH.
     */
    public int getMuscleFatStrengthModifier() {
        int muscleKg = getAttribute(CharacterAttribute.MUSCLE_KG.name());
        int fatKg    = getAttribute(CharacterAttribute.FAT_KG.name());
        return muscleKg / MUSCLE_FAT_STRENGTH_DIVISOR - fatKg / MUSCLE_FAT_STRENGTH_DIVISOR;
    }

    /**
     * Returns the total weight (kg) of all currently equipped items
     * (non-utility slots + all utility items).
     */
    public float getTotalCarriedWeight() {
        float total = 0f;
        for (EquipItem item : equipment.values()) {
            total += item.getWeight();
        }
        for (EquipItem item : utilityItems) {
            total += item.getWeight();
        }
        return total;
    }

    /**
     * Returns the maximum weight (kg) this character can carry.
     * <p>Formula: {@code bodyWeight / 4 + STRENGTH * 2}, minimum 1.0, where
     * {@code bodyWeight = base(height, gender) + muscleKg + fatKg}.
     * <ul>
     *   <li>Base carry = total body weight / 4 (20 kg for an 80 kg person)</li>
     *   <li>+ 2 kg per point of STRENGTH attribute</li>
     * </ul>
     */
    public float getWeightCapacity() {
        int strength   = getAttribute(CharacterAttribute.STRENGTH.name());
        int bodyWeight = getTotalBodyWeightKg();
        return Math.max(1f, bodyWeight / BODY_WEIGHT_CAPACITY_DIVISOR
                + strength * STRENGTH_CARRY_KG_PER_POINT);
    }

    /**
     * Returns {@code true} if the total carried weight exceeds
     * {@link #getWeightCapacity()}.
     */
    public boolean isOverEncumbered() {
        return getTotalCarriedWeight() > getWeightCapacity();
    }

    // -------------------------------------------------------------------------
    // Detective level
    // -------------------------------------------------------------------------

    /**
     * The 11 investigative attributes used to compute the detective level.
     * Body measurements and the derived DETECTIVE_LEVEL itself are excluded.
     */
    private static final CharacterAttribute[] INVESTIGATIVE_ATTRS = {
        CharacterAttribute.INTELLIGENCE,
        CharacterAttribute.PERCEPTION,
        CharacterAttribute.MEMORY,
        CharacterAttribute.INTUITION,
        CharacterAttribute.AGILITY,
        CharacterAttribute.STAMINA,
        CharacterAttribute.STRENGTH,
        CharacterAttribute.CHARISMA,
        CharacterAttribute.INTIMIDATION,
        CharacterAttribute.EMPATHY,
        CharacterAttribute.STEALTH
    };

    /** Number of investigative attributes (always 11). */
    private static final int INVESTIGATIVE_ATTR_COUNT = INVESTIGATIVE_ATTRS.length;
    /** Minimum possible sum (all attributes at 1). */
    private static final int MIN_ATTR_SUM = INVESTIGATIVE_ATTR_COUNT;   // 11
    /** Maximum possible sum (all attributes at 10). */
    private static final int MAX_ATTR_SUM = INVESTIGATIVE_ATTR_COUNT * 10; // 110

    /**
     * Returns the detective's overall capability level (1–10), derived from the
     * sum of the eleven investigative attributes.
     *
     * <p>Formula:
     * <pre>
     *   level = 1 + (sum - MIN_ATTR_SUM) / (INVESTIGATIVE_ATTR_COUNT)
     * </pre>
     * At minimum (all attributes = 1, sum = 11) the level is 1; at maximum
     * (all attributes = 10, sum = 110) the level is 10.
     *
     * <p>This value is shown next to the character name in the UI and serves as
     * the {@link CharacterAttribute#DETECTIVE_LEVEL} derived attribute.
     */
    public int getDetectiveLevel() {
        int sum = 0;
        for (CharacterAttribute attr : INVESTIGATIVE_ATTRS) {
            sum += getAttribute(attr.name());
        }
        int level = 1 + (sum - MIN_ATTR_SUM) / INVESTIGATIVE_ATTR_COUNT;
        return Math.min(10, Math.max(1, level));
    }

    /** Returns the current in-game hour (0–23), or 0 if parsing fails. */
    public int getCurrentHour() {
        try {
            return Integer.parseInt(gameDateTime.split(" ")[1].split(":")[0]);
        } catch (Exception e) { return 0; }
    }

    /** Returns the current in-game minute (0–59), or 0 if parsing fails. */
    public int getCurrentMinute() {
        try {
            return Integer.parseInt(gameDateTime.split(" ")[1].split(":")[1]);
        } catch (Exception e) { return 0; }
    }

    /**
     * Advances the in-game date/time by the specified number of minutes.
     * The date/time string format is "YYYY-MM-DD HH:MM".
     *
     * @param minutes Number of minutes to advance
     */
    public void advanceGameTime(int minutes) {
        if (minutes <= 0) return;
        try {
            String[] spaceSplit = gameDateTime.split(" ");
            String[] dateParts = spaceSplit[0].split("-");
            String[] timeParts = spaceSplit[1].split(":");

            int year   = Integer.parseInt(dateParts[0]);
            int month  = Integer.parseInt(dateParts[1]);
            int day    = Integer.parseInt(dateParts[2]);
            int hour   = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            minute += minutes;
            hour   += minute / 60;
            minute  = minute % 60;
            day    += hour / 24;
            hour    = hour % 24;

            int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
            // Update Feb length for leap years
            if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
                daysInMonth[1] = 29;
            }
            while (day > daysInMonth[month - 1]) {
                day -= daysInMonth[month - 1];
                month++;
                if (month > 12) {
                    month = 1;
                    year++;
                    // Recompute Feb length for the new year
                    daysInMonth[1] = ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) ? 29 : 28;
                }
            }

            gameDateTime = String.format("%04d-%02d-%02d %02d:%02d",
                    year, month, day, hour, minute);
        } catch (Exception e) {
            // Leave unchanged if parsing fails
        }
    }
    
    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /** Unique identifier for this character instance (UUID string). */
    public String getCharacterId() { return characterId; }

    // -------------------------------------------------------------------------
    // Equipment
    // -------------------------------------------------------------------------

    /**
     * Returns the item currently equipped in the given non-utility slot, or
     * {@code null} if the slot is empty.
     */
    public EquipItem getEquipped(EquipmentSlot slot) {
        if (slot == EquipmentSlot.UTILITY) return null; // utility uses getUtilityItems()
        return equipment.get(slot);
    }

    /**
     * Equips {@code item} in its designated slot, replacing any previous item.
     * For {@link EquipmentSlot#UTILITY} items use {@link #addUtilityItem(EquipItem)}.
     *
     * @throws IllegalArgumentException if {@code item} is null or is a UTILITY item
     */
    public void equip(EquipItem item) {
        if (item == null) throw new IllegalArgumentException("Item must not be null");
        if (item.getSlot() == EquipmentSlot.UTILITY)
            throw new IllegalArgumentException("Use addUtilityItem() for UTILITY items");
        equipment.put(item.getSlot(), item);
    }

    /**
     * Removes the item from the given non-utility slot (makes it empty).
     * Has no effect if the slot is already empty or is UTILITY.
     */
    public void unequip(EquipmentSlot slot) {
        if (slot != EquipmentSlot.UTILITY) equipment.remove(slot);
    }

    /**
     * Adds a utility item.  Multiple utility items may be carried simultaneously.
     *
     * @throws IllegalArgumentException if {@code item} is null or is not a UTILITY item
     */
    public void addUtilityItem(EquipItem item) {
        if (item == null) throw new IllegalArgumentException("Item must not be null");
        if (item.getSlot() != EquipmentSlot.UTILITY)
            throw new IllegalArgumentException("Item is not a UTILITY item");
        utilityItems.add(item);
    }

    /** Removes the first utility item with the given name. Returns {@code true} if removed. */
    public boolean removeUtilityItem(String itemName) {
        return utilityItems.removeIf(i -> i.getName().equals(itemName));
    }

    /** Returns an unmodifiable view of all utility items currently carried. */
    public List<EquipItem> getUtilityItems() {
        return Collections.unmodifiableList(utilityItems);
    }

    /** Returns an unmodifiable view of all items currently in the stash. */
    public List<EquipItem> getStash() {
        return Collections.unmodifiableList(stash);
    }

    /** Adds {@code item} to the stash (must not be null). */
    public void addToStash(EquipItem item) {
        if (item != null) stash.add(item);
    }

    // -------------------------------------------------------------------------
    // Calendar
    // -------------------------------------------------------------------------

    /** Returns an unmodifiable view of all calendar entries. */
    public List<CalendarEntry> getCalendarEntries() {
        return Collections.unmodifiableList(calendarEntries);
    }

    /** Adds a calendar entry (must not be null). */
    public void addCalendarEntry(CalendarEntry entry) {
        if (entry != null) calendarEntries.add(entry);
    }

    /** Removes a calendar entry (no-op if null or not found). */
    public void removeCalendarEntry(CalendarEntry entry) {
        if (entry != null) calendarEntries.remove(entry);
    }

    /** Removes all calendar entries whose dateTime is strictly before the current game dateTime. */
    public void removeExpiredCalendarEntries() {
        String now = getGameDateTime();
        calendarEntries.removeIf(e -> e.dateTime != null && e.dateTime.compareTo(now) < 0);
    }

    /** Returns the game-date string (YYYY-MM-DD) when emails were last generated, or "" if never. */
    public String getLastEmailCheckDate() { return lastEmailCheckDate; }

    /** Records the game-date string (YYYY-MM-DD) when emails were last generated. */
    public void setLastEmailCheckDate(String date) {
        this.lastEmailCheckDate = date != null ? date : "";
    }

    // -------------------------------------------------------------------------
    // Phone contacts
    // -------------------------------------------------------------------------

    /**
     * Marks the given contact as phoned.
     *
     * @param caseId      ID of the case this contact belongs to
     * @param contactName name of the contact
     */
    public void markContactPhoned(String caseId, String contactName) {
        if (caseId != null && contactName != null) {
            phonedContactKeys.add(caseId + "|" + contactName);
        }
    }

    /**
     * Returns {@code true} if the player has already phoned the given contact.
     *
     * @param caseId      ID of the case this contact belongs to
     * @param contactName name of the contact
     */
    public boolean isContactPhoned(String caseId, String contactName) {
        if (caseId == null || contactName == null) return false;
        return phonedContactKeys.contains(caseId + "|" + contactName);
    }

    /**
     * Sets the {@link PhoneMessageRating} for a contact's call.
     * The contact is also automatically marked as phoned.
     *
     * @param caseId      ID of the case this contact belongs to
     * @param contactName name of the contact
     * @param rating      the rating to assign; {@code null} clears any existing rating
     */
    public void setContactMessageRating(String caseId, String contactName,
                                        PhoneMessageRating rating) {
        if (caseId == null || contactName == null) return;
        String key = caseId + "|" + contactName;
        phonedContactKeys.add(key);
        if (rating != null) {
            contactMessageRatings.put(key, rating);
        } else {
            contactMessageRatings.remove(key);
        }
    }

    /**
     * Returns the {@link PhoneMessageRating} for a contact's call, or
     * {@code null} if the contact has not been rated (or not called at all).
     *
     * @param caseId      ID of the case this contact belongs to
     * @param contactName name of the contact
     */
    public PhoneMessageRating getContactMessageRating(String caseId, String contactName) {
        if (caseId == null || contactName == null) return null;
        return contactMessageRatings.get(caseId + "|" + contactName);
    }

    /**
     * Removes and returns the stash item at {@code index}, or {@code null} if the
     * index is out of range.
     */
    public EquipItem takeFromStash(int index) {
        if (index < 0 || index >= stash.size()) return null;
        return stash.remove(index);
    }

    /**
     * Removes the utility item at the given 0-based index in {@link #getUtilityItems()}.
     * Returns {@code true} if the item was removed.
     */
    public boolean removeUtilityItemAt(int index) {
        if (index < 0 || index >= utilityItems.size()) return false;
        utilityItems.remove(index);
        return true;
    }

    /**
     * Returns the total attribute modifier contributed by all currently equipped items
     * (non-utility slots + all utility items).
     */
    public int getEquipmentModifier(CharacterAttribute attr) {
        int total = 0;
        for (EquipItem item : equipment.values()) {
            total += item.getModifiers().getOrDefault(attr, 0);
        }
        for (EquipItem item : utilityItems) {
            total += item.getModifiers().getOrDefault(attr, 0);
        }
        return total;
    }

    // -------------------------------------------------------------------------
    // Case files
    // -------------------------------------------------------------------------

    private final List<CaseFile> caseFiles = new ArrayList<>();
    private CaseFile activeCaseFile = null;

    /** Adds a case file and makes it the active case. */
    public void addCaseFile(CaseFile caseFile) {
        if (caseFile == null) throw new IllegalArgumentException("CaseFile must not be null");
        caseFiles.add(caseFile);
        activeCaseFile = caseFile;
    }

    /** Returns an unmodifiable view of all case files. */
    public List<CaseFile> getCaseFiles() {
        return Collections.unmodifiableList(caseFiles);
    }

    /** Returns only the open case files. */
    public List<CaseFile> getOpenCases() {
        List<CaseFile> open = new ArrayList<>();
        for (CaseFile cf : caseFiles) {
            if (cf.isOpen()) open.add(cf);
        }
        return open;
    }

    /** Returns the currently selected/active case file, or {@code null} if none. */
    public CaseFile getActiveCaseFile() {
        return activeCaseFile;
    }

    /** Sets the active case file (must already be in the case files list, or null). */
    public void setActiveCaseFile(CaseFile caseFile) {
        if (caseFile != null && !caseFiles.contains(caseFile)) {
            throw new IllegalArgumentException("CaseFile is not in the case files list");
        }
        this.activeCaseFile = caseFile;
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "Profile{" +
                "characterId='" + characterId + '\'' +
                ", characterName='" + characterName + '\'' +
                ", gender='" + gender + '\'' +
                ", difficulty='" + difficulty + '\'' +
                '}';
    }
}


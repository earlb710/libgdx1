package eb.framework1;

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
        this.gameDateTime = "2050-01-02 13:20";
        this.characterId = UUID.randomUUID().toString();
        this.equipment   = new EnumMap<>(EquipmentSlot.class);
        this.utilityItems = new ArrayList<>();
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

    /** Divisor converting body weight (kg) to base carry capacity (kg). */
    private static final float BODY_WEIGHT_CAPACITY_DIVISOR = 4f;

    /** Carry capacity gain (kg) per point of STRENGTH. */
    private static final float STRENGTH_CARRY_KG_PER_POINT = 2f;

    /** Kg of muscle or fat needed to shift STRENGTH by ±1 point. */
    private static final int MUSCLE_FAT_STRENGTH_DIVISOR = 10;

    /**
     * Fallback body weights used when an old profile has no muscle/fat data.
     * Values match the gender defaults in {@code CharacterAttributeScreen}.
     */
    private static final int DEFAULT_BODY_WEIGHT_MALE   = 80;
    private static final int DEFAULT_BODY_WEIGHT_FEMALE = 65;

    // -------------------------------------------------------------------------

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
     * {@code bodyWeight = muscleKg + fatKg}.
     * <ul>
     *   <li>Base carry = total body weight / 4 (20 kg for an 80 kg person)</li>
     *   <li>+ 2 kg per point of STRENGTH attribute</li>
     * </ul>
     *
     * <p>Old profiles that were saved before body-composition tracking was added
     * may have {@code MUSCLE_KG} and {@code FAT_KG} both at zero.  In that case
     * a gender-average body weight is used as a fallback so the carry capacity is
     * not unrealistically low.
     */
    public float getWeightCapacity() {
        int muscleKg = getAttribute(CharacterAttribute.MUSCLE_KG.name());
        int fatKg    = getAttribute(CharacterAttribute.FAT_KG.name());
        int strength = getAttribute(CharacterAttribute.STRENGTH.name());
        int bodyWeight = muscleKg + fatKg;
        if (bodyWeight == 0) {
            // Old profile with no body-composition data — fall back to gender average
            boolean isFemale = "female".equalsIgnoreCase(gender);
            bodyWeight = isFemale ? DEFAULT_BODY_WEIGHT_FEMALE : DEFAULT_BODY_WEIGHT_MALE;
        }
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


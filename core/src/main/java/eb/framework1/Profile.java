package eb.framework1;

import java.util.HashMap;
import java.util.Map;

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

    /** Maximum stamina pool = STAMINA attribute value × 10. */
    public int getMaxStamina() {
        return Math.max(1, getAttribute(CharacterAttribute.STAMINA.name())) * 10;
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
    
    @Override
    public String toString() {
        return "Profile{" +
                "characterName='" + characterName + '\'' +
                ", gender='" + gender + '\'' +
                ", difficulty='" + difficulty + '\'' +
                ", characterIcon='" + characterIcon + '\'' +
                ", attributes=" + attributes +
                ", gameDate=" + gameDate +
                ", randSeed=" + randSeed +
                '}';
    }
}

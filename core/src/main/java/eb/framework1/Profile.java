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

package eb.framework1;

import java.util.HashMap;
import java.util.Map;

public class Profile {
    private String characterName;
    private String gender;
    private String difficulty;
    private Map<String, Integer> attributes;
    
    public Profile(String characterName, String gender, String difficulty) {
        this(characterName, gender, difficulty, new HashMap<>());
    }
    
    public Profile(String characterName, String gender, String difficulty, Map<String, Integer> attributes) {
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
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
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
    
    public Map<String, Integer> getAttributes() {
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
    
    @Override
    public String toString() {
        return "Profile{" +
                "characterName='" + characterName + '\'' +
                ", gender='" + gender + '\'' +
                ", difficulty='" + difficulty + '\'' +
                ", attributes=" + attributes +
                '}';
    }
}

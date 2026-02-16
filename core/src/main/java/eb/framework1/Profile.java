package eb.framework1;

public class Profile {
    private String characterName;
    private String gender;
    private String difficulty;
    
    public Profile(String characterName, String gender, String difficulty) {
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
    
    @Override
    public String toString() {
        return "Profile{" +
                "characterName='" + characterName + '\'' +
                ", gender='" + gender + '\'' +
                ", difficulty='" + difficulty + '\'' +
                '}';
    }
}

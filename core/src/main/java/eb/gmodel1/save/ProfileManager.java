package eb.gmodel1.save;

import eb.gmodel1.character.*;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.ArrayList;
import java.util.List;

public class ProfileManager {
    private static final String PREFS_NAME = "gmodel1.profiles";
    private static final String KEY_PROFILES = "profiles";
    private static final String KEY_SELECTED_PROFILE = "selectedProfile";
    private static final int MAX_PROFILES = 5; // Maximum number of profiles allowed
    
    private Preferences preferences;
    private List<Profile> profiles;
    private Profile selectedProfile;
    private Json json;
    
    public ProfileManager() {
        Gdx.app.log("ProfileManager", "Constructor called");
        try {
            Gdx.app.log("ProfileManager", "Getting preferences...");
            preferences = Gdx.app.getPreferences(PREFS_NAME);
            
            Gdx.app.log("ProfileManager", "Creating JSON parser...");
            json = new Json();
            json.setOutputType(JsonWriter.OutputType.json);
            
            Gdx.app.log("ProfileManager", "Loading profiles...");
            loadProfiles();
            
            Gdx.app.log("ProfileManager", "Constructor completed successfully");
        } catch (Exception e) {
            Gdx.app.error("ProfileManager", "Error in constructor: " + e.getMessage(), e);
            throw e;
        }
    }
    
    private void loadProfiles() {
        Gdx.app.log("ProfileManager", "loadProfiles() called");
        profiles = new ArrayList<>();
        String profilesJson = preferences.getString(KEY_PROFILES, "[]");
        Gdx.app.log("ProfileManager", "Profiles JSON: " + profilesJson);
        
        try {
            // Parse JSON array of profiles
            String[] profileData = json.fromJson(String[].class, profilesJson);
            Gdx.app.log("ProfileManager", "Parsed profile data array, length: " + (profileData != null ? profileData.length : "null"));
            
            if (profileData != null) {
                for (String data : profileData) {
                    ProfileData pd = json.fromJson(ProfileData.class, data);
                    // Handle backwards compatibility - set defaults if not present
                    int gameDate = (pd.gameDate == 0) ? 2050 : pd.gameDate;
                    long randSeed = (pd.randSeed == 0) ? System.currentTimeMillis() : pd.randSeed;
                    // Default portrait icon based on gender for old profiles
                    String icon = pd.characterIcon;
                    if (icon == null || icon.isEmpty()) {
                        icon = "Female".equalsIgnoreCase(pd.gender) ? "woman1" : "man1";
                    }
                    Profile profile = new Profile(pd.characterName, pd.gender, pd.difficulty, 
                        icon, pd.attributes, gameDate, randSeed);
                    profile.setMoney(pd.money == 0 ? 1000 : pd.money);
                    profile.setGameDateTime(pd.gameDateTime != null ? pd.gameDateTime : "2050-01-02 16:00");
                    profile.setCurrentStamina(pd.currentStamina == 0 ? profile.getMaxStamina() : pd.currentStamina);
                    profiles.add(profile);
                }
            }
            Gdx.app.log("ProfileManager", "Loaded " + profiles.size() + " profiles successfully");
        } catch (Exception e) {
            Gdx.app.error("ProfileManager", "Error loading profiles: " + e.getMessage(), e);
            profiles = new ArrayList<>();
        }
        
        // Load selected profile - use case-insensitive comparison for consistency
        String selectedName = preferences.getString(KEY_SELECTED_PROFILE, null);
        Gdx.app.log("ProfileManager", "Selected profile name: " + selectedName);
        
        if (selectedName != null) {
            for (Profile profile : profiles) {
                if (profile.getName().equalsIgnoreCase(selectedName)) {
                    selectedProfile = profile;
                    Gdx.app.log("ProfileManager", "Found selected profile: " + profile.getName());
                    break;
                }
            }
        }
    }
    
    private void saveProfiles() {
        List<ProfileData> dataList = new ArrayList<>();
        for (Profile profile : profiles) {
            ProfileData pd = new ProfileData();
            pd.characterName = profile.getCharacterName();
            pd.gender = profile.getGender();
            pd.difficulty = profile.getDifficulty();
            pd.characterIcon = profile.getCharacterIcon();
            pd.attributes = profile.getAttributes();
            pd.gameDate = profile.getGameDate();
            pd.randSeed = profile.getRandSeed();
            pd.money = profile.getMoney();
            pd.gameDateTime = profile.getGameDateTime();
            pd.currentStamina = profile.getCurrentStamina();
            dataList.add(pd);
        }
        
        // Convert to JSON array of strings
        String[] profileStrings = new String[dataList.size()];
        for (int i = 0; i < dataList.size(); i++) {
            profileStrings[i] = json.toJson(dataList.get(i));
        }
        
        String profilesJson = json.toJson(profileStrings);
        preferences.putString(KEY_PROFILES, profilesJson);
        preferences.flush();
    }
    
    public Profile createProfile(String characterName, String gender, String difficulty) {
        // Check profile limit
        if (profiles.size() >= MAX_PROFILES) {
            throw new IllegalArgumentException("Maximum number of profiles (" + MAX_PROFILES + ") reached");
        }
        
        // Check if character name already exists (case-insensitive)
        for (Profile profile : profiles) {
            if (profile.getCharacterName().equalsIgnoreCase(characterName)) {
                throw new IllegalArgumentException("Character with name '" + characterName + "' already exists");
            }
        }
        
        Profile newProfile = new Profile(characterName, gender, difficulty);
        profiles.add(newProfile);
        saveProfiles();
        return newProfile;
    }
    
    public void addProfile(Profile profile) {
        // Check profile limit
        if (profiles.size() >= MAX_PROFILES) {
            throw new IllegalArgumentException("Maximum number of profiles (" + MAX_PROFILES + ") reached");
        }
        
        // Check if character name already exists (case-insensitive)
        for (Profile existingProfile : profiles) {
            if (existingProfile.getCharacterName().equalsIgnoreCase(profile.getCharacterName())) {
                throw new IllegalArgumentException("Character with name '" + profile.getCharacterName() + "' already exists");
            }
        }
        profiles.add(profile);
        saveProfiles();
    }
    
    public Profile getProfileByName(String characterName) {
        for (Profile profile : profiles) {
            if (profile.getCharacterName().equalsIgnoreCase(characterName)) {
                return profile;
            }
        }
        return null;
    }
    
    public void selectProfile(Profile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile cannot be null");
        }
        if (!profiles.contains(profile)) {
            throw new IllegalArgumentException("Profile not found in profiles list");
        }
        selectedProfile = profile;
        preferences.putString(KEY_SELECTED_PROFILE, profile.getName());
        preferences.flush();
    }
    
    public Profile getSelectedProfile() {
        return selectedProfile;
    }
    
    public List<Profile> getProfiles() {
        return new ArrayList<>(profiles);
    }
    
    public boolean hasProfiles() {
        boolean result = !profiles.isEmpty();
        Gdx.app.log("ProfileManager", "hasProfiles() returning: " + result);
        return result;
    }
    
    public boolean canCreateNewProfile() {
        return profiles.size() < MAX_PROFILES;
    }
    
    public int getMaxProfiles() {
        return MAX_PROFILES;
    }
    
    public void deleteProfile(Profile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile cannot be null");
        }
        
        Gdx.app.log("ProfileManager", "Deleting profile: " + profile.getName());
        
        // If this is the selected profile, clear the selection
        if (profile.equals(selectedProfile)) {
            selectedProfile = null;
            preferences.remove(KEY_SELECTED_PROFILE);
        }
        
        // Remove from list
        profiles.remove(profile);
        
        // Save updated profiles
        saveProfiles();
        
        Gdx.app.log("ProfileManager", "Profile deleted successfully. Remaining profiles: " + profiles.size());
    }
    
    // Helper class for JSON serialization
    private static class ProfileData {
        public String characterName;
        public String gender;
        public String difficulty;
        public String characterIcon;
        public java.util.Map<String, Integer> attributes;
        public int gameDate;
        public long randSeed;
        public int money;
        public String gameDateTime;
        public int currentStamina;
    }
}

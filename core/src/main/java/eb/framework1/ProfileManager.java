package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.ArrayList;
import java.util.List;

public class ProfileManager {
    private static final String PREFS_NAME = "framework1.profiles";
    private static final String KEY_PROFILES = "profiles";
    private static final String KEY_SELECTED_PROFILE = "selectedProfile";
    
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
                    Profile profile = new Profile(pd.characterName, pd.gender, pd.difficulty);
                    // Load attributes if present
                    if (pd.attributes != null) {
                        profile.setAttributes(pd.attributes);
                    }
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
            pd.attributes = profile.getAttributes();
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
        public java.util.Map<String, Integer> attributes;
    }
}

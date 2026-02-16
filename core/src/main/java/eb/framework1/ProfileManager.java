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
        preferences = Gdx.app.getPreferences(PREFS_NAME);
        json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        loadProfiles();
    }
    
    private void loadProfiles() {
        profiles = new ArrayList<>();
        String profilesJson = preferences.getString(KEY_PROFILES, "[]");
        
        try {
            // Parse JSON array of profiles
            String[] profileData = json.fromJson(String[].class, profilesJson);
            if (profileData != null) {
                for (String data : profileData) {
                    ProfileData pd = json.fromJson(ProfileData.class, data);
                    profiles.add(new Profile(pd.name, pd.characterName, pd.gender, pd.difficulty));
                }
            }
        } catch (Exception e) {
            Gdx.app.error("ProfileManager", "Error loading profiles: " + e.getMessage());
            profiles = new ArrayList<>();
        }
        
        // Load selected profile
        String selectedName = preferences.getString(KEY_SELECTED_PROFILE, null);
        if (selectedName != null) {
            for (Profile profile : profiles) {
                if (profile.getName().equals(selectedName)) {
                    selectedProfile = profile;
                    break;
                }
            }
        }
    }
    
    private void saveProfiles() {
        List<ProfileData> dataList = new ArrayList<>();
        for (Profile profile : profiles) {
            ProfileData pd = new ProfileData();
            pd.name = profile.getName();
            pd.characterName = profile.getCharacterName();
            pd.gender = profile.getGender();
            pd.difficulty = profile.getDifficulty();
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
    
    public void createProfile(String name, String characterName, String gender, String difficulty) {
        // Check if profile name already exists
        for (Profile profile : profiles) {
            if (profile.getName().equalsIgnoreCase(name)) {
                throw new IllegalArgumentException("Profile with name '" + name + "' already exists");
            }
        }
        
        Profile newProfile = new Profile(name, characterName, gender, difficulty);
        profiles.add(newProfile);
        saveProfiles();
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
        return !profiles.isEmpty();
    }
    
    // Helper class for JSON serialization
    private static class ProfileData {
        public String name;
        public String characterName;
        public String gender;
        public String difficulty;
    }
}

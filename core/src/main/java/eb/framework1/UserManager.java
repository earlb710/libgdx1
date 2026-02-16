package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class UserManager {
    private static final String PREFS_NAME = "framework1.preferences";
    private static final String KEY_USERNAME = "username";
    
    private Preferences preferences;
    private String currentUser;
    
    public UserManager() {
        preferences = Gdx.app.getPreferences(PREFS_NAME);
        currentUser = preferences.getString(KEY_USERNAME, null);
    }
    
    public boolean hasUser() {
        return currentUser != null && !currentUser.trim().isEmpty();
    }
    
    public String getCurrentUser() {
        return currentUser;
    }
    
    public void setCurrentUser(String username) {
        currentUser = username.trim();
        preferences.putString(KEY_USERNAME, currentUser);
        preferences.flush();
    }
}

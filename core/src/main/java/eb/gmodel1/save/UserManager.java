package eb.gmodel1.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class UserManager {
    private static final String PREFS_NAME = "gmodel1.preferences";
    private static final String KEY_USERNAME = "username";
    
    private Preferences preferences;
    private String currentUser;
    
    public UserManager() {
        preferences = Gdx.app.getPreferences(PREFS_NAME);
        currentUser = preferences.getString(KEY_USERNAME, null);
        // Ensure loaded username is trimmed for consistency
        if (currentUser != null) {
            currentUser = currentUser.trim();
            // Clear empty strings
            if (currentUser.isEmpty()) {
                currentUser = null;
            }
        }
    }
    
    public boolean hasUser() {
        return currentUser != null && !currentUser.isEmpty();
    }
    
    public String getCurrentUser() {
        return currentUser;
    }
    
    public void setCurrentUser(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        currentUser = username.trim();
        if (currentUser.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        preferences.putString(KEY_USERNAME, currentUser);
        preferences.flush();
    }
}

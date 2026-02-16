package eb.framework1;

import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    private UserManager userManager;

    @Override
    public void create() {
        userManager = new UserManager();
        
        // Check if user exists, if not show login screen
        if (userManager.hasUser()) {
            setScreen(new MainScreen(this));
        } else {
            setScreen(new LoginScreen(this));
        }
    }
    
    public UserManager getUserManager() {
        return userManager;
    }
}

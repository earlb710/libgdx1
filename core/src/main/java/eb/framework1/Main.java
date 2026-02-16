package eb.framework1;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    private UserManager userManager;

    @Override
    public void create() {
        userManager = new UserManager();
        
        // Check if user exists, if not show login screen
        if (userManager.hasUser()) {
            setScreen(new SplashScreen(this));
        } else {
            setScreen(new LoginScreen(this));
        }
    }
    
    public UserManager getUserManager() {
        return userManager;
    }
    
    @Override
    public void setScreen(Screen screen) {
        // Dispose old screen to prevent resource leaks
        // Note: LibGDX Game class does NOT automatically dispose screens
        Screen oldScreen = getScreen();
        super.setScreen(screen);
        if (oldScreen != null) {
            oldScreen.dispose();
        }
    }
}

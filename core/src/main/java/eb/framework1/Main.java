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
            setScreen(new MainScreen(this));
        } else {
            setScreen(new LoginScreen(this));
        }
    }
    
    public UserManager getUserManager() {
        return userManager;
    }
    
    @Override
    public void setScreen(Screen screen) {
        // Dispose old screen before setting new one
        Screen oldScreen = getScreen();
        if (oldScreen != null) {
            oldScreen.dispose();
        }
        super.setScreen(screen);
    }
}

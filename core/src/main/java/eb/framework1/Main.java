package eb.framework1;

import eb.framework1.save.*;
import eb.framework1.screen.*;
import eb.framework1.ui.*;


import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    private UserManager userManager;
    private ProfileManager profileManager;
    private FontManager fontManager;
    private GameDataManager gameDataManager;
    private Viewport viewport;

    @Override
    public void create() {
        Gdx.app.log("Main", "create() called");
        try {
            // Create ScreenViewport for pixel-perfect rendering
            // ScreenViewport uses screen coordinates (1:1 mapping to pixels)
            // This ensures crisp text rendering without interpolation artifacts
            Gdx.app.log("Main", "Creating ScreenViewport...");
            viewport = new ScreenViewport();
            
            Gdx.app.log("Main", "Creating FontManager...");
            fontManager = new FontManager();
            
            Gdx.app.log("Main", "Creating UserManager...");
            userManager = new UserManager();
            
            Gdx.app.log("Main", "Creating ProfileManager...");
            profileManager = new ProfileManager();
            
            Gdx.app.log("Main", "Creating GameDataManager...");
            gameDataManager = new GameDataManager();
            
            // Check if user exists, if not show login screen
            Gdx.app.log("Main", "Checking if user exists...");
            if (userManager.hasUser()) {
                Gdx.app.log("Main", "User exists, showing SplashScreen");
                setScreen(new SplashScreen(this));
            } else {
                Gdx.app.log("Main", "No user, showing LoginScreen");
                setScreen(new LoginScreen(this));
            }
            Gdx.app.log("Main", "create() completed successfully");
        } catch (Exception e) {
            Gdx.app.error("Main", "Error in create(): " + e.getMessage(), e);
        }
    }
    
    public UserManager getUserManager() {
        return userManager;
    }
    
    public ProfileManager getProfileManager() {
        return profileManager;
    }
    
    public FontManager getFontManager() {
        return fontManager;
    }
    
    public GameDataManager getGameDataManager() {
        return gameDataManager;
    }
    
    public Viewport getViewport() {
        return viewport;
    }
    
    @Override
    public void setScreen(Screen screen) {
        Gdx.app.log("Main", "setScreen() called with: " + screen.getClass().getSimpleName());
        try {
            // Dispose old screen to prevent resource leaks
            // Note: LibGDX Game class does NOT automatically dispose screens
            Screen oldScreen = getScreen();
            if (oldScreen != null) {
                Gdx.app.log("Main", "Old screen: " + oldScreen.getClass().getSimpleName());
            }
            
            Gdx.app.log("Main", "Calling super.setScreen()...");
            super.setScreen(screen);
            
            if (oldScreen != null) {
                Gdx.app.log("Main", "Disposing old screen: " + oldScreen.getClass().getSimpleName());
                oldScreen.dispose();
                Gdx.app.log("Main", "Old screen disposed");
            }
            
            Gdx.app.log("Main", "setScreen() completed successfully");
        } catch (Exception e) {
            Gdx.app.error("Main", "Error in setScreen(): " + e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public void dispose() {
        Gdx.app.log("Main", "dispose() called");
        if (fontManager != null) {
            fontManager.dispose();
        }
        super.dispose();
    }
}

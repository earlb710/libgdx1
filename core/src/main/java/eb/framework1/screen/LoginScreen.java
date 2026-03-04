package eb.framework1.screen;

import eb.framework1.*;
import eb.framework1.ui.*;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

public class LoginScreen implements Screen {
    private Main game;
    private SpriteBatch batch;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    private StringBuilder usernameInput;
    private boolean cursorVisible;
    private float cursorTimer;
    private boolean initialized = false;
    private FontManager fontManager;
    private static final float CURSOR_BLINK_TIME = 0.5f;
    // Minimum username length for validation
    private static final int MIN_USERNAME_LENGTH = 2;
    // Maximum username length to prevent UI overflow and keep usernames manageable
    private static final int MAX_USERNAME_LENGTH = 20;
    
    public LoginScreen(Main game) {
        this.game = game;
        this.usernameInput = new StringBuilder();
        this.cursorVisible = true;
        this.cursorTimer = 0;
    }
    
    @Override
    public void show() {
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        
        // Get FontManager from Main game
        this.fontManager = game.getFontManager();
        this.font = fontManager.getBodyFont();
        
        // Set up input processor
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
                if (character == '\r' || character == '\n') {
                    // Enter key
                    if (usernameInput.toString().trim().length() >= MIN_USERNAME_LENGTH) {
                        login();
                    }
                    return true;
                } else if (character == '\b') {
                    // Backspace
                    if (usernameInput.length() > 0) {
                        usernameInput.deleteCharAt(usernameInput.length() - 1);
                    }
                    return true;
                } else if (Character.isLetterOrDigit(character) || character == ' ') {
                    if (usernameInput.length() < MAX_USERNAME_LENGTH) {
                        usernameInput.append(character);
                    }
                    return true;
                }
                return false;
            }
        });
        
        initialized = true;
    }
    
    @Override
    public void render(float delta) {
        // Skip rendering if not initialized yet
        if (!initialized) {
            ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
            return;
        }
        
        // Update cursor blinking
        cursorTimer += delta;
        if (cursorTimer >= CURSOR_BLINK_TIME) {
            cursorVisible = !cursorVisible;
            cursorTimer = 0;
        }
        
        // Check again - screen transition might have started from input handler
        if (!initialized) {
            ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
            return;
        }
        
        // Clear screen
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        
        // Draw UI
        batch.begin();
        
        // Title
        font.draw(batch, "Welcome! Please enter your name:", 
                  Gdx.graphics.getWidth() / 2 - 200, 
                  Gdx.graphics.getHeight() / 2 + 100);
        
        // Username input box
        String displayText = usernameInput.toString();
        if (cursorVisible) {
            displayText += "|";
        }
        font.draw(batch, displayText, 
                  Gdx.graphics.getWidth() / 2 - 150, 
                  Gdx.graphics.getHeight() / 2);
        
        // Instructions - use small font from FontManager
        BitmapFont smallFont = fontManager.getSmallFont();
        smallFont.draw(batch, "Press ENTER to login (minimum " + MIN_USERNAME_LENGTH + " characters)", 
                  Gdx.graphics.getWidth() / 2 - 170, 
                  Gdx.graphics.getHeight() / 2 - 100);
        
        batch.end();
        
        // Draw input box border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(Gdx.graphics.getWidth() / 2 - 160, 
                          Gdx.graphics.getHeight() / 2 - 30, 
                          320, 40);
        shapeRenderer.end();
    }
    
    private void login() {
        // Trim and validate final username before saving
        String username = usernameInput.toString().trim();
        if (username.length() >= MIN_USERNAME_LENGTH) {
            game.getUserManager().setCurrentUser(username);
            // Stop rendering before transition
            initialized = false;
            game.setScreen(new SplashScreen(game));
        }
    }
    
    @Override
    public void resize(int width, int height) {
    }
    
    @Override
    public void pause() {
    }
    
    @Override
    public void resume() {
    }
    
    @Override
    public void hide() {
        // Clear input processor when screen is hidden
        Gdx.input.setInputProcessor(null);
    }
    
    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        // Fonts are managed by FontManager, don't dispose them here
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}

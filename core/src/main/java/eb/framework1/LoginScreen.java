package eb.framework1;

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
    private static final float CURSOR_BLINK_TIME = 0.5f;
    private static final int MAX_USERNAME_LENGTH = 20;
    
    public LoginScreen(Main game) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        this.font.setColor(Color.WHITE);
        this.font.getData().setScale(2.0f);
        this.shapeRenderer = new ShapeRenderer();
        this.usernameInput = new StringBuilder();
        this.cursorVisible = true;
        this.cursorTimer = 0;
        
        // Set up input processor
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
                if (character == '\r' || character == '\n') {
                    // Enter key
                    if (usernameInput.length() > 0) {
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
    }
    
    @Override
    public void show() {
    }
    
    @Override
    public void render(float delta) {
        // Update cursor blinking
        cursorTimer += delta;
        if (cursorTimer >= CURSOR_BLINK_TIME) {
            cursorVisible = !cursorVisible;
            cursorTimer = 0;
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
        
        // Instructions
        font.getData().setScale(1.0f);
        font.draw(batch, "Press ENTER to login", 
                  Gdx.graphics.getWidth() / 2 - 100, 
                  Gdx.graphics.getHeight() / 2 - 100);
        font.getData().setScale(2.0f);
        
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
        String username = usernameInput.toString().trim();
        if (!username.isEmpty()) {
            game.getUserManager().setCurrentUser(username);
            Screen nextScreen = new MainScreen(game);
            game.setScreen(nextScreen);
            // Dispose this screen after transitioning
            dispose();
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
        // Clear input processor
        Gdx.input.setInputProcessor(null);
        batch.dispose();
        font.dispose();
        shapeRenderer.dispose();
    }
}

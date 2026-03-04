package eb.framework1.screen;

import eb.framework1.*;
import eb.framework1.ui.*;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

public class SplashScreen implements Screen {
    private Main game;
    private SpriteBatch batch;
    private BitmapFont titleFont;
    private BitmapFont subtitleFont;
    private BitmapFont buttonFont;
    private ShapeRenderer shapeRenderer;
    private GlyphLayout glyphLayout;
    private Texture logo;
    private boolean initialized = false;
    private FontManager fontManager;
    
    // Game title
    private static final String GAME_TITLE = "Veritas Detegere";
    private static final String GAME_SUBTITLE = "A Detective Game";
    
    // Button dimensions and positions
    private Rectangle playButton;
    private Rectangle quitButton;
    private Rectangle yesButton;
    private Rectangle noButton;
    private boolean   quitConfirming = false;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 60;
    
    // Colors
    private Color buttonColor = new Color(0.3f, 0.3f, 0.4f, 1f);
    private Color buttonHoverColor = new Color(0.4f, 0.4f, 0.5f, 1f);
    private Color buttonTextColor = Color.WHITE;
    
    public SplashScreen(Main game) {
        Gdx.app.log("SplashScreen", "Constructor called");
        this.game = game;
    }
    
    @Override
    public void show() {
        Gdx.app.log("SplashScreen", "show() called - initializing resources");
        try {
            this.batch = new SpriteBatch();
            Gdx.app.log("SplashScreen", "SpriteBatch created");
            
            this.shapeRenderer = new ShapeRenderer();
            Gdx.app.log("SplashScreen", "ShapeRenderer created");
            
            this.glyphLayout = new GlyphLayout();
            Gdx.app.log("SplashScreen", "GlyphLayout created");
            
            this.logo = new Texture("logo.png");
            Gdx.app.log("SplashScreen", "Logo texture loaded");
            
            // Get FontManager from Main game
            this.fontManager = game.getFontManager();
            
            // Use viewport-based fonts from FontManager
            this.titleFont = fontManager.getTitleFont();
            Gdx.app.log("SplashScreen", "Title font retrieved from FontManager");
            
            this.subtitleFont = fontManager.getSubtitleFont();
            Gdx.app.log("SplashScreen", "Subtitle font retrieved from FontManager");
            
            this.buttonFont = fontManager.getBodyFont();
            Gdx.app.log("SplashScreen", "Button font retrieved from FontManager");
            
            // Initialize button positions (will be properly set in resize)
            int centerX = Gdx.graphics.getWidth() / 2;
            int centerY = Gdx.graphics.getHeight() / 2;
            
            playButton = new Rectangle(
                centerX - BUTTON_WIDTH / 2,
                centerY - 200,  // Moved down significantly to avoid subtitle overlap with extra space
                BUTTON_WIDTH,
                BUTTON_HEIGHT
            );
            
            quitButton = new Rectangle(
                centerX - BUTTON_WIDTH / 2,
                centerY - 280,  // Moved down to maintain spacing with extra buffer
                BUTTON_WIDTH,
                BUTTON_HEIGHT
            );
            
            initialized = true;
            Gdx.app.log("SplashScreen", "Initialization complete - screen ready to render");
        } catch (Exception e) {
            Gdx.app.error("SplashScreen", "Error during initialization: " + e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public void render(float delta) {
        // Skip rendering if not initialized yet
        if (!initialized) {
            Gdx.app.log("SplashScreen", "render() called but not initialized yet, skipping");
            ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
            return;
        }
        
        // Handle input
        handleInput();
        
        // Check again after handleInput - might have started screen transition
        if (!initialized) {
            Gdx.app.log("SplashScreen", "Screen transition started, skipping render");
            ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
            return;
        }
        
        // Clear screen with dark background
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
        
        // Draw logo, title and subtitle
        batch.begin();
        
        // Logo above title
        float logoX = (Gdx.graphics.getWidth() - logo.getWidth()) / 2;
        float logoY = Gdx.graphics.getHeight() / 2 + 220;
        batch.draw(logo, logoX, logoY);
        
        // Title: "Veritas Detegere"
        glyphLayout.setText(titleFont, GAME_TITLE);
        float titleX = (Gdx.graphics.getWidth() - glyphLayout.width) / 2;
        float titleY = Gdx.graphics.getHeight() / 2 + 150;
        titleFont.draw(batch, GAME_TITLE, titleX, titleY);
        
        // Subtitle: "A Detective Game"
        // Positioned lower to avoid overlapping with title (150px gap instead of 50px)
        glyphLayout.setText(subtitleFont, GAME_SUBTITLE);
        float subtitleX = (Gdx.graphics.getWidth() - glyphLayout.width) / 2;
        float subtitleY = Gdx.graphics.getHeight() / 2 + 0;  // Changed from +100 to +0
        subtitleFont.draw(batch, GAME_SUBTITLE, subtitleX, subtitleY);
        
        batch.end();
        
        // Draw buttons
        drawButtons();
    }
    
    private void drawButtons() {
        int mouseY = getFlippedMouseY();
        int mouseX = Gdx.input.getX();

        if (quitConfirming) {
            // Draw confirmation question
            batch.begin();
            glyphLayout.setText(buttonFont, "Exit App : Are you sure?");
            float qX = (Gdx.graphics.getWidth() - glyphLayout.width) / 2f;
            float qY = quitButton.y + BUTTON_HEIGHT + glyphLayout.height + 20f;
            buttonFont.setColor(Color.WHITE);
            buttonFont.draw(batch, "Exit App : Are you sure?", qX, qY);
            batch.end();

            // Yes / No buttons (sized by TextMeasurer for consistency)
            int scrW = Gdx.graphics.getWidth();
            float pad = 20f;
            TextMeasurer.TextBounds yB = TextMeasurer.measure(buttonFont, glyphLayout, "YES", 48f, 22f);
            TextMeasurer.TextBounds nB = TextMeasurer.measure(buttonFont, glyphLayout, "NO",  48f, 22f);
            float totalW = yB.width + pad + nB.width;
            float startX = (scrW - totalW) / 2f;
            float btnY   = quitButton.y;
            yesButton = new Rectangle(startX,                  btnY, yB.width, yB.height);
            noButton  = new Rectangle(startX + yB.width + pad, btnY, nB.width, nB.height);

            drawButton(yesButton, mouseX, mouseY);
            drawButtonText(yesButton, "YES");
            drawButton(noButton,  mouseX, mouseY);
            drawButtonText(noButton, "NO");
        } else {
            // Normal Play + Quit buttons
            drawButton(playButton, mouseX, mouseY);
            drawButtonText(playButton, "PLAY");

            drawButton(quitButton, mouseX, mouseY);
            drawButtonText(quitButton, "QUIT");
        }
    }
    
    private void drawButton(Rectangle button, int mouseX, int mouseY) {
        // Draw filled button
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (button.contains(mouseX, mouseY)) {
            shapeRenderer.setColor(buttonHoverColor);
        } else {
            shapeRenderer.setColor(buttonColor);
        }
        shapeRenderer.rect(button.x, button.y, button.width, button.height);
        shapeRenderer.end();
        
        // Draw button border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(button.x, button.y, button.width, button.height);
        shapeRenderer.end();
    }
    
    private void drawButtonText(Rectangle button, String text) {
        batch.begin();
        glyphLayout.setText(buttonFont, text);
        float textX = button.x + (BUTTON_WIDTH - glyphLayout.width) / 2;
        // Center text vertically using glyph height
        float textY = button.y + (BUTTON_HEIGHT + glyphLayout.height) / 2;
        buttonFont.draw(batch, text, textX, textY);
        batch.end();
    }
    
    private int getFlippedMouseY() {
        return Gdx.graphics.getHeight() - Gdx.input.getY();
    }
    
    private void handleInput() {
        if (Gdx.input.justTouched()) {
            int mouseX = Gdx.input.getX();
            int mouseY = getFlippedMouseY();

            if (quitConfirming) {
                // Yes → exit app
                if (yesButton != null && yesButton.contains(mouseX, mouseY)) {
                    Gdx.app.log("SplashScreen", "Quit confirmed – exiting");
                    initialized = false;
                    Gdx.app.exit();
                }
                // No → cancel
                if (noButton != null && noButton.contains(mouseX, mouseY)) {
                    quitConfirming = false;
                }
                return;
            }

            if (playButton.contains(mouseX, mouseY)) {
                Gdx.app.log("SplashScreen", "Play button clicked!");
                try {
                    // Stop rendering this screen before transitioning
                    initialized = false;
                    Gdx.app.log("SplashScreen", "Stopped rendering (initialized = false)");
                    
                    // Check if profiles exist, show selection or creation screen
                    Gdx.app.log("SplashScreen", "Checking if profiles exist...");
                    boolean hasProfiles = game.getProfileManager().hasProfiles();
                    Gdx.app.log("SplashScreen", "Has profiles: " + hasProfiles);
                    
                    if (hasProfiles) {
                        Gdx.app.log("SplashScreen", "Creating ProfileSelectionScreen...");
                        game.setScreen(new ProfileSelectionScreen(game));
                        Gdx.app.log("SplashScreen", "ProfileSelectionScreen created and set");
                    } else {
                        Gdx.app.log("SplashScreen", "Creating ProfileCreationScreen...");
                        game.setScreen(new ProfileCreationScreen(game));
                        Gdx.app.log("SplashScreen", "ProfileCreationScreen created and set");
                    }
                } catch (Exception e) {
                    Gdx.app.error("SplashScreen", "Error handling Play button click: " + e.getMessage(), e);
                    // Re-enable rendering if screen transition failed
                    initialized = true;
                }
            } else if (quitButton.contains(mouseX, mouseY)) {
                // Show "Are you sure?" before exiting
                Gdx.app.log("SplashScreen", "Quit button clicked – asking confirmation");
                quitConfirming = true;
            }
        }
    }
    
    @Override
    public void resize(int width, int height) {
        // Recalculate button positions when window is resized
        int centerX = width / 2;
        int centerY = height / 2;
        
        // Use the same positions as in show() method - generous spacing to avoid text overlap
        playButton.setPosition(centerX - BUTTON_WIDTH / 2, centerY - 200);
        quitButton.setPosition(centerX - BUTTON_WIDTH / 2, centerY - 280);
    }
    
    @Override
    public void pause() {
    }
    
    @Override
    public void resume() {
    }
    
    @Override
    public void hide() {
        Gdx.app.log("SplashScreen", "hide() called");
    }
    
    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        // Fonts are managed by FontManager, don't dispose them here
        // FontManager will dispose all fonts when the game exits
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (logo != null) {
            logo.dispose();
        }
    }
}

package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

public class ProfileLoadSummaryScreen implements Screen {
    private Main game;
    private SpriteBatch batch;
    private BitmapFont titleFont;
    private BitmapFont subtitleFont;
    private BitmapFont bodyFont;
    private ShapeRenderer shapeRenderer;
    private GlyphLayout glyphLayout;
    private boolean initialized = false;
    private FontManager fontManager;
    
    private Profile profile;
    
    // UI Elements
    private Rectangle continueButton;
    private Rectangle backButton;
    
    // Button dimensions
    private static final int BUTTON_WIDTH = 300;
    private static final int BUTTON_HEIGHT = 80;
    
    private Color buttonColor = new Color(0.3f, 0.3f, 0.4f, 1f);
    private Color buttonHoverColor = new Color(0.4f, 0.4f, 0.5f, 1f);
    private Color continueButtonColor = new Color(0.2f, 0.5f, 0.3f, 1f);
    private Color continueButtonHoverColor = new Color(0.3f, 0.6f, 0.4f, 1f);
    
    public ProfileLoadSummaryScreen(Main game, Profile profile) {
        Gdx.app.log("ProfileLoadSummaryScreen", "Constructor called");
        this.game = game;
        this.profile = profile;
        Gdx.app.log("ProfileLoadSummaryScreen", "Constructor completed");
    }
    
    @Override
    public void show() {
        Gdx.app.log("ProfileLoadSummaryScreen", "show() called");
        try {
            this.batch = new SpriteBatch();
            this.shapeRenderer = new ShapeRenderer();
            this.glyphLayout = new GlyphLayout();
            
            // Get fonts from FontManager
            this.fontManager = game.getFontManager();
            this.titleFont = fontManager.getTitleFont();
            this.subtitleFont = fontManager.getSubtitleFont();
            this.bodyFont = fontManager.getBodyFont();
            
            // Create UI buttons
            int centerX = Gdx.graphics.getWidth() / 2;
            
            continueButton = new Rectangle(centerX - BUTTON_WIDTH - 10, 100, BUTTON_WIDTH, BUTTON_HEIGHT);
            backButton = new Rectangle(centerX + 10, 100, BUTTON_WIDTH, BUTTON_HEIGHT);
            
            Gdx.app.log("ProfileLoadSummaryScreen", "Initialization complete");
            initialized = true;
        } catch (Exception e) {
            Gdx.app.error("ProfileLoadSummaryScreen", "Error in show(): " + e.getMessage(), e);
        }
    }
    
    @Override
    public void render(float delta) {
        if (!initialized) {
            ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
            return;
        }
        
        handleInput();
        
        if (!initialized) {
            ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
            return;
        }
        
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
        
        // Draw summary
        drawSummary();
        
        // Draw buttons
        drawButtons();
    }
    
    private void drawSummary() {
        batch.begin();
        
        int centerX = Gdx.graphics.getWidth() / 2;
        float currentY = Gdx.graphics.getHeight() - 100;
        
        // Title
        String titleText = "Profile Summary";
        glyphLayout.setText(titleFont, titleText);
        float titleX = (Gdx.graphics.getWidth() - glyphLayout.width) / 2;
        titleFont.draw(batch, titleText, titleX, currentY);
        
        currentY -= 120;
        
        // Character Name
        String nameLabel = "Character:";
        glyphLayout.setText(subtitleFont, nameLabel);
        subtitleFont.setColor(Color.YELLOW);
        subtitleFont.draw(batch, nameLabel, centerX - 300, currentY);
        subtitleFont.setColor(Color.WHITE);
        bodyFont.draw(batch, profile.getCharacterName(), centerX - 100, currentY);
        
        currentY -= 80;
        
        // Gender
        String genderLabel = "Gender:";
        glyphLayout.setText(subtitleFont, genderLabel);
        subtitleFont.setColor(Color.YELLOW);
        subtitleFont.draw(batch, genderLabel, centerX - 300, currentY);
        subtitleFont.setColor(Color.WHITE);
        bodyFont.draw(batch, profile.getGender(), centerX - 100, currentY);
        
        currentY -= 80;
        
        // Difficulty
        String difficultyLabel = "Difficulty:";
        glyphLayout.setText(subtitleFont, difficultyLabel);
        subtitleFont.setColor(Color.YELLOW);
        subtitleFont.draw(batch, difficultyLabel, centerX - 300, currentY);
        subtitleFont.setColor(Color.WHITE);
        bodyFont.draw(batch, profile.getDifficulty(), centerX - 100, currentY);
        
        currentY -= 80;
        
        // Game Date
        String dateLabel = "Year:";
        glyphLayout.setText(subtitleFont, dateLabel);
        subtitleFont.setColor(Color.YELLOW);
        subtitleFont.draw(batch, dateLabel, centerX - 300, currentY);
        subtitleFont.setColor(Color.WHITE);
        bodyFont.draw(batch, String.valueOf(profile.getGameDate()), centerX - 100, currentY);
        
        currentY -= 80;
        
        // Seed (just show that it exists, not the full value)
        String seedLabel = "Seed:";
        glyphLayout.setText(subtitleFont, seedLabel);
        subtitleFont.setColor(Color.YELLOW);
        subtitleFont.draw(batch, seedLabel, centerX - 300, currentY);
        subtitleFont.setColor(Color.WHITE);
        bodyFont.draw(batch, String.format("%d", profile.getRandSeed()), centerX - 100, currentY);
        
        currentY -= 100;
        
        // Attributes summary
        String attrLabel = "Attributes:";
        glyphLayout.setText(subtitleFont, attrLabel);
        subtitleFont.setColor(Color.YELLOW);
        subtitleFont.draw(batch, attrLabel, centerX - 300, currentY);
        subtitleFont.setColor(Color.WHITE);
        
        currentY -= 60;
        
        // Show a few key attributes
        int attrCount = 0;
        for (CharacterAttribute attr : CharacterAttribute.values()) {
            if (attrCount >= 5) break; // Only show first 5 attributes
            int value = profile.getAttribute(attr.name());
            if (value > 0) {
                bodyFont.draw(batch, attr.getDisplayName() + ": " + value, centerX - 280, currentY);
                currentY -= 50;
                attrCount++;
            }
        }
        
        batch.end();
    }
    
    private void drawButtons() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        
        // Continue button (green)
        float spacingLR = 4;
        float spacingB = 2;
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (continueButton.contains(mouseX, mouseY)) {
            shapeRenderer.setColor(continueButtonHoverColor);
        } else {
            shapeRenderer.setColor(continueButtonColor);
        }
        shapeRenderer.rect(continueButton.x + spacingLR, continueButton.y + spacingB, 
                          continueButton.width - (spacingLR * 2), continueButton.height - spacingB);
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(continueButton.x + spacingLR, continueButton.y + spacingB, 
                          continueButton.width - (spacingLR * 2), continueButton.height - spacingB);
        shapeRenderer.end();
        
        batch.begin();
        String continueText = "Continue";
        glyphLayout.setText(subtitleFont, continueText);
        float continueX = continueButton.x + spacingLR + ((continueButton.width - (spacingLR * 2)) - glyphLayout.width) / 2;
        float continueY = continueButton.y + spacingB + ((continueButton.height - spacingB) + glyphLayout.height) / 2;
        subtitleFont.draw(batch, continueText, continueX, continueY);
        batch.end();
        
        // Back button (normal)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (backButton.contains(mouseX, mouseY)) {
            shapeRenderer.setColor(buttonHoverColor);
        } else {
            shapeRenderer.setColor(buttonColor);
        }
        shapeRenderer.rect(backButton.x + spacingLR, backButton.y + spacingB, 
                          backButton.width - (spacingLR * 2), backButton.height - spacingB);
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(backButton.x + spacingLR, backButton.y + spacingB, 
                          backButton.width - (spacingLR * 2), backButton.height - spacingB);
        shapeRenderer.end();
        
        batch.begin();
        String backText = "Back";
        glyphLayout.setText(subtitleFont, backText);
        float backX = backButton.x + spacingLR + ((backButton.width - (spacingLR * 2)) - glyphLayout.width) / 2;
        float backY = backButton.y + spacingB + ((backButton.height - spacingB) + glyphLayout.height) / 2;
        subtitleFont.draw(batch, backText, backX, backY);
        batch.end();
    }
    
    private void handleInput() {
        if (Gdx.input.justTouched()) {
            int mouseX = Gdx.input.getX();
            int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
            
            // Check Continue button
            if (continueButton.contains(mouseX, mouseY)) {
                initialized = false;
                game.setScreen(new MainScreen(game));
            }
            
            // Check Back button
            if (backButton.contains(mouseX, mouseY)) {
                initialized = false;
                game.setScreen(new ProfileSelectionScreen(game));
            }
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
    }
    
    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }
        initialized = false;
    }
}

package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

public class ProfileCreationScreen implements Screen {
    private Main game;
    private SpriteBatch batch;
    private BitmapFont font;          // For input text
    private BitmapFont labelFont;     // For labels like "Character Name:"
    private BitmapFont buttonFont;    // For button text
    private ShapeRenderer shapeRenderer;
    private GlyphLayout glyphLayout;
    private boolean initialized = false;
    private FontManager fontManager;
    
    // Input fields
    private StringBuilder characterNameInput;
    private int selectedGender; // 0 = Male, 1 = Female
    private int selectedDifficulty; // 0 = Easy, 1 = Normal, 2 = Hard
    
    private boolean cursorVisible;
    private float cursorTimer;
    private static final float CURSOR_BLINK_TIME = 0.5f;
    private static final int MAX_INPUT_LENGTH = 20;
    private static final int MIN_INPUT_LENGTH = 2;
    
    // Button dimensions
    private Rectangle createButton;
    private Rectangle cancelButton;
    private Rectangle genderMaleButton;
    private Rectangle genderFemaleButton;
    private Rectangle diffEasyButton;
    private Rectangle diffNormalButton;
    private Rectangle diffHardButton;
    private static final int BUTTON_WIDTH = 300;  // Increased from 150 for large fonts
    private static final int BUTTON_HEIGHT = 80;  // Increased from 50 for large fonts
    private static final int SMALL_BUTTON_WIDTH = 250;  // Increased from 100 for large fonts
    
    private Color buttonColor = new Color(0.3f, 0.3f, 0.4f, 1f);
    private Color buttonHoverColor = new Color(0.4f, 0.4f, 0.5f, 1f);
    private Color selectedButtonColor = new Color(0.5f, 0.6f, 0.7f, 1f);
    
    public ProfileCreationScreen(Main game) {
        Gdx.app.log("ProfileCreationScreen", "Constructor called");
        this.game = game;
        this.characterNameInput = new StringBuilder();
        this.selectedGender = 0;
        this.selectedDifficulty = 1; // Default to Normal
        this.cursorVisible = true;
        this.cursorTimer = 0;
        Gdx.app.log("ProfileCreationScreen", "Constructor completed successfully");
    }
    
    @Override
    public void show() {
        Gdx.app.log("ProfileCreationScreen", "show() called");
        try {
            Gdx.app.log("ProfileCreationScreen", "Creating SpriteBatch...");
            this.batch = new SpriteBatch();
            
            Gdx.app.log("ProfileCreationScreen", "Creating ShapeRenderer...");
            this.shapeRenderer = new ShapeRenderer();
            
            Gdx.app.log("ProfileCreationScreen", "Creating GlyphLayout...");
            this.glyphLayout = new GlyphLayout();
            
            Gdx.app.log("ProfileCreationScreen", "Creating fonts...");
            // Get FontManager from Main game
            this.fontManager = game.getFontManager();
            this.font = fontManager.getSubtitleFont();           // 24dp for input text
            this.labelFont = fontManager.getTitleFont();          // 32dp for labels - larger and more prominent
            this.buttonFont = fontManager.getSubtitleFont();      // 24dp for button text
            
            Gdx.app.log("ProfileCreationScreen", "Getting screen dimensions...");
            int centerX = Gdx.graphics.getWidth() / 2;
            int centerY = Gdx.graphics.getHeight() / 2;
            Gdx.app.log("ProfileCreationScreen", "Screen: " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight());
            Gdx.app.log("ProfileCreationScreen", "Center: (" + centerX + ", " + centerY + ")");
            
            Gdx.app.log("ProfileCreationScreen", "Creating buttons...");
            createButton = new Rectangle(centerX - BUTTON_WIDTH - 10, 50, BUTTON_WIDTH, BUTTON_HEIGHT);
            cancelButton = new Rectangle(centerX + 10, 50, BUTTON_WIDTH, BUTTON_HEIGHT);
            
            // Calculate button positions relative to labels for proper layout hierarchy
            // Using the same startY as in render() for consistency
            int startY = Gdx.graphics.getHeight() - 200;
            
            // Gender buttons - positioned below "Gender:" label
            // Gender label is at startY - 500, so buttons start 150px below that
            int genderButtonY = startY - 500 - 150;  
            genderMaleButton = new Rectangle(centerX - SMALL_BUTTON_WIDTH / 2, genderButtonY, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT);
            genderFemaleButton = new Rectangle(centerX - SMALL_BUTTON_WIDTH / 2, genderButtonY - 100, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT);
            
            // Difficulty buttons - positioned below "Difficulty:" label
            // Difficulty label is at startY - 820, so buttons start 150px below that
            int diffButtonY = startY - 820 - 150;
            diffEasyButton = new Rectangle(centerX - SMALL_BUTTON_WIDTH / 2, diffButtonY, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT);
            diffNormalButton = new Rectangle(centerX - SMALL_BUTTON_WIDTH / 2, diffButtonY - 100, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT);
            diffHardButton = new Rectangle(centerX - SMALL_BUTTON_WIDTH / 2, diffButtonY - 200, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT);
            
            Gdx.app.log("ProfileCreationScreen", "Button positions - Gender Male: " + genderMaleButton);
            Gdx.app.log("ProfileCreationScreen", "Button positions - Difficulty Hard: " + diffHardButton);
            
            // Set up input processor
            Gdx.app.log("ProfileCreationScreen", "Setting up input processor...");
            Gdx.input.setInputProcessor(new InputAdapter() {
                @Override
                public boolean keyTyped(char character) {
                    if (character == '\r' || character == '\n') {
                        // Enter key - create profile
                        if (canCreateProfile()) {
                            createProfile();
                        }
                        return true;
                    } else if (character == '\b') {
                        // Backspace
                        if (characterNameInput.length() > 0) {
                            characterNameInput.deleteCharAt(characterNameInput.length() - 1);
                        }
                        return true;
                    } else if (Character.isLetterOrDigit(character) || character == ' ') {
                        if (characterNameInput.length() < MAX_INPUT_LENGTH) {
                            characterNameInput.append(character);
                        }
                        return true;
                    }
                    return false;
                }
            });
            
            initialized = true;
            Gdx.app.log("ProfileCreationScreen", "show() completed successfully - screen ready to render");
        } catch (Exception e) {
            Gdx.app.error("ProfileCreationScreen", "Error in show(): " + e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public void render(float delta) {
        // Skip rendering if not initialized yet
        if (!initialized) {
            Gdx.app.log("ProfileCreationScreen", "render() called but not initialized yet, skipping");
            ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
            return;
        }
        
        handleInput();
        
        // Check again after handleInput - might have started screen transition
        if (!initialized) {
            Gdx.app.log("ProfileCreationScreen", "Screen transition started, skipping render");
            ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
            return;
        }
        
        // Update cursor
        cursorTimer += delta;
        if (cursorTimer >= CURSOR_BLINK_TIME) {
            cursorVisible = !cursorVisible;
            cursorTimer = 0;
        }
        
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
        
        batch.begin();
        
        // Title
        BitmapFont titleFont = fontManager.getTitleFont();
        String titleText = "Create Profile";
        glyphLayout.setText(titleFont, titleText);
        titleFont.draw(batch, titleText, 
                      (Gdx.graphics.getWidth() - glyphLayout.width) / 2, 
                      Gdx.graphics.getHeight() - 50);
        
        int centerX = Gdx.graphics.getWidth() / 2;
        int startY = Gdx.graphics.getHeight() - 200;  // Start from top with more margin
        
        // Character Name field - with much more spacing for large fonts
        labelFont.draw(batch, "Character Name:", 20, startY);
        String characterText = characterNameInput.toString();
        if (cursorVisible) characterText += "|";
        font.draw(batch, characterText, 20, startY - 280);  // Increased spacing from 30 to 280 to prevent overlap
        
        // Gender label - positioned relative to gender buttons
        labelFont.draw(batch, "Gender:", 20, startY - 500);  // Increased spacing
        
        // Difficulty label - positioned relative to difficulty buttons
        labelFont.draw(batch, "Difficulty:", 20, startY - 820);  // Increased spacing
        
        batch.end();
        
        // Draw buttons
        drawButtons();
    }
    
    private void drawButtons() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        
        // Gender buttons
        drawButton(genderMaleButton, "Male", mouseX, mouseY, selectedGender == 0);
        drawButton(genderFemaleButton, "Female", mouseX, mouseY, selectedGender == 1);
        
        // Difficulty buttons
        drawButton(diffEasyButton, "Easy", mouseX, mouseY, selectedDifficulty == 0);
        drawButton(diffNormalButton, "Normal", mouseX, mouseY, selectedDifficulty == 1);
        drawButton(diffHardButton, "Hard", mouseX, mouseY, selectedDifficulty == 2);
        
        // Action buttons
        drawButton(createButton, "Create", mouseX, mouseY, false);
        drawButton(cancelButton, "Cancel", mouseX, mouseY, false);
    }
    
    private void drawButton(Rectangle button, String text, int mouseX, int mouseY, boolean selected) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (selected) {
            shapeRenderer.setColor(selectedButtonColor);
        } else if (button.contains(mouseX, mouseY)) {
            shapeRenderer.setColor(buttonHoverColor);
        } else {
            shapeRenderer.setColor(buttonColor);
        }
        shapeRenderer.rect(button.x, button.y, button.width, button.height);
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(button.x, button.y, button.width, button.height);
        shapeRenderer.end();
        
        batch.begin();
        glyphLayout.setText(buttonFont, text);
        float textX = button.x + (button.width - glyphLayout.width) / 2;
        float textY = button.y + (button.height + glyphLayout.height) / 2;
        buttonFont.draw(batch, text, textX, textY);
        batch.end();
    }
    
    private void handleInput() {
        if (Gdx.input.justTouched()) {
            int mouseX = Gdx.input.getX();
            int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
            
            if (genderMaleButton.contains(mouseX, mouseY)) {
                selectedGender = 0;
            } else if (genderFemaleButton.contains(mouseX, mouseY)) {
                selectedGender = 1;
            } else if (diffEasyButton.contains(mouseX, mouseY)) {
                selectedDifficulty = 0;
            } else if (diffNormalButton.contains(mouseX, mouseY)) {
                selectedDifficulty = 1;
            } else if (diffHardButton.contains(mouseX, mouseY)) {
                selectedDifficulty = 2;
            } else if (createButton.contains(mouseX, mouseY)) {
                if (canCreateProfile()) {
                    createProfile();
                }
            } else if (cancelButton.contains(mouseX, mouseY)) {
                // Stop rendering before transition
                initialized = false;
                // Return to profile selection if profiles exist, otherwise splash screen
                if (game.getProfileManager().hasProfiles()) {
                    game.setScreen(new ProfileSelectionScreen(game));
                } else {
                    game.setScreen(new SplashScreen(game));
                }
            }
        }
    }
    
    private boolean canCreateProfile() {
        String characterName = characterNameInput.toString().trim();
        return characterName.length() >= MIN_INPUT_LENGTH;
    }
    
    private void createProfile() {
        String characterName = characterNameInput.toString().trim();
        String gender = selectedGender == 0 ? "Male" : "Female";
        String difficulty = selectedDifficulty == 0 ? "Easy" : 
                          (selectedDifficulty == 1 ? "Normal" : "Hard");
        
        try {
            Profile profile = game.getProfileManager().createProfile(characterName, gender, difficulty);
            game.getProfileManager().selectProfile(profile);
            // Stop rendering before transition
            initialized = false;
            game.setScreen(new MainScreen(game));
        } catch (Exception e) {
            Gdx.app.error("ProfileCreation", "Error creating profile: " + e.getMessage());
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
        Gdx.input.setInputProcessor(null);
    }
    
    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        // Fonts are managed by FontManager, don't dispose them here
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}

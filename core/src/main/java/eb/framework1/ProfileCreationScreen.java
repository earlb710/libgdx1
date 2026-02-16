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
    private BitmapFont font;
    private BitmapFont labelFont;
    private ShapeRenderer shapeRenderer;
    private GlyphLayout glyphLayout;
    
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
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 50;
    private static final int SMALL_BUTTON_WIDTH = 100;
    
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
            this.font = new BitmapFont();
            this.font.setColor(Color.WHITE);
            this.font.getData().setScale(1.5f);
            
            this.labelFont = new BitmapFont();
            this.labelFont.setColor(Color.GOLD);
            this.labelFont.getData().setScale(2.0f);
            
            Gdx.app.log("ProfileCreationScreen", "Getting screen dimensions...");
            int centerX = Gdx.graphics.getWidth() / 2;
            int centerY = Gdx.graphics.getHeight() / 2;
            Gdx.app.log("ProfileCreationScreen", "Screen: " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight());
            Gdx.app.log("ProfileCreationScreen", "Center: (" + centerX + ", " + centerY + ")");
            
            Gdx.app.log("ProfileCreationScreen", "Creating buttons...");
            createButton = new Rectangle(centerX - BUTTON_WIDTH - 10, 50, BUTTON_WIDTH, BUTTON_HEIGHT);
            cancelButton = new Rectangle(centerX + 10, 50, BUTTON_WIDTH, BUTTON_HEIGHT);
            
            // Gender buttons - positioned to fit in portrait mode (480 width)
            int genderY = centerY + 10;
            int genderStartX = centerX - SMALL_BUTTON_WIDTH - 10;
            genderMaleButton = new Rectangle(genderStartX, genderY, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT);
            genderFemaleButton = new Rectangle(centerX + 10, genderY, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT);
            
            // Difficulty buttons - stacked vertically to fit in portrait mode
            int diffY = centerY - 60;
            diffEasyButton = new Rectangle(centerX - SMALL_BUTTON_WIDTH / 2, diffY, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT);
            diffNormalButton = new Rectangle(centerX - SMALL_BUTTON_WIDTH / 2, diffY - 60, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT);
            diffHardButton = new Rectangle(centerX - SMALL_BUTTON_WIDTH / 2, diffY - 120, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT);
            
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
            
            Gdx.app.log("ProfileCreationScreen", "show() completed successfully");
        } catch (Exception e) {
            Gdx.app.error("ProfileCreationScreen", "Error in show(): " + e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public void render(float delta) {
        handleInput();
        
        // Update cursor
        cursorTimer += delta;
        if (cursorTimer >= CURSOR_BLINK_TIME) {
            cursorVisible = !cursorVisible;
            cursorTimer = 0;
        }
        
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
        
        batch.begin();
        
        // Title
        labelFont.getData().setScale(1.5f);
        String titleText = "Create Profile";
        glyphLayout.setText(labelFont, titleText);
        labelFont.draw(batch, titleText, 
                      (Gdx.graphics.getWidth() - glyphLayout.width) / 2, 
                      Gdx.graphics.getHeight() - 50);
        labelFont.getData().setScale(2.0f);
        
        int centerX = Gdx.graphics.getWidth() / 2;
        int startY = Gdx.graphics.getHeight() / 2 + 180;
        
        // Character Name field
        font.getData().setScale(1.0f);
        font.draw(batch, "Character Name:", 20, startY);
        font.getData().setScale(1.2f);
        String characterText = characterNameInput.toString();
        if (cursorVisible) characterText += "|";
        font.draw(batch, characterText, 20, startY - 30);
        
        // Gender label
        font.getData().setScale(1.0f);
        font.draw(batch, "Gender:", 20, startY - 100);
        
        // Difficulty label
        font.draw(batch, "Difficulty:", 20, startY - 250);
        
        batch.end();
        
        // Draw buttons
        drawButtons();
        
        batch.begin();
        font.getData().setScale(1.5f);
        batch.end();
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
        font.getData().setScale(1.2f);
        glyphLayout.setText(font, text);
        float textX = button.x + (button.width - glyphLayout.width) / 2;
        float textY = button.y + (button.height + glyphLayout.height) / 2;
        font.draw(batch, text, textX, textY);
        font.getData().setScale(1.5f);
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
        if (font != null) font.dispose();
        if (labelFont != null) labelFont.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}

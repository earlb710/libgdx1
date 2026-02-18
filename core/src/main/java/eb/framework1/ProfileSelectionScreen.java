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
import java.util.ArrayList;
import java.util.List;

public class ProfileSelectionScreen implements Screen {
    private Main game;
    private SpriteBatch batch;
    private BitmapFont font;          // For regular text
    private BitmapFont titleFont;     // For titles
    private BitmapFont buttonFont;    // For button text
    private ShapeRenderer shapeRenderer;
    private GlyphLayout glyphLayout;
    private boolean initialized = false;
    private FontManager fontManager;
    
    private List<Profile> profiles;
    private List<Rectangle> profileButtons;
    private List<Rectangle> deleteButtons;
    private Rectangle newProfileButton;
    private Rectangle backButton;
    
    // Confirmation dialog state
    private boolean showingConfirmDialog = false;
    private Profile profileToDelete = null;
    private Rectangle confirmYesButton;
    private Rectangle confirmNoButton;
    
    private static final int BUTTON_WIDTH = 600;  // Increased for better readability
    private static final int BUTTON_HEIGHT = 150; // Increased for more comfortable spacing
    private static final int DELETE_BUTTON_SIZE = 80; // Square delete button
    private static final int BUTTON_SPACING = 25;
    
    private Color buttonColor = new Color(0.3f, 0.3f, 0.4f, 1f);
    private Color buttonHoverColor = new Color(0.4f, 0.4f, 0.5f, 1f);
    private Color newButtonColor = new Color(0.2f, 0.5f, 0.3f, 1f);
    private Color newButtonHoverColor = new Color(0.3f, 0.6f, 0.4f, 1f);
    private Color deleteButtonColor = new Color(0.8f, 0.2f, 0.2f, 1f);
    private Color deleteButtonHoverColor = new Color(0.9f, 0.3f, 0.3f, 1f);
    
    public ProfileSelectionScreen(Main game) {
        Gdx.app.log("ProfileSelectionScreen", "Constructor called");
        this.game = game;
        Gdx.app.log("ProfileSelectionScreen", "Constructor completed successfully");
    }
    
    @Override
    public void show() {
        Gdx.app.log("ProfileSelectionScreen", "show() called");
        try {
            Gdx.app.log("ProfileSelectionScreen", "Creating SpriteBatch...");
            this.batch = new SpriteBatch();
            
            Gdx.app.log("ProfileSelectionScreen", "Creating ShapeRenderer...");
            this.shapeRenderer = new ShapeRenderer();
            
            Gdx.app.log("ProfileSelectionScreen", "Creating GlyphLayout...");
            this.glyphLayout = new GlyphLayout();
            
            Gdx.app.log("ProfileSelectionScreen", "Creating fonts...");
            // Get FontManager from Main game
            this.fontManager = game.getFontManager();
            this.font = fontManager.getBodyFont();           // 18dp for regular text
            this.titleFont = fontManager.getTitleFont();      // 32dp for titles
            this.buttonFont = fontManager.getSubtitleFont();  // 24dp for button text
            
            Gdx.app.log("ProfileSelectionScreen", "Loading profiles...");
            loadProfiles();
            
            Gdx.app.log("ProfileSelectionScreen", "Creating buttons...");
            createButtons();
            
            initialized = true;
            Gdx.app.log("ProfileSelectionScreen", "show() completed successfully - screen ready to render");
        } catch (Exception e) {
            Gdx.app.error("ProfileSelectionScreen", "Error in show(): " + e.getMessage(), e);
            throw e;
        }
    }
    
    private void loadProfiles() {
        Gdx.app.log("ProfileSelectionScreen", "loadProfiles() called");
        profiles = game.getProfileManager().getProfiles();
        Gdx.app.log("ProfileSelectionScreen", "Loaded " + profiles.size() + " profiles");
    }
    
    private void createButtons() {
        Gdx.app.log("ProfileSelectionScreen", "createButtons() called");
        profileButtons = new ArrayList<>();
        deleteButtons = new ArrayList<>();
        
        int centerX = Gdx.graphics.getWidth() / 2;
        int startY = Gdx.graphics.getHeight() / 2 + 100;
        Gdx.app.log("ProfileSelectionScreen", "Screen: " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight());
        
        for (int i = 0; i < profiles.size(); i++) {
            int y = startY - (i * (BUTTON_HEIGHT + BUTTON_SPACING));
            
            // Profile button (with space for delete button on the right)
            Rectangle button = new Rectangle(
                centerX - (BUTTON_WIDTH + DELETE_BUTTON_SIZE + 10) / 2,
                y,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
            );
            profileButtons.add(button);
            
            // Delete button (positioned to the right of profile button)
            Rectangle deleteBtn = new Rectangle(
                button.x + button.width + 10,
                y + (BUTTON_HEIGHT - DELETE_BUTTON_SIZE) / 2, // Center vertically
                DELETE_BUTTON_SIZE,
                DELETE_BUTTON_SIZE
            );
            deleteButtons.add(deleteBtn);
        }
        
        // New profile button
        int newButtonY = startY - (profiles.size() * (BUTTON_HEIGHT + BUTTON_SPACING)) - 40;
        newProfileButton = new Rectangle(
            centerX - BUTTON_WIDTH / 2,
            newButtonY,
            BUTTON_WIDTH,
            BUTTON_HEIGHT
        );
        
        // Back button
        backButton = new Rectangle(50, 50, 150, 50);
        
        // Confirmation dialog buttons
        confirmYesButton = new Rectangle(
            centerX - 220,
            Gdx.graphics.getHeight() / 2 - 60,
            200,
            80
        );
        confirmNoButton = new Rectangle(
            centerX + 20,
            Gdx.graphics.getHeight() / 2 - 60,
            200,
            80
        );
    }
    
    @Override
    public void render(float delta) {
        // Skip rendering if not initialized yet
        if (!initialized) {
            Gdx.app.log("ProfileSelectionScreen", "render() called but not initialized yet, skipping");
            ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
            return;
        }
        
        handleInput();
        
        // Check again after handleInput - might have started screen transition
        if (!initialized) {
            Gdx.app.log("ProfileSelectionScreen", "Screen transition started, skipping render");
            ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
            return;
        }
        
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
        
        batch.begin();
        
        // Title
        String title = "Select Profile";
        glyphLayout.setText(titleFont, title);
        float titleX = (Gdx.graphics.getWidth() - glyphLayout.width) / 2;
        titleFont.draw(batch, title, titleX, Gdx.graphics.getHeight() - 50);
        
        batch.end();
        
        // Draw buttons
        drawButtons();
        
        // Draw confirmation dialog if showing
        if (showingConfirmDialog) {
            drawConfirmationDialog();
        }
    }
    
    private void drawConfirmationDialog() {
        // Draw semi-transparent overlay
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.7f);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        
        // Draw dialog box
        int dialogWidth = 500;
        int dialogHeight = 250;
        int dialogX = (Gdx.graphics.getWidth() - dialogWidth) / 2;
        int dialogY = (Gdx.graphics.getHeight() - dialogHeight) / 2;
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 1f);
        shapeRenderer.rect(dialogX, dialogY, dialogWidth, dialogHeight);
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(dialogX, dialogY, dialogWidth, dialogHeight);
        shapeRenderer.end();
        
        // Draw text
        batch.begin();
        String message = "Delete Profile?";
        glyphLayout.setText(titleFont, message);
        float messageX = (Gdx.graphics.getWidth() - glyphLayout.width) / 2;
        titleFont.draw(batch, message, messageX, dialogY + dialogHeight - 40);
        
        if (profileToDelete != null) {
            String profileName = profileToDelete.getName();
            glyphLayout.setText(buttonFont, profileName);
            float nameX = (Gdx.graphics.getWidth() - glyphLayout.width) / 2;
            buttonFont.draw(batch, profileName, nameX, dialogY + dialogHeight - 100);
        }
        batch.end();
        
        // Draw Yes/No buttons
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        
        // Yes button (red)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (confirmYesButton.contains(mouseX, mouseY)) {
            shapeRenderer.setColor(deleteButtonHoverColor);
        } else {
            shapeRenderer.setColor(deleteButtonColor);
        }
        shapeRenderer.rect(confirmYesButton.x, confirmYesButton.y, confirmYesButton.width, confirmYesButton.height);
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(confirmYesButton.x, confirmYesButton.y, confirmYesButton.width, confirmYesButton.height);
        shapeRenderer.end();
        
        batch.begin();
        String yesText = "Yes, Delete";
        glyphLayout.setText(buttonFont, yesText);
        float yesX = confirmYesButton.x + (confirmYesButton.width - glyphLayout.width) / 2;
        float yesY = confirmYesButton.y + (confirmYesButton.height + glyphLayout.height) / 2;
        buttonFont.draw(batch, yesText, yesX, yesY);
        batch.end();
        
        // No button (normal)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (confirmNoButton.contains(mouseX, mouseY)) {
            shapeRenderer.setColor(buttonHoverColor);
        } else {
            shapeRenderer.setColor(buttonColor);
        }
        shapeRenderer.rect(confirmNoButton.x, confirmNoButton.y, confirmNoButton.width, confirmNoButton.height);
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(confirmNoButton.x, confirmNoButton.y, confirmNoButton.width, confirmNoButton.height);
        shapeRenderer.end();
        
        batch.begin();
        String noText = "Cancel";
        glyphLayout.setText(buttonFont, noText);
        float noX = confirmNoButton.x + (confirmNoButton.width - glyphLayout.width) / 2;
        float noY = confirmNoButton.y + (confirmNoButton.height + glyphLayout.height) / 2;
        buttonFont.draw(batch, noText, noX, noY);
        batch.end();
    }
    
    private void drawButtons() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        
        // Draw profile buttons
        for (int i = 0; i < profiles.size(); i++) {
            Profile profile = profiles.get(i);
            Rectangle button = profileButtons.get(i);
            Rectangle deleteBtn = deleteButtons.get(i);
            
            // Draw button background
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
            
            // Draw profile info
            batch.begin();
            BitmapFont profileNameFont = fontManager.getSubtitleFont();
            BitmapFont profileDetailFont = fontManager.getSmallFont();
            
            // Center text vertically with proper spacing for two lines
            glyphLayout.setText(profileNameFont, profile.getName());
            float nameHeight = glyphLayout.height;
            glyphLayout.setText(profileDetailFont, "X");  // Measure typical character height
            float detailHeight = glyphLayout.height;
            
            float totalTextHeight = nameHeight + detailHeight + 10; // 10px spacing between lines
            float startY = button.y + (button.height + totalTextHeight) / 2;
            
            // Draw profile name (top line)
            profileNameFont.draw(batch, profile.getName(), button.x + 20, startY);
            
            // Draw profile details (bottom line)
            profileDetailFont.draw(batch, 
                     profile.getCharacterName() + " (" + profile.getGender() + ") - " + profile.getDifficulty(),
                     button.x + 20, startY - nameHeight - 10);
            batch.end();
            
            // Draw delete button
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            if (deleteBtn.contains(mouseX, mouseY)) {
                shapeRenderer.setColor(deleteButtonHoverColor);
            } else {
                shapeRenderer.setColor(deleteButtonColor);
            }
            shapeRenderer.rect(deleteBtn.x, deleteBtn.y, deleteBtn.width, deleteBtn.height);
            shapeRenderer.end();
            
            // Draw delete button border
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.rect(deleteBtn.x, deleteBtn.y, deleteBtn.width, deleteBtn.height);
            shapeRenderer.end();
            
            // Draw minus symbol
            batch.begin();
            String minusSymbol = "-";
            glyphLayout.setText(titleFont, minusSymbol);
            float minusX = deleteBtn.x + (deleteBtn.width - glyphLayout.width) / 2;
            float minusY = deleteBtn.y + (deleteBtn.height + glyphLayout.height) / 2;
            titleFont.draw(batch, minusSymbol, minusX, minusY);
            batch.end();
        }
        
        // Draw new profile button
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (newProfileButton.contains(mouseX, mouseY)) {
            shapeRenderer.setColor(newButtonHoverColor);
        } else {
            shapeRenderer.setColor(newButtonColor);
        }
        shapeRenderer.rect(newProfileButton.x, newProfileButton.y, 
                          newProfileButton.width, newProfileButton.height);
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(newProfileButton.x, newProfileButton.y, 
                          newProfileButton.width, newProfileButton.height);
        shapeRenderer.end();
        
        batch.begin();
        String newProfileText = "+ Create New Profile";
        glyphLayout.setText(buttonFont, newProfileText);
        float textX = newProfileButton.x + (newProfileButton.width - glyphLayout.width) / 2;
        float textY = newProfileButton.y + (newProfileButton.height + glyphLayout.height) / 2;
        buttonFont.draw(batch, newProfileText, textX, textY);
        batch.end();
        
        // Draw back button
        drawButton(backButton, "Back", mouseX, mouseY);
    }
    
    private void drawButton(Rectangle button, String text, int mouseX, int mouseY) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (button.contains(mouseX, mouseY)) {
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
            
            // If showing confirmation dialog, handle dialog buttons only
            if (showingConfirmDialog) {
                if (confirmYesButton.contains(mouseX, mouseY)) {
                    // Delete the profile
                    if (profileToDelete != null) {
                        game.getProfileManager().deleteProfile(profileToDelete);
                        // Reload profiles and recreate buttons
                        loadProfiles();
                        createButtons();
                    }
                    showingConfirmDialog = false;
                    profileToDelete = null;
                    return;
                } else if (confirmNoButton.contains(mouseX, mouseY)) {
                    // Cancel deletion
                    showingConfirmDialog = false;
                    profileToDelete = null;
                    return;
                }
                return; // Don't process other clicks while dialog is showing
            }
            
            // Check delete button clicks first (they're on top)
            for (int i = 0; i < deleteButtons.size(); i++) {
                if (deleteButtons.get(i).contains(mouseX, mouseY)) {
                    // Show confirmation dialog
                    profileToDelete = profiles.get(i);
                    showingConfirmDialog = true;
                    return;
                }
            }
            
            // Check profile button clicks
            for (int i = 0; i < profileButtons.size(); i++) {
                if (profileButtons.get(i).contains(mouseX, mouseY)) {
                    Profile selectedProfile = profiles.get(i);
                    game.getProfileManager().selectProfile(selectedProfile);
                    // Stop rendering before transition
                    initialized = false;
                    // Go to summary screen instead of directly to main screen
                    game.setScreen(new ProfileLoadSummaryScreen(game, selectedProfile));
                    return;
                }
            }
            
            // Check new profile button
            if (newProfileButton.contains(mouseX, mouseY)) {
                // Stop rendering before transition
                initialized = false;
                game.setScreen(new ProfileCreationScreen(game));
                return;
            }
            
            // Check back button
            if (backButton.contains(mouseX, mouseY)) {
                // Stop rendering before transition
                initialized = false;
                game.setScreen(new SplashScreen(game));
                return;
            }
        }
    }
    
    @Override
    public void resize(int width, int height) {
        createButtons();
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
        if (batch != null) batch.dispose();
        // Fonts are managed by FontManager, don't dispose them here
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}

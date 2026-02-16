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
    private BitmapFont font;
    private BitmapFont titleFont;
    private ShapeRenderer shapeRenderer;
    private GlyphLayout glyphLayout;
    
    private List<Profile> profiles;
    private List<Rectangle> profileButtons;
    private Rectangle newProfileButton;
    private Rectangle backButton;
    
    private static final int BUTTON_WIDTH = 380;  // Reduced to fit portrait mode better
    private static final int BUTTON_HEIGHT = 60;
    private static final int BUTTON_SPACING = 20;
    
    private Color buttonColor = new Color(0.3f, 0.3f, 0.4f, 1f);
    private Color buttonHoverColor = new Color(0.4f, 0.4f, 0.5f, 1f);
    private Color newButtonColor = new Color(0.2f, 0.5f, 0.3f, 1f);
    private Color newButtonHoverColor = new Color(0.3f, 0.6f, 0.4f, 1f);
    
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
            this.font = new BitmapFont();
            this.font.setColor(Color.WHITE);
            this.font.getData().setScale(1.5f);
            
            this.titleFont = new BitmapFont();
            this.titleFont.setColor(Color.GOLD);
            this.titleFont.getData().setScale(2.5f);
            
            Gdx.app.log("ProfileSelectionScreen", "Loading profiles...");
            loadProfiles();
            
            Gdx.app.log("ProfileSelectionScreen", "Creating buttons...");
            createButtons();
            
            Gdx.app.log("ProfileSelectionScreen", "show() completed successfully");
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
        
        int centerX = Gdx.graphics.getWidth() / 2;
        int startY = Gdx.graphics.getHeight() / 2 + 100;
        Gdx.app.log("ProfileSelectionScreen", "Screen: " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight());
        
        for (int i = 0; i < profiles.size(); i++) {
            int y = startY - (i * (BUTTON_HEIGHT + BUTTON_SPACING));
            Rectangle button = new Rectangle(
                centerX - BUTTON_WIDTH / 2,
                y,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
            );
            profileButtons.add(button);
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
    }
    
    @Override
    public void render(float delta) {
        handleInput();
        
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
    }
    
    private void drawButtons() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        
        // Draw profile buttons
        for (int i = 0; i < profiles.size(); i++) {
            Profile profile = profiles.get(i);
            Rectangle button = profileButtons.get(i);
            
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
            font.getData().setScale(1.5f);
            font.draw(batch, profile.getName(), button.x + 20, button.y + button.height - 15);
            font.getData().setScale(1.0f);
            font.draw(batch, 
                     profile.getCharacterName() + " (" + profile.getGender() + ") - " + profile.getDifficulty(),
                     button.x + 20, button.y + 20);
            font.getData().setScale(1.5f);
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
        glyphLayout.setText(font, newProfileText);
        float textX = newProfileButton.x + (newProfileButton.width - glyphLayout.width) / 2;
        float textY = newProfileButton.y + (newProfileButton.height + glyphLayout.height) / 2;
        font.draw(batch, newProfileText, textX, textY);
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
            
            // Check profile button clicks
            for (int i = 0; i < profileButtons.size(); i++) {
                if (profileButtons.get(i).contains(mouseX, mouseY)) {
                    Profile selectedProfile = profiles.get(i);
                    game.getProfileManager().selectProfile(selectedProfile);
                    game.setScreen(new MainScreen(game));
                    return;
                }
            }
            
            // Check new profile button
            if (newProfileButton.contains(mouseX, mouseY)) {
                game.setScreen(new ProfileCreationScreen(game));
                return;
            }
            
            // Check back button
            if (backButton.contains(mouseX, mouseY)) {
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
        if (font != null) font.dispose();
        if (titleFont != null) titleFont.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}

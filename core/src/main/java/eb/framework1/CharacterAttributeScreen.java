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

import java.util.HashMap;
import java.util.Map;

public class CharacterAttributeScreen implements Screen {
    private Main game;
    private SpriteBatch batch;
    private BitmapFont titleFont;
    private BitmapFont subtitleFont;
    private BitmapFont bodyFont;
    private BitmapFont smallFont;
    private ShapeRenderer shapeRenderer;
    private GlyphLayout glyphLayout;
    private boolean initialized = false;
    private FontManager fontManager;
    
    // Profile data from ProfileCreationScreen
    private String characterName;
    private String gender;
    private String difficulty;
    
    // Attribute values (default to 1, can allocate more)
    private Map<CharacterAttribute, Integer> attributeValues;
    
    // Point allocation
    // 10 distributable points above minimum (11 attributes × 1 min = 11 base points)
    // Total: 21 points (11 minimum + 10 distributable)
    private static final int TOTAL_POINTS = 21;
    private static final int MIN_ATTRIBUTE_VALUE = 1;
    private static final int MAX_ATTRIBUTE_VALUE = 10;
    private int pointsRemaining;
    
    // UI Elements
    private Rectangle confirmButton;
    private Rectangle backButton;
    private Map<CharacterAttribute, Rectangle> plusButtons;
    private Map<CharacterAttribute, Rectangle> minusButtons;
    
    // Button dimensions
    private static final int BUTTON_WIDTH = 300;
    private static final int BUTTON_HEIGHT = 80;
    private static final int SMALL_BUTTON_SIZE = 60;  // For +/- buttons
    
    private Color buttonColor = new Color(0.3f, 0.3f, 0.4f, 1f);
    private Color buttonHoverColor = new Color(0.4f, 0.4f, 0.5f, 1f);
    private Color buttonActiveColor = new Color(0.5f, 0.6f, 0.7f, 1f);
    private Color categoryColor = new Color(0.8f, 0.7f, 0.3f, 1f);  // Gold for category headers
    
    public CharacterAttributeScreen(Main game, String characterName, String gender, String difficulty) {
        Gdx.app.log("CharacterAttributeScreen", "Constructor called");
        this.game = game;
        this.characterName = characterName;
        this.gender = gender;
        this.difficulty = difficulty;
        
        // Initialize attribute values to minimum
        this.attributeValues = new HashMap<>();
        for (CharacterAttribute attr : CharacterAttribute.values()) {
            attributeValues.put(attr, MIN_ATTRIBUTE_VALUE);
        }
        
        // Calculate initial points remaining
        this.pointsRemaining = TOTAL_POINTS - (CharacterAttribute.values().length * MIN_ATTRIBUTE_VALUE);
        
        this.plusButtons = new HashMap<>();
        this.minusButtons = new HashMap<>();
        
        Gdx.app.log("CharacterAttributeScreen", "Constructor completed");
    }
    
    @Override
    public void show() {
        Gdx.app.log("CharacterAttributeScreen", "show() called");
        try {
            this.batch = new SpriteBatch();
            this.shapeRenderer = new ShapeRenderer();
            this.glyphLayout = new GlyphLayout();
            
            // Get fonts from FontManager
            this.fontManager = game.getFontManager();
            this.titleFont = fontManager.getTitleFont();        // 40dp
            this.subtitleFont = fontManager.getSubtitleFont();  // 30dp
            this.bodyFont = fontManager.getBodyFont();          // 22dp
            this.smallFont = fontManager.getSmallFont();        // 18dp
            
            // Create UI buttons
            int centerX = Gdx.graphics.getWidth() / 2;
            
            confirmButton = new Rectangle(centerX - BUTTON_WIDTH - 10, 50, BUTTON_WIDTH, BUTTON_HEIGHT);
            backButton = new Rectangle(centerX + 10, 50, BUTTON_WIDTH, BUTTON_HEIGHT);
            
            // Create +/- buttons for each attribute (positioned in render method)
            for (CharacterAttribute attr : CharacterAttribute.values()) {
                plusButtons.put(attr, new Rectangle());
                minusButtons.put(attr, new Rectangle());
            }
            
            Gdx.app.log("CharacterAttributeScreen", "Initialization complete");
            initialized = true;
        } catch (Exception e) {
            Gdx.app.error("CharacterAttributeScreen", "Error in show(): " + e.getMessage(), e);
        }
    }
    
    @Override
    public void render(float delta) {
        if (!initialized) return;
        
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
        
        handleInput();
        
        if (!initialized) return;  // Check again after handleInput
        
        // Draw UI
        drawTitle();
        drawAttributes();
        drawButtons();
    }
    
    private void drawTitle() {
        batch.begin();
        
        // Title
        String titleText = "Character Attributes";
        glyphLayout.setText(titleFont, titleText);
        float titleX = (Gdx.graphics.getWidth() - glyphLayout.width) / 2;
        float titleY = Gdx.graphics.getHeight() - 100;
        titleFont.draw(batch, titleText, titleX, titleY);
        
        // Character info
        String infoText = characterName + " (" + gender + ") - " + difficulty;
        glyphLayout.setText(bodyFont, infoText);
        float infoX = (Gdx.graphics.getWidth() - glyphLayout.width) / 2;
        float infoY = titleY - 100;  // Increased from 60 to 100 to prevent overlap with title
        bodyFont.draw(batch, infoText, infoX, infoY);
        
        // Points remaining
        String pointsText = "Points Remaining: " + pointsRemaining;
        glyphLayout.setText(subtitleFont, pointsText);
        float pointsX = (Gdx.graphics.getWidth() - glyphLayout.width) / 2;
        float pointsY = infoY - 80;
        if (pointsRemaining > 0) {
            subtitleFont.setColor(Color.YELLOW);
        } else {
            subtitleFont.setColor(Color.WHITE);
        }
        subtitleFont.draw(batch, pointsText, pointsX, pointsY);
        subtitleFont.setColor(Color.WHITE);
        
        batch.end();
    }
    
    private void drawAttributes() {
        float startY = Gdx.graphics.getHeight() - 450;  // More space from top
        float currentY = startY;
        float leftMargin = 50;
        float attributeHeight = 90;  // Increased from 70 to 90 for better spacing
        
        // Draw Mental Attributes
        currentY = drawAttributeCategory("Mental", CharacterAttribute.getMentalAttributes(), currentY, leftMargin, attributeHeight);
        currentY -= 70;  // Increased spacing between categories from 50 to 70 for better visual separation
        
        // Draw Physical Attributes
        currentY = drawAttributeCategory("Physical", CharacterAttribute.getPhysicalAttributes(), currentY, leftMargin, attributeHeight);
        currentY -= 70;  // Increased spacing between categories from 50 to 70 for better visual separation
        
        // Draw Social Attributes
        currentY = drawAttributeCategory("Social", CharacterAttribute.getSocialAttributes(), currentY, leftMargin, attributeHeight);
    }
    
    private float drawAttributeCategory(String categoryName, CharacterAttribute[] attributes, 
                                       float startY, float leftMargin, float attributeHeight) {
        float currentY = startY;
        
        // Category header
        batch.begin();
        subtitleFont.setColor(categoryColor);
        subtitleFont.draw(batch, categoryName + ":", leftMargin, currentY);
        subtitleFont.setColor(Color.WHITE);
        batch.end();
        
        currentY -= 100;  // Increased from 80 to 100 for better spacing after category header
        
        // Draw each attribute in the category
        for (CharacterAttribute attr : attributes) {
            drawAttributeLine(attr, currentY, leftMargin, attributeHeight);
            currentY -= attributeHeight;
        }
        
        return currentY;
    }
    
    private void drawAttributeLine(CharacterAttribute attr, float y, float leftMargin, float height) {
        int value = attributeValues.get(attr);
        
        // Position for minus button
        float minusX = leftMargin;
        float buttonY = y - SMALL_BUTTON_SIZE + 10;
        minusButtons.get(attr).set(minusX, buttonY, SMALL_BUTTON_SIZE, SMALL_BUTTON_SIZE);
        
        // Position for attribute name and value
        float textX = minusX + SMALL_BUTTON_SIZE + 20;
        
        // Position for plus button - increased from 200 to 280 to prevent overlap with longer attribute names
        float plusX = textX + 280;
        plusButtons.get(attr).set(plusX, buttonY, SMALL_BUTTON_SIZE, SMALL_BUTTON_SIZE);
        
        // Position for value display
        float valueX = plusX + SMALL_BUTTON_SIZE + 20;
        
        // Draw buttons
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        
        drawSmallButton(minusButtons.get(attr), "-", mouseX, mouseY, value > MIN_ATTRIBUTE_VALUE);
        drawSmallButton(plusButtons.get(attr), "+", mouseX, mouseY, value < MAX_ATTRIBUTE_VALUE && pointsRemaining > 0);
        
        // Draw attribute name and value
        batch.begin();
        smallFont.draw(batch, attr.getDisplayName(), textX, y);
        bodyFont.draw(batch, String.valueOf(value), valueX, y);
        batch.end();
    }
    
    private void drawSmallButton(Rectangle button, String text, int mouseX, int mouseY, boolean enabled) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (!enabled) {
            shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f);
        } else if (button.contains(mouseX, mouseY)) {
            shapeRenderer.setColor(buttonHoverColor);
        } else {
            shapeRenderer.setColor(buttonColor);
        }
        shapeRenderer.rect(button.x, button.y, button.width, button.height);
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(enabled ? Color.WHITE : Color.DARK_GRAY);
        shapeRenderer.rect(button.x, button.y, button.width, button.height);
        shapeRenderer.end();
        
        if (enabled) {
            batch.begin();
            glyphLayout.setText(subtitleFont, text);
            float textX = button.x + (button.width - glyphLayout.width) / 2;
            float textY = button.y + (button.height + glyphLayout.height) / 2;
            subtitleFont.draw(batch, text, textX, textY);
            batch.end();
        }
    }
    
    private void drawButtons() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        
        drawButton(confirmButton, "Confirm", mouseX, mouseY, pointsRemaining == 0);
        drawButton(backButton, "Back", mouseX, mouseY, true);
    }
    
    private void drawButton(Rectangle button, String text, int mouseX, int mouseY, boolean enabled) {
        float spacingLR = 4;
        float spacingB = 2;
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (!enabled) {
            shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f);
        } else if (button.contains(mouseX, mouseY)) {
            shapeRenderer.setColor(buttonHoverColor);
        } else {
            shapeRenderer.setColor(buttonColor);
        }
        shapeRenderer.rect(button.x + spacingLR, button.y + spacingB, 
                          button.width - (spacingLR * 2), button.height - spacingB);
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(enabled ? Color.WHITE : Color.DARK_GRAY);
        shapeRenderer.rect(button.x + spacingLR, button.y + spacingB, 
                          button.width - (spacingLR * 2), button.height - spacingB);
        shapeRenderer.end();
        
        batch.begin();
        glyphLayout.setText(subtitleFont, text);
        float textX = button.x + spacingLR + ((button.width - (spacingLR * 2)) - glyphLayout.width) / 2;
        float textY = button.y + spacingB + ((button.height - spacingB) + glyphLayout.height) / 2;
        if (enabled) {
            subtitleFont.setColor(Color.WHITE);
        } else {
            subtitleFont.setColor(Color.DARK_GRAY);
        }
        subtitleFont.draw(batch, text, textX, textY);
        subtitleFont.setColor(Color.WHITE);
        batch.end();
    }
    
    private void handleInput() {
        if (Gdx.input.justTouched()) {
            int mouseX = Gdx.input.getX();
            int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
            
            // Check +/- buttons for each attribute
            for (CharacterAttribute attr : CharacterAttribute.values()) {
                Rectangle plusBtn = plusButtons.get(attr);
                Rectangle minusBtn = minusButtons.get(attr);
                int currentValue = attributeValues.get(attr);
                
                if (plusBtn.contains(mouseX, mouseY) && currentValue < MAX_ATTRIBUTE_VALUE && pointsRemaining > 0) {
                    attributeValues.put(attr, currentValue + 1);
                    pointsRemaining--;
                } else if (minusBtn.contains(mouseX, mouseY) && currentValue > MIN_ATTRIBUTE_VALUE) {
                    attributeValues.put(attr, currentValue - 1);
                    pointsRemaining++;
                }
            }
            
            // Check Confirm button
            if (confirmButton.contains(mouseX, mouseY) && pointsRemaining == 0) {
                createCharacter();
            }
            
            // Check Back button
            if (backButton.contains(mouseX, mouseY)) {
                initialized = false;
                game.setScreen(new ProfileCreationScreen(game));
            }
        }
    }
    
    private void createCharacter() {
        try {
            // Convert CharacterAttribute enum to String keys for Profile
            Map<String, Integer> attributeMap = new HashMap<>();
            for (Map.Entry<CharacterAttribute, Integer> entry : attributeValues.entrySet()) {
                attributeMap.put(entry.getKey().name(), entry.getValue());
            }
            
            // Create profile with attributes, starting date (2050), and random seed
            int startingDate = 2050;
            long randomSeed = System.currentTimeMillis();
            Profile profile = new Profile(characterName, gender, difficulty, attributeMap, startingDate, randomSeed);
            game.getProfileManager().addProfile(profile);
            game.getProfileManager().selectProfile(profile);
            
            initialized = false;
            game.setScreen(new ProfileLoadSummaryScreen(game, profile));
        } catch (Exception e) {
            Gdx.app.error("CharacterAttributeScreen", "Error creating character: " + e.getMessage(), e);
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
        Gdx.app.log("CharacterAttributeScreen", "hide() called");
        if (initialized) {
            dispose();
        }
    }
    
    @Override
    public void dispose() {
        Gdx.app.log("CharacterAttributeScreen", "dispose() called");
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }
        initialized = false;
        Gdx.app.log("CharacterAttributeScreen", "dispose() completed");
    }
}

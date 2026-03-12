package eb.framework1.screen;

import eb.framework1.*;
import eb.framework1.character.*;
import eb.framework1.ui.*;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
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
    private int selectedGender; // 0 = index of first gender entry, 1 = second
    private int selectedDifficulty; // 0 = Easy, 1 = Normal, 2 = Hard
    private int selectedIconIndex; // 0 = first icon, 1 = second icon
    /** Display label for the first (male) gender button, loaded from category_en.json. */
    private String maleLabel   = "Male";
    /** Display label for the second (female) gender button, loaded from category_en.json. */
    private String femaleLabel = "Female";
    
    // Character icon textures
    private Texture man1Texture;
    private Texture man2Texture;
    private Texture woman1Texture;
    private Texture woman2Texture;
    private Rectangle icon1Button;
    private Rectangle icon2Button;
    private static final int ICON_SIZE   = 96;  // portrait icon display size
    private static final int ICON_BORDER =  4;  // highlight border width around icon

    // Layout padding constants (device pixels – not density-scaled)
    private static final int INPUT_PAD_H       = 10; // horizontal text inset inside the name box
    private static final int INPUT_PAD_V       =  6; // vertical text inset inside the name box
    private static final int BTN_PAD_H         = 28; // horizontal text padding inside buttons
    private static final int BTN_PAD_V         = 14; // vertical text padding inside buttons
    /** Total gap-unit weight used to distribute vertical whitespace across the form (see show()). */
    private static final int TOTAL_GAP_UNITS   = 19;
    private static final String FONT_MEASURE_CHARS = "Ag"; // representative chars for cap-height measurement

    private boolean cursorVisible;
    private float cursorTimer;
    private static final float CURSOR_BLINK_TIME = 0.5f;
    private static final int MAX_INPUT_LENGTH = 20;
    private static final int MIN_INPUT_LENGTH = 2;
    
    // Button / input dimensions – all computed in show() from measured font metrics
    private Rectangle createButton;
    private Rectangle cancelButton;
    private Rectangle randomNameButton;
    private Rectangle genderMaleButton;
    private Rectangle genderFemaleButton;
    private Rectangle diffEasyButton;
    private Rectangle diffNormalButton;
    private Rectangle diffHardButton;
    private Rectangle nameInputBox;  // visible bordered input field
    private int nameInputY;          // text baseline Y inside nameInputBox
    // Label Y positions (top of each glyph row, computed from font metrics in show())
    private int titleDrawY;      // Y at which the title is drawn in render()
    private int labelNameY;
    private int genderLabelY;
    private int portraitLabelY;
    private int difficultyLabelY;

    private Color buttonColor = new Color(0.3f, 0.3f, 0.4f, 1f);
    private Color buttonHoverColor = new Color(0.4f, 0.4f, 0.5f, 1f);
    private Color selectedButtonColor = new Color(0.5f, 0.6f, 0.7f, 1f);
    
    public ProfileCreationScreen(Main game) {
        Gdx.app.log("ProfileCreationScreen", "Constructor called");
        this.game = game;
        this.characterNameInput = new StringBuilder();
        this.selectedGender = 0;
        this.selectedDifficulty = 1; // Default to Normal
        this.selectedIconIndex = 0; // Default to first icon
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

            // Load gender labels from the category file (falls back to "Male"/"Female" if not found)
            eb.framework1.character.GenderDefinition maleDef =
                    game.getGameDataManager().getGenderCategoryByCode("male");
            eb.framework1.character.GenderDefinition femaleDef =
                    game.getGameDataManager().getGenderCategoryByCode("female");
            if (maleDef   != null) maleLabel  = maleDef.getName();
            if (femaleDef != null) femaleLabel = femaleDef.getName();

            // ==============================================================
            // Flowing layout: all Y positions derived from actual font metrics
            // so the form looks correct at any screen density.
            //
            // libGDX coordinate system: Y=0 at bottom, increases upward.
            //   font.draw(y)   → y is the top (ascent line) of the glyphs
            //   Rectangle.y    → bottom-left corner of the rect
            // curY always holds the "top edge" of the next item to place.
            // ==============================================================
            int screenWidth = Gdx.graphics.getWidth();

            // Measure font cap heights via GlyphLayout
            glyphLayout.setText(labelFont, FONT_MEASURE_CHARS);
            int labelH = (int) glyphLayout.height;
            glyphLayout.setText(font, FONT_MEASURE_CHARS);
            int inputTextH = (int) glyphLayout.height;

            // Derived sizes
            int inputBoxH = inputTextH + INPUT_PAD_V * 2;
            int inputBoxW = screenWidth - 2 * 20; // margin = 20

            // Button bounds
            TextMeasurer.TextBounds createBounds     = TextMeasurer.measure(buttonFont, "Create",      BTN_PAD_H, BTN_PAD_V);
            TextMeasurer.TextBounds cancelBounds     = TextMeasurer.measure(buttonFont, "Cancel",      BTN_PAD_H, BTN_PAD_V);
            TextMeasurer.TextBounds randomNameBounds = TextMeasurer.measure(buttonFont, "Random Name", BTN_PAD_H, BTN_PAD_V);
            TextMeasurer.TextBounds maleBounds       = TextMeasurer.measure(buttonFont, maleLabel,   BTN_PAD_H, BTN_PAD_V);
            TextMeasurer.TextBounds femaleBounds     = TextMeasurer.measure(buttonFont, femaleLabel, BTN_PAD_H, BTN_PAD_V);
            TextMeasurer.TextBounds easyBounds       = TextMeasurer.measure(buttonFont, "Easy",        BTN_PAD_H, BTN_PAD_V);
            TextMeasurer.TextBounds normalBounds     = TextMeasurer.measure(buttonFont, "Normal",      BTN_PAD_H, BTN_PAD_V);
            TextMeasurer.TextBounds hardBounds       = TextMeasurer.measure(buttonFont, "Hard",        BTN_PAD_H, BTN_PAD_V);

            // Create / Cancel button sizes are needed for the gap calculation below
            // (positions are computed in the flowing layout further down)

            // === Flowing top→bottom layout – gaps proportional to screen height ===
            //
            // libGDX coordinate system: Y=0 at bottom, increases upward.
            //   font.draw(y)  → y is the top (ascent line) of the glyphs
            //   Rectangle.y   → bottom-left corner of the rect
            // curY always holds the "top edge" of the next item to place.
            //
            // Gap unit weights (19 units total):
            //   top-margin         : 1
            //   title → name label : 2  (TITLE_FORM_GAP)
            //   label → input      : 1  (ITEM_GAP)
            //   input → random-btn : 1  (ITEM_GAP)
            //   random-btn → gender: 3  (SECTION_GAP)
            //   gender  → portrait : 3  (SECTION_GAP)
            //   portrait → diff    : 3  (SECTION_GAP)
            //   diff label → btns  : 1  (ITEM_GAP)
            //   diff btns → create : 3  (SECTION_GAP)
            //   bottom-margin      : 1
            //   ──────────────────────
            //   Total              : 19
            // ==============================================================
            int margin  = 20;
            int screenH = Gdx.graphics.getHeight();

            // Measure title height
            BitmapFont titleFont = fontManager.getTitleFont();
            glyphLayout.setText(titleFont, "Create Profile");
            int titleFontH = (int) glyphLayout.height;

            // Gender-row height – computed here (before totalContent) because it contributes
            // to the fixed-content sum used to derive the dynamic gap unit.
            int genderRowH = Math.max(labelH, (int) maleBounds.height);

            // Total height consumed by all content items (no gaps included)
            int totalContent = titleFontH
                    + labelH                            // Character Name label
                    + inputBoxH                         // name input field
                    + (int) randomNameBounds.height     // Random Name button
                    + genderRowH                        // Gender label + buttons row
                    + ICON_SIZE                         // Portrait icons
                    + labelH                            // Difficulty label
                    + (int) easyBounds.height           // Difficulty buttons
                    + (int) createBounds.height;        // Create / Cancel buttons

            // Distribute remaining space proportionally (min 8px per unit).
            // If totalContent > screenH the gap unit falls back to 8px minimum,
            // which gracefully degrades on very small screens.
            int availableForGaps = screenH - totalContent;
            int gapUnit       = Math.max(8, availableForGaps / TOTAL_GAP_UNITS);
            int dynItemGap    = gapUnit;
            int dynSectionGap = gapUnit * 3;

            titleDrawY = screenH - gapUnit;                    // 1-unit top margin
            int curY   = titleDrawY - titleFontH - gapUnit * 2; // 2-unit TITLE_FORM_GAP

            // -- Character Name --
            labelNameY = curY;
            curY -= labelH + dynItemGap;

            // Input field box: top edge at curY
            nameInputBox = new Rectangle(margin, curY - inputBoxH, inputBoxW, inputBoxH);
            this.nameInputY = curY - INPUT_PAD_V;   // text baseline inside box
            curY -= inputBoxH + dynItemGap;

            // Random Name button: just below input box
            randomNameButton = new Rectangle(margin, curY - (int) randomNameBounds.height,
                                             (int) randomNameBounds.width, (int) randomNameBounds.height);
            curY -= (int) randomNameBounds.height + dynSectionGap;

            // -- Gender (label + [Male] [Female] inline) --
            genderLabelY = curY;
            glyphLayout.setText(labelFont, "Gender:");
            int genderBtnX      = margin + (int) glyphLayout.width + 12;
            int genderBtnBottom = curY - (int) maleBounds.height;
            genderMaleButton   = new Rectangle(genderBtnX,
                                               genderBtnBottom,
                                               (int) maleBounds.width, (int) maleBounds.height);
            genderFemaleButton = new Rectangle(genderBtnX + (int) maleBounds.width + 8,
                                               genderBtnBottom,
                                               (int) femaleBounds.width, (int) femaleBounds.height);
            curY -= genderRowH + dynSectionGap;

            // -- Portrait (label + icons inline on the same row) --
            portraitLabelY = curY;
            glyphLayout.setText(labelFont, "Portrait:");
            int portraitLabelW = (int) glyphLayout.width;
            int iconLeft    = margin + portraitLabelW + 16; // 16px gap after label text
            int iconBottomY = curY - ICON_SIZE;             // icons top-aligned with label row
            icon1Button = new Rectangle(iconLeft,                    iconBottomY, ICON_SIZE, ICON_SIZE);
            icon2Button = new Rectangle(iconLeft + ICON_SIZE + 10,   iconBottomY, ICON_SIZE, ICON_SIZE);
            curY -= ICON_SIZE + dynSectionGap;

            // -- Difficulty (label row then buttons row) --
            difficultyLabelY = curY;
            curY -= labelH + dynItemGap;
            int diffBtnBottom = curY - (int) easyBounds.height;
            diffEasyButton   = new Rectangle(margin,
                                             diffBtnBottom,
                                             (int) easyBounds.width, (int) easyBounds.height);
            diffNormalButton = new Rectangle(margin + (int) easyBounds.width + 8,
                                             diffBtnBottom,
                                             (int) normalBounds.width, (int) normalBounds.height);
            diffHardButton   = new Rectangle(margin + (int) easyBounds.width + 8 + (int) normalBounds.width + 8,
                                             diffBtnBottom,
                                             (int) hardBounds.width, (int) hardBounds.height);
            curY -= (int) easyBounds.height + dynSectionGap;

            // Create / Cancel: positioned just below difficulty buttons in the flowing layout
            createButton = new Rectangle(centerX - (int) createBounds.width - 10,
                                         curY - (int) createBounds.height,
                                         (int) createBounds.width, (int) createBounds.height);
            cancelButton = new Rectangle(centerX + 10,
                                         curY - (int) cancelBounds.height,
                                         (int) cancelBounds.width, (int) cancelBounds.height);
            
            // Load character icon textures as-is (no pixel transformation)
            Gdx.app.log("ProfileCreationScreen", "Loading character icon textures...");
            man1Texture = TextureUtils.loadAsIs("character/man1.png");
            man2Texture = TextureUtils.loadAsIs("character/man2.png");
            woman1Texture = TextureUtils.loadAsIs("character/woman1.png");
            woman2Texture = TextureUtils.loadAsIs("character/woman2.png");
            man1Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            man2Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            woman1Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            woman2Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            
            Gdx.app.log("ProfileCreationScreen", "Button positions - Gender Male: " + genderMaleButton);
            Gdx.app.log("ProfileCreationScreen", "Button positions - Difficulty Hard: " + diffHardButton);
            
            // Set up input processor
            Gdx.app.log("ProfileCreationScreen", "Setting up input processor...");
            Gdx.input.setInputProcessor(new InputAdapter() {
                private boolean shiftPressed = false;
                
                @Override
                public boolean keyDown(int keycode) {
                    if (keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT) {
                        shiftPressed = true;
                        return true;
                    }
                    return false;
                }
                
                @Override
                public boolean keyUp(int keycode) {
                    if (keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT) {
                        shiftPressed = false;
                        return true;
                    }
                    return false;
                }
                
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
                    } else if (Character.isLetter(character) || Character.isDigit(character) || character == ' ') {
                        if (characterNameInput.length() < MAX_INPUT_LENGTH) {
                            // On Android soft keyboards, keyTyped may receive lowercase even when shift is pressed
                            // Check if this is a letter and should be uppercase based on context
                            char charToAdd = character;
                            if (Character.isLetter(character)) {
                                // Auto-capitalize first letter, or if shift is pressed (for desktop)
                                if (characterNameInput.length() == 0 || shiftPressed) {
                                    charToAdd = Character.toUpperCase(character);
                                }
                            }
                            characterNameInput.append(charToAdd);
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

        // Draw the name input field border (ShapeRenderer must run before batch.begin())
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.15f, 0.15f, 0.22f, 1f);  // slightly lighter dark fill
        shapeRenderer.rect(nameInputBox.x, nameInputBox.y, nameInputBox.width, nameInputBox.height);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.LIGHT_GRAY);
        shapeRenderer.rect(nameInputBox.x, nameInputBox.y, nameInputBox.width, nameInputBox.height);
        shapeRenderer.end();

        batch.begin();
        
        // Title
        BitmapFont titleFont = fontManager.getTitleFont();
        String titleText = "Create Profile";
        glyphLayout.setText(titleFont, titleText);
        titleFont.draw(batch, titleText, 
                      (Gdx.graphics.getWidth() - glyphLayout.width) / 2, 
                      titleDrawY);
        
        // Character Name label and input text (using font-metric-based positions)
        labelFont.draw(batch, "Character Name:", 20, labelNameY);
        String characterText = characterNameInput.toString();
        if (cursorVisible) characterText += "|";
        font.draw(batch, characterText, nameInputBox.x + INPUT_PAD_H, nameInputY);
        
        // Gender label (inline with buttons, computed in show())
        labelFont.draw(batch, "Gender:", 20, genderLabelY);
        
        // Portrait label
        labelFont.draw(batch, "Portrait:", 20, portraitLabelY);
        
        // Difficulty label
        labelFont.draw(batch, "Difficulty:", 20, difficultyLabelY);
        
        batch.end();
        
        // Draw character icons
        drawCharacterIcons();
        
        // Draw buttons
        drawButtons();
    }
    
    private void drawButtons() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        
        // Gender buttons
        drawButton(genderMaleButton, maleLabel, mouseX, mouseY, selectedGender == 0);
        drawButton(genderFemaleButton, femaleLabel, mouseX, mouseY, selectedGender == 1);
        
        // Difficulty buttons
        drawButton(diffEasyButton, "Easy", mouseX, mouseY, selectedDifficulty == 0);
        drawButton(diffNormalButton, "Normal", mouseX, mouseY, selectedDifficulty == 1);
        drawButton(diffHardButton, "Hard", mouseX, mouseY, selectedDifficulty == 2);
        
        // Action buttons
        drawButton(createButton, "Create", mouseX, mouseY, false);
        drawButton(cancelButton, "Cancel", mouseX, mouseY, false);
        drawButton(randomNameButton, "Random Name", mouseX, mouseY, false);
    }
    
    private void drawCharacterIcons() {
        Texture tex1, tex2;
        if (selectedGender == 0) {
            tex1 = man1Texture;
            tex2 = man2Texture;
        } else {
            tex1 = woman1Texture;
            tex2 = woman2Texture;
        }
        
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        
        // Draw selection border for icon 1
        drawIconBorder(icon1Button, mouseX, mouseY, selectedIconIndex == 0);
        // Draw selection border for icon 2
        drawIconBorder(icon2Button, mouseX, mouseY, selectedIconIndex == 1);
        
        // Draw the icon textures
        batch.begin();
        batch.draw(tex1, icon1Button.x, icon1Button.y, icon1Button.width, icon1Button.height);
        batch.draw(tex2, icon2Button.x, icon2Button.y, icon2Button.width, icon2Button.height);
        batch.end();
    }
    
    private void drawIconBorder(Rectangle iconRect, int mouseX, int mouseY, boolean selected) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // Outer border highlight (selection state)
        if (selected) {
            shapeRenderer.setColor(selectedButtonColor);
        } else if (iconRect.contains(mouseX, mouseY)) {
            shapeRenderer.setColor(buttonHoverColor);
        } else {
            shapeRenderer.setColor(buttonColor);
        }
        shapeRenderer.rect(iconRect.x - ICON_BORDER, iconRect.y - ICON_BORDER,
                          iconRect.width + ICON_BORDER * 2, iconRect.height + ICON_BORDER * 2);
        // Light background inside the icon area so dark portrait silhouettes are visible
        shapeRenderer.setColor(0.90f, 0.90f, 0.88f, 1f);
        shapeRenderer.rect(iconRect.x, iconRect.y, iconRect.width, iconRect.height);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(selected ? Color.YELLOW : Color.WHITE);
        shapeRenderer.rect(iconRect.x - ICON_BORDER, iconRect.y - ICON_BORDER,
                          iconRect.width + ICON_BORDER * 2, iconRect.height + ICON_BORDER * 2);
        shapeRenderer.end();
    }
    
    private void drawButton(Rectangle button, String text, int mouseX, int mouseY, boolean selected) {
        // Apply spacing: 4 pixels left/right, 2 pixels bottom
        float spacingLR = 4;  // Left/Right spacing
        float spacingB = 2;   // Bottom spacing
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (selected) {
            shapeRenderer.setColor(selectedButtonColor);
        } else if (button.contains(mouseX, mouseY)) {
            shapeRenderer.setColor(buttonHoverColor);
        } else {
            shapeRenderer.setColor(buttonColor);
        }
        // Apply inset padding to the button rectangle
        shapeRenderer.rect(button.x + spacingLR, button.y + spacingB, 
                          button.width - (spacingLR * 2), button.height - spacingB);
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        // Border also gets the same padding
        shapeRenderer.rect(button.x + spacingLR, button.y + spacingB, 
                          button.width - (spacingLR * 2), button.height - spacingB);
        shapeRenderer.end();
        
        batch.begin();
        glyphLayout.setText(buttonFont, text);
        // Center text within the padded button area
        float textX = button.x + spacingLR + ((button.width - (spacingLR * 2)) - glyphLayout.width) / 2;
        float textY = button.y + spacingB + ((button.height - spacingB) + glyphLayout.height) / 2;
        buttonFont.draw(batch, text, textX, textY);
        batch.end();
    }
    
    private void handleInput() {
        if (Gdx.input.justTouched()) {
            int mouseX = Gdx.input.getX();
            int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
            
            if (genderMaleButton.contains(mouseX, mouseY)) {
                if (selectedGender != 0) {
                    selectedGender = 0;
                    selectedIconIndex = 0; // Reset icon selection when gender changes
                }
            } else if (genderFemaleButton.contains(mouseX, mouseY)) {
                if (selectedGender != 1) {
                    selectedGender = 1;
                    selectedIconIndex = 0; // Reset icon selection when gender changes
                }
            } else if (icon1Button.contains(mouseX, mouseY)) {
                selectedIconIndex = 0;
            } else if (icon2Button.contains(mouseX, mouseY)) {
                selectedIconIndex = 1;
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
            } else if (randomNameButton.contains(mouseX, mouseY)) {
                generateRandomName();
            }
        }
    }
    
    private void generateRandomName() {
        String genderCode = selectedGender == 0 ? "M" : "F";
        String randomName = game.getGameDataManager()
                .getPersonNameGenerator()
                .generateFull(genderCode);
        characterNameInput.setLength(0);
        // Trim to the maximum allowed length
        if (randomName.length() > MAX_INPUT_LENGTH) {
            randomName = randomName.substring(0, MAX_INPUT_LENGTH);
        }
        characterNameInput.append(randomName);
    }
    
    private boolean canCreateProfile() {
        String characterName = characterNameInput.toString().trim();
        return characterName.length() >= MIN_INPUT_LENGTH;
    }
    
    private void createProfile() {
        String characterName = characterNameInput.toString().trim();
        String gender = selectedGender == 0 ? maleLabel : femaleLabel;
        String difficulty = selectedDifficulty == 0 ? "Easy" : 
                          (selectedDifficulty == 1 ? "Normal" : "Hard");
        
        // Determine selected character icon name
        String characterIcon;
        if (selectedGender == 0) {
            characterIcon = selectedIconIndex == 0 ? "man1" : "man2";
        } else {
            characterIcon = selectedIconIndex == 0 ? "woman1" : "woman2";
        }
        
        try {
            // Stop rendering before transition
            initialized = false;
            // Go to character attribute screen instead of creating profile directly
            game.setScreen(new CharacterAttributeScreen(game, characterName, gender, difficulty, characterIcon));
        } catch (Exception e) {
            Gdx.app.error("ProfileCreation", "Error proceeding to attribute screen: " + e.getMessage());
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
        if (man1Texture != null) man1Texture.dispose();
        if (man2Texture != null) man2Texture.dispose();
        if (woman1Texture != null) woman1Texture.dispose();
        if (woman2Texture != null) woman2Texture.dispose();
    }
}

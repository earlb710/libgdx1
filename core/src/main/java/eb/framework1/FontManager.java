package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Disposable;

/**
 * FontManager provides viewport-based, density-independent font generation.
 * 
 * Uses FreeTypeFontGenerator to create scalable fonts that:
 * - Scale proportionally based on screen dimensions
 * - Maintain visual consistency across devices
 * - Avoid pixelation through runtime generation
 * - Support density-independent pixels (dp)
 */
public class FontManager implements Disposable {
    
    private FreeTypeFontGenerator generator;
    private FreeTypeFontGenerator handwrittenGenerator;
    private BitmapFont titleFont;
    private BitmapFont subtitleFont;
    private BitmapFont bodyFont;
    private BitmapFont smallFont;
    private BitmapFont tinyFont;
    private BitmapFont boldBodyFont;
    private BitmapFont boldSmallFont;
    private BitmapFont boldTinyFont;
    private BitmapFont noteFont;
    private boolean noteFontIsOwned; // true when noteFont uses its own instance (not a fallback reference)
    
    private float screenWidth;
    private float screenHeight;
    private float density;
    
    // Font size in density-independent pixels (dp) for consistent sizing
    // These are absolute dp values optimized for mobile screens
    // 
    // Font sizes reduced to fit properly on screen while maintaining readability:
    // - Title: 40dp - Large headers and titles
    // - Subtitle: 30dp - Section headers, input text
    // - Body: 22dp - Regular text, buttons
    // - Small: 18dp - Small details, hints
    //
    // These dp values will be multiplied by density to get actual pixel sizes:
    // 
    // At density 1.5 (low-DPI):
    //   Title: 40 * 1.5 = 60px
    //   Body: 22 * 1.5 = 33px
    // 
    // At density 3.0 (high-DPI):
    //   Title: 40 * 3.0 = 120px
    //   Body: 22 * 3.0 = 66px
    //
    // This ensures consistent physical size and good readability on all devices
    //
    private static final int TITLE_SIZE_DP = 40;    // Large titles
    private static final int SUBTITLE_SIZE_DP = 30; // Subtitles, input text
    private static final int BODY_SIZE_DP = 22;     // Body text, buttons
    private static final int SMALL_SIZE_DP = 18;    // Small text
    private static final int TINY_SIZE_DP = 14;     // Tiny text (attribute modifiers)
    
    /**
     * Initialize FontManager with current screen dimensions.
     * Fonts will be generated based on these dimensions for optimal rendering.
     */
    public FontManager() {
        this.screenWidth = Gdx.graphics.getWidth();
        this.screenHeight = Gdx.graphics.getHeight();
        this.density = Gdx.graphics.getDensity();
        
        Gdx.app.log("FontManager", String.format("Initializing - Screen: %.0fx%.0f, Density: %.2f", 
            screenWidth, screenHeight, density));
        
        initializeFontGenerator();
        generateFonts();
    }
    
    /**
     * Initialize the FreeType font generator.
     * Uses default LibGDX font if no custom TTF is available.
     */
    private void initializeFontGenerator() {
        // Try to load a custom font, fall back to default
        FileHandle fontFile = Gdx.files.internal("font.ttf");
        
        if (!fontFile.exists()) {
            // Use LibGDX's default font by creating a generator from it
            // We'll use the arial.ttf that comes with LibGDX
            fontFile = Gdx.files.classpath("com/badlogic/gdx/utils/lsans-15.fnt").parent().child("lsans-15.fnt");
            
            // If that doesn't exist either, we'll create fonts programmatically
            if (!fontFile.exists()) {
                Gdx.app.log("FontManager", "No TTF font found, will use default BitmapFont scaling");
                generator = null;
                return;
            }
        }
        
        try {
            // For internal fonts in assets
            fontFile = Gdx.files.internal("font.ttf");
            if (!fontFile.exists()) {
                // Use a built-in font - we'll generate one using the default mechanism
                Gdx.app.log("FontManager", "Using fallback font generation");
                generator = null;
                return;
            }
            generator = new FreeTypeFontGenerator(fontFile);
            Gdx.app.log("FontManager", "FreeTypeFontGenerator initialized successfully");

            // Load handwritten font for player notes
            FileHandle handwrittenFile = Gdx.files.internal("handwritten.ttf");
            if (handwrittenFile.exists()) {
                try {
                    handwrittenGenerator = new FreeTypeFontGenerator(handwrittenFile);
                    Gdx.app.log("FontManager", "Handwritten font generator initialized");
                } catch (Exception e) {
                    Gdx.app.error("FontManager", "Error loading handwritten font, notes will use regular font", e);
                    handwrittenGenerator = null;
                }
            } else {
                Gdx.app.log("FontManager", "No handwritten.ttf found, notes will use regular font");
                handwrittenGenerator = null;
            }
        } catch (Exception e) {
            Gdx.app.error("FontManager", "Error loading font, will use fallback", e);
            generator = null;
        }
    }
    
    /**
     * Generate all font sizes based on screen dimensions.
     */
    private void generateFonts() {
        if (generator != null) {
            generateFontsWithFreeType();
        } else {
            generateFontsWithBitmapFont();
        }
    }
    
    /**
     * Generate fonts using FreeTypeFontGenerator for best quality.
     *
     * <p>Anti-aliasing: LibGDX FreeType fonts are anti-aliased by default
     * ({@code mono = false}).  We make this explicit here and use
     * {@link FreeTypeFontGenerator.Hinting#AutoFull} for the highest-quality
     * sub-pixel hinting, which produces the smoothest rendered glyphs.</p>
     */
    private void generateFontsWithFreeType() {
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        // Anti-aliasing: FreeType renders with AA when mono=false (the default).
        // Setting it explicitly documents the intent and prevents accidental toggling.
        parameter.mono = false;
        // AutoFull hinting: best balance of sharpness and smooth AA curves
        parameter.hinting = FreeTypeFontGenerator.Hinting.AutoFull;
        // Use MipMapLinearNearest for minFilter to reduce aliasing at small sizes
        // Use Linear for magFilter for smooth scaling when magnified
        // This combination provides crisp, high-quality text rendering
        parameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearNearest;
        parameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        parameter.genMipMaps = true;  // Generate mipmaps for better quality at various sizes
        // Include extra UI characters: ▲ (expand), ▼ (collapse), • (bullet list)
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "\u25B2\u25BC\u2022";

        // Title font - large, for headers
        int titleSize = calculateFontSize(TITLE_SIZE_DP);
        parameter.size = titleSize;
        parameter.color = Color.GOLD;
        titleFont = generator.generateFont(parameter);
        Gdx.app.log("FontManager", "Generated title font at size: " + titleSize);
        
        // Subtitle font - medium-large
        int subtitleSize = calculateFontSize(SUBTITLE_SIZE_DP);
        parameter.size = subtitleSize;
        parameter.color = Color.LIGHT_GRAY;
        subtitleFont = generator.generateFont(parameter);
        Gdx.app.log("FontManager", "Generated subtitle font at size: " + subtitleSize);
        
        // Body font - standard reading size
        int bodySize = calculateFontSize(BODY_SIZE_DP);
        parameter.size = bodySize;
        parameter.color = Color.WHITE;
        bodyFont = generator.generateFont(parameter);
        Gdx.app.log("FontManager", "Generated body font at size: " + bodySize);
        
        // Small font - for details
        int smallSize = calculateFontSize(SMALL_SIZE_DP);
        parameter.size = smallSize;
        parameter.color = Color.LIGHT_GRAY;
        smallFont = generator.generateFont(parameter);
        Gdx.app.log("FontManager", "Generated small font at size: " + smallSize);

        // Tiny font - for attribute modifiers
        int tinySize = calculateFontSize(TINY_SIZE_DP);
        parameter.size = tinySize;
        parameter.color = Color.LIGHT_GRAY;
        tinyFont = generator.generateFont(parameter);
        Gdx.app.log("FontManager", "Generated tiny font at size: " + tinySize);

        // Bold variants – simulated via a thin border around each glyph
        // Use a fresh parameter to avoid state leaking from the regular-font parameter
        FreeTypeFontParameter boldParameter = new FreeTypeFontParameter();
        boldParameter.mono = false;
        boldParameter.hinting = FreeTypeFontGenerator.Hinting.AutoFull;
        boldParameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearNearest;
        boldParameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        boldParameter.genMipMaps = true;
        boldParameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "\u25B2\u25BC\u2022";
        boldParameter.borderWidth = 1f;
        boldParameter.borderStraight = true;

        boldParameter.size = bodySize;
        boldParameter.color = Color.WHITE;
        boldBodyFont = generator.generateFont(boldParameter);
        Gdx.app.log("FontManager", "Generated bold body font at size: " + bodySize);

        boldParameter.size = smallSize;
        boldParameter.color = Color.LIGHT_GRAY;
        boldSmallFont = generator.generateFont(boldParameter);
        Gdx.app.log("FontManager", "Generated bold small font at size: " + smallSize);

        boldParameter.size = tinySize;
        boldParameter.color = Color.LIGHT_GRAY;
        boldTinyFont = generator.generateFont(boldParameter);
        Gdx.app.log("FontManager", "Generated bold tiny font at size: " + tinySize);

        // Handwritten font for player notes (uses separate TTF)
        if (handwrittenGenerator != null) {
            FreeTypeFontParameter noteParam = new FreeTypeFontParameter();
            noteParam.mono = false;
            noteParam.hinting = FreeTypeFontGenerator.Hinting.AutoFull;
            noteParam.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearNearest;
            noteParam.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
            noteParam.genMipMaps = true;
            noteParam.size = smallSize;
            noteParam.color = Color.WHITE;
            noteFont = handwrittenGenerator.generateFont(noteParam);
            noteFontIsOwned = true;
            Gdx.app.log("FontManager", "Generated handwritten note font at size: " + smallSize);
        } else {
            noteFont = smallFont; // fallback to regular small font
            noteFontIsOwned = false;
            Gdx.app.log("FontManager", "Using regular small font as note font fallback");
        }
    }
    
    /**
     * Fallback: Generate fonts using BitmapFont with larger scaling.
     * Used when FreeTypeFontGenerator is not available.
     * 
     * Note: BitmapFont scales poorly and will show some pixelation at large scales.
     * For best quality, add a TTF font file to assets/font.ttf.
     */
    private void generateFontsWithBitmapFont() {
        Gdx.app.log("FontManager", "Using BitmapFont fallback with larger scales");
        Gdx.app.log("FontManager", "WARNING: BitmapFont will be pixelated. Add a TTF font to assets/font.ttf for better quality.");
        
        // Use larger scale values for better readability
        // There will be some pixelation, but fonts will be appropriately sized
        
        titleFont = new BitmapFont();
        titleFont.setColor(Color.GOLD);
        titleFont.getData().setScale(6.0f);  // 6x scale - large titles
        
        subtitleFont = new BitmapFont();
        subtitleFont.setColor(Color.LIGHT_GRAY);
        subtitleFont.getData().setScale(4.5f);  // 4.5x scale - medium-large
        
        bodyFont = new BitmapFont();
        bodyFont.setColor(Color.WHITE);
        bodyFont.getData().setScale(3.5f);  // 3.5x scale - readable size
        
        smallFont = new BitmapFont();
        smallFont.setColor(Color.LIGHT_GRAY);
        smallFont.getData().setScale(2.5f);  // 2.5x scale - visible details

        tinyFont = new BitmapFont();
        tinyFont.setColor(Color.LIGHT_GRAY);
        tinyFont.getData().setScale(2.0f);  // 2.0x scale - attribute modifiers

        // Bold fallback: slightly wider scaleX gives a heavier appearance
        boldBodyFont = new BitmapFont();
        boldBodyFont.setColor(Color.WHITE);
        boldBodyFont.getData().setScale(3.85f, 3.5f);

        boldSmallFont = new BitmapFont();
        boldSmallFont.setColor(Color.LIGHT_GRAY);
        boldSmallFont.getData().setScale(2.75f, 2.5f);

        boldTinyFont = new BitmapFont();
        boldTinyFont.setColor(Color.LIGHT_GRAY);
        boldTinyFont.getData().setScale(2.2f, 2.0f);

        // Note font fallback – same as small font (no handwritten TTF available)
        noteFont = smallFont;
        noteFontIsOwned = false;
        
        Gdx.app.log("FontManager", "BitmapFont scales - Title: 6.0x, Subtitle: 4.5x, Body: 3.5x, Small: 2.5x, Tiny: 2.0x");
        Gdx.app.log("FontManager", "Bold BitmapFont scales - Body: 3.85×3.5x, Small: 2.75×2.5x, Tiny: 2.2×2.0x");
        Gdx.app.log("FontManager", "To eliminate pixelation, add a TrueType font file to assets/font.ttf");
    }
    
    /**
     * Calculate font size in pixels based on density-independent pixels (dp).
     * 
     * Converts dp to actual pixels by multiplying by screen density.
     * This ensures fonts appear at the same physical size on all devices.
     * 
     * For example:
     * - 18dp @ density 1.5 = 27px
     * - 18dp @ density 2.0 = 36px  
     * - 18dp @ density 3.0 = 54px
     * 
     * All three will appear the same physical size on their respective screens.
     * 
     * @param dp Desired size in density-independent pixels
     * @return Font size in actual pixels
     */
    private int calculateFontSize(int dp) {
        // Convert dp to pixels: pixels = dp * density
        int pixels = (int)(dp * density);
        
        // Ensure minimum readable size
        int size = Math.max(12, pixels);
        
        Gdx.app.log("FontManager", String.format("Font calc - %ddp × %.1f density = %dpx (screen: %.0fx%.0f)", 
            dp, density, size, screenWidth, screenHeight));
        
        return size;
    }
    
    /**
     * Generate a custom font at a specific density-independent pixel size.
     * 
     * @param dp Desired size in density-independent pixels
     * @param color Font color
     * @return Generated BitmapFont
     */
    public BitmapFont generateCustomFont(int dp, Color color) {
        if (generator != null) {
            FreeTypeFontParameter parameter = new FreeTypeFontParameter();
            parameter.size = (int)(dp * density);
            parameter.color = color;
            parameter.mono = false;
            parameter.hinting = FreeTypeFontGenerator.Hinting.AutoFull;
            parameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearNearest;
            parameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
            parameter.genMipMaps = true;
            parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "\u25B2\u25BC\u2022";
            return generator.generateFont(parameter);
        } else {
            // Fallback to BitmapFont
            BitmapFont font = new BitmapFont();
            font.setColor(color);
            // Approximate dp to scale conversion
            font.getData().setScale(dp * density * 0.1f);
            return font;
        }
    }
    
    /**
     * Generate a custom bold font at a specific density-independent pixel size.
     * Bold is achieved via a 1px border (FreeType) or wider scaleX (BitmapFont fallback).
     *
     * @param dp    Desired size in density-independent pixels
     * @param color Font color
     * @return Generated bold BitmapFont (caller is responsible for disposal)
     */
    public BitmapFont generateBoldFont(int dp, Color color) {
        if (generator != null) {
            FreeTypeFontParameter parameter = new FreeTypeFontParameter();
            parameter.size = (int)(dp * density);
            parameter.color = color;
            parameter.borderWidth = 1f;
            parameter.borderStraight = true;
            parameter.mono = false;
            parameter.hinting = FreeTypeFontGenerator.Hinting.AutoFull;
            parameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearNearest;
            parameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
            parameter.genMipMaps = true;
            parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "\u25B2\u25BC\u2022";
            return generator.generateFont(parameter);
        } else {
            BitmapFont font = new BitmapFont();
            font.setColor(color);
            float scale = dp * density * 0.1f;
            font.getData().setScale(scale * 1.1f, scale); // wider X for bold effect
            return font;
        }
    }

    // Getters for pre-generated fonts
    
    public BitmapFont getTitleFont() {
        return titleFont;
    }
    
    public BitmapFont getSubtitleFont() {
        return subtitleFont;
    }
    
    public BitmapFont getBodyFont() {
        return bodyFont;
    }
    
    public BitmapFont getSmallFont() {
        return smallFont;
    }

    public BitmapFont getTinyFont() {
        return tinyFont;
    }

    /** Bold variant of the body font. */
    public BitmapFont getBoldBodyFont() {
        return boldBodyFont;
    }

    /** Bold variant of the small font. */
    public BitmapFont getBoldSmallFont() {
        return boldSmallFont;
    }

    /** Bold variant of the tiny font. */
    public BitmapFont getBoldTinyFont() {
        return boldTinyFont;
    }

    /** Handwritten-style font for player notes. */
    public BitmapFont getNoteFont() {
        return noteFont;
    }
    
    /**
     * Get screen width used for font generation.
     */
    public float getScreenWidth() {
        return screenWidth;
    }
    
    /**
     * Get screen height used for font generation.
     */
    public float getScreenHeight() {
        return screenHeight;
    }
    
    /**
     * Get screen density used for font generation.
     */
    public float getDensity() {
        return density;
    }
    
    @Override
    public void dispose() {
        Gdx.app.log("FontManager", "Disposing fonts");
        
        if (titleFont != null) titleFont.dispose();
        if (subtitleFont != null) subtitleFont.dispose();
        if (bodyFont != null) bodyFont.dispose();
        if (smallFont != null) smallFont.dispose();
        if (tinyFont != null) tinyFont.dispose();
        if (boldBodyFont != null) boldBodyFont.dispose();
        if (boldSmallFont != null) boldSmallFont.dispose();
        if (boldTinyFont != null) boldTinyFont.dispose();
        if (noteFont != null && noteFontIsOwned) noteFont.dispose();
        
        if (generator != null) {
            generator.dispose();
        }
        if (handwrittenGenerator != null) {
            handwrittenGenerator.dispose();
        }
    }
}

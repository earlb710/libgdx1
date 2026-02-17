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
    private BitmapFont titleFont;
    private BitmapFont subtitleFont;
    private BitmapFont bodyFont;
    private BitmapFont smallFont;
    
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
     */
    private void generateFontsWithFreeType() {
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        // Use MipMapLinearNearest for minFilter to reduce aliasing at small sizes
        // Use Linear for magFilter for smooth scaling when magnified
        // This combination provides crisp, high-quality text rendering
        parameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearNearest;
        parameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        parameter.genMipMaps = true;  // Generate mipmaps for better quality at various sizes
        
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
        
        Gdx.app.log("FontManager", "BitmapFont scales - Title: 6.0x, Subtitle: 4.5x, Body: 3.5x, Small: 2.5x");
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
            parameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearNearest;
            parameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
            parameter.genMipMaps = true;
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
        
        if (generator != null) {
            generator.dispose();
        }
    }
}

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
    
    // Base font sizes as percentage of LOGICAL screen width for proportional scaling
    // These ratios work with density-independent width to ensure proper sizing
    // 
    // Example calculations for common screens:
    // 
    // 480x640 @ density 1.5 (older phone):
    //   Logical width: 480 / 1.5 = 320dp
    //   Title: 320 * 0.08 = 25.6px
    //   Body: 320 * 0.04 = 12.8px (min 12px)
    // 
    // 1080x2400 @ density 3.0 (modern phone):
    //   Logical width: 1080 / 3.0 = 360dp  
    //   Title: 360 * 0.08 = 28.8px
    //   Body: 360 * 0.04 = 14.4px
    //
    // 1080x2400 @ density 2.0 (tablet mode):
    //   Logical width: 1080 / 2.0 = 540dp
    //   Title: 540 * 0.08 = 43.2px
    //   Body: 540 * 0.04 = 21.6px
    //
    private static final float TITLE_SIZE_RATIO = 0.08f;    // 8% of logical width
    private static final float SUBTITLE_SIZE_RATIO = 0.05f; // 5% of logical width
    private static final float BODY_SIZE_RATIO = 0.04f;     // 4% of logical width
    private static final float SMALL_SIZE_RATIO = 0.03f;    // 3% of logical width
    
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
        parameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        parameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        
        // Title font - large, for headers
        int titleSize = calculateFontSize(TITLE_SIZE_RATIO);
        parameter.size = titleSize;
        parameter.color = Color.GOLD;
        titleFont = generator.generateFont(parameter);
        Gdx.app.log("FontManager", "Generated title font at size: " + titleSize);
        
        // Subtitle font - medium-large
        int subtitleSize = calculateFontSize(SUBTITLE_SIZE_RATIO);
        parameter.size = subtitleSize;
        parameter.color = Color.LIGHT_GRAY;
        subtitleFont = generator.generateFont(parameter);
        Gdx.app.log("FontManager", "Generated subtitle font at size: " + subtitleSize);
        
        // Body font - standard reading size
        int bodySize = calculateFontSize(BODY_SIZE_RATIO);
        parameter.size = bodySize;
        parameter.color = Color.WHITE;
        bodyFont = generator.generateFont(parameter);
        Gdx.app.log("FontManager", "Generated body font at size: " + bodySize);
        
        // Small font - for details
        int smallSize = calculateFontSize(SMALL_SIZE_RATIO);
        parameter.size = smallSize;
        parameter.color = Color.LIGHT_GRAY;
        smallFont = generator.generateFont(parameter);
        Gdx.app.log("FontManager", "Generated small font at size: " + smallSize);
    }
    
    /**
     * Fallback: Generate fonts using BitmapFont with scaling.
     * Used when FreeTypeFontGenerator is not available.
     */
    private void generateFontsWithBitmapFont() {
        Gdx.app.log("FontManager", "Using BitmapFont fallback with viewport-based scaling");
        
        // Calculate density-independent width
        float logicalWidth = screenWidth / density;
        
        // Calculate scale factors based on logical width
        float baseScale = logicalWidth * 0.002f; // Base multiplier for BitmapFont
        
        titleFont = new BitmapFont();
        titleFont.setColor(Color.GOLD);
        titleFont.getData().setScale(baseScale * (TITLE_SIZE_RATIO / 0.002f));
        
        subtitleFont = new BitmapFont();
        subtitleFont.setColor(Color.LIGHT_GRAY);
        subtitleFont.getData().setScale(baseScale * (SUBTITLE_SIZE_RATIO / 0.002f));
        
        bodyFont = new BitmapFont();
        bodyFont.setColor(Color.WHITE);
        bodyFont.getData().setScale(baseScale * (BODY_SIZE_RATIO / 0.002f));
        
        smallFont = new BitmapFont();
        smallFont.setColor(Color.LIGHT_GRAY);
        smallFont.getData().setScale(baseScale * (SMALL_SIZE_RATIO / 0.002f));
        
        Gdx.app.log("FontManager", String.format("BitmapFont scales (LogicalW: %.0f) - Title: %.2f, Subtitle: %.2f, Body: %.2f, Small: %.2f",
            logicalWidth, titleFont.getData().scaleX, subtitleFont.getData().scaleX, 
            bodyFont.getData().scaleX, smallFont.getData().scaleX));
    }
    
    /**
     * Calculate font size in pixels based on screen dimension ratio and density.
     * 
     * Uses density-independent width to ensure consistent sizing across devices.
     * For example, a 1080px screen with density 2.0 has a logical width of 540dp.
     * The font size is calculated from this logical width, not physical pixels.
     * 
     * @param ratio Percentage of logical screen width to use for font size
     * @return Font size in pixels
     */
    private int calculateFontSize(float ratio) {
        // Calculate density-independent width (logical pixels/dp)
        // This prevents fonts from being too large on high-DPI screens
        float logicalWidth = screenWidth / density;
        
        // Calculate font size as percentage of logical width
        // This ensures consistent visual proportions across all devices
        float fontSize = logicalWidth * ratio;
        
        // Ensure minimum readable size
        int size = Math.max(12, (int) fontSize);
        
        Gdx.app.log("FontManager", String.format("Font calc - Ratio: %.3f, Physical: %.0fx%.0f, Density: %.1f, LogicalW: %.0f, FontSize: %d", 
            ratio, screenWidth, screenHeight, density, logicalWidth, size));
        
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
            parameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
            parameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
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

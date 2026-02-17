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
    
    // Base font sizes as percentage of screen width for proportional scaling
    private static final float TITLE_SIZE_RATIO = 0.08f;    // 8% of screen width
    private static final float SUBTITLE_SIZE_RATIO = 0.05f; // 5% of screen width
    private static final float BODY_SIZE_RATIO = 0.04f;     // 4% of screen width
    private static final float SMALL_SIZE_RATIO = 0.03f;    // 3% of screen width
    
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
        
        // Calculate scale factors based on screen width
        float baseScale = screenWidth * 0.002f; // Base multiplier
        
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
        
        Gdx.app.log("FontManager", String.format("BitmapFont scales - Title: %.2f, Subtitle: %.2f, Body: %.2f, Small: %.2f",
            titleFont.getData().scaleX, subtitleFont.getData().scaleX, 
            bodyFont.getData().scaleX, smallFont.getData().scaleX));
    }
    
    /**
     * Calculate font size in pixels based on screen dimension ratio and density.
     * 
     * @param ratio Percentage of screen width to use for font size
     * @return Font size in pixels, adjusted for screen density
     */
    private int calculateFontSize(float ratio) {
        // Base size from screen width
        float baseSize = screenWidth * ratio;
        
        // Adjust for density (converts to density-independent pixels)
        // Higher density = sharper fonts at same physical size
        float dpSize = baseSize * density;
        
        // Ensure minimum readable size
        int size = Math.max(12, (int) dpSize);
        
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

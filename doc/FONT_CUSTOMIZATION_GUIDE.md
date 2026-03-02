# Font Customization Guide

## Quick Answer

**Question:** Can a different font be used?

**Answer:** **YES!** You can absolutely use different fonts in the application. This guide shows you how.

## Current Font Setup

**Current Font:**
- File: `assets/font.ttf`
- Type: TrueType Font (TTF)
- Current: Roboto-Regular (504KB)
- Managed by: `FontManager.java`

**Font Rendering System:**
- Uses `FreeTypeFontGenerator` for high-quality rendering
- Generates 4 font sizes: Title (40dp), Subtitle (30dp), Body (22dp), Small (18dp)
- Density-independent sizing ensures consistent appearance across devices
- Automatic fallback to BitmapFont if TTF not available

## Simple Font Replacement (Recommended)

The easiest way to change fonts is to replace the existing font file.

### Steps:

1. **Get a new TTF font file**
   - Download from Google Fonts, Font Squirrel, etc.
   - Ensure it's a `.ttf` file (TrueType Font)

2. **Replace the font file**
   ```bash
   # Navigate to your project
   cd /path/to/libgdx1/assets/
   
   # Backup current font (optional)
   mv font.ttf font.ttf.backup
   
   # Copy your new font
   cp /path/to/YourNewFont.ttf font.ttf
   ```

3. **Keep the filename as `font.ttf`**
   - The FontManager looks for this exact filename
   - Don't rename it to anything else

4. **Rebuild and run**
   ```bash
   # Android
   ./gradlew android:assembleDebug
   ./gradlew android:installDebug
   
   # Desktop
   ./gradlew lwjgl3:run
   ```

5. **Done!** Your new font is now used throughout the app.

## Advanced: Multiple Fonts

You can use different fonts for different text elements (titles, body, etc.).

### Add Multiple Font Files

1. **Add font files to assets:**
   ```
   assets/
   ├── font-title.ttf      # For titles/headers
   ├── font-body.ttf       # For body text
   ├── font-mono.ttf       # For code/clues
   └── logo.png
   ```

2. **Modify FontManager.java:**

Find the `initializeFontGenerator()` method around line 78 and modify it:

```java
private FreeTypeFontGenerator titleGenerator;
private FreeTypeFontGenerator bodyGenerator;
private FreeTypeFontGenerator monoGenerator;

private void initializeFontGenerator() {
    try {
        // Load different fonts for different purposes
        FileHandle titleFont = Gdx.files.internal("font-title.ttf");
        FileHandle bodyFont = Gdx.files.internal("font-body.ttf");
        FileHandle monoFont = Gdx.files.internal("font-mono.ttf");
        
        if (titleFont.exists()) {
            titleGenerator = new FreeTypeFontGenerator(titleFont);
            Gdx.app.log("FontManager", "Loaded title font");
        }
        
        if (bodyFont.exists()) {
            bodyGenerator = new FreeTypeFontGenerator(bodyFont);
            Gdx.app.log("FontManager", "Loaded body font");
        }
        
        if (monoFont.exists()) {
            monoGenerator = new FreeTypeFontGenerator(monoFont);
            Gdx.app.log("FontManager", "Loaded mono font");
        }
        
        // Use title generator as default if available
        generator = titleGenerator != null ? titleGenerator : bodyGenerator;
        
    } catch (Exception e) {
        Gdx.app.error("FontManager", "Error loading fonts", e);
        generator = null;
    }
}
```

3. **Modify font generation:**

Update `generateFontsWithFreeType()` to use different generators:

```java
private void generateFontsWithFreeType() {
    FreeTypeFontParameter parameter = new FreeTypeFontParameter();
    parameter.minFilter = Texture.TextureFilter.MipMapLinearNearest;
    parameter.magFilter = Texture.TextureFilter.Linear;
    parameter.genMipMaps = true;
    
    // Use title generator for title font
    if (titleGenerator != null) {
        parameter.size = calculateFontSize(TITLE_SIZE_DP);
        parameter.color = Color.GOLD;
        titleFont = titleGenerator.generateFont(parameter);
    }
    
    // Use body generator for other fonts
    FreeTypeFontGenerator bodyGen = bodyGenerator != null ? bodyGenerator : generator;
    
    if (bodyGen != null) {
        parameter.size = calculateFontSize(SUBTITLE_SIZE_DP);
        parameter.color = Color.LIGHT_GRAY;
        subtitleFont = bodyGen.generateFont(parameter);
        
        parameter.size = calculateFontSize(BODY_SIZE_DP);
        parameter.color = Color.WHITE;
        bodyFont = bodyGen.generateFont(parameter);
        
        parameter.size = calculateFontSize(SMALL_SIZE_DP);
        parameter.color = Color.LIGHT_GRAY;
        smallFont = bodyGen.generateFont(parameter);
    }
}
```

## Recommended Fonts for Detective Game

### Serif Fonts (Classic Detective Feel)

**Crimson Text** - Elegant, readable serif
- Use for: Story text, dialogues
- Download: [Google Fonts - Crimson Text](https://fonts.google.com/specimen/Crimson+Text)
- License: OFL (Open Font License)

**Merriweather** - Traditional, professional
- Use for: Investigation notes, reports
- Download: [Google Fonts - Merriweather](https://fonts.google.com/specimen/Merriweather)
- License: OFL

**Playfair Display** - Dramatic, stylish
- Use for: Chapter titles, important headings
- Download: [Google Fonts - Playfair Display](https://fonts.google.com/specimen/Playfair+Display)
- License: OFL

### Sans-Serif Fonts (Modern, Clean)

**Roboto** (Current) - Modern, neutral
- Use for: UI elements, buttons
- Already installed!
- License: Apache 2.0

**Open Sans** - Friendly, highly readable
- Use for: Long text, descriptions
- Download: [Google Fonts - Open Sans](https://fonts.google.com/specimen/Open+Sans)
- License: OFL

**Lato** - Professional, versatile
- Use for: General purpose
- Download: [Google Fonts - Lato](https://fonts.google.com/specimen/Lato)
- License: OFL

### Monospace Fonts (Code/Clues)

**Courier Prime** - Typewriter style
- Use for: Evidence, typed documents
- Download: [Google Fonts - Courier Prime](https://fonts.google.com/specimen/Courier+Prime)
- License: OFL

**Source Code Pro** - Clean, readable
- Use for: Computer screens, digital clues
- Download: [Google Fonts - Source Code Pro](https://fonts.google.com/specimen/Source+Code+Pro)
- License: OFL

**IBM Plex Mono** - Modern monospace
- Use for: Technical information
- Download: [Google Fonts - IBM Plex Mono](https://fonts.google.com/specimen/IBM+Plex+Mono)
- License: OFL

### Display Fonts (Titles/Headers)

**Bebas Neue** - Bold, impactful
- Use for: Game title, chapter headers
- Download: [Google Fonts - Bebas Neue](https://fonts.google.com/specimen/Bebas+Neue)
- License: OFL

**Oswald** - Strong, condensed
- Use for: Section titles
- Download: [Google Fonts - Oswald](https://fonts.google.com/specimen/Oswald)
- License: OFL

### Handwriting Fonts (Notes/Evidence)

**Permanent Marker** - Marker pen style
- Use for: Hand-written notes, annotations
- Download: [Google Fonts - Permanent Marker](https://fonts.google.com/specimen/Permanent+Marker)
- License: Apache 2.0

**Indie Flower** - Casual handwriting
- Use for: Personal notes, diary entries
- Download: [Google Fonts - Indie Flower](https://fonts.google.com/specimen/Indie+Flower)
- License: OFL

## Where to Get Fonts

### Free Sources (Open Source)

1. **Google Fonts** (Recommended)
   - URL: https://fonts.google.com/
   - 100% free, open-source
   - Easy to download
   - Great selection
   - All OFL or Apache licensed

2. **Font Squirrel**
   - URL: https://www.fontsquirrel.com/
   - 100% free for commercial use
   - Hand-picked quality fonts
   - Font identifier tool

3. **DaFont**
   - URL: https://www.dafont.com/
   - Large collection
   - **Check license!** (varies by font)
   - Good for creative/unique fonts

### Paid Sources

1. **Adobe Fonts** (Subscription)
   - Included with Creative Cloud
   - Professional quality
   - Sync across devices

2. **MyFonts**
   - Individual font purchases
   - Commercial licenses
   - Professional typefaces

## Font Selection Tips

### For Readability

1. **Test at different sizes**
   - Ensure small text (18dp) is readable
   - Check body text (22dp) comfort
   - Verify title text (40dp) looks good

2. **Consider screen usage**
   - Players read for extended periods
   - Avoid overly decorative fonts for body text
   - Save fancy fonts for titles/headers

3. **Check character support**
   - Ensure font has all needed characters
   - Test with your game's language
   - Check for special symbols (if used)

### For Detective Game Theme

1. **Classic Mystery**: Use serif fonts
   - Crimson Text for body
   - Playfair Display for titles

2. **Modern Noir**: Use clean sans-serif
   - Roboto or Open Sans for UI
   - Bebas Neue for titles

3. **Vintage Detective**: Use typewriter style
   - Courier Prime for everything
   - Adds authentic feel

4. **Professional Investigation**: Mix serif and sans
   - Merriweather for reports
   - Roboto for UI elements

## Technical Details

### Font File Formats

**TTF (TrueType Font)** - Recommended
- Widely supported
- Works with FreeTypeFontGenerator
- Good quality at all sizes
- Most common format

**OTF (OpenType Font)** - Also works
- Modern format
- Enhanced features
- Works with FreeTypeFontGenerator
- Slightly larger files

**Avoid:**
- BDF, PCF (Bitmap fonts - poor scaling)
- WOFF, WOFF2 (Web fonts - need conversion)

### Font File Size Considerations

**Small (50-100 KB):**
- Basic character sets
- Fast loading
- Lower memory usage

**Medium (100-500 KB):** - **Current**
- Standard character sets
- Good balance
- Recommended

**Large (500 KB - 2 MB):**
- Extended character sets
- Multiple weights included
- Consider impact on app size

**Note:** FreeTypeFontGenerator only loads characters you use, so large font files don't significantly impact runtime memory.

### Character Sets

Most fonts include:
- **Basic Latin**: A-Z, a-z, 0-9, punctuation
- **Latin Extended**: À, É, Ñ, etc.
- **Symbols**: ©, ®, ™, etc.

For international games, ensure your font supports:
- The target language's characters
- Special symbols/diacritics

## Example Font Setups

### Example 1: Classic Detective Novel

```bash
# Download Crimson Text from Google Fonts
# Rename CrimsonText-Regular.ttf to font.ttf
# Replace assets/font.ttf
# Result: Traditional serif font throughout
```

### Example 2: Modern Noir

```bash
# Keep current Roboto font
# Or use Open Sans for slightly warmer feel
# Download OpenSans-Regular.ttf, rename to font.ttf
# Replace assets/font.ttf
```

### Example 3: Vintage Mystery

```bash
# Download Courier Prime from Google Fonts
# Rename CourierPrime-Regular.ttf to font.ttf
# Replace assets/font.ttf
# Result: Typewriter aesthetic
```

### Example 4: Professional Investigation (Multi-Font)

```bash
# Add multiple fonts:
# - Merriweather for body text
# - Bebas Neue for titles
# - Courier Prime for evidence
# Modify FontManager.java as shown in Advanced section
```

## Troubleshooting

### Font Not Loading

**Symptom:** App uses pixelated BitmapFont fallback

**Solutions:**
1. Check filename is exactly `font.ttf`
2. Ensure file is in `assets/` directory
3. Check file is valid TTF format: `file assets/font.ttf`
4. Look for errors in logs: `adb logcat | grep FontManager`

### Font Looks Pixelated

**Symptom:** Text has jagged edges

**Solutions:**
1. Ensure using TTF file (not bitmap font)
2. Check font file exists in assets
3. Verify FreeTypeFontGenerator is being used
4. Look for "Using BitmapFont fallback" in logs

### Font Missing Characters

**Symptom:** Some characters show as boxes or missing

**Solutions:**
1. Choose a font with broader character support
2. Test font with your game's text
3. Use Google Fonts (usually comprehensive)
4. Check font's character map before using

### Font Too Large/Small

**Symptom:** Font size doesn't match expectations

**Solutions:**
1. Font sizes are in density-independent pixels (dp)
2. Actual pixel size = dp × screen density
3. Sizes are set in FontManager.java (lines 53-56)
4. Can be adjusted if needed:
   ```java
   private static final int TITLE_SIZE_DP = 40;    // Adjust this
   private static final int SUBTITLE_SIZE_DP = 30; // And this
   private static final int BODY_SIZE_DP = 22;     // And this
   private static final int SMALL_SIZE_DP = 18;    // And this
   ```

### Performance Issues

**Symptom:** App slower with new font

**Solutions:**
1. Use smaller font files (< 500KB if possible)
2. Enable mipmap generation (already enabled)
3. Avoid generating custom fonts repeatedly
4. Use pre-generated fonts from FontManager

## Testing Your Font

### Build and Test

```bash
# Android
./gradlew android:assembleDebug
./gradlew android:installDebug
adb logcat | grep FontManager

# Desktop
./gradlew lwjgl3:run
```

### What to Check

1. **All Screens:**
   - Splash screen
   - Login screen
   - Profile creation
   - Character attributes
   - Main game

2. **All Font Sizes:**
   - Title (40dp) - large headers
   - Subtitle (30dp) - section headers
   - Body (22dp) - regular text
   - Small (18dp) - details

3. **All Elements:**
   - Buttons
   - Labels
   - Input fields
   - Lists

4. **Readability:**
   - Small text is clear
   - Body text is comfortable
   - Titles are impactful

## Font Licenses

**Important:** Always check font licenses before use!

### Common Licenses

**OFL (Open Font License):**
- Free for personal and commercial use
- Can modify and redistribute
- Most Google Fonts use this

**Apache 2.0:**
- Free for personal and commercial use
- Very permissive
- Used by Roboto and others

**CC0 (Creative Commons Zero):**
- Public domain
- No restrictions

**100% Free:**
- Check Font Squirrel
- Explicitly marked "100% Free"

### What to Avoid

**Freeware (Personal Use Only):**
- Cannot use in commercial projects
- Read license carefully

**Shareware/Demo:**
- Limited use
- Usually requires purchase

**Unlicensed:**
- No clear license
- Don't use in commercial projects

## Summary

### Quick Steps to Change Font

1. Download a TTF font (Google Fonts recommended)
2. Rename to `font.ttf`
3. Replace `assets/font.ttf`
4. Rebuild: `./gradlew android:assembleDebug`
5. Test all screens

### Recommended Fonts for Detective Game

- **Classic:** Crimson Text, Merriweather
- **Modern:** Roboto (current), Open Sans
- **Vintage:** Courier Prime
- **Display:** Bebas Neue, Oswald

### Resources

- Google Fonts: https://fonts.google.com/
- Font Squirrel: https://www.fontsquirrel.com/
- FontManager: `core/src/main/java/eb/gmodel1/FontManager.java`

## Need Help?

If you encounter issues:
1. Check the logs: `adb logcat | grep FontManager`
2. Verify font file is valid TTF
3. Ensure filename is exactly `font.ttf`
4. Try a different font from Google Fonts
5. Check this guide's troubleshooting section

**The font system is flexible and powerful - experiment to find the perfect look for your detective game!**

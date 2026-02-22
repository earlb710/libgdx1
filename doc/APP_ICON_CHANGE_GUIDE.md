# App Icon Change Guide

## Overview

Yes, the app icon can be changed! This guide explains how to replace the default LibGDX icon with your own custom icon for the Veritas Detegere detective game.

## Current Icon Files

The app uses different icon files for different platforms and screen densities:

### Android Icons

Located in `android/res/drawable-*/ic_launcher.png`:

| Density | Size | File Path |
|---------|------|-----------|
| mdpi | 48×48 px | `android/res/drawable-mdpi/ic_launcher.png` |
| hdpi | 72×72 px | `android/res/drawable-hdpi/ic_launcher.png` |
| xhdpi | 96×96 px | `android/res/drawable-xhdpi/ic_launcher.png` |
| xxhdpi | 144×144 px | `android/res/drawable-xxhdpi/ic_launcher.png` |
| xxxhdpi | 192×192 px | `android/res/drawable-xxxhdpi/ic_launcher.png` |
| Web/Play Store | 512×512 px | `android/ic_launcher-web.png` |

### Android Adaptive Icons (API 26+)

Modern Android devices (8.0+) use adaptive icons with separate foreground and background layers:

- **XML Definition:** `android/res/drawable-anydpi-v26/ic_launcher.xml`
- **Foreground:** `android/res/drawable-anydpi-v26/ic_launcher_foreground.xml`
- **Background Color:** Defined in `android/res/values/ic_background_color.xml`

### Desktop Icon

Desktop applications use window icons configured in `Lwjgl3Launcher.java`:
- Currently commented out (line 35)
- Would use files like: `libgdx128.png`, `libgdx64.png`, etc.

## How to Change the App Icon

### Step 1: Create Your Icon

**Design Requirements:**
- **Square format** (1:1 aspect ratio)
- **Transparent background** (recommended for adaptive icons)
- **Clear, simple design** that works at small sizes
- **High resolution source** (at least 512×512 px)

**Detective Game Theme Ideas:**
- Magnifying glass
- Detective badge
- Fingerprint
- Clue marker
- Detective hat/coat silhouette

### Step 2: Generate Icon Sizes

Create versions of your icon in all required sizes:

**Option A: Manual Creation**
Using image editing software (Photoshop, GIMP, etc.):
1. Start with 512×512 px version
2. Scale down to each required size
3. Export as PNG with transparency
4. Optimize for each size (sharpen details as needed)

**Option B: Online Tools**
Use online Android icon generators:
- Android Asset Studio: https://romannurik.github.io/AndroidAssetStudio/
- AppIcon.co: https://appicon.co/
- MakeAppIcon: https://makeappicon.com/

These tools can generate all sizes from a single source image.

### Step 3: Replace Android Icons

**Standard Icons:**
```bash
# Replace each density's icon file
android/res/drawable-mdpi/ic_launcher.png     (48×48)
android/res/drawable-hdpi/ic_launcher.png     (72×72)
android/res/drawable-xhdpi/ic_launcher.png    (96×96)
android/res/drawable-xxhdpi/ic_launcher.png   (144×144)
android/res/drawable-xxxhdpi/ic_launcher.png  (192×192)
android/ic_launcher-web.png                   (512×512)
```

**Important:** Keep the same file names! Android looks for files by name.

### Step 4: Update Adaptive Icons (Optional but Recommended)

For modern Android (8.0+), update the adaptive icon:

**Option A: Simple Approach**
1. Replace `ic_launcher_foreground.xml` with a simple drawable
2. Or create PNG versions in each drawable-* folder
3. Update background color in `android/res/values/ic_background_color.xml`

**Option B: Custom Vector Foreground**
1. Create a vector drawable (XML) for the foreground
2. Edit `android/res/drawable-anydpi-v26/ic_launcher_foreground.xml`
3. Use vector paths for scalable icon

**Example ic_background_color.xml:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_background_color">#1A1A2E</color>  <!-- Dark blue-gray -->
</resources>
```

### Step 5: Desktop Icon (Optional)

To add a desktop window icon:

1. **Create Icon Images:**
   - Create PNG files: 128×128, 64×64, 32×32, 16×16
   - Name them: `icon128.png`, `icon64.png`, `icon32.png`, `icon16.png`
   - Place in `assets/` directory

2. **Update Lwjgl3Launcher.java:**
   ```java
   // Uncomment and update line 35:
   configuration.setWindowIcon("icon128.png", "icon64.png", "icon32.png", "icon16.png");
   ```

3. **Build and Test:**
   ```bash
   ./gradlew lwjgl3:run
   ```

## Example: Creating a Detective Badge Icon

Here's a complete example for creating a detective-themed icon:

### 1. Design Concept
- Badge shape (shield or circle)
- Detective symbol (magnifying glass)
- Color scheme: Gold/brass for badge, dark background

### 2. Create SVG/Vector Source
```
- Base: Gold badge shape
- Center: Magnifying glass symbol
- Text: "VD" (Veritas Detegere initials)
```

### 3. Export Sizes
Using your vector graphics program:
- 512×512 for web
- 192×192 for xxxhdpi
- 144×144 for xxhdpi
- 96×96 for xhdpi
- 72×72 for hdpi
- 48×48 for mdpi

### 4. Place Files
```bash
cp detective-badge-512.png android/ic_launcher-web.png
cp detective-badge-192.png android/res/drawable-xxxhdpi/ic_launcher.png
cp detective-badge-144.png android/res/drawable-xxhdpi/ic_launcher.png
cp detective-badge-96.png android/res/drawable-xhdpi/ic_launcher.png
cp detective-badge-72.png android/res/drawable-hdpi/ic_launcher.png
cp detective-badge-48.png android/res/drawable-mdpi/ic_launcher.png
```

## Testing Your Icon

### Android
1. Build the APK:
   ```bash
   ./gradlew android:assembleDebug
   ```

2. Install on device/emulator:
   ```bash
   ./gradlew android:installDebug
   ```

3. Check the app drawer to see your new icon

### Desktop
1. Run the desktop version:
   ```bash
   ./gradlew lwjgl3:run
   ```

2. Check the window title bar icon (if configured)

## Important Notes

### Icon Design Best Practices

1. **Keep It Simple**
   - Icons should be recognizable at small sizes (48×48 px)
   - Avoid fine details that disappear when scaled down
   - Use bold, clear shapes

2. **Test at Multiple Sizes**
   - View your icon at 48px, 72px, 96px to ensure clarity
   - Details visible at 512px may be lost at smaller sizes

3. **Use Appropriate Contrast**
   - Ensure icon is visible on both light and dark backgrounds
   - Consider adding a subtle outline or shadow

4. **Adaptive Icon Safe Zone**
   - Keep important content in center 66% of canvas
   - Outer edges may be masked into circles or other shapes

### File Format Requirements

- **Format:** PNG (Portable Network Graphics)
- **Transparency:** Supported (recommended for adaptive icons)
- **Color Depth:** 32-bit (RGBA) recommended
- **Compression:** PNG compression is fine

### Common Issues

**Icon appears blurry:**
- Ensure you're creating icons at exact required sizes
- Don't rely on automatic scaling
- Use proper image editing software

**Icon not updating:**
- Uninstall the app completely before reinstalling
- Clear Android build cache: `./gradlew clean`
- On some devices, launcher icon cache may need clearing

**Adaptive icon looks cut off:**
- Remember the safe zone (center 66%)
- Test with circular, square, and squircle masks
- Use Android Studio's adaptive icon preview

## Resources

### Icon Design Tools
- **Inkscape** (Free vector graphics): https://inkscape.org/
- **GIMP** (Free raster graphics): https://www.gimp.org/
- **Figma** (Free online design): https://www.figma.com/

### Icon Generators
- **Android Asset Studio**: https://romannurik.github.io/AndroidAssetStudio/
- **IconKitchen**: https://icon.kitchen/

### Android Documentation
- **Icon Design Guidelines**: https://developer.android.com/guide/practices/ui_guidelines/icon_design_launcher
- **Adaptive Icons**: https://developer.android.com/guide/practices/ui_guidelines/icon_design_adaptive

## Quick Reference

**Minimum Required Changes:**
```
1. Replace all 6 Android icon PNGs (mdpi through xxxhdpi + web)
2. Rebuild: ./gradlew android:assembleDebug
3. Install: ./gradlew android:installDebug
4. Check app drawer for new icon
```

**Recommended Changes:**
```
1. Replace all Android icons
2. Update adaptive icon foreground
3. Update background color
4. Add desktop icons
5. Test on multiple devices
```

## Conclusion

Yes, the app icon can definitely be changed! Follow this guide to replace the default LibGDX icon with your custom detective game icon. The process involves creating icons at multiple sizes and replacing the existing PNG files in the Android resource directories.

For a professional result, use a vector graphics program to create a scalable icon design, then export it at all required sizes. Test your icon on actual devices to ensure it looks good at all sizes and on different Android versions.

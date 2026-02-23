# Font Hierarchy Fix - Profile Screens

## Problem Statement

User reported issues with ProfileCreationScreen fonts:

> "Label fonts like 'character name' is small, the text item you type in is correct size but pixelated; button sizes way smaller than the label for the button"

### Specific Issues

1. **Labels too small** - Field labels like "Character Name:" were only 24dp
2. **Input text pixelated** - Text typed by user was scaled 7x causing severe pixelation
3. **Button text too small** - Button text was 18dp, smaller than labels (24dp)

## Root Cause Analysis

### ProfileCreationScreen Issues

**Line 79:** Labels used subtitleFont (24dp)
```java
this.labelFont = fontManager.getSubtitleFont();  // 24dp - too small
```

**Line 78:** Input text used bodyFont (18dp)
```java
this.font = fontManager.getBodyFont();  // 18dp - then scaled badly
```

**Lines 199-200:** **CRITICAL BUG** - Rogue scaling
```java
batch.begin();
font.getData().setScale(7.0f);  // ← SEVERE PIXELATION!
batch.end();
```

This scaled bodyFont (18dp) by 7x, resulting in massively pixelated text.

**Line 242:** Button text used bodyFont (18dp)
```java
font.draw(batch, text, textX, textY);  // 18dp - smaller than labels
```

### The Problem

1. Labels at 24dp were too small to be prominent
2. Input text was scaled 7x from 18dp → severe pixelation
3. Button text at 18dp was smaller than labels → inconsistent hierarchy
4. No clear visual hierarchy

## Solution

### Font Hierarchy Established

Created a clear 4-tier font system:

| Tier | Font Type | Size (dp) | Purpose | Usage |
|------|-----------|-----------|---------|-------|
| 1 | titleFont | 32 | Titles, headers | Screen titles |
| 2 | labelFont (titleFont) | 32 | Field labels | "Character Name:", "Gender:" |
| 3 | Input/Button (subtitleFont) | 24 | Interactive elements | User input, button text |
| 4 | Details (bodyFont/smallFont) | 18/14 | Secondary info | Profile details, hints |

### Implementation

**ProfileCreationScreen.java Changes:**

1. **Added buttonFont field** for button text
```java
private BitmapFont buttonFont;    // For button text
```

2. **Changed labelFont to titleFont** (24dp → 32dp)
```java
this.labelFont = fontManager.getTitleFont();  // 32dp - prominent
```

3. **Changed font to subtitleFont** (18dp → 24dp)
```java
this.font = fontManager.getSubtitleFont();  // 24dp - crisp, readable
```

4. **Added buttonFont initialization**
```java
this.buttonFont = fontManager.getSubtitleFont();  // 24dp for buttons
```

5. **REMOVED lines 199-200** - The pixelation bug
```java
// DELETED:
// batch.begin();
// font.getData().setScale(7.0f);
// batch.end();
```

6. **Updated drawButton() to use buttonFont**
```java
buttonFont.draw(batch, text, textX, textY);  // Was: font.draw()
```

**ProfileSelectionScreen.java Changes:**

1. **Added buttonFont field**
2. **Changed titleFont to titleFont** for consistency
3. **Updated button rendering** to use buttonFont

## Results

### Before Fix

**For 1080x2400 @ density 3.0:**
- Labels: 24 × 3.0 = 72px (too small)
- Input: 18 × 3.0 × 7.0 = 378px (MASSIVE and pixelated!)
- Buttons: 18 × 3.0 = 54px (too small, inconsistent)

### After Fix

**For 1080x2400 @ density 3.0:**
- Labels: 32 × 3.0 = 96px (large, prominent ✅)
- Input: 24 × 3.0 = 72px (readable, crisp ✅)
- Buttons: 24 × 3.0 = 72px (consistent with input ✅)

## Font Usage Guide

### ProfileCreationScreen

| Element | Font | Size | Code |
|---------|------|------|------|
| "Create Profile" | titleFont | 32dp | `fontManager.getTitleFont()` |
| "Character Name:" | labelFont (titleFont) | 32dp | `labelFont.draw(batch, ...)` |
| User input text | font (subtitleFont) | 24dp | `font.draw(batch, characterText, ...)` |
| "Male", "Female" | buttonFont (subtitleFont) | 24dp | `buttonFont.draw(batch, text, ...)` |
| "Create", "Cancel" | buttonFont (subtitleFont) | 24dp | `buttonFont.draw(batch, text, ...)` |

### ProfileSelectionScreen

| Element | Font | Size | Code |
|---------|------|------|------|
| Screen title | titleFont | 32dp | `fontManager.getTitleFont()` |
| Profile name | subtitleFont | 24dp | `fontManager.getSubtitleFont()` |
| Profile details | smallFont | 14dp | `fontManager.getSmallFont()` |
| Button text | buttonFont (subtitleFont) | 24dp | `buttonFont.draw(batch, ...)` |

## Visual Hierarchy Principles

### Size Ratios

The font sizes follow a clear hierarchy:
- **32dp** : Prominent elements (titles, labels)
- **24dp** : Interactive elements (input, buttons)
- **18dp** : Body text and details
- **14dp** : Small text and hints

**Ratio:** 32:24:18:14 ≈ 2.3:1.7:1.3:1

This provides clear visual distinction while maintaining readability.

### Consistency Rules

1. **Labels should be prominent** - Use titleFont (32dp)
2. **Interactive elements should match** - Use same font for input and buttons
3. **Details should be smaller** - Use bodyFont or smallFont
4. **Never scale fonts manually** - Use FontManager's pre-generated fonts

## Testing & Verification

### Visual Checks

1. **Labels are prominent** - "Character Name:", "Gender:", etc. should be large and clear
2. **Input text is crisp** - No pixelation or blurriness
3. **Button text matches input** - Same visual size
4. **No scaling artifacts** - All text should be sharp

### Size Verification

For a 1080x2400 device @ density 3.0:
- Labels should be ~96px (clearly visible)
- Input/Buttons should be ~72px (readable)
- Details should be ~54px (appropriate for secondary info)

### Code Checks

1. **No manual setScale() calls** - Check that no code manually scales fonts
2. **Consistent font usage** - All buttons use buttonFont, all labels use labelFont
3. **FontManager fonts only** - Don't create new BitmapFont instances

## Best Practices

### When to Use Each Font

**titleFont (32dp):**
- Screen titles
- Section headers
- Field labels that need prominence

**subtitleFont (24dp):**
- User input fields
- Button text
- Interactive element labels

**bodyFont (18dp):**
- Regular body text
- Profile details
- Secondary information

**smallFont (14dp):**
- Help text
- Hints and tips
- Very small details

### Maintaining Consistency

1. **Always use FontManager fonts** - Don't create your own
2. **Never call setScale()** - Use pre-generated fonts at correct sizes
3. **Match interactive elements** - Input and buttons should use same font
4. **Make labels prominent** - Use larger fonts for field labels

### Future Development

When adding new screens:
1. Get fonts from FontManager
2. Use titleFont for labels
3. Use subtitleFont for input/buttons
4. Use bodyFont/smallFont for details
5. Never manually scale fonts

## Summary

### What Was Fixed

✅ **Removed critical bug** - Deleted rogue setScale(7.0f) causing pixelation  
✅ **Increased label size** - Changed from 24dp to 32dp  
✅ **Increased input size** - Changed from 18dp to 24dp  
✅ **Increased button size** - Changed from 18dp to 24dp  
✅ **Established hierarchy** - Clear 4-tier font system  
✅ **Improved consistency** - Input and buttons now match  

### Benefits

- **No pixelation** - All fonts crisp and clear
- **Better readability** - Larger fonts easier to read
- **Professional appearance** - Proper visual hierarchy
- **Consistent sizing** - Related elements use same fonts
- **Maintainable code** - Clear font usage patterns

### Status

🟢 **COMPLETE** - All font issues in profile screens resolved!

The Veritas Detegere game now has professional, readable fonts with proper visual hierarchy.

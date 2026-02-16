# Veritas Detegere - Splash Screen Implementation

## Overview
This document describes the splash screen implementation for "Veritas Detegere" (Latin for "Uncover Truth"), a detective game built with LibGDX.

## Game Flow

### For New Users
1. **LoginScreen** - User enters their name
2. **SplashScreen** - Game title screen with Play/Quit buttons
3. **MainScreen** - Main game (when Play is clicked)

### For Returning Users
1. **SplashScreen** - Game title screen with Play/Quit buttons (login skipped)
2. **MainScreen** - Main game (when Play is clicked)

## SplashScreen Features

### Visual Elements
- **Title**: "Veritas Detegere" in gold color (scale 3.0x)
- **Subtitle**: "A Detective Game" in light gray (scale 1.5x)
- **Play Button**: Rectangular button with hover effect that starts the game
- **Quit Button**: Rectangular button with hover effect that exits the application
- **Dark Background**: (RGB: 0.1, 0.1, 0.15) for a detective game atmosphere

### Interactive Elements

#### Play Button
- **Action**: Transitions to MainScreen (starts the game)
- **Position**: Center of screen, slightly above center
- **Size**: 200px width × 60px height
- **Hover Effect**: Changes from dark gray to lighter gray on mouse hover

#### Quit Button
- **Action**: Exits the application using `Gdx.app.exit()`
- **Position**: Center of screen, below the Play button
- **Size**: 200px width × 60px height
- **Hover Effect**: Changes from dark gray to lighter gray on mouse hover

### Technical Implementation

#### Text Rendering
- Uses `GlyphLayout` for accurate text measurement and centering
- No magic numbers - all positioning is calculated based on actual text dimensions
- Static constants for game title and subtitle to avoid per-frame allocations

#### Button Rendering
- Helper methods (`drawButton`, `drawButtonText`) to eliminate code duplication
- Proper mouse coordinate handling with Y-axis flipping
- Rectangle-based hit detection for button clicks

#### Resource Management
- All resources (fonts, batch, shape renderer) initialized in `show()`
- Proper disposal of all resources in `dispose()` with null checks
- Separate fonts for title, subtitle, and buttons to avoid state mutation

#### Screen Lifecycle
- Proper implementation of all Screen interface methods
- Responsive design: buttons repositioned on window resize
- Clean separation of concerns: input handling, rendering, and drawing

## Code Quality

### Best Practices
✅ No code duplication (extracted helper methods)
✅ No magic numbers (constants and calculated positions)
✅ Proper resource management
✅ Separation of concerns
✅ Null-safe disposal
✅ GlyphLayout for accurate text rendering
✅ Static constants for strings (no per-frame allocations)

### Constants
```java
private static final String GAME_TITLE = "Veritas Detegere";
private static final String GAME_SUBTITLE = "A Detective Game";
private static final int BUTTON_WIDTH = 200;
private static final int BUTTON_HEIGHT = 60;
```

### Helper Methods
- `drawButtons()`: Coordinates drawing of all buttons
- `drawButton(Rectangle, int, int)`: Draws a single button with hover effect
- `drawButtonText(Rectangle, String)`: Renders centered text on a button
- `getFlippedMouseY()`: Converts screen Y coordinate to world Y coordinate

## Files Modified

1. **SplashScreen.java** (NEW) - The splash screen implementation
2. **LoginScreen.java** - Updated to transition to SplashScreen instead of MainScreen
3. **Main.java** - Updated to show SplashScreen for returning users

## Security
- CodeQL analysis passed with 0 alerts
- No security vulnerabilities detected

## Future Enhancements (Optional)
- Custom font files for more thematic typography
- Background image or texture
- Animated title or buttons
- Sound effects on button hover/click
- Fade-in/fade-out transitions
- Options/Settings button
- Credits button

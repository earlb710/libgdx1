# Critical Bug Fix: Splash Screen Buttons

## The Problem

**User Report:** "The splash screen buttons did not move, they still exactly where they were"

Despite commits showing button positions at `centerY - 200` and `centerY - 280`, the buttons appeared at the OLD positions (`centerY - 20` and `centerY - 100`).

## Root Cause

The bug was in the `resize()` method of `SplashScreen.java`:

```java
// Lines 241-242 (BEFORE FIX):
public void resize(int width, int height) {
    int centerX = width / 2;
    int centerY = height / 2;
    
    playButton.setPosition(centerX - BUTTON_WIDTH / 2, centerY - 20);   // OLD VALUE!
    quitButton.setPosition(centerX - BUTTON_WIDTH / 2, centerY - 100);  // OLD VALUE!
}
```

### Why This Caused the Bug

1. **Constructor/show() method** set buttons to NEW positions (centerY - 200, -280)
2. **LibGDX calls resize()** when:
   - Screen is first shown
   - Window is resized
   - Screen orientation changes
3. **resize() overwrote positions** with OLD values (centerY - 20, -100)
4. **User saw OLD positions**, not the NEW ones set in constructor

This is a classic bug pattern: initialization sets correct values, but event handler resets them.

## The Fix

Updated `resize()` method to use the CORRECT positions:

```java
// Lines 241-242 (AFTER FIX):
public void resize(int width, int height) {
    int centerX = width / 2;
    int centerY = height / 2;
    
    // Use the same positions as in show() method - generous spacing to avoid text overlap
    playButton.setPosition(centerX - BUTTON_WIDTH / 2, centerY - 200);  // FIXED!
    quitButton.setPosition(centerX - BUTTON_WIDTH / 2, centerY - 280);  // FIXED!
}
```

## Impact

### Before Fix
- Buttons appeared at centerY - 20 and centerY - 100
- Only ~55px gap from subtitle
- Potential overlap with subtitle text
- User confusion: "Why aren't my changes working?"

### After Fix
- Buttons appear at centerY - 200 and centerY - 280
- Generous 175px gap from subtitle
- No overlap with subtitle text
- Consistent behavior: what you commit is what you see

## Lesson Learned

**Always check event handlers and lifecycle methods!**

When working with LibGDX screens:
- `show()` - Called when screen becomes active
- `resize()` - Called when screen dimensions change
- Both methods must set the same positions for consistency

If you set positions in one method but not the other, the last method called will determine the actual positions.

## Testing

To verify the fix works:

1. Run the application:
   ```bash
   ./gradlew android:installDebug
   # or
   ./gradlew lwjgl3:run
   ```

2. Check the splash screen

3. Buttons should be far below the subtitle with generous spacing

4. Verify positions:
   - Play button: 175px below subtitle bottom
   - Quit button: 80px below Play button

## Files Changed

- `core/src/main/java/eb/framework1/SplashScreen.java` - Line 241-242

## Commit

Commit: dccc246
Message: "Fix critical bug: resize method was resetting button positions to old values"

## Related Issues

This fix addresses the user's report and ensures all previous button positioning work is actually visible in the running application.

Previous attempts to move buttons:
1. First move: centerY - 80 and centerY - 160 (55px gap)
2. Second move: centerY - 200 and centerY - 280 (175px gap)

Both attempts were correct in the constructor but were being overridden by resize().
Now resize() is fixed, so the generous spacing is actually applied!

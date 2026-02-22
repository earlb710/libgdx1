# How to Collect and Analyze Application Logs

## Purpose

This guide explains how to collect logs from the Android application to debug the Play button crash issue.

## Comprehensive Logging Added

The application now includes extensive logging at every step of the screen transition process:

### What Gets Logged

1. **Application Startup** (Main.java)
   - UserManager creation
   - ProfileManager creation  
   - Initial screen selection

2. **Screen Transitions** (Main.setScreen)
   - Which screen is being set
   - Old screen disposal

3. **Play Button Click** (SplashScreen.handleInput)
   - Button click detection
   - Profile existence check
   - Screen creation start/completion

4. **Profile Screen Initialization** (ProfileCreationScreen/ProfileSelectionScreen)
   - Constructor entry/exit
   - show() method execution
   - Resource creation (SpriteBatch, fonts, etc.)
   - Button position calculations
   - Screen dimensions
   - Any exceptions with full stack traces

5. **Profile Management** (ProfileManager)
   - Profile loading
   - JSON parsing
   - Profile count

## How to Collect Logs on Android

### Method 1: Using Android Studio

1. **Open Android Studio**
2. **Connect your device or start emulator**
3. **Open Logcat**:
   - View → Tool Windows → Logcat
   - Or use shortcut: Alt+6 (Windows/Linux) or Cmd+6 (Mac)

4. **Filter for application logs**:
   - In the filter box, enter: `tag:Main|SplashScreen|ProfileCreationScreen|ProfileSelectionScreen|ProfileManager`
   - Or select your app package from the dropdown

5. **Run the application**:
   - Deploy and run from Android Studio
   - Or launch the installed app

6. **Reproduce the issue**:
   - Navigate to the splash screen
   - Click the Play button
   - Wait for crash

7. **Save the logs**:
   - Right-click in Logcat → Copy
   - Or File → Save As to save to file

### Method 2: Using ADB Command Line

1. **Clear existing logs** (optional):
   ```bash
   adb logcat -c
   ```

2. **Start logging to file**:
   ```bash
   adb logcat > app_logs.txt
   ```
   Or for filtered logs:
   ```bash
   adb logcat | grep -E "Main|SplashScreen|ProfileCreationScreen|ProfileSelectionScreen|ProfileManager" > app_logs.txt
   ```

3. **Run the application** and reproduce the crash

4. **Stop logging**: Press Ctrl+C

5. **View the logs**:
   ```bash
   cat app_logs.txt
   ```

### Method 3: Using Built-in Bug Report

Android automatically creates bug reports on crash. To access:

1. **On device**:
   - Settings → About Phone → Tap "Build Number" 7 times to enable Developer Options
   - Settings → System → Developer Options → Take Bug Report
   - Select "Interactive Report"

2. **Download the bug report**:
   - Notification will appear when ready
   - Share/save the bug report ZIP file

3. **Extract and examine**:
   - Unzip the file
   - Look for logcat files

## Analyzing the Logs

### What to Look For

1. **Last Successful Log Before Crash**
   
   The logs will show a sequence like:
   ```
   I/Main: create() called
   I/Main: Creating ProfileManager...
   I/ProfileManager: Constructor called
   I/ProfileManager: Loading profiles...
   I/SplashScreen: Play button clicked!
   I/ProfileCreationScreen: Constructor called
   I/ProfileCreationScreen: show() called
   I/ProfileCreationScreen: Creating SpriteBatch...
   [CRASH - no more logs after this point]
   ```

   The crash occurs immediately after the last log message.

2. **Exception Stack Traces**

   Look for error messages:
   ```
   E/ProfileCreationScreen: Error in show(): <error message>
       at eb.framework1.ProfileCreationScreen.show(...)
       at com.badlogic.gdx.Game.setScreen(...)
       ...
   ```

   This shows exactly which line caused the problem.

3. **Coordinate Values**

   Check button positions:
   ```
   I/ProfileCreationScreen: Screen: 480x640
   I/ProfileCreationScreen: Center: (240, 320)
   I/ProfileCreationScreen: Button positions - Gender Male: [x,y,w,h]
   I/ProfileCreationScreen: Button positions - Difficulty Hard: [x,y,w,h]
   ```

   Verify all coordinates are within screen bounds:
   - X: 0 to 480
   - Y: 0 to 640

### Common Issues to Check

1. **Off-Screen Coordinates**
   - If button X coordinate > 480 or < 0 → Off-screen right/left
   - If button Y coordinate > 640 or < 0 → Off-screen top/bottom

2. **Missing Resources**
   - "FileNotFoundException" → Asset not found
   - "NullPointerException" → Uninitialized object

3. **Memory Issues**
   - "OutOfMemoryError" → Too many resources
   - May need to check resource disposal

4. **OpenGL Errors**
   - "GLException" → Graphics rendering issue
   - Usually from invalid coordinates or states

## Example Log Analysis

### Successful Flow (No Crash)
```
I/Main: create() called
I/Main: Creating UserManager...
I/Main: Creating ProfileManager...
I/ProfileManager: Constructor called
I/ProfileManager: Loading profiles...
I/ProfileManager: Loaded 0 profiles successfully
I/Main: User exists, showing SplashScreen
I/Main: setScreen() called with: SplashScreen
I/Main: Calling super.setScreen()...
I/Main: setScreen() completed successfully
I/SplashScreen: Play button clicked!
I/SplashScreen: Checking if profiles exist...
I/ProfileManager: hasProfiles() returning: false
I/SplashScreen: Has profiles: false
I/SplashScreen: Creating ProfileCreationScreen...
I/ProfileCreationScreen: Constructor called
I/ProfileCreationScreen: Constructor completed successfully
I/Main: setScreen() called with: ProfileCreationScreen
I/ProfileCreationScreen: show() called
I/ProfileCreationScreen: Creating SpriteBatch...
I/ProfileCreationScreen: Creating ShapeRenderer...
I/ProfileCreationScreen: Creating fonts...
I/ProfileCreationScreen: Screen: 480x640
I/ProfileCreationScreen: Center: (240, 320)
I/ProfileCreationScreen: Creating buttons...
I/ProfileCreationScreen: Button positions - Gender Male: [130,330,100,50]
I/ProfileCreationScreen: Button positions - Difficulty Hard: [190,140,100,50]
I/ProfileCreationScreen: Setting up input processor...
I/ProfileCreationScreen: show() completed successfully
I/SplashScreen: ProfileCreationScreen created and set
```

### Failed Flow (With Crash)
```
I/Main: create() called
I/Main: Creating ProfileManager...
I/ProfileManager: Constructor called
I/ProfileManager: Loading profiles...
I/ProfileManager: Loaded 0 profiles successfully
I/SplashScreen: Play button clicked!
I/SplashScreen: Creating ProfileCreationScreen...
I/ProfileCreationScreen: Constructor called
I/ProfileCreationScreen: show() called
I/ProfileCreationScreen: Creating SpriteBatch...
[CRASH - Application stops here]
```

**Analysis**: Crash occurs during SpriteBatch creation. Possible OpenGL initialization issue.

## Sharing Logs for Analysis

When sharing logs with the development team:

1. **Include the complete log sequence** from app start to crash
2. **Note the timestamp** of the crash
3. **Include any exception messages** and stack traces
4. **Mention device information**:
   - Device model
   - Android version
   - Screen resolution
5. **Describe exact steps to reproduce**

## Next Steps After Log Collection

1. **Identify the crash point** - Last successful log message
2. **Check for exceptions** - Any error messages or stack traces
3. **Verify coordinates** - All button positions within bounds
4. **Report findings** - Share log excerpt showing the issue
5. **Proposed fix** - Based on log analysis

## Filtering Logs by Tag

To focus on specific components:

### Application Logs Only
```bash
adb logcat | grep "I/Main\|I/SplashScreen\|I/ProfileCreationScreen\|I/ProfileSelectionScreen\|I/ProfileManager\|E/"
```

### Error Logs Only
```bash
adb logcat *:E
```

### Specific Component
```bash
adb logcat ProfileCreationScreen:* *:S
```

## Continuous Logging

To capture logs over time (useful if crash is intermittent):

```bash
adb logcat -v time > logs_$(date +%Y%m%d_%H%M%S).txt
```

This creates a timestamped log file.

## Conclusion

With comprehensive logging in place, we can now:
- ✅ See exactly where the crash occurs
- ✅ Identify which resource fails to initialize
- ✅ Verify button coordinates are correct
- ✅ Catch and log any exceptions
- ✅ Track the complete execution flow

The logs will reveal the exact cause of the crash and allow for a targeted fix.

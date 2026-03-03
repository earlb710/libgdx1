# Login Screen Feature

## Overview
This implementation adds a login screen that appears when the current user is unknown. The user's name is persisted using LibGDX Preferences, so returning users will bypass the login screen.

## Components

### UserManager (45 lines)
- Manages user persistence using LibGDX Preferences
- Stores the current username in `framework1.preferences`
- Provides methods to check if a user exists and to set/get the current user
- **Validation:**
  - Null check on username parameter
  - Trims whitespace on both save and load
  - Rejects empty usernames (after trimming)
  - Clears empty strings when loading from preferences

### LoginScreen (155 lines)
- Displays a simple login form with a text input field
- Uses LibGDX's InputProcessor (InputAdapter) for text input handling
- Supports alphanumeric characters and spaces
- Username validation: minimum 2 characters (MIN_USERNAME_LENGTH), maximum 20 characters (MAX_USERNAME_LENGTH)
- Features a blinking cursor for better user experience (0.5s blink interval)
- Resources initialized in show(), disposed in dispose() (LibGDX best practices)
- Input processor cleared in hide() to prevent memory leaks
- Re-validates username length after trimming before saving

### MainScreen (56 lines)
- The main application screen (previously the content of Main.java)
- Shows the LibGDX logo as before
- Resources initialized in show(), disposed in dispose()

### Main (36 lines)
- Converted from ApplicationAdapter to Game to support multiple screens
- Checks on startup if a user exists via UserManager
- Shows LoginScreen if no user is found, otherwise shows MainScreen
- Overrides setScreen() to automatically dispose old screens (standard LibGDX pattern)
- Prevents resource leaks by managing screen lifecycle properly

## User Flow
1. On first launch, the application checks if a username is stored in preferences
2. If no username exists (or it's empty after trimming), the LoginScreen is displayed
3. User enters their name (minimum 2 characters after trimming) and presses ENTER
4. Username is trimmed, validated, and saved to preferences
5. MainScreen is displayed with the LibGDX logo
6. On subsequent launches, the MainScreen is shown directly (user is known)

## Technical Details

### Screen Management
- Uses LibGDX's Screen interface for proper screen lifecycle management
- Resources allocated in show() and deallocated in dispose()
- Main.setScreen() override ensures old screens are disposed to prevent leaks
- Input processors cleared when screens are hidden

### Validation
- Username minimum length: 2 characters (after trimming)
- Username maximum length: 20 characters (raw input, to prevent UI overflow)
- Whitespace is trimmed from both ends before saving
- Empty strings (after trimming) are rejected
- Validation happens at multiple points for consistency:
  - Input handler checks trimmed length before allowing login
  - login() method re-validates after trimming
  - UserManager.setCurrentUser() validates and throws IllegalArgumentException for invalid input

### Security
- CodeQL analysis passed with 0 alerts
- No sensitive data exposure
- Input validation prevents empty or whitespace-only usernames
- Preferences API used for local storage (platform-appropriate location)

## Testing
To test this feature:
1. Run the application (`./gradlew lwjgl3:run`)
2. First launch should show the login screen
3. Enter a username (minimum 2 characters) and press ENTER
4. The main screen with LibGDX logo should appear
5. Close and restart the application
6. The login screen should be skipped and main screen shown directly

To reset and see the login screen again:
- Delete the preferences file (location varies by platform)
  - Windows: User home directory/.prefs/framework1.preferences
  - Linux: User home directory/.prefs/framework1.preferences
  - macOS: User home directory/.prefs/framework1.preferences
- Or modify UserManager to clear the stored username programmatically

## Code Statistics
- Total lines of code: 292
- Files created: 4 (Main.java modified, UserManager.java, LoginScreen.java, MainScreen.java created)
- Commits: 13 (including iterative improvements and fixes)
- Security alerts: 0 (CodeQL passed)

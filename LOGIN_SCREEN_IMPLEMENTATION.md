# Login Screen Feature

## Overview
This implementation adds a login screen that appears when the current user is unknown. The user's name is persisted using LibGDX Preferences, so returning users will bypass the login screen.

## Components

### UserManager
- Manages user persistence using LibGDX Preferences
- Stores the current username in `framework1.preferences`
- Provides methods to check if a user exists and to set/get the current user

### LoginScreen
- Displays a simple login form with a text input field
- Uses LibGDX's InputProcessor for text input handling
- Supports alphanumeric characters and spaces
- Maximum username length: 20 characters
- Features a blinking cursor for better user experience

### MainScreen
- The main application screen (previously the content of Main.java)
- Shows the LibGDX logo as before

### Main
- Converted from ApplicationAdapter to Game to support multiple screens
- Checks on startup if a user exists
- Shows LoginScreen if no user is found, otherwise shows MainScreen

## User Flow
1. On first launch, the application checks if a username is stored
2. If no username exists, the LoginScreen is displayed
3. User enters their name and presses ENTER
4. Username is saved to preferences
5. MainScreen is displayed
6. On subsequent launches, the MainScreen is shown directly

## Testing
To test this feature:
1. Run the application (`./gradlew lwjgl3:run`)
2. First launch should show the login screen
3. Enter a username and press ENTER
4. The main screen with LibGDX logo should appear
5. Close and restart the application
6. The login screen should be skipped and main screen shown directly

To reset and see the login screen again:
- Delete the preferences file (location varies by platform)
- Or modify UserManager to clear the stored username

# Profile System Implementation

## Overview
This document describes the profile selection and creation system for the Veritas Detegere game. Players must create or select a profile before starting the game.

## Features

### Profile System
- **Profile Data**: Each profile contains:
  - Character Name (2-20 characters) - serves as both profile identifier and in-game name
  - Gender (Male/Female)
  - Difficulty (Easy/Normal/Hard)

### Profile Management
- Profiles stored in LibGDX Preferences as JSON
- Multiple profiles supported per user
- Selected profile persisted across sessions
- Case-insensitive profile name comparison

## User Flow

### First Time Player
1. Login Screen (enter username)
2. Splash Screen
3. Click "Play" button
4. ProfileCreationScreen (no existing profiles)
5. Create profile with character details
6. MainScreen (game starts)

### Returning Player
1. Splash Screen (login bypassed)
2. Click "Play" button
3. ProfileSelectionScreen (shows all profiles)
4. Select existing profile OR create new profile
5. MainScreen (game starts)

## Screens

### ProfileCreationScreen
**Purpose**: Create a new game profile

**Features**:
- Character Name input field (serves as both profile identifier and in-game character name)
- Gender selection buttons (Male/Female)
- Difficulty selection buttons (Easy/Normal/Hard)
- Visual feedback with hover effects
- Create button (enabled when character name has 2+ characters)
- Cancel button (returns to profile list if profiles exist, splash otherwise)

**Keyboard Shortcuts**:
- Enter: Create profile (if valid)
- Backspace: Delete character

**Validation**:
- Character name must be 2-20 characters
- Character name must be unique (case-insensitive)
- All fields are required

### ProfileSelectionScreen
**Purpose**: Select an existing profile or create a new one

**Features**:
- List of all existing profiles showing:
  - Profile name (large text)
  - Character name, gender, and difficulty (smaller text)
- Each profile is a clickable button
- "+ Create New Profile" button (green)
- Back button to return to splash screen
- Hover effects on all buttons

**Profile Display Format**:
```
[Character Name]
Gender - Difficulty
```

## Technical Implementation

### Class: Profile
Simple data class storing profile information with validation.

```java
Profile(String characterName, String gender, String difficulty)
```

### Class: ProfileManager
Manages all profile operations:

**Methods**:
- `createProfile(characterName, gender, difficulty)` → Profile
- `selectProfile(profile)` → void
- `getSelectedProfile()` → Profile
- `getProfiles()` → List<Profile>
- `hasProfiles()` → boolean
- `getProfileByName(characterName)` → Profile

**Storage**:
- Preferences key: "framework1.profiles"
- JSON format for easy serialization
- Selected profile stored separately

### Integration with Main
The Main class initializes ProfileManager alongside UserManager:

```java
private ProfileManager profileManager;

public void create() {
    userManager = new UserManager();
    profileManager = new ProfileManager();
    // ...
}
```

## Portrait Orientation

The game is configured for portrait orientation:

### Desktop (LWJGL3)
- Window size: 480x640 (width x height)
- Configured in `Lwjgl3Launcher.java`

### Android
- Screen orientation: "portrait"
- Configured in `AndroidManifest.xml`
- Activity attribute: `android:screenOrientation="portrait"`

## Data Persistence

### Profile Storage
Profiles are stored in LibGDX Preferences with the following structure:

```json
{
  "profiles": [
    "{\"characterName\":\"Hero\",\"gender\":\"Male\",\"difficulty\":\"Normal\"}",
    "{\"characterName\":\"Warrior\",\"gender\":\"Female\",\"difficulty\":\"Hard\"}"
  ],
  "selectedProfile": "Hero"
}
```

### Location by Platform
- **Desktop**: User home directory/.prefs/framework1.profiles
- **Android**: SharedPreferences
- **iOS**: NSUserDefaults

## Error Handling

### Profile Creation Errors
- Duplicate profile name: Shows error in log
- Invalid input: Create button remains disabled
- Empty fields: Validation prevents creation

### Profile Loading Errors
- Corrupted JSON: Empty profile list initialized
- Missing selected profile: No profile selected
- Errors logged to console

## Best Practices

### For Players
1. Choose meaningful profile names for easy identification
2. Each profile can have different difficulty settings
3. Profiles are saved automatically

### For Developers
1. Always check `hasProfiles()` before showing profile selection
2. Use `getSelectedProfile()` to access current player data
3. Profile names are case-insensitive for comparison
4. Always call `selectProfile()` after creating or selecting

## Future Enhancements (Optional)

Potential improvements:
- Profile deletion feature
- Profile statistics (play time, achievements)
- Profile avatars/icons
- Profile import/export
- Multiple save slots per profile
- Profile rename feature
- Confirmation dialogs for important actions

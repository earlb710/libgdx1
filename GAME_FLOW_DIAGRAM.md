# Game Flow Diagram - Profile System

## Complete User Journey

```
┌─────────────────────────────────────────────────────────────────────┐
│                         APPLICATION START                            │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
                    ┌──────────────────────────┐
                    │   Check UserManager      │
                    │   Has User?              │
                    └──────────────────────────┘
                          │              │
                     NO   │              │  YES
                          ▼              ▼
              ┌─────────────────┐  ┌─────────────────┐
              │  LoginScreen    │  │  SplashScreen   │
              │  Enter Username │  │  - Logo         │
              │                 │  │  - Title        │
              │  [Submit]       │  │  - [PLAY]       │
              │                 │  │  - [QUIT]       │
              └─────────────────┘  └─────────────────┘
                      │                     │
                      │                     │
                      └──────┬──────────────┘
                             ▼
                   ┌─────────────────┐
                   │  SplashScreen   │
                   │  User logged in │
                   │  Click [PLAY]   │
                   └─────────────────┘
                             │
                             ▼
              ┌──────────────────────────┐
              │   Check ProfileManager   │
              │   Has Profiles?          │
              └──────────────────────────┘
                   │                │
              NO   │                │  YES
                   ▼                ▼
    ┌────────────────────────┐  ┌─────────────────────────┐
    │ ProfileCreationScreen  │  │ ProfileSelectionScreen  │
    │                        │  │                         │
    │ Character Name: [____] │  │ ┌─────────────────────┐ │
    │                        │  │ │ Hero                │ │
    │ Gender:                │  │ │ Male - Normal       │ │
    │  [Male] [Female]       │  │ └─────────────────────┘ │
    │                        │  │                         │
    │ Difficulty:            │  │ ┌─────────────────────┐ │
    │  [Easy]                │  │ │ Mage                │ │
    │  [Normal]              │  │ │ Female - Hard       │ │
    │  [Hard]                │  │ └─────────────────────┘ │
    │                        │  │                         │
    │ [Create] [Cancel]      │  │ [+ Create New Profile]  │
    └────────────────────────┘  │ [Back]                  │
    └────────────────────────┘  │                         │
    │                        │  │ [+ Create New Profile]  │
    └────────────────────────┘  │ [Back]                  │
                   │            └─────────────────────────┘
                   │                     │        │
                   │                     │        │ Select Profile
                   │  Create Profile     │        │ OR Create New
                   │                     │        │
                   └──────────┬──────────┴────────┘
                              ▼
                    ┌─────────────────────┐
                    │   Profile Selected  │
                    │   ProfileManager    │
                    │   selectProfile()   │
                    └─────────────────────┘
                              │
                              ▼
                    ┌─────────────────────┐
                    │    MainScreen       │
                    │  (Game Starts)      │
                    │                     │
                    │  Selected Profile:  │
                    │  - Character Name   │
                    │  - Gender           │
                    │  - Difficulty       │
                    └─────────────────────┘
```

## Screen Descriptions

### 1. LoginScreen
- **When**: First time user (no saved username)
- **Purpose**: Create user account
- **Input**: Username (2-20 characters)
- **Next**: SplashScreen

### 2. SplashScreen
- **When**: User is logged in
- **Purpose**: Main menu / Game title
- **Actions**:
  - PLAY: Navigate to profile system
  - QUIT: Exit application
- **Assets**: logo.png displayed

### 3. ProfileCreationScreen
- **When**: 
  - First time playing (no profiles exist)
  - User clicks "Create New Profile" from selection
- **Purpose**: Create game profile
- **Inputs**:
  - Character Name (serves as both profile identifier and in-game name)
  - Gender: Male / Female (horizontal buttons)
  - Difficulty: Easy / Normal / Hard (vertical stack for portrait mode)
- **Actions**:
  - Create: Save profile and start game
  - Cancel: Back to selection (if profiles exist) or splash
- **Features**: Enter creates profile, visual hover effects
- **Layout**: Optimized for portrait mode (480x640)

### 4. ProfileSelectionScreen
- **When**: Profiles exist and user clicks Play
- **Purpose**: Choose which profile to play
- **Display**: List of profiles with:
  - Character name (profile identifier)
  - Gender and difficulty
- **Actions**:
  - Click profile: Select and start game
  - Create New: Go to creation screen
  - Back: Return to splash

### 5. MainScreen
- **When**: Profile selected
- **Purpose**: Main game
- **Data Available**:
  - Selected profile information
  - Character name for display
  - Difficulty for game settings
  - Gender for character sprite/dialogue

## Portrait Orientation

All screens are designed for portrait mode:
- **Desktop**: 480 x 640 pixels
- **Android**: Portrait orientation locked
- **Aspect Ratio**: 3:4 (0.75)

## Data Flow

```
User Login → UserManager (saves username)
Profile Creation → ProfileManager (saves to JSON)
Profile Selection → ProfileManager (loads and selects)
Game Start → MainScreen (reads selected profile)
```

## Navigation Map

```
LoginScreen ──────────────────────────► SplashScreen
                                             │
                                             ▼ [PLAY]
                                        ProfileManager.hasProfiles()?
                                        │                  │
                                  false │                  │ true
                                        ▼                  ▼
                            ProfileCreationScreen  ProfileSelectionScreen
                                        │                  │
                                        │    ┌─────────────┘
                                        │    │
                                        ▼    ▼
                            Profile Selected/Created
                                        │
                                        ▼
                                  MainScreen (Game)
```

## Session Persistence

### First Launch
1. No user → LoginScreen required
2. No profiles → ProfileCreationScreen required
3. Profile created → Saved to preferences
4. Session: User + Selected Profile stored

### Subsequent Launches
1. User exists → Skip LoginScreen
2. Profiles exist → Show ProfileSelectionScreen
3. Last selected profile remembered (optional auto-select)
4. Direct to game if desired

## File Structure

```
core/src/main/java/eb/framework1/
├── Main.java (manages UserManager + ProfileManager)
├── UserManager.java (username persistence)
├── ProfileManager.java (profile CRUD operations)
├── Profile.java (data model)
├── LoginScreen.java (username entry)
├── SplashScreen.java (main menu)
├── ProfileCreationScreen.java (create profile)
├── ProfileSelectionScreen.java (select profile)
└── MainScreen.java (game)
```

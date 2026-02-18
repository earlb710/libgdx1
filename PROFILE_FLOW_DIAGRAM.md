# Profile System Flow Diagram

## Creating a New Profile

```
┌─────────────────────┐
│  SplashScreen       │
│  (Start)            │
└──────────┬──────────┘
           │ Click "Create Profile"
           ▼
┌─────────────────────┐
│ ProfileCreation     │
│ Screen              │
│ - Enter Name        │
│ - Select Gender     │
│ - Select Difficulty │
└──────────┬──────────┘
           │ Click "Create"
           ▼
┌─────────────────────┐
│ CharacterAttribute  │
│ Screen              │
│ - Allocate Points   │
│ - Set Attributes    │
└──────────┬──────────┘
           │ Click "Confirm"
           │ 
           │ ┌─────────────────────────┐
           │ │ Profile Created:        │
           │ │ - name                  │
           └─│ - gender                │
             │ - difficulty            │
             │ - attributes (map)      │
             │ - gameDate = 2050       │ ← NEW
             │ - randSeed = timestamp  │ ← NEW
             └────────────┬────────────┘
                          │ Save to storage
                          ▼
                  ┌───────────────┐
                  │  MainScreen   │
                  │  (Game Start) │
                  └───────────────┘
```

## Loading an Existing Profile

```
┌─────────────────────┐
│  SplashScreen       │
│  (Start)            │
└──────────┬──────────┘
           │ Click "Play"
           ▼
┌─────────────────────┐
│ ProfileSelection    │
│ Screen              │
│ - List of Profiles  │
│ - Delete Option     │
└──────────┬──────────┘
           │ Click on Profile
           ▼
┌─────────────────────┐   ← NEW SCREEN
│ ProfileLoadSummary  │
│ Screen              │
│                     │
│ Shows:              │
│ • Character Name    │
│ • Gender            │
│ • Difficulty        │
│ • Year (gameDate)   │ ← NEW
│ • Seed (randSeed)   │ ← NEW
│ • Top 5 Attributes  │
│                     │
│ [Continue] [Back]   │ ← NEW BUTTONS
└──────────┬──────────┘
           │
           ├─ Click "Continue"
           │  ▼
           │  ┌───────────────┐
           │  │  MainScreen   │
           │  │  (Game Start) │
           │  └───────────────┘
           │
           └─ Click "Back"
              ▼
           ┌─────────────────────┐
           │ ProfileSelection    │
           │ Screen              │
           └─────────────────────┘
```

## Data Structure

### Profile Class
```
Profile {
    - characterName: String
    - gender: String
    - difficulty: String
    - attributes: Map<String, Integer>
    - gameDate: int           ← NEW (default: 2050)
    - randSeed: long          ← NEW (default: System.currentTimeMillis())
}
```

### Storage (JSON via ProfileManager)
```json
{
  "characterName": "John",
  "gender": "Male",
  "difficulty": "Normal",
  "attributes": {
    "INTELLIGENCE": 5,
    "PERCEPTION": 4,
    "MEMORY": 3,
    ...
  },
  "gameDate": 2050,
  "randSeed": 1739890212749
}
```

## Key Features

1. **Backwards Compatibility**: Old profiles without gameDate/randSeed will get default values
2. **Random Seed**: Each profile gets a unique seed for procedural generation
3. **Summary Screen**: Players can review their profile before continuing
4. **Data Persistence**: All fields saved/loaded via ProfileManager

# Character Data Model

This document describes **every piece of information** stored for characters in the
game, covering both the **player character** (`Profile`) and **NPCs**
(`NpcCharacter`). It highlights where the two overlap and where they differ.

---

## Quick Comparison

| Capability | Player (`Profile`) | NPC (`NpcCharacter`) |
|---|:---:|:---:|
| Name | `characterName` | `fullName` |
| Gender | ✓ | ✓ |
| Age | ✗ | ✓ |
| Occupation | ✗ | ✓ |
| Sprite / icon | `characterIcon` | `spriteKey` |
| Physical description text | ✗ | ✓ |
| Hair type, hair colour, beard style | ✗ | ✓ |
| Height & weight | via attributes map | dedicated `int` fields |
| Wealthy level | ✗ | ✓ |
| Favourite colour | ✗ | ✓ |
| Vision trait | ✓ | ✓ |
| Vector face (`FaceConfig`) | ✗ | ✓ |
| Skin-tone code | ✗ | ✓ |
| Home address | ✓ | ✓ |
| Workplace address | ✗ | ✓ |
| Frequent locations | ✗ | ✓ |
| Phone number / email | ✗ | ✓ |
| Investigative attributes (×11) | ✓ (String keys) | ✓ (enum keys) |
| Body measurements (×4) | ✓ (in attributes map) | ✗ |
| Detective level (derived) | ✓ (computed) | ✗ |
| Personality traits | ✗ | ✓ (cooperativeness, honesty, nervousness) |
| Personality profile archetype | ✗ | ✓ (`PersonalityProfile` enum) |
| Skills | ✗ | ✓ (`List<NpcSkill>`) |
| Daily schedule | ✗ | ✓ (`NpcSchedule`) |
| Equipment (slot-based) | ✓ | ✗ |
| Utility items | ✓ | ✗ |
| Stash (home storage) | ✓ | ✗ |
| Carried items | ✗ | ✓ (`List<EquipItem>`) |
| Relationships | ✓ | ✓ |
| Birthdate | ✗ | ✓ |
| Trackable on map | ✗ | ✓ |
| Case files | ✓ | ✗ |
| Calendar / appointments | ✓ | ✗ |
| Phone call tracking | ✓ | ✗ |
| Money | ✓ | ✗ |
| Stamina (runtime) | ✓ | ✗ |
| Game date / time | ✓ | ✗ |
| Difficulty setting | ✓ | ✗ |
| World NPC list | ✓ (owns the list) | ✗ |

---

## 1  Player Character — `Profile`

**Source:** `core/src/main/java/eb/framework1/character/Profile.java`

### 1.1  Identity

| Field | Type | Notes |
|---|---|---|
| `characterId` | `String` | UUID; allows multiple save-slots without name collisions |
| `characterName` | `String` | Player-chosen name |
| `gender` | `String` | `"M"` or `"F"` |
| `difficulty` | `String` | Chosen difficulty level |
| `characterIcon` | `String` | Selected sprite key (e.g. `"man1"`, `"woman2"`) |

### 1.2  Attributes

| Field | Type | Notes |
|---|---|---|
| `attributes` | `Map<String, Integer>` | Keyed by `CharacterAttribute.name()`. Stores all 11 investigative attributes **plus** body measurements (`HEIGHT_CM`, `WEIGHT_KG`, `MUSCLE_KG`, `FAT_KG`). Values 1–10 for investigative attrs; body measurements use real-world units. |

Detective Level is **derived** at read-time from the sum of the 11 investigative
attributes — it is not stored separately.

### 1.3  Physical / Traits

| Field | Type | Notes |
|---|---|---|
| `visionTrait` | `VisionTrait` | `NONE`, `FARSIGHTED`, or `NEARSIGHTED`; defaults to `NONE` |
| `homeAddress` | `String` | Display name of the building where the player lives |

The player does **not** have a generated face (`FaceConfig`), hair type/colour,
beard style, skin-tone code, height/weight as separate fields, age, or
occupation.

### 1.4  Equipment & Inventory

| Field | Type | Notes |
|---|---|---|
| `equipment` | `Map<EquipmentSlot, EquipItem>` | One item per slot (`WEAPON`, `HEAD`, `BODY`, `LEGS`, `FEET`); `null` = empty |
| `utilityItems` | `List<EquipItem>` | Multiple utility items may be carried at once |
| `stash` | `List<EquipItem>` | Items stored at the player's home office (not carried) |

### 1.5  Economy & Stamina

| Field | Type | Notes |
|---|---|---|
| `money` | `int` | Current money balance |
| `currentStamina` | `int` | Runtime stamina pool; `-1` = lazy-initialise from the `STAMINA` attribute on first access |

### 1.6  Game Time

| Field | Type | Notes |
|---|---|---|
| `gameDate` | `int` | Game date starting from year 2050 |
| `gameDateTime` | `String` | Full in-game date/time (`"YYYY-MM-DD HH:MM"`) |
| `randSeed` | `long` | Seed used for procedural generation |

### 1.7  Cases & Calendar

| Field | Type | Notes |
|---|---|---|
| `caseFiles` | `List<CaseFile>` | All case files (open and closed) |
| `activeCaseFile` | `CaseFile` | Currently selected case (nullable) |
| `calendarEntries` | `List<CalendarEntry>` | Accepted appointments and case starts |
| `lastEmailCheckDate` | `String` | `"YYYY-MM-DD"` when emails were last generated; `""` = never |

### 1.8  Phone & Communication

| Field | Type | Notes |
|---|---|---|
| `phonedContactKeys` | `Set<String>` | Keys of contacts already phoned, formatted as `"caseId\|contactName"` |
| `contactMessageRatings` | `Map<String, PhoneMessageRating>` | Tone rating (`FRIENDLY` / `NEUTRAL` / `UNFRIENDLY`) for each phoned contact |

### 1.9  Social

| Field | Type | Notes |
|---|---|---|
| `relationships` | `List<Relationship>` | Relationships with characters the player has met (mutable) |

### 1.10  World

| Field | Type | Notes |
|---|---|---|
| `worldNpcs` | `List<NpcCharacter>` | NPCs that populate the game world; generated at the start of a new game |

---

## 2  NPC — `NpcCharacter`

**Source:** `core/src/main/java/eb/framework1/character/NpcCharacter.java`

Built via an inner `Builder`; immutable once constructed (collections are
mutable for `relationships` only).

### 2.1  Identity

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | Unique identifier within its context (e.g. `"world-npc-7"`) |
| `fullName` | `String` | First name + surname |
| `gender` | `String` | `"M"` or `"F"` (auto-uppercased by the builder) |
| `age` | `int` | Age in years |
| `occupation` | `String` | Job title or occupation string |

### 2.2  Appearance

| Field | Type | Notes |
|---|---|---|
| `spriteKey` | `String` | Lookup key for the 2-D sprite (`"man1"`, `"woman2"`, …) |
| `physicalDescription` | `String` | One-sentence flavour text for interview / surveillance UI |
| `hairType` | `String` | `"straight"`, `"wavy"`, `"curly"`, `"bald"`, etc. |
| `hairColor` | `String` | `"black"`, `"brown"`, `"blonde"`, `"red"`, `"gray"`, `"white"` |
| `beardStyle` | `String` | `"short beard"`, `"long beard"`, `"stubble"`, or `""` (clean-shaven / female) |
| `heightCm` | `int` | Height in centimetres (0 = not set) |
| `weightKg` | `int` | Body weight in kilograms (0 = not set) |
| `wealthyLevel` | `int` | Apparent wealth on 1–10 scale |
| `favColor` | `String` | Favourite colour (may be empty) |
| `visionTrait` | `VisionTrait` | `NONE`, `FARSIGHTED`, or `NEARSIGHTED` |
| `faceConfig` | `FaceConfig` | Full vector-face configuration (see §4); nullable |
| `skinToneCode` | `String` | Skin-tone category code (e.g. `"fair_light"`); nullable |

### 2.3  Location

| Field | Type | Notes |
|---|---|---|
| `homeAddress` | `String` | Building name resolvable on the city map |
| `workplaceAddress` | `String` | Building name for the NPC's workplace |
| `frequentLocations` | `List<String>` | Up to three additional frequently-visited places (bars, gyms, etc.) |

### 2.4  Contact

| Field | Type | Notes |
|---|---|---|
| `phoneNumber` | `String` | Formatted phone number |
| `email` | `String` | Email address (may be empty) |

### 2.5  Personality

| Field | Type | Notes |
|---|---|---|
| `cooperativeness` | `int` (1–10) | Willingness to provide information when interviewed |
| `honesty` | `int` (1–10) | Truthfulness; low values require extra verification |
| `nervousness` | `int` (1–10) | Visible anxiety; signal of involvement during interview / surveillance |
| `personalityProfile` | `PersonalityProfile` | Archetype that constrains trait ranges (see §3.2) |

### 2.6  Investigative Attributes

| Field | Type | Notes |
|---|---|---|
| `attributes` | `Map<CharacterAttribute, Integer>` | The same 11 investigative attributes as the player (1–10 each), randomly generated. Body measurements and `DETECTIVE_LEVEL` are **not** stored. |

### 2.7  Skills & Schedule

| Field | Type | Notes |
|---|---|---|
| `skills` | `List<NpcSkill>` | Skills inferred from occupation (populated by `NpcGenerator`; empty if created directly) |
| `schedule` | `NpcSchedule` | Daily schedule of location/activity per hour block (nullable) |

### 2.8  Items

| Field | Type | Notes |
|---|---|---|
| `carriedItems` | `List<EquipItem>` | Visibly carried items (e.g. pistol for a police officer); empty for most NPCs |

### 2.9  Social & Misc

| Field | Type | Notes |
|---|---|---|
| `relationships` | `List<Relationship>` | Relationships with characters the NPC has met (mutable) |
| `birthdate` | `String` | Date of birth in `"YYYY-MM-DD"` format; empty if not set |
| `tracked` | `boolean` | Whether this NPC is shown / trackable on the city map |

---

## 3  Supporting Enums

### 3.1  `VisionTrait`

| Value | Display Name | Effect |
|---|---|---|
| `NONE` | — | No impairment |
| `FARSIGHTED` | Farsighted | Clear at distance, struggles up close; −1 `PERCEPTION` |
| `NEARSIGHTED` | Nearsighted | Clear up close, struggles at distance; −1 `PERCEPTION` |

Used by both `Profile` and `NpcCharacter`. When an NPC is impaired, the face
generator applies glasses via the `glassesMale` / `glassesFemale` face rules.

### 3.2  `PersonalityProfile`

| Value | Cooperativeness | Honesty | Nervousness | Description |
|---|---|---|---|---|
| `DEFAULT` | 1–10 | 1–10 | 1–10 | Normal personality, no trait skew |
| `PSYCHOPATH` | 5–9 | 1–2 | 1–2 | Superficially charming but compulsive liar; shows no outward anxiety |

NPC-only. The player character does not have a personality profile.

### 3.3  `CharacterAttribute`

Organised into three investigative categories plus body measurements and one
derived value.

**Mental (4):**
`INTELLIGENCE`, `PERCEPTION`, `MEMORY`, `INTUITION`

**Physical (3):**
`AGILITY`, `STAMINA`, `STRENGTH`

**Social (4):**
`CHARISMA`, `INTIMIDATION`, `EMPATHY`, `STEALTH`

**Body Measurements (4) — player only:**
`HEIGHT_CM`, `WEIGHT_KG`, `MUSCLE_KG`, `FAT_KG`

**Derived (1) — player only:**
`DETECTIVE_LEVEL` — computed from the sum of the 11 investigative attributes.

NPCs store only the 11 investigative attributes; body measurements and
detective level are not applicable.

### 3.4  `NpcSkill` (12 work skills)

| Skill | Building Categories | Hours |
|---|---|---|
| `SHOP_CLERK` | commercial, retail | 08–18 |
| `OFFICE_WORKER` | office | 09–17 |
| `MEDICAL_PROFESSIONAL` | medical | 07–19 |
| `EDUCATOR` | education | 07–15 |
| `LAW_ENFORCEMENT` | public_services | 07–19 |
| `HOSPITALITY_WORKER` | hospitality | 06–22 |
| `ENTERTAINER` | entertainment | 12–23 |
| `LABORER` | industrial | 06–14 |
| `GOVERNMENT_WORKER` | government, public_services | 08–17 |
| `RESEARCHER` | education, office | 09–18 |
| `FREELANCER` | office, commercial | 08–18 |
| `HOMEMAKER` | residential, commercial | 09–17 |

Each skill has a `SkillCategory`: `WORK`, `HOBBIES`, or `GENERAL`.

### 3.5  `EquipmentSlot`

`WEAPON` · `HEAD` · `BODY` · `LEGS` · `FEET` · `UTILITY`

Player equipment uses one item per non-utility slot. The `UTILITY` slot allows
multiple items. NPC `carriedItems` is a flat list (not slotted).

---

## 4  Vector Face — `FaceConfig`

**Source:** `core/src/main/java/eb/framework1/face/FaceConfig.java`

NPC-only. The player character does not have a generated face.

### 4.1  Global Parameters

| Field | Type | Notes |
|---|---|---|
| `fatness` | `double` | 0.0 (thin) → 1.0 (fat); scales body width |
| `teamColors` | `String[3]` | `[primary, secondary, accent]` hex colours |

### 4.2  Facial Features

Each feature is an immutable nested type.

| Feature | Type | Key Fields |
|---|---|---|
| `hairBg` | `SimpleFeature` | `id` |
| `body` | `BodyFeature` | `id`, `color` (hex), `size` |
| `jersey` | `SimpleFeature` | `id` |
| `ear` | `EarFeature` | `id`, `size` |
| `head` | `HeadFeature` | `id`, `shave` (rgba colour) |
| `eyeLine` | `SimpleFeature` | `id` |
| `smileLine` | `SmileLineFeature` | `id`, `size` |
| `miscLine` | `SimpleFeature` | `id` |
| `facialHair` | `SimpleFeature` | `id` |
| `eye` | `EyeFeature` | `id`, `angle` (degrees) |
| `eyebrow` | `EyebrowFeature` | `id`, `angle` (degrees) |
| `hair` | `HairFeature` | `id`, `color` (hex), `flip` |
| `mouth` | `MouthFeature` | `id`, `flip` |
| `nose` | `NoseFeature` | `id`, `flip`, `size` |
| `glasses` | `SimpleFeature` | `id` |
| `accessories` | `SimpleFeature` | `id` |

Features with `id = "none"` are not rendered. Several features (`jersey`,
`glasses`, `accessories`, `miscLine`, `facialHair`) default to `"none"` and
are only populated when face rules supply them.

---

## 5  Schedule — `NpcSchedule` / `NpcScheduleEntry`

NPC-only.

### Activity Types

`SLEEP` · `HOME` · `WORK` · `LEISURE` · `SHOPPING`

### Entry Fields

| Field | Type | Notes |
|---|---|---|
| `startHour` | `int` | Start of time slot (inclusive), 0–23 |
| `endHour` | `int` | End of time slot (exclusive), 1–24 |
| `activityType` | `String` | One of the five activity types |
| `locationName` | `String` | Building name or address |
| `cellX` | `int` | City-map X-coordinate (−1 if unknown) |
| `cellY` | `int` | City-map Y-coordinate (−1 if unknown) |

---

## 6  Relationships — `Relationship`

Shared by both `Profile` and `NpcCharacter`.

| Field | Type | Notes |
|---|---|---|
| `targetId` | `String` | Identifier of the other character |
| `targetName` | `String` | Display name of the other character |
| `opinion` | `int` | Positive = like, negative = dislike |

Initial opinion on first meeting is derived from the contact's `CHARISMA`
attribute: `(charisma − 5) × 10`. A bilateral relationship is created so both
parties have an opinion of each other.

---

## 7  Equipment Items — `EquipItem`

Used by both player (`equipment` / `utilityItems` / `stash`) and NPCs
(`carriedItems`).

| Field | Type | Notes |
|---|---|---|
| `name` | `String` | Display name |
| `slot` | `EquipmentSlot` | Which slot this item occupies |
| `description` | `String` | Short description |
| `weight` | `float` | Weight in kg |
| `caseItem` | `boolean` | Belongs to active case (cannot be dropped) |
| `modifiers` | `Map<CharacterAttribute, Integer>` | Attribute bonuses while equipped |

### Predefined Items

| Item | Slot | Modifiers | Weight |
|---|---|---|---|
| `PISTOL` | WEAPON | +1 INTIMIDATION | 0.9 kg |
| `BINOCULARS` | UTILITY | +1 PERCEPTION | 0.5 kg |
| `CAMERA` | UTILITY | — | 0.8 kg |
| `PEPPER_SPRAY` | UTILITY | +1 STRENGTH | 0.2 kg |
| `GLASSES` | UTILITY | +1 PERCEPTION | 0.05 kg |
| `HAT` | HEAD | +1 STEALTH | 0.1 kg |
| `COAT` | BODY | +1 STEALTH | 1.2 kg |
| `SUN_GLASSES` | UTILITY | +1 STEALTH, +1 PERCEPTION | 0.05 kg |

---

## 8  Calendar — `CalendarEntry`

Player-only.

| Field | Type | Notes |
|---|---|---|
| `dateTime` | `String` | `"YYYY-MM-DD HH:MM"` |
| `title` | `String` | Appointment title |
| `location` | `String` | Location name |
| `rewardMoney` | `int` | Money reward (0 = none) |
| `rewardItemName` | `String` | Item reward (nullable) |
| `locationCellX` | `int` | Map cell X (−1 if unknown) |
| `locationCellY` | `int` | Map cell Y (−1 if unknown) |
| `contactName` | `String` | Name of person being met (empty if none) |
| `contactGender` | `String` | `"M"` or `"F"` (defaults to `"M"`) |

---

## 9  Key Differences Summarised

### What only the player has

- **Slot-based equipment** with weapon, armour, and utility slots
- **Stash** (home storage)
- **Money and stamina** (runtime resources)
- **Case files** and an active case
- **Calendar** with appointments
- **Phone call tracking** (who has been phoned, message tone ratings)
- **Body measurements** (height, weight, muscle, fat) stored as attributes
- **Detective level** (derived attribute)
- **Difficulty setting** and procedural generation seed

### What only NPCs have

- **Generated face** (`FaceConfig`) with 16 vector features
- **Full physical appearance**: hair type/colour, beard style, wealthy level,
  favourite colour, skin-tone code, and a one-sentence physical description
- **Age, occupation, and birthdate**
- **Workplace address** and frequent locations
- **Phone number and email** (contact details)
- **Personality traits**: cooperativeness, honesty, nervousness
- **Personality profile** archetype (`DEFAULT` or `PSYCHOPATH`)
- **Skills** inferred from occupation
- **Daily schedule** (activity + location per hour)
- **Carried items** (flat list, not slotted)
- **Tracked flag** (whether they appear on the city map)

### What both share

- **Gender** — stored identically (`"M"` / `"F"`)
- **Home address** — building name on the city map
- **Vision trait** — `NONE`, `FARSIGHTED`, or `NEARSIGHTED`
- **11 investigative attributes** — same `CharacterAttribute` values, but the
  player stores them as `Map<String, Integer>` (allocated via point-buy) while
  NPCs store them as `Map<CharacterAttribute, Integer>` (randomly generated)
- **Relationships** — `List<Relationship>` with opinion tracking
- **A visual representation** — player has `characterIcon` (sprite key); NPCs
  have both `spriteKey` and a full `FaceConfig`

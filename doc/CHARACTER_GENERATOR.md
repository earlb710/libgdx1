# Character Generator — Design Reference

## Overview

The `PersonNameGenerator` currently produces only a **name string** (first name
+ surname, with an optional gender filter) for the client and subject of a case.
For the game to present client, victim, and suspect characters as believable
individuals — and for investigation mechanics such as interviews, surveillance,
and background checks to interact with them meaningfully — each NPC needs a
richer data model.

This document lists every extra field that `PersonNameGenerator` (or a new
`CharacterGenerator` that wraps it) would need to produce, grouped first by the
**base fields** common to all three roles, then by the **role-specific additions**
unique to clients, victims, and suspects.

---

## Current State

`PersonNameGenerator` exposes:

| Output | Method |
|--------|--------|
| First name (gender-filtered) | `generate(gender)` |
| First name + explicit surname | `generate(gender, surname)` |
| Random full name | `generateFull(gender)` |
| Random surname only | `randomSurname()` |

`CaseGenerator` calls `generateFull()` twice to set `CaseFile.clientName` and
`CaseFile.subjectName` — both are plain `String`s.  No structured character
object exists for either role.

---

## Proposed `NpcCharacter` Base Data Model

The generator should produce an `NpcCharacter` object (or equivalent record)
carrying the following base fields for every NPC regardless of role.

### Identity

| Field | Type | Notes |
|-------|------|-------|
| `id` | `String` | Unique identifier within the case (e.g. `"char-client-1"`) |
| `fullName` | `String` | Already produced by `PersonNameGenerator.generateFull()` |
| `gender` | `String` | `"M"` or `"F"` — already chosen by `CaseGenerator.randomGender()` |
| `age` | `int` | Randomly generated within a plausible range for the role (see below) |
| `occupation` | `String` | Job title drawn from a pool appropriate to the case type |

### Physical Appearance

| Field | Type | Notes |
|-------|------|-------|
| `spriteKey` | `String` | References an existing character sprite (`man1`, `man2`, `woman1`, `woman2`) matching gender |
| `physicalDescription` | `String` | One sentence (height, build, distinguishing features) used in interview and surveillance flavour text |

### Location

| Field | Type | Notes |
|-------|------|-------|
| `homeAddress` | `String` | Street address used to anchor surveillance and physical-search activities |
| `workplaceAddress` | `String` | Where the character can typically be found during working hours |
| `frequentLocations` | `List<String>` | Up to three additional addresses the character visits regularly (bars, gyms, etc.) |

Location values are building names drawn from the city map
(`Building.getName()`) so that in-game navigation can resolve them to actual
map cells.

### Contact Details

| Field | Type | Notes |
|-------|------|-------|
| `phoneNumber` | `String` | Formatted phone string (e.g. `"07700 900 123"`), used with `PhoneContact` |
| `email` | `String` | Optional; used for email-based story beats |

### Personality

| Field | Type | Notes |
|-------|------|-------|
| `cooperativeness` | `int` (1–10) | How willingly the NPC provides information when interviewed; affects `INTERVIEW` lead discovery checks |
| `honesty` | `int` (1–10) | Whether they volunteer truthful details or withhold them; low honesty means extra actions needed to verify claims |
| `nervousness` | `int` (1–10) | How visibly anxious the NPC is; a useful in-game signal of involvement |

### Character Attributes

Each NPC carries the **same eleven investigative attributes** as the player character,
stored as `Map<CharacterAttribute, Integer>` with each value in the range 1–10.
Unlike the player character (which allocates 30 points through the point-buy screen),
NPC attribute values are **generated randomly** by `CharacterGenerator`.

| Attribute | Category | Gameplay relevance for NPCs |
|-----------|----------|-----------------------------|
| `INTELLIGENCE` | Mental | How quickly the NPC processes information and adapts when questioned |
| `PERCEPTION` | Mental | How likely the NPC is to notice if they are being surveilled |
| `MEMORY` | Mental | Consistency of the NPC's account across multiple interviews |
| `INTUITION` | Mental | Whether the NPC senses that the detective is getting close |
| `AGILITY` | Physical | Ability to evade surveillance or flee if `fleeRisk` triggers |
| `STAMINA` | Physical | How long the NPC can sustain an alibi under prolonged questioning |
| `STRENGTH` | Physical | Relevant for suspects in cases involving physical confrontation |
| `CHARISMA` | Social | How convincing the NPC is when giving a false statement |
| `INTIMIDATION` | Social | Whether the NPC tries to intimidate witnesses or the detective |
| `EMPATHY` | Social | How effective the NPC is at reading and responding to the detective's approach |
| `STEALTH` | Social | Whether the NPC is good at covering their tracks |

Body-measurement attributes (`HEIGHT_CM`, `WEIGHT_KG`, `MUSCLE_KG`, `FAT_KG`) and the
derived `DETECTIVE_LEVEL` are **not** stored on NPCs; querying them returns 0.

---

## Client-Specific Extra Fields

The client is the person who hired the detective.  In addition to all base
fields, a client NPC needs:

| Field | Type | Notes |
|-------|------|-------|
| `motivation` | `String` | Why they hired the detective (e.g. `"suspects infidelity"`, `"wants closure"`, `"threatened by blackmailer"`) — used in the opening description |
| `relationshipToSubject` | `String` | How the client knows the subject (e.g. `"spouse"`, `"employer"`, `"sibling"`) |
| `budget` | `int` | In-game currency available for fees and expenses |
| `trustLevel` | `int` (1–10) | How much the client trusts the detective; affects willingness to share sensitive information up front |
| `isWithholdingInfo` | `boolean` | When `true`, the client is concealing something; a plot-twist trigger can flip this mid-case |
| `withheldDetail` | `String` | The secret the client is hiding (hidden from player until discovered) |
| `contactPreference` | `enum` | `PHONE`, `EMAIL`, or `IN_PERSON` — drives how updates are delivered |

**Age range:** 25–70 (clients are typically adults with disposable income or
an urgent personal problem).

---

## Victim-Specific Extra Fields

The victim is the person who was harmed.  Depending on case type the victim
may be a separate character from the subject (e.g. in a `MURDER` case the
subject is the suspect, not the victim).

| Field | Type | Notes |
|-------|------|-------|
| `isDeceased` | `boolean` | `true` for `MURDER` and some `MISSING_PERSON` cases after the plot-twist |
| `dateOfDeath` | `String` | In-game date/time (nullable; set when `isDeceased` becomes `true`) |
| `causeOfDeath` | `String` | Short description used in forensic lead flavour text (nullable) |
| `lastKnownLocation` | `String` | Building name where the victim was last seen |
| `lastKnownTime` | `String` | In-game timestamp of last confirmed sighting |
| `relationshipToClient` | `String` | How the victim is connected to the client (e.g. `"son"`, `"colleague"`, `"former employee"`) |
| `relationshipToSuspect` | `String` | How the victim is connected to the suspect (drives motive generation) |
| `vulnerabilityTraits` | `List<String>` | Characteristics that made them a target (e.g. `"wealthy"`, `"isolated"`, `"aware of wrongdoing"`) |
| `timeline` | `List<String>` | Ordered list of in-game events in the victim's last 48 hours; feeds `DOCUMENTS` and `INTERVIEW` leads |

**Age range:** 18–80 (any adult; skew by case type — `MISSING_PERSON` skews
younger, `MURDER` and `BLACKMAIL` skew middle-aged).

---

## Suspect-Specific Extra Fields

The suspect is the person being investigated.  This maps to the existing
`CaseFile.subjectName` but needs far more structured data.

### Guilt and Motive

| Field | Type | Notes |
|-------|------|-------|
| `guiltLevel` | `enum` | `INNOCENT`, `PARTIALLY_GUILTY`, `GUILTY` — the hidden truth; never shown directly to the player |
| `motive` | `String` | What drove the suspect (e.g. `"financial gain"`, `"revenge"`, `"jealousy"`, `"coercion"`) |
| `opportunityDescription` | `String` | Short statement of how they could have committed the act |
| `priorRecord` | `boolean` | Has a prior criminal history; affects `BACKGROUND_CHECK` lead outcomes |
| `priorOffences` | `List<String>` | Brief descriptions of prior offences (visible via `BACKGROUND_CHECK`) |

### Alibi

| Field | Type | Notes |
|-------|------|-------|
| `alibi` | `String` | Where the suspect claims to have been |
| `alibiWitness` | `String` | Name of the person who can corroborate or contradict the alibi |
| `alibiIsSound` | `boolean` | Hidden truth — when `false` the alibi can be broken via `INTERVIEW` or `DOCUMENTS` |

### Behaviour Under Investigation

| Field | Type | Notes |
|-------|------|-------|
| `fleeRisk` | `int` (1–10) | Likelihood of attempting to leave the area if they believe they are being watched; drives plot-twist urgency |
| `counterSurveillanceAware` | `boolean` | When `true`, a low `STEALTH` player attribute makes surveillance actions harder |
| `willConfessUnderPressure` | `boolean` | Determines whether a high `INTIMIDATION` check during interview can extract an admission |

### Associates

| Field | Type | Notes |
|-------|------|-------|
| `knownAssociates` | `List<String>` | Names of other characters (clients, victims, or additional NPCs) the suspect interacts with |
| `employer` | `String` | Organisation name; used in corporate or fraud cases |

**Age range:** 20–65 (adults capable of the alleged act; skew by case type —
`THEFT` skews 18–35, `FRAUD` and `CORPORATE_ESPIONAGE` skew 30–55).

---

## `CharacterGenerator` and `NpcCharacter` — What Is Implemented

`NpcCharacter.java` and `CharacterGenerator.java` are implemented in the
`eb.gmodel1` package.

### `NpcCharacter`
A builder-based immutable data class carrying all base NPC fields (identity,
appearance, location, contact, personality) plus the eleven investigative
`CharacterAttribute` values (`Map<CharacterAttribute, Integer>`).

### `CharacterGenerator`
Wraps `PersonNameGenerator` and a `Random` instance.  Exposes three methods:

```java
public NpcCharacter generateClient(CaseType caseType)
public NpcCharacter generateVictim(CaseType caseType)
public NpcCharacter generateSuspect(CaseType caseType)
```

Each method:
- Picks a random gender and generates a full name via `PersonNameGenerator`.
- Generates an age within the role-appropriate range (client 25–70, victim 18–80,
  suspect 20–65).
- Assigns a sprite key consistent with gender (`man1`/`man2` or `woman1`/`woman2`).
- Randomly assigns values 1–10 to all eleven investigative attributes.
- Randomly assigns values 1–10 to the three personality traits
  (cooperativeness, honesty, nervousness).
- Selects an occupation from a small inline pool appropriate to the case type
  and role.

Integration of `CharacterGenerator` into `CaseGenerator` — replacing the current
plain-string `clientName` / `subjectName` fields with `NpcCharacter` objects —
is outstanding (see table below).

### Data sources needed

| Generator input | Source |
|-----------------|--------|
| Occupation lists per case type | New `occupations_en.json` asset |
| Physical description templates | New `physical_descriptions_en.json` asset, or inline string pool in `CharacterGenerator` |
| Motive pools per case type | Inline string pool (small enough to embed) |
| Alibi location and witness name | `PersonNameGenerator` + building names from `CityMap` |
| Phone number format | `PhoneContact` patterns (already in `phone.json`) |

---

## Integration Points with Existing Systems

| Existing system | Change needed |
|-----------------|---------------|
| `CaseFile.clientName` (`String`) | Replace with `NpcCharacter client` |
| `CaseFile.subjectName` (`String`) | Replace with `NpcCharacter suspect` (+ optional `NpcCharacter victim`) |
| `CaseGenerator.generate()` | Call `CharacterGenerator` instead of `nameGen.generateFull()` |
| `CaseLead` hints | Reference `suspect.homeAddress`, `suspect.alibi`, etc. for richer hint text |
| `DiscoveryMethod.INTERVIEW` | Use `cooperativeness`, `honesty`, and `nervousness` to weight success |
| `DiscoveryMethod.SURVEILLANCE` | Use `frequentLocations` to determine where surveillance can be run |
| `DiscoveryMethod.BACKGROUND_CHECK` | Use `priorRecord`, `priorOffences`, and `employer` |
| `PhoneContact` / `PhonePopup` | Populate from `NpcCharacter.phoneNumber` and `cooperativeness` |
| `SaveGameManager` | Add `NpcCharacterData` DTO (mirrors the new fields; same pattern as `StoryNodeData`) |
| `InfoPanelRenderer` (case-file tab) | Show client/suspect card with sprite, name, occupation, and address |

---

## Summary of Outstanding Work

| Priority | Item |
|----------|------|
| High | Migrate `CaseFile` from `String clientName` / `String subjectName` to `NpcCharacter` objects |
| High | Update `CaseGenerator.generate()` to call `CharacterGenerator` instead of `nameGen.generateFull()` |
| High | Update `SaveGameManager` with `NpcCharacterData` DTO for save/load |
| Medium | Create `occupations_en.json` data file and load occupations from it (replacing inline pools) |
| Medium | Wire `cooperativeness`, `honesty`, and `nervousness` into `INTERVIEW` lead-discovery checks |
| Medium | Wire `frequentLocations` into `SURVEILLANCE` action targets |
| Medium | Wire `alibi` / `alibiIsSound` into `DOCUMENTS` / `INTERVIEW` lead outcomes |
| Medium | Show NPC card (sprite + name + occupation) in case-file info panel |
| Low | Create `physical_descriptions_en.json` for flavour text |
| Low | Wire NPC attributes (e.g. `STEALTH`) into difficulty of `SURVEILLANCE` checks |
| Low | Wire `willConfessUnderPressure` + player `INTIMIDATION` vs NPC `CHARISMA` into interview outcomes |
| Low | Support multiple suspects per case (list instead of single `suspect` field) |

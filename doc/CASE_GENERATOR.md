# Case Generator — Design Reference

## Overview

The `CaseGenerator` class procedurally builds fully-populated `CaseFile`
objects for the detective game. Each generated case carries narrative context,
hidden information the player must uncover, a hidden complexity rating, and a
four-level **story-progression tree** that maps every step of the investigation.

---

## What Has Been Implemented

### 1. `CaseType` enum
Eight case categories, each with a display name, description, and a
min/max difficulty range (1–10):

| Constant | Display name | Difficulty |
|---|---|---|
| `MISSING_PERSON` | Missing Person | 1–5 |
| `INFIDELITY` | Infidelity | 1–4 |
| `THEFT` | Theft | 1–5 |
| `FRAUD` | Fraud | 4–9 |
| `BLACKMAIL` | Blackmail | 3–7 |
| `MURDER` | Murder | 5–10 |
| `STALKING` | Stalking | 2–6 |
| `CORPORATE_ESPIONAGE` | Corporate Espionage | 5–10 |

### 2. `DiscoveryMethod` enum
Six in-game techniques used to uncover hidden leads:
`INTERVIEW`, `SURVEILLANCE`, `FORENSICS`, `DOCUMENTS`,
`PHYSICAL_SEARCH`, `BACKGROUND_CHECK`.

### 3. `CaseLead` — hidden investigation facts
Each lead carries:
- `id` — unique identifier within the case
- `description` — the hidden fact (revealed when discovered)
- `hint` — a vague clue always visible to the player
- `discoveryMethod` — which technique reveals it
- `discovered` flag — set by `discover()`

### 4. `CaseFile` — the case record
Stores all investigation data for one case:
- Status (`OPEN`, `CLOSED`, `COLD`)
- Clues, evidence strings, player notes, and physical `EvidenceItem`s
- Generator-populated fields: `caseType`, `clientName`, `subjectName`,
  `objective`, leads, `complexity`, and `storyRoot`

#### `complexity` (hidden, integer 1–3)
- **1** — One phase (the initial investigation); straightforward, no plot twists.
- **2** — Two phases: the initial investigation plus one plot twist (e.g. a
  missing-person case becomes a murder investigation when a body is found).
- **3** — Three phases: the initial investigation plus two plot twists.

#### `storyRoot` (`CaseStoryNode`)
The root of the story-progression tree (see §5 below).
`null` for manually created / legacy cases.

### 5. `CaseStoryNode` — story-progression tree

A four-level tree structure encoding every step of the investigation.
Each sub-branch must be **fully completed before the next branch is unlocked**
(sequential-unlock rule enforced by `isChildAvailable(int)`).

#### Tree levels

| Level | `NodeType` | Count per parent | Description |
|---|---|---|---|
| 0 (root) | `ROOT` | 1 per case | Invisible container |
| 1 | `PLOT_TWIST` | 1 per complexity level | Major investigative phase or plot twist |
| 2 | `MAJOR_PROGRESS` | 2 per phase | Significant milestone within a phase |
| 3 | `MINOR_PROGRESS` | 2 per milestone | Focused sub-task |
| 4 (leaf) | `ACTION` | 2 per sub-task | Concrete player action/discovery/puzzle |

#### Fixed shape per case
```
ROOT
└── PLOT_TWIST  (×complexity)
     └── MAJOR_PROGRESS  (×2)
          └── MINOR_PROGRESS  (×2)
               └── ACTION  (×2)  ← leaf
```

For complexity 1: **8 leaf actions** total  
For complexity 2: **16 leaf actions** total  
For complexity 3: **24 leaf actions** total

#### Key API
| Method | Purpose |
|---|---|
| `isFullyComplete()` | True when the whole sub-tree is done |
| `getNextActiveChild()` | The first unlocked-but-incomplete child |
| `isChildAvailable(index)` | True when all prior siblings are complete |
| `complete()` | Mark a leaf `ACTION` node as done |

### 6. `CaseGenerator` — the generator

Constructs a complete `CaseFile` from a `CaseType` and an in-game date.

| What is generated | Method / data |
|---|---|
| Client and subject names | `PersonNameGenerator` |
| Narrative description | `buildDescription()` |
| Case objective | `buildObjective()` |
| Hidden leads | `buildLeads()` |
| Complexity (1–3, random) | `random.nextInt(3) + 1` |
| Story tree | `buildStoryTree()` → `buildPhase()` |

#### Story template data
`STORY_DATA` is a `String[8][3][24]` table:
- dimension 1 — case type ordinal (matches `CaseType.ordinal()`)
- dimension 2 — phase index (0 = initial, 1 = first twist, 2 = second twist)
- dimension 3 — 24 strings per phase encoding titles and action descriptions

The `{s}` placeholder in any string is replaced with the subject's name at
generation time.

---

## What Is Outstanding

### High priority

- **Save/load support for the story tree.**  
  `CaseFile` serialisation (used by `SaveGameManager`) currently ignores
  `storyRoot`.  The tree must be serialised and deserialised so that player
  progress persists across sessions.

- **Story tree advancement in game logic.**  
  Nothing in the game currently calls `CaseStoryNode.complete()` or queries
  `getNextActiveChild()`.  Each `ACTION` leaf node needs to be wired to the
  corresponding in-game activity (interview, surveillance, etc.) so the tree
  advances as the player works the case.

- **Plot-twist triggers.**  
  When the player completes all `ACTION` leaves in phase *n*, the game must
  detect that a new `PLOT_TWIST` branch is now available and surface the
  narrative moment (journal entry, dialogue, screen notification) that
  explains the twist.

### Medium priority

- **UI exposure of story-tree progress.**  
  The case-file screen currently has no view of the story tree.  A progress
  panel showing the current phase, next milestone, and available actions would
  significantly improve player guidance.

- **Per-case-type difficulty scaling.**  
  `CaseType` exposes `getMinDifficulty()` / `getMaxDifficulty()`, but
  `CaseGenerator` does not yet use those bounds to vary the number or depth of
  leads and story nodes.  High-difficulty cases should have more leads and more
  complex trees.

- **Randomised tree depth / breadth.**  
  The story tree is currently fixed at 2 majors × 2 minors × 2 actions per
  phase.  A future iteration could vary these counts based on difficulty, giving
  simple cases shallower trees and hard cases deeper ones.

- **Unique action content per run.**  
  Currently each case type always uses the same template text for each phase.
  Randomly selecting from a pool of narrative variants would make repeated
  play-throughs feel fresh.

### Low priority

- **Additional case types.**  
  The `CaseType` enum contains eight types.  Candidate additions include
  `KIDNAPPING`, `ARSON`, `WITNESS_PROTECTION`, and `COLD_CASE`.

- **Multi-suspect branching.**  
  The current tree is linear within each phase.  A future enhancement could
  allow the player to investigate multiple suspects in parallel, with the tree
  branching and merging based on what is discovered.

- **Lead–tree integration.**  
  `CaseLead` objects and `CaseStoryNode` `ACTION` leaves are currently
  independent.  Linking each lead directly to a leaf node (so discovering a
  lead automatically completes its corresponding action) would unify the two
  systems.

- **Localisation.**  
  All narrative text is currently English only.  Externalising the
  `STORY_DATA` table and lead / description templates to resource bundles
  would enable localisation.

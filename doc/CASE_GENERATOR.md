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
| `getNextAvailableAction()` | Recursively finds the first non-completed `ACTION` leaf (skipping fully-done branches) |
| `isChildAvailable(index)` | True when all prior siblings are complete |
| `complete()` | Mark a leaf `ACTION` node as done |

### 6. `CaseGenerator` — the generator (2 310 lines)

Constructs a complete `CaseFile` from a `CaseType` and an in-game date.

| What is generated | Method / data |
|---|---|
| Client and subject names | `PersonNameGenerator` |
| Narrative description | `buildDescription()` |
| Case objective | `buildObjective()` |
| Hidden leads | `buildLeads()` |
| Complexity (1–3, random) | `random.nextInt(3) + 1` |
| Story tree | `buildStoryTree()` → `buildPhase()` |
| Interview scripts | `buildInterviewScripts()` → per-NPC builders |

#### Story template data
`STORY_DATA` is a `String[8][3][24]` table:
- dimension 1 — case type ordinal (matches `CaseType.ordinal()`)
- dimension 2 — phase index (0 = initial, 1 = first twist, 2 = second twist)
- dimension 3 — 24 strings per phase encoding titles and action descriptions

The `{s}` placeholder in any string is replaced with the subject's name at
generation time.

#### Attribute-constrained action checks
Each `ACTION` node's skill check attribute is drawn from a pool constrained
by the action's type (`attributesForAction()`), not from the full set of 7
attributes. This ensures narrative coherence — e.g., evidence collection
uses PERCEPTION / INTELLIGENCE / STEALTH, never CHARISMA.

#### Context-aware success / failure narratives
`buildAttributeSuccessNarrative(attr, actionTitle)` and
`buildAttributeFailureNarrative(attr, actionTitle)` produce a short prose
sentence for every combination of attribute (7) × action category (4 + generic
fallback). The narrative explains *how* the attribute helped or hindered the
specific action. For full tables see `CASE_GENERATION_PIPELINE.md § Step 8`.

### 7. `InterviewTopic` enum — interview question categories

Eight topics a player can ask about during NPC interviews:

| Constant | Description |
|---|---|
| `ALIBI` | Where were you at the time of the crime? |
| `WHEREABOUTS` | What do you know about another character's location? |
| `OPINION` | What do you think of another character? (reveals jealousy, rivalry, etc.) |
| `RELATIONSHIP` | How do you know another character? |
| `LAST_CONTACT` | When did you last see the victim or subject? |
| `OBSERVATION` | Did you notice anything unusual around the time of the crime? |
| `MOTIVE` | Do you know of anyone with a reason to harm the victim? (Murder only) |
| `CONTACT_INFO` | Do you have a way to reach another character? (Charisma-gated) |

### 8. `InterviewResponse` — attribute-gated answers

Each response stores:
- `topic` — the `InterviewTopic` being answered
- `question` / `answer` — the Q&A text
- `truthful` — hidden flag; subjects may be deceptive
- `aboutNpcName` — the NPC the question concerns

#### Attribute gates
Three optional fields gate whether the player receives the full or a
shortened answer:

| Field | Purpose |
|---|---|
| `requiredAttribute` | Display name of the required attribute (e.g. `"Empathy"`) or empty |
| `requiredValue` | Minimum threshold (1–10) or `0` if no gate |
| `alternateAnswer` | Fallback text shown when the player's attribute is below the threshold |

`hasAttributeRequirement()` returns `true` when `requiredAttribute` is
non-empty **and** `requiredValue > 0`.  
`getEffectiveAnswer(int playerVal)` returns the full `answer` if the
player meets the threshold, otherwise `alternateAnswer`.

### 9. `InterviewScript` — per-NPC interview data

Wraps a list of `InterviewResponse` objects for one NPC.  
`CaseFile` stores `List<InterviewScript>` so every NPC's script is
accessible at runtime.

#### Interview generation (`CaseGenerator.buildInterviewScripts()`)

Four dedicated builder methods generate scripts for the core cast:

| Builder | NPC role | Key behaviour |
|---|---|---|
| `buildClientInterview()` | Client | Always truthful; opinion gate Empathy ≥ 5 |
| `buildSubjectInterview()` | Subject (suspect) | May be deceptive; alibi truthful 30 %, opinion 50 % |
| `buildWitnessInterview()` | Key Witness | Always truthful; observation gate Perception ≥ 6 |
| `buildAssociateInterview()` | Associate | Always truthful; observation gate Perception ≥ 5 |

Each builder is 138–166 lines and covers 6–8 topics with randomised
answer pools (3–9 options per pool).

#### Attribute gate summary

| Topic | Non-suspect gate | Subject/suspect gate |
|---|---|---|
| OPINION | Empathy ≥ 5 | — (no gate) |
| OBSERVATION | Perception ≥ 5–6 | Intimidation ≥ 6 |
| MOTIVE (Murder) | Intuition ≥ 5 | Intimidation ≥ 7 |
| CONTACT_INFO | Charisma ≥ 4 (client/associate) or ≥ 5 (witness) | Always refuses |

#### Truthfulness by role

| Role | Alibi | Opinion | Last Contact | Observation | Motive |
|---|---|---|---|---|---|
| Client / Witness / Associate | 100 % | 100 % | 100 % | 100 % | 100 % |
| Subject / Suspect | **30 %** | **50 %** | **40 %** | **50 %** | **50 %** |

### 10. Phone & location system

#### `NpcLocation` — location model

`NpcLocation.LocationCode` is an enum of **18** predefined urban/suburban
locations:

> Café, Bar, Office, Public Park, Library, Restaurant, Their Home, Gym,
> Warehouse District, Church, Hospital, Police Station, Diner, Hotel Lobby,
> Parking Garage, Bus Station, Street Market, Courthouse

Each NPC is assigned a **default location** via `locationForRole()`, which
maps role keywords to plausible venues (e.g. "Police Contact" → Police
Station, "Neighbour" → Their Home, "Friend" → random social venue).

#### `PhoneContact` — phone number model

| Field | Type | Description |
|---|---|---|
| `name` | `String` | Display name in the player's phone |
| `caseId` | `String` | Source case reference |
| `phoneNumber` | `String` | Format `555-XXXX` (suffix 0100–9999) |
| `phoneDiscovered` | `boolean` | `true` once the player has learnt the number |
| `defaultLocation` | `String` | Where the NPC can usually be found |
| `phoned` | `boolean` | Has the player called this contact? |
| `rating` | `PhoneMessageRating` | Call outcome rating (NEUTRAL / POSITIVE / NEGATIVE / BLACKLIST) |

The **client's phone number is always discovered** at case start.  All
other numbers begin hidden and must be uncovered through `CONTACT_INFO`
interviews (Charisma-gated) or story progression.

### 11. Suspect attributes

At complexity ≥ 2 the case adds extra suspects (1–2 at complexity 2,
2–3 at complexity 3).  Five distinguishing attributes allow the player to
eliminate innocent suspects:

| Attribute | Pool size | Subject value | Suspect rule |
|---|---|---|---|
| Hair Color | 6 | Random | 50 % match / 50 % differ |
| Beard Style | 6 (males) or "none" | Random | 50 % match / 50 % differ |
| Opportunity | 5 | Random | **Always matches** (all suspects present) |
| Access | 6 | Random | 50 % match / 50 % differ |
| Has Motive | 2 | **Always true** | 50 % match / 50 % differ |

If a suspect's four testable attributes all accidentally match the true
perpetrator (6.25 % chance), one is randomly forced to differ.

### 12. NPC table columns (admin tool)

The NPC data table in `CaseEditorPanel` has **19 columns** (indices 0–18):

| Index | Name | Notes |
|---|---|---|
| 0–10 | Role, Name, Gender, Age, Occupation, Cooperativeness, Honesty, Nervousness, Dead, Death Date/Time, Variance | Original character fields |
| 11 | Hair Color | Suspect attribute |
| 12 | Beard Style | Suspect attribute |
| 13 | Opportunity | Suspect attribute |
| 14 | Access | Suspect attribute |
| 15 | Has Motive | Suspect attribute (Boolean) |
| 16 | Phone Number | Format `555-XXXX` |
| 17 | Phone Discovered | Boolean |
| 18 | Default Location | Location display name |

### 13. Unknown facts & motive narratives

`generateUnknownFacts()` creates 7–8 facts with status `UNKNOWN` that
represent the core mysteries the player must solve:

| # | Category | Templates | Importance |
|---|---|---|---|
| 1 | METHOD | 5 (murder-specific) / 4 (other) | 5 |
| 2 | MOTIVE | 10 codes × 3–4 templates = ~37 | 5 |
| 3 | ITEM (weapon, murder only) | 5 | 4 |
| 4 | DATE (timeline) | 4 | 4 |
| 5 | EVIDENCE (location) | 4 | 3 |
| 6 | RELATIONSHIP (accomplices) | 4 | 3 |
| 7 | DATE (alibi contradictions) | 4 | 4 |
| 8 | EVIDENCE (cover-up) | 4 | 3 |

`buildMotiveNarrative(motiveCode, subject, victim)` selects from a pool
of 3–4 unique sentence templates per motive code (10 codes, ~37 templates
total).  Each template uses `{subject}` and `{victim}` substitution.

### 14. Save/load persistence for the story tree

`SaveGameManager` serialises and deserialises the complete story tree so that
player progress survives a save/load cycle.  Four nested DTO classes carry the
data:

| DTO class | What it captures |
|---|---|
| `CaseFileData` | All `CaseFile` fields including the `storyRoot` reference and `activeCaseId` |
| `StoryNodeData` | `id`, `title`, `description`, `nodeType`, `completed` flag, and recursive `children` list |
| `CaseLeadData` | `id`, `description`, `hint`, `discoveryMethod`, `discovered` flag |
| `EvidenceItemData` | `name`, `description`, `evidenceType`, and all `EvidenceModifier` values |

`GameSave` now stores `List<CaseFile> caseFiles` and the `activeCaseId`.
`GameSave.from()` snapshots them; `GameSave.applyToProfile()` restores them and
re-selects the active case.  Old saves without the `caseFiles` field load cleanly
(backward-compatible).

### 15. Case-file tab: story-tree progress panel

`InfoPanelRenderer.drawCaseFileTab()` was rewritten to add a scrollable content
area and a **Progress** section displayed above Clues / Evidence / Notes:

```
Progress
Phase 2 of 3: Initial Investigation
Milestone: Gather Primary Evidence
Next: □ Interview the bartender
```

- `Phase X of Y` counts completed and total `PLOT_TWIST` children of the root.
- `Milestone` shows the title of the current `MAJOR_PROGRESS` node (novel/blue).
- `Next` uses `getNextAvailableAction()` to find the first concrete action
  (white, prefixed with `□`).
- Displays "All objectives complete!" (green) when the tree is fully done.
- Hidden for legacy cases where `storyRoot` is `null`.

The checkboxes and "Add Note" button are now pinned at fixed screen positions at
the bottom of the panel, independent of scroll position.

---

## What Is Outstanding

### High priority

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

- **Code architecture — class size and duplication.**  
  `CaseEditorPanel` (~2 900 lines, ~90 methods) and `CaseGenerator`
  (2 310 lines, 29 methods) were significantly larger and shared more
  duplicated logic.  The following cleanup has been completed:

  | Issue | Status | Detail |
  |---|---|---|
  | **Action-type detection repetition** | ✅ Done | Extracted `ActionType` enum in core module.  All 7+ copy-paste sites in `CaseEditorPanel` now call `ActionType.classify()`. |
  | **Motive narratives locked in admin tool** | ✅ Done | Moved to `NarrativeTemplates` class in core.  `CaseEditorPanel` delegates; core engine can now also use them. |
  | **Attribute success/failure narratives locked in admin tool** | ✅ Done | Moved to `NarrativeTemplates` alongside motive narratives. |
  | **Hardcoded name arrays** | ✅ Done | Replaced `MALE_NAMES`/`FEMALE_NAMES`/`SURNAMES` with `PersonNameGenerator`. |
  | **Interview generation duplication** | ⬜ Remaining | `CaseEditorPanel.generateInterviews()` still reimplements interview logic. Should delegate to `CaseGenerator`. |
  | **CaseEditorPanel responsibilities** | ⬜ Remaining | Panel still mixes UI and generation. Further extraction possible. |

  **Remaining refactoring path:**
  1. Have `CaseEditorPanel.generateInterviews()` delegate to
     `CaseGenerator.buildInterviewScripts()`.
  2. Consider splitting `CaseGenerator` into `CaseFileGenerator` +
     `InterviewScriptBuilder`.

### Medium priority

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

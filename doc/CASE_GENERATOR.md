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
- `expirationDay` — optional in-game deadline (`0` = never expires)

#### Time-pressured leads

- `CaseLead.setExpirationDay(day)` marks a lead as expiring after a given
  in-game day.
- `CaseLead.isExpired(currentDay)` returns `true` when the deadline has passed
  and the lead was never discovered.
- At complexity **≥ 2**, `CaseGenerator.assignLeadExpiration()` gives roughly
  **30 %** of generated leads an `expirationDay` between **3 and 7**.

This creates optional time pressure: some leads stay available indefinitely,
while others become stale if the player waits too long.

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
By default each sub-branch must be **fully completed before the next branch is
unlocked** (sequential-unlock rule enforced by `isChildAvailable(int)`), but a
node can also be marked `parallel=true` so that all of its children are
available at once.

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
For complexity 3: **24 main-case leaf actions** total, plus an optional
**2-action side case**

#### Key API
| Method | Purpose |
|---|---|
| `isFullyComplete()` | True when the whole sub-tree is done |
| `getNextActiveChild()` | The first unlocked-but-incomplete child |
| `getNextAvailableAction()` | Recursively finds the first non-completed `ACTION` leaf (skipping fully-done branches) |
| `getAvailableActions()` | Returns all currently reachable `ACTION` leaves across parallel branches |
| `isChildAvailable(index)` | True when all prior siblings are complete |
| `setParallel(boolean)` / `isParallel()` | Enable or inspect parallel child availability |
| `complete()` | Mark a leaf `ACTION` node as done |

#### Parallel progression

- Each `PLOT_TWIST` phase is marked `parallel=true`, so its two
  `MAJOR_PROGRESS` branches can be pursued in either order.
- At complexity 3, the root node is also marked `parallel=true`, allowing the
  optional side-case branch to remain available alongside the main phases.
- `getNextAvailableAction()` still returns the first available action in
  insertion order; use `getAvailableActions()` when the UI or runtime needs the
  full set of simultaneously available investigation actions.

#### Side-case branch at complexity 3

At complexity **≥ 3**, `CaseGenerator.addSideCaseNode()` appends an extra
`PLOT_TWIST` child representing a **SIDE_CASE** branch:

- It is a self-contained mini-investigation with **1 major**, **1 minor**, and
  **2 `ACTION` leaves**:
  - `Investigate the side lead`
  - `Report findings`
- Its title is case-type specific (for example *"Side Case — Suspicious
  Associate of {subject}"* for murder or *"Side Case — Fence Operation Tip"*
  for theft).
- Because the root is marked `parallel=true`, this side case can be pursued at
  any time alongside the main investigative phases.

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

#### Personality-trait generation

The generator also creates hidden personality-trait maps for the key NPCs:

- `generateTraitMap()` assigns **3–5** `PersonalityTrait` values per NPC
  with non-zero scores from **−3 to +3**
- Traits are generated for the client, subject, victim (when present), and any
  newly created seed-contact NPCs
- The resulting maps are stored on the `CaseFile` as NPC personality data

#### Trait-colour narrative and leads

- `buildTraitColour(subject, traits, gender)` appends **1–2** narrative
  sentences to the case description based on strong likes / dislikes
  (typically traits with magnitude **≥ 2**)
- `addTraitDrivenLeads()` adds **1–2** additional
  `DiscoveryMethod.SURVEILLANCE` leads based on notable traits
- `traitLocation()` maps specific traits to plausible venues
  (library, gym, gallery district, bus station, etc.)
- `traitBehaviour()` turns strong positive / negative traits into behavioural
  snippets that feed those surveillance leads

This means personality traits are not just stored metadata — they actively
influence the opening description and create extra investigative threads.

#### Meeting dialogue (`MeetingQA`)

The client appointment is also pre-generated as a list of immutable
`MeetingQA` question/answer pairs stored on the `CaseFile` and consumed by
`MeetPopup` at runtime.

`buildMeetingDialogue(type, subject, objective, description, storyRoot)` always
creates:

1. **Four standard questions**
   - *What exactly do you need me to do?*
   - *Tell me more about the subject.*
   - *How long has this been going on?*
   - *Is there anyone else who knows about this?*
2. **One extra question per `PLOT_TWIST` phase** in the story tree, using the
   phase title and description as the Q&A content

The first four answers are case-type-aware and summarise the objective,
background, timeline, and likely contacts.  The per-phase questions ensure that
every major branch of the story tree is represented in the initial meeting.

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

#### Witness reliability

Each script also carries a `reliability` score from **0.5 to 1.0**:

- **1.0** — fully reliable (default)
- **0.5–0.9** — partially unreliable; some generated responses may be false

`InterviewScript.isUnreliable()` returns `true` when reliability is below 1.0.
The generator uses this score to flip some responses away from the truthful
path, creating witnesses who are mistaken, evasive, or contradictory without
changing the interview topics themselves.

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

#### Reliability by complexity

| Complexity | Reliability behaviour |
|---|---|
| 1 | Witnesses use the default reliability of **1.0** |
| 2 | ~40 % chance that the key witness is unreliable (**0.7–0.9**) |
| 3 | ~60 % chance that the key witness is unreliable (**0.5–0.8**) and an additional **Contradictory Witness** may be generated |

At complexity 3, the contradictory witness uses low reliability
(roughly **0.5–0.7**) and is designed to conflict with other testimony on
topics such as `WHEREABOUTS`, `LAST_CONTACT`, and `OBSERVATION`.

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
2–3 at complexity 3).  Six attributes describe each suspect, of which five
are testable for elimination (Opportunity always matches and cannot
eliminate a suspect):

| Attribute | Pool size | Subject value | Suspect rule |
|---|---|---|---|
| Hair Color | 6 | Random | Complexity-weighted match |
| Beard Style | 6 (males) or "none" | Random | Complexity-weighted match |
| Opportunity | 5 | Random | **Always matches** (all suspects present) |
| Access | 6 full + 6 partial | Random (full) | Complexity-weighted; non-matches: 40 % partial / 60 % differ |
| Has Motive | 2 | **Always true** | Complexity-weighted match |
| Alibi | 8 | Random (falsifiable) | Complexity-weighted match |

**Complexity weighting:** The match probability per attribute is no longer a
flat 50 %.  Instead it scales with complexity:

| Complexity | Match chance | Effect |
|---|---|---|
| 1 | 30 % | Easy — most attributes differ, quick elimination |
| 2 | 50 % | Moderate — balanced |
| 3 | 70 % | Hard — most attributes match, difficult to eliminate |

If a suspect's five testable attributes all accidentally match the true
perpetrator, one is randomly forced to differ (`random.nextInt(5)`).

**Partial access:** When a suspect does not fully match the perpetrator's
access, 40 % of the time they receive a "partial access" label (e.g., "had
access to the building but not the safe") rather than a completely different
access type. This requires the player to evaluate degree of access.

**Temporal alibi:** Each suspect is assigned a plausible alibi from a pool
of 8.  The perpetrator's alibi is always falsifiable; other suspects either
share it (suspicious, needs verification) or have a distinct one (verifiable,
can be cleared).

### 12. NPC table columns (admin tool)

The NPC data table in `CaseEditorPanel` has **21 columns** (indices 0–20):

| Index | Name | Notes |
|---|---|---|
| 0–10 | Role, Name, Gender, Age, Occupation, Cooperativeness, Honesty, Nervousness, Dead, Death Date/Time, Variance | Original character fields |
| 11 | Hair Color | Suspect attribute |
| 12 | Beard Style | Suspect attribute |
| 13 | Opportunity | Suspect attribute |
| 14 | Access | Suspect attribute (full or partial) |
| 15 | Has Motive | Suspect attribute (Boolean) |
| 16 | Phone Number | Format `555-XXXX` |
| 17 | Phone Discovered | Boolean |
| 18 | Default Location | Location display name |
| 19 | Personality Traits | Comma-separated "TRAIT:value" pairs |
| 20 | Alibi | Temporal alibi — elimination dimension |

### 13. Unknown facts, motive narratives & evidence chains

`generateUnknownFacts()` creates 7–12 facts with status `UNKNOWN` that
represent the core mysteries the player must solve.  Several are linked
into **evidence chains** via prerequisite dependencies:

| # | Category | Templates | Importance | Prerequisite | Avail Day |
|---|---|---|---|---|---|
| 1 | METHOD | 5 (murder) / 4 (other) | 5 | — | 0 |
| 2 | MOTIVE (primary) | 10 codes × 10 = ~100 | 5 | — | 0 |
| 2a | MOTIVE (red herring, complexity ≥ 2) | 10 × 10 × 4 wrappers | 2 | — | 0 |
| 2b | MOTIVE (secondary, complexity 3) | 10 × 10 × 4 connectors | 4 | #2 primary | 0 |
| 3 | EVIDENCE (weapon, murder) | 5 | 4 | — | 0 |
| 4 | DATE (timeline) | 4 | 4 | #3 weapon | 1 (murder) |
| 5 | ITEM (location) | 4 | 3 | — | 0 |
| 6 | RELATIONSHIP (accomplices) | 4 | 3 | — | 0 |
| 7 | DATE (alibi) | 4 | 4 | #4 timeline | 0 |
| 8 | ITEM (cover-up) | 4 | 3 | #5 location | 2 |

#### Procedural motive complexity

At complexity 1, there is a single primary motive.  At complexity ≥ 2, a
**red-herring motive** (status HIDDEN, importance 2) is added — a plausible
but false explanation that misleads the investigator until contradicted by
the true motive.  At complexity 3, a **secondary / layered motive** (status
UNKNOWN, importance 4) is chained to the primary — it becomes discoverable
only after the primary motive is found.

```
Complexity 1:  primary motive only
Complexity 2:  primary + red-herring motive
Complexity 3:  primary + red-herring + secondary motive (chained to primary)
```

`NarrativeTemplates` provides:
- `pickSecondaryMotiveCode(primaryCode)` — 10 logical pairings
- `buildSecondaryMotiveNarrative(secondaryCode, primaryCode, subject, victim)`
- `pickRedHerringMotiveCode(trueMotiveCode)` — random different code
- `buildRedHerringMotiveNarrative(herringCode, subject, victim)`

#### Evidence chain structure

```
Murder cases:
  weapon (#3) → timeline (#4, day 1) → alibi verification (#7)
  location (#5) → cover-up (#8, day 2)

Motive chains (complexity 3):
  primary motive (#2) → secondary motive (#2b)

generateFacts() chains:
  scene evidence → DNA analysis (day 2) → toxicology report (day 4)
  scene item → digital forensics (day 1) → financial records (day 3)

Story tree chains (complexity ≥ 2):
  Within each major block, action N+1's success fact requires action N's
  discovery.  At complexity 3, evidence-type facts also get 1–3 day delays.
```

#### Fact table columns (14 total)

| Index | Name | Type | Notes |
|---|---|---|---|
| 0 | ID | String | `fact-N` |
| 1 | Category | String | DATE, RELATIONSHIP, ITEM, EVIDENCE, METHOD, MOTIVE |
| 2 | Fact | String | Fact text |
| 3 | Status | String | KNOWN, HIDDEN, UNKNOWN |
| 4 | Date (epoch) | Long | In-game timestamp |
| 5 | Char1 | String | First character reference |
| 6 | Char2 | String | Second character reference |
| 7 | Rel Type | String | Relationship type |
| 8 | Item ID | String | Item identifier |
| 9 | Evidence ID | String | Evidence type identifier |
| 10 | Importance | Integer | 0–5 |
| 11 | Prerequisite Fact ID | String | ID of gating fact (evidence chain) |
| 12 | Availability Day | Integer | In-game day when available (forensics) |
| 13 | Source | String | Fact origin: CASE, POLICE, or DISCOVERED |

**Source categories:**
- **CASE** — client-provided information from the case briefing (seed facts,
  case report date, client relationships).
- **POLICE** — official police/forensic investigation results (scene evidence,
  DNA analysis, toxicology, digital forensics, financial records, death dates).
  All forensic lab results end up under POLICE.
- **DISCOVERED** — facts uncovered by the investigator through interviews,
  surveillance, or research (hidden disputes, unknown items, trait-driven facts).

The `FactSource` enum and `KnownFact` class in the core model mirror this
categorisation.  `CaseFile.getKnownFactsBySource(FactSource)` returns facts
filtered by origin.

`buildMotiveNarrative(motiveCode, subject, victim)` selects from a pool
of ~10 unique sentence templates per motive code (10 codes, ~100 templates
total).  Each template uses the `subject` and `victim` names directly.

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

### 16. Case description & objective templates

Case descriptions and objectives are now driven by an external JSON template
file: **`assets/text/case_templates_en.json`**.

#### Template structure
```json
{
  "descriptions": {
    "MISSING_PERSON.1": ["...", ...],
    "MISSING_PERSON.2": ["...", ...],
    "MISSING_PERSON.3": ["...", ...]
  },
  "objectives": {
    "MISSING_PERSON.1": ["...", ...],
    ...
  },
  "caseSeeds": {
    "MISSING_PERSON": [
      {
        "facts": ["..."],
        "leads": [{"hint": "...", "description": "...", "method": "INTERVIEW"}],
        "contacts": [{"name": "...", "role": "...", "reason": "..."}]
      }
    ]
  },
  "caseDescriptions": {
    "MISSING_PERSON": {
      "roles": ["Client", "Subject", "Key Witness"],
      "extraSuspectsComplexity2": "1-2",
      "extraSuspectsComplexity3": "2-3",
      "suspectLabels": ["Friend", "Neighbour"],
      "summary": "..."
    }
  }
}
```

Keys follow the pattern `CASE_TYPE.complexity` where complexity is 1, 2, or 3.
Each pool contains **12 template strings** that use `$client`, `$subject`,
`$victim`, `$pronounCap`, and `$pron` placeholders.

#### Complexity tiers
| Complexity | Description style | Objective style |
|---|---|---|
| 1 | Straightforward narrative; single suspect, clear motive | Direct single-goal objective |
| 2 | Added complications: conflicting witnesses, secondary evidence, false leads | Multi-part objective with secondary investigation thread |
| 3 | Deep layering: multiple evidence chains, third-party involvement, cross-referenced data | Comprehensive objective covering primary, secondary, and tertiary investigation threads |

#### Data model — `CaseTemplateData`
- Parsed by `CaseTemplateData.parse(String json)` using libGDX `JsonReader`
- `pickDescription(caseTypeName, complexity, rng)` — random template from
  the matching pool; falls back to complexity 1 if no pool exists
- `pickObjective(caseTypeName, complexity, rng)` — same pattern
- `pickSeed(caseTypeName, rng)` — picks a coherent seed bundle for the case
- `getCaseDescription(caseTypeName)` — returns admin-facing NPC-role metadata
- Loaded at startup by `GameDataManager.loadCaseTemplates()` and by
  `CaseEditorPanel.loadCaseTemplates()` (admin tool)

#### Integration
- `CaseGenerator.buildDescription(type, client, subject, victim, clientGender,
  subjectGender, rng, complexity, caseTemplateData)` — uses template data when
  available; falls back to built-in hardcoded templates when `null`
- `CaseGenerator.buildObjective(type, client, subject, victim, rng, complexity,
  caseTemplateData)` — same fallback pattern
- `CaseGenerator.resolveCasePlaceholders()` — handles `$client`, `$subject`,
  `$victim`, `$pronounCap`, `$pron` token substitution
- Complexity is determined **before** description generation so templates can
  vary by difficulty level

#### `caseSeeds` — coherent starting information

`caseSeeds` is keyed by case type name only (for example `MURDER`), not by
complexity.  Each seed bundles together:

- `facts[]` — appended to the case description under **"What we know so far:"**
  and added as starting clues / CASE-sourced known facts
- `leads[]` — converted into `CaseLead`s and listed in the objective under
  **"Initial lines of enquiry:"**
- `contacts[]` — turned into initial contact entries under
  **"Initial contacts:"**

Seed contacts support both principal placeholders (`$client`, `$subject`,
`$victim`) and generic labels.  When a generic label is used, the generator
creates a concrete NPC name and stores a CASE-sourced known fact in the form:

> `Contact: Alice Smith — Neighbour (may have noticed unusual activity)`

This keeps the opening case brief, case file, and generated NPC set aligned.

#### `caseDescriptions` — NPC-role template metadata

The `caseDescriptions` section stores per-case-type metadata for the admin
tool's **Description & Objective** and **NPC Characters** workflow:

- `roles[]` — base NPC roles expected for the case type
- `extraSuspectsComplexity2` / `extraSuspectsComplexity3` — human-readable
  ranges for additional suspects
- `suspectLabels[]` — labels for those extra suspect roles
- `summary` — short human-readable explanation shown in the editor

These entries are edited through `CaseDescriptionTemplatePanel`, which merges
changes back into `case_templates_en.json` while preserving the `descriptions`,
`objectives`, and `caseSeeds` sections.

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

# Case Generation Pipeline — Steps, Stats & Future Improvements

> Comprehensive reference for every stage of procedural case generation,  
> including exact probabilities, pool sizes, and planned enhancements.

---

## Table of Contents

1. [Step 1 — Case Type & Complexity Selection](#step-1--case-type--complexity-selection)
2. [Step 2 — NPC Character Generation](#step-2--npc-character-generation)
3. [Step 3 — Suspect Attribute Generation](#step-3--suspect-attribute-generation)
4. [Step 4 — Relationship Generation](#step-4--relationship-generation)
5. [Step 5 — Description & Objective](#step-5--description--objective)
6. [Step 6 — Leads Generation](#step-6--leads-generation)
7. [Step 7 — Facts Generation](#step-7--facts-generation)
8. [Step 8 — Story Tree Generation](#step-8--story-tree-generation)
9. [Step 9 — Unknown Facts (Core Mysteries)](#step-9--unknown-facts-core-mysteries)
10. [Step 10 — Interview Generation](#step-10--interview-generation)
11. [Overall Stats Summary](#overall-stats-summary)
12. [Future Improvements](#future-improvements)

---

## Step 1 — Case Type & Complexity Selection

**Source:** `CaseEditorPanel` — Case Type tab

### Inputs
| Control          | Range       | Distribution        |
|------------------|-------------|---------------------|
| Case Type        | 8 types     | User-selected or random (uniform) |
| Motive           | 10 codes    | Filtered by case type via `case_types` field |
| Complexity       | 1, 2, or 3  | 33.3 % each (when random: `1 + random.nextInt(3)`) |

### Case Types (8)

| Code                 | Display Name          | Difficulty | Base NPCs |
|----------------------|-----------------------|------------|-----------|
| MISSING_PERSON       | Missing Person        | 1–5        | 5         |
| INFIDELITY           | Infidelity            | 1–4        | 5         |
| THEFT                | Theft                 | 1–5        | 5         |
| FRAUD                | Fraud                 | 4–9        | 5         |
| BLACKMAIL            | Blackmail             | 3–7        | 5         |
| MURDER               | Murder                | 5–10       | 6         |
| STALKING             | Stalking              | 2–6        | 5         |
| CORPORATE_ESPIONAGE  | Corporate Espionage   | 5–10       | 5         |

### Motive Categories (10)

| Code          | Applicable Case Types |
|---------------|----------------------|
| FINANCIAL     | All                  |
| REVENGE       | Murder, Blackmail, Stalking |
| JEALOUSY      | Infidelity, Murder, Stalking |
| COERCION      | Blackmail, Fraud, Corporate Espionage |
| POWER         | Fraud, Murder, Corporate Espionage |
| SELF_DEFENSE  | Murder               |
| IDEOLOGY      | Corporate Espionage  |
| CONCEALMENT   | Murder, Fraud, Blackmail |
| PASSION       | Murder, Infidelity, Stalking |
| LOYALTY       | Murder, Corporate Espionage |

### Complexity Impact

| Complexity | Extra Suspects | Total NPCs (typical) | Story Phases | Leaf Actions |
|------------|---------------|----------------------|-------------|-------------|
| 1          | 0             | 5–6                  | 1           | 8           |
| 2          | 1–2 (50/50)   | 6–8                  | 2           | 16          |
| 3          | 2–3 (50/50)   | 7–9                  | 3           | 24          |

---

## Step 2 — NPC Character Generation

**Source:** `CaseEditorPanel.generateNpcs()`

### Base Roles per Case Type

| Case Type            | Roles                                                              |
|----------------------|---------------------------------------------------------------------|
| Missing Person       | Client, Subject (Missing), Witness, Last-Known Contact, Neighbour   |
| Infidelity           | Client, Subject (Partner), Other Party, Mutual Friend, Witness      |
| Theft                | Client (Victim), Subject (Suspect), Witness, Insurance Adjuster, Fence/Dealer |
| Fraud                | Client (Victim), Subject (Perpetrator), Accountant, Business Associate, Insider Witness |
| Blackmail            | Client (Victim), Subject (Blackmailer), Witness, Intermediary, Confidant |
| Murder               | Client, Subject (Suspect), Victim, Key Witness, Victim's Associate, Police Contact |
| Stalking             | Client (Victim), Subject (Stalker), Neighbour Witness, Ex-Partner, Friend of Client |
| Corporate Espionage  | Client (Employer), Subject (Leak), Rival Contact, Trusted Colleague, IT Specialist |

### Additional Suspect Labels (up to 5)

When complexity > 1, the generator appends from this pool:

1. "Suspect — Neighbour"
2. "Suspect — Colleague"
3. "Suspect — Associate"
4. "Suspect — Ex-Partner"
5. "Suspect — Acquaintance"

### Attribute Generation

| Attribute          | General Range  | Client Override | Subject/Suspect Override |
|--------------------|----------------|-----------------|--------------------------|
| Gender             | 50 % M / 50 % F | —              | —                        |
| Age                | 22–64          | —               | —                        |
| Cooperativeness    | 2–9            | **6–9** (high)  | **1–5** (low)            |
| Honesty            | 2–9            | **5–9** (high)  | —                        |
| Nervousness        | 1–9            | —               | **4–9** (high)           |

### Death State (Murder only)

Only the **Victim** role is marked dead.

| Property        | Value / Distribution |
|-----------------|---------------------|
| Days ago        | 1–14 (uniform)      |
| Hour            | 0–23 (uniform)      |
| Minute          | 0–59 (uniform)      |

**Death-time variance:**

| Roll (`nextInt(10)`) | Result        | Probability |
|---------------------|---------------|-------------|
| 0–2                 | Precise (0 min) | **30 %**   |
| 3–8                 | 15–180 min range | **60 %**  |
| 9                   | Unknown / body missing (−1) | **10 %** |

### Phone Number Generation

Every NPC receives a phone number in the format `555-XXXX` (suffix 0100–9999).
The number is generated at creation time by `generatePhoneNumber()`.

| Property          | Value                              |
|-------------------|------------------------------------|
| Format            | `555-XXXX`                         |
| Suffix range      | 0100–9999 (uniform)                |
| Unique per NPC    | Not enforced (collision unlikely)  |

### Phone Number Discovery

Each NPC's phone number starts as **hidden** (`phoneDiscovered = false`).
The player must discover it through investigation before they can call the NPC.

| Role              | Phone Discovered at Start | Notes                                     |
|-------------------|--------------------------|-------------------------------------------|
| **Client**        | **Always true**          | The client hires the detective, so their number is known |
| All other NPCs    | **false**                | Must be discovered via interviews or leads |

**Discovery methods:**
- **Interview (CONTACT_INFO topic):** Other NPCs can reveal a character's phone number when asked. Gated by **Charisma ≥ 4** (client/associate sources) or **Charisma ≥ 5** (witness source).
- **Story progression:** Discovering facts may also reveal phone numbers as a game mechanic.

### Default Location Assignment

Each NPC is assigned a default location — the place where they can usually
be found without an appointment. Locations are chosen by `locationForRole()`.

| Role Pattern               | Location Pool                                    |
|----------------------------|--------------------------------------------------|
| Police Contact             | Police Station (fixed)                           |
| Accountant / Insurance / IT / Colleague | Office (fixed)                       |
| Neighbour                  | Their Home (fixed)                               |
| Friend                     | Café, Bar, Restaurant, Public Park               |
| Associate                  | Office, Restaurant, Hotel Lobby                  |
| Witness                    | Café, Diner, Public Park, Their Home             |
| Fence / Dealer             | Bar, Warehouse District, Street Market           |
| Intermediary               | Café, Hotel Lobby, Bus Station                   |
| Confidant                  | Church, Café, Library                            |
| Rival                      | Bar, Restaurant, Office                          |
| Other Party                | Gym, Bar, Restaurant                             |
| Contact                    | Café, Diner, Bus Station                         |
| Client                     | Café, Office, Restaurant, Their Home             |
| Subject                    | Bar, Their Home, Gym                             |
| All others                 | Random from full pool (18 locations)             |

**Full location pool (18):**
Café, Bar, Office, Public Park, Library, Restaurant, Their Home, Gym,
Warehouse District, Church, Hospital, Police Station, Diner, Hotel Lobby,
Parking Garage, Bus Station, Street Market, Courthouse

---

## Step 3 — Suspect Attribute Generation

**Source:** `CaseEditorPanel.generateNpcs()` lines 1362–1422

Every NPC with a role starting with "Subject" or "Suspect" receives distinguishing
attributes. The **true perpetrator** (Subject) matches all criteria; additional
suspects share opportunity but **at least one** other attribute differs.

### Attribute Pools

| Attribute       | Pool                                                                              | Size |
|-----------------|------------------------------------------------------------------------------------|------|
| Hair Color      | black, brown, blonde, red, gray, white                                            | 6    |
| Beard Style     | clean-shaven, stubble, short beard, long beard, goatee, moustache (males only)    | 6    |
| Opportunity     | was near the scene, had keys to the building, was seen in the area, lives close by, visited the location earlier that day | 5 |
| Access (full)   | owns a firearm, had access to the victim's home, has a key to the office, had access to the safe, drives a vehicle matching witness description, works in the same building | 6 |
| Access (partial)| had access to the building but not the safe, had a visitor pass but not a permanent key, could enter the lobby but not the restricted area, had daytime access only (not after hours), had access to the grounds but not the main office, could reach the car park but not the residence | 6 |
| Has Motive      | true / false                                                                       | 2    |
| Alibi           | claims to have been at home alone, says at a bar with friends, working late at the office, at a family dinner, out of town, at the gym, at a community event, at a cinema | 8 |

### Perpetrator (Subject Role) — 100 % Match

| Attribute    | Value                          |
|-------------|--------------------------------|
| Hair Color  | Random from pool (1/6)         |
| Beard Style | Random from pool (males) or "none" (females) |
| Opportunity | Random from pool (1/5)         |
| Access      | Random from full-access pool (1/6) |
| Has Motive  | **Always true**                |
| Alibi       | Random from pool (1/8) — always falsifiable |

### Red-Herring Suspects — Differentiation Rules

Each additional suspect rolls **independently** per attribute, with the match
probability **weighted by complexity** to control elimination difficulty:

| Complexity | Match Threshold | Difficulty | Effect                     |
|------------|----------------|------------|----------------------------|
| 1          | **30 %**       | Easy       | Few attributes match — suspects are quickly eliminated |
| 2          | **50 %**       | Moderate   | Balanced matching — standard investigation |
| 3          | **70 %**       | Hard       | Most attributes match — suspects are difficult to tell apart |

| Attribute   | Match Perpetrator?    | Probability |
|-------------|----------------------|-------------|
| Hair Color  | `random.nextInt(100) < matchThreshold` | Complexity-weighted |
| Beard Style | `random.nextInt(100) < matchThreshold` | Complexity-weighted |
| Access      | `random.nextInt(100) < matchThreshold` | Complexity-weighted (non-matches: 40 % partial, 60 % full differ) |
| Has Motive  | `random.nextInt(100) < matchThreshold` | Complexity-weighted |
| Alibi       | `random.nextInt(100) < matchThreshold` | Complexity-weighted |

**Guarantee:** If all five match (probability depends on complexity), one is
randomly forced to differ (`random.nextInt(5)` picks which one flips → 20 % each).

**Partial access:** When a suspect's access does not match the perpetrator,
there is a 40 % chance the suspect receives **partial access** (e.g., "had
access to the building but not the safe") instead of a completely different
access type. This adds nuance — the player must carefully evaluate the degree
of access, not just whether the suspect had any.

**All suspects always have opportunity** — they had a plausible reason to be
present. Opportunity alone cannot eliminate a suspect.

### Elimination Logic for Players

The player thins out suspects by discovering facts about:

| Dimension       | Example Clue                                         |
|-----------------|------------------------------------------------------|
| **Physical Looks** | "Witness saw someone with blonde hair leaving"       |
| **Access**       | "The killer had access to the safe" (full vs partial) |
| **Motive**       | "Only someone with a financial grudge would…"        |
| **Alibi**        | "Suspect claims they were at the gym, but CCTV contradicts this" |

---

## Step 4 — Relationship Generation

**Source:** `CaseEditorPanel.generateRelationships()`

### Client ↔ Subject Relationship

Inferred from case type:

| Case Type            | Relationship Type    |
|----------------------|---------------------|
| Missing Person       | Family              |
| Infidelity           | Partner             |
| Theft                | Acquaintance        |
| Fraud                | Business Associate  |
| Blackmail            | Acquaintance        |
| Murder               | Acquaintance        |
| Stalking             | Ex-Partner          |
| Corporate Espionage  | Employer            |

**Opinion range:** −20 to +20 (uniform, `−20 + nextInt(41)`)

### Other NPC Relationships

| Link                    | Opinion Range    | Notes                         |
|-------------------------|-----------------|-------------------------------|
| NPC → Client or Subject | −10 to +40      | Alternates by index (even→Client, odd→Subject) |
| Inter-NPC (if ≥4 NPCs)  | −20 to +40      | 1 random pair, random type from 11 relationship types |

### Relationship Types Pool (11)

Family, Friend, Colleague, Acquaintance, Rival, Employer, Employee,
Neighbour, Partner, Ex-Partner, Business Associate

---

## Step 5 — Description & Objective

**Source:** `CaseGenerator.buildDescription()` / `buildObjective()` +
`assets/text/case_templates_en.json`

- **12 description templates per case type × 3 complexity tiers = 288 total**
  (8 case types × 12 × 3)
- **12 objective templates per case type × 3 complexity tiers = 288 total**
- Templates loaded from `case_templates_en.json` via `CaseTemplateData.parse()`
- Placeholders: `$client`, `$subject`, `$victim`, `$pronounCap`, `$pron`
- Resolved by `CaseGenerator.resolveCasePlaceholders()`
- `CaseGenerator.capitalizeSentences()` ensures sentence-initial capitals
- Complexity is determined **before** description/objective generation
- Falls back to built-in hardcoded templates (3 per type) when JSON is absent

### Complexity tiers

| Tier | Description style | Objective style |
|---|---|---|
| 1 | Straightforward narrative; single suspect, clear problem | Direct single-goal |
| 2 | Added complications: conflicting witnesses, secondary evidence | Multi-part with secondary thread |
| 3 | Deep layering: multiple evidence chains, third parties | Comprehensive multi-objective |

### Example (Murder, complexity 2)

> *"Alex Turner doesn't believe the official story. The coroner called it an
> accident, but the family called it murder. Mike Rhodes was the last person
> seen with Jordan Voss. A second witness has now come forward with a different
> account of that evening."*

---

## Step 6 — Leads Generation

**Source:** `CaseEditorPanel.generateLeads()`

### Lead Count

| Complexity | Total Leads |
|------------|-------------|
| 1          | 3           |
| 2          | 4           |
| 3          | 5           |

Formula: `leadCount = 2 + complexity`

### Discovery Methods (6)

Each lead is assigned a random method from `category_en.json`:

1. Background Check
2. Documents
3. Forensics
4. Interview
5. Physical Search
6. Surveillance

### Red Herring Classification (in Story Tree)

When leads appear in the KNOWN_FACTS section of the story tree:

```java
boolean redHerring = random.nextInt(100) < 25;  // 25% chance
```

| Outcome      | Probability |
|-------------|-------------|
| Genuine lead | **75 %**   |
| Red herring  | **25 %**   |

### Red Herring Template Pool (8)

The core `CaseGenerator` has 8 red herring lead templates, e.g.:
- Associate seen in area but for unrelated reasons
- Surveillance footage resembles subject but is a different person
- Anonymous tip from unreliable source
- Phone records show calls to innocent party
- Forensic traces initially point to subject but ruled out

---

## Step 7 — Facts Generation

**Source:** `CaseEditorPanel.generateFacts()`

### Fact Categories

| Category     | Purpose                         |
|-------------|----------------------------------|
| DATE         | Timeline / temporal information  |
| RELATIONSHIP | Character connections            |
| ITEM         | Physical objects                 |
| EVIDENCE     | Forensic / scientific findings   |
| METHOD       | How the crime was committed      |
| MOTIVE       | Why the crime was committed      |

### Fact Status

| Status    | Meaning                                  |
|-----------|------------------------------------------|
| KNOWN     | Starting information, visible to player  |
| HIDDEN    | Must be discovered through investigation |
| UNKNOWN   | Core mystery — what the player must solve |

### Generated Facts Breakdown

**DATE facts (3):**
- Victim death date/time (KNOWN if available, HIDDEN if unknown) — importance **5**
- Subject last seen (HIDDEN) — importance **3**
- Client reported case (KNOWN) — importance **0**

**RELATIONSHIP facts (2 + per-relationship):**
- Client knew victim personally (KNOWN) — importance **0**
- Subject and victim had prior dispute (HIDDEN) — importance **4**
- Each table relationship becomes a KNOWN (if client-related) or HIDDEN fact

**ITEM facts (2):**

| Pool     | Options (random 1-of)                              | Status |
|----------|-----------------------------------------------------|--------|
| Known    | Document, Mobile Phone, Envelope (3 options)        | KNOWN  |
| Hidden   | Kitchen Knife, Bullet Casing, Firearm, Syringe, Burned Material, Letter (6 options) | HIDDEN |

**EVIDENCE facts (2):**

| Pool     | Options (random 1-of)                              | Status |
|----------|-----------------------------------------------------|--------|
| Known    | Fingerprints, Blood, Hair (3 options)               | KNOWN  |
| Hidden   | DNA, Ballistics, Toxicology, Digital Data, Gunshot Residue (5 options) | HIDDEN |

---

## Step 8 — Story Tree Generation

**Source:** `CaseEditorPanel.generateStoryTree()`

### Tree Structure

```
Story Root [CaseType / Motive / complexity=N]         (parallel=true at c≥3)
├── [KNOWN_FACTS] Known Facts & Leads
│   ├── [FACT] ...              (all KNOWN-status facts)
│   ├── [LEAD] ...              (75% genuine leads)
│   ├── [LEAD:RED_HERRING]      (25% red herring leads)
│   └── [LEAD ... EXPIRES:day N] (30% of leads at complexity ≥ 2)
├── [UNKNOWN_FACTS] Unknown Facts (To Be Solved)
│   └── [FACT:UNKNOWN] ...      (all UNKNOWN-status facts)
├── [PLOT_TWIST:PARALLEL] Initial Investigation       ← always; majors parallel
│   ├── [MAJOR] ...  ← can be pursued in any order
│   │   ├── [MINOR] ...
│   │   │   ├── [ACTION] ...
│   │   │   │   └── [RESULT] ... | req:ATTR:N | ok:fact:X | fail:...
│   │   │   └── [ACTION] ...
│   │   └── [MINOR] ...
│   └── [MAJOR] ...  ← can be pursued in any order
├── [PLOT_TWIST:PARALLEL] Plot Twist 1                ← if complexity ≥ 2
│   └── (same structure)
├── [PLOT_TWIST:PARALLEL] Plot Twist 2                ← if complexity ≥ 3
│   └── (same structure)
└── [SIDE_CASE:PARALLEL] Side Case — ...              ← if complexity ≥ 3
    └── [MAJOR] Side Lead
        └── [MINOR] Quick Enquiry
            ├── [ACTION] Investigate the side lead
            └── [ACTION] Report findings
```

### Non-Linear Progression

Each PLOT_TWIST phase sets `parallel=true`, so both MAJOR_PROGRESS branches
within it are available simultaneously — the player chooses which thread to
pursue first.  At complexity ≥ 3, the story root itself is also parallel,
allowing an optional SIDE_CASE branch to be investigated at any time.

**Time-pressured leads:** ~30 % of leads at complexity ≥ 2 receive an
`expirationDay` (3–7 in-game days).  `CaseLead.isExpired(currentDay)` lets
the runtime determine if a lead is no longer actionable.

### Node Counts per Complexity

| Level          | Per Phase | Complexity 1 | Complexity 2 | Complexity 3 |
|---------------|-----------|-------------|-------------|-------------|
| PLOT_TWIST     | 1         | 1           | 2           | 3 + 1 side  |
| MAJOR_PROGRESS | 2         | 2           | 4           | 6 + 1 side  |
| MINOR_PROGRESS | 4         | 4           | 8           | 12 + 1 side |
| ACTION (leaf)  | 8         | **8**       | **16**      | **24 + 2 side** |
| RESULT         | 8         | 8           | 16          | 24 + 1 side |

### Action Title Generation

**Source:** `CaseEditorPanel.buildActionTitle(minor, action, subject)`

Each MINOR node has two ACTION children. The action titles are drawn from
pools determined by which minor slot and action slot they occupy:

| Minor | Action | Category          | Pool (3 options each)                                            |
|-------|--------|-------------------|------------------------------------------------------------------|
| 1     | 1      | Photography/Scene | "Photograph the scene", "Sketch the layout", "Map entry and exit points" |
| 1     | 2      | Evidence Collection | "Collect physical evidence", "Bag and tag trace samples", "Recover latent fingerprints" |
| 2     | 1      | Interview         | "Interview a contact of {s}", "Speak to a known associate of {s}", "Question a neighbour of {s}" |
| 2     | 2      | Documents/Records | "Review documents or records", "Analyse financial records", "Search public records for {s}" |

`{s}` is replaced with the subject's name at generation time.

### Action Node — Attribute Requirements

**Source:** `CaseEditorPanel.attributesForAction(actionTitle)`

Each ACTION has a random skill check. The attribute is chosen from a pool
that is **constrained by action type** — not from the full set of 7. This
ensures the narrative connection between the action and the skill check
makes logical sense (e.g., evidence collection never requires CHARISMA).

| Action Category       | Prefixes                               | Attribute Pool                         |
|-----------------------|----------------------------------------|----------------------------------------|
| Evidence Collection   | Collect, Bag, Recover                  | `PERCEPTION`, `INTELLIGENCE`, `STEALTH` |
| Interview/Conversation | Interview, Speak, Question            | `CHARISMA`, `EMPATHY`, `INTIMIDATION`  |
| Documents/Records     | Review, Analyse, Search                | `INTELLIGENCE`, `MEMORY`, `PERCEPTION` |
| Photography/Scene     | Photograph, Sketch, Map entry          | `PERCEPTION`, `MEMORY`, `STEALTH`      |
| Fallback (unmatched)  | —                                      | All 7 attributes                       |

**Threshold:** 2–5 (uniform, `2 + nextInt(4)`)

### Result Description Pools

**Source:** `CaseEditorPanel.buildResultDescription(actionTitle)`

Each RESULT node gets a short description randomly chosen from a pool
specific to the action category. Each category has **two sub-pools of 3 options**
(one pool is selected at random, then one item from that pool):

| Action Category    | Sub-pool A                                                          | Sub-pool B                                                           |
|--------------------|---------------------------------------------------------------------|----------------------------------------------------------------------|
| Photography/Scene  | Inconsistency in official report / contradicts coroner / disturbance inconsistent with timeline | Hidden marking/tag visible / different point of origin / background connects to known location |
| Evidence Collection | Trace evidence links to key person / sample matches known substance / fingerprint places third party | Concealed item — origin requires investigation / secondary sample widens suspect pool / low-grade trace inconclusive but directional |
| Interview          | Hidden relationship revealed / secret meeting contradicts alibi / neighbour saw something unreported | Information disclosed under pressure / associate lets slip a detail / subject's evasiveness is itself a clue |
| Documents/Records  | Record anomaly contradicts timeline / document trail to unknown account / payment that shouldn't exist | Forged/altered entry identified / gap suggests deliberate concealment / cross-reference reveals second individual |

### Fact Category Mapping

**Source:** `CaseEditorPanel.categoryForAction(actionTitle)`

| Action Prefixes                                          | Fact Category  |
|----------------------------------------------------------|---------------|
| Photograph, Sketch, Collect, Bag, Recover, Map entry    | EVIDENCE      |
| Interview, Speak, Question                               | RELATIONSHIP  |
| Review, Analyse, Search (and all others)                 | ITEM          |

### Action Outcomes

| Branch  | Creates              | Importance | Probability |
|---------|---------------------|------------|-------------|
| Success | HIDDEN fact (high)   | 3–5        | Based on player attribute ≥ threshold |
| Failure | HIDDEN fact (low) **or** Lead | 0–2 (fact) | 50 % fact / 50 % lead |

### Inline Fact Templates (per action type)

**Source:** `CaseEditorPanel.buildInlineFact(actionTitle, subject, victim, phase, major, highImportance)`

Facts created by action outcomes use `{subject}` and `{victim}` substitution.
Each category has 3 high-importance and 3 low-importance templates:

| Action Category     | High-Importance Templates (3)                                                  | Low-Importance Templates (3)                                         |
|--------------------|-------------------------------------------------------------------------------|----------------------------------------------------------------------|
| Photography/Scene   | Second set of footprints near victim / door forced from inside implicating subject / unaccounted exit revealed | No obvious signs of struggle / layout consistent with initial report / minor damage unrelated to incident |
| Evidence Collection | Subject placed at scene within critical window / subject's fingerprints + victim's blood on recovered item / lab links substance to toxin in victim | Common fibre sample — inconclusive / low-quality latent print — partial match pending / residue may be cleaning product |
| Interview           | Witness confirms seeing subject near victim / associate reveals secret arrangement / interviewee discloses subject threatened victim | Contact barely knows subject / neighbour heard raised voices but can't identify / associate repeats official story |
| Documents/Records   | Large payment around incident date / shell company connection / digital records show search for victim's routine | Routine transactions — no irregularities / subject listed at different address / no prior criminal record |

### Success & Failure Narratives

**Source:** `CaseEditorPanel.buildAttributeSuccessNarrative(attr, actionTitle)` and
`buildAttributeFailureNarrative(attr, actionTitle)`

When the player meets or fails the attribute threshold on a RESULT node,
a context-aware narrative sentence is displayed. The narrative is determined
by the **combination of attribute × action type**, ensuring logical coherence
(e.g., a PERCEPTION check on evidence collection describes spotting a missed
detail, not charming a guard).

Each of the 7 attributes has a specific sentence for each of the 4 action
categories (evidence, interview, document, photo) plus a generic fallback.
That gives **7 × 5 = 35 success narratives** and **7 × 5 = 35 failure narratives**
(70 total).

#### Success Narrative Examples

| Attribute      | Evidence Collection                                      | Interview                                                | Documents/Records                                       | Photography/Scene                                       |
|---------------|----------------------------------------------------------|----------------------------------------------------------|---------------------------------------------------------|---------------------------------------------------------|
| PERCEPTION    | Sharp eye catches something others missed                | Picks up on a micro-expression                           | Mismatched date jumps out                               | Odd reflection captured; reveals critical detail        |
| INTELLIGENCE  | Reconstructs sequence from physical evidence alone       | Exact right question cracks composure                    | Cross-references dates and figures, spots anomaly       | *(uses generic fallback)*                                |
| CHARISMA      | Charms perimeter officer into granting access            | Warm smile earns trust; they open up                     | Friendly rapport gets access to non-public records      | Conversation gets access to better vantage point        |
| INTIMIDATION  | Commanding presence clears the area                      | Stern look makes cooperation mandatory                   | Authoritative demand produces files without delay       | *(uses generic fallback)*                                |
| EMPATHY       | Senses something personal about item arrangement         | Reads tension; gently presses at right moment            | Senses fear in report writer; finds buried detail       | *(uses generic fallback)*                                |
| MEMORY        | Remembers serial number; confirms item was moved         | Something said triggers a memory; names the detail       | Detail from earlier briefing maps onto this record      | Compares to earlier photo; spots moved object           |
| STEALTH       | Moves quietly; collects evidence before anyone notices   | Observes from distance before approaching                | Slips into records room unnoticed                       | Photographs from concealed position; raw state tells story |

#### Failure Narrative Examples

| Attribute      | Evidence Collection                                      | Interview                                                | Documents/Records                                       | Photography/Scene                                       |
|---------------|----------------------------------------------------------|----------------------------------------------------------|---------------------------------------------------------|---------------------------------------------------------|
| PERCEPTION    | Scans area carefully but nothing stands out               | Misses subtle cue in their expression                    | Numbers blur; anomaly hides in plain sight              | Misses critical angle; shots add nothing new            |
| INTELLIGENCE  | Evidence doesn't connect to anything known yet            | Wrong question; they steer conversation away             | Paperwork volume/complexity is overwhelming             | *(uses generic fallback)*                                |
| CHARISMA      | Officer on guard isn't swayed; spots secondary entrance   | Approach falls flat; conversation shut down (overhears something on way out) | Clerk turns you away; need better credentials           | Bystander blocks best angle; mediocre shots with one odd background detail |
| INTIMIDATION  | Challenged on right to be here; forced to back down (glimpses something) | Person won't stand aside; recognise connection to subject | Clerk refuses restricted file; nervous glance at drawer is a clue | *(uses generic fallback)*                                |
| EMPATHY       | Sterile environment yields nothing to instinct            | Try to connect but they remain guarded; defensiveness is telling | Nothing strikes emotional chord in statements           | *(uses generic fallback)*                                |
| MEMORY        | Feel like you've seen item before; case reference escapes | They mention something that should ring a bell           | Relevant detail just out of reach; review notes         | Can't recall earlier photos; miss what changed          |
| STEALTH       | Presence noticed; forced to abandon but pocketed one item | They spot you watching before you're ready               | Records clerk spots you in restricted section           | Camera spotted; asked to leave but notice protected area |

---

## Step 9 — Unknown Facts (Core Mysteries)

**Source:** `CaseEditorPanel.generateUnknownFacts()`

These represent what the player must ultimately discover to solve the case.
Each has status `UNKNOWN` and a concrete "truth" value.

### Unknown Facts (Murder Case — 8 facts)

| #  | Category       | Template Pool Size | Importance | Example                                                |
|----|---------------|-------------------|------------|--------------------------------------------------------|
| 1  | METHOD         | 5                 | **5**      | "Subject killed victim by poison / strangulation / head trauma / stabbing / pushed from height" |
| 2  | MOTIVE         | 10 codes × 3–4 templates each | **5** | Selected motive narrative from `buildMotiveNarrative()` |
| 3  | ITEM (Weapon)  | 5                 | **4**      | Kitchen knife / brass paperweight / toxin / cord / unregistered firearm |
| 4  | DATE (Timeline)| 4                 | **4**      | "10 PM–midnight window / 45-min footage gap / power outage / phone records" |
| 5  | EVIDENCE (Location) | 4            | **3**      | Back office / residence study / storage unit / car park |
| 6  | RELATIONSHIP (Accomplices) | 4    | **3**      | Acted alone / single accomplice / unidentified associate / coerced colleague |
| 7  | DATE (Alibi)   | 4                 | **4**      | CCTV contradicts / GPS contradicts / witness recants / toll cameras |
| 8  | EVIDENCE (Cover-up) | 4            | **3**      | Wiped surfaces / deleted footage / disposed clothing / bleach evidence |

### Motive Narrative Templates

Each of the 10 motive codes has 3–4 unique sentence templates that use
`{subject}` and `{victim}` substitution. The selected template is embedded in
the MOTIVE unknown fact. Example for JEALOUSY:

> *"Mike Rhodes was consumed by jealousy over Jordan Voss's success and
> relationships. The envy festered until it turned violent."*

---

## Step 10 — Interview Generation

**Source:** `CaseEditorPanel.generateInterviews()`

### Who Gets Interviewed

All **living** NPCs. Dead NPCs (e.g., Victim in Murder cases) are skipped.

### Topics per NPC

| Topic         | Generated For            | Gate Attribute | Gate Value  |
|---------------|--------------------------|---------------|-------------|
| Alibi         | All NPCs                 | —             | —           |
| Opinion       | All NPCs (per other NPC) | Empathy       | 5 (opinions about subject, non-suspects only) |
| Whereabouts   | Non-suspects only        | —             | —           |
| Last Contact  | All NPCs                 | —             | —           |
| Observation   | All NPCs                 | Perception 5 (non-suspect) or Intimidation 6 (suspect) | See below |
| Motive        | All NPCs (murder only)   | Intuition 5 (non-suspect) or Intimidation 7 (suspect) | See below |
| Relationship  | All NPCs (with target)   | —             | —           |
| Contact Info  | All NPCs (about others)  | Charisma 4–5  | See below   |

### Attribute Gates Summary

| Topic       | Non-Suspect Gate      | Subject/Suspect Gate     |
|------------|----------------------|--------------------------|
| Opinion     | Empathy ≥ 5          | — (no gate)              |
| Observation | Perception ≥ 5       | Intimidation ≥ 6         |
| Motive      | Intuition ≥ 5        | Intimidation ≥ 7         |
| Contact Info | Charisma ≥ 4 (client/associate) or Charisma ≥ 5 (witness) | — (uncooperative, no gate; always refuses) |

When the player's attribute is **below** the gate value, they receive the
`alternateAnswer` (a generic, less revealing response).

### Truthfulness by Role

| Role                 | Topic          | Truthful Probability |
|---------------------|----------------|---------------------|
| **Client**          | All topics      | **100 %**           |
| **Witness / Other** | All topics      | **100 %**           |
| **Subject / Suspect** | Alibi         | **30 %** (`nextInt(10) < 3`) |
| **Subject / Suspect** | Opinion       | **50 %** (`nextBoolean()`) |
| **Subject / Suspect** | Last Contact  | **40 %** (`nextInt(10) < 4`) |
| **Subject / Suspect** | Observation   | **50 %** (`nextBoolean()`) |
| **Subject / Suspect** | Motive        | **50 %** (`nextBoolean()`) |

### Answer Pool Sizes

| Topic          | Subject Pool | Client Pool | Witness Pool | Associate Pool | Generic Pool |
|---------------|-------------|-------------|-------------|----------------|-------------|
| Alibi          | 5           | 4           | 5           | 4              | —           |
| Opinion (of Subject) | 3     | —           | 9 (character traits) | 6       | 5           |
| Opinion (of Victim)  | —     | —           | 4           | —              | —           |
| Whereabouts    | —           | —           | 4           | 4              | 4           |
| Last Contact   | 4           | 4           | 4           | 4              | —           |
| Observation    | 4 + 4 (revealed) | —      | 7           | 4              | 4           |
| Motive         | 4 + 4 (revealed) | —      | 8           | 5              | 4           |
| Contact Info   | 3 (refusal) | 4 + 3 (gated) | 3 + 3 (gated) | 4 + 3 (gated) | 5 (refusal) |

---

## Overall Stats Summary

### Pool Sizes

| Category                  | Count |
|--------------------------|-------|
| Case Types                | 8     |
| Complexity Levels         | 3     |
| Motive Categories         | 10    |
| **Description Templates** | **8 types × 3 complexities × 12 = 288** |
| **Objective Templates**   | **8 types × 3 complexities × 12 = 288** |
| Base Roles (per type)     | 5–6   |
| Suspect Labels            | 5     |
| Hair Colors               | 6     |
| Beard Styles (male)       | 6     |
| Opportunity Labels        | 5     |
| Access Labels             | 6     |
| Relationship Types        | 11    |
| Discovery Methods         | 6     |
| Interview Topics          | **8** |
| Red Herring Templates     | 8     |
| Story Attributes          | 7     |
| Action Title Pools        | **4 categories × 3 options = 12** |
| Result Description Pools  | **4 categories × 2 sub-pools × 3 = 24** |
| Inline Fact Templates     | **4 categories × 3 high + 3 low = 24** |
| Success Narratives        | **7 attr × 5 action types = 35** |
| Failure Narratives        | **7 attr × 5 action types = 35** |
| Murder Methods            | 5     |
| Murder Weapons            | 5     |
| Unknown Fact Sets (Murder)| 8     |
| NPC Locations             | **18** |
| Phone Number Format       | 555-XXXX (9900 possible suffixes) |

### Key Probabilities

| Event                                    | Probability   |
|-----------------------------------------|---------------|
| Gender: Male                             | 50 %          |
| Death variance: Precise                  | 30 %          |
| Death variance: 15–180 min range         | 60 %          |
| Death variance: Unknown / body missing   | 10 %          |
| Suspect attribute matches perpetrator    | 30 %–70 % per attribute (complexity-weighted) |
| All 5 attributes accidentally match      | Complexity-dependent (forced mismatch) |
| Non-matching access is partial (not full differ) | 40 % |
| Lead classified as red herring           | 25 %          |
| Failure branch creates lead (vs fact)    | 50 %          |
| Subject alibi is truthful                | 30 %          |
| Subject last-contact is truthful         | 40 %          |
| Subject opinion/observation/motive truthful | 50 %       |
| Story fact has prerequisite (complexity ≥ 2)| ~75 % of non-first actions |
| Evidence fact gets forensics delay (complexity 3)| ~100 % of evidence-type |
| Red-herring motive generated (complexity ≥ 2)| 100 %     |
| Secondary motive generated (complexity 3)  | 100 %       |

### Evidence Chain Statistics

| Chain Type                 | Prerequisite Depth | Availability Days |
|---------------------------|-------------------|-------------------|
| Scene → DNA → Toxicology   | 2 links           | 0 → 2 → 4        |
| Item → Digital → Financial  | 2 links           | 0 → 1 → 3        |
| Weapon → Timeline → Alibi  | 2 links (murder)  | 0 → 1 → 0        |
| Location → Cover-up        | 1 link            | 0 → 2             |
| Story facts (complexity ≥ 2)| 1 link per major  | 0 (or 1–3 at complexity 3) |

### Total Generated Content (typical Murder case)

| Content Type         | Complexity 1 | Complexity 2 | Complexity 3 |
|---------------------|-------------|-------------|-------------|
| NPCs                 | 6           | 7–8         | 8–9         |
| Relationships        | ~8–10       | ~10–14      | ~12–16      |
| Leads                | 3           | 4           | 5           |
| Known Facts          | ~5–8        | ~6–10       | ~7–12       |
| Hidden Facts         | ~10–16      | ~18–26      | ~26–36      |
| Unknown Facts        | 8–10        | 8–10        | 9–12        |
| Leaf Actions         | 8           | 16          | 24          |
| Interview Responses  | ~30–50      | ~40–65      | ~50–80      |

---

## Future Improvements

### 1. Expanded Template Pools ✅ (Implemented)

**Previous status:** Most answer categories had 3–5 templates, leading to repetition
across cases.

**What was implemented:**
- **Case description pools** (`CaseGenerator.buildDescription`) expanded from 1 fixed
  string per case type to **3 random variants** per type (8 types × 3 = 24 descriptions)
- **Case objective pools** (`CaseGenerator.buildObjective`) expanded from 1 fixed
  string per type to **3 random variants** per type; both methods now accept a `Random`
  parameter so callers control seeding
- **Motive narrative pools** (`NarrativeTemplates.buildMotiveNarrative`) expanded
  from **4 to 10 entries** per motive code (10 codes × 10 = 100 motive strings)
- **Attribute success/failure narratives** (`buildAttributeSuccessNarrative` /
  `buildAttributeFailureNarrative`) converted from single fixed strings to **pools of
  3 per (attribute × action-type) combination**, using a new `pick(String...)` helper
- **Interview answer pools** across all four NPC builders (client, subject, witness,
  associate) expanded from **3–4 entries to 6–8 entries** per pool, covering alibi,
  opinion, observation, motive, relationship, last-contact, contact-info, and
  personality topics
- **Word pools** in `InterviewTemplateEngine` (HOBBY_WORDS, SOCIAL_WORDS,
  LIKE_DISLIKE_WORDS, LOCATION_CLUE_WORDS) expanded from 5–8 entries to **10–15
  entries** each
- Per-motive and per-case-type specialised variants added throughout

### 2. Dynamic Suspect Attribute Assignment ✅ (Implemented)

**Previous status:** Suspect attributes were purely random 50/50 per dimension.

**What was implemented:**
- Attribute matching is **weighted by complexity**: complexity 1 → 30 % match
  chance (easy), complexity 2 → 50 % (moderate), complexity 3 → 70 % (hard)
- **Temporal alibi** added as a 5th testable elimination dimension with a pool of 8
  plausible alibis — the perpetrator always has a falsifiable alibi, and
  suspects either share it (suspicious) or have a different one (verifiable)
- **Partial access** introduced: when a suspect's access doesn't match the
  perpetrator, 40 % of the time they receive partial access (e.g., "had
  access to the building but not the safe") instead of a completely different
  access type, adding nuanced evaluation
- NPC table column 20 (`Alibi`) stores the temporal alibi for each suspect
- The all-match guarantee now covers 5 dimensions (was 4): if all match, one
  is forced to differ via `random.nextInt(5)`
- Debug/export output includes alibi data for each NPC

### 3. Witness Reliability Variance ✅ (Implemented)

**Previous status:** All witnesses were 100 % truthful.

**What was implemented:**
- **Witness reliability score** (0.5–1.0) added to `InterviewScript` via
  `getReliability()` / `isUnreliable()` accessors and a new four-arg
  constructor `InterviewScript(id, name, role, reliability)`
- **Complexity-driven reliability:** at complexity 1 all witnesses are fully
  reliable; at complexity 2 there is a 40 % chance the key witness has
  reduced reliability (0.7–0.9); at complexity 3 there is a 60 % chance of
  lower reliability (0.5–0.8)
- Each witness response's truthfulness is determined by a per-response dice
  roll against the reliability score (`reliableTruth(reliability)`) — lower
  reliability means more non-truthful responses, creating subtle misinformation
- **Contradictory witness** added at complexity ≥ 3: a fifth interview script
  with role "Contradictory Witness" is generated with low reliability
  (0.5–0.7).  This witness deliberately contradicts the primary witness on
  three key topics: WHEREABOUTS, LAST_CONTACT, and OBSERVATION.  All of the
  contradictory witness's responses are flagged as non-truthful, requiring
  the player to cross-reference testimony and determine which account is
  accurate
- `InterviewTemplateEngine.buildAll()` now accepts an optional `complexity`
  parameter; the existing single-arg overload defaults to complexity 1 for
  backward compatibility
- `CaseGenerator.generate()` passes case complexity through to the interview
  engine automatically

#### Witness reliability by complexity

| Complexity | Key Witness Reliability | Contradictory Witness | Total Witness NPCs |
|------------|------------------------|-----------------------|-------------------|
| 1          | 1.0 (always)           | —                     | 1                 |
| 2          | 1.0 (60 %) or 0.7–0.9  | —                     | 1                 |
| 3          | 1.0 (40 %) or 0.5–0.8  | 0.5–0.7               | 2                 |

### 3b. Forensic Results → POLICE Known Facts ✅ (Implemented)

**Previous status:** Forensic-method leads were generated but not reflected
in the `KnownFact` system.

**What was implemented:**
- When leads are added to a `CaseFile` during generation, any lead whose
  `DiscoveryMethod` is `FORENSICS` also creates a `KnownFact` with
  `FactSource.POLICE` containing the lead's full description text
- This means forensic evidence (fingerprints, DNA analysis, toxicology
  reports, etc.) automatically appears under the player's POLICE known-facts
  category, consistent with the `FactSource.POLICE` documentation
- Applies to all case types with forensic leads (currently Theft and Murder)

### 4. Evidence Chain System ✅ (Implemented)

**Previous status:** Facts existed independently with no dependencies.

**What was implemented:**
- Two new fact columns: **Prerequisite Fact ID** (col 11) and **Availability
  Day** (col 12) enable fact → fact dependencies and forensics timelines
- **`generateFacts()` evidence chains:**
  - Chain 1: scene evidence → DNA analysis (day 2) → toxicology report (day 4)
  - Chain 2: scene item → digital forensics (day 1) → financial records (day 3)
- **`generateUnknownFacts()` evidence chains:**
  - Weapon → timeline (day 1, murder only) → alibi verification
  - Location → cover-up evidence (day 2)
- **Story tree fact chains** at complexity ≥ 2: within each major block, the
  second action's success fact requires the first action's discovery; at
  complexity 3, evidence-type facts also receive a 1–3 day availability delay
- **EVIDENCE_CHAINS** section added to the story tree visualization, showing
  the prerequisite graph with availability days
- Debug/export output shows prerequisite and availability day info on every
  chained fact
- Fact table UI updated with two new columns; `appendChainInfo()` helper
  annotates fact references throughout

### 5. Procedural Motive Complexity ✅ (Implemented)

**Previous status:** One motive per case, chosen at generation time.

**What was implemented:**
- **Layered motives** at complexity 3: a **secondary motive** is generated from
  a logical pairing table (e.g. FINANCIAL_GAIN → CONCEALMENT, REVENGE → PASSION)
  and is chained to the primary motive fact via an evidence-chain prerequisite —
  the investigator must discover the primary motive before the secondary one
  becomes available
- **Red-herring motives** at complexity ≥ 2: a HIDDEN fact with a plausible but
  false motive narrative is generated using a different motive code.  It appears
  convincing early in the investigation but is ultimately contradicted
- **`NarrativeTemplates` additions:**
  - `pickSecondaryMotiveCode(primaryCode)` — selects a logically paired
    secondary motive (10 predefined pairings)
  - `buildSecondaryMotiveNarrative(secondaryCode, primaryCode, subject, victim)`
    — wraps a standard motive narrative with a "deeper motivation" connector
  - `pickRedHerringMotiveCode(trueMotiveCode)` — selects a different motive
    code for the false lead
  - `buildRedHerringMotiveNarrative(herringCode, subject, victim)` — wraps a
    standard narrative with misleading framing text
- **Story tree visualisation:** `[MOTIVE_LAYERS]` section shows all motive
  facts tagged as `[MOTIVE:PRIMARY]`, `[MOTIVE:SECONDARY]`, or
  `[MOTIVE:RED_HERRING]` with chain info
- **Summary output** includes a Motive Complexity section counting
  primary/secondary/red-herring motive facts

#### Motive complexity by level

| Complexity | Primary | Red Herring | Secondary | Total motive facts |
|------------|---------|-------------|-----------|-------------------|
| 1          | 1+traits| —           | —         | 1–3               |
| 2          | 1+traits| 1           | —         | 2–4               |
| 3          | 1+traits| 1           | 1         | 3–5               |

#### Secondary motive pairings

| Primary         | Secondary       |
|-----------------|-----------------|
| FINANCIAL_GAIN  | CONCEALMENT     |
| REVENGE         | PASSION         |
| JEALOUSY        | REVENGE         |
| COERCION        | SELF_DEFENSE    |
| POWER           | FINANCIAL_GAIN  |
| SELF_DEFENSE    | CONCEALMENT     |
| IDEOLOGY        | POWER           |
| CONCEALMENT     | FINANCIAL_GAIN  |
| PASSION         | JEALOUSY        |
| LOYALTY         | COERCION        |

### 6. Location-Based Investigation ✅ (Implemented)

**Previous status:** Location was a single text label ("back office", "car park").

**What was implemented:**
- Each NPC is assigned a **default location** from a pool of 18 recognisable
  urban/suburban places (Café, Bar, Office, Park, Library, etc.)
- Locations are **role-aware**: a Police Contact is at the Police Station,
  a Neighbour is at Their Home, a Friend is at a random social venue
- Each NPC has a **phone number** (format `555-XXXX`) that starts as hidden
- The **client's phone number is always known** from the start
- Other phone numbers must be **discovered** through CONTACT_INFO interview
  questions (gated by Charisma ≥ 4 or ≥ 5)
- Once a phone number is discovered, the player can call the NPC to arrange
  a **meeting at a specific location** for quest progression
- `NpcLocation` model class provides the `LocationCode` enum with 18
  predefined codes and display names
- `PhoneContact` carries `phoneNumber`, `phoneDiscovered`, and
  `defaultLocation` fields
- `InterviewTopic.CONTACT_INFO` is a new interview topic across all 4
  NPC interview builders (client, subject, witness, associate)
- NPC table in the admin tool has 3 new columns: Phone Number (16),
  Phone Discovered (17), Default Location (18)

### 7. Non-Linear Story Progression

**Status:** ✅ Done

**Previous:** Story tree was strictly linear: all sub-branches had to complete
before the next MAJOR_PROGRESS unlocked.

**Implementation:**
- **Parallel investigation threads:** Each PLOT_TWIST phase node is now
  marked as `parallel=true`, meaning its two MAJOR_PROGRESS children can
  be pursued in any order.  `CaseStoryNode.isChildAvailable()`,
  `getNextActiveChild()`, and the new `getAvailableActions()` method all
  respect the flag.  The admin tool displays `[PLOT_TWIST:PARALLEL]` tags.
- **Time pressure:** At complexity ≥ 2, approximately 30 % of leads receive
  an `expirationDay` (3–7 in-game days).  `CaseLead.isExpired(currentDay)`
  lets the runtime check if a lead has become stale.  The admin tool shows
  `[EXPIRES:day N]` on affected leads.
- **Side cases:** At complexity 3, an optional `SIDE_CASE` node (a
  self-contained mini-investigation) is appended to the story root with
  `parallel=true`, so the player can pursue it alongside the main phases.

### 8. Physical Appearance Expansion

**Current:** Only hair color and beard style differentiate suspects visually.

**Improvement:**
- Add **height range** (short, average, tall)
- Add **build** (slim, average, heavy)
- Add **distinguishing marks** (scar, tattoo, glasses)
- Add **clothing description** at time of crime
- Generate **witness descriptions** that partially match multiple suspects,
  requiring corroboration

### 9. Interview System Enhancements

**Current:** Fixed question set per NPC, single attribute gate per response.

**Improvement:**
- **Follow-up questions** that unlock based on previous answers (dialogue trees)
- **Emotional state tracking** — nervousness increases if the player asks
  pointed questions, changing available responses
- **Multi-attribute gates** — some reveals require BOTH Empathy ≥ 5 AND
  Perception ≥ 4
- **Rapport system** — repeated interviews with the same NPC build or
  erode trust, affecting truthfulness

### 10. Red Herring Sophistication

**Current:** Red herrings are randomly tagged (25 % flat chance).

**Improvement:**
- Scale red herring probability with complexity: 15 % at complexity 1,
  25 % at complexity 2, 35 % at complexity 3
- Create **planted evidence** red herrings — false facts that appear in the
  Hidden category and must be disproven
- Add **suspect alibi red herrings** — initially alibi appears solid but
  crumbles under investigation

### 11. Difficulty Scaling

**Current:** Attribute thresholds are uniform 2–5 regardless of complexity.

**Improvement:**
- Scale attribute thresholds with complexity and phase:
  - Complexity 1: thresholds 2–4
  - Complexity 2: thresholds 3–5
  - Complexity 3: thresholds 4–6
- Later phases within a case have higher thresholds than early phases
- Add a **difficulty rating** to the case summary based on generated content

### 12. Case Interconnection

**Current:** Each case is fully standalone.

**Improvement:**
- Generate recurring NPCs across cases (reputation carries over)
- Allow **multi-case arcs** where solving case A reveals a lead for case B
- Persistent suspect records that the player can reference across cases

### 13. Code Architecture Cleanup

**Current:** Case generation logic is split between two very large classes
with significant duplication.

| Class | Lines | Methods | Primary role |
|-------|-------|---------|-------------|
| `CaseGenerator` | ~1 480 | ~29 | Core generation engine (leads, story tree) |
| `CaseEditorPanel` | ~2 770 | ~85 | Admin UI with delegated generation |

**Addressed duplication areas:**

| Area | Status | What was done |
|------|--------|---------------|
| Action-type classification (`isInterview`/`isEvidence`/…) | ✅ Done | Extracted `ActionType` enum; all 7+ copy-paste sites now use `ActionType.classify()` |
| Narrative templates (success/failure/motive) | ✅ Done | Extracted `NarrativeTemplates` class in core; `CaseEditorPanel` delegates to it |
| Name arrays | ✅ Done | Replaced hardcoded arrays with `PersonNameGenerator` — same API as `CaseGenerator` |
| Interview generation | ✅ Done | `CaseEditorPanel.generateInterviews()` now delegates to `InterviewTemplateEngine.buildAll()` for the four core NPC scripts (client, subject, witness, associate); dynamic cross-NPC rows (opinion, contact info with real phone/location, personality from trait col 19) are produced in the panel using engine text-generation helpers (`buildOpinionText`, `buildContactInfoText`, etc.). The 14 duplicated `buildInterview*()` helper methods have been removed from `CaseEditorPanel`. |

**Remaining improvements:**
- Consider splitting `CaseGenerator` into `CaseFileGenerator` + `InterviewScriptBuilder`

---

*Last updated: 2026-04-14*

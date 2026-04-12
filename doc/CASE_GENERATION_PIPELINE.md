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
| All others                 | Random from full pool (16 locations)             |

**Full location pool (16):**
Café, Bar, Office, Public Park, Library, Restaurant, Their Home, Gym,
Warehouse District, Church, Hospital, Diner, Hotel Lobby,
Bus Station, Street Market, Courthouse

---

## Step 3 — Suspect Attribute Generation

**Source:** `CaseEditorPanel.generateNpcs()` lines 1362–1422

Every NPC with a role starting with "Subject" or "Suspect" receives distinguishing
attributes. The **true perpetrator** (Subject) matches all criteria; additional
suspects share opportunity but **at least one** other attribute differs.

### Attribute Pools

| Attribute     | Pool                                                                              | Size |
|---------------|------------------------------------------------------------------------------------|------|
| Hair Color    | black, brown, blonde, red, gray, white                                            | 6    |
| Beard Style   | clean-shaven, stubble, short beard, long beard, goatee, moustache (males only)    | 6    |
| Opportunity   | was near the scene, had keys to the building, was seen in the area, lives close by, visited the location earlier that day | 5 |
| Access        | owns a firearm, had access to the victim's home, has a key to the office, had access to the safe, drives a vehicle matching witness description, works in the same building | 6 |
| Has Motive    | true / false                                                                       | 2    |

### Perpetrator (Subject Role) — 100 % Match

| Attribute    | Value                          |
|-------------|--------------------------------|
| Hair Color  | Random from pool (1/6)         |
| Beard Style | Random from pool (males) or "none" (females) |
| Opportunity | Random from pool (1/5)         |
| Access      | Random from pool (1/6)         |
| Has Motive  | **Always true**                |

### Red-Herring Suspects — Differentiation Rules

Each additional suspect rolls **independently** per attribute:

| Attribute   | Match Perpetrator?    | Probability |
|-------------|----------------------|-------------|
| Hair Color  | `random.nextBoolean()` | **50 %** match / 50 % differ |
| Beard Style | `random.nextBoolean()` | **50 %** match / 50 % differ |
| Access      | `random.nextBoolean()` | **50 %** match / 50 % differ |
| Has Motive  | `random.nextBoolean()` | **50 %** match / 50 % differ |

**Guarantee:** If all four match (probability: 6.25 %), one is randomly forced
to differ (`random.nextInt(4)` picks which one flips → 25 % each).

**All suspects always have opportunity** — they had a plausible reason to be
present. Opportunity alone cannot eliminate a suspect.

### Elimination Logic for Players

The player thins out suspects by discovering facts about:

| Dimension       | Example Clue                                         |
|-----------------|------------------------------------------------------|
| **Physical Looks** | "Witness saw someone with blonde hair leaving"       |
| **Access**       | "The killer had access to the safe"                  |
| **Motive**       | "Only someone with a financial grudge would…"        |
| **Time**         | (Alibi verification via interviews)                  |

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

**Source:** `CaseGenerator.buildDescription()` / `buildObjective()`

- **1 description template per case type** (8 total)
- **1 objective template per case type** (8 total)
- Templates use `{client}`, `{subject}`, `{victim}`, `he/she` substitution
- `CaseGenerator.capitalizeSentences()` ensures sentence-initial capitals

### Example (Murder)

> *"The client, Alex Turner, doesn't believe the official story. the victim,
> Jordan Voss, called it murder from the start, and nobody listened. Mike Rhodes
> was the last person seen with the victim. Follow the evidence."*

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
Story Root [CaseType / Motive / complexity=N]
├── [KNOWN_FACTS] Known Facts & Leads
│   ├── [FACT] ...          (all KNOWN-status facts)
│   ├── [LEAD] ...          (75% genuine leads)
│   └── [LEAD:RED_HERRING]  (25% red herring leads)
├── [UNKNOWN_FACTS] Unknown Facts (To Be Solved)
│   └── [FACT:UNKNOWN] ...  (all UNKNOWN-status facts)
├── [PLOT_TWIST] Initial Investigation          ← always present
│   ├── [MAJOR] ...
│   │   ├── [MINOR] ...
│   │   │   ├── [ACTION] ...
│   │   │   │   └── [RESULT] ... | req:ATTR:N | ok:fact:X | fail:...
│   │   │   └── [ACTION] ...
│   │   └── [MINOR] ...
│   └── [MAJOR] ...
├── [PLOT_TWIST] Plot Twist 1                   ← if complexity ≥ 2
│   └── (same structure)
└── [PLOT_TWIST] Plot Twist 2                   ← if complexity ≥ 3
    └── (same structure)
```

### Node Counts per Complexity

| Level          | Per Phase | Complexity 1 | Complexity 2 | Complexity 3 |
|---------------|-----------|-------------|-------------|-------------|
| PLOT_TWIST     | 1         | 1           | 2           | 3           |
| MAJOR_PROGRESS | 2         | 2           | 4           | 6           |
| MINOR_PROGRESS | 4         | 4           | 8           | 12          |
| ACTION (leaf)  | 8         | **8**       | **16**      | **24**      |
| RESULT         | 8         | 8           | 16          | 24          |

### Action Node — Attribute Requirements

Each ACTION has a random skill check:

- **Attribute:** Random from 7: `PERCEPTION`, `INTELLIGENCE`, `CHARISMA`, `INTIMIDATION`, `EMPATHY`, `MEMORY`, `STEALTH`
- **Threshold:** 2–5 (uniform, `2 + nextInt(4)`)

### Action Outcomes

| Branch  | Creates              | Importance | Probability |
|---------|---------------------|------------|-------------|
| Success | HIDDEN fact (high)   | 3–5        | Based on player attribute ≥ threshold |
| Failure | HIDDEN fact (low) **or** Lead | 0–2 (fact) | 50 % fact / 50 % lead |

### Inline Fact Templates (per action type)

| Action Category     | High-Importance Examples                | Low-Importance Examples              |
|--------------------|----------------------------------------|--------------------------------------|
| Photograph / Scene  | Footprints, forced entry, hidden exit  | No struggle, layout consistency      |
| Collect / Forensics | Trace evidence, fingerprint match      | Common fiber, partial print          |
| Interview           | Witness confirms sighting, threats     | Barely knows suspect, repeats story  |
| Documents / Records | Large payment, shell company           | Routine transactions, clean record   |

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
| Murder Methods            | 5     |
| Murder Weapons            | 5     |
| Unknown Fact Sets (Murder)| 8     |
| NPC Locations             | **16** |
| Phone Number Format       | 555-XXXX (9900 possible suffixes) |

### Key Probabilities

| Event                                    | Probability   |
|-----------------------------------------|---------------|
| Gender: Male                             | 50 %          |
| Death variance: Precise                  | 30 %          |
| Death variance: 15–180 min range         | 60 %          |
| Death variance: Unknown / body missing   | 10 %          |
| Suspect attribute matches perpetrator    | 50 % per attribute |
| All 4 attributes accidentally match      | 6.25 % (forced mismatch) |
| Lead classified as red herring           | 25 %          |
| Failure branch creates lead (vs fact)    | 50 %          |
| Subject alibi is truthful                | 30 %          |
| Subject last-contact is truthful         | 40 %          |
| Subject opinion/observation/motive truthful | 50 %       |

### Total Generated Content (typical Murder case)

| Content Type         | Complexity 1 | Complexity 2 | Complexity 3 |
|---------------------|-------------|-------------|-------------|
| NPCs                 | 6           | 7–8         | 8–9         |
| Relationships        | ~8–10       | ~10–14      | ~12–16      |
| Leads                | 3           | 4           | 5           |
| Known Facts          | ~5–8        | ~6–10       | ~7–12       |
| Hidden Facts         | ~10–16      | ~18–26      | ~26–36      |
| Unknown Facts        | 8           | 8           | 8           |
| Leaf Actions         | 8           | 16          | 24          |
| Interview Responses  | ~30–50      | ~40–65      | ~50–80      |

---

## Future Improvements

### 1. Expanded Template Pools

**Current:** Most answer categories have 3–5 templates, leading to repetition
across cases.

**Improvement:**
- Increase each answer pool to **10–15 templates**
- Add per-motive and per-case-type specialised variants
- Consider a template-composition system (sentence fragments assembled randomly)

### 2. Dynamic Suspect Attribute Assignment

**Current:** Suspect attributes are purely random 50/50 per dimension.

**Improvement:**
- Weight attribute matching by complexity — higher complexity means more attributes
  match, making elimination harder
- Add **temporal alibi** as a full elimination dimension (not just interview text)
- Allow suspects to have **partial access** (e.g., "had access to the building
  but not the safe") for nuanced elimination

### 3. Witness Reliability Variance

**Current:** All witnesses are 100 % truthful.

**Improvement:**
- Introduce a **witness reliability score** (0.5–1.0) affecting truthfulness
- At higher complexity, some witnesses could be unreliable or bribed
- Add CONTRADICTORY witnesses whose accounts conflict, requiring the player
  to determine which is accurate

### 4. Evidence Chain System

**Current:** Facts exist independently with no dependencies.

**Improvement:**
- Create fact → fact dependencies ("discovering DNA evidence unlocks the
  toxicology report")
- Implement **evidence chains** where early clues gate access to later discoveries
- Add a **forensics timeline** — evidence degrades or becomes available over
  in-game days

### 5. Procedural Motive Complexity

**Current:** One motive per case, chosen at generation time.

**Improvement:**
- Allow **layered motives** (primary + secondary) at complexity 3
- Generate motive **red herrings** — false motives that appear plausible
  until contradicted
- Tie motive discovery to specific interview gates and evidence findings

### 6. Location-Based Investigation ✅ (Implemented)

**Previous status:** Location was a single text label ("back office", "car park").

**What was implemented:**
- Each NPC is assigned a **default location** from a pool of 16 recognisable
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

**Current:** Story tree is strictly linear: all sub-branches must complete
before the next MAJOR_PROGRESS unlocks.

**Improvement:**
- Allow **parallel investigation threads** — player chooses which major
  branch to pursue first
- Add **time pressure** — certain leads expire after N in-game days
- Introduce optional **side cases** at complexity 3 that interweave with
  the main case

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

---

*Last updated: 2026-04-12*

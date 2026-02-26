# Case Types

The following case types are currently defined in `CaseType.java`.  
Each type has a **display name** shown in the UI, a short **description** of the work involved, and a **difficulty level** from 1 (easiest) to 10 (hardest).  
The `CaseGenerator` uses the type to produce contextually appropriate case descriptions, objectives, and hidden leads.

| # | Enum Constant | Display Name | Difficulty | Description |
|---|--------------|--------------|:----------:|-------------|
| 1 | `MISSING_PERSON` | Missing Person | 5 | Locate a person who has disappeared and determine what happened to them |
| 2 | `INFIDELITY` | Infidelity | 3 | Gather evidence of a partner's unfaithful behaviour |
| 3 | `THEFT` | Theft | 3 | Identify who stole property and, if possible, recover it |
| 4 | `FRAUD` | Fraud | 7 | Uncover deliberate financial deception or identity misrepresentation |
| 5 | `BLACKMAIL` | Blackmail | 6 | Identify the source of a blackmail threat and neutralise it |
| 6 | `MURDER` | Murder | 9 | Reinvestigate a suspicious death the authorities closed too quickly |
| 7 | `STALKING` | Stalking | 5 | Identify and document a stalker threatening the client or their family |
| 8 | `CORPORATE_ESPIONAGE` | Corporate Espionage | 8 | Uncover an internal information leak damaging a business |

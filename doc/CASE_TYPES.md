# Case Types

The following case types are currently defined in `CaseType.java`.  
Each type has a **display name** shown in the UI, a short **description** of the work involved, and a **difficulty range** (min–max, both on a 1–10 scale) that reflects how simple or complex a specific instance of that case type can be.  
For example, a *Missing Person* case might be a straightforward runaway (**level 1**) or a full kidnapping investigation (**level 5**).  
The `CaseGenerator` uses the type to produce contextually appropriate case descriptions, objectives, and hidden leads.

| # | Enum Constant | Display Name | Min | Max | Description |
|---|--------------|--------------|:---:|:---:|-------------|
| 1 | `MISSING_PERSON` | Missing Person | 1 | 5 | Locate a person who has disappeared and determine what happened to them |
| 2 | `INFIDELITY` | Infidelity | 1 | 4 | Gather evidence of a partner's unfaithful behaviour |
| 3 | `THEFT` | Theft | 1 | 5 | Identify who stole property and, if possible, recover it |
| 4 | `FRAUD` | Fraud | 4 | 9 | Uncover deliberate financial deception or identity misrepresentation |
| 5 | `BLACKMAIL` | Blackmail | 3 | 7 | Identify the source of a blackmail threat and neutralise it |
| 6 | `MURDER` | Murder | 5 | 10 | Reinvestigate a suspicious death the authorities closed too quickly |
| 7 | `STALKING` | Stalking | 2 | 6 | Identify and document a stalker threatening the client or their family |
| 8 | `CORPORATE_ESPIONAGE` | Corporate Espionage | 5 | 10 | Uncover an internal information leak damaging a business |

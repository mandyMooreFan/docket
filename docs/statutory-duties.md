# Statutory duties of a hosted user-to-user service

Research for [mandyMooreFan/docket#18](https://github.com/mandyMooreFan/docket/issues/18). Motivated by [Spec moderation, reporting, and abuse response](https://github.com/mandyMooreFan/docket/issues/16), whose shape was reasoned to from first principles: one reviewer, a capability-withdrawal ladder, reasons given, one appeal, a published log. Several of those *resemble* statutory requirements. This checks whether the resemblance holds.

**Date of research:** 2026-08-22. UK and EU only; US law not covered.

> **I am not a lawyer. This is not legal advice.** Everything below is a non-lawyer's reading of the sources linked inline. Anywhere marked **LAWYER** needs a real one before launch.

> ⚠️ **Retrieval note — resolved on second pass.** Ofcom's live site returns HTTP 403 to all automated fetching, including its PDFs. **A second pass retrieved Ofcom's own documents via the Internet Archive** (`https://web.archive.org/web/<ts>id_/<ofcom-url>` returns Ofcom's unaltered bytes), so the UK findings below now rest on **Ofcom's own primary text** rather than law-firm summaries. `legislation.gov.uk` was directly reachable throughout. Documents read in full: Children's Access Assessments Guidance (24 Apr 2025), Guidance on Highly Effective Age Assurance for Part 3 Services (24 Apr 2025), Record-Keeping and Review Guidance, Risk Assessment Guidance and Risk Profiles, and four Ofcom web pages.

---

## 0a. ⚠️ Corrections to the first pass

**This document was written before Ofcom's own text could be read. Four things were wrong or missing.**

### C1 — Docket is NOT late. **(a) ESTABLISHED**

The first pass said the 2025 deadlines "passed" and that a service launching now "is late to it." **That is wrong for a service that has not launched.**

[Schedule 3 para 3](https://www.legislation.gov.uk/ukpga/2023/50/schedule/3) gives a **new** Part 3 service **three months from its first day of operation** for *both* assessments. Ofcom confirms twice: Risk Assessment Guidance ¶2.19 — *"If you start a new service … you must complete your risk assessment within three months of doing so."* CAA Guidance ¶2.23 — *"If you started operating a new Part 3 service after 16 January 2025, you must complete the first children's access assessment … within three months of the first day of operation … you will have to complete the illegal content risk assessment and the children's access assessment concurrently."*

**The dated deadlines applied to services already operating. Docket's clock starts at launch.** This is a launch precondition on a three-month clock, not an overdue debt.

### C2 — The age escape hatch is wider than described, but not in a way that helps. **(a)**

The first pass implied highly effective age assurance is the only way out. It is not: HEAA settles **Stage 1** only, and a service can still conclude "not likely to be accessed by children" at **Stage 2** on evidence. Ofcom's own case studies do exactly that for a retirement forum and a 40+ career-change community.

**But Docket's specific facts run hard against a Stage 2 escape**, in Ofcom's own words:

- **CAA Guidance Table 7** lists as content appealing to children: *"Content about future careers and finance"* and *"Content providing advice on further education and careers."*
- **¶4.31**: *"Some functionalities are appealing to children and used by children, for example, **the ability to make a user profile, making connections with other users, and direct messaging**."*
- **¶4.19**: a minimum age does not discharge the test — *"if your service is likely to attract a significant number of children between 15-17, this would be sufficient."*
- **¶1.4**: *"We anticipate that **most Part 3 services that are not using highly effective age assurance are likely to be accessed by children**."*
- The closest Ofcom case study is a **dating service** — 18+ in terms, self-declared age, publicly not targeted at children — and Ofcom walks it to *"the child user condition **is** met"*, citing profiles, connections and direct messaging.

**The sharper framing: the children's-regime exposure is driven by what Docket *is*, not by where it sets its age floor. "Just raise it to 18" does not resolve it. (a) on the sources; (b) as applied.**

Two asymmetries worth knowing: concluding **"yes, children"** needs no detailed evidence record (¶4.5); concluding **"no children"** must be evidenced and recorded (¶2.30, ¶4.2). And **not doing the assessment at all deems the service likely to be accessed by children** ([s.37(4)–(5)](https://www.legislation.gov.uk/ukpga/2023/50/section/37)).

### C3 — ⚠️ A duty this document missed entirely, in force since 29 June 2026. **(a)**

The Crime and Policing Act 2026 inserted **[OSA s.20A](https://www.legislation.gov.uk/ukpga/2023/50/section/20A)** and **[s.10(3A)–(3B)](https://www.legislation.gov.uk/ukpga/2023/50/section/10)**, commenced by S.I. 2026/689:

- **s.20A(1)** — a duty to operate systems and processes allowing users and affected persons to *"easily make an **intimate image content report**"*. This is a **distinct reporting route**, not the generic illegal-content one.
- **s.10(3A)** — a duty to take down reported intimate image content **and any substantially similar content** *"as soon as reasonably practicable, and **no later than 48 hours**, after the provider receives the report."*

**This binds all regulated user-to-user services regardless of size, and it lands squarely on Docket's image-bearing private messages.** A hard 48-hour clock is a serious operational constraint on the single-moderator model [#16](https://github.com/mandyMooreFan/docket/issues/16) chose. **Nothing in #16 covers it.**

### C4 — The "named accountable individual" is a Code measure, not a statutory duty. **(b)**

It does not appear in [s.23](https://www.legislation.gov.uk/ukpga/2023/50/section/23) or elsewhere in Part 3's duties. It is a **recommendation**: Record-Keeping Guidance ¶3.6 says the risk assessment record *should* name the person responsible and who approved it, and Ofcom's small-services page lists *"a specific individual responsible for compliance, who we can contact if we need to."*

Because Codes are recommendations, [s.23(4)](https://www.legislation.gov.uk/ukpga/2023/50/section/23) permits **alternative measures** provided you record how they amount to compliance — in practice adopting the measure is cheaper than justifying a departure.

⚠️ **Unresolved and contradictory across passes.** One verification pass reported reading the Illegal Content Codes and quoted a governance measure (cited as **ICU A2**, applicability *"All services"*, with a children's twin **PCU A2** scoped to services likely to be accessed by children). A second pass **could not retrieve the Codes PDF at all** and warned against treating any measure code as established. **Treat the specific measure reference and its applicability line as (c) UNRESOLVED until someone reads the Codes text directly.** The general shape — one named person, recorded, cheap — is not in doubt; Ofcom explicitly contemplates a one-person service satisfying it, and its guidance says *"For small services without formal boards or oversight teams, this can simply mean reporting to a senior manager with responsibility for online safety."*

### C5 — Two findings that *help*

- **Proportionality relief is real and in Ofcom's own words, upgrading §5's claim from (b) to (a).** Helping small services: if risks are assessed low with good reason, *"they will only be expected to have basic but important measures to remove illegal content when they become aware of it"* — findable terms, a complaints tool, the ability to review and take down quickly, and a named responsible individual. Plus: *"**We are not setting out to penalise small, low risk services trying to comply in good faith.**"* **This vindicates #16's reactive-by-default posture from the regulator's own mouth.**
- **[s.12(5)](https://www.legislation.gov.uk/ukpga/2023/50/section/12) is a cheap escape from mandatory HEAA even inside the children's regime.** s.12(4)/(6) would otherwise require *highly effective* age assurance where a provider identifies primary priority content on the service — **but not where "(a) a term of service indicates (in whatever words) that the presence of that kind of primary priority content … is prohibited on the service, and (b) that policy applies in relation to all users."** A blanket prohibition in the terms, applied to everyone, disapplies the trigger. **LAWYER**, but materially useful.

### C6 — Landscape: there is no industry norm to point at **(a)**

| Service | Stated minimum | Age assurance |
|---|---|---|
| LinkedIn | **16**, global | None — self-declared |
| Indeed | **18 in the UK** since 1 Feb 2024 (16 elsewhere) | None — reactive removal only |
| Welcome to the Jungle / Otta | 18 | None documented |
| Totaljobs / Milkround | "not directed towards" 18; **hard floor 14** | None documented |
| Glassdoor | 18 | None documented |
| Adzuna | 13 | Self-declared |
| Reed.co.uk, CV-Library | **none stated at all** | n/a |

**The spread is 13 → 14 → 16 → 18 → nothing, and not one documents any age-assurance mechanism.** LinkedIn — large, well-resourced, UK-regulated, structurally the closest analogue — sits at **16 with nothing but self-declaration**. No comparator publishes a children's access assessment outcome, but **there is no duty to publish one**, so that silence is uninformative. **(c)**

---

## 0. Confidence key

| Tag | Meaning |
|---|---|
| **(a) ESTABLISHED** | Settled by the text of the statute or regulation as quoted. |
| **(b) PROBABLE** | Strongly supported, but resting on a secondary source or on applying a general rule to Docket's particular facts. |
| **(c) UNRESOLVED** | Genuinely open — the term is undefined, or the answer depends on facts not yet decided. |

---

## 1. Headline: two regimes that behave oppositely at Docket's size

**The EU regime is generous to small providers and may not apply to Docket at all. The UK regime is not generous, applies regardless of size, and is already fully in force.**

The instinct that a tiny free project is beneath the law's notice is **half right, and the wrong half is the one that bites.**

1. **The [UK Online Safety Act](https://www.legislation.gov.uk/ukpga/2023/50/contents) illegal-content duties apply to all regulated user-to-user services "no matter their size or reach."** There is no hobby exemption. Its compliance deadlines passed in 2025 and Ofcom opened an enforcement programme on 3 March 2025. **(b)**
2. **The [EU DSA](https://eur-lex.europa.eu/legal-content/EN/TXT/PDF/?uri=CELEX%3A32022R2065) might not apply at all**, because it turns on *targeting*, not accessibility — see §4.1. And where it does, [Article 19](https://www.eu-digital-services-act.com/Digital_Services_Act_Article_19.html) exempts micro and small enterprises from most platform obligations. **(a)**

**The single most consequential finding is §3.2: [#7](https://github.com/mandyMooreFan/docket/issues/7)'s decision to admit 16-year-olds pulls Docket into the UK children's regime, which is materially larger than the one #16 designed for.**

---

## 2. Does Docket fall in scope at all?

**Yes for the UK, almost certainly. (b)** The OSA reaches any service with "links to the UK", read broadly to include a significant number of UK users or targeting a UK audience. Docket is a public user-to-user service with UGC. Nothing about being free, open source, or single-instance changes this.

**For the EU, it depends on a decision not yet made — see §4.1.**

---

## 3. United Kingdom — Online Safety Act 2023

### 3.1 Illegal content duties apply regardless of size **(b)**

Every regulated user-to-user service must:

- **Complete an illegal content risk assessment** across the Act's priority offence categories, rating each. Deadline was **16 March 2025**.
- **Complete a children's access assessment.** Deadline was **16 April 2025**.
- **Take proportionate mitigation measures** matched to the assessed risk.
- **Let users report illegal content, and complain about reports and takedowns.**
- **Keep records** of assessments and decisions.
- **Name an individual accountable** for content safety.
- **Say the relevant things in terms of service.**

~~**Both deadlines are in the past.** A service launching now is not "preparing for" this regime; it is late to it.~~ **CORRECTED — see C1.** Those deadlines bound services already operating. A new service gets **three months from its first day of operation** for both assessments, concurrently. **(a)**

**One genuine relief: proactive monitoring is not required of small low-risk services** — responsive action on reports is the standard. **This vindicates #16's reactive-by-default decision. (b)**

**Fees do not apply below a £250m worldwide revenue threshold. (b)**

### 3.2 ⚠️ The finding that changes the spec: 16+ means children

**A "child" under the OSA is a person under 18. (a)**

[#7](https://github.com/mandyMooreFan/docket/issues/7) decided **16+, self-declared, unverified**, reasoning that this "keeps the product clear of the children's-data regimes (COPPA, GDPR consent) that would otherwise reshape the design."

**For the OSA, that reasoning does not hold. 16- and 17-year-olds are children, and #7 admits them deliberately.**

Worse, the escape hatch is narrow: a provider **may only conclude that children cannot access the service if it has "highly effective age assurance" in place**, plus effective access controls. **(b)** Self-declaration is not highly effective age assurance. **(b)**

The test has two stages: whether it is *possible* for children to access, and then a "child user condition" — whether there is a significant number of child users, or the service is *of a kind likely to attract* a significant number of children. "Significant number" is **undefined**, and turns on the nature and context of the service. **(c)**

**Consequence:** Docket's children's access assessment will very likely conclude *likely to be accessed by children*, which triggers the **children's risk assessment and the children's safety duties** — a materially larger regime than #16 designed for.

**This is a decision to take back to the map, not a research finding to file. LAWYER.** The realistic options:

- **Raise the floor to 18+.** Strengthens the "not of a kind likely to attract children" argument considerably — a professional network for working adults is a plausible non-child service. It does **not** by itself discharge the assessment, because without highly effective age assurance you cannot assert children *cannot* access. **(b)**
- **Keep 16+ and accept the children's duties.** Honest, and defensible for a product genuinely serving school-leavers and apprentices, but it is a large scope addition.
- **Adopt highly effective age assurance.** Contradicts #7's entire posture — no ID upload, no corporation in the login path — and costs money the map does not have.

---

## 4. European Union — Digital Services Act

### 4.1 The DSA may not apply, and [#17](https://github.com/mandyMooreFan/docket/issues/17) decides it

The DSA applies to services offered to recipients in the Union, which requires a **"substantial connection to the Union"**: an EU establishment, or a significant number of EU recipients relative to population, or **targeting of activities** towards Member States. **(a)**

**Recital 8 is explicit that mere technical accessibility of a website from the Union is not, on that ground alone, a substantial connection. (a)**

Targeting is judged on factors like: use of a Member State language or currency, a relevant national top-level domain, local advertising, or customer service in a Member State language. **(a)**

**Docket is English-language, on `.social`, with no advertising and no currency at all.** [Decide the cold-start plan](https://github.com/mandyMooreFan/docket/issues/17) chose to launch into **one dense community you can personally reach** — so **whether the DSA applies is, in practice, a consequence of which community that is.** A UK-targeted launch has a real argument that it does not. **(b) — LAWYER before relying on it.**

The rest of §4 applies **only if** the DSA is in scope.

### 4.2 What the micro/small exemption actually buys **(a)**

[Article 19](https://www.eu-digital-services-act.com/Digital_Services_Act_Article_19.html) disapplies **Section 3** (the online-platform obligations, Arts 20–28) for micro and small enterprises — **except Article 24(3)**. [Article 15(2)](https://www.eu-digital-services-act.com/Digital_Services_Act_Article_15.html) separately exempts them from transparency reporting.

**Exempt:** internal complaint-handling (Art 20), out-of-court dispute settlement (Art 21), trusted flaggers (Art 22), measures against misuse (Art 23), transparency reports (Arts 15, 24(1)-(2)), dark-pattern rules (Art 25), advertising rules (Art 26), recommender transparency (Art 27), protection of minors (Art 28).

**Not exempt — these still bind:**

| | Obligation |
|---|---|
| **Art 11** | Point of contact for authorities |
| **Art 12** | Point of contact for recipients |
| **Art 13** | **Legal representative in the Union** |
| **Art 14** | Terms and conditions |
| **Art 16** | Notice and action mechanisms |
| **Art 17** | Statement of reasons |
| **Art 18** | Notification of suspected criminal offences |
| **Art 24(3)** | Monthly-active-recipient data, on request |

### 4.3 ⚠️ Article 13: an EU legal representative, with no small exemption **(a)**

> "Providers of intermediary services which do not have an establishment in the Union but which offer services in the Union shall designate, in writing, a legal or natural person to act as their legal representative."

They must have "necessary powers and sufficient resources", their details must be notified to the Digital Services Coordinator and made publicly available. **No micro or small exemption is provided.**

**This is a recurring, real cost against a hard cost constraint, and it is the strongest practical argument for not targeting the EU in v1.**

### 4.4 [Article 16](https://www.eu-digital-services-act.com/Digital_Services_Act_Article_16.html) — notice and action **(a)**

- **"Any individual or entity"** may submit a notice. Membership cannot be a precondition. **#16 already reached this conclusion independently.**
- A notice must carry a substantiated explanation, the exact URL, the submitter's name and email (waived for child-safety offences), and a good-faith statement.
- The provider must send **confirmation of receipt**, and later **notification of its decision including information on redress**.
- Processing must be "timely, diligent, non-arbitrary and objective"; both notifications "without undue delay".

### 4.5 [Article 17](https://www.eu-digital-services-act.com/Digital_Services_Act_Article_17.html) — statement of reasons **(a)**

Owed to the affected recipient **at the latest from the date the restriction is imposed**. It must state:

1. whether the decision is removal, disabling of access, **demotion**, or restriction of visibility (and any effect on monetisation or the account);
2. the facts and circumstances relied on, including **whether it followed a notice or an own-initiative investigation**;
3. **whether automated means were used**;
4. the **legal ground** and why the content is illegal, or the **contractual ground** and why it is incompatible;
5. **clear, user-friendly information on redress**.

**Docket's advantage:** #16 refused demotion and visibility limiting outright, so category 1 collapses to a much simpler statement.

### 4.6 [Article 18](https://www.eu-digital-services-act.com/Digital_Services_Act_Article_18.html) — criminal offence notification **(a)**

A hosting provider that becomes aware of information giving rise to suspicion that **a criminal offence involving a threat to the life or safety of a person** has taken place, is taking place, or is likely to, must **promptly inform law enforcement or judicial authorities**. No small exemption stated. **Nothing in #16 covers this.**

### 4.7 [Article 14](https://www.eu-digital-services-act.com/Digital_Services_Act_Article_14.html) — terms and conditions **(a)**

Terms must set out **restrictions imposed on use of the service, including the policies, procedures, measures and tools used for content moderation**, in "clear, plain, intelligible, user-friendly and unambiguous language", publicly available in an **easily accessible and machine-readable format**.

**Where a service is primarily directed at minors or predominantly used by them, the conditions and restrictions must be explained in a way minors can understand.** This interacts directly with §3.2.

---

## 5. What #16 already got right

- **Reactive by default** — proactive monitoring is not required of small low-risk services. **(b)**
- **Reports accepted from non-members** — Art 16 requires exactly this. **(a)**
- **Reasons given for every action** — the substance of Art 17. **(a)**
- **Refusing demotion and shadowbanning** — simplifies the Art 17 statement, and removes a whole disclosure category. **(a)**
- **An appeal** — DSA Art 20 is exempt for micro/small, so this **exceeds** requirement; the OSA separately expects complaints about takedowns. **(a)/(b)**
- **A published transparency log** — Arts 15 and 24(1)-(2) are exempt, so this is **voluntary**. **(a)**
- **The member conduct policy #16 identified as a gap** — Art 14 makes it mandatory rather than merely good practice. **(a)**

---

## 6. Gaps — what must change

| # | Gap | Source | Where it lands |
|---|---|---|---|
| **1** | **16+ admits children under the OSA**, and self-declaration is not highly effective age assurance | §3.2 | **Amends [#7](https://github.com/mandyMooreFan/docket/issues/7).** Needs a decision on the map. |
| **2** | **No illegal content risk assessment**, no record-keeping, no named accountable person | §3.1 | A precondition to lawful operation, not a feature. Deadlines already passed. |
| **3** | **No acknowledgement of receipt** for a report | Art 16 | [#13](https://github.com/mandyMooreFan/docket/issues/13) — a required step in the reporting flow |
| **4** | **Statement of reasons lacks required fields** — notice vs own-initiative, automated means used, legal vs contractual ground | Art 17 | #13 |
| **5** | **No duty to notify authorities of life-or-safety offences** | Art 18 | #13, and a moderator procedure |
| **6** | **No terms of service** describing moderation tools and procedures, machine-readable | Art 14 | #13 — larger than the short conduct policy #16 envisaged |
| **7** | **No published points of contact** for authorities and for recipients | Arts 11, 12 | #13 |
| **8** | **EU legal representative** if the DSA applies | Art 13 | A cost decision, tied to #17's cohort |

---

## 7. What this does *not* require

Worth stating, because compliance anxiety tends to over-build:

- **No proactive scanning** of posts or messages beyond what #16 already chose. **(b)**
- **No out-of-court dispute settlement body**, no trusted-flagger programme, no recommender transparency, no periodic transparency report — all exempt for micro/small under DSA Art 19. **(a)**
- **No fees to Ofcom** below £250m revenue. **(b)**
- **Nothing that contradicts the map's refusals.** No mandated advertising disclosure (there are no ads), no recommender rules (there is no recommender), no dark-pattern rules to satisfy (Art 25 is exempt anyway, and the map refused the patterns regardless).

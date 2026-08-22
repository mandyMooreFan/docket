# Data-subject rights against Docket's model

Research supporting [Decide what a Member may export and take with them](https://github.com/mandyMooreFan/docket/issues/19). Companion to [`statutory-duties.md`](./statutory-duties.md), which covers the OSA and DSA; this covers UK/EU GDPR.

**Date of research:** 2026-08-22.

> **I am not a lawyer. This is not legal advice.** A non-lawyer's reading of the sources linked inline. Anything marked **LAWYER** needs a real one.

Sources prioritised: ICO guidance, Article 29 Working Party [WP242 rev.01](https://ec.europa.eu/newsroom/article29/items/611233/en) (expressly endorsed by the EDPB, so still operative on portability), [EDPB Guidelines 01/2022 on the right of access](https://www.edpb.europa.eu/system/files/2023-04/edpb_guidelines_202201_data_subject_rights_access_v2_en.pdf), and statutory text.

**Confidence key:** **(a) ESTABLISHED** — settled by the text quoted. **(b) PROBABLE** — supported but inferential. **(c) UNRESOLVED** — genuinely open.

---

## 1. Headline: the law is more permissive about shared threads than instinct suggests

The intuition that a two-person Thread cannot be exported because it contains someone else's words **is wrong, and the guidance says so directly.**

WP242 rev.01 p.9, reproduced verbatim by the EDPB in Guidelines 01/2022 (fn. 65):

> "controllers should not take an overly restrictive interpretation of the sentence 'personal data concerning the data subject'. As an example, telephone, **interpersonal messaging** or VoIP records may include (in the subscriber's account history) details of third parties involved in incoming and outgoing calls. Although records will therefore contain personal data concerning multiple people, **subscribers should be able to have these records provided to them** in response to data portability requests, because the records are (also) concerning the data subject."

**This is the single most on-point sentence available for a Docket Thread. (a)**

Contact directories get the same treatment (p.11): *"controllers should transmit the entire directory of incoming and outgoing e-mails to that data subject"* — read across to a mutual Connection list, that is a close but not identical analogy. **(b)**

**Art. 20(4)'s "rights and freedoms of others" proviso targets the receiving controller's onward use, not the export.** WP242 p.11: it is *"intended to avoid the retrieval and transmission of data containing the personal data of other (non-consenting) data subjects **to a new data controller** in cases where these data are **likely to be processed in a way that would adversely affect** the rights and freedoms of the other data subjects."* The condition attaches to **purpose**: the receiving controller *"may not use the transmitted third party data for his own purposes … should not be used to enrich the profile of the third party data subject and rebuild his social environment."* A footnote is directly on point: *"A social networking service should not enrich the profile of its members by using personal data transmitted by [a portability request]."* **(a)**

**A blanket refusal is not available.** WP242 p.12, quoting Recital 63: even where others' rights are engaged, *"the result of those considerations should not be a refusal to provide all information to the data subject."* And the burden is on the controller: EDPB Guidelines 01/2022 ¶172 — *"The **general concern** that rights and freedoms of others might be affected … **is not enough**."* ¶173 sets a three-step method: identify actual adverse effect; **try to reconcile (redact rather than refuse)**; only then decide which right prevails. **(a)**

**The one source cutting the other way. (c)** ICO says that where data was provided by **multiple data subjects** (its example: a joint bank account) *"you need to be satisfied that all parties agree."* That sits in tension with WP242's messaging paragraph. The plausible distinction is between a record *co-provided* by several people and a record to which each party contributed their own separable material — **but no source draws that distinction expressly.** This is the genuine discretion point.

**Selective export is "leading practice", not obligation.** WP242 p.12 recommends tools letting a subject *"select the relevant data they wish to receive and … exclude, where relevant, data of other individuals"* — but *"it is up to data controllers to decide on the leading practice to follow."* **(a)**

---

## 2. Article 20 portability — scope

- **Gated by lawful basis. (a)** Applies only where processing rests on **consent or contract** and is automated ([Art. 20(1)](https://gdpr-info.eu/art-20-gdpr/), [Recital 68](https://gdpr-info.eu/recitals/no-68/)). Anything genuinely resting on **legitimate interests** — plausibly Reports, moderation records, abuse logs — is outside Art. 20 entirely, though still inside Art. 15. Which of Docket's processing sits on which basis is **(c)**, and it is Docket's own determination to make and document.
- **"Provided by" is broader than it sounds. (a)** Covers data knowingly supplied (the whole Profile) **and** data from **observation** of activity.
- **Derived and inferred data is excluded. (a)** WP242 p.10: data *"created by the data controller … by a personalisation or recommendation process, by user categorisation or profiling … are **not covered**."* **So Docket's Capabilities and effective visibility — derived, never stored per [ADR-0002](./adr/0002-derived-not-stored.md) — fall outside Art. 20 as a category.**
- **An Application Outcome** is recorded by the poster, not provided by the applicant, so on a plain reading it is outside Art. 20 for the applicant — but inside Art. 15. **(b)**
- **A Recommendation** was provided by its author, so it is portable **by the author** and not by its subject, though it is the subject's personal data and reachable by them under Art. 15. **(b)** — no guidance found on published third-party testimonials.
- **Format. (a)** "Structured, commonly used and machine-readable"; no format mandated. ICO names CSV, XML, JSON. Recital 68: no obligation to adopt technically compatible systems. ICO's practical caveat: *"If the individual cannot make use of the format … the data will be of no use to them."*
- **No obligation to retain data longer** just to serve a future request (WP242 p.6), and Art. 20(3): porting neither triggers nor delays erasure. **(a)**

---

## 3. Article 17 erasure — the genuinely open question

- **Not absolute; grounds exhaustive; applies only to data held at the time of the request. (a)**
- **[Art. 17(3)](https://gdpr-info.eu/art-17-gdpr/) permits retention** for freedom of expression and information, legal obligation, public-interest archiving, and establishment/exercise/defence of legal claims. **(a)**
- **Backups: ICO requires data be put "beyond use"** until the backup is replaced on schedule, and that members be told clearly what happens *"including in respect of backup systems."* **(a)**
- **Downstream notification** (Arts. 17(2), 19): where data was made public, take reasonable steps to tell other controllers to erase links and copies. **Relevant because Profiles are publicly indexable.** **(a)**

### May B's messages stay in the Thread after A leaves?

**No source answers this squarely. (c) — the largest gap in this research.**

What can be said: B's message text, insofar as it relates to A, is normally **also A's personal data**, so it is in principle within reach of A's erasure request. The real question is whether an Art. 17(1) ground is triggered at all — if the retention purpose is B's continued access to B's own correspondence, the argument is that the data is **still necessary**, so 17(1)(a) never fires. Art. 17(3)(a) freedom of expression is the other candidate. **Both are arguments the operator would have to make and defend, not settled positions.**

### May a Recommendation the leaver *wrote* stay up?

Better placed, same uncertainty. The **evaluative content is primarily the subject's personal data**, not the author's; the author's personal data in it is chiefly the **attribution**. Art. 17(3)(a) is a stronger fit for published, approved speech. **De-identifying the attribution while keeping the text** is exactly the "reconcile rather than refuse" move at EDPB ¶173. Whether that satisfies Art. 17, and whether an unattributed Recommendation is still meaningful, is **(c)**.

---

## 4. Article 15 access is the bigger right, and it has no basis gate

| | **Art. 15 access** | **Art. 20 portability** |
|---|---|---|
| Lawful-basis gate | **None** | Consent or contract only |
| Derived / inferred data | **Included** | Excluded |
| Third-party-authored data about you | **Included** | Excluded |
| Format | A copy; electronic if requested | Structured + machine-readable |
| Supplementary information | **Yes** — purposes, recipients, retention, source, rights | No |

**(a)** — and the bridge matters: ICO says that if it is clear an individual seeks derived data *"as part of a wider portability request, you **must** include this data in your response."* **In practice a member clicking "download my data" is usually making both requests at once.**

**UK-specific:** [DUAA s.78](https://www.legislation.gov.uk/ukpga/2025/18/section/78/enacted) inserts Art. 15(1A) — entitlement limited to what the controller can provide on a **"reasonable and proportionate search"**, treated as in force from 1 Jan 2024. **No EU equivalent.** [DUAA s.76](https://www.legislation.gov.uk/ukpga/2025/18/section/76/enacted) inserts Art. 12A with a "stop the clock" for clarification — **Art. 15 only, not Art. 20 or Art. 17**. **(a)**

---

## 5. Size, cost and open-sourcing change nothing

**Bluntly, no exemption exists. (a)**

- UK GDPR *"covers companies of all sizes from sole traders … through to large global corporations."* There is **no small-operator, non-profit, volunteer or open-source exemption** from Arts. 15, 17 or 20.
- Being **free is expressly irrelevant** — [Art. 3(2)(a)](https://gdpr-info.eu/art-3-gdpr/) applies *"irrespective of whether a payment of the data subject is required."*
- The only size concession in the regime, [Art. 30(5)](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/accountability-and-governance/documentation/who-needs-to-document-their-processing-activities/) record-keeping under 250 employees, requires processing to be *occasional* — a continuously running network's is not.
- **Open-sourcing the code changes nothing about controllership.** The operator of the hosted instance is the controller for that instance. **(a)** as principle, **(b)** as applied.

Unresolved and size-adjacent: the **UK data protection fee** (a narrow not-for-profit exemption exists) **(c)**; a **DPO** under Art. 37(1)(b) **(c)**; an **[Art. 27](https://gdpr-info.eu/art-27-gdpr/) EU representative** if EU GDPR applies — **(b)** that one would be required if EU members are targeted. Note this is a *second, separate* representative from the DSA Art. 13 one in `statutory-duties.md` §4.3.

---

## 6. Is a manual, on-request process compliant?

**Yes. A self-service export tool is not legally required. (a)**

ICO: you may satisfy Art. 20 *"either"* by transmitting the data directly *"or"* by *"providing access to an automated tool that allows the individual to extract the requested data themselves."* WP242 frames download tools and APIs as *"good practice"*, not obligation.

But the manual route carries hard conditions, all **(a)**:

- **One month**, extendable by two for complexity or volume, with notice and reasons inside the first month.
- **Free of charge**, except for manifestly unfounded or excessive requests.
- **"Without hindrance"** — no *"legal, technical or financial obstacles which slow down or prevent the transmission"*, and **the controller bears the burden of justifying any obstacle**.
- You must **recognise a request in any form** — verbal or written, to any part of the organisation, with no magic words and no article references.
- Transmission must be **secure**.

**Inference, flagged as such:** a manual process a one-person operation cannot staff reliably risks breaching the one-month deadline and, if habitually slow, edging toward "hindrance". No source says manual is *per se* hindrance. **(b)**

---

## 7. ⚠️ A live UK duty neither #16 nor #19 anticipated

**[DUAA s.103](https://www.legislation.gov.uk/ukpga/2025/18/section/103/enacted) inserts s.164A into the DPA 2018:** a controller **must facilitate** data protection complaints *"by taking steps such as providing a complaint form which can be completed electronically and by other means"*, **must acknowledge receipt within 30 days**, and must respond and inform the complainant of the outcome without undue delay.

**This is a self-service-shaped obligation with no exemption for small or non-commercial operators**, and it is distinct from #16's moderation reporting and from the OSA's reporting duties. Reported commencement **19 June 2026** — already passed. **(a)** that the duty exists; **(b)** on commencement, not verified against a commencement SI.

---

## 8. What could not be verified

1. **No authority squarely on erasing a two-party message thread.** WP242's messaging sentence is about *export*, not erasure.
2. **No authority on published third-party testimonials** under either Art. 17 or Art. 20.
3. **The ICO / WP242 tension** on jointly-provided data is unreconciled in the sources.
4. Both ICO pages quoted carry a live banner: *"Due to changes made by the Data (Use and Access) Act, this guidance is under review and may be subject to change."*
5. **Commencement dates** for DUAA ss. 76 and 103 come from the ICO's framing and legal commentary, not commencement SIs.
6. **Docket's own lawful bases** are undetermined, and §2's gate turns entirely on them.

# The ICO Children's code applied to Docket

Research for [mandyMooreFan/docket#22](https://github.com/mandyMooreFan/docket/issues/22). Companion to [`statutory-duties.md`](./statutory-duties.md) (OSA and DSA) and [`data-rights.md`](./data-rights.md) (UK/EU GDPR subject rights). This covers the third regime: the ICO's [Age appropriate design: a code of practice for online services](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/childrens-information/childrens-code-guidance-and-resources/age-appropriate-design-a-code-of-practice-for-online-services/), the "Children's code".

Triggered by [#20](https://github.com/mandyMooreFan/docket/issues/20), which concluded Docket is likely to be accessed by children under the Online Safety Act and warned — on Ofcom's own instruction — that data protection law asks a **different question** and needs its own answer. **§2.1 refines that warning: the two assessments are legally separate, but Ofcom and the ICO both say the factors are substantively the same, and evidence may be reused.**

**Date of research:** 2026-08-22. UK only.

> **I am not a lawyer. This is not legal advice.** Everything below is a non-lawyer's reading of the sources linked inline. Anywhere marked **LAWYER** needs a real one before launch.

> **Retrieval note.** `ico.org.uk` returns HTTP 403 to some automated fetchers but serves normally to a browser user-agent; `ofcom.org.uk` is Cloudflare-protected and was read through a headful browser. **Every quotation below is from the ICO's or Ofcom's own published text.** Read in full: all fifteen code standards, `About this code`, `Services covered by this code`, `Enforcement of this code`, Annexes A, B, C and D, the ICO's 2023 [`'Likely to be accessed' by children` guidance](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/childrens-information/childrens-code-guidance-and-resources/likely-to-be-accessed-by-children/) including all eleven case studies, Ofcom's Children's Access Assessments Guidance, and Ofcom's Age Assurance and Children's Access Statement. No secondary source is relied on for any tagged claim.

> ⚠️ **The code is not the whole regime, and this is the trap.** The 2020 code text is no longer the current authority on its own scope test, on age assurance, or on what "high privacy" means for a profile. Three later ICO instruments move all three, and one of them (§4.2) is the most consequential finding in this document. **Anyone checking this work against the code alone will reach a more comfortable answer than the correct one.**

---

## 0. Confidence key

| Tag | Meaning |
|---|---|
| **(a) ESTABLISHED** | Settled by the text of the code or the statute as quoted. |
| **(b) PROBABLE** | Strongly supported, but resting on applying a general rule to Docket's particular facts. |
| **(c) UNRESOLVED** | Genuinely open — the term is undefined, or the answer depends on facts not yet decided. |

---

## 1. Headline: the code itself is cheap; what the ICO has said *since* the code is not

**The single most consequential finding is §4.2: the ICO's position since March 2026 is that a service which *publishes* a minimum age must enforce it with an *effective age gate*, that self-declaration is not one, and that the failure is a *lawful basis* failure rather than a risk-proportionality one. Docket publishes a 16+ minimum and enforces it with self-declaration alone.**

That is a different attack from anything [#7](https://github.com/mandyMooreFan/docket/issues/7) or [#20](https://github.com/mandyMooreFan/docket/issues/20) considered. #20 correctly established that raising the floor buys nothing under the OSA; **it did not consider that the ICO might come at the 16 itself, precisely because Docket states it.** The argument is contestable and §4.2 gives four reasons it is weaker at 16 than at the 13 the ICO was writing about — but it is live, the ICO is actively enforcing it this year, and its August 2026 strategy update records work on age assurance **at 16 specifically**. **This is the one item in this document that could reopen a closed decision, and it is marked LAWYER.**

Four further findings, in order of how much they cost.

**1. The code applies, and the only route out is one Docket has already refused for product reasons. (a)** Docket is a relevant information society service — being free, open-source and single-instance changes nothing (§2.2) — and children are likely to access it on the ICO's own deliberately low threshold, because [#7](https://github.com/mandyMooreFan/docket/issues/7) set the floor at 16 *specifically to admit* the school-leaver and the apprentice. The code's escape hatch is robust age assurance, available only *"if it would not be appropriate"* for children to use the service. **Docket's whole position is that it is appropriate.** (§2.1)

**2. ⚠️ The code's clock runs earlier than the OSA's, and this is the finding that changes the schedule. (a)** [`statutory-duties.md` C1](./statutory-duties.md) established that a new Part 3 service gets **three months from its first day of operation** to complete its OSA assessments. **The Children's code gives no such grace.** Standard 2:

> "You **must complete your DPIA before the service is launched**, and ensure the outcomes can influence your design. You should not treat a DPIA as a rubber stamp or tick-box exercise at the end of the design process."

A DPIA is not optional and not a judgement call:

> "The nature and context of online services within the scope of this code mean they **inevitably** involve a type of processing likely to result in a high risk to the rights and freedoms of children. … In practice, this means that **if you offer an online service likely to be accessed by children, you must do a DPIA**."

**So the binding pre-launch deliverable is the DPIA, not the OSA paperwork.** #20 handed #13 two documents due three months *after* launch. This adds a third, due *before* it, and it is the largest of the three because it must also record how Docket conforms to each of the fifteen standards.

**3. Conforming to the code now discharges a directly enforceable statutory duty, not just an evidential standard. (a)** [DUAA 2025 s.81](https://www.legislation.gov.uk/ukpga/2025/18/section/81/enacted) inserted **Article 25(1A)–(1B) UK GDPR** — the *"children's higher protection matters"* — in force since **5 February 2026**, triggered on exactly the same services as the code. The ICO's position: *"if you already conform to the code, you are likely to comply with this duty."* **§11a.** Neither [`statutory-duties.md`](./statutory-duties.md) nor [`data-rights.md`](./data-rights.md) has it.

**4. The good news is real and it is structural. (b)** Docket's refusals — no ranking, no recommender, no personalisation, no engagement metrics, no ads, no infinite scroll, no push, no read receipts, no follow graph, no people-you-may-know — do not merely satisfy individual standards by accident. They **lower the risk level against which everything else in the code is measured**, including the level of age certainty the code demands. That is the whole of Docket's answer to §4.2, and it arrives from three independent directions — Standard 3's risk factors, Annex C's Article 8 route, and the ICO's own statement that *"significant" scales with risk* (§12). **The refusals are load-bearing, and the DPIA is where that argument gets made or lost.**

**The residual design work is small.** Of fifteen standards, one is inapplicable (connected toys), one is conditional and unengaged (parental controls), and nine are satisfied outright or by refusal. **Four carry genuine gaps**, and they are listed in §13 — along with the age-gate question, which comes from outside the code entirely.

**Read §13 and §14 together.** The gaps are real but mostly clerical; the *"what this does not require"* list is longer than the gaps list, and every item on it is a place Docket had already declined to build.

---

## 2. Does the code apply at all?

### 2.1 The test, and how it *actually* differs from the OSA's

Section 123 DPA 2018 applies the code to *"relevant information society services which are likely to be accessed by children."* The 2020 code text sets a threshold:

> "We consider that for a service to be 'likely' to be accessed, **the possibility of this happening needs to be more probable than not.** This recognises the intention of Parliament to cover services that children use in reality, but does not extend the definition to cover all services that children could possibly access."

**⚠️ But the code is not the current authority on its own scope test. (a)** The ICO published dedicated guidance in 2023 — [**'Likely to be accessed' by children – FAQs, list of factors and case studies**](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/childrens-information/childrens-code-guidance-and-resources/likely-to-be-accessed-by-children/) — which restates the test around *"a significant number of children"* and **sets that bar deliberately low**:

> "'**Significant**' in this context **does not mean that a large number of children must be using the service or that children form a substantial proportion of your users. It means that there are more than a *de minimis* or insignificant number of children using the service. This low threshold** depends on a variety of factors relating to the type of service, how it has been designed and the personal data processing risks that it presents to children."

**This is the document Ofcom cites, and it changes the comparison materially.** #22's framing — following Ofcom's warning — treats the two tests as different in a way that implies they might come out differently. **On the primary text, they are legally separate but substantively convergent.**

**Ofcom's actual words. (a)** Children's Access Assessments Guidance **¶1.5**, in full — note the trailing clause, which the ticket's excerpt omitted:

> "You may already have assessed whether a service is likely to be accessed by children as set out in the ICO's Children's code for the purposes of complying with data protection regulation. Please note that **the requirements of data protection law are different, and you will need to carry out a separate children's access assessment**, although **you may be able to draw on similar evidence and analysis for both**."

Its footnote 5 cites the ICO's 2023 guidance by name and adds: *"Nothing in this guidance should be taken to comment on the data protection law requirements."* **¶1.5 and that footnote are the only mentions of the ICO, data protection, or the Children's code in the entire guidance.**

**And Ofcom repeatedly asserts *compatibility*, with the ICO agreeing. (a)** From Ofcom's Age Assurance and Children's Access Statement:

> "**5.62** In response to stakeholders who commented that our approach to 'significant number' was inconsistent with that of the ICO, **we remain of the view … that our approach is compatible with the ICO's guidance on its Children's code** which, similarly, does not offer a numerical threshold for 'significant' … and also encourages providers to consider a range of relevant factors."

> "**5.96** … the ICO commented that the factors in its own non-exhaustive list … **are 'broadly the same as those outlined by Ofcom'**, and agreed with our suggestion that providers may be able to consider evidence that they might already have gathered for the purposes of assessing themselves against the ICO's Children's code."

> "**3.290** Compliance by service providers with both the online safety and the data protection regime is mandatory and **should not be considered a trade-off**."

**Corrected comparison:**

| | **OSA children's access assessment** | **Children's code** |
|---|---|---|
| Instrument | [s.35–37 OSA](https://www.legislation.gov.uk/ukpga/2023/50/section/35) | [s.123 DPA 2018](https://www.legislation.gov.uk/ukpga/2018/12/section/123) |
| Structure | **Two stages** — possibility of access, then the "child user condition" | **One assessment**, run against a non-exhaustive factor list |
| Threshold | **"Significant number"**, undefined; *"Even a relatively small number or percentage of children could be a significant number"* | **"Significant number"**, undefined; *"more than a* de minimis *or insignificant number"* — expressly *"this low threshold"* |
| Numerical threshold | **None**, deliberately | **None**, deliberately — *"Numerical thresholds can be inflexible"* |
| Escape hatch | **Highly effective age assurance** settles Stage 1 | **Robust age assurance** takes you out of scope — see below |
| Self-declaration | Not capable of being highly effective ([s.230(4)](https://www.legislation.gov.uk/ukpga/2023/50/section/230)) | *"**Self-declaration will not be sufficient to demonstrate that children cannot access your service.**"* |
| "Child" | Under 18 | Under 18 — *"defined in the UNCRC and for the purposes of this code as a person under 18"* |
| Not assessing | [s.37(4)–(5)](https://www.legislation.gov.uk/ukpga/2023/50/section/37) **deems** the service likely to be accessed | No deeming provision, but *"you risk non-compliance with your accountability requirements"* |

**⚠️ Correcting a point I had wrong on a first reading: the code *does* have an age-assurance escape hatch, and it is the same one. (a)** The 2023 guidance is explicit that a service which concludes children are likely to access it has *"two options"*:

> "- apply the principles of the code to all users in a risk-based and proportionate way; or
> - **apply appropriate age assurance measures to restrict access by under 18s so that they are no longer likely to access the service, if it would not be appropriate for them to do so. If you do this, the code will not apply to you.**"

**But look at the condition attached: *"if it would not be appropriate for them to do so."*** Every case study in which the ICO endorses the age-assurance route — dating, pornography, an 18+ game — is a service where children's presence is the problem. **Docket's 16-year-old is not a leak to be plugged; [#7](https://github.com/mandyMooreFan/docket/issues/7) built the floor to let them in.** So the escape hatch is closed to Docket **as a matter of product, not of law** — which is a cleaner and more durable reason than "no escape hatch exists", and it is the reason that should go in the record.

**The practical upshot for #13: the two assessments are separate documents, but ¶1.5 licenses shared evidence, and the ICO says the factors are broadly the same. Write the evidence once.** That is a real saving, and it was not visible from the ticket's framing.

### 2.2 A free, open-source, non-commercial service is still a relevant ISS **(a)**

This is the point where the instinct that a hobby project is beneath the law's notice fails again, and the code closes it explicitly:

> "These services are covered **even if the 'remuneration' or funding of the service doesn't come directly from the end user.** … This code also covers **not-for-profit** apps, games and educational sites, **as long as those services can be considered as 'economic activity' in a more general sense. For example, they are types of services which are typically provided on a commercial basis.**"

**A professional network with a jobs board is the paradigm case of a service type provided on a commercial basis** — LinkedIn and Indeed are the comparators [`statutory-duties.md` C6](./statutory-duties.md) already tabled. **(b)** as applied, **(a)** on the rule.

Annex A's flowchart makes this the operative decision node, and phrases it as a question Docket answers *yes*: *"Is your online service the kind of service which is **typically provided or funded on a commercial basis** (even if the funding isn't provided by the end user)?"* The annex also sets expectations about how often the answer goes the other way: *"we expect the vast majority of online services used by children to be covered, and **those that aren't covered to be exceptional**. The services that fall out of scope tend to do so for fairly technical legal reasons."*

None of the carve-outs reach Docket: it is not a public authority service, not a website merely describing a real-world business, not voice telephony, not a broadcast service, and not a *"preventive or counselling service"*.

**Jurisdiction is satisfied. (a)** *"The DPA 2018 applies to online services based in the UK."* Docket is UK-based.

### 2.3 Is Docket "likely to be accessed by children"? **(b), and effectively settled by #20**

**Yes, and the argument is shorter here than it was for Ofcom.** Docket does not merely fail to exclude children; [#7](https://github.com/mandyMooreFan/docket/issues/7) set the floor at 16 *in order to* serve *"the apprentice or school-leaver who genuinely is job-hunting."* A service that deliberately admits 16- and 17-year-olds to serve them cannot argue their access is improbable.

The code also anticipates services in exactly Docket's position:

> "If your service is not aimed at children but is not inappropriate for them to use either, then your focus should be on assessing how appealing your service will be to them. If the nature, content or presentation of your service makes you think that children will want to use it, then you should conform to the standards in this code."

**And there is a practical trap worth naming. (b)** The tests are legally distinct, so an OSA "yes" does not *decide* the code's question. But #20 has publicly concluded, on Ofcom's own factors — careers content, profiles, connections, direct messaging — that children are likely to access Docket. **Arguing "yes" to Ofcom and "no" to the ICO on the same facts is not a position that could be defended**, least of all when Ofcom says its factors are *"broadly the same"* as the ICO's and the ICO agreed. The permissive answer is not merely expensive, as #20 found for the OSA; here it is unavailable.

Note the code's own words about documenting a "no": *"If you decide that your service is not likely to be accessed by children and that you are therefore not going to implement the code then you should **document and support your reasons** for your decision."* Not needed. Docket's answer is yes.

### 2.4 ⚠️ The one case study that cuts the other way — and why it does not rescue Docket **(b)**

The 2023 guidance's **"SME and hobby sites"** case study is uncomfortably close to home, and it is the closest thing to a Docket analogue either regulator has published:

> "A small business which provides **services for parents to help them return to work after a career break** is assessing whether they are likely to be accessed by children. They consider the list of factors, and cannot find any evidence to suggest that their service would be appealing to children. This is because the content, design features and activities on the site are not appealing to children. … **They therefore document their decision that they do not need to take any action at present.**"

**This is a careers service that the ICO puts out of scope.** And it sits in direct tension with Ofcom, whose CAA Guidance Table 7 lists *"Content about future careers and finance"* and *"Content providing advice on further education and careers"* **as content appealing to children** — the very finding #20 relied on. **So the two regulators do diverge on Docket's subject matter, just not in the direction #22 anticipated: the divergence is about careers content, not about the threshold. (b)**

**It does not rescue Docket, for three reasons, and the distinctions are clean:**

1. **The cohort is defined as adult.** "Parents returning to work after a career break" excludes children by description. **Docket's floor was set at 16 precisely to include the school-leaver and the apprentice** — the one population that is simultaneously child and jobseeker.
2. **The case study service has no profiles, connections, feed or messaging.** Ofcom ¶4.31 names exactly those functionalities as *"appealing to children and used by children"*, and the ICO's own factor list includes *"design features and activities which are appealing to children"* and *"presence of children"*.
3. **Docket has already answered the question publicly in #20.** Factor one on the ICO's list is *"Whether children can access your service … **Self-declaration will not be sufficient**."*

**Worth recording in the assessment anyway.** The ICO's factor list asks for *"research relating to similar providers of ISS"*, and this case study is the ICO's own nearest published comparator. Citing it and distinguishing it is stronger than not mentioning it — and it is exactly the *"efforts made to conform"* the ICO says it weighs (§12).

---

## 3. ⚠️ Standard 2 — the DPIA is mandatory, pre-launch, and carries most of the code's cost **(a)**

This is the deliverable. Everything else in this document is, in practice, a section of it.

**It must be embedded in design, not bolted on:** *"You must embed a DPIA into the design of any new online service that is likely to be accessed by children."*

**Step 2 requires Docket to describe**, among other things: whether children are likely to access; **the age range of those children**; plans for parental controls; plans for establishing age; the intended benefits for children; **the commercial interests taken into account**; any profiling or automated decision-making; **any geolocation elements**; the use of any nudge techniques; **any processing of inferred data**; relevant industry standards; and equality-legislation responsibilities.

**Step 4 requires an explicit conformance statement:** *"you should include **an explanation of how you conform to each of the standards set out in this code**."* — plus lawful basis (still undetermined; [`data-rights.md` §8.6](./data-rights.md) flagged this and it is now blocking two documents rather than one).

**Step 5 is the risk list, and four of its entries land on Docket specifically:**

> "- physical harm;
> - **online grooming or other sexual exploitation**;
> - social anxiety, self-esteem issues, bullying or peer pressure;
> - access to harmful or inappropriate content;
> - …
> - loss of autonomy or rights (including control over data);
> - …
> - **economic exploitation or unfair commercial pressure**; or
> - any other significant economic, social or developmental disadvantage."

A jobs board serving 16-year-olds engages *economic exploitation or unfair commercial pressure* directly. Image-bearing private Threads ([#6](https://github.com/mandyMooreFan/docket/issues/6)) engage *online grooming*. Neither is prohibited; both must be assessed and mitigated in writing.

**Step 3 consultation is where the small-operator relief lives. (a)** The code expects consultation with children and parents, but scales it:

> "**Depending on the size of your organisation, resources and the risks you have identified**, you can seek and document the views of children and parents… **We will expect larger organisations to do some form of consultation in most cases.** … If you consider that it is not possible to do any form of consultation, or it is unnecessary or wholly disproportionate, **you should record that decision in your DPIA, and be prepared to justify it to us.**"

**A one-person project may record a proportionality justification instead of running a consultation.** It must be recorded, not merely thought. Note the sting in the tail: *"However, it is usually possible to carry out some form of market research or user feedback."*

**Step 7 carries one hard stop. (a)** *"If you identify a high risk that you are not mitigating, **you must consult the ICO before you can go ahead**."* This is [Article 36 UK GDPR](https://gdpr-info.eu/art-36-gdpr/) prior consultation, and it is a launch blocker if triggered.

*"It is good practice to publish your DPIA."* [#16](https://github.com/mandyMooreFan/docket/issues/16) already committed to a published transparency log, so publishing costs Docket almost nothing — **but there is no duty to publish.**

**Annex D provides a DPIA template.** There is no need to invent a format.

---

## 4. Standard 3 — age assurance: the code accepts what Ofcom rejects, and the ICO has since partly taken it back

**§4.1 is the most reassuring finding in this document. §4.2 is the most alarming. They are about the same fact, and they are not reconciled by any source.**

### 4.1 What the code itself says, and it is generous **(a)**

The standard offers a fork:

> "**Either** establish age with a level of certainty that is appropriate to the risks to the rights and freedoms of children that arise from your data processing, **or apply the standards in this code to all your users instead.**"

And on methods, the code is expressly non-prescriptive:

> "This code is **not prescriptive** about exactly what methods you should use to establish age, or what level of certainty different methods provide. … However you should always use a method that is **appropriate to the risks that arise from your data processing**. … In assessing whether you have chosen an appropriate method, **we will take into account the products currently available in the marketplace, particularly for small businesses which don't have the resources to develop their own solutions.**"

> "**Self-declaration** – This is where a user simply states their age but does not provide any evidence to confirm it. **It may be suitable for low risk processing or when used in conjunction with other techniques.**"

**⚠️ The distinction that reconciles this with §2.1, and it is the crux of the whole age question: (a)**

> **Self-declaration cannot get you *out* of the code. It can be a perfectly acceptable way to *apply* the code once you are in.**

The 2023 guidance is categorical on the first half — *"Self-declaration will not be sufficient to demonstrate that children cannot access your service"* — and Standard 3 is permissive on the second. **These are not in tension; they are answers to different questions.** #20 read the OSA's version of the first half and correctly concluded that raising the floor to 18 buys nothing. **The second half is new, and it is what makes Docket's posture workable under this code.**

**Compare the OSA.** [s.230(4)](https://www.legislation.gov.uk/ukpga/2023/50/section/230) and Ofcom's HEAA guidance treat self-declaration as *not capable of being highly effective*, and Ofcom offers no in-regime alternative — under the OSA, if you are in, you are in, and your age assurance is either highly effective or irrelevant. **The code offers a second lever the OSA does not: adjust the data risk instead of the age certainty.** Annex C confirms the same logic for the Article 8 route: *"If you can show that your processing is particularly low-impact and does not carry any significant risk to children, you may be able to show that self-declaration mechanisms are reasonable on their own."*

**Docket's answer is the third option the code offers, and it is the one Docket has already built. (b)** The standard lists three responses to insufficient age certainty:

> "- **reduce the data risks inherent in your service**;
> - put additional measures in place to increase your level of age confidence; or
> - apply the standards in this code to all users of your service."

The risk factors the code names for this assessment are *"the types of data collected; the volume of data; **the intrusiveness of any profiling**; **whether decision making or other actions follow from profiling**; and **whether the data is being shared with third parties**."* **Three of those five are zero for Docket** — there is no profiling, nothing follows from profiling, and there is no third-party sharing. That is not a coincidence of drafting; it is the direct product of [#4](https://github.com/mandyMooreFan/docket/issues/4), [#5](https://github.com/mandyMooreFan/docket/issues/5) and [#15](https://github.com/mandyMooreFan/docket/issues/15).

**The two remaining factors cut the other way and must be met honestly:** Docket collects a full name, a photograph, an employment history, an education history and a location, and **publishes them**. That is not low-risk data. **LAWYER** on whether the balance lands. My reading is that it does, on the strength of the profiling and sharing zeros, **(b)** — but this is the single argument the whole age posture rests on, and it belongs in the DPIA in as many words.

### 4.2 ⚠️⚠️ The finding that reopens a closed decision: a *stated* minimum age is now the ICO's active enforcement front

**Everything in §4 so far comes from the 2020 code. The ICO's position has moved since, and it has moved against Docket's specific configuration — a published 16+ minimum with nothing behind it.**

**First, the governing age-assurance document is not the one usually cited. (a)** The Commissioner's Opinion of October 2021 has been **withdrawn** and replaced by an [updated Opinion of 18 January 2024](https://ico.org.uk/about-the-ico/what-we-do/information-commissioners-opinions/age-assurance-for-the-children-s-code/): *"We have withdrawn our opinion published in October 2021 and have replaced it with this updated version."* It restates the risk-sliding-scale generously for Docket:

> "**If your service presents minimal information processing risks to children, self-declaration may be appropriate.**" (§5.4)

> "The Commissioner does not consider that self-declaration on its own is an appropriate method for services that are **considered high risk**." (§5.3)

**Second, [the ICO/Ofcom joint statement on age assurance of 25 March 2026](https://ico.org.uk/media2/5ybpmabf/ofcom-ico-joint-statement.pdf) states the two regulators' tests side by side — and this is the cleanest answer to #22's question (h) available anywhere. (a)**

> "- Where your service falls in scope of the OSA age assurance duties, you must have an age assurance process that is **highly effective** at determining whether or not a user is a child.
> - **If you set a minimum age for your service, you should use an *effective age gate* to prevent underage access and avoid unlawful processing under UK GDPR.**"

> "If you are using age assurance to comply with these obligations under data protection law, then you must ensure that the age assurance method you choose is **necessary, effective for your purposes and proportionate to level of risk** on your service."

> "**We agree that self-declaration alone is not an effective means to determine the age or age range of users and prevent access by underage users.**"

Note also what Ofcom concedes in the same document, which helps: *"**You do not need to use HEAA to prevent children under your minimum age from accessing your service.** However, if you do not, you should assume that underage children are present on your service."* **The OSA does not require Docket to enforce its own 16. The question is entirely a data protection one.**

**Third — and this is the sharp edge — the ICO's [open letter of 12 March 2026](https://ico.org.uk/about-the-ico/open-letter-to-social-media-and-video-sharing-platforms-on-strengthening-age-assurance/) reframes the issue from *risk proportionality* to *lawful basis*: (a)**

> "**Where services have set a minimum age – such as 13 – they generally have no lawful basis for processing the personal data of children under that age on their service.** If your service is not suitable for children under a minimum age set out in your terms of service, you should therefore prevent access to children under your minimum age by implementing an **effective age gate**."

> "We understand that most services are relying on self-declaration to identify whether children are 13 or over… **As currently deployed, we don't think that these tools are effective and therefore they should not continue to be relied upon.**"

A [follow-up statement of 21 May 2026](https://ico.org.uk/about-the-ico/media-centre/news-and-blogs/2026/05/ico-statement-on-age-assurance/) hardened it: *"none have yet introduced new, viable and privacy friendly age assurance solutions … we are ready to use the full range of regulatory powers available to us."* And the ICO's **August 2026** strategy update records that it is *"working closely with Ofcom on the rapid assessment of highly effective age assurance **at 16**"* — Docket's exact threshold, live this month.

**⚠️ Why this matters more than anything else in this document. (b)** [#7](https://github.com/mandyMooreFan/docket/issues/7) closed the age-assurance question with *no ID upload, no corporation in the login path*. [#20](https://github.com/mandyMooreFan/docket/issues/20) reopened and re-closed it, correctly, on the ground that a bare 18+ term *"buys no regulatory relief whatsoever"* under the OSA. **Neither considered that the ICO might attack the 16 itself, on lawful-basis grounds, precisely *because* Docket publishes it.** That is a different argument from anything #20 examined, and Standard 6's requirement to uphold your own *"age restriction"* (§9.3) is its code-side twin.

**Four things genuinely weaken the argument as applied to Docket, and they should be in the record: (b)**

1. **The letter is addressed to social media and video-sharing platforms**, and its worked example is the **13** threshold, where [Article 8](https://gdpr-info.eu/art-8-gdpr/) does independent work. **At 16, a 14-year-old who lies is still above the Article 8 line and can give their own consent** — so "no lawful basis" is a materially weaker claim at 16 than at 13. **This distinction is mine; no source draws it. (c)**
2. **Docket does not rely on consent** (Annex C: contract or legitimate interests for core processing), so the Article 8 machinery the letter leans on is not Docket's machinery.
3. **The 2024 Opinion, which is formal guidance rather than an open letter, still says self-declaration may be appropriate for minimal-risk processing** — and it is the instrument with statutory standing here. The two are in tension and the ICO has not reconciled them.
4. **The "current viable technologies" the ICO names — facial age estimation, digital ID, one-time photo matching — are exactly what #7 refused**, and the Opinion itself warns that age assurance *"may be disproportionately intrusive"* and *"may result in exclusion or discrimination of already marginalised groups."* **The ICO is arguing against itself here, and Docket is on the side it usually takes.**

**This is a decision for the map, not a research conclusion, and it is the second time the age question has come back. LAWYER — this is the single item in this document most in need of one.** The options are visible without my choosing among them: keep 16 and argue minimal risk on the strength of the refusals in §4; adopt an age-estimation gate and contradict #7; or reconsider whether Docket needs to *state* a minimum age at all, since the ICO's argument is triggered by the statement. **I am not recommending any of these.**

### 4.3 Applying the standards to all users

**This does not mean treating adults as children. (a)** The code says so directly:

> "However, it doesn't mean that you have to ignore any information you do have about the user's age, or that **adult users have to be infantilised**. It just means that all users will receive some basic protections in how their personal data is used by default. You should apply the standards in the code in a way that recognises **both** the information you do have about the user's age **and** the fact that your level of confidence in this information is inadequate to the risks inherent in your processing."

**So #20's asymmetric design — protections derived from declared age — is the right shape.** What the code adds is that the shape must be justified by a risk argument, and that where confidence is low the protections should not stop dead at the self-declared boundary.

### 4.4 ⚠️ Two cheap measures the code names that Docket does not have **(a)**

The code lists **technical measures** as a way to *"support or strengthen self-declaration mechanisms"*:

> "Examples include **neutral presentation of age declaration screens (rather than nudging towards the selection of certain ages)**, or **preventing users from immediately resubmitting a new age if they are denied access** to your service when they first self-declare their age."

Standard 13 says the same thing as a prohibition:

> "You should not use nudge techniques that might lead children to lie about their age. For example **pre-selecting an older age range for them, or not allowing them the option of selecting their true age range**."

**A signup that asks "confirm you are 16 or over" is exactly the pattern the code names**, because it does not allow a 14-year-old the option of selecting their true age — it offers one box and a refusal. Asking for an age or age band neutrally, and refusing an immediate retry after a rejection, are the two named fixes. Both are trivial. **Neither is in [#7](https://github.com/mandyMooreFan/docket/issues/7)'s signup.**

---

## 5. Standard 7 — default settings, and what "high privacy" means for a public resume

**Standard:** *"Settings must be 'high privacy' by default (unless you can demonstrate a compelling reason for a different default setting, taking account of the best interests of the child)."*

### 5.1 The provision that decides it **(a)**

The code grounds the standard in [Article 25(2) UK GDPR](https://gdpr-info.eu/art-25-gdpr/) and quotes it:

> "In particular, such measures shall ensure that **by default personal data are not made accessible without the individual's intervention to an indefinite number of natural persons.**"

and glosses it:

> "This means that, by default, you should not: … **make your users' personal data visible to indefinite numbers of other users of your online service.**"

> "This means that children's personal data is **only visible or accessible to other users of the service if the child amends their settings to allow this**."

### 5.2 Public-by-default is plainly out for a child, and #20 already fixed that **(a)**

[#2](https://github.com/mandyMooreFan/docket/issues/2)'s public, indexable Profile is the exact thing Article 25(2) names. **#20's override — under-18 Profiles un-indexed and members-only regardless of the dial — removes the open web, which is the large half of the problem.** That override was reasoned to from [s.11(6)(e)](https://www.legislation.gov.uk/ukpga/2023/50/section/11) of the OSA with no reference to data protection, and it happens to land on the code's central requirement. **Satisfied by accident of a refusal already made** — but only partly.

### 5.3 "Members-only" and open signup — a collision that the ICO's later guidance largely resolves in #20's favour **(b)**

Docket's signup is **open, free, and magic-link only** ([#7](https://github.com/mandyMooreFan/docket/issues/7)). Anyone with an email address can become a Member in one step. So for an under-18 Profile set to members-only, the audience is:

- not the open web, and not indexed — **good**; but
- **still an indefinite number of natural persons**, because membership is not a boundary, it is a form.

The code's own words are *"only visible or accessible to **other users of the service** if the child amends their settings"*. On a plain reading, **the high-privacy default for a child on Docket's three-position dial is connections-only, not members-only.** #20 chose the middle position for OSA reasons; the code's default argument points one notch further.

**⚠️ But the ICO has since published the alternatives it accepts, and #20 hit two of them. (a)** This is the most useful thing found in the whole research, and it is not in the code — it is in the [Children's code strategy progress update of March 2025](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/childrens-information/childrens-code-guidance-and-resources/protecting-childrens-privacy-online-our-childrens-code-strategy/children-s-code-strategy-progress-update-march-2025/):

> "The code is **not prescriptive** about how organisations should deliver this. However, the code does set out that other users should only see a child's information if the child amends their settings to allow this, unless there is a compelling reason to do otherwise.
> Organisations can achieve this by making children's profiles private by default. **Alternative approaches might include:**
> - enabling children to have public profiles that do not contain personal information;
> - **providing settings to control the visibility and searchability of children's content and associated profile information;**
> - **providing settings or safeguards to prevent children receiving messages from strangers;** or
> - limiting the visibility of children's personal information in profiles to 'friends only.'"

**These are alternatives, not cumulative requirements. #20 independently delivered the second and the third:**

- *"control the **visibility and searchability**"* → under-18 Profiles are members-only, un-indexed, and **absent from people search**. #20 built the searchability limb explicitly, from [s.11(6)(e)](https://www.legislation.gov.uk/ukpga/2023/50/section/11)'s *"search for"*.
- *"prevent children receiving messages from strangers"* → **an adult cannot send a Connection request to an under-18**, and #6's single gate means the connection request *is* the message request. #20 built this from s.11(6)(e)'s *"contact"* limb.

**So #20's OSA-derived design lands on two of the ICO's own four named alternatives, and it is materially better supported than a plain reading of Article 25(2) suggested. (b)** I had this as a probable spec change on the first pass; **on the strategy text it is not one.** The honest conclusion is that **members-only plus un-indexed plus out-of-search plus no-adult-contact is a defensible high-privacy configuration**, and what remains is to *say so* in the DPIA rather than to change the design.

**The compelling-reason argument, should it be needed, is also strong. (b)** Standard 1 lists among the child's interests *"freedom of association"* and *"protection from economic … exploitation"*, and the UNCRC frame includes access to work. **A connections-only default would leave a 16-year-old job-seeker invisible to every prospective employer on a service whose entire purpose is to be found for work** — and a new Member has no connections at all, so connections-only means visible to nobody.

**Two cautions remain. (b)** The ICO's footnote to that passage: *"There are risks when the personal information within profiles is made public. This could for example facilitate bullying or result in children receiving **unwanted attention or contact from strangers**."* And the [December 2025 update](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/childrens-information/childrens-code-guidance-and-resources/protecting-childrens-privacy-online/) cites as good practice platforms *"only allowing child users to interact with users of the same age group"* — **stricter than #20, which lets a child initiate to an adult.** That is *good practice* named in a strategy document, not a requirement, and #20's asymmetry has a reasoned best-interests basis. **It should be acknowledged and distinguished in the DPIA, not silently passed over.**

### 5.4 The three sub-questions #22 asked

**Connections list — already satisfied. (a)** [#2](https://github.com/mandyMooreFan/docket/issues/2): *"A member's full connection list is visible to their connections only."* That is high privacy by default for that element, decided for product reasons before any of this was considered. Nothing to do.

**⚠️ Posts — a genuine hole. (b)** #20's override is scoped to the **Profile**. [#15](https://github.com/mandyMooreFan/docket/issues/15) settled that *"public profiles, public posts, company entities, and job postings"* are searchable, and treats *"non-members searching the open web"* as a first-class case. **So an under-18 Member's Profile is un-indexed and out of people search, while their Posts may remain public and indexed.** That is precisely *"visible to an indefinite number of natural persons"*, it is inconsistent with the override that sits beside it, and it leaks the same identity through a different door. **Nothing in #20, #15 or #4 addresses it.**

**⚠️ Open-to-work flag — default unstated, and it is a sensitive one. (b)** #2 specifies *"a **quiet open-to-work flag**, member-controlled and shown at the member's chosen audience"* but does not state its default. Under Standard 7 a member-controlled visibility setting must default to high privacy, so for an under-18 it must default **off**. It also deserves particular care: a flag announcing that a 16-year-old is looking for work, on a service where adults can see it, sits directly on the DPIA's *"economic exploitation or unfair commercial pressure"* risk.

**Also unresolved and worth naming: the connection count and mutual connections shown on a Profile (#2) are graph facts about a child, visible to any Member who can see the Profile. (c)** No source is on point. Minor, but it belongs in the DPIA's description of processing.

### 5.5 ⚠️ #20's accepted cost has a data-protection dimension it was not weighed against **(b)**

#20 recorded plainly: *"**Accepted cost:** under-18 status becomes inferable from behaviour — a profile absent from search that cannot be sent a request."*

That was weighed as a privacy cost against an OSA benefit. **Under the code it is more than a cost: it is processing.** The code *"covers your use of **'inferred data'** (information about a child that you don't collect directly, but that you infer from other information or from their behaviours online)"*, and DPIA Step 2 requires describing *"any processing of inferred data"*. Docket is not inferring anything — but its design makes a child's age band **inferable by third parties** from the service's own behaviour.

**I found no source addressing whether a design that makes a protected characteristic externally inferable is itself a data protection problem, as opposed to merely a design trade-off. (c)** The conclusion is not that #20 was wrong — the alternatives it rejected were worse — but that **the trade-off must be restated in the DPIA in data protection terms**, because the DPIA is where a knowingly accepted risk has to be justified rather than merely noted.

---

## 6. Standard 8 — data minimisation, and the deliberately-published resume

**Standard:** *"Collect and retain only the minimum amount of personal data you need to provide the elements of your service **in which a child is actively and knowingly engaged**. Give children separate choices over which elements they wish to activate."*

### 6.1 The code does not carve out deliberately-published data — but its wording accommodates it **(c) on the question, (b) on the answer**

**#22 asked whether a professional resume, published on purpose, is treated differently from ordinary social data. No source says it is.** The code never distinguishes volunteered-and-published data from observed or inferred data in this standard, and I could find nothing in the code, its annexes, or its supporting guidance that addresses professional or vocational services at all.

**But the standard's own language fits Docket unusually well. (b)** Three phrases do the work:

- *"in which a child is **actively and knowingly engaged**"* — a resume is the paradigm of active, knowing engagement. The code's counter-example is location tracking that continues after the map is closed: *"It is not acceptable to continue to track their location after they have closed the map or reached their destination."* Docket has no analogue.
- *"the minimum amount of personal data you need to **provide the elements of your service**"* — for a service whose product *is* the published resume, the resume is the element.
- The **core service** doctrine in Standard 7: *"It is not necessary however for you to provide a privacy setting for any personal data that you have to process in order to provide your core or most basic service."*

**So Docket has a strong core-service argument for the Profile's contents.** The check on it is the code's warning against stretching the concept: *"You should take care not to abuse the concept of a core service by applying it more widely than is warranted."* Docket is well inside that line — it collects nothing it does not display, and displays nothing it did not collect from the Member.

**The genuinely useful distinction the code draws is not *what you collect* but *who can see it*.** Minimisation governs collection; Standard 7 governs visibility. A resume being deliberately published answers the first question and **says nothing about the second** — which is why §5 is where the real gap is and this section is not.

### 6.2 ⚠️ "and **retain**" is the half that bites **(b)**

Minimisation is not only about collection. The code binds it to [Article 5(1)(e)](https://gdpr-info.eu/art-5-gdpr/) storage limitation: *"personal data should be kept 'no longer than is necessary'."* And Standard 15 quotes **Recital 65** on why this matters for children in particular:

> "…that right is relevant in particular where **the data subject has given his or her consent as a child and is not fully aware of the risks involved by the processing, and later wants to remove such personal data, especially on the internet**…"

Annex B's 16–17 entry says the same thing in developmental terms:

> "they are still developing cognitively and emotionally and **should not be expected to have the same resilience, experience or appreciation of the long term consequences of their online actions as adults may have**."

**Docket's model is unusually permanent.** The Thread is *"the single, **permanent** correspondence between a pair of Members"*, both sides keep history after disconnection ([#6](https://github.com/mandyMooreFan/docket/issues/6)), and Posts and Profiles persist. [`data-rights.md` §3](./data-rights.md) already found the question of erasing a two-party Thread to be the largest open question in that research. **Recital 65 does not resolve it, but it tilts it: for content published as a child, the code points toward an affordance for taking it back.** It does not create a new right — Article 17 already exists — but it says the ICO expects that right to be *easy* for a child to use, which is Standard 15's territory.

### 6.3 A concrete, cheap minimisation point on the new age fact **(b)**

#20 handed [#11](https://github.com/mandyMooreFan/docket/issues/11) a constraint: *"**declared age becomes a stored fact**."* Standard 3 attaches a condition to data collected for age purposes:

> "you only collect the minimum amount of personal data you need to give you an appropriate level of certainty about the age of your individual users, and making sure you **don't use personal data collected for the purposes of establishing or estimating age in order to conform to this code for other purposes**."

**Every protection Docket derives from age needs only an age *band*, not a date of birth.** Un-indexing, members-only, search exclusion and the connection-request block all turn on a single boolean. Storing a DOB where a band suffices is a minimisation failure with no offsetting benefit — and a DOB is a far more damaging thing to hold. **This is a one-line decision for #11 and it should be made deliberately rather than by default.**

### 6.4 ⚠️ The Application sends the whole Profile, with no ability to send less **(b)**

[#5](https://github.com/mandyMooreFan/docket/issues/5) settled that *"The Profile **is** the application; there is nothing else to send."* Elegant, and it removes an entire class of gimmick. But Standard 9 defines data sharing broadly:

> "**Data sharing includes making a child's personal data visible to a third party.**"

> "You should not share personal data unless you have a **compelling reason** to do so, taking account of the best interests of the child."

When a 16-year-old applies for a job, their photograph, full name, location, education and history go to a Member acting for a Company. **The compelling reason is obvious and it is the child's own request** — they applied — so this is not a breach. But Standard 8's second sentence, *"Give children separate choices over which elements they wish to activate"*, sits awkwardly against a design in which the child has exactly one choice: send everything, or do not apply.

**I am not proposing a change to #5.** The one-artefact model is a deliberate refusal of a real gimmick, and unbundling it would be a product decision, not a legal conclusion. **The finding is that the DPIA must address it**, because it is the clearest place where a settled Docket decision reduces a child's granular control. **LAWYER** on whether Standard 8's "separate choices" limb reaches inside a single deliberate act of publication.

---

## 7. Standard 12 — profiling. Satisfied, and not quite trivially **(a)**

**Standard:** *"Switch options which use profiling 'off' by default … Only allow profiling if you have appropriate measures in place to protect the child from any harmful effects."*

The GDPR definition the code uses is *"any form of automated processing of personal data consisting of the use of personal data to evaluate certain aspects relating to a natural person…"*, and the code spells out the online forms:

> "It can be used extensively in an online context **to suggest or serve content to users, to determine where, when and how frequently that content should be served, to encourage users towards particular behaviours**, or to identify users as belonging to particular groups. … Profiling may also be used **to suggest other users to 'connect with' or 'follow'**."

**Docket has none of it.** The feed is reverse-chronological and connections-only ([#4](https://github.com/mandyMooreFan/docket/issues/4)); search is unranked and there is no people-you-may-know suggester ([#15](https://github.com/mandyMooreFan/docket/issues/15)); there is no advertising, so no behavioural advertising; there are no engagement metrics to feed anything. **The last sentence quoted above describes the single feature #15 explicitly refused as a gimmick.**

**So the standard is satisfied — but "trivially" is the wrong word in two respects. (a)**

1. **It is satisfied by refusal, not by absence of thought**, and the refusal is worth an enormous amount elsewhere in this document. It is one of the three risk zeros that make the self-declaration argument in §4 work.
2. **It still generates a documentation obligation.** DPIA Step 2 requires describing *"any profiling or automated decision-making involved"*, and the honest answer — *none* — has to be written down to be worth anything. An undocumented absence of profiling is indistinguishable from an unexamined one.

**One thing to check rather than assume. (b)** [#16](https://github.com/mandyMooreFan/docket/issues/16) established exactly one automated exception: *"every uploaded image is hash-matched against known CSAM before it is stored."* **This is not profiling** — it matches content against a known-bad list; it does not evaluate aspects of a person. And even if it were, the code carves it out: *"if you are profiling in order to meet a legal or regulatory requirement (such as a safeguarding or child protection requirement), **to prevent child sexual exploitation or abuse online**…"* it is not appropriate to offer a privacy setting over it. **It must still be disclosed** in the privacy information and the DPIA — and #20 already noted the parallel OSA duty to *"disclose any proactive technology"*, so this is one disclosure serving two regimes.

**Article 22 is not engaged either. (b)** #16's moderation ladder is human throughout — *"Everything is reported by a human and judged by a human"* — so no decision with legal or similarly significant effect is *"based solely on automated processing"*. Recital 71's statement that such decisions *"should not concern a child"* has nothing to bite on. **#16's refusal of automated account actions turns out to be worth a whole GDPR article.**

---

## 8. Standard 13 — nudge techniques. Satisfied wholesale, with two exceptions pointing opposite ways

**Standard:** *"Do not use nudge techniques to lead or encourage children to provide unnecessary personal data or turn off privacy protections."*

The three patterns the code illustrates are a prominent "yes" against a small-print "no"; asymmetrically positive framing of two options; and *"making one option much less cumbersome or time consuming than the alternative … providing a low privacy option instantly with just one 'click', and the high privacy alternative via a six click mechanism."*

**Docket's map refused the engagement mechanics that generate most of these, and the refusals are real. (a)** No infinite scroll, no push notifications, no re-engagement email, no read receipts, no presence dots, no badges except an inbox unread count *"earned by a person writing to you personally"* ([#6](https://github.com/mandyMooreFan/docket/issues/6)). #6's stated reason — *"That layer exists to manufacture urgency"* — is the nudge standard's own reasoning arrived at independently.

**⚠️ Exception one: the age gate, already covered in §4.4.** *"pre-selecting an older age range for them, or not allowing them the option of selecting their true age range"* is the specific nudge the code names, and it is the one Docket is most likely to ship by default.

**⚠️ Exception two, and it is subtle: #7's incomplete-profile rule is a structural nudge toward publication. (b)** [#7](https://github.com/mandyMooreFan/docket/issues/7) made incomplete Profiles members-only and un-indexed — a sound anti-spam measure. But viewed from Standard 13, it means **the service withholds reach until the Member supplies more personal data**, which is close to *"lead or encourage children to provide unnecessary personal data"*. The saving argument is that the data is not *unnecessary* — a resume with no content is not a resume — and that the rule is an abuse control, not a growth mechanic. **(b)** that this holds. It is worth a sentence in the DPIA rather than a design change.

**A third thing the code asks for that Docket currently has no place for. (a)** Standard 7:

> "If a user does change their settings you should generally give them the option to do so **permanently or to return to the high privacy defaults when they end the current session**. You should not 'nudge' them towards taking a lower privacy option."

Docket's visibility dial is a persistent setting with no session concept. Whether a session-scoped option is warranted here is a judgement for the DPIA; the code says *"generally"*, and its own example of the session-revert rule is geolocation, which Docket does not have (§10).

**The 16–17 row of the code's own nudge table is what Docket must actually satisfy:**

> "Provide design architecture which is high-privacy by default. Provide explanations of functionality and inherent risk. Present options in ways that encourage conscious decision making. Signpost towards sources of support including parents. Suggest wellbeing enhancing behaviours (such as taking breaks). Provide tools to support wellbeing enhancing behaviours."

**"Signpost towards sources of support including parents"** is the one line here Docket has nothing for. Note the code softens this for this age band — Annex B: *"Parental support is more likely to be viewed as **one option that they may or may not wish to use** … Signposting to other sources of support in addition to parental support is important."*

---

## 9. Standards 4, 5, 6 and 15 — transparency, detriment, upholding your own rules, and tools

### 9.1 ⚠️ Standard 4 — transparency written for the age of the reader. A real gap **(a)**

**Standard:** *"The privacy information you provide to users, and other published terms, policies and community standards, must be concise, prominent, and in clear language suited to the age of the child. Provide additional specific 'bite-sized' explanations about how you use personal data at the point that use is activated."*

Three distinct requirements, none of which Docket has specced:

1. **Articles 13/14 privacy information, written for 16–17-year-olds**, with *"the choice to upscale or downscale the information they see"* and a choice between written and video/audio formats.
2. **Just-in-time "bite-size" explanations** at the point a use of personal data is activated. The code is explicit that a privacy policy is not enough: *"it is **not sufficient** to rely on children or their parents seeking out this privacy information."* **For Docket the obvious trigger points are the visibility dial, the open-to-work flag, and submitting an Application.**
3. **Terms, conduct policy and community standards in clear language too**, with the concession that *"If you believe that you need to draft your terms and conditions in a certain way in order to make them legally robust, then you can provide child-friendly explanations to sit alongside the legal drafting."*

**The 16–17 row adds a requirement that will surprise a service with no parental relationship: (a)**

> "If a child in this age group attempts to change a default high privacy setting provide written, video or audio materials to explain what will happen to their information and any associated risks. Prompt them to check with an adult or other source of trusted information and not change the setting if they have any concerns or don't understand what you have told them.
> **Provide full information in a format suitable for parents to sit alongside the child focused information.**"

**This lands on [#13](https://github.com/mandyMooreFan/docket/issues/13), and it overlaps precisely with DSA Article 14's parallel duty** — [`statutory-duties.md` §4.7](./statutory-duties.md) found that where a service is predominantly used by minors the terms must be explained so minors can understand. **The ICO's version is broader**, because it does not require minors to predominate and it reaches privacy information as well as terms. **One drafting job satisfies both.**

There is genuine relief on user testing: *"**Depending on the size of your organisation, your number of users, and your assessment of risk** you may decide to carry out user testing… If you decide that user testing isn't warranted, then you should **document the reasons why in your DPIA**."*

**⚠️ And there is a much larger relief that halves this gap: Docket only has to write for 16–17-year-olds. (a)** The code says so — *"There is **no requirement for you to design services for development stages that aren't likely to access your service**"* — and the 2023 guidance's Games case study 2 applies it in exactly the way Docket needs:

> "**they are aware that there is no requirement to design their service for primary school children as they aren't likely to access the service. Their privacy notices are already accessible for teenagers. The ISS decide not to offer transparency information for younger audiences** as there is no indication that primary school children are accessing their service. They record this decision including the evidence it is based on and will keep it under review."

**Docket's floor is 16, so only the 16–17 band is in play.** No cartoons, no audio for pre-literate users, no simplified rule-based framing, none of the four younger bands' recommendations. **The obligation collapses to: clear plain-English privacy information pitched at a 16-year-old, just-in-time explanations, a parent-facing version alongside, and a recorded decision that the younger bands are out of scope.** That is a writing job, not a design programme.

Note the same case study is the ICO's clearest published statement that a service with Docket's shape can already be conformant: *"they do not use nudge techniques, and geolocation tracking is already compliant with the code … **they do not need to take any further action. This is because they are already compliant with the code in a way that is appropriate to the data risks that their service represents.**"*

### 9.2 Standard 5 — detrimental use of data. Satisfied by refusal **(a)**

**Standard:** *"Do not use children's personal data in ways that have been shown to be detrimental to their wellbeing, or that go against industry codes of practice, other regulatory provisions, or Government advice."*

The code's operative list is about engagement:

> "you should: avoid using personal data in a way that **incentivises children to stay engaged**…; **present options to continue playing or otherwise engaging with your service neutrally** without suggesting that children will lose out if they don't; avoid features which use personal data to **automatically extend use** instead of requiring children to make an active choice…; introduce mechanisms such as pause buttons…"

> "features which use personal data to exploit human susceptibility to reward, anticipatory and pleasure seeking behaviours, or peer pressure."

**Docket has no such features to remove**, and #4's explicit refusal list was written from the same instinct. The code's phrase *"strategies used to extend user engagement, such as **timed notifications that respond to inactivity**"* names something #6 refused by name.

**The one live risk under this standard is not engagement — it is economics. (b)** Standard 1's best-interests list includes *"keep them safe from exploitation risks, including the risks of **commercial** or sexual exploitation"*, and DPIA Step 5 names *"economic exploitation or unfair commercial pressure"*. **A jobs board is the only part of Docket that touches this**, and #5's mandatory salary range is, as it happens, a mitigation — an under-18 cannot be approached about a posting whose pay is concealed. Worth saying so in the DPIA rather than leaving it implicit.

### 9.3 ⚠️ Standard 6 — upholding your own rules. The one that constrains #16 **(a)**

**Standard:** *"Uphold your own published terms, policies and community standards (including but not limited to privacy policies, **age restriction**, behaviour rules and content policies)."*

This standard is where reactive moderation is examined, and the code permits it **conditionally**:

> "**If you only rely on 'back end' processes, such as user reporting, to identify behaviour which breaches your policies then you need to have made that very clear in your policies or community standards.** This approach also needs to be **reasonable given the risks to children of different ages inherent in your service.** If the risks are high then 'light touch' or 'back end only' processes to uphold your standards are unlikely to be sufficient."

> "**If you do not have adequate systems to properly uphold your own user behaviour policies then your original collection and continued use of a child's personal data may be unfair and in breach of the GDPR.**"

**This is a materially colder statement than Ofcom's.** [`statutory-duties.md` C5](./statutory-duties.md) recorded Ofcom saying small low-risk services *"will only be expected to have basic but important measures"* and *"We are not setting out to penalise small, low risk services trying to comply in good faith."* **The ICO offers no equivalent warmth** — it offers a conditional permission with a disclosure requirement attached.

**Two consequences for #16, both cheap:**

- **The member conduct policy must state plainly that enforcement is reactive and report-driven.** #16 already found the conduct policy to be a gap; this specifies part of its content. Saying "we review reports" is not the same as saying "we do not proactively monitor", and the code asks for the second.
- **⚠️ The "age restriction" limb is the uncomfortable one. (b)** Docket publishes a 16+ minimum that it does not enforce and cannot verify. Standard 6 requires you to uphold your published age restriction; §4.4's neutral age gate and no-immediate-retry measures are the code's own named answer, and without them the published restriction is a promise with nothing behind it. The code's warning is directly on point: *"you should provide information that is accurate and **does not promise protections or standards that are not routinely upheld**."*

### 9.4 ⚠️ Standard 15 — online tools. A small, specific gap **(a)**

**Standard:** *"Provide **prominent and accessible** tools to help children exercise their data protection rights and report concerns."*

> "You have an obligation **not just to allow children to exercise their rights but to help them to do so**."

> "**You should highlight the reporting tool in your set up process and provide a clear and easily identifiable icon or other access mechanism in a prominent place on the screen display.**"

**Docket has the underlying capabilities**: #16 built reporting, and [`data-rights.md` §6](./data-rights.md) established that a manual, on-request data-rights process is compliant. **What the code adds is placement and prominence** — the reporting tool must be surfaced during signup and given a persistent, identifiable affordance, not buried in a footer.

**This does not overturn `data-rights.md`'s finding that a self-service export tool is not legally required. (a)** That finding stands: Article 20 permits a manual route. But the code raises the floor on **discoverability** of whatever route exists, and it interacts with the [DUAA s.103](https://www.legislation.gov.uk/ukpga/2025/18/section/103/enacted) complaint-facilitation duty `data-rights.md` §7 found — which already required *"a complaint form which can be completed electronically"*. **Two regimes now point at the same small piece of UI.**

---

## 10. Standard 10 — geolocation. The standard does not apply; the risk still does **(a)**

**#22 flagged the Profile's location field. The good news is that the standard is defined out of reach of it.**

> "**Geolocation data means data taken from a user's device which indicates the geographical location of that device, including GPS data or data about connection with local wifi equipment.**"

**Docket's location is a field a Member types into their own resume.** It is not taken from a device, it is not GPS, it is not wifi-derived, and it does not track. The standard's three operative requirements — off by default, an obvious sign when tracking is active, and reverting to off at the end of each session — **have nothing to attach to. (a)**

PECR's separate definition of "location data" is narrower still (*"processed in an electronic communications network"*) and plainly does not reach a typed town name.

**⚠️ And the ICO's own enforcement outcomes turn on exactly this distinction, which is the strongest confirmation available. (a)** The March 2025 strategy update lists what platforms changed under ICO pressure on geolocation:

> "Sendit **has stopped automatically populating user profiles with location information**"; "Soda **has removed country-level location information that was previously automatically included in children's profiles**"; BeReal "stopped precise geolocation for under-18s"; X "removed the ability for under 18s to opt-in to geolocation sharing".

**The thing the ICO made them stop was *automatic population of a profile with location the service derived*. Docket's location is a line a Member types into their own resume.** Sendit and Soda are the closest published cases to Docket's field, and **the conduct the ICO objected to is the conduct Docket does not engage in.** That is a considerably better position than the code's text alone establishes.

The ICO's overall strategy position — *"children's profiles should be private by default and **geolocation settings should be turned off by default**"* — is about settings that exist. Docket has no geolocation setting because it collects no geolocation.

**Three carry-overs survive, and none of them is Standard 10. (b)**

1. **The harm rationale travels even though the standard does not.** The code's stated concern is that *"the ability to ascertain or track the physical location of a child … can make children vulnerable to risks such as abduction, physical and mental abuse, sexual abuse and trafficking."* **A town published on a child's Profile alongside their photograph, full name, and school or employer is location information about a child**, however it was obtained. The mechanism the code fears is absent; the exposure it fears is not.
2. **Standard 5 says so directly:** *"You should take particular care when profiling children, including making inferences based on their personal data, **or processing geo-location data**."*
3. **DPIA Step 2 requires describing *"any geolocation elements"*.** The correct entry is not "none" — it is a description of the typed location field and why the standard does not engage.

**The granularity question is a product decision, not a legal one. (c)** The code's minimisation logic — *"consider at what level of granularity the location needs to be tracked … Do not collect more granular detail than you actually need"* — is written for device-derived data, but the instinct reads across. Whether an under-18 Profile should show a region rather than a town is a call for the map, and **I am not making it here.** What the code requires is that the question be asked in the DPIA.

**⚠️ One thing I could not check, and the ICO has named it. (b)** If Docket logs IP addresses — plausible for magic-link auth, rate limiting, or abuse control — those are device-derived and resolvable to a location. **The ICO's December 2025 update lists *"collecting IP addresses"* first among the geolocation practices it scrutinises**, alongside *"mechanisms that nudge users to enable location sharing"* and *"using geolocation information for marketing or advertising purposes"* (the last two do not apply to Docket). **Not decided anywhere in the map I could find**, and it is the one place Standard 10 could yet bite. See §15.

---

## 11. Standards 1, 9, 11 and 14 — briefly

**Standard 1, best interests of the child. (a)** Not a checklist but a lens: *"The best interests of the child should be a primary consideration."* Two sentences matter for Docket. First: *"It is unlikely however that **the commercial interests of an organisation will outweigh a child's right to privacy**."* Docket has no commercial interests at all, which removes the counterweight the ICO is most sceptical of — a genuinely favourable structural fact. Second, the interests to be supported include *"protect and support their right to freedom of association"* and *"recognise the evolving capacity of the child to form their own view."* **#20's asymmetry — a child may send connection requests, and keeps jobs and applications — is a best-interests argument in the code's own terms**, and it should be restated as one. The ICO publishes a [best interests self-assessment](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/childrens-information/childrens-code-guidance-and-resources/) tool.

**Standard 9, data sharing. Satisfied, with the one qualification in §6.4. (a)** Docket shares nothing with third parties. There are no ads, no analytics vendors named in the map, and no data sales. The code's example of an unacceptable reason — *"selling on children's personal data for commercial re-use"* — is structurally impossible here. The only "sharing" in the code's broad sense (*"making a child's personal data visible to a third party"*) is between Members, which Standard 7 already governs.

**Standard 11, parental controls. Conditional, and unengaged. (a)** *"**If you provide parental controls**, give the child age appropriate information about this."* Docket provides none and the code does not require any. Combined with Annex B's 16–17 note that parental support is *"one option that they may or may not wish to use"*, **the absence of parental controls is not a gap.** The only parent-facing obligation anywhere in the code that reaches Docket is Standard 4's parallel information format (§9.1).

**Standard 14, connected toys and devices. Not applicable. (a)**

---

## 11a. ⚠️ A statutory duty neither this ticket nor `data-rights.md` anticipated: DUAA s.81 **(a)**

**Conforming to the code is no longer only "a key measure of your compliance". Since 5 February 2026 there is a directly enforceable Article 25 duty pointing at the same thing.**

[**DUAA 2025 s.81**](https://www.legislation.gov.uk/ukpga/2025/18/section/81/enacted), *"Data protection by design: children's higher protection matters"*, inserts new **Article 25(1A)–(1B) UK GDPR**. Commenced by **S.I. 2026/82** reg. 2(k) on **5 February 2026** — already in force:

> "**1A.** In the case of processing carried out in the course of providing information society services which are likely to be accessed by children, when assessing what are appropriate technical and organisational measures in accordance with paragraph 1, the controller **must take into account the children's higher protection matters**.
>
> **1B.** The children's higher protection matters are—
> (a) **how children can best be protected and supported when using the services**, and
> (b) the fact that children—
> (i) merit specific protection with regard to their personal data because they may be less aware of the risks and consequences associated with processing of personal data and of their rights in relation to such processing, and
> (ii) **have different needs at different ages and at different stages of development**."

**Four observations. (a)**

1. **The trigger is identical to the code's.** The ICO's [updated data protection by design guidance](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/accountability-and-governance/guide-to-accountability-and-governance/data-protection-by-design-and-by-default/) (revised the day it commenced) says: *"The 'children's higher protection matters' duty applies to the **same online services that are in scope of our children's code**."*
2. **Code conformance discharges it.** *"So, **if you already conform to the code, you are likely to comply with this duty**."* **This is the strongest single reason to conform deliberately rather than incidentally** — the work is now doing double duty.
3. **It attaches to Article 25(1) — by *design*, not by *default*.** So it reaches the whole of Docket's architecture, not just its settings.
4. **[DUAA s.91](https://www.legislation.gov.uk/ukpga/2025/18/section/91/enacted)** separately inserts s.120B(e) DPA 2018, making *"the fact that children merit specific protection with regard to their personal data"* a mandatory regard for **the Commissioner** in carrying out every function. **That is a regulator being statutorily pointed at children, and it colours §12.**

**This is the third live UK duty that `statutory-duties.md` and `data-rights.md` between them did not have** — after [OSA s.20A](https://www.legislation.gov.uk/ukpga/2023/50/section/20A) intimate-image reporting and [DUAA s.103](https://www.legislation.gov.uk/ukpga/2025/18/section/103/enacted) complaint facilitation. **It does not add a task; it raises the cost of skipping the ones already listed.**

---

## 12. Enforcement posture toward a service Docket's size **(a)**

**There is proportionality, there is no exemption, and the tone is cooler than Ofcom's.**

What helps:

> "To ensure proportionate and effective regulation we will **target our most significant powers, focusing on organisations and individuals suspected of repeated or wilful misconduct or serious failure to comply with the law**."

> "We will **take account of the size and resources of the organisation concerned, the availability of technological solutions in the marketplace and the risks to children that are inherent in the processing.** We will take a proportionate and responsible approach, focussing on areas with the potential for most harm."

> "We will also take into account **the efforts made to conform to the provision in this code**."

What does not:

> "In line with our policy, we consider that **the public interest in protecting children online is a significant factor weighing in the balance** when considering the type of regulatory action. This means that **where we see harm or potential harm to children we will likely take more severe action against a company than would be the case for other types of personal data.**"

> "If you do not follow this code, **you may find it difficult to demonstrate that your processing is fair** and complies with the GDPR or PECR."

**⚠️ And the ICO has addressed the small-operator question head-on, in terms that leave no room. (a)** From [its response to the consultation on the "likely to be accessed" guidance](https://ico.org.uk/about-the-ico/the-ico-s-response-to-the-consultation-on-the-draft-guidance-for-likely-to-be-accessed/):

> "**The risks are related to the processing undertaken and not the size of the organisation.** Therefore, **SMEs also have a responsibility** to assess whether a significant number of children are accessing their service, as some SMEs may pose high data processing risks to children and should conduct the assessment accordingly."

> "a new SME ISS may only have a small number of children accessing its service, but the processing taking place may track the location data of users … **even a small number or percentage of children would represent a significant number of children in relation to the risk.**"

**Read that carefully: "significant" scales inversely with risk.** The lower Docket's processing risk, the more children it takes to be "significant" — **which is the §4 argument again, arriving from a third direction.** The refusals keep earning.

**Enforcement to date, and the trend line: (b)** TikTok £12.7m (2023), Reddit £14.47m, and **MediaLab.AI (Imgur) £247,590 in February 2026**. **The band is widening downward.** £247k is not a fine sized for a giant, and it should dispose of any assumption that only household names are reachable. **No ICO action against a service of Docket's size was found, and no statement of leniency for one beyond the code's own enforcement section. (c)**

**The net reading: (b)**

- **No de minimis, no hobby exemption, no non-commercial carve-out.** Consistent with [`data-rights.md` §5](./data-rights.md), which found no small-operator exemption anywhere in the UK GDPR regime. The ICO's phrasing — *"risks are related to the processing undertaken and not the size of the organisation"* — is about as explicit as a regulator gets.
- **Size genuinely mitigates**, and it mitigates twice — once in enforcement, and once inside the code itself, in Standard 3's marketplace-availability concession, Standard 2's scaled consultation, and Standard 4's scaled user testing.
- **But children raise the severity dial**, and the ICO says so where Ofcom says the opposite. `statutory-duties.md` C5's *"We are not setting out to penalise small, low risk services trying to comply in good faith"* **has no ICO counterpart. Do not import it across regimes.**
- **The realistic exposure is a complaint, not an audit.** *"If someone raises a concern with us about your conformance to this code … We will assess your initial response to the complaint … we may also ask for details of your policies and procedures, **your DPIA**, and other relevant documentation."* **The DPIA is the artefact that gets asked for.** That is the strongest practical reason to write it well rather than minimally.

---

## 13. Gaps — what changes the v1 spec

Only items that alter a decision or add spec surface. Everything satisfied by an existing refusal is in the analysis above and absent here.

| # | Gap | Standard | Where it lands |
|---|---|---|---|
| **1** | **A DPIA is mandatory and must be complete *before* launch** — earlier than the OSA's three-month post-launch clock. It must describe the processing, assess a named risk list, explain conformance to **each of the fifteen standards**, and record the lawful basis. | 2 | **[#13](https://github.com/mandyMooreFan/docket/issues/13).** A launch precondition and the largest single document the map now owes. Template at Annex D. |
| **2** | **Under-18 Posts are not covered by #20's override.** The Profile is un-indexed and out of people search; Posts remain public and indexed under [#15](https://github.com/mandyMooreFan/docket/issues/15). Same exposure, different door. | 7 | **Amends [#15](https://github.com/mandyMooreFan/docket/issues/15) and/or [#4](https://github.com/mandyMooreFan/docket/issues/4).** A decision for the map. |
| **0** | **⚠️ Docket publishes a 16+ minimum and enforces it with self-declaration alone.** The ICO's March 2026 joint statement and open letter treat an unenforced stated minimum as a **lawful-basis** problem and say self-declaration is not an effective age gate. Contestable at 16 (§4.2 gives four reasons), but live and actively enforced. | — (outside the code) | **Reopens [#7](https://github.com/mandyMooreFan/docket/issues/7) and [#20](https://github.com/mandyMooreFan/docket/issues/20). A decision for the map, not a spec edit. LAWYER — the most urgent in this document.** |
| **3** | **Record why members-only + un-indexed + out-of-search + no-adult-contact *is* "high privacy"** for an under-18, given open signup makes Membership an indefinite audience under Art 25(2). The ICO's own March 2025 alternatives list supports the configuration; the argument just has to be made. | 7 | **[#13](https://github.com/mandyMooreFan/docket/issues/13) via the DPIA. Not a design change** — downgraded from a spec amendment on the strength of the ICO strategy text (§5.3). |
| **4** | **The open-to-work flag has no stated default** and must default **off** for under-18s. It also sits on the DPIA's economic-exploitation risk. | 7 | **Amends [#2](https://github.com/mandyMooreFan/docket/issues/2).** One line. |
| **5** | **The age gate is a nudge the code names.** Present age neutrally rather than as "confirm you are 16+", and **block immediate resubmission** after a refusal. These are the code's own named measures for strengthening self-declaration. | 3, 13, 6 | **Amends [#7](https://github.com/mandyMooreFan/docket/issues/7).** Cheap, and it is what makes the published 16+ restriction something Docket actually upholds. |
| **6** | **Store an age *band*, not a date of birth**, unless a DOB is independently needed. Every derived protection turns on one boolean. | 8, 3 | **[#11](https://github.com/mandyMooreFan/docket/issues/11)**, which #20 already told to store declared age. Decide the shape deliberately. |
| **7** | **An age-appropriate transparency layer, scoped to the 16–17 band only**: privacy information pitched at a 16-year-old, **just-in-time "bite-size" explanations** at the visibility dial, the open-to-work flag and Application submission, plain-language terms and conduct policy, and **a parent-facing version alongside**. Record the decision that younger bands are out of scope. | 4 | **[#13](https://github.com/mandyMooreFan/docket/issues/13).** Overlaps DSA Art 14 ([`statutory-duties.md` §4.7](./statutory-duties.md)) — one drafting job serves both. |
| **8** | **The conduct policy must state plainly that moderation is reactive and report-driven.** The code permits back-end-only enforcement *only* where it is "made very clear" and reasonable for the risk. | 6 | **Specifies the conduct policy [#16](https://github.com/mandyMooreFan/docket/issues/16) already identified as a gap.** |
| **9** | **Reporting and rights tools must be prominent** — highlighted in the signup flow, with a persistent identifiable affordance. | 15 | **[#16](https://github.com/mandyMooreFan/docket/issues/16), [#19](https://github.com/mandyMooreFan/docket/issues/19), [#13](https://github.com/mandyMooreFan/docket/issues/13).** Placement, not new capability. Converges with the [DUAA s.103](https://www.legislation.gov.uk/ukpga/2025/18/section/103/enacted) complaint form. |
| **10** | **The Application → Thread route lets an adult write to a child**, which #20's connection-request block does not close and expressly preserves. Not prohibited; must be assessed and justified. | 2, 1 | **DPIA. May touch [#5](https://github.com/mandyMooreFan/docket/issues/5)/[#6](https://github.com/mandyMooreFan/docket/issues/6) if the assessment concludes so. LAWYER.** |

**Three things deliberately *not* in this table**, because they are documentation obligations rather than spec changes: restating #20's inferable-age cost in data protection terms (§5.5); recording the absence of profiling rather than assuming it (§7); and describing the typed location field and why Standard 10 does not engage (§10). All three belong in the DPIA and none of them changes a decision.

---

## 14. What this does *not* require

Worth stating plainly, because the code's reputation is heavier than its application to a service like this one.

- **⚠️ No *highly effective* age assurance — but "no age assurance at all" is no longer safe to assert. (b)** *The code itself* asks only that age certainty match data risk, calls self-declaration *"suitable for low risk processing"*, takes small-business resources into account, and warns against *"giving users no choice but to provide hard identifiers unless the risks inherent in your processing really warrant such an approach."* Ofcom's HEAA bar does not reach a service outside OSA age-assurance scope. **But §4.2's open letter cuts across this**, and I am not able to say #7's no-ID posture survives intact. **The code leaves it standing; the ICO's 2026 enforcement position may not.** Treated as an open question, not a reassurance.
- **No hard identifiers specifically. (b)** Even on the ICO's hardest current line, the named *"current viable technologies"* are facial age estimation, digital ID **or** one-time photo matching — a menu, not a passport requirement — and the 2024 Opinion's own warnings about intrusiveness and exclusion remain live guidance.
- **No parental consent. (a)** Article 8 UK GDPR as modified by [s.9 DPA 2018](https://www.legislation.gov.uk/ukpga/2018/12/section/9) sets the threshold at **13**. Annex B: UK 16–17-year-olds *"can provide their own consent"*. Docket's floor is 16. The Article 8 machinery never engages.
- **No parental controls, no parental dashboard, no monitoring affordance. (a)** Standard 11 is conditional on providing them. Docket provides none. The only parent-facing item anywhere is Standard 4's information format.
- **No profiling controls, no recommender safeguards, no algorithmic testing, no behavioural-advertising consent flow. (a)** There is nothing to switch off, nothing to test, and no advertising. Standard 12's entire second limb is inert.
- **No geolocation setting, no location-tracking indicator, no session-revert for location. (a)** Standard 10 is defined around device-derived data. A typed location field is not it.
- **No connected-toys tooling.** Standard 14 is inapplicable.
- **No engagement-mechanic redesign. (a)** Standard 5's operative list — reward loops, autoplay, continuous scroll, inactivity notifications, streaks — describes features [#4](https://github.com/mandyMooreFan/docket/issues/4) and [#6](https://github.com/mandyMooreFan/docket/issues/6) refused on product grounds. Nothing to remove. **Pause-and-save tools are a "consider", not a requirement**, and are aimed at games.
- **No design, transparency materials or nudges for anyone under 16. (a)** *"There is no requirement for you to design services for development stages that aren't likely to access your service."* Four of the code's five age bands are out of scope, and with them the cartoons, audio prompts, rule-based framing and parental-involvement machinery that make the code look expensive. **Record the decision; do not build for it.**
- **No consultation with children and parents**, provided the decision not to consult is **recorded and justified** in the DPIA. **(a)**
- **No user testing** of the transparency materials, on the same recorded-justification basis. **(a)**
- **No obligation to publish the DPIA.** *"It is good practice"*, nothing more. **(a)**
- **No requirement to raise the age floor, and no requirement to exclude children.** The code never asks a service to shut children out; its purpose is stated as *"not by seeking to protect children from the digital world, but by protecting them within it."* **This is the opposite of the pressure the OSA applied in [#20](https://github.com/mandyMooreFan/docket/issues/20), and it settles a question #20 left slightly open: the code gives no reason to revisit 16.**
- **No self-service data export tool.** [`data-rights.md` §6](./data-rights.md)'s finding survives; Standard 15 raises prominence, not automation. **(a)**
- **No new lawful basis.** Annex C confirms **contract** or **legitimate interests** are available for core processing and that *"Consent is unlikely to be the most appropriate basis for processing which is necessary to deliver the core service."* Docket does not need a consent flow. **(b)**
- **Nothing that contradicts the map's refusals.** As with the DSA in [`statutory-duties.md` §7](./statutory-duties.md), **every place the code is most demanding is a place Docket already declined to build.**

---

## 15. What I could not verify

1. **⚠️ Whether the ICO's open-letter position on stated minimum ages reaches a 16 threshold, a non-social-media service, or a controller not relying on consent.** §4.2's four counter-arguments are **mine, not the ICO's** — no source draws the 13-versus-16 distinction, and the ICO has not reconciled the open letter with its own 2024 Opinion. **This is the most important unresolved point in the document. (c) — LAWYER.**
2. **Whether a deliberately-published professional resume is treated differently from ordinary social data.** §6.1 finds nothing in the code, its annexes, the 2023 guidance's eleven case studies, or the strategy updates that addresses professional or vocational services. The core-service argument is mine, not the ICO's. **(c)**
3. **Whether a design that makes a child's age band externally inferable is itself a data protection concern** (§5.5). No source found either way. **(c)**
4. **Whether Standard 8's "separate choices over which elements they wish to activate" reaches inside a single deliberate act of publication** such as [#5](https://github.com/mandyMooreFan/docket/issues/5)'s Profile-as-Application. **(c)**
5. **Whether Docket logs IP addresses**, which would bring device-derived location into §10's analysis and onto a practice the ICO has named. A question for [#11](https://github.com/mandyMooreFan/docket/issues/11) and the stack, not for this document.
6. **Docket's lawful bases remain undetermined.** [`data-rights.md` §8.6](./data-rights.md) flagged this; it now blocks the DPIA as well as the Article 20 analysis — and §4.2's counter-argument (2) depends on it. **This is Docket's own determination to make and record, and it is now on the critical path.**
7. **No ICO enforcement action or statement addressing a service of Docket's size under this code could be found**, and none of the eleven published case studies is a professional network. §12's posture reading is drawn from the code's enforcement section, the consultation response, and a fine trend line, not from a comparable precedent. **(c)**
8. **Whether the 16–17 recommendation to "provide full information in a format suitable for parents" is genuinely expected of a service with no parental relationship at all**, or is drafting carried down from the younger bands. The code states it without qualification. **(c)**
9. **The status of the Commissioner's Opinion.** It carries the same live banner `data-rights.md` §8.4 recorded on other ICO pages — *"Due to changes made by the Data (Use and Access) Act, this guidance is under review and may be subject to change"* — and §7.1 says the ICO *"intends to replace this opinion with guidance on age assurance in due course."* **The code itself carries no such banner**, as expected for a statutory instrument laid before Parliament under s.123. **So the least stable text is the one §4.2 turns on. (a)** on the banner, **(c)** on what replaces it.
10. **Commencement of DUAA s.81 is cited to S.I. 2026/82 reg. 2(k) via the ICO's own dated guidance revision, not read against the SI directly.** **(b)**

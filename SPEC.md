# Docket v1 — specification

This document is the complete design for Docket v1. It is written so that a build effort can start
from it without reopening a decision. Where a decision looks arbitrary, the reasoning is given — not
as history, but because the reasoning is usually the thing that stops an implementer from
"improving" a rule into something that breaks a guarantee elsewhere.

**Status: specified, and being built.** Every decision here is settled, and the v1 build is under
way on its own map, [Map: the v1 build](https://github.com/mandyMooreFan/docket/issues/26) — that
map's Decisions-so-far is the record of what has landed on `main`, and where the build settled a
detail this document had left open, this document now says so in place. The
[wayfinder map](https://github.com/mandyMooreFan/docket/issues/1) that produced this spec is
indexed in §17, and each section names the tickets whose resolutions it folds in — the ticket holds
the full argument, this document holds the design. The language used throughout is
[`CONTEXT.md`](./CONTEXT.md)'s, and the two ADRs in [`docs/adr/`](./docs/adr/) are part of this
spec.

**§15 is the one to read before writing any code that touches a member under 18, an export, a
report, or a takedown clock** — the compliance ledger, including the items that are due *before*
launch.

---

## 1. What this is

An open-source professional network — **LinkedIn parity across four pillars, minus the gimmicks**:

> **Profiles + Connections · Feed + Posts · Jobs · Messaging**

It exists because LinkedIn's UI is terrible and charging people to find a job is wrong. Every
feature earns its place by serving the professional use, never an engagement metric. The product is
named **Docket** — a docket is a list of matters awaiting resolution, which is exactly the jobs
board's outcome guarantee (§6.4) — and lives at **`docket.social`** (unregistered as of the last
check; verify before launch). Companies exist only as a **minimal entity**: name, logo, description,
their postings, their people (§6.1).

### 1.1 Constraints that are not negotiable

1. **Everything is free.** No ads, no premium tiers, no paid postings, no donations. Sustainability
   is a future effort, not a v1 design input.
2. **One canonical hosted instance.** Open source is about trust and contributions, not
   self-hosting; federation does not exist and no design decision may anticipate it.
3. **NYT-simple.** The product is a reading experience, not an app that performs at you. "No
   gimmicks" is a standing test: no reactions, likes, shares, view counts, streaks, badges, or
   ranking of any kind. The domain vocabulary deliberately has **no word for reach, ranking, score
   or engagement** — a glossary that cannot say a thing keeps it out.
4. **The model stores facts, never conclusions**
   ([ADR-0002](./docs/adr/0002-derived-not-stored.md)). Capability, effective visibility and
   authorisation-to-write are always derived at read time from stored facts (Completeness, Work
   verifications, moderation history, the age facts of §9). No boolean that a rule could compute is
   ever persisted — except where a fact must outlive its inputs, which is exactly why
   *authored-as-minor* is stored (§9.4).
5. **Docket never judges what members say by machine.** Moderation is reactive and human (§10). The
   only automated actions in the product are two hash checks on image upload (§10.4) — neither is a
   judgement about speech.

---

## 2. The design language

*From [Pin the design language](https://github.com/mandyMooreFan/docket/issues/3); the pinned
prototype (pass A3) is the visual record:
<https://claude.ai/code/artifact/c77f740d-a08e-4f92-97a6-556265c77748>.*

**The rule the language turns on: serif for anything a person wrote; sans for anything the app
said.** Names, headlines, summaries, Post bodies, Recommendations, job titles and role descriptions
are serif. Navigation, labels, timestamps, counts, buttons and form controls are sans. This is what
makes Docket read as a publication rather than a dashboard, and it is the test to apply to every new
element.

### 2.1 Type

- **Faces:** Source Serif 4 (400/600/700) and Libre Franklin (400/500/600/700), both open-licensed.
  The wordmark "Docket" is Source Serif 700.
- **Scale (px, non-linear):** 12 · 13 · 14 · 16 · 18 · 20 · 24 · 30 · 36 · 44. Nothing off the
  scale.
- **Reading size is 18px serif at 1.7 line-height** (Post bodies, summaries, Recommendations);
  supporting prose 16px. Profile name 44px serif 600 at −0.025em. Long-form Posts may carry a 24px
  serif kicker. Uppercase section labels: 12px sans 700 at 0.12em tracking in the palest ink.

### 2.2 Colour

Four-step ink ramp, not ink-plus-grey. Light: `#101418` → `#333B45` → `#5D6775` → `#8A94A2`;
grounds `#FFFFFF` / `#F5F7F9` / `#EAEEF2`. **One accent, `#1F5C99`** — links, the primary button,
the open-to-work pill, nothing else. **Dark theme ships in v1**: ink `#F1F4F8` → `#C6CDD7` →
`#949EAB` → `#6E7887`; grounds `#0D1117` / `#161B22` / `#1E252E`; accent `#7CB2E8`.

### 2.3 Space and structure

- **Spacing scale (px):** 4 · 8 · 12 · 16 · 24 · 32 · 48 · 64 · 96. More space between groups than
  within them: 8 inside an entry, 32 between entries, 64 between sections.
- **Near-zero borders.** One hairline in the whole UI — under the app bar, as a shadow, not a
  stroke. Space, background fill and shadow divide; never a 1px rule. Radii 8px on controls, 12px
  on panels and the portrait; two soft shadow levels only.
- **Hierarchy moves three dials at once** — size, weight, colour. Nothing is de-emphasised by size
  alone.
- **Layout:** sticky 60px app bar (wordmark, four nav items, search, avatar). Profile is a single
  42rem column. Feed is a 1fr main column with an 18rem sticky rail (jobs from your network,
  pending Connection requests). At phone width the rail's contents are **promoted to nav
  destinations, not stacked** (§14.1).
- The tokens above are the spec; no CSS framework is chosen or implied.

**Settled at build time (was this spec's one open visual question):** photo treatment — real
photos render **plain**, never greyscaled. Company logos and Post images already shipped
untouched, so a filter would have singled out faces alone, which is a house style imposed on how
people choose to present themselves. Initials on a neutral fill remain the first-class fallback
for a member with no photo. Decided in
[Build the member photo](https://github.com/mandyMooreFan/docket/issues/52).

---

## 3. Identity and signup

*From [Decide identity and signup](https://github.com/mandyMooreFan/docket/issues/7), as amended by
[the age-gate resolution](https://github.com/mandyMooreFan/docket/issues/23). Principle: anyone can
join; a fresh account can't do anything to anyone until it looks like a person.*

### 3.1 The signup flow, in order

1. **The age ask — first screen, before any email is collected.** A neutral question asking
   **month + year of birth**, every answer available. Never "confirm you are 16+" — the pattern
   where a 14-year-old is offered one box and a refusal is the exact nudge the ICO Children's code
   names as prohibited. Under-16 → refused; **nothing is stored server-side about a refusal** — the
   refused person has handed Docket nothing. Immediate resubmission is **blocked device-locally for
   ~24 hours** (a cookie). Evadable by anyone determined; that residual risk is what the DPIA's
   minimal-risk argument (§15.2) covers.
2. **Email.** Magic link only — **no passwords, ever**. The address *is* the account and the inbox
   *is* the recovery path; there is no reset flow, and losing inbox access loses the account. Login
   and verification are the same mechanism.
3. **Profile-building at the member's own pace** — capability arrives with Completeness (§3.2).

What is retained from the age ask: an adult's birth month/year **evaporates at the signup screen**
— only "adult, declared on \<date\>" is stored. A 16–17-year-old's month/year is kept solely to lift
the §9 protections automatically at 18, then deleted (§9.3). Docket's steady state holds **no birth
date for any adult member**.

### 3.2 Capability by Completeness

- **The bar:** verified email + name + headline + at least one Position or education entry. Minutes
  for a person; a real per-account cost for a bulk registrar. Deliberately *not* a photo
  (exclusionary; generated faces are free), *not* a summary (the field people stall on), *not* an
  accepted Connection (an invite gate through the back door).
- **Never gated (consumption):** browsing public Profiles, the jobs board, reading the feed,
  editing your own Profile. A lurker doing job research is a legitimate user.
- **Withheld until complete:** Connection requests, messaging, posting, posting a job. Applying
  gates itself — the Profile *is* the Application (§6.3), so the apply button simply reports the
  same check.
- **Incomplete Profiles are members-only and un-indexed regardless of the Dial** — mass
  registration has no payoff because the pages do not exist to Google until a human has done a
  human's worth of typing.

### 3.3 Names, badges, sessions, doors

- **The name is the one you are professionally known by** — the resume standard, not the ID
  standard. No ID upload, no real-name algorithm, no validation beyond non-empty. Pseudonymity is
  possible and not hunted; **impersonating a specific real person is the only name offence**,
  handled reactively (§10).
- **No verification badges.** A badge sorts members into two tiers — the trust need is real but
  narrower, and it lives on the *posting*, not the person (§6.2).
- **90-day sliding sessions**, a visible session list in settings, one-tap sign-out-everywhere.
- **No CAPTCHA** (an accessibility tax; the good ones put a corporation in the login path).
  **Rate-limited link requests** per address and per IP — anti-abuse for the mailer as much as
  anti-spam. **Public-inbox throwaway domains blocked as a security rule** (a world-readable
  mailbox is an account anyone can open); **private aliasing services allowed** (Hide My Email,
  SimpleLogin, Fastmail aliases).
- **Floor: 16+**, self-declared per §3.1, kept and defended as minimal-risk processing — the full
  posture is §9.1 and §15.2. **One account per person: stated as policy, unenforced** — like the
  name rule, Docket has deliberately built no way to detect it.

**Load-bearing consequence:** transactional email is the *entire* authentication system. Mail
deliverability is a launch-blocking dependency (§14.2), even though the product itself sends no
notification email (§7.4).

---

## 4. The Profile and the graph

*From [Spec the profile and the connection graph](https://github.com/mandyMooreFan/docket/issues/2).*

### 4.1 The Profile

A public resume page on the open web — being findable is the point. Contents: name, headline,
location, photo, summary, Positions (each linked to a Company where one exists), education,
**Skills as a self-declared plain list** (no endorsements — LinkedIn's emptiest gimmick), a **quiet
open-to-work flag** (member-controlled, shown at the member's chosen audience, **defaulting to
off**, never searchable, never indexed — §8.1), and approved **Recommendations**.

**Visibility: public by default, one Dial** — public → members-only → connections-only. No
per-section matrix; that labyrinth is the UI this product exists to escape. The Dial has exactly
three **service-imposed floors**, all the same shape (a whole-profile floor, never a per-section
setting): incomplete Profiles (§3.2) and under-18 Profiles (§9.2) are members-only and un-indexed
regardless of the Dial; under-18s are additionally absent from people search.

### 4.2 Connections

- **Mutual only.** A request accepted by both sides; no follow edge — follow exists to serve an
  influencer economy this product refuses. A Connection means *we know each other professionally*.
- **Requests** carry an optional short note (the product's only message request — §7.1). **Decline
  is silent** — the sender never hears back — and repeat requests after a decline are blocked.
  Adults cannot send a Connection request to an under-18; the under-18 may send one (§9.2).
- **Display:** a Profile shows Mutuals and a connection count. No 1st/2nd/3rd badges, no
  path-to-person. A member's full connection list is visible **to their Connections only**.
- **Disconnect** is quiet and reversible; **Block** is total and durable, both directions (§7.3).

### 4.3 Recommendations

Only a Connection may write one; it displays only after the subject approves; the subject can hide
it later. No reciprocal prompts, no "ask for a recommendation" flow — requesting one is a plain
Message.

---

## 5. The feed

*From [Design the feed without gimmicks](https://github.com/mandyMooreFan/docket/issues/4). Purpose:
weak-tie maintenance, not discovery — success is "I heard that Tom left Skyscanner", not "I read
something interesting".*

### 5.1 Distribution and ordering

**Distribution is the mutual graph and nothing else.** Nothing from a stranger can ever reach your
feed — no topic subscription, no interest graph, no suggested source. **Reverse-chronological, no
ranking, and the feed ends**: everything since your last visit, newest first, then a hard "You're
caught up" boundary (per-member read position is tracked state). Nothing is re-surfaced, promoted
or shown twice; no infinite scroll — older Posts are reached from the author's Profile.

### 5.2 Post types — exactly three

1. **Written Posts**: long-form plain text with a generous ceiling (a few thousand words), links,
   still images. **No video, no audio** — video is where watch-time optimisation lives and its
   hosting cost breaks a free product. Link previews are a title and a domain, never a large
   auto-fetched card.
2. **Job-attached Posts**: a member writes a Post and attaches a role from the jobs board — the
   *only* path from board to feed; nothing auto-syndicates.
3. **Opt-in work changes**: generated when a member edits their Profile, only if they tick a box at
   that moment. Never automatic, never retroactive, only genuine news (started a role, left a
   role, published something).

### 5.3 Interactions and numbers

**Replies and a private Save — the complete list.** Replying costs a sentence, which is the filter.
**Replies are limited to the author's Connections**, even on a Post a stranger can read; the author
may remove any Reply from their thread and may close the thread. **The reply count is the only
visible number in the feed** — navigation, not a scoreboard. No analytics of any kind, not even
privately to the author.

### 5.4 Where a Post lives, and who can see it

On the author's Profile as a dated list, riding the Profile's single Dial — no per-Post visibility.
A public Profile means Posts readable and indexable on the open web: **writing on Docket is public
writing**, and public Posts are the front door for strangers (§13). **Exception: nothing authored
by an under-18 is ever visible logged-out or indexed** (§9.4) — their Posts are members-only
regardless of the Dial, their Replies are omitted from logged-out rendering with no placeholder,
and the logged-out reply count counts only what that view shows.

### 5.5 Notifications

Replies to you — to your Post, or to your Reply in a thread you joined. That is the entire list.
The feed never comes to get you.

### 5.6 Refused, explicitly

Named so a future contributor has to argue against a decision rather than fill a gap:
algorithmic ranking · infinite scroll · automatic profile broadcasts (work anniversaries, added
skills, new-connection announcements, photo changes, "congratulate X") · reactions · likes ·
reposts · quote-posts · shares · view counts · impression counts · save counts · trending topics ·
suggested posts · suggested people · "people also viewed" · unread badges and dots (§7.4's inbox
count is the one exception, earned by a person writing to you) · digest email · re-engagement email
· autoplay media · creator analytics.

---

## 6. Jobs and Companies

*From [Spec the jobs board](https://github.com/mandyMooreFan/docket/issues/5) and
[Decide how company entities are created and trusted](https://github.com/mandyMooreFan/docket/issues/14).
Free on both sides means the board can afford to be demanding; it spends that leverage on the two
things that make job hunting miserable — pay secrecy and silence.*

### 6.1 Companies

- **Creation is a byproduct of a claim about yourself**: any member creates a Company by naming it
  while adding a Position — autocomplete-first, so an existing employer is reused rather than
  forked; no queue, no gatekeeper. Accepted cost: joke/spam entities get created; cleanup is
  moderation's (§10.5).
- **A Company is never an actor**: no account, no admin hierarchy, no owner, nobody speaks for it.
  **A Company never claims itself — the page belongs to the people who work there.** Anyone holding
  a Work verification at its domain may edit its name, logo and description, all equally. Company
  pages keep an **edit history** (vandalism by a verified employee is a real, accepted surface —
  §10.5).
- **The Verified domain set is the identity key** — derived from Work verifications, never
  declared: you cannot claim a domain, only demonstrate you receive mail at one. Entities sharing a
  verified domain are the same Company and **auto-merge**; domain-less companies merge manually on
  report; a Company may hold several domains. Merges are privileged, audited, reversible (§10.5).

### 6.2 The trust gate

**A Work verification — a magic link to an address at the Company's domain, the same machinery as
login — gates the two capabilities that can do harm: posting a Job under that Company, and editing
its page.** It is not a badge, not an account, not a hierarchy; it decorates nothing and unlocks
capability. The verified address is a *second* address, not the login address (§3.3's alias rules
are untouched). A Work verification is a **dated fact that never lapses** — it records a moment.

The gate exists because two settled decisions compose into an attack: posting rights derive from a
self-declared Position, and applying hands the poster a **full Profile regardless of the Dial**.
Without the gate, a fake employer harvests private profiles, and the harm is complete at the moment
of application — reporting after the fact is no remedy.

**Accepted cost with real reach:** an employer with no controllable mail domain — the self-employed,
one-person consultancies, firms on generic mail — cannot have jobs posted for it in v1. At launch,
the posting-capable set is the subset of members holding a work address at a domain they can
receive mail on (§13.2 plans against the real number).

### 6.3 The posting and the Application

- **A posting is always authored by a person**, attaching to a Company that is a *current* Position
  on the author's own Profile plus a Work verification at its domain. Accountability is a named
  human with a Profile and a graph. A posting carries: title, Company, location and remote policy,
  description, the posting member — and a **mandatory real salary range and currency**. No
  "competitive", no "DOE", no single number, no £30k–£300k band. Shown at the top of the posting
  and in every list. Some companies will not post under this rule; that is the trade.
- A posting runs a **fixed window, then closes automatically**.
- **The Profile is the Application**: one click sends it plus an optional short note. No CV upload,
  no forms, no third-party ATS. **Applying grants the poster a full view of the Profile for that
  Application, whatever the Dial** — the one deliberate exception to the single Dial, consent given
  by the act of applying. The poster also sees Mutuals — how a referral has always worked. Nothing
  lets a poster contact members who did not apply.

### 6.4 The closure guarantee

The heart of the board, possible only because the Application lives here:

- The poster works a queue; each Application is marked **advanced** or **not selected** (the
  Outcome).
- Anything untouched when the posting closes is automatically **"closed without response"**, and
  the applicant is told.
- **A poster with an unresolved queue cannot open a new posting.** The obligation follows the
  person, not the posting.
- The applicant can always see their Application's state.

*Threshold settled at build time:* **one is enough.** A poster cannot open a new posting while any
Application on any of their postings closed without response and still lacks an Outcome. The
obligation is owed to each applicant singly, the remedy is two clicks each, and any threshold N
licenses ignoring N−1 people per posting. While a posting is open its window is the deadline, so
neglect only crystallises at close. Decided in
[Build the jobs board](https://github.com/mandyMooreFan/docket/issues/35).

### 6.5 Browsing and email

**One list, newest first, no ranking, no personalisation.** Seeker-chosen filters: keyword,
location and remote policy, **salary floor** (mandatory salary is what makes this filter work),
company, and **"roles where I know someone"** — a fact about the graph, not a prediction about the
person. No matching, no match scores, no "recommended for you", no promoted postings.

**Email: opt-in saved searches only** — off by default, seeker-chosen frequency, one click to stop,
contents limited to matching postings — plus the transactional mail the closure guarantee requires
(application received; application closed). This is the deliberate exception to the product's
no-email rule, because postings are time-limited and an employed seeker cannot check daily. No
"jobs you might like", no "X people applied", no re-engagement mail, ever.

---

## 7. Messaging

*From [Spec messaging](https://github.com/mandyMooreFan/docket/issues/6), as amended by
[Decide the minimum age](https://github.com/mandyMooreFan/docket/issues/20). One Thread per pair of
Members, correspondence rather than chat, and the inbox never emails you. LinkedIn's inbox is
ruined by one decision — letting strangers pay to reach you; money is out of scope, so the gate is
the graph.*

### 7.1 Who can write to whom

**Writing to a Thread is authorised by a Connection or by an open Application**
([ADR-0001](./docs/adr/0001-one-thread-per-pair.md)) — one concept covering both. The job poster's
channel is scoped to that Application and never reaches non-applicants. The Connection request's
optional note **is** the product's only message request — one gate, not two; decline stays silent
with repeats blocked. Nothing else opens a channel: no InMail, no paid reach, no requests queue.
**One asymmetric exception (§9.2): an adult may not send a Connection request to an under-18; the
under-18 may send one**, and a poster's reply to an under-18 applicant works normally.

### 7.2 Thread shape and delivery

**One-to-one, always; exactly one permanent Thread per pair, ever.** No groups, no adding or
leaving, no thread names. The inbox is a list of people. **Polite near-realtime**: Messages arrive
when you open a Thread and refresh quietly while it is open — **no persistent socket per user**, no
realtime infrastructure; ordinary request/response with polling. **Nothing that reports on you**:
no typing indicators, read receipts, last-seen, or presence. A reply that takes a day is a normal
reply.

A Message is **text, links, and still images** — no file attachments, no CV uploads (the Profile is
the CV). Images share the feed's upload pipeline, including its hash checks (§10.4).

### 7.3 Ending

- **Disconnect closes the Thread to new Messages; both sides keep the history** — correspondence
  you had is yours, and neither person can destroy the other's record (§11.1 extends this).
  Reconnecting reopens the same Thread. Quiet, reversible.
- **Block is separate and total**: no Messages either direction, no request possible either way,
  Profile hidden from them. Deliberate, durable.

### 7.4 Notifications

**An in-app Unread count on the inbox, and nothing else. No email at all** — not even a first
message from a new Connection. The Unread count is **the only badge in the product**, earned by a
person writing to you personally, not by content existing. Accepted cost, weighed deliberately: a
job poster's reply can sit unseen for a week in a weekly-checking seeker's inbox — the jobs-side
transactional mail (§6.5) survives, the poster's actual Message arrives only in the inbox.

---

## 8. Search and discovery

*From [Decide search and discovery](https://github.com/mandyMooreFan/docket/issues/15). One box in
the app bar returning people, companies, posts and jobs, with two rules about who may see what.
Backend: **Postgres full-text search, no dedicated search service** — no personalisation to
compute, no attribute faceting, one instance, a hard cost constraint; a separate index is a second
datastore for a workload Postgres handles at this scale.*

### 8.1 People: identity, never attributes

**You search a person by name. That is the whole of it** — no filtering by skill, location,
employer, seniority or availability. Outreach is already dead (messaging is connections-only,
posters cannot contact non-applicants), so attribute filtering is the surviving half of a sourcing
product, and it is the half that does the work. **Open-to-work is never a search axis and never
appears in the index.** Accepted cost: you cannot find "a designer near me" — the loss that keeps
this a directory rather than a sourcing tool. **Under-18 Profiles are never returned** (§9.2).

### 8.2 Ordering: relevance, but impersonal

**The same query returns the same results in the same order for everyone** — textual match
quality, never a function of who is asking. **Mutuals are displayed on results but never reorder
them**; to narrow by the graph you **tick a filter** ("only people I share a connection with") —
a fact about the graph chosen by the seeker, the same mechanism as jobs' "roles where I know
someone", never a weight applied on your behalf.

### 8.3 Finding someone you know: nothing new is built

Name search, a Connection's connection list, and a Company's people list already cover the honest
cases. Rejected by name: **exact-email lookup** (a membership oracle), **address-book import** (the
archetypal privacy trespass and the growth gimmick this product exists against), suggestion
engines. The *first* connection is cold start's problem (§13), and search deliberately does not
solve it.

### 8.4 The open web

- **Jobs, companies and Posts are searchable logged-out** — the lurker doing job research is a
  legitimate user, and requiring an account to look for work would betray the board's founding
  rule. **Under-18-authored content is excluded** (§9.4).
- **People search requires an account**, so bulk enumeration of the membership stops being free —
  the single highest-value anti-scraping rule available. Google still indexes public Profiles; the
  front door is intact, the bulk-query door is closed. **A Company's people list is account-gated
  for the same reason** — logged-out, a Company page shows name, logo, description and postings
  only.

### 8.5 Derived data never exceeds the Dial

The Dial is honoured on every surface — results, people lists, connection lists, the index. Company
people-lists are derived from published Positions. Mutuals are computed per-viewer and never appear
on an indexed or logged-out page. Incomplete and under-18 Profiles stay un-indexed regardless of
the Dial. **No enumeration surface exists beyond these** — nothing returns a set of people defined
by their properties rather than their name.

---

## 9. Members under 18

*The whole minors regime in one place, from
[Decide the minimum age](https://github.com/mandyMooreFan/docket/issues/20),
[the age-gate resolution](https://github.com/mandyMooreFan/docket/issues/23) and
[the under-18 Posts resolution](https://github.com/mandyMooreFan/docket/issues/24), on the research
in [`docs/statutory-duties.md`](./docs/statutory-duties.md) and
[`docs/childrens-code.md`](./docs/childrens-code.md). The posture in one sentence: **the floor is
16, Docket accepts it is likely to be accessed by children and designs for it, and every
protection is derived from a stored age fact held in its minimal form.***

### 9.1 The floor, and why it is 16

**16+, self-declared through the neutral ask of §3.1.** Raising it buys no regulatory relief — a
bare 18+ term is not age assurance at all (OSA s.230(4)), the child-attracting exposure is driven
by *what Docket is* (Ofcom names careers content, profiles, connections and DMs), and a stricter
label would put lying minors on the site with **none** of the protections below. Examined and
rejected twice, on different attacks. The floor exists to protect the apprentice and school-leaver
who genuinely is job-hunting; the stated 16 is defended against the ICO's lawful-basis position as
minimal-risk processing, in the DPIA, in as many words (§15.2). Age is never *verified* — these
are proportionate protections, not age assurance, and nothing here claims otherwise.

### 9.2 What is different for an under-18 Member

- **Profile: un-indexed and members-only regardless of the Dial**, and never returned in people
  search — the same service-imposed floor shape as incomplete profiles.
- **An adult cannot send them a Connection request; they may send one to anyone.** The asymmetry
  is the point: the young person keeps their agency and their access to work — jobs, applying, and
  a poster's reply all work normally — while adults lose the ability to find and approach them.
  (This answers OSA s.11(6)(e)'s two named functionalities: adults *searching for* and
  *contacting* children.)
- **Open-to-work defaults off for everyone** (§4.1), which for under-18s is also the Children's
  code Standard 7 answer.
- **Accepted cost, restated in the DPIA in data-protection terms (§15.2):** under-18 status
  becomes inferable from behaviour — a profile absent from search that cannot be sent a request.
  The alternatives were worse.

### 9.3 The age fact, in its minimal form

- Signup's neutral ask collects month + year of birth (§3.1). **A declared adult's evaporates at
  the signup screen** — only "adult, declared on \<date\>" is stored.
- **A 16–17-year-old's month/year is kept for exactly one purpose** — lifting the protections
  automatically at the end of the birth month they turn 18 — and is deleted at that rollover,
  collapsing to the same adult fact.
- **Steady state: Docket holds no birth date for any adult member.** A refused under-16 leaves no
  record at all.

### 9.4 Nothing authored by an under-18 is ever visible logged-out or indexed

- Their **Posts are members-only regardless of the Dial**; their **Replies inherit the author's
  protection, not the Post's audience** — a logged-out view of a public Post omits them with no
  placeholder, and its reply count counts only what that view shows.
- **The 18 rollover never lifts this cap.** Content authored as a minor stays members-only
  permanently; **delete-and-repost as an adult is the only release**. Auto-lifting would publish a
  child's writing to the open web in one silent moment with no intervention by the individual —
  UK GDPR Article 25(2)'s exact wording, and Recital 65's exact worry.
- **Mechanic:** every Post and Reply stores an immutable **authored-as-minor** fact at creation
  (true when the author's declared band is 16–17). It must be fixed at write time because §9.3
  deletes the birth data the derivation would need. Never displayed; exists only to drive derived
  visibility.

### 9.5 Two compliance-shaped consequences

- The conduct policy's prohibition of the four primary priority categories **for all users**
  (§10.6) is what disapplies the OSA's mandatory age-assurance trigger (s.12(5)) — "for all users"
  is load-bearing, and it works category by category.
- The **children's access assessment and children's risk assessment** are due within three months
  of first day of operation; the **DPIA is due before launch** (§15.1–15.2).

---

## 10. Moderation, reporting and safety

*From [Spec moderation, reporting, and abuse response](https://github.com/mandyMooreFan/docket/issues/16)
and [Decide how the 48-hour intimate-image takedown duty is met](https://github.com/mandyMooreFan/docket/issues/21).
One person reviews reports in a queue; responses withdraw the capability that was abused; everything
is reactive except two hash checks and one auto-hide; every action is explained, appealable once,
and counted in a published log.*

### 10.1 Who reviews, honestly stated

**The owner, working a reactive queue, decision and reason recorded.** The product **states the
real response expectation in plain words** — reviewed by one person, usually within a named period
— rather than implying a staffed desk. Accepted cost: one person, one timezone; appointing
moderators later is a change to this decision, made when there are people to appoint. Member
self-help is the first line and is specced elsewhere: authors remove/close Replies (§5.3), Block is
total (§7.3), declined requests stay declined (§4.2), a poster with an unresolved queue cannot post
(§6.4).

### 10.2 Reports

**Reportable:** Profiles, Posts, Replies, Messages in a Thread you are part of, Job postings,
Companies and their pages. **Not reportable:** anything inside a Thread you are not part of —
private is private by construction. **Members report in-product; non-members have a published
contact address** for illegal content seen logged-out — plus the distinct intimate-image route of
§10.4. Report acknowledgement and the statement-of-reasons content carry the statutory fields
(§15.3).

### 10.3 The ladder: withdraw what was abused

1. **Remove the item.**
2. **Withdraw the specific Capability that was abused** — job posting, Connection requests,
   Replies, messaging — for a stated period or indefinitely. Proportionality is the point:
   someone abusing job postings does not thereby lose their correspondence.
3. **Suspension** — read-only; the member can still sign in.
4. **Termination.**

This inverts §3.2's earned-capability model rather than inventing a second system. **A capability
never earned and a capability withdrawn are different states, and the member is told which they are
in.** **Visibility limiting and shadowbanning are refused outright** — covert reach reduction is a
lie told to a member about their own account, and there is no ranking dial to turn down anyway.
Every action states what was done and why; **one Appeal**, described as what it is — the same
person reconsidering with new information; a **periodic public transparency log** (reports
received and actioned, by category, no names).

**Company surfaces** (from §6): joke/spam entities are cleaned up reactively; page vandalism is
answerable from edit history; **merges are privileged, audited, reversible destructive actions on
other people's employment history**. **Scraping** is answered by rate limits on search (per
account; per IP logged-out) — a system control applied to everyone, not moderation, so it never
enters the queue or the ladder.

### 10.4 Automation: two hash checks, one auto-hide, and nothing else

**Everything is reported by a human and judged by a human** — no keyword filters, no ML
classifiers, no link scanning, no automated account actions, applied to feed and private threads
alike. The complete list of automation:

1. **CSAM hash-matching on every image upload, before storage.** Named and bounded; a third-party
   dependency in the upload path is the accepted cost.
2. **A local blocklist of hashes of images taken down under s.20A**, checked on the same upload
   path. More than the law requires; it means the person depicted reports once, not every time.
   Note: "substantially the same" implies **perceptual** hashing — a real step up from exact
   hashing, and not free.
3. **Auto-hide on an intimate-image report** (§10.5) — a content-level, reversible, pre-decision
   hold, outside the four-rung ladder, implying no finding against the uploader.

None of these is a judgement about what members say.

### 10.5 The intimate-image route (OSA s.20A — statutory, 48-hour clock)

- **A report hides the content at once; a human confirms or restores later on the ordinary queue.**
  The 48-hour statutory deadline is met **structurally**, not by someone being awake. This is not
  shadowbanning: one item, visibly removed, **disclosed to both parties**, reversible. Takedown on
  accusation is the accepted cost, bounded by s.20A(2)'s required declarations, the narrow
  category, reversibility, and false reports being reportable conduct.
- **A public form, no account required** — s.20A covers "users *and affected persons*", and the
  person depicted is precisely the one least likely to hold an account. It carries the prescribed
  contents (subject-or-acting-for declaration, good-faith statement, enough information to locate
  the content — accepting an imprecise location, since a non-member cannot see a private thread —
  and contact details). Rate-limited per address and per IP. A **distinct route**, not the general
  report flow; the 48-hour clock is a **stated operational property**.

⚠️ **A tension the build surfaced, unresolved here:** the two commitments above — that the hide is
*structural*, and that the location may be *imprecise* — cannot both hold for a report whose
locator the product cannot resolve, because there is then nothing to hide on receipt. The build
implements the defensible reading (resolvable locators held in the receiving transaction;
unresolvable ones accepted, prioritised, and the sender told plainly that nothing is hidden yet,
because guessing would take down an uninvolved member's content on nobody's accusation), and
Messages are the concrete gap, being exactly where imprecision is expected. Which way §10.5 should
finally read is argued in
[issue #59](https://github.com/mandyMooreFan/docket/issues/59) — **LAWYER** at the §15.6 gate
either way.

### 10.6 The member conduct policy

§15.3's terms duties require it stated per category; the Contributor Covenant governs contributors
to the repo, not members — this is the members' list, short and enumerated. **Everything not
enumerated is not an offence.**

A member may not:

1. Post or send **illegal content**.
2. **Impersonate a specific real person** (the only name offence — §3.3).
3. **Harass a specific member.**
4. Send **bulk or commercial spam**.
5. Post or send **pornographic content** — prohibited for all users.
6. Post or send **content encouraging, promoting or providing instructions for suicide,
   self-harm, or eating disorders** — prohibited for all users.

Items 5–6 cover the OSA's four primary priority categories, deliberately for **all** users — that
scope is what disapplies mandatory age assurance (s.12(5)), and the spec must state, per category,
that prevention is by blanket prohibition plus reactive removal on report (a proportionality
argument for a low-risk service; **LAWYER** at the §15.6 gate).

---

## 11. Leaving, and taking your data with you

*From [Decide what a Member may export and take with them](https://github.com/mandyMooreFan/docket/issues/19),
on the research in [`docs/data-rights.md`](./docs/data-rights.md). Principle: **authorship gives
you a copy, not a veto** — you may take everything you wrote, anywhere it sits; you may not unmake
someone else's record of an exchange you were both part of.*

### 11.1 The export

**Self-service, one button, one archive: JSON for portability plus readable pages** — the Member's
profile and writing as documents, not a developer artefact. A manual-on-request process would have
been lawful; the button is a product decision — for a product defined against a network that traps
you, the export is the promise made concrete (and it removes the operational risk of the manual
route's one-month statutory deadline).

- **Threads export whole**, both halves — WP242 (EDPB-endorsed) says interpersonal messaging
  records go to the subscriber *because* they also concern them. The archive carries a
  plain-language note: *other people's words are in here; they're yours to keep, not to reuse.*
  That note is the purpose limitation the guidance cares about, said readably — spec copy, not a
  UI detail.
- **One button covers the wider Article 15 right**, not just Article 20 — members don't know the
  difference and shouldn't have to. So the archive includes Recommendations written *about* you,
  the Outcomes on your Applications, and the required supplementary information (purposes,
  recipients, retention, source). Derived Capabilities and effective visibility fall outside
  Art. 20 as a category (they are never stored — ADR-0002).

### 11.2 Termination

Your Profile goes; anything that stood alone is unpublished. **Your side of a Thread stays,
attributed to a former member; Recommendations you wrote stay published.** Accepted cost, stated
plainly: you cannot fully disappear from Docket — the alternative is "delete my account" deleting a
colleague's correspondence into a monologue of holes. **Deletion offers the export first and never
requires it.** For content authored as a minor, deletion stays the easy exit the Children's code
expects (§9.4).

**The genuinely unresolved part, recorded honestly:** no authority squarely addresses erasing a
two-party Thread on account closure. Docket's position is an argument to defend (the history is
still necessary for the other party's access to their own correspondence); the fallback is
**de-identifying the attribution while keeping the text**. LAWYER at the §15.6 gate.

### 11.3 Complaints, backups, downstream

- **DUAA s.164A (DPA 2018): a data-protection complaints route** — an electronic complaint form,
  acknowledgement within 30 days, and a response. No small-operator exemption. Distinct from the
  moderation report flow and the intimate-image route; it is its own form.
- **Backups:** erased data is put "beyond use" until backups roll over, and the deletion copy says
  so plainly.
- **Downstream notification:** public Profiles are indexed, so erasure carries the Art. 17(2)/19
  duty to take reasonable steps to have links and copies removed elsewhere.
- Recorded because the instinct is strong and wrong: **there is no small-operator, non-profit or
  open-source exemption from any of this, and being free is expressly irrelevant.**

---

## 12. Licence and governance

*From [Pick the license and contribution basics](https://github.com/mandyMooreFan/docket/issues/12).
Enacted, not just decided: `LICENSE` is on the repo.*

- **AGPL-3.0-or-later.** Docket ships as a service: GPL's copyleft never triggers (no
  distribution) and MIT/Apache permit a closed hosted fork — the precise dynamic this product
  opposes. AGPL is the only licence whose obligation fires on a hosted network, and it makes the
  founding promise structural rather than stated. Accepted cost: employers who prohibit AGPL
  narrow the corporate-adjacent contributor pool.
- **DCO sign-off (`git commit -s`), deliberately no CLA.** Without a CLA, relicensing requires
  every contributor's permission — **the AGPL promise is irrevocable, including by the owner.**
- **Governance: BDFL, legitimised by the written record.** Significant changes are argued in an
  issue and resolved in writing before code. Every resolution names what was chosen, what was
  rejected, why, and which costs were knowingly accepted.
- **Name policy stated, not registered** (AGPL §7(e)): the code is AGPL; the name and the
  canonical instance are not licensed with it — fork freely, call it something else. With
  magic-link auth this is member safety, not branding: a lookalike instance is a phishing surface.
- **AGPL §13 is a UI requirement, not paperwork: every page must offer a durable, reachable
  "Source" link** to the code archive. §2's layout has no footer, so the build had to place it
  deliberately: it **sits in the app bar itself**, on every page, logged in or out — settled in
  [Build the design language base](https://github.com/mandyMooreFan/docket/issues/28).
- **Deferred to handoff with content already decided:** `CONTRIBUTING.md` follows linkpage's
  structure (status banner, who-this-is-for tie-breaker, rejection table — already written across
  the map's refusals, see §5.6 and §17.1 — and invariants); `CODE_OF_CONDUCT.md` is the
  Contributor Covenant (contributors, not members — members get §10.6); `SECURITY.md` uses GitHub
  private vulnerability reporting.

---

## 13. Cold start and launch

*From [Decide the cold-start plan](https://github.com/mandyMooreFan/docket/issues/17). At zero
connections Docket is **a public resume page** — the one supply-free pillar: a permanent,
well-designed professional page at a URL you control, better than the incumbent's and free. Every
other pillar needs somebody else to exist.*

### 13.1 The first cohort

**One dense community you can personally reach** — a single profession, city or industry where
people already overlap. Density beats volume, and growth is capacity-capped anyway (§10.1's one
moderator). ⚠️ **The named hazard: the repo's own developer audience is the default cohort if none
is chosen** — free, self-selecting, tolerant of a rough v1, and a network of only developers is a
niche forum whose culture sets early. Choosing a cohort is an active decision that has to beat the
default. The strongest argument for a **UK** cohort is §15.4: targeting the EU in v1 buys two
legal-representative obligations.

### 13.2 Launch supply realities

- **Job posting at launch is only possible for members holding a work address at a verifiable
  domain** (§6.2) — plan against the real number, not the theoretical one.
- With no Connections and no Applications, **no Thread is writable at all** on day one.
- The **Invite** (below) is the seeding mechanism; the first connection is otherwise unsolved by
  design (§8.3).

### 13.3 The Invite

An offer sent to an email address that belongs to no Member yet, carrying an optional note; if they
join, a Connection request from the inviter is already waiting. **Optional, never a gate** — signup
stays completely open. It reuses the Connection request; no quota (a quota throttles exactly the
members doing the seeding). It is a way to email a stranger, so it inherits the rate limits (per
sender and per address) and enters the report queue — and it is the **third outbound-mail source**
against the 100/day cap (§14.2), the one that scales with enthusiasm.

### 13.4 Nothing changes for the empty graph — the empty parts say what they are

The feed stays connections-only and is simply empty. No fallback to public posts — "while you have
zero connections" becomes "while your feed is thin", which is a relevance algorithm with extra
steps; the only stable place to stand is the original rule. **Empty-state copy is spec surface.**
Each zero state says what it is and points somewhere useful; the copy below is the v1 baseline
(editable in tone, not in honesty):

- **Feed:** *"Your feed shows what your connections write — you don't have any connections yet.
  It stays empty until you do; nothing gets put here for you. Find someone you know, or invite
  them."* → search, Invite, jobs board.
- **Jobs board (empty):** *"No open postings right now. Postings appear here the moment a member
  posts one — there's no backlog you can't see."* → saved search opt-in.
- **Rail / its nav destinations:** *"No jobs from your network yet"* · *"No pending
  requests."*
- **Search, no results:** *"Nobody by that name yet. People search matches names only — that's
  deliberate."*
- **Company page with one person:** the page renders normally; the people list says *"1 member
  works here."* No padding, no "suggested companies".
- **Inbox, nothing writable:** *"Messages open when you're connected to someone, or when someone
  applies to your posting. No connections yet — your inbox is waiting on the graph, not on you."*

---

## 14. Platform and stack

*From [Choose the platform target](https://github.com/mandyMooreFan/docket/issues/9) and
[Choose the stack](https://github.com/mandyMooreFan/docket/issues/10).*

### 14.1 Platform: responsive web only

One codebase; **no native apps, no PWA, no public API** (all out of scope — §17.1). The three
strongest reasons for native were already dead by prior decision: no push (§7.4), a feed that ends
(§5.1), and a login path deliberately cleared of corporations (§3.3). **No API tax**: v1 is plain
server-rendered with no JSON layer; if native ever happens it brings its own API and pays for it
then. At phone width: one fluid reading column; **the 18rem rail does not stack — its contents are
promoted to nav destinations**. Accepted cost: pending requests are less discoverable on a phone;
if that proves too quiet, the §7.4 precedent (a count earned by a person acting toward you) is the
shape of the answer, decided then. **Browser floor: modern evergreen, iOS Safari as the binding
constraint** — with no native app, mobile Safari is the entire iPhone experience.

### 14.2 Stack

```
Java + Spring Boot        single Maven module, com.mbeebe.docket
Thymeleaf + htmx          server-rendered; htmx polls where messaging needs it
JPA / Hibernate           open-in-view OFF, explicit view models
Postgres, pinned major    self-hosted in a container; Postgres FTS for search (§8)
Flyway                    versioned migrations
Resend over plain SMTP    JavaMailSender, no vendor SDK
```

- **Server-rendered, deliberately departing from the house Spring-API-plus-React pattern** (Balk):
  the product is documents, not an application — there is very little client state to manage, no
  realtime (polling is one htmx attribute), and removing the API contract removes the whole
  contract-drift bug class. One language lowers the contributor bar. Accepted cost: server-side
  templating is new ground for these projects; the composer and inline replies need the care a
  React component would have given free.
- **`spring.jpa.open-in-view: false`; the service layer hands templates fully-loaded view
  models** — the lazy-loading-in-templates footgun made structurally impossible. View models are
  hand-written; that is the price of the guarantee.
- **Mail is the whole auth system** (§3.3), so the provider must stay swappable: **plain SMTP via
  `JavaMailSender`** (`smtp.resend.com:587`), never the vendor SDK. Switching provider is
  configuration, not a rewrite. **Three outbound sources** — magic links (login), work-address
  verification (§6.2), Invites (§13.3) — share Resend's free tier (~3,000/month, **100/day**);
  **the daily cap is the paid-tier trigger**, a cost decision for whoever runs the instance, not a
  redesign.
- **Runtime shape** (the spec records shape, not procedure): containers on a single self-hosted
  VPS, self-hosted pinned Postgres, cloud-agnostic, no managed services, single process holding a
  connection pool. DNS/TLS/proxy/CI/backups/secrets/monitoring/deploy belong to a successor
  deployment effort (§17.1).
- **No npm, no JavaScript build.** CSS implements §2's tokens by hand; htmx is a single pinned
  vendored script.
- Version pinning is implementation detail: current LTS JVM and Spring Boot 4.x at build time;
  Postgres pinned by major (the unpinned-major `PGDATA` lesson is already paid for).
- One stack-adjacent surface from moderation: the CSAM hash-match (§10.4) is a **third-party
  dependency in the upload path** — the accepted dent in cloud-agnosticism.

---

## 15. The compliance ledger

*The duties the map found, consolidated. Sources:
[`docs/statutory-duties.md`](./docs/statutory-duties.md),
[`docs/data-rights.md`](./docs/data-rights.md),
[`docs/childrens-code.md`](./docs/childrens-code.md) — each with a per-claim confidence key and a
not-a-lawyer disclaimer that this section inherits. The pattern that earned this section: **every
legal research pass found a live duty the previous one missed** (s.20A; DUAA s.164A; DUAA s.81).
Treat this ledger as the floor, not the ceiling.*

### 15.1 UK Online Safety Act — applies regardless of size, no hobby exemption

- **Deadlines:** a new service gets **three months from first day of operation** for the illegal
  content risk assessment and both children's assessments (access + risk), run concurrently — the
  evidence written once. Docket is not late; it also must not launch and drift.
- **Children's posture:** likely-accessed-by-children is **accepted** (§9.1); the protections of
  §9.2/§9.4 are the s.12(2) proportionate mitigations, aimed at s.11(6)(e)'s named
  functionalities.
- **Terms duties (s.12(9)–(13)):** state, **per primary priority category separately**, how
  children are prevented from encountering it (blanket prohibition for all users + reactive
  removal — §10.6); state how the under-age measures operate; disclose the proactive technology in
  use (the two hash checks); apply it all consistently.
- **s.20A intimate-image route:** specified at §10.5 — the distinct public form, the structural
  48-hour clock, the local hash blocklist.
- The **14 ICU measures** binding all user-to-user services and the **13 PCU measures** binding
  Docket are enumerated in `statutory-duties.md` §0a C4a; ICU/PCU C1–C2's "content moderation
  function capable of swift review and takedown" is §10 of this spec.

### 15.2 The DPIA — mandatory, and due before launch

The earliest deadline the map owes and the largest document: complete **before** launch (ahead of
the OSA's three-month clock). It must describe the processing, assess the named risk list, explain
conformance to each of the Children's code's fifteen standards, and record the lawful bases. Its
core arguments are already decided and must appear in as many words:

1. **The age posture** (§9.1): keep the stated 16, self-declared, defended as minimal-risk
   processing. The skeleton: three of Standard 3's five risk factors are zero by refusal (no
   profiling, nothing following from profiling, no third-party sharing); the 2024 Commissioner's
   Opinion — the instrument with statutory standing — permits self-declaration for minimal-risk
   processing; the March 2026 open letter's "no lawful basis" claim is materially weaker at 16
   than at its worked example of 13 (a lying 14-year-old is still above the Article 8 line;
   Docket does not rely on consent; the named gate technologies are the ones the ICO's own
   Opinion warns against). The unreconciled tension between the letter and the Opinion is stated,
   not hidden.
2. **Why members-only + un-indexed + out-of-search + no-adult-contact *is* high privacy** for an
   under-18 (the ICO's own March 2025 alternatives list supports the configuration).
3. **The inferable-age cost restated in data-protection terms** (§9.2's accepted cost).
4. **The Application-→-Thread route** (an adult may write to an under-18 applicant — preserved
   deliberately, assessed and justified, §9.2).
5. **Profile-as-Application vs Standard 8's "separate choices"** (one deliberate act of
   publication; the one-artefact model is a considered refusal).
6. Recorded absences: no profiling (recorded, not assumed); the typed location field and why
   Standard 10 does not engage; the decision not to consult children/parents and not to user-test
   transparency materials, with justification.

**⚠️ Docket's lawful bases must be determined and documented** — contract / legitimate interests
per processing purpose. On the critical path: the DPIA, the Article 20 scope, and age-posture
argument (2) all depend on it.

### 15.3 Statutory surfaces the product itself must carry

- **Report acknowledgement and statement of reasons** (§10.2–10.3) carry the DSA Art. 16/17
  fields where they apply: acknowledgement of receipt, the facts and legal/terms basis for the
  decision, redress information. **Art. 18**: a criminal-offence notification duty on becoming
  aware of a threat to life or safety.
- **Terms (DSA Art. 14 / Children's code Standard 4):** plain-language terms and conduct policy,
  **pitched so a 16-year-old can read them** — one drafting job serves both regimes — with
  bite-size just-in-time notes at the Dial, the open-to-work flag, and the Apply button, and a
  parent-facing version alongside. The conduct policy states plainly that moderation is reactive
  and report-driven (§10.1). Younger bands are recorded as out of scope, not designed for.
- **Contact points** (DSA Arts. 11/12): a published contact for authorities and one for users —
  satisfied by the §10.2 address and the complaint form, named as such.
- **DUAA s.164A complaints form** (§11.3). **DUAA s.81 → Article 25(1A) UK GDPR** (in force
  5 Feb 2026): children's higher protection needs are a named factor in
  data-protection-by-design — the DPIA cites it; §9 is the compliance.
- **Prominence (Standard 15):** reporting and rights tools are highlighted at signup and carry a
  persistent, identifiable affordance — placement, not new capability.
- **Age-purpose limitation (Standard 3):** the §9.3 age facts are used to establish age and drive
  the derived protections, and for nothing else, ever.

### 15.4 The EU question

The DSA may not apply at all (mere accessibility is not a substantial connection), and where it
does, Art. 19 exempts micro/small services from most of it — **but DSA Art. 13 (EU legal
representative) and GDPR Art. 27 (EU representative) have no small exemption and both trigger on
targeting the EU.** Two representatives is real money and real exposure: **v1 does not target the
EU**, which is also §13.1's strongest cohort argument. Revisit deliberately, never by drift.

### 15.5 Ongoing obligations

The transparency log (§10.3) is forever. The children's assessments are re-run on significant
change. The 48-hour clock is an operational property to keep true, not a launch checkbox.
Complaints get acknowledged within 30 days. Erased data goes beyond use as backups roll (§11.3).

### 15.6 The LAWYER gate — pre-launch legal review, in one place

No lawyer reviewed any of this; the map deliberately deferred counsel to the successor launch
effort rather than ticketing it. **Before launch, a lawyer reviews, at minimum:** the age posture
and its DPIA argument (the single most-flagged item — the 13-vs-16 distinction is the map's own,
not the ICO's); s.12(3)'s "proportionate systems" read on blanket-prohibition-plus-reactive-removal;
erasure of two-party Threads (§11.2, with the de-identification fallback); whether Standard 8's
"separate choices" reaches inside Profile-as-Application; and Ofcom-source verification where
automated retrieval was blocked (`statutory-duties.md` §3).

---

## 16. The domain model

*From [Model the domain](https://github.com/mandyMooreFan/docket/issues/11). The spec uses
[`CONTEXT.md`](./CONTEXT.md)'s vocabulary or amends it deliberately; there is no third option.*

- **Member is the account (never public); Profile is the page it publishes.** There is no Person
  concept — the product deliberately cannot observe one.
- **One Thread per pair, ever**; writing authorised by a Connection or an open Application
  ([ADR-0001](./docs/adr/0001-one-thread-per-pair.md)). If a poster and applicant later connect,
  the same correspondence continues.
- **Facts stored, conclusions derived**
  ([ADR-0002](./docs/adr/0002-derived-not-stored.md)): stored — Completeness inputs, moderation
  actions, Work verifications, the Dial, Connections, Blocks, Positions, the age facts (§9.3),
  authored-as-minor (§9.4), content-hidden-pending-review and blocked-hash (§10.5), reports,
  appeals, audit trails, company edit history. Derived — Capability, effective visibility,
  authorisation-to-write, *never earned* vs *withdrawn* (which falls out free), mutuals, reply
  counts per viewer. Accepted cost: every access evaluates rules rather than reading a flag.
- **A Position's currency gates** (people list, job posting, page editing); **a Work verification
  is a dated fact that never lapses**. Ending a Position removes all three derived rights at once;
  open postings run out their window; the applicant-queue obligation follows the person. A
  Position ending is self-reported — the same self-declaration cost accepted everywhere.
- **The vocabulary has no word for reach, ranking, score, relevance-to-you, degree of separation,
  or engagement.** A glossary that cannot say a thing keeps it out.
- **Amendment made by this spec:** `CONTEXT.md` gains two fact-shaped terms — the **Age fact**
  (§9.3's minimal form) and **Authored as minor** (§9.4) — recorded in this PR, per the map's
  create-then-wire note on Model the domain.

---

## 17. Boundaries, open items, and the map index

### 17.1 Out of scope — ruled out, not forgotten

Monetisation of any kind · federation and self-hosting interop (one canonical instance) ·
recruiter tooling (candidate search/outreach; jobs stay post-browse-apply) · groups, events and
learning products · native mobile apps · a public documented API and third-party clients · the
deployment procedure (DNS, TLS, reverse proxy, CI, backups, secrets, monitoring, deploys — a
successor effort; the spec records only the runtime shape, §14.2) · implementation itself, which
begins from this document.

Product-level refusals live where they bind: §5.6 (feed), §8 (search), §10.3 (moderation), §3.3
(identity). Together with this list they are `CONTRIBUTING.md`'s rejection table (§12).

### 17.2 Open items — the complete list

Anything not listed here is settled.

1. **Implementation-time details, flagged in place:** a version-numbered browser support matrix if
   wanted (§14.1). The other three are now settled where they were flagged, each decided inside the
   build ticket that met it: the unresolved-queue threshold is **one** (§6.4), photo treatment is
   **plain** (§2), and the Source link **sits in the app bar** (§12).
2. **A tension the build surfaced** (§10.5): the intimate-image hide is structural only where the
   reported location resolves. The build ships the defensible reading; which way the spec should
   finally read is argued in
   [issue #59](https://github.com/mandyMooreFan/docket/issues/59), and is **LAWYER**-gated.
3. **Pre-launch documents owed:** the DPIA (§15.2, before launch); the illegal-content and two
   children's assessments (§15.1, three months from first operation); terms + conduct policy +
   transparency layer drafting (§15.3); `CONTRIBUTING.md` / `CODE_OF_CONDUCT.md` / `SECURITY.md`
   (§12, shapes decided).
4. **The LAWYER gate** (§15.6) — the one review standing between this spec and launch.
5. **Facts to verify at launch time:** `docket.social` registration (§1); Resend account and
   deliverability posture (§14.2); Ofcom source text where automated retrieval was blocked;
   whether the stack logs IP addresses (a Children's code Standard 10 input the research could
   not check from a spec).
6. **The cohort choice** (§13.1) — an active decision that has to beat the developer default.

### 17.3 The map index

The [wayfinder map](https://github.com/mandyMooreFan/docket/issues/1) charted 2026-08-21 and
walked to this document; each ticket holds the full argument, alternatives, and accepted costs.

| Ticket | Where it landed |
|---|---|
| [Spec the profile and the connection graph](https://github.com/mandyMooreFan/docket/issues/2) | §4 |
| [Pin the design language](https://github.com/mandyMooreFan/docket/issues/3) | §2 |
| [Design the feed without gimmicks](https://github.com/mandyMooreFan/docket/issues/4) | §5 |
| [Spec the jobs board](https://github.com/mandyMooreFan/docket/issues/5) | §6 |
| [Spec messaging](https://github.com/mandyMooreFan/docket/issues/6) | §7 |
| [Decide identity and signup](https://github.com/mandyMooreFan/docket/issues/7) | §3 |
| [Name the product](https://github.com/mandyMooreFan/docket/issues/8) | §1 |
| [Choose the platform target](https://github.com/mandyMooreFan/docket/issues/9) | §14.1 |
| [Choose the stack](https://github.com/mandyMooreFan/docket/issues/10) | §14.2 |
| [Model the domain](https://github.com/mandyMooreFan/docket/issues/11) | §16, `CONTEXT.md`, ADRs |
| [Pick the license and contribution basics](https://github.com/mandyMooreFan/docket/issues/12) | §12 |
| [Decide how company entities are created and trusted](https://github.com/mandyMooreFan/docket/issues/14) | §6.1–6.2 |
| [Decide search and discovery](https://github.com/mandyMooreFan/docket/issues/15) | §8 |
| [Spec moderation, reporting, and abuse response](https://github.com/mandyMooreFan/docket/issues/16) | §10 |
| [Decide the cold-start plan](https://github.com/mandyMooreFan/docket/issues/17) | §13 |
| [Research the statutory duties of a hosted user-to-user service](https://github.com/mandyMooreFan/docket/issues/18) | §15, [`docs/statutory-duties.md`](./docs/statutory-duties.md) |
| [Decide what a Member may export and take with them](https://github.com/mandyMooreFan/docket/issues/19) | §11, [`docs/data-rights.md`](./docs/data-rights.md) |
| [Decide the minimum age, given the OSA children's regime](https://github.com/mandyMooreFan/docket/issues/20) | §9 |
| [Decide how the 48-hour intimate-image takedown duty is met](https://github.com/mandyMooreFan/docket/issues/21) | §10.5 |
| [Research what the ICO Children's code requires](https://github.com/mandyMooreFan/docket/issues/22) | §15, [`docs/childrens-code.md`](./docs/childrens-code.md) |
| [Decide what to do about the ICO's effective-age-gate position](https://github.com/mandyMooreFan/docket/issues/23) | §3.1, §9.1, §9.3, §15.2 |
| [Decide whether an under-18's Posts inherit the Profile's protection](https://github.com/mandyMooreFan/docket/issues/24) | §9.4 |

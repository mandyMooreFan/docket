# Contributing to Docket

Thanks for looking. Docket is deliberately narrow, and the fastest way to have a good time
here is to read the short sections at the top before you write code.

> **Status: build phase.** The v1 design is finished and settled — all of it is in
> [`SPEC.md`](./SPEC.md), and the reasoning behind every decision is on the
> [spec map](../../issues/1). The product is being built ticket by ticket on the
> [build map](../../issues/26); until that map closes, expect whole pillars to be missing
> and the parts that exist to say so plainly. If you want to change the *design*, the place
> to argue is an issue, not a PR.

## Who this is for

A person maintaining a professional life: a public resume page at a URL they control, a
handful of colleagues they actually know, a job search that doesn't cost money or dignity.
Not an audience-builder, not a recruiter with a pipeline, not a growth team.

That person is the tie-breaker for every design argument. **Every feature earns its place by
serving the professional use, never an engagement metric.** The product is a reading
experience — NYT-simple — and "no gimmicks" is a standing test, not a mood.

## What will be rejected

Not because the ideas are bad — because they contradict a constraint the product is built
on. The reasoning for each lives in `SPEC.md` at the cited section; several were examined
more than once before being refused. Please don't open a PR for these:

|                                                                | Why                                                                                     |
| -------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| Ads, premium tiers, paid postings, donations — money of any kind | Everything is free (§1.1). Sustainability is a future effort, not a v1 design input.    |
| Algorithmic ranking, infinite scroll, re-surfacing              | The feed is reverse-chronological and it ends (§5.1, §5.6).                             |
| Reactions, likes, shares, reposts, view counts, streaks, badges | The vocabulary deliberately has no word for reach or engagement (§1.1, §5.6, §16).      |
| A follow edge, or any influencer machinery                      | Connections are mutual; follow exists to serve an economy this product refuses (§4.2).  |
| Notification email, digests, re-engagement mail                 | The product never emails to bring you back (§5.6, §7.4). Jobs' opt-in mail is the one exception (§6.5). |
| Searching people by attributes — skill, employer, location, availability | People search is by name, full stop; attribute search is a sourcing tool (§8.1).  |
| Address-book import, exact-email lookup, suggestion engines     | Rejected by name (§8.3).                                                                |
| Analytics of any kind, including privately to the author        | Not even the author sees numbers beyond the reply count (§5.3).                         |
| Shadowbanning or covert visibility limiting                     | Refused outright — a lie told to a member about their own account (§10.3).              |
| Passwords, CAPTCHA, verification badges                         | Magic links only; no corporation in the login path; trust lives on the posting (§3.3, §6.2). |
| Video or audio posts                                            | Watch-time optimisation lives there, and hosting cost breaks a free product (§5.2).     |
| Automated judgement of what members say                         | Moderation is reactive and human; the only automation is the §10.4 list (§1.1, §10).    |
| Federation, self-hosting interop, a public API, native apps     | One canonical instance; v1 is responsive web only (§1.1, §17.1).                        |

If you think one of these is wrong, open an issue and argue against the written decision —
that's how the project governs itself (§12), and it has changed its mind before when the
argument was good.

## The invariants

These are load-bearing guarantees, enforced by integration tests. A PR that weakens a test
to make a feature fit will be closed:

1. **The model stores facts, never conclusions** (ADR-0002). Capability, effective
   visibility and authorisation-to-write are derived at read time. No boolean a rule could
   compute is ever persisted — except where a fact must outlive its inputs, which is exactly
   why *authored-as-minor* is stored (§9.4).
2. **Derived data never exceeds the Dial** (§8.5). Search results, people lists, connection
   lists and the index all honour the member's visibility choice, and the three
   service-imposed floors are floors, not settings.
3. **Nothing authored by an under-18 is ever visible logged-out or indexed** (§9.4), and the
   18 rollover never lifts it.
4. **`spring.jpa.open-in-view` stays `false`** — the service layer hands templates
   fully-loaded view models (§14.2). A lazy load in a template is a build error in spirit.
5. **No JavaScript build.** htmx is a single pinned vendored script; the CSS is hand-written
   from §2's tokens. There is no `package.json` and there won't be one (§14.2).
6. **The vocabulary is [`CONTEXT.md`](./CONTEXT.md)'s** — code uses its words or amends it
   deliberately in a PR that says so (§16). It has no word for reach, ranking, score or
   engagement; a glossary that cannot say a thing keeps it out.

## Architecture in one paragraph

Docket is a server-rendered Spring Boot application — deliberately, because the product is
documents, not an application: there is almost no client state, no realtime beyond an htmx
poll, and removing the JSON API tier removes the whole contract-drift bug class (§14.2).
Postgres holds the facts, Flyway owns the schema, Hibernate validates against it, and
templates receive hand-written view models. Transactional email is the entire authentication
system (§3.3), so mail stays plain SMTP with a swappable provider.

```
Java + Spring Boot        single Maven module, com.mbeebe.docket
Thymeleaf + htmx          server-rendered; htmx polls where messaging needs it
JPA / Hibernate           open-in-view OFF, explicit view models
Postgres, pinned major    Flyway migrations; Postgres FTS for search
plain SMTP                JavaMailSender, never a vendor SDK
```

## Getting set up

You need a **JDK 25** and **Docker** (the dev Postgres and the tests' throwaway databases
both run in containers):

```bash
git clone https://github.com/mandyMooreFan/docket.git
cd docket
./mvnw spring-boot:run    # starts the compose.yaml Postgres itself; app at :8080
./mvnw test               # integration tests against a real Postgres via Testcontainers
```

Everything CI runs, you can run: it's `./mvnw verify` on every PR.

## Tests

Tests are integration-first and written against the real stack — MockMvc over a running
context with a Testcontainers Postgres, not mocks of the repository layer. The invariants
above live in tests on purpose: the compliance-shaped rules (the Dial, the floors, everything
in §9) must be enforced by something that fails, not by care. There is no coverage target and
won't be one.

## Proposing a change

1. **Open an issue first** for anything beyond a typo or an obvious bug fix. Significant
   changes are argued in an issue and resolved in writing before code — the resolution names
   what was chosen, what was rejected, why, and which costs were knowingly accepted (§12).
   Governance is BDFL, legitimised by that written record.
2. Branch from `main`, keep the change small, and make sure `./mvnw verify` is green.
3. **Sign off your commits** (`git commit -s`). Docket uses the
   [Developer Certificate of Origin](https://developercertificate.org/) and deliberately has
   no CLA: without one, relicensing requires every contributor's permission, which makes the
   AGPL promise irrevocable — including by the owner.
4. Say in the PR body what a *member* gets out of the change. "Developers get a nicer API"
   is a fine answer — say it, it just gets weighed differently.

## License

Docket is [AGPL-3.0-or-later](./LICENSE) — chosen because Docket ships as a service, and
AGPL is the only licence whose copyleft fires on a hosted network. By contributing you agree
your contributions are licensed the same way.

Two consequences worth knowing (§12): **every page must carry a reachable "Source" link**
(AGPL §13 — it lives in the app bar, and removing it breaks the licence, not just a test);
and **the name is not licensed with the code** (AGPL §7(e)) — fork freely, call it something
else. With magic-link auth, a lookalike instance is a phishing surface, so that rule is
member safety, not branding.

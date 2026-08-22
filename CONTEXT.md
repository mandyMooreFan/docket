# Docket

An open-source professional network: profiles and connections, a feed, a jobs board, and messaging. Every term below was settled on the [wayfinder map](https://github.com/mandyMooreFan/docket/issues/1); the map holds the reasoning, this file holds the language.

## Language

### Identity

**Member**:
The account a person holds on Docket. One person may hold more than one; the product has deliberately built no way to tell.
_Avoid_: user, account, person

**Profile**:
The page a Member publishes about themselves. Exactly one per Member.
_Avoid_: page, resume, CV, bio

**Completeness**:
The state of a Profile carrying a verified email, a name, a headline, and at least one Position or education entry.
_Avoid_: onboarding, profile strength, completion score

**Dial**:
The single setting that governs who may see a whole Profile: public, members-only, or connections-only.
_Avoid_: privacy settings, visibility matrix, audience selector

**Capability**:
Something a Member may do — connect, message, post, post a job. A Capability is never held or granted; it is a conclusion drawn from Completeness, Work verifications, and moderation history.
_Avoid_: permission, role, entitlement, grant

**Open to work**:
A quiet flag on a Profile, shown to an audience the Member chooses. It is never searchable and never indexed.
_Avoid_: available, looking, job-seeking status

### The graph

**Connection**:
A mutual, accepted relationship between two Members. There is no one-directional equivalent.
_Avoid_: contact, follow, friend, link

**Connection request**:
An offer of a Connection, carrying an optional note. It is the product's only message request.
_Avoid_: invite, invitation, add request

**Disconnect**:
Ending a Connection. Quiet and reversible.
_Avoid_: remove, unfriend, unfollow

**Block**:
A total, durable severance between two Members: no correspondence, no request, no visibility, in either direction.
_Avoid_: mute, restrict, hide

**Mutuals**:
The Connections two Members share. Docket names no other relationship between two Members.
_Avoid_: 1st/2nd/3rd degree, network distance, path

### Publishing

**Post**:
Something a Member wrote for the feed. A Post is written, job-attached, or a work change.
_Avoid_: update, status, article, share

**Reply**:
A response to a Post, written by one of the Post author's Connections. A Reply is not a Post and never enters a feed.
_Avoid_: comment, thread

**Save**:
A Member's private bookmark of a Post. Visible to nobody else.
_Avoid_: bookmark, like, favourite

**Recommendation**:
Something one Member writes about another, published on the subject's Profile only once they approve it.
_Avoid_: endorsement, testimonial, reference

**Skill**:
A self-declared word on a Profile. Nobody may attest to anyone's Skill.
_Avoid_: endorsement, competency, tag

### Work

**Company**:
An employer, held as a name, a logo, a description, its Job postings and its people. A Company is never an actor: it holds no account and nobody speaks for it.
_Avoid_: organisation, employer account, company page, brand

**Position**:
A Member's claim to a role, current or past, optionally at a Company. Self-declared, like everything on a Profile.
_Avoid_: job, role, experience, employment record

**Work verification**:
A dated fact that a Member could receive mail at one of a Company's domains. It records a moment and never lapses.
_Avoid_: badge, verified status, company membership, employment proof

**Verified domain**:
A mail domain a Company is known by. Derived from Work verifications and never declared.
_Avoid_: website, company domain, claimed domain

**Job posting**:
An opening authored by a Member and attached to a Company, carrying a mandatory salary range and running for a fixed window.
_Avoid_: listing, vacancy, req, ad

**Application**:
A Member offering their Profile to a Job posting. The Profile is the application; there is nothing else to send.
_Avoid_: submission, candidacy, applicant record

**Outcome**:
The resolution an Application is owed — advanced, or not selected. Every Application receives one.
_Avoid_: status, stage, disposition, rejection

### Correspondence

**Thread**:
The single, permanent correspondence between a pair of Members. Writing to it is authorised by a Connection or by an open Application, and the history outlives either.
_Avoid_: chat, conversation, DM, inbox item

**Message**:
One entry in a Thread. Text, links and still images.
_Avoid_: DM, note, chat

**Unread count**:
The number of Messages awaiting a Member. The only count the product shows anywhere.
_Avoid_: notification, badge, alert

### Moderation

**Report**:
A Member's or a visitor's account of something they believe breaks a rule.
_Avoid_: flag, complaint, ticket

**Withdrawal**:
The removal of one Capability from one Member, for a stated period or indefinitely.
_Avoid_: ban, restriction, strike, penalty

**Suspension**:
A Member reduced to reading only. They may still sign in.
_Avoid_: ban, lock, freeze, timeout

**Termination**:
The end of a Member.
_Avoid_: ban, deletion, closure

**Appeal**:
A Member's request that a moderation decision be reconsidered, by the person who made it.
_Avoid_: dispute, review, escalation

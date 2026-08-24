-- Moderation, reporting and safety (SPEC.md §10, §11.3, §15.3, §16; ADR-0002; ticket #38).
-- Facts only, and one absence is the whole design: there is NO capability state, no
-- suspended flag and no terminated flag on member. §10.3's ladder "inverts §3.2's
-- earned-capability model rather than inventing a second system", so a Withdrawal is
-- a dated fact here and the Capability is derived at every ask (ADR-0002). "Never
-- earned" and "withdrawn" — which §10.3 requires the Member be told apart — then fall
-- out of the same derivation for free, and "every suspended Member" is a query over
-- these rows rather than a column someone must remember to clear.

-- A Report (§10.2, CONTEXT.md): a Member's account of something they believe breaks a
-- rule. The reporter reference deliberately does not cascade — a decision and its
-- reason must outlive the account that raised it, for the transparency log (§10.3) and
-- for the statement of reasons already sent. acknowledged_at is not nullable: DSA Art.
-- 16 confirmation of receipt happens in the same transaction as the Report, so there
-- is no window in which a Report exists unacknowledged.
create table report (
    id              bigint generated always as identity primary key,
    reporter_id     bigint not null references member (id),
    target_kind     text not null check (target_kind in
                        ('PROFILE', 'POST', 'REPLY', 'MESSAGE', 'JOB_POSTING', 'COMPANY')),
    target_id       bigint not null,
    category        text not null check (category in
                        ('ILLEGAL_CONTENT', 'IMPERSONATION', 'HARASSMENT', 'SPAM',
                         'PORNOGRAPHY', 'SELF_HARM')),
    account         text not null check (char_length(account) <= 4000),
    created_at      timestamptz not null,
    acknowledged_at timestamptz not null,
    decided_at      timestamptz,
    decision        text check (decision in ('UPHELD', 'DISMISSED')),
    decision_reason text not null default '',
    decided_by      bigint references member (id),
    check ((decided_at is null) = (decision is null))
);
-- The queue is the open reports, oldest first (§10.1); the partial index is the query.
create index report_open_idx on report (created_at) where decided_at is null;
create index report_target_idx on report (target_kind, target_id);
create index report_reporter_idx on report (reporter_id, created_at);
create index report_category_idx on report (category, created_at);

-- A moderation action (§10.3): one rung of the ladder, as a dated fact. One table, not
-- four, because the Appeal, the audit trail and the transparency log all want to read
-- "what was done to whom, and is it still in force" without knowing which rung it was.
-- reversed_at is how an upheld Appeal or a plain change of mind undoes a rung without
-- erasing that it happened. Member references do not cascade: §11.2 keeps a former
-- Member's side of a Thread, and the record of why must survive alongside it.
create table moderation_action (
    id          bigint generated always as identity primary key,
    report_id   bigint references report (id),
    kind        text not null check (kind in
                    ('REMOVAL', 'WITHDRAWAL', 'SUSPENSION', 'TERMINATION')),
    member_id   bigint references member (id),
    target_kind text check (target_kind in
                    ('PROFILE', 'POST', 'REPLY', 'MESSAGE', 'JOB_POSTING', 'COMPANY')),
    target_id   bigint,
    capability  text check (capability in
                    ('CONNECT', 'MESSAGE', 'POST', 'POST_JOB', 'REPLY')),
    until       timestamptz,
    reason      text not null check (char_length(reason) <= 4000),
    actor_id    bigint not null references member (id),
    acted_at    timestamptz not null,
    reversed_at timestamptz,
    -- Each rung's shape, held by the schema rather than by care.
    check (kind <> 'REMOVAL' or (target_kind is not null and target_id is not null)),
    check (kind <> 'WITHDRAWAL' or (member_id is not null and capability is not null)),
    check (kind <> 'SUSPENSION' or member_id is not null),
    -- Termination is the end of a Member (CONTEXT.md); it cannot expire.
    check (kind <> 'TERMINATION' or (member_id is not null and until is null)),
    check (capability is null or kind = 'WITHDRAWAL'),
    -- §10.3 refuses visibility limiting and shadowbanning outright. There is no rung
    -- for them, and no column here could express one.
    check (until is null or kind in ('WITHDRAWAL', 'SUSPENSION'))
);
create index moderation_action_live_idx on moderation_action (member_id, kind)
    where reversed_at is null;
create index moderation_action_report_idx on moderation_action (report_id);
create index moderation_action_acted_idx on moderation_action (acted_at);
create index moderation_action_target_idx on moderation_action (target_kind, target_id);

-- An Appeal (§10.3, CONTEXT.md): a Member's request that a decision be reconsidered, by
-- the person who made it. "One Appeal" is the unique index, not a rule someone enforces.
create table appeal (
    id         bigint generated always as identity primary key,
    action_id  bigint not null unique references moderation_action (id),
    member_id  bigint not null references member (id),
    account    text not null check (char_length(account) <= 4000),
    made_at    timestamptz not null,
    decided_at timestamptz,
    outcome    text check (outcome in ('UPHELD', 'REFUSED')),
    reason     text not null default '',
    check ((decided_at is null) = (outcome is null))
);
create index appeal_open_idx on appeal (made_at) where decided_at is null;

-- The intimate image content report (§10.5, OSA s.20A): a distinct route, no account.
-- s.20A(2)'s prescribed contents are held as check constraints rather than as form
-- validation, so a row that does not carry the declarations cannot exist at all. The
-- locator is deliberately free text of generous length: a non-member cannot see a
-- private Thread, so §10.5 accepts an imprecise location.
create table intimate_image_report (
    id               bigint generated always as identity primary key,
    locator          text not null check (char_length(locator) <= 4000),
    subject_declared boolean not null,
    acting_for       boolean not null,
    good_faith       boolean not null,
    contact          text not null,
    request_ip       text not null,
    created_at       timestamptz not null,
    decided_at       timestamptz,
    outcome          text check (outcome in ('CONFIRMED', 'RESTORED')),
    decision_reason  text not null default '',
    -- s.20A(2): the report is made by the subject or by someone acting for them.
    check (subject_declared or acting_for),
    -- s.20A(2): the good-faith statement is a precondition, not a preference.
    check (good_faith),
    check ((decided_at is null) = (outcome is null))
);
create index intimate_image_report_open_idx on intimate_image_report (created_at)
    where decided_at is null;

-- The auto-hide (§10.4.3, §10.5): a content-level, reversible, pre-decision hold that
-- is deliberately outside the four-rung ladder and implies no finding against the
-- uploader. It is a separate table from moderation_action for exactly that reason.
--
-- s.10(3A)'s 48-hour clock is met structurally: the hold is written in the same
-- transaction that receives the report, so no state exists in which reported content
-- is visible and a deadline is running. Nothing here is a deadline column, because
-- there is no deadline to track.
create table content_hold (
    id          bigint generated always as identity primary key,
    target_kind text not null check (target_kind in
                    ('PROFILE', 'POST', 'REPLY', 'MESSAGE', 'JOB_POSTING', 'COMPANY')),
    target_id   bigint not null,
    report_id   bigint not null references intimate_image_report (id),
    held_at     timestamptz not null,
    released_at timestamptz
);
-- One live hold per item: a second report on held content changes nothing about its
-- visibility, and must not be able to.
create unique index content_hold_live_key on content_hold (target_kind, target_id)
    where released_at is null;
create index content_hold_report_idx on content_hold (report_id);

-- The local blocklist of hashes of images taken down under s.20A (§10.4.2). More than
-- the law requires: it means the person depicted reports once, not every time. The
-- hash is perceptual, so "substantially the same" is a distance rather than equality —
-- the unique index is dedupe of identical hashes, not the matching rule.
create table blocked_image_hash (
    id        bigint generated always as identity primary key,
    hash      bigint not null,
    report_id bigint references intimate_image_report (id),
    added_at  timestamptz not null
);
create unique index blocked_image_hash_key on blocked_image_hash (hash);

-- The DUAA s.164A data-protection complaint (§11.3, §15.3): its own route, distinct
-- from a Report and from the intimate-image route, because the statute makes it one.
-- acknowledged_at is set on receipt; the 30-day duty is then impossible to miss rather
-- than merely tracked.
create table data_protection_complaint (
    id              bigint generated always as identity primary key,
    contact         text not null,
    account         text not null check (char_length(account) <= 8000),
    request_ip      text not null,
    created_at      timestamptz not null,
    acknowledged_at timestamptz not null,
    responded_at    timestamptz,
    response        text not null default ''
);
create index data_protection_complaint_open_idx on data_protection_complaint (created_at)
    where responded_at is null;

-- The request ledger for the two public, no-account forms, in identity's link_request
-- shape (§3.3, §8.3): one row per accepted request, written before and independently of
-- whether anything real happens downstream, so the limiter's behaviour never depends on
-- what exists at the other end.
create table public_form_request (
    id         bigint generated always as identity primary key,
    form       text not null check (form in ('INTIMATE_IMAGE', 'DATA_PROTECTION')),
    contact    text not null,
    request_ip text not null,
    created_at timestamptz not null
);
create index public_form_request_contact_idx
    on public_form_request (form, contact, created_at);
create index public_form_request_ip_idx
    on public_form_request (form, request_ip, created_at);

-- Termination (CONTEXT.md: the end of a Member). One row per Member, ever. The cause
-- distinguishes the ladder's fourth rung from §11.2's own front door — #39 owns the
-- member-facing flow and writes cause = 'MEMBER' behind the same port; moderation
-- writes cause = 'MODERATION' alongside its moderation_action row. Member references
-- across the product deliberately do not cascade, so terminating never deletes rows
-- that someone else's record depends on (§7.3, §11.1, §11.2).
create table member_termination (
    id            bigint generated always as identity primary key,
    member_id     bigint not null unique references member (id),
    cause         text not null check (cause in ('MODERATION', 'MEMBER')),
    reason        text not null default '',
    terminated_at timestamptz not null
);

-- Removal (§10.3 rung 1) as a dated fact on the item itself, in reply.removed_at's
-- shape — the one complete remove-primitive the product already had (§5.3). Putting it
-- here rather than in a moderation-owned table is deliberate: each module's read path
-- already filters its own rows, so a module that forgets gets a failing test, whereas a
-- moderation-side registry that a module forgot to consult would leak silently.
--
-- reply.removed_at is reused as-is: the author's own removal and a moderation removal
-- make the same item stop rendering, and moderation_action is where the difference is
-- recorded.
alter table post add column removed_at timestamptz;
alter table job_posting add column removed_at timestamptz;
alter table company add column removed_at timestamptz;
alter table profile add column removed_at timestamptz;

-- A Message is immutable to both correspondents by design (§7.3, §11.1) — neither may
-- edit or destroy the other's record. Moderation is not a correspondent, and illegal
-- content in a private Thread is still illegal content, so removal reaches here too.
-- The column is writable by moderation alone; no member-facing route sets it.
alter table message add column removed_at timestamptz;

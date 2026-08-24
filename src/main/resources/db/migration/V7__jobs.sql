-- The jobs board (SPEC.md §6.3–6.5, §5.2.2, §16; ticket #35).
-- Facts only, per ADR-0002: no is_open, no queue_blocked, no applicant counts.
-- Open/closed is derived from the stored window edge against the clock; the
-- queue block is derived from Outcome facts; every feed panel is a query.

-- A Job posting (§6.3, CONTEXT.md): an opening authored by a Member — never a
-- Company — attached to a Company, carrying a mandatory real salary range and
-- running a fixed window. closes_at is the window's edge, fixed at posting;
-- closed_at records the moment the closing sweep executed the §6.4 guarantee
-- (a dated fact of an action, like used_at on a link). Whether a posting is
-- open is always derived: closed_at is null AND the clock is before closes_at.
-- salary_max > salary_min is structural: the spec's "no single number" (§6.3)
-- means a range is two different numbers, and the schema cannot hold less.
create table job_posting (
    id            bigint generated always as identity primary key,
    company_id    bigint not null references company (id),
    poster_id     bigint not null references member (id),
    title         text not null check (char_length(title) between 1 and 200),
    description   text not null check (char_length(description) between 1 and 20000),
    location      text not null default '',
    remote_policy text not null check (remote_policy in ('ON_SITE', 'HYBRID', 'REMOTE')),
    salary_min    integer not null check (salary_min > 0),
    salary_max    integer not null check (salary_max > salary_min),
    currency      text not null check (currency in ('GBP', 'EUR', 'USD', 'CAD', 'AUD', 'CHF')),
    posted_at     timestamptz not null,
    closes_at     timestamptz not null,
    closed_at     timestamptz
);
create index job_posting_company_idx on job_posting (company_id, posted_at desc);
create index job_posting_poster_idx on job_posting (poster_id);
create index job_posting_posted_idx on job_posting (posted_at desc);
create index job_posting_open_idx on job_posting (closes_at) where closed_at is null;

-- An Application (§6.3, CONTEXT.md): a Member offering their Profile to a Job
-- posting — one click plus an optional note; there is nothing else to send.
-- One per Member per posting, structurally. The Outcome (advanced / not
-- selected) is the poster's dated decision; closed_without_response_at is the
-- sweep's separate, immutable record that the posting closed while this
-- Application was untouched (§6.4). Keeping them apart means a late resolution
-- never erases the fact of the silence — and the queue block derives as
-- "closed without response, still no Outcome".
create table application (
    id                         bigint generated always as identity primary key,
    posting_id                 bigint not null references job_posting (id),
    applicant_id               bigint not null references member (id) on delete cascade,
    note                       text not null default '' check (char_length(note) <= 1000),
    applied_at                 timestamptz not null,
    outcome                    text check (outcome in ('ADVANCED', 'NOT_SELECTED')),
    outcome_at                 timestamptz,
    closed_without_response_at timestamptz,
    unique (posting_id, applicant_id)
);
create index application_posting_idx on application (posting_id, applied_at);
create index application_applicant_idx on application (applicant_id, applied_at desc);

-- A saved search (§6.5): the one opt-in email in the product, created
-- explicitly from a filter set the seeker chose, at a frequency they chose.
-- The stop token is stored raw, deliberately: every send must carry the
-- one-click stop link, so a hash (which can only be checked, never re-sent)
-- would force rotating tokens and killing the links in older mail. The token
-- authorises exactly one thing — stopping this search's mail — so its damage
-- ceiling is below the database access needed to read it.
create table job_search (
    id             bigint generated always as identity primary key,
    member_id      bigint not null references member (id) on delete cascade,
    keyword        text not null default '',
    location       text not null default '',
    remote_policy  text check (remote_policy in ('ON_SITE', 'HYBRID', 'REMOTE')),
    salary_floor   integer,
    floor_currency text check (floor_currency in ('GBP', 'EUR', 'USD', 'CAD', 'AUD', 'CHF')),
    company        text not null default '',
    known_only     boolean not null default false,
    frequency      text not null check (frequency in ('DAILY', 'WEEKLY')),
    stop_token     text not null unique,
    created_at     timestamptz not null,
    last_sent_at   timestamptz,
    stopped_at     timestamptz
);
create index job_search_member_idx on job_search (member_id, created_at);

-- §5.2.2: the job-attached Post — the ONLY path from board to feed. The kind
-- check widens by exactly one value (the widening #33 left to this ticket),
-- and the posting reference exists only on that kind. Nothing auto-syndicates:
-- a row here still only ever comes from a member writing a Post.
alter table post drop constraint post_kind_check;
alter table post add constraint post_kind_check
    check (kind in ('WRITTEN', 'WORK_CHANGE', 'JOB_ATTACHED'));
alter table post add column job_posting_id bigint references job_posting (id);

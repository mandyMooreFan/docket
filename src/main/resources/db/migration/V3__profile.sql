-- The Profile: the page a Member publishes (SPEC.md §4.1). Facts only — Completeness,
-- Capability and effective visibility are derived at read time, never stored (ADR-0002):
-- no complete flag, no visible-to column, no indexable boolean.
create table profile (
    member_id    bigint primary key references member (id) on delete cascade,
    name         text not null default '',
    headline     text not null default '',
    location     text not null default '',
    summary      text not null default '',
    dial         text not null default 'PUBLIC'
                 check (dial in ('PUBLIC', 'MEMBERS_ONLY', 'CONNECTIONS_ONLY')),
    -- The quiet open-to-work flag (§4.1): off by default; its audience deliberately has
    -- no PUBLIC option, which makes "never searchable, never indexed" (§8.1) structural.
    open_to_work text not null default 'OFF'
                 check (open_to_work in ('OFF', 'CONNECTIONS', 'MEMBERS'))
);

-- A Company exists because somebody named it while adding a Position (§6.1) — no
-- queue, no gatekeeper, no owner. One row per name, case-insensitively: autocomplete
-- reuses, never forks. Logo, description and the trust gate arrive with #34.
create table company (
    id         bigint generated always as identity primary key,
    name       text not null,
    created_at timestamptz not null
);
create unique index company_name_key on company (lower(name));

-- A Position: a Member's self-declared claim to a role (§4.1, §16). Month resolution,
-- stored as the first of the month; a null end month is what "current" means — currency
-- is derived, never flagged (ADR-0002).
create table position (
    id          bigint generated always as identity primary key,
    member_id   bigint not null references member (id) on delete cascade,
    company_id  bigint references company (id),
    title       text not null,
    description text not null default '',
    start_month date not null,
    end_month   date,
    created_at  timestamptz not null
);
create index position_member_idx on position (member_id);
create index position_company_idx on position (company_id);

-- An education entry: the §3.2 bar's other half — a Position or one of these.
create table education_entry (
    id          bigint generated always as identity primary key,
    member_id   bigint not null references member (id) on delete cascade,
    institution text not null,
    course      text not null default '',
    start_year  integer,
    end_year    integer,
    created_at  timestamptz not null
);
create index education_member_idx on education_entry (member_id);

-- Skills: self-declared words, one row each. Nobody may attest to anyone's Skill —
-- there is deliberately nowhere for an endorsement to live.
create table skill (
    id         bigint generated always as identity primary key,
    member_id  bigint not null references member (id) on delete cascade,
    name       text not null,
    created_at timestamptz not null
);
create unique index skill_member_name_key on skill (member_id, lower(name));
create index skill_member_idx on skill (member_id);

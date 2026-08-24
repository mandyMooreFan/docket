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

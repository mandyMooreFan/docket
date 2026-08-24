-- Identity and sessions (SPEC.md §3), with the age fact in its minimal form (§9.3):
-- an adult's birth month/year is never stored — only "adult, declared on <date>";
-- a 16–17-year-old's month/year is kept solely for the automatic 18 rollover.

create table member (
    id              bigint generated always as identity primary key,
    email           text not null unique,
    created_at      timestamptz not null,
    age_kind        text not null check (age_kind in ('ADULT', 'MINOR')),
    age_declared_on date not null,
    birth_month     integer  check (birth_month between 1 and 12),
    birth_year      integer ,
    constraint age_fact_minimal check (
        (age_kind = 'ADULT' and birth_month is null and birth_year is null)
     or (age_kind = 'MINOR' and birth_month is not null and birth_year is not null)
    )
);

-- The ledger the §3.3 rate limits count against: one row per request, whether or not
-- a link was sent — counting only real sends would let the limiter leak who has an
-- account, the §8.3 membership oracle by another door.
create table link_request (
    id         bigint generated always as identity primary key,
    email      text not null,
    request_ip text not null,
    created_at timestamptz not null
);
create index link_request_email_idx on link_request (email, created_at);
create index link_request_ip_idx on link_request (request_ip, created_at);

-- One row per link actually sent.
create table magic_link (
    id          bigint generated always as identity primary key,
    token_hash  text not null unique,
    email       text not null,
    purpose     text not null check (purpose in ('JOIN', 'LOGIN')),
    age_kind    text check (age_kind in ('ADULT', 'MINOR')),
    birth_month integer check (birth_month between 1 and 12),
    birth_year  integer,
    request_ip  text not null,
    created_at  timestamptz not null,
    expires_at  timestamptz not null,
    used_at     timestamptz
);

-- 90-day sliding sessions, enumerable for the settings list (§3.3).
create table member_session (
    id           bigint generated always as identity primary key,
    token_hash   text not null unique,
    member_id    bigint not null references member (id) on delete cascade,
    created_at   timestamptz not null,
    last_used_at timestamptz not null,
    client       text not null
);
create index member_session_member_idx on member_session (member_id);

-- The walking skeleton has served its purpose (#27).
drop table walking_skeleton;

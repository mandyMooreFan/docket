-- The graph (SPEC.md §4.2–4.3, §7.3): facts only, conclusions derived (ADR-0002).
-- No degrees, no scores, no stored "connected" flag anywhere else — whether two
-- Members are connected is always a query over these rows at the point of asking.

-- A Connection request (§4.2): the offer, its optional note, and what became of it.
-- Rows are never deleted: a DECLINED row is the fact that blocks repeat requests,
-- while the sender's view keeps deriving "sent" from it — decline stays silent
-- because nothing the sender can observe changes.
create table connection_request (
    id           bigint generated always as identity primary key,
    requester_id bigint not null references member (id) on delete cascade,
    recipient_id bigint not null references member (id) on delete cascade,
    note         text not null default '',
    state        text not null default 'PENDING'
                 check (state in ('PENDING', 'ACCEPTED', 'DECLINED')),
    sent_at      timestamptz not null,
    responded_at timestamptz,
    check (requester_id <> recipient_id)
);
-- At most one live request per ordered pair.
create unique index connection_request_pending_key
    on connection_request (requester_id, recipient_id) where state = 'PENDING';
create index connection_request_recipient_idx on connection_request (recipient_id);
create index connection_request_requester_idx on connection_request (requester_id);

-- The Connection itself, stored as a fact (§16): one row per pair, lower id first.
-- Acceptance creates it; Disconnect or Block deletes it — the row ending IS the
-- quiet severance, and reconnecting is simply a new row.
create table connection (
    id           bigint generated always as identity primary key,
    member_a     bigint not null references member (id) on delete cascade,
    member_b     bigint not null references member (id) on delete cascade,
    connected_at timestamptz not null,
    check (member_a < member_b)
);
create unique index connection_pair_key on connection (member_a, member_b);
create index connection_member_b_idx on connection (member_b);

-- A Block (§7.3): deliberate, durable, total. Who blocked whom is the stored fact;
-- every conclusion drawn from it treats the pair symmetrically. There is
-- deliberately no lifted_at — v1 builds no unblock.
create table member_block (
    id         bigint generated always as identity primary key,
    blocker_id bigint not null references member (id) on delete cascade,
    blocked_id bigint not null references member (id) on delete cascade,
    created_at timestamptz not null,
    check (blocker_id <> blocked_id)
);
create unique index member_block_pair_key on member_block (blocker_id, blocked_id);
create index member_block_blocked_idx on member_block (blocked_id);

-- A Recommendation (§4.3): the words one Member wrote about another, plus two dated
-- facts. "Displayed" is derived — approved and not since hidden — never stored.
-- One per author per subject: "only a Connection may write one" is singular.
create table recommendation (
    id          bigint generated always as identity primary key,
    author_id   bigint not null references member (id) on delete cascade,
    subject_id  bigint not null references member (id) on delete cascade,
    text        text not null,
    written_at  timestamptz not null,
    approved_at timestamptz,
    hidden_at   timestamptz,
    check (author_id <> subject_id)
);
create unique index recommendation_author_subject_key
    on recommendation (author_id, subject_id);
create index recommendation_subject_idx on recommendation (subject_id);

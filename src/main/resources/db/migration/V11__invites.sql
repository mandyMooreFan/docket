-- The Invite (SPEC.md §13.3; ticket #40): an offer sent to an email address that
-- belongs to no Member yet, carrying an optional note. It never gates signup; it
-- becomes a Connection request if the person joins.
--
-- One table, and it is doing two jobs at once on purpose. It is the record of what
-- was sent, and it is §3.3's rate-limit ledger in identity's link_request shape —
-- one row per accepted attempt, posted or not. Limits that moved only for mail
-- that actually went would answer differently for an address that already has an
-- account, and that difference is §8.3's rejected membership oracle by another
-- door.
--
-- What is deliberately NOT a column here, and the reason in each case:
--
--   * whether mail was sent. The only fact it records is "was that address already
--     a Member", which is the question §8.3 refuses to answer. Nothing in the
--     product reads it, so it does not exist to leak.
--
--   * what became of the waiting request. §9.2 lets the graph refuse an adult's
--     request to a 16-year-old, and a row saying "refused" would be precisely the
--     age fact about a named child that §9.2 exists to keep from that adult.
--     landed_at says only that this Invite has been spent, so it cannot be spent
--     twice, and the sender is never shown it.
--
--   * a token. §13.3's "optional, never a gate" is structural rather than a
--     promise: there is nothing to redeem, the mail links to the same open /join
--     as the front page, and the landing is found by ADDRESS at the far end. An
--     invited signup is therefore an ordinary signup (§3.1) in every respect.
--
-- No quota column either, at any grain. §13.3 rules a quota out in as many words:
-- it throttles exactly the members doing the seeding cold start depends on (§13.2).
create table invite (
    id        bigint generated always as identity primary key,
    sender_id bigint not null references member (id) on delete cascade,
    email     text not null,
    note      text not null default '',
    sent_at   timestamptz not null,
    landed_at timestamptz
);

-- Case-insensitive, because an address typed "Bob@" by the sender and "bob@" by
-- the joiner is one address to a postmaster: it has to be one address to the
-- per-address limit and to the landing, or both come apart on a shift key.
create index invite_email_idx on invite (lower(email));
create index invite_sender_idx on invite (sender_id, sent_at);

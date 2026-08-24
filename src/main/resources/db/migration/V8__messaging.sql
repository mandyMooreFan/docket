-- Messaging (SPEC.md §7, §16; ADR-0001, ADR-0002; ticket #36).
-- Facts only: threads, messages, per-member read marks, message images. There
-- is deliberately NO open/closed/writable column anywhere: whether a Thread
-- may be written to is derived at every ask from the graph and the
-- applications (a Connection, or an Application whose channel is open), so
-- Disconnect, Block and Outcome changes need no thread updates at all — and
-- ADR-0001's "reconnecting reopens the same Thread" falls out free.

-- The Thread (ADR-0001, CONTEXT.md): the single, permanent correspondence
-- between a pair of Members — one row per pair, EVER, lower id first, created
-- lazily on the first authorised write. Member references deliberately do not
-- cascade: §11.2 keeps a former member's side of a Thread, and neither person
-- may destroy the other's record (§7.3, §11.1). Termination display is #39's;
-- the attribution seam is these non-cascading references.
create table thread (
    id         bigint generated always as identity primary key,
    member_a   bigint not null references member (id),
    member_b   bigint not null references member (id),
    created_at timestamptz not null,
    check (member_a < member_b)
);
create unique index thread_pair_key on thread (member_a, member_b);
create index thread_member_b_idx on thread (member_b);

-- A Message (§7.2, CONTEXT.md): one entry in a Thread — text, links, still
-- images. author_id does not cascade for the same #39 reason as above.
create table message (
    id         bigint generated always as identity primary key,
    thread_id  bigint not null references thread (id),
    author_id  bigint not null references member (id),
    body       text not null check (char_length(body) <= 8000),
    created_at timestamptz not null
);
create index message_thread_idx on message (thread_id, id);

-- Still images on a Message, stored through the one image store (§10.4) in
-- the feed's post_image shape — this table only references image rows the
-- checks already passed.
create table message_image (
    id         bigint generated always as identity primary key,
    message_id bigint not null references message (id) on delete cascade,
    image_id   bigint not null references image (id),
    position   int not null
);
create index message_image_message_idx on message_image (message_id);

-- The per-member, per-thread read mark (§7.4, in §5.1's read-position shape):
-- a high-water mark over message ids, advanced past what a view rendered. The
-- Unread count — the only badge in the product — is derived from it at every
-- ask. Nothing here is ever visible to the other member (§7.2: no read
-- receipts, ever). Ids, not timestamps, so a tie can never hide a message.
create table thread_read (
    thread_id            bigint not null references thread (id) on delete cascade,
    member_id            bigint not null references member (id) on delete cascade,
    last_read_message_id bigint not null,
    primary key (thread_id, member_id)
);

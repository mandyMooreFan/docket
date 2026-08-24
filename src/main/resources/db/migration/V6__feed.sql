-- The feed and Posts (SPEC.md §5, §9.4, §16; ticket #33).
-- Facts only, per ADR-0002: no reply_count, no read/unread flags, no visibility
-- column. Feeds, counts and visibility are all derived at read time. The one
-- deliberate stored derivation is authored_as_minor (§9.4): it must be fixed at
-- write time because §9.3 deletes the birth data the derivation would need.

-- A Post (§5.2): written now, work-change via the opt-in tick; job-attached
-- arrives with the jobs board (#35), which widens the kind check and adds its
-- posting reference then. thread_closed_at is the author closing the thread —
-- a dated fact, like every other action here.
create table post (
    id                bigint generated always as identity primary key,
    author_id         bigint not null references member (id) on delete cascade,
    kind              text not null check (kind in ('WRITTEN', 'WORK_CHANGE')),
    body              text not null check (char_length(body) <= 40000),
    authored_as_minor boolean not null,
    thread_closed_at  timestamptz,
    created_at        timestamptz not null
);
create index post_author_idx on post (author_id, created_at desc);
create index post_created_idx on post (created_at desc);

-- Still images on a written Post (§5.2.1), stored through the one image store
-- (§10.4) — this table only references image rows the checks already passed.
create table post_image (
    id       bigint generated always as identity primary key,
    post_id  bigint not null references post (id) on delete cascade,
    image_id bigint not null references image (id),
    position int not null
);
create index post_image_post_idx on post_image (post_id);

-- A Reply (§5.3): not a Post, never enters a feed (CONTEXT.md). removed_at is
-- the author-of-the-Post's removal, stored as a fact; removed replies simply
-- stop rendering and stop counting.
create table reply (
    id                bigint generated always as identity primary key,
    post_id           bigint not null references post (id) on delete cascade,
    author_id         bigint not null references member (id) on delete cascade,
    body              text not null check (char_length(body) <= 2000),
    authored_as_minor boolean not null,
    removed_at        timestamptz,
    created_at        timestamptz not null
);
create index reply_post_idx on reply (post_id, created_at);
create index reply_author_idx on reply (author_id, created_at);

-- The private Save (§5.3): visible to nobody but its owner, counted nowhere.
create table saved_post (
    id        bigint generated always as identity primary key,
    member_id bigint not null references member (id) on delete cascade,
    post_id   bigint not null references post (id) on delete cascade,
    saved_at  timestamptz not null,
    unique (member_id, post_id)
);
create index saved_post_member_idx on saved_post (member_id, saved_at desc);

-- The per-member read position (§5.1's "tracked state"): a single high-water
-- mark. Everything after it is new; viewing the feed advances it past what was
-- rendered, so nothing is ever shown twice and nothing ever re-surfaces.
create table feed_visit (
    member_id  bigint primary key references member (id) on delete cascade,
    seen_up_to timestamptz not null
);

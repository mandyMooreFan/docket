-- Search (SPEC.md §8, §10.3, §14.2; ticket #37).
--
-- Postgres full-text search, no dedicated search service (§14.2): there is no
-- personalisation to compute and no attribute faceting to fan out, so a second
-- datastore would buy nothing and cost an instance.
--
-- ADR-0002 still holds. A generated tsvector is NOT a stored conclusion: it is a
-- mechanical, deterministic transform of text columns that already exist on the
-- same row — the same fact, tokenised, recomputed by Postgres on every write. No
-- rule is frozen here. Everything a rule decides is still decided at query time:
-- the Dial and its floors, Blocks, open/closed, merged/absorbed, §9.4's cap. That
-- is why turning the Dial down takes effect on the very next query rather than on
-- the next reindex, and why the columns below are named *_tsv — there is no
-- indexable, searchable or visible column anywhere in this schema.
--
-- What is deliberately NOT indexed anywhere below: open_to_work. §8.1 says it is
-- never a search axis and never appears in the index; keeping it out of every
-- expression here is what makes that structural rather than a rule to remember.

-- People: the NAME and nothing else (§8.1). Headline, location, summary and
-- Skills are §8.1's refused axis, so they are not in the document at all —
-- "you search a person by name, that is the whole of it" is enforced by there
-- being nowhere else for a match to come from.
--
-- Config 'simple', not 'english', and the reason is names: the english
-- dictionary would drop a member called Will, An or May as stop words and stem
-- Manning to man. Names are not English prose; they get tokenised and folded to
-- lower case, and nothing else happens to them.
alter table profile
    add column name_tsv tsvector
        generated always as (to_tsvector('simple', name)) stored;
create index profile_name_tsv_idx on profile using gin (name_tsv);

-- Companies: the name, on the same reasoning ("The Guardian" must not lose its
-- first word to a stop list). Absorbed companies are excluded at query time from
-- merged_into_id, never here — a merge is reversible (§10.5) and an index that
-- had baked it in would be a second thing to unwind.
alter table company
    add column name_tsv tsvector
        generated always as (to_tsvector('simple', name)) stored;
create index company_name_tsv_idx on company using gin (name_tsv);

-- Posts: the body, in 'english' — this is prose, and stemming is what makes
-- "memoirs" find "memoir". Whether a viewer may see the Post is derived at
-- query time from the author's Dial plus §9.4's permanent cap; the authored-as-
-- minor fact is deliberately not in this document, because it is not a word
-- anyone searches for, it is a rule that runs after the match.
alter table post
    add column body_tsv tsvector
        generated always as (to_tsvector('english', body)) stored;
create index post_body_tsv_idx on post using gin (body_tsv);

-- Job postings: title, location and description, weighted A/B/C so that a title
-- match outranks a passing mention in a description on /search. The jobs BOARD
-- deliberately ignores this ranking — §6.5 is one list, newest first — and uses
-- the same document only to narrow. Salary and remote policy stay their own
-- filter params: they are structured facts, not words.
alter table job_posting
    add column text_tsv tsvector
        generated always as (
            setweight(to_tsvector('english', title), 'A')
            || setweight(to_tsvector('english', location), 'B')
            || setweight(to_tsvector('english', description), 'C')
        ) stored;
create index job_posting_text_tsv_idx on job_posting using gin (text_tsv);

-- §10.3: scraping is answered by rate limits on search — per account, and per IP
-- logged out. A system control applied to everyone, never moderation: nothing
-- here enters the report queue or the four-rung ladder, and no member state
-- changes when a limit is hit.
--
-- The same ledger shape identity uses for link_request: one row per search that
-- actually ran, counted in a window. Data-minimising by construction — a signed-
-- in search records the member and no address; a signed-out one records the
-- address and no member. The check constraint makes that structural, so the
-- table can never quietly become an audit trail of who searched from where.
create table search_request (
    id         bigint generated always as identity primary key,
    member_id  bigint references member (id) on delete cascade,
    request_ip text,
    created_at timestamptz not null,
    constraint search_request_one_subject check (
        (member_id is not null and request_ip is null)
        or (member_id is null and request_ip is not null))
);
create index search_request_member_idx on search_request (member_id, created_at);
create index search_request_ip_idx on search_request (request_ip, created_at);

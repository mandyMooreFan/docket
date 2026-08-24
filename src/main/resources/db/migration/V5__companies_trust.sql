-- Companies and the trust gate (SPEC.md §6.1–6.2, §10.4–10.5, §16; ticket #34).
-- Facts only, per ADR-0002: no is_verified, no can_edit, no verified-domain column.
-- The Verified domain set is derived from work_verification rows at read time;
-- editing rights are derived from a current Position plus a Work verification.

-- The product's first image store (§10.4): bytes as a fact, capped and typed here so
-- the cap is structural. The two upload hash checks run BEFORE a row is created —
-- a rejected image never reaches this table.
create table image (
    id           bigint generated always as identity primary key,
    content_type text not null check (content_type in ('image/png', 'image/jpeg')),
    data         bytea not null check (octet_length(data) <= 524288),
    created_at   timestamptz not null
);

-- The Company grows its page (§6.1): description and logo. merged_into_id is the
-- merge fact's pointer — an absorbed Company's row survives, so a merge is
-- reversible and its old URL can redirect (§10.5).
alter table company
    add column description text not null default '',
    add column logo_image_id bigint references image (id),
    add column merged_into_id bigint references company (id);
create index company_merged_into_idx on company (merged_into_id);

-- A Work verification (§6.2, §16): a dated fact that a Member could receive mail at
-- one of a Company's domains. It records a moment and never lapses — there is no
-- expiry column on purpose. Only the DOMAIN is kept durably; the full address was
-- operational (needed to send the link) and is not retained here (§9.3's spirit).
create table work_verification (
    id          bigint generated always as identity primary key,
    member_id   bigint not null references member (id) on delete cascade,
    company_id  bigint not null references company (id),
    domain      text not null,
    verified_at timestamptz not null
);
create index work_verification_member_idx on work_verification (member_id);
create index work_verification_company_idx on work_verification (company_id);
create index work_verification_domain_idx on work_verification (domain);

-- One row per verification link actually sent — the same shape as magic_link (§6.2:
-- the same machinery as login). The address is needed to send and is blanked when
-- the link is consumed or dead: the durable fact above keeps only the domain.
create table work_link (
    id         bigint generated always as identity primary key,
    token_hash text not null unique,
    member_id  bigint not null references member (id) on delete cascade,
    company_id bigint not null references company (id),
    domain     text not null,
    address    text,
    created_at timestamptz not null,
    expires_at timestamptz not null,
    used_at    timestamptz
);

-- The rate-limit ledger for verification sends, one row per accepted request whether
-- or not mail went out — identity's link_request reasoning (§8.3): limits that moved
-- only on real sends would leak which addresses exist.
create table work_link_request (
    id         bigint generated always as identity primary key,
    member_id  bigint not null references member (id) on delete cascade,
    address    text not null,
    created_at timestamptz not null
);
create index work_link_request_member_idx on work_link_request (member_id, created_at);
create index work_link_request_address_idx on work_link_request (address, created_at);

-- The Company page's edit history (§6.1, §10.5): who changed what, when, from what
-- to what. Vandalism by a verified employee is answerable from these rows, so they
-- reference member without cascade — the record must outlive convenience.
create table company_edit (
    id         bigint generated always as identity primary key,
    company_id bigint not null references company (id),
    member_id  bigint not null references member (id),
    field      text not null check (field in ('NAME', 'LOGO', 'DESCRIPTION')),
    old_value  text not null,
    new_value  text not null,
    edited_at  timestamptz not null
);
create index company_edit_company_idx on company_edit (company_id, edited_at);

-- A merge, recorded as an audited, reversible fact (§10.5): destructive actions on
-- other people's employment history keep a full record. cause SHARED_DOMAIN is the
-- §6.1 auto-merge; MANUAL (with an actor) arrives with moderation (#38).
create table company_merge (
    id                   bigint generated always as identity primary key,
    absorbed_company_id  bigint not null references company (id),
    surviving_company_id bigint not null references company (id),
    cause                text not null check (cause in ('SHARED_DOMAIN', 'MANUAL')),
    actor_member_id      bigint references member (id),
    merged_at            timestamptz not null,
    reversed_at          timestamptz
);

-- Every row a merge repointed, so reversal is mechanical rather than forensic.
create table company_merge_item (
    id       bigint generated always as identity primary key,
    merge_id bigint not null references company_merge (id) on delete cascade,
    kind     text not null check (kind in ('POSITION', 'WORK_VERIFICATION', 'COMPANY_EDIT')),
    row_id   bigint not null
);
create index company_merge_item_merge_idx on company_merge_item (merge_id);

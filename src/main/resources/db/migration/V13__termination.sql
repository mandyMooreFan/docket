-- Termination (SPEC.md §11.2, §9.3; ticket #39).
--
-- A Member who leaves cannot be deleted, and that is deliberate rather than a
-- limitation. V8 gave thread.member_a/b and message.author_id references that do
-- NOT cascade, "precisely so a former member's side survives" — which means the
-- row they point at has to survive too. Deleting the member row would be refused
-- by those references, and making them cascade would be one person deleting a
-- colleague's correspondence into a monologue of holes (§11.2's named cost).
--
-- So the member row becomes a tombstone: dated, reasoned, and stripped of
-- everything that identified the person. Their email is replaced (they must be
-- able to join again from scratch, and nothing here may re-identify them), their
-- sessions and links go, and every module deletes its own rows through the
-- leaving.Departure port. What is left behind is an id — the anchor the kept
-- correspondence, the Recommendations they wrote and the Replies they left under
-- other people's Posts hang from, all of which now render as "A former member".
--
-- Facts only, per ADR-0002: terminated_at is a dated fact of an action, in the
-- shape of used_at, closed_at and hidden_at. Nothing derived is stored — whether
-- a Profile 404s, whether a card says "a former member", and whether a Thread is
-- writable are all still worked out at the point of asking.
alter table member add column terminated_at timestamptz;
alter table member add column termination_reason text;

-- §9.3's minimal age fact, extended to the end of a Member: a tombstone holds no
-- birth data at all. The 18 rollover already collapses a minor's month/year to
-- the bare adult fact; leaving does the same thing for the same reason, except
-- that it must not also assert "adult" about someone who never was one. So the
-- constraint gains a third legal shape — MINOR, terminated, no birth data — and
-- the honest record ("this was a 16–17-year-old's account, and it ended") is kept
-- without keeping the data §9.3 says to hold for exactly one purpose that no
-- longer exists.
alter table member drop constraint age_fact_minimal;
alter table member add constraint age_fact_minimal check (
    (age_kind = 'ADULT' and birth_month is null and birth_year is null)
 or (age_kind = 'MINOR' and birth_month is not null and birth_year is not null)
 or (age_kind = 'MINOR' and terminated_at is not null
     and birth_month is null and birth_year is null)
);

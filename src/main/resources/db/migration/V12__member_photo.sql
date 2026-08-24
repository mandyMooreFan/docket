-- The member photo (SPEC.md §4.1, §2; ticket #52), plus the image-lookup indexes
-- #51 deferred.
--
-- A photo is one nullable pointer from the Profile into the one image store
-- (§10.4) — no second pipeline, no bytes of its own. It is deliberately absent
-- from the §3.2 bar ("exclusionary; generated faces are free"), so there is
-- nothing here for Completeness to read, and nothing here says who may see it:
-- that is derived from the Profile's effective visibility on every request
-- (ADR-0002), exactly like the page the photo sits on.
alter table profile
    add column photo_image_id bigint references image (id);

-- /images/{id} asks every owning module "is this image yours, and may this
-- viewer have it?" on every single request (images.ImageAudience). Each answer
-- is a lookup BY image_id, and until now none of them had an index — the small
-- scan #51 accepted rather than burn a migration number while V8 and V9 were in
-- flight. A third image kind joins that read path here, so all four get one.
create index profile_photo_image_idx on profile (photo_image_id);
create index post_image_image_idx on post_image (image_id);
create index message_image_image_idx on message_image (image_id);
create index company_logo_image_idx on company (logo_image_id);

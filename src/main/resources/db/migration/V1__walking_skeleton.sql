-- The walking skeleton: proves Flyway runs and the app reads what a migration wrote.
-- Dropped by a later migration once the first real feature lands.
create table walking_skeleton (
    id   bigint primary key,
    note text not null
);

insert into walking_skeleton (id, note)
values (1, 'Docket walks: template, database and migration are wired end to end.');

create table if not exists employee (
    id bigserial not null,
    name varchar,
    salary bigint,
    department bigint,
    primary key (id)
);
create table if not exists organization (
    id bigserial not null,
    name varchar,
    tax_number integer not null,
    primary key (id)
);
create table if not exists department (
    id bigserial not null,
    organization bigint
    constraint organization_fk
        references organization
        on delete restrict,
    name varchar,
    primary key (id)
);
CREATE SEQUENCE IF NOT EXISTS mysequence START 1;
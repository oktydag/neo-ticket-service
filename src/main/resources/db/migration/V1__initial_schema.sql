-- ---------------------------------------------------------------------------
-- iam
-- ---------------------------------------------------------------------------

create table users (
    id            uuid         not null,
    email         varchar(254) not null,
    password_hash varchar(100) not null,
    created_at    timestamp(6) not null,
    last_login_at timestamp(6),
    constraint pk_users primary key (id),
    constraint uq_users_email unique (email)
);

create table user_roles (
    user_id uuid        not null,
    role    varchar(32) not null,
    constraint pk_user_roles primary key (user_id, role),
    constraint fk_user_roles_user foreign key (user_id) references users (id) on delete cascade
);

create table refresh_tokens (
    token_id    uuid         not null,
    user_id     uuid         not null,
    family_id   uuid         not null,
    issued_at   timestamp(6) not null,
    expires_at  timestamp(6) not null,
    revoked_at  timestamp(6),
    replaced_by uuid,
    constraint pk_refresh_tokens primary key (token_id),
    constraint fk_refresh_tokens_user foreign key (user_id) references users (id) on delete cascade
);

create index ix_refresh_tokens_family on refresh_tokens (family_id);
create index ix_refresh_tokens_expires_at on refresh_tokens (expires_at);

-- ---------------------------------------------------------------------------
-- event catalog
-- ---------------------------------------------------------------------------

create table events (
    id             uuid         not null,
    owner_id       uuid         not null,
    title          varchar(140) not null,
    venue          varchar(200) not null,
    starts_at      timestamp(6) not null,
    ends_at        timestamp(6) not null,
    capacity       integer      not null,
    reserved_seats integer      not null,
    published      boolean      not null,
    published_at   timestamp(6),
    created_at     timestamp(6) not null,
    updated_at     timestamp(6) not null,
    version        bigint       not null,
    constraint pk_events primary key (id),
    constraint fk_events_owner foreign key (owner_id) references users (id),
    constraint ck_events_capacity check (capacity > 0 and reserved_seats >= 0 and reserved_seats <= capacity),
    constraint ck_events_schedule check (ends_at > starts_at)
);

create index ix_events_published_starts_at on events (published, starts_at);
create index ix_events_owner on events (owner_id);

-- ---------------------------------------------------------------------------
-- reservations
-- ---------------------------------------------------------------------------

create table reservations (
    id         uuid         not null,
    event_id   uuid         not null,
    user_id    uuid         not null,
    status     varchar(16)  not null,
    seats      integer      not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    version    bigint       not null,
    constraint pk_reservations primary key (id),
    constraint fk_reservations_event foreign key (event_id) references events (id),
    constraint fk_reservations_user foreign key (user_id) references users (id),
    constraint ck_reservations_seats check (seats between 1 and 10)
);

create index ix_reservations_event_status on reservations (event_id, status);
create index ix_reservations_user on reservations (user_id);

-- ---------------------------------------------------------------------------
-- idempotency
-- ---------------------------------------------------------------------------

create table idempotency_keys (
    id              uuid         not null,
    idempotency_key varchar(128) not null,
    endpoint        varchar(200) not null,
    user_id         uuid         not null,
    request_hash    varchar(64)  not null,
    response_hash   varchar(64),
    response_body   varchar(8192),
    response_status integer,
    status          varchar(16)  not null,
    created_at      timestamp(6) not null,
    expires_at      timestamp(6) not null,
    constraint pk_idempotency_keys primary key (id),
    constraint uq_idempotency_keys_scope unique (idempotency_key, endpoint, user_id)
);

create index ix_idempotency_keys_expires_at on idempotency_keys (expires_at);

-- ---------------------------------------------------------------------------
-- audit
-- ---------------------------------------------------------------------------

create table audit_logs (
    id            uuid         not null,
    actor_id      uuid,
    action        varchar(64)  not null,
    resource_type varchar(64)  not null,
    resource_id   varchar(64)  not null,
    ip            varchar(45)  not null,
    user_agent    varchar(256) not null,
    request_id    varchar(64)  not null,
    created_at    timestamp(6) not null,
    constraint pk_audit_logs primary key (id)
);

create index ix_audit_logs_created_at on audit_logs (created_at);
create index ix_audit_logs_actor on audit_logs (actor_id);
create index ix_audit_logs_resource on audit_logs (resource_type, resource_id);

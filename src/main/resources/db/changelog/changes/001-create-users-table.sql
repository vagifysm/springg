--liquibase formatted sql

--changeset starter:001-create-users-table
CREATE TABLE users (
    id          UUID                     NOT NULL,
    email       VARCHAR(255)             NOT NULL,
    password    VARCHAR(100)             NOT NULL,
    full_name   VARCHAR(150)             NOT NULL,
    role        VARCHAR(20)              NOT NULL,
    enabled     BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email)),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'))
);
--rollback DROP TABLE users;

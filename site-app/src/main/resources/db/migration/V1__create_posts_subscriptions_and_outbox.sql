CREATE TABLE posts (
    id           bigserial PRIMARY KEY,
    title        text NOT NULL,
    body         text NOT NULL,
    published_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE subscriptions (
    id                 bigserial PRIMARY KEY,
    email              text NOT NULL UNIQUE,
    confirmation_token uuid NOT NULL UNIQUE,
    status             text NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'UNSUBSCRIBED')),
    created_at         timestamptz NOT NULL DEFAULT now(),
    confirmed_at       timestamptz
);

CREATE TABLE outbox (
    id             uuid PRIMARY KEY,
    subject_type   text NOT NULL,
    subject_id     text NOT NULL,
    event_type     text NOT NULL,
    payload        jsonb NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now()
);

CREATE PUBLICATION site_outbox_pub FOR TABLE outbox WITH (publish = 'insert');

CREATE TABLE post_notification_subscribers (
    email        text PRIMARY KEY,
    token        uuid NOT NULL,
    confirmed_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE processed_events (
    event_id uuid PRIMARY KEY,
    processed_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE mail_jobs (
    id           bigserial PRIMARY KEY,
    event_id     uuid NOT NULL,
    recipient    text NOT NULL,
    kind         text NOT NULL CHECK (kind IN ('CONFIRMATION', 'POST')),
    subject      text NOT NULL,
    body         text NOT NULL,
    status       text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'CANCELED')),
    attempts     integer NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL DEFAULT now(),
    sent_at      timestamptz,
    UNIQUE (event_id, recipient)
);

-- Local development only. Production provisions a separate replication role and secret.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'neoguri95_relay') THEN
        CREATE ROLE neoguri95_relay WITH LOGIN REPLICATION PASSWORD 'neoguri95_relay';
    ELSE
        ALTER ROLE neoguri95_relay LOGIN REPLICATION PASSWORD 'neoguri95_relay';
    END IF;
END
$$;
GRANT USAGE ON SCHEMA public TO neoguri95_relay;
GRANT SELECT ON outbox TO neoguri95_relay;

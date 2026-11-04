-- CREATE DATABASE는 트랜잭션 안에서 실행할 수 없으므로 별도 연결에서 실행한다.
CREATE EXTENSION IF NOT EXISTS dblink;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'neoguri95_site') THEN
        PERFORM dblink_exec('dbname=postgres', 'CREATE DATABASE neoguri95_site');
    END IF;
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'neoguri95_email') THEN
        PERFORM dblink_exec('dbname=postgres', 'CREATE DATABASE neoguri95_email');
    END IF;
END
$$;

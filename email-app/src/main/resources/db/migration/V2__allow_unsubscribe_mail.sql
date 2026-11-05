ALTER TABLE mail_jobs DROP CONSTRAINT mail_jobs_kind_check;
ALTER TABLE mail_jobs ADD CONSTRAINT mail_jobs_kind_check
    CHECK (kind IN ('CONFIRMATION', 'POST', 'UNSUBSCRIBE'));

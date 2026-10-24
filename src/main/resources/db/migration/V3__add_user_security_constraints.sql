ALTER TABLE users
    ADD CONSTRAINT chk_failed_login_attempts_non_negative
        CHECK (failed_login_attempts >= 0);
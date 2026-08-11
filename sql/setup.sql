-- Issue Tracking System - one-time database setup.
--
-- Run this once as a MySQL administrator:
--     sudo mysql < sql/setup.sql
--
-- The services create their own tables at start-up (Hibernate ddl-auto: update) and seed
-- the reference workbook's sample rows into empty tables, so this script only has to
-- create the login and the four schemas.
--
-- Four schemas, not four servers: database-per-service is about ownership, not hardware
-- (SRS A-02). No service may read another's tables, and nothing here grants it the
-- ability to - the isolation is a discipline enforced by the code, and the schema split
-- makes a violation obvious in a query log.
--
-- The password satisfies MySQL's default validate_password policy (MEDIUM): at least
-- eight characters with upper case, lower case, a digit and a special character. A
-- simpler one is rejected outright with ERROR 1819. It is a development credential and
-- is committed deliberately so a clean checkout runs; anything real belongs in .env,
-- which is gitignored.

CREATE USER IF NOT EXISTS 'its'@'localhost' IDENTIFIED BY 'Its#Tracker2026!';

-- Re-assert the password, so re-running this after a failed or partial attempt fixes
-- an account that was created with something else rather than silently leaving it.
ALTER USER 'its'@'localhost' IDENTIFIED BY 'Its#Tracker2026!';

CREATE DATABASE IF NOT EXISTS user_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS project_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS issue_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS comment_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON user_db.*    TO 'its'@'localhost';
GRANT ALL PRIVILEGES ON project_db.* TO 'its'@'localhost';
GRANT ALL PRIVILEGES ON issue_db.*   TO 'its'@'localhost';
GRANT ALL PRIVILEGES ON comment_db.* TO 'its'@'localhost';

FLUSH PRIVILEGES;

SELECT 'Databases and the its user are ready.' AS status;

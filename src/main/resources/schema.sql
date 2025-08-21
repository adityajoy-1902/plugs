-- Oracle Database Schema for Dashboard User Authentication
-- Run this script as a database administrator

-- Create tablespace for dashboard application (optional)
-- CREATE TABLESPACE dashboard_ts
-- DATAFILE 'dashboard_ts.dbf' SIZE 100M
-- AUTOEXTEND ON NEXT 10M;

-- Create user for dashboard application
-- CREATE USER dashboard_user IDENTIFIED BY dashboard_pass
-- DEFAULT TABLESPACE dashboard_ts
-- QUOTA UNLIMITED ON dashboard_ts;

-- Grant necessary privileges
-- GRANT CONNECT, RESOURCE TO dashboard_user;
-- GRANT CREATE SESSION TO dashboard_user;
-- GRANT CREATE TABLE TO dashboard_user;
-- GRANT CREATE SEQUENCE TO dashboard_user;

-- Connect as dashboard_user and create tables
-- CONNECT dashboard_user/dashboard_pass

-- Create USERS table
CREATE TABLE USERS (
    USER_ID NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    USERNAME VARCHAR2(50) UNIQUE NOT NULL,
    PASSWORD VARCHAR2(255) NOT NULL,
    ROLE VARCHAR2(50) NOT NULL,
    ENABLED NUMBER(1) DEFAULT 1 NOT NULL,
    ACCOUNT_NON_EXPIRED NUMBER(1) DEFAULT 1 NOT NULL,
    ACCOUNT_NON_LOCKED NUMBER(1) DEFAULT 1 NOT NULL,
    CREDENTIALS_NON_EXPIRED NUMBER(1) DEFAULT 1 NOT NULL
);

-- Create indexes for better performance
CREATE INDEX idx_users_username ON USERS(USERNAME);
CREATE INDEX idx_users_role ON USERS(ROLE);

-- Insert default users (passwords will be BCrypt encoded by the application)
-- Note: These are just placeholders, the actual BCrypt encoded passwords will be inserted by the application

-- Create sequence for USER_ID (if not using IDENTITY)
-- CREATE SEQUENCE users_seq START WITH 1 INCREMENT BY 1;

-- Grant permissions to the application user
GRANT SELECT, INSERT, UPDATE, DELETE ON USERS TO dashboard_user;
GRANT SELECT ON users_seq TO dashboard_user;

-- Commit the changes
COMMIT;

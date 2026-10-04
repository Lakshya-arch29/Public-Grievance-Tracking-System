-- JanSunwai schema. Safe to run repeatedly (IF NOT EXISTS everywhere).

CREATE TABLE IF NOT EXISTS city (
    city_id  SERIAL PRIMARY KEY,
    name     VARCHAR(80) NOT NULL UNIQUE,
    state    VARCHAR(80) NOT NULL
);

CREATE TABLE IF NOT EXISTS category (
    category_id SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    user_id       SERIAL PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    username      VARCHAR(30)  NOT NULL UNIQUE,
    email         VARCHAR(120) NOT NULL UNIQUE,
    phone         VARCHAR(15)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(10)  NOT NULL
                  CHECK (role IN ('CITIZEN', 'OFFICER', 'ADMIN')),
    city_id       INT REFERENCES city(city_id),
    category_id   INT REFERENCES category(category_id),
    active        BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CHECK (role <> 'OFFICER' OR (city_id IS NOT NULL AND category_id IS NOT NULL))
);

CREATE TABLE IF NOT EXISTS grievance (
    grievance_id        SERIAL PRIMARY KEY,
    tracking_id         VARCHAR(20) GENERATED ALWAYS AS
                        ('GRV-' || LPAD(grievance_id::text, 5, '0')) STORED UNIQUE,
    user_id             INT NOT NULL REFERENCES users(user_id),
    category_id         INT NOT NULL REFERENCES category(category_id),
    city_id             INT NOT NULL REFERENCES city(city_id),
    area                VARCHAR(120) NOT NULL,
    title               VARCHAR(150) NOT NULL,
    description         TEXT NOT NULL,
    priority            VARCHAR(10) NOT NULL DEFAULT 'MEDIUM'
                        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    status              VARCHAR(12) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'RESOLVED', 'REJECTED')),
    assigned_officer_id INT REFERENCES users(user_id),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at         TIMESTAMP
);

CREATE TABLE IF NOT EXISTS grievance_history (
    history_id   SERIAL PRIMARY KEY,
    grievance_id INT NOT NULL REFERENCES grievance(grievance_id) ON DELETE CASCADE,
    status       VARCHAR(12) NOT NULL
                 CHECK (status IN ('PENDING', 'IN_PROGRESS', 'RESOLVED', 'REJECTED')),
    remark       VARCHAR(500) NOT NULL,
    changed_by   INT REFERENCES users(user_id),
    changed_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS auth_token (
    token      VARCHAR(64) PRIMARY KEY,
    user_id    INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_grievance_user        ON grievance(user_id);
CREATE INDEX IF NOT EXISTS idx_grievance_officer     ON grievance(assigned_officer_id);
CREATE INDEX IF NOT EXISTS idx_grievance_status      ON grievance(status);
CREATE INDEX IF NOT EXISTS idx_grievance_city_status ON grievance(city_id, status);
CREATE INDEX IF NOT EXISTS idx_history_grievance     ON grievance_history(grievance_id);

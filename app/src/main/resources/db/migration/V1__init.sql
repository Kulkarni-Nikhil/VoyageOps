-- VoyageOps initial schema

CREATE TABLE users (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255),
    age         INTEGER,
    email       VARCHAR(255) NOT NULL UNIQUE,
    created_at  TIMESTAMP WITH TIME ZONE,
    updated_at  TIMESTAMP WITH TIME ZONE
);

CREATE TABLE trip (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    budget      NUMERIC(12, 2),
    start_date  DATE,
    end_date    DATE,
    status      VARCHAR(20),
    owner_id    UUID REFERENCES users (id),
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE,
    updated_at  TIMESTAMP WITH TIME ZONE
);

CREATE TABLE destination (
    id          BIGSERIAL PRIMARY KEY,
    city        VARCHAR(255),
    country     VARCHAR(255),
    arrival     DATE,
    departure   DATE,
    trip_id     BIGINT NOT NULL REFERENCES trip (id)
);

CREATE TABLE activity (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    description   VARCHAR(1000),
    cost          NUMERIC(12, 2),
    status        VARCHAR(20),
    scheduled_for DATE,
    trip_id       BIGINT NOT NULL REFERENCES trip (id)
);

CREATE TABLE trip_member (
    id        UUID PRIMARY KEY,
    trip_id   BIGINT NOT NULL REFERENCES trip (id),
    user_id   UUID NOT NULL REFERENCES users (id),
    role      VARCHAR(20),
    joined_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_trip_member_trip_user UNIQUE (trip_id, user_id)
);

CREATE TABLE invitation (
    id            UUID PRIMARY KEY,
    trip_id       BIGINT NOT NULL REFERENCES trip (id),
    invited_by    UUID REFERENCES users (id),
    invited_email VARCHAR(255),
    status        VARCHAR(20),
    token         VARCHAR(64) UNIQUE,
    expires_at    TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE
);

CREATE TABLE expense (
    id          BIGSERIAL PRIMARY KEY,
    trip_id     BIGINT NOT NULL REFERENCES trip (id),
    paid_by     UUID REFERENCES users (id),
    amount      NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    category    VARCHAR(30),
    description VARCHAR(255),
    spent_on    DATE,
    created_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_destination_trip_id ON destination (trip_id);
CREATE INDEX idx_activity_trip_id ON activity (trip_id);
CREATE INDEX idx_expense_trip_id ON expense (trip_id);
CREATE INDEX idx_trip_member_trip_id ON trip_member (trip_id);
CREATE INDEX idx_invitation_trip_id ON invitation (trip_id);
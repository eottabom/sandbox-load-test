CREATE TABLE IF NOT EXISTS scenarios (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    engine      VARCHAR(20)  NOT NULL,
    content     TEXT         NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS runs (
    id          VARCHAR(36) NOT NULL PRIMARY KEY,
    scenario_id VARCHAR(36) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    started_at  TIMESTAMP   NOT NULL,
    finished_at TIMESTAMP,
    CONSTRAINT fk_runs_scenarios
        FOREIGN KEY (scenario_id) REFERENCES scenarios(id)
);

CREATE INDEX IF NOT EXISTS idx_scenarios_created_at ON scenarios(created_at);
CREATE INDEX IF NOT EXISTS idx_scenarios_created_at_id ON scenarios(created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_runs_scenario_id ON runs(scenario_id);
CREATE INDEX IF NOT EXISTS idx_runs_status      ON runs(status);
CREATE INDEX IF NOT EXISTS idx_runs_started_at  ON runs(started_at);
CREATE INDEX IF NOT EXISTS idx_runs_started_at_id ON runs(started_at DESC, id DESC);

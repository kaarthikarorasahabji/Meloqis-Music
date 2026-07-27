CREATE TABLE IF NOT EXISTS installations (
    install_hash TEXT PRIMARY KEY,
    first_seen TEXT NOT NULL,
    last_seen TEXT NOT NULL,
    first_open_at TEXT,
    first_open_kind TEXT,
    app_version TEXT NOT NULL,
    version_code INTEGER NOT NULL,
    android_version TEXT NOT NULL,
    sdk_int INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS events (
    event_id TEXT PRIMARY KEY,
    install_hash TEXT NOT NULL,
    event_name TEXT NOT NULL,
    created_at TEXT NOT NULL,
    app_version TEXT NOT NULL,
    version_code INTEGER NOT NULL,
    android_version TEXT NOT NULL,
    sdk_int INTEGER NOT NULL,
    detail_code TEXT,
    from_version TEXT,
    to_version TEXT
);

CREATE INDEX IF NOT EXISTS idx_events_created_at
    ON events(created_at);

CREATE INDEX IF NOT EXISTS idx_events_name_created_at
    ON events(event_name, created_at);

CREATE INDEX IF NOT EXISTS idx_events_install_created_at
    ON events(install_hash, created_at);

CREATE TABLE IF NOT EXISTS download_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TEXT NOT NULL,
    app_version TEXT NOT NULL,
    source TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_download_events_created_at
    ON download_events(created_at);

CREATE TABLE IF NOT EXISTS login_attempts (
    fingerprint TEXT PRIMARY KEY,
    failures INTEGER NOT NULL DEFAULT 0,
    first_failure_at TEXT NOT NULL,
    blocked_until TEXT
);

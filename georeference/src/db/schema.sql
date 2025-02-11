DROP TABLE IF EXISTS resolutions;

CREATE TABLE resolutions (
    address TEXT PRIMARY KEY NOT NULL,
    complete_address TEXT NOT NULL UNIQUE,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    access_counter INTEGER NOT NULL
);
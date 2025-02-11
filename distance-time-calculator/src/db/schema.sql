DROP TABLE IF EXISTS distances;

CREATE TABLE distances (
    origin TEXT NOT NULL,
    destination TEXT NOT NULL,
    distance REAL NOT NULL,
    duration REAL NOT NULL,
    access_counter INTEGER NOT NULL,
    PRIMARY KEY (origin, destination)
);

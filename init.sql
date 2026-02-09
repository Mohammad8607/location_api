CREATE TABLE IF NOT EXISTS locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Generate 1000 random locations using a recursive CTE
INSERT INTO locations (name, latitude, longitude)
WITH RECURSIVE counter AS (
    SELECT 1 AS i
    UNION ALL
    SELECT i + 1 FROM counter WHERE i < 1000
)
SELECT
    CONCAT('Location ', i),
    ROUND(-90 + (RAND() * 180), 8),
    ROUND(-180 + (RAND() * 360), 8)
FROM counter;

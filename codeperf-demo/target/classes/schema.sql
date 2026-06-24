CREATE TABLE IF NOT EXISTS users (
    id   BIGINT PRIMARY KEY,
    name VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS orders (
    id        BIGINT PRIMARY KEY,
    user_id   BIGINT,
    item_name VARCHAR(100),
    amount    DECIMAL(12, 2)
);

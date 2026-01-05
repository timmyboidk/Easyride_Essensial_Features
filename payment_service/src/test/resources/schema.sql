CREATE TABLE IF NOT EXISTS withdrawals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id BIGINT,
    amount INT,
    status VARCHAR(50),
    notes VARCHAR(255),
    request_time TIMESTAMP,
    completion_time TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT
);
 
CREATE TABLE IF NOT EXISTS wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    driver_id BIGINT,
    balance INT,
    currency VARCHAR(10),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT
);
 
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    user_id BIGINT,
    driver_id BIGINT,
    amount INT,
    refunded_amount INT,
    currency VARCHAR(10),
    transaction_id VARCHAR(100),
    payment_gateway VARCHAR(50),
    payment_method VARCHAR(50),
    status VARCHAR(50),
    transaction_type VARCHAR(50),
    paid_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT
);

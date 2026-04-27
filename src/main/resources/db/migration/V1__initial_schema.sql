CREATE TABLE app_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    roles VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_users_username UNIQUE (username),
    CONSTRAINT uk_app_users_email UNIQUE (email)
);

CREATE TABLE jellyfin_user (
    id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NULL,
    policy_is_disabled BIT(1) NOT NULL,
    policy_authentication_provider_id VARCHAR(255) NULL,
    policy_password_reset_provider_id VARCHAR(255) NULL,
    PRIMARY KEY (id)
);

CREATE TABLE user_expiration (
    jellyfin_user_id VARCHAR(255) NOT NULL,
    username VARCHAR(255) NULL,
    expiry_date TIMESTAMP NULL,
    processed BIT(1) NOT NULL,
    PRIMARY KEY (jellyfin_user_id)
);

ALTER TABLE jellyfin_user
    ADD COLUMN IF NOT EXISTS policy_authentication_provider_id VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS policy_password_reset_provider_id VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS policy_is_disabled BIT(1) NOT NULL DEFAULT b'0';

UPDATE jellyfin_user
SET policy_authentication_provider_id = COALESCE(
        policy_authentication_provider_id,
        'com.jellyfin.authentication.providers.DefaultAuthenticationProvider'
    ),
    policy_password_reset_provider_id = COALESCE(
        policy_password_reset_provider_id,
        'com.jellyfin.passwordreset.providers.DefaultPasswordResetProvider'
    )
WHERE policy_authentication_provider_id IS NULL
   OR policy_password_reset_provider_id IS NULL;

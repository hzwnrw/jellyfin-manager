package com.hzwnrw.jellyfin.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPolicy {
    @Column(name = "policy_is_disabled", nullable = false)
    @JsonProperty("IsDisabled")
    private boolean isDisabled;

    // Jellyfin requires these fields for validation
    @Column(name = "policy_authentication_provider_id", length = 255)
    @JsonProperty("AuthenticationProviderId")
    private String authenticationProviderId = "com.jellyfin.authentication.providers.DefaultAuthenticationProvider";

    @Column(name = "policy_password_reset_provider_id", length = 255)
    @JsonProperty("PasswordResetProviderId")
    private String passwordResetProviderId = "com.jellyfin.passwordreset.providers.DefaultPasswordResetProvider";
}

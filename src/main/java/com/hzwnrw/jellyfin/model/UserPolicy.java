package com.hzwnrw.jellyfin.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPolicy {
    @JsonProperty("IsDisabled")
    private boolean isDisabled;

    // Jellyfin requires these fields for validation
    @JsonProperty("AuthenticationProviderId")
    private String authenticationProviderId = "com.jellyfin.authentication.providers.DefaultAuthenticationProvider";

    @JsonProperty("PasswordResetProviderId")
    private String passwordResetProviderId = "com.jellyfin.passwordreset.providers.DefaultPasswordResetProvider";
}

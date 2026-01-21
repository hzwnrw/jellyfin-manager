package com.hzwnrw.jellyfin.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Embedded;
import lombok.Data;

@Entity
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JellyfinUser {
    @Id
    @JsonProperty("Id")
    private String id;

    @JsonProperty("Name")
    private String name;

    @Embedded
    @JsonProperty("Policy")
    private UserPolicy policy = new UserPolicy();
}
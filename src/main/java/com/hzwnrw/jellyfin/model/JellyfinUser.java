package com.hzwnrw.jellyfin.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Embedded;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "jellyfin_user")
@JsonIgnoreProperties(ignoreUnknown = true)
public class JellyfinUser {
    @Id
    @Column(name = "id", nullable = false, length = 255)
    @JsonProperty("Id")
    private String id;

    @Column(name = "name", length = 255)
    @JsonProperty("Name")
    private String name;

    @Embedded
    @JsonProperty("Policy")
    private UserPolicy policy = new UserPolicy();
}

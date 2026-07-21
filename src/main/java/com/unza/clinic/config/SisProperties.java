package com.unza.clinic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Binds all app.sis.* keys from application.properties.
 * Configures the connection to the UNZA Online Application Portal / student
 * records system (devoap.unza.zm) that supplies student metadata for clinic
 * patient registration.
 *
 * Unlike the HR system, this API uses a single long-lived Bearer token
 * (no login/refresh flow) supplied directly by the provider.
 */
@Configuration
@ConfigurationProperties(prefix = "app.sis")
public class SisProperties {

    /** Base URL of the student records system, e.g. https://devoap.unza.zm */
    private String baseUrl = "";

    /** Static Bearer token issued by the provider — does not expire/rotate. */
    private String token = "";

    /** Known campus/programme instances to search when the caller doesn't specify one. */
    private List<String> instances = List.of("UG", "PG", "GSB", "IDE", "ZOU", "ecampus");

    public String getBaseUrl()         { return baseUrl; }
    public void   setBaseUrl(String v) { this.baseUrl = v == null ? "" : v.trim(); }

    public String getToken()         { return token; }
    public void   setToken(String v) { this.token = v == null ? "" : v.trim(); }

    public List<String> getInstances()          { return instances; }
    public void         setInstances(List<String> v) { this.instances = v == null || v.isEmpty() ? instances : v; }

    public boolean isConfigured() {
        return !baseUrl.isBlank() && !token.isBlank();
    }
}

package com.unza.clinic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds all app.counseling.* keys from application.properties.
 * Registering this class eliminates IDE "unknown property" warnings and
 * gives a single typed object that controllers and services can inject.
 */
@Configuration
@ConfigurationProperties(prefix = "app.counseling")
public class CounselingProperties {

    /** Base URL of the counseling system, e.g. https://counselling.unza.ac.zm */
    private String baseUrl = "";

    /** Username of the service account in the counseling system (ROLE_COUNSELOR). */
    private String serviceUsername = "";

    /** Password for the service account. */
    private String servicePassword = "";

    /**
     * Optional static token override.
     * When non-blank this is used directly instead of auto-login.
     * Useful for testing; not recommended in production (tokens expire after 1 h).
     */
    private String serviceToken = "";

    public String getBaseUrl()            { return baseUrl; }
    public void   setBaseUrl(String v)    { this.baseUrl = v == null ? "" : v.trim(); }

    public String getServiceUsername()         { return serviceUsername; }
    public void   setServiceUsername(String v) { this.serviceUsername = v == null ? "" : v; }

    public String getServicePassword()         { return servicePassword; }
    public void   setServicePassword(String v) { this.servicePassword = v == null ? "" : v; }

    public String getServiceToken()         { return serviceToken; }
    public void   setServiceToken(String v) { this.serviceToken = v == null ? "" : v; }

    public boolean isConfigured() {
        return !baseUrl.isBlank();
    }
}

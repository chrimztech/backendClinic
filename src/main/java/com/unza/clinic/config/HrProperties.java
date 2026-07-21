package com.unza.clinic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds all app.hr.* keys from application.properties.
 * Configures the connection to the UNZA HR system (devhr.unza.zm) that
 * supplies staff and dependents metadata for clinic patient registration.
 */
@Configuration
@ConfigurationProperties(prefix = "app.hr")
public class HrProperties {

    /** Base URL of the HR system, e.g. http://devhr.unza.zm */
    private String baseUrl = "";

    /** Service-account username used for auto-login (e.g. clinic.test@unza.ac.zm). */
    private String serviceUsername = "";

    /** Password for the service account. */
    private String servicePassword = "";

    /**
     * Optional static token override.
     * When non-blank this is used directly instead of auto-login.
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

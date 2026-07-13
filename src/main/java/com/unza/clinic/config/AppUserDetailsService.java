package com.unza.clinic.config;

import com.unza.clinic.service.ClinicDataStore;
import com.unza.clinic.model.AppUser;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final ClinicDataStore dataStore;

    public AppUserDetailsService(ClinicDataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = dataStore.findUserByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        List<org.springframework.security.core.GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(() -> "ROLE_" + user.getRole());

        // Parse permissions from JSON if available
        if (user.getPermissionsJson() != null && !user.getPermissionsJson().isEmpty()) {
            try {
                // Simple parsing for comma-separated JSON array
                String json = user.getPermissionsJson();
                if (json.startsWith("[") && json.endsWith("]")) {
                    json = json.substring(1, json.length() - 1);
                    String[] parts = json.split(",");
                    for (String part : parts) {
                        String perm = part.replace("\"", "").trim();
                        if (!perm.isEmpty()) {
                            authorities.add(() -> perm);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore permission parsing errors, user still has ROLE
            }
        }

        return User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(!user.getStatus().equalsIgnoreCase("active"))
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}

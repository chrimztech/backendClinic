package com.unza.clinic.util;

import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordHasher {

    private static PasswordEncoder encoder;

    public static void setEncoder(PasswordEncoder encoder) {
        PasswordHasher.encoder = encoder;
    }

    public static String hash(String rawPassword) {
        if (encoder == null) {
            throw new IllegalStateException("PasswordEncoder not initialized");
        }
        return encoder.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String encoded) {
        if (encoder == null) {
            return rawPassword.equals(encoded); // fallback
        }
        return encoder.matches(rawPassword, encoded);
    }
}

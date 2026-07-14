-- V1 inserted a demo admin account with a plaintext (unhashed) password.
-- Remove it now that a real admin account is in use. Guarded on the original
-- plaintext password so this is a no-op if the row was already changed/claimed.
DELETE FROM app_users WHERE email = 'admin@unza.zm' AND password = 'admin123';

package com.officetime.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {

    /** bcrypt only looks at the first 72 bytes, so a longer password would be silently cut short. */
    private static final int MAX_PASSWORD_BYTES = 72;

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /** Creates the account. The returned principal still carries the password hash; the caller wipes it. */
    public AppUserPrincipal register(String rawEmail, String displayName, String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "That password is too long. Use at most " + MAX_PASSWORD_BYTES + " bytes.");
        }

        String email = Emails.normalize(rawEmail);
        if (users.findByEmail(email).isPresent()) {
            throw emailTaken();
        }
        try {
            AppUser saved = users.saveAndFlush(
                    new AppUser(email, displayName.strip(), passwordEncoder.encode(password)));
            return AppUserPrincipal.from(saved);
        } catch (DataIntegrityViolationException e) {
            // Someone registered the same address between the check above and the insert.
            throw emailTaken();
        }
    }

    private static ResponseStatusException emailTaken() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
    }
}

package com.officetime.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /** Expects an address already passed through {@link Emails#normalize(String)}. */
    Optional<AppUser> findByEmail(String email);
}

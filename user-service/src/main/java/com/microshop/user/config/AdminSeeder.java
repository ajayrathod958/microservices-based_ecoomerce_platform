package com.microshop.user.config;

import com.microshop.user.entity.Role;
import com.microshop.user.entity.User;
import com.microshop.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/** Creates the first admin account on startup (self-registration can never grant ADMIN). */
@Slf4j
@Component
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public AdminSeeder(UserRepository users, PasswordEncoder encoder,
                       @Value("${app.admin.email}") String email,
                       @Value("${app.admin.password}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email.trim().toLowerCase();
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByEmail(email)) {
            return;
        }
        users.save(User.builder()
                .email(email)
                .passwordHash(encoder.encode(password))
                .firstName("System")
                .lastName("Admin")
                .roles(new HashSet<>(Set.of(Role.ADMIN, Role.USER)))
                .build());
        log.warn("Seeded admin account '{}'. Change ADMIN_PASSWORD / the password for any non-dev environment.", email);
    }
}

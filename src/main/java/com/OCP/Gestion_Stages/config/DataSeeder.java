package com.OCP.Gestion_Stages.config;

import com.OCP.Gestion_Stages.Repository.UserRepository;
import com.OCP.Gestion_Stages.domain.enums.UserRole;
import com.OCP.Gestion_Stages.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seed un compte RH par défaut au démarrage si aucun n'existe.
 * Idempotent : ne crée rien si 'admin.rh' est déjà présent.
 * Désactivable via app.seed.admin=false (actif par défaut).
 */
@Component
@ConditionalOnProperty(name = "app.seed.admin", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername("admin.rh")) {
            log.info("Seeder : 'admin.rh' existe déjà — aucune création.");
            return;
        }
        userRepository.save(User.builder()
                .username("admin.rh")
                .email("admin.rh@ocp.ma")
                .password(passwordEncoder.encode("test123"))
                .role(UserRole.ADMIN_RH)
                .actif(true)
                .build());
        log.warn("Seeder : compte 'admin.rh' / 'test123' (ADMIN_RH) créé.");
    }
}

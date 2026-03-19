package com.ap101gamestudio.timetracker.config;

import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String adminEmail = "admin@ap101.com";

        User admin = userRepository.findByEmail(adminEmail)
                .orElseGet(() -> new User(adminEmail, passwordEncoder.encode("admin123"), "Super Admin"));

        if (!admin.isSystemAdmin()) {
            admin.setSystemAdmin(true);

            userRepository.save(admin);
            System.out.println("Super Admin criado com sucesso: " + adminEmail + " / admin123");
        }
    }
}
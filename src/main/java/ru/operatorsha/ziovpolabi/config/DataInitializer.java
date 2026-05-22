package ru.operatorsha.ziovpolabi.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.operatorsha.ziovpolabi.user.Role;
import ru.operatorsha.ziovpolabi.user.UserAccount;
import ru.operatorsha.ziovpolabi.user.UserAccountRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner createDefaultAdmin(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        return args -> {
            String email = "admin@example.com";
            if (users.existsByEmail(email)) {
                return;
            }

            UserAccount admin = new UserAccount();
            admin.setName("Administrator");
            admin.setEmail(email);
            admin.setPasswordHash(passwordEncoder.encode("Admin123!"));
            admin.setRole(Role.ADMIN);
            users.save(admin);
        };
    }
}

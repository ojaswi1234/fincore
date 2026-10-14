package com.fincore.config;

import com.fincore.model.*;
import com.fincore.repository.AccountRepository;
import com.fincore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking demo data initialization...");

        // Ensure Admin user
        User admin = userRepository.findByUsername("admin").orElseGet(() -> {
            User u = new User("admin", "admin@fincore.internal", passwordEncoder.encode("Admin@123"), "System Administrator", Role.ROLE_ADMIN);
            return userRepository.save(u);
        });
        if (!admin.getPasswordHash().startsWith("$2a$")) {
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            userRepository.save(admin);
        }

        // Ensure Customer 1: john_doe
        User john = userRepository.findByUsername("john_doe").orElseGet(() -> {
            User u = new User("john_doe", "john.doe@fincore.demo", passwordEncoder.encode("Customer@123"), "John Doe", Role.ROLE_CUSTOMER);
            return userRepository.save(u);
        });
        john.setPasswordHash(passwordEncoder.encode("Customer@123"));
        userRepository.save(john);

        // Ensure Customer 2: jane_smith
        User jane = userRepository.findByUsername("jane_smith").orElseGet(() -> {
            User u = new User("jane_smith", "jane.smith@fincore.demo", passwordEncoder.encode("Customer@123"), "Jane Smith", Role.ROLE_CUSTOMER);
            return userRepository.save(u);
        });
        jane.setPasswordHash(passwordEncoder.encode("Customer@123"));
        userRepository.save(jane);

        // Ensure Accounts
        if (accountRepository.findByAccountNumber("ACC-10001").isEmpty()) {
            Account acc1 = new Account("ACC-10001", john, AccountType.CHECKING, "INR", new BigDecimal("75000.00"), AccountStatus.ACTIVE);
            accountRepository.save(acc1);
        }
        if (accountRepository.findByAccountNumber("ACC-10002").isEmpty()) {
            Account acc2 = new Account("ACC-10002", john, AccountType.SAVINGS, "INR", new BigDecimal("150000.00"), AccountStatus.ACTIVE);
            accountRepository.save(acc2);
        }
        if (accountRepository.findByAccountNumber("ACC-20001").isEmpty()) {
            Account acc3 = new Account("ACC-20001", jane, AccountType.CHECKING, "INR", new BigDecimal("45000.00"), AccountStatus.ACTIVE);
            accountRepository.save(acc3);
        }

        log.info("Demo data verified. Credentials: admin / Admin@123 | john_doe / Customer@123 | jane_smith / Customer@123");
    }
}

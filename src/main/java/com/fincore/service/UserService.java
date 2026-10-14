package com.fincore.service;

import com.fincore.dto.UserRegistrationDto;
import com.fincore.exception.FinCoreException;
import com.fincore.model.Account;
import com.fincore.model.AccountStatus;
import com.fincore.model.AccountType;
import com.fincore.model.Role;
import com.fincore.model.User;
import com.fincore.repository.AccountRepository;
import com.fincore.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserService(UserRepository userRepository, AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(UserRegistrationDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new FinCoreException("Passwords do not match");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new FinCoreException("Username already taken: " + dto.getUsername());
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new FinCoreException("Email already registered: " + dto.getEmail());
        }

        User user = new User(
                dto.getUsername(),
                dto.getEmail(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getFullName(),
                Role.ROLE_CUSTOMER
        );
        User savedUser = userRepository.save(user);

        // Automatically provision a default Checking account with initial welcome balance
        String generatedAccNumber = "ACC-" + (10000 + secureRandom.nextInt(90000));
        Account defaultAccount = new Account(
                generatedAccNumber,
                savedUser,
                AccountType.CHECKING,
                "INR",
                new BigDecimal("25000.00"),
                AccountStatus.ACTIVE
        );
        accountRepository.save(defaultAccount);

        return savedUser;
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new FinCoreException("User not found: " + username));
    }
}

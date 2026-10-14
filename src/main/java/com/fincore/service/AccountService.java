package com.fincore.service;

import com.fincore.dto.AccountDto;
import com.fincore.exception.AccountNotFoundException;
import com.fincore.model.Account;
import com.fincore.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAccountsByUsername(String username) {
        return accountRepository.findByUserUsername(username).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountEntitiesByUsername(String username) {
        return accountRepository.findByUserUsername(username);
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountDtoByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        return toDto(account);
    }

    @Transactional(readOnly = true)
    public Account getAccountEntityByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }

    public AccountDto toDto(Account a) {
        return new AccountDto(
                a.getId(),
                a.getAccountNumber(),
                a.getAccountType().name(),
                a.getCurrency(),
                a.getBalance(),
                a.getStatus().name(),
                a.getUser().getUsername(),
                a.getUser().getFullName(),
                a.getCreatedAt()
        );
    }
}

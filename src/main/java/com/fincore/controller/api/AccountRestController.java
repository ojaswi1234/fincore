package com.fincore.controller.api;

import com.fincore.dto.AccountDto;
import com.fincore.exception.UnauthorizedAccountAccessException;
import com.fincore.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountRestController {

    private final AccountService accountService;

    public AccountRestController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<List<AccountDto>> getMyAccounts(@AuthenticationPrincipal UserDetails userDetails) {
        List<AccountDto> accounts = accountService.getAccountsByUsername(userDetails.getUsername());
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountDto> getAccountDetails(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal UserDetails userDetails) {

        AccountDto account = accountService.getAccountDtoByNumber(accountNumber);
        // Enforce ownership check unless admin
        boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !account.getOwnerUsername().equalsIgnoreCase(userDetails.getUsername())) {
            throw new UnauthorizedAccountAccessException("You are not authorized to view account " + accountNumber);
        }

        return ResponseEntity.ok(account);
    }
}

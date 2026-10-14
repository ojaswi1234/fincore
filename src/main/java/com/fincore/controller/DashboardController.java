package com.fincore.controller;

import com.fincore.dto.AccountDto;
import com.fincore.dto.TransferRequestDto;
import com.fincore.model.Role;
import com.fincore.model.TransactionRecord;
import com.fincore.model.User;
import com.fincore.repository.TransactionRepository;
import com.fincore.service.AccountService;
import com.fincore.service.NotificationService;
import com.fincore.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
public class DashboardController {

    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final UserService userService;

    public DashboardController(AccountService accountService,
                               TransactionRepository transactionRepository,
                               NotificationService notificationService,
                               UserService userService) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        if (user.getRole() == Role.ROLE_ADMIN) {
            return "redirect:/admin/dashboard";
        }

        List<AccountDto> accounts = accountService.getAccountsByUsername(user.getUsername());
        BigDecimal totalBalance = accounts.stream()
                .map(AccountDto::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TransactionRecord> recentTransactions = transactionRepository.findTop10ByUserId(user.getId());

        TransferRequestDto transferDto = new TransferRequestDto();
        transferDto.setIdempotencyKey(UUID.randomUUID().toString());

        model.addAttribute("user", user);
        model.addAttribute("accounts", accounts);
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("notifications", notificationService.getRecentNotifications(user.getId()));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user.getId()));
        model.addAttribute("transferDto", transferDto);

        return "dashboard";
    }
}

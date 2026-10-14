package com.fincore.controller;

import com.fincore.model.TransactionRecord;
import com.fincore.model.User;
import com.fincore.repository.TransactionRepository;
import com.fincore.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TransactionViewController {

    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public TransactionViewController(TransactionRepository transactionRepository, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    @GetMapping("/transactions")
    public String listTransactions(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Model model) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        Page<TransactionRecord> transactionPage = transactionRepository.findByUserId(
                user.getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        model.addAttribute("transactionPage", transactionPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactionPage.getTotalPages());
        return "transactions";
    }
}

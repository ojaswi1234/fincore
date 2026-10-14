package com.fincore.controller;

import com.fincore.dto.AccountDto;
import com.fincore.dto.TransferRequestDto;
import com.fincore.dto.TransferResponseDto;
import com.fincore.exception.FinCoreException;
import com.fincore.service.AccountService;
import com.fincore.service.TransferService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
public class TransferController {

    private final TransferService transferService;
    private final AccountService accountService;

    public TransferController(TransferService transferService, AccountService accountService) {
        this.transferService = transferService;
        this.accountService = accountService;
    }

    @GetMapping("/transfers")
    public String transferPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<AccountDto> myAccounts = accountService.getAccountsByUsername(userDetails.getUsername());
        TransferRequestDto dto = new TransferRequestDto();
        dto.setIdempotencyKey(UUID.randomUUID().toString());

        model.addAttribute("myAccounts", myAccounts);
        model.addAttribute("transferDto", dto);
        return "transfers";
    }

    @PostMapping("/transfers")
    public String executeTransfer(@Valid @ModelAttribute("transferDto") TransferRequestDto transferDto,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("myAccounts", accountService.getAccountsByUsername(userDetails.getUsername()));
            return "transfers";
        }

        try {
            TransferResponseDto response = transferService.executeTransfer(
                    transferDto,
                    userDetails.getUsername(),
                    request.getRemoteAddr()
            );

            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Transfer of %s %s completed successfully! Reference: %s",
                            response.getAmount(), response.getCurrency(), response.getReferenceNumber()));
            if (response.isFlagged()) {
                redirectAttributes.addFlashAttribute("warningMessage",
                        "Transaction flagged by compliance monitoring for standard verification: " + response.getMessage());
            }
            return "redirect:/transfers";

        } catch (FinCoreException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/transfers";
        }
    }
}

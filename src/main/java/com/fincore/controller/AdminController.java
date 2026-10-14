package com.fincore.controller;

import com.fincore.model.AuditEvent;
import com.fincore.model.FraudAlert;
import com.fincore.model.FraudAlertStatus;
import com.fincore.model.TransactionRecord;
import com.fincore.repository.AccountRepository;
import com.fincore.repository.AuditEventRepository;
import com.fincore.repository.FraudAlertRepository;
import com.fincore.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final AccountRepository accountRepository;
    private final AuditEventRepository auditEventRepository;

    public AdminController(TransactionRepository transactionRepository,
                           FraudAlertRepository fraudAlertRepository,
                           AccountRepository accountRepository,
                           AuditEventRepository auditEventRepository) {
        this.transactionRepository = transactionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.accountRepository = accountRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @GetMapping({"", "/dashboard"})
    public String adminDashboard(Model model) {
        long totalAccounts = accountRepository.count();
        long totalTransactions = transactionRepository.count();
        long pendingAlerts = fraudAlertRepository.countByStatus(FraudAlertStatus.REVIEW_REQUIRED);

        List<TransactionRecord> recentTransactions = transactionRepository.findTop20ByOrderByCreatedAtDesc();
        List<FraudAlert> activeAlerts = fraudAlertRepository.findByStatusOrderByCreatedAtDesc(FraudAlertStatus.REVIEW_REQUIRED);

        model.addAttribute("totalAccounts", totalAccounts);
        model.addAttribute("totalTransactions", totalTransactions);
        model.addAttribute("pendingAlerts", pendingAlerts);
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("activeAlerts", activeAlerts);

        return "admin/dashboard";
    }

    @GetMapping("/alerts")
    public String listFraudAlerts(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  Model model) {
        Page<FraudAlert> alertsPage = fraudAlertRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size)
        );
        model.addAttribute("alertsPage", alertsPage);
        model.addAttribute("currentPage", page);
        return "admin/alerts";
    }

    @PostMapping("/alerts/{id}/resolve")
    public String resolveAlert(@PathVariable Long id,
                               @RequestParam String decision, // "RESOLVED" or "DISMISSED"
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        FraudAlert alert = fraudAlertRepository.findById(id).orElse(null);
        if (alert != null) {
            alert.setStatus("DISMISSED".equalsIgnoreCase(decision) ? FraudAlertStatus.DISMISSED : FraudAlertStatus.RESOLVED);
            alert.setResolvedBy(userDetails.getUsername());
            alert.setResolvedAt(Instant.now());
            fraudAlertRepository.save(alert);
            redirectAttributes.addFlashAttribute("successMessage", "Alert ID " + id + " marked as " + alert.getStatus());
        }
        return "redirect:/admin/alerts";
    }

    @GetMapping("/audit")
    public String auditLogs(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "15") int size,
                            Model model) {
        Page<AuditEvent> auditPage = auditEventRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size)
        );
        model.addAttribute("auditPage", auditPage);
        model.addAttribute("currentPage", page);
        return "admin/audit";
    }
}

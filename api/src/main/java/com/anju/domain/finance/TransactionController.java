package com.anju.domain.finance;

import com.anju.aspect.Auditable;
import com.anju.common.BusinessException;
import com.anju.common.Result;
import com.anju.domain.auth.AuthService;
import com.anju.domain.auth.CurrentUserService;
import com.anju.domain.finance.dto.InvoiceIssueRequest;
import com.anju.domain.finance.dto.RefundRequest;
import com.anju.domain.finance.dto.SettlementRequest;
import com.anju.domain.finance.dto.TransactionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/finance")
@Tag(name = "Finance", description = "Financial transaction, refund, settlement, and invoice endpoints")
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public TransactionController(TransactionService transactionService,
                                 AuthService authService,
                                 CurrentUserService currentUserService) {
        this.transactionService = transactionService;
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/bookkeeping")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "Bookkeeping transaction")
    public Result<Transaction> record(@RequestBody TransactionRequest request) {
        return Result.success(transactionService.recordTransaction(request));
    }
    
    @Auditable(module = "FINANCE", action = "Create transaction")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "Create transaction")
    public Result<Transaction> create(
            @Valid @RequestBody TransactionRequest request,
            @RequestHeader("X-Secondary-Password") String secondaryPassword,
            Authentication authentication) {
        authService.verifySecondaryPassword(authentication.getName(), secondaryPassword);
        return Result.success(transactionService.recordTransaction(request));
    }

    @GetMapping("/{transactionNo}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','AUDITOR')")
    @Operation(summary = "Get transaction by number")
    public Result<Transaction> getByTransactionNo(@PathVariable String transactionNo) {
        return Result.success(transactionService.getTransactionByNo(transactionNo));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','AUDITOR')")
    @Operation(summary = "List transactions")
    public Result<List<Transaction>> list() {
        if (!currentUserService.hasAnyRole("ADMIN", "FINANCE", "AUDITOR")) {
            throw new BusinessException(4030, "Forbidden.");
        }
        return Result.success(transactionService.listTransactions());
    }

    @Auditable(module = "FINANCE", action = "Create refund")
    @PostMapping("/{transactionNo}/refund")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "Create refund")
    public Result<Transaction> refund(@PathVariable String transactionNo, @Valid @RequestBody RefundRequest request) {
        return Result.success(transactionService.createRefund(transactionNo, request));
    }

    @Auditable(module = "FINANCE", action = "Daily settlement")
    @PostMapping("/settlements/daily")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "Run daily settlement")
    public Result<Map<String, Object>> settleDaily(@Valid @RequestBody SettlementRequest request) {
        return Result.success(transactionService.settleDaily(request));
    }

    @GetMapping("/statements/daily")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','AUDITOR')")
    @Operation(summary = "Get daily statement")
    public Result<Map<String, Object>> dailyStatement(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(transactionService.dailyStatement(date));
    }

    @Auditable(module = "FINANCE", action = "Request invoice")
    @PostMapping("/{transactionNo}/invoice/request")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "Request invoice")
    public Result<Transaction> requestInvoice(@PathVariable String transactionNo) {
        return Result.success(transactionService.requestInvoice(transactionNo));
    }

    @Auditable(module = "FINANCE", action = "Issue invoice")
    @PostMapping("/{transactionNo}/invoice/issue")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "Issue invoice")
    public Result<Transaction> issueInvoice(
            @PathVariable String transactionNo,
            @Valid @RequestBody InvoiceIssueRequest request,
            @RequestHeader("X-Secondary-Password") String secondaryPassword,
            Authentication authentication) {
        authService.verifySecondaryPassword(authentication.getName(), secondaryPassword);
        return Result.success(transactionService.issueInvoice(transactionNo, request));
    }
}

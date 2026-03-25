package com.anju.domain.finance;

import com.anju.common.BusinessException;
import com.anju.domain.auth.CurrentUserService;
import com.anju.domain.auth.User;
import com.anju.domain.finance.dto.InvoiceIssueRequest;
import com.anju.domain.finance.dto.RefundRequest;
import com.anju.domain.finance.dto.SettlementRequest;
import com.anju.domain.finance.dto.TransactionRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public TransactionService(TransactionRepository transactionRepository,
                              CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    public Transaction recordTransaction(TransactionRequest request) {
        if (transactionRepository.existsByTransactionNo(request.getTransactionNumber())) {
            throw new BusinessException(400, "Transaction already exists");
        }

        User currentUser = currentUserService.requireCurrentUser();
        Long payerId = resolvePayerId(currentUser, request.getPayerId());

        Transaction transaction = new Transaction();
        transaction.setTransactionNo(request.getTransactionNumber());
        transaction.setAppointmentId(request.getAppointmentId());
        transaction.setPayerId(payerId);
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setCurrency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency().trim().toUpperCase() : "USD");
        transaction.setStatus("SUCCESS");
        transaction.setRemark(request.getRemark());
        transaction.setOccurredAt(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    public Transaction getTransactionByNo(String transactionNo) {
        Transaction transaction = transactionRepository.findByTransactionNo(transactionNo)
                .orElseThrow(() -> new BusinessException(4040, "Transaction not found"));
        enforceTransactionAccess(transaction);
        return transaction;
    }

    public List<Transaction> listTransactions() {
        User currentUser = currentUserService.requireCurrentUser();
        if ("STAFF".equalsIgnoreCase(currentUser.getRole())) {
            return transactionRepository.findByPayerIdOrderByOccurredAtDesc(currentUser.getId());
        }
        return transactionRepository.findAll();
    }

    public Transaction createRefund(String transactionNo, RefundRequest request) {
        Transaction original = getTransactionByNo(transactionNo);
        if (!"PAYMENT".equalsIgnoreCase(original.getType())) {
            throw new BusinessException(4004, "Only PAYMENT transactions can be refunded.");
        }
        if (!"SUCCESS".equalsIgnoreCase(original.getStatus()) && !"SETTLED".equalsIgnoreCase(original.getStatus())) {
            throw new BusinessException(4005, "Only successful or settled transactions can be refunded.");
        }
        if (request.getAmount().compareTo(original.getAmount()) > 0) {
            throw new BusinessException(4006, "Refund amount cannot exceed original transaction amount.");
        }

        Transaction refund = new Transaction();
        refund.setTransactionNo(generateTransactionNo("RF"));
        refund.setAppointmentId(original.getAppointmentId());
        refund.setPayerId(original.getPayerId());
        refund.setType("REFUND");
        refund.setAmount(request.getAmount());
        refund.setCurrency(original.getCurrency());
        refund.setStatus("SUCCESS");
        refund.setOccurredAt(LocalDateTime.now());
        refund.setRelatedTransactionNo(original.getTransactionNo());
        refund.setRemark(request.getReason());
        return transactionRepository.save(refund);
    }

    public Map<String, Object> settleDaily(SettlementRequest request) {
        LocalDateTime start = request.getDate().atStartOfDay();
        LocalDateTime end = request.getDate().plusDays(1).atStartOfDay();
        List<Transaction> transactions = transactionRepository.findByStatusAndOccurredAtBetween("SUCCESS", start, end);

        BigDecimal totalAmount = BigDecimal.ZERO;
        int settledCount = 0;
        for (Transaction tx : transactions) {
            if (!"PAYMENT".equalsIgnoreCase(tx.getType())) {
                continue;
            }
            tx.setStatus("SETTLED");
            tx.setSettledAt(LocalDateTime.now());
            transactionRepository.save(tx);
            settledCount++;
            totalAmount = totalAmount.add(tx.getAmount());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("date", request.getDate());
        result.put("settledCount", settledCount);
        result.put("settledAmount", totalAmount);
        return result;
    }

    public Map<String, Object> dailyStatement(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<Transaction> transactions = transactionRepository.findByOccurredAtBetween(start, end);

        BigDecimal paymentTotal = BigDecimal.ZERO;
        BigDecimal refundTotal = BigDecimal.ZERO;
        int paymentCount = 0;
        int refundCount = 0;
        for (Transaction tx : transactions) {
            if ("PAYMENT".equalsIgnoreCase(tx.getType())) {
                paymentCount++;
                paymentTotal = paymentTotal.add(tx.getAmount());
            }
            if ("REFUND".equalsIgnoreCase(tx.getType())) {
                refundCount++;
                refundTotal = refundTotal.add(tx.getAmount());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("date", date);
        result.put("paymentCount", paymentCount);
        result.put("paymentTotal", paymentTotal);
        result.put("refundCount", refundCount);
        result.put("refundTotal", refundTotal);
        result.put("netAmount", paymentTotal.subtract(refundTotal));
        return result;
    }

    public Transaction requestInvoice(String transactionNo) {
        Transaction transaction = getTransactionByNo(transactionNo);
        if ("REQUESTED".equalsIgnoreCase(transaction.getInvoiceStatus()) || "ISSUED".equalsIgnoreCase(transaction.getInvoiceStatus())) {
            throw new BusinessException(4091, "Invoice is already requested or issued.");
        }
        transaction.setInvoiceStatus("REQUESTED");
        return transactionRepository.save(transaction);
    }

    public Transaction issueInvoice(String transactionNo, InvoiceIssueRequest request) {
        Transaction transaction = getTransactionByNo(transactionNo);
        if (!"REQUESTED".equalsIgnoreCase(transaction.getInvoiceStatus())) {
            throw new BusinessException(4007, "Invoice must be requested before issue.");
        }
        transaction.setInvoiceNo(request.getInvoiceNo().trim());
        transaction.setInvoiceStatus("ISSUED");
        return transactionRepository.save(transaction);
    }

    private Long resolvePayerId(User currentUser, Long payerIdFromRequest) {
        if ("ADMIN".equalsIgnoreCase(currentUser.getRole()) || "FINANCE".equalsIgnoreCase(currentUser.getRole())) {
            return payerIdFromRequest != null ? payerIdFromRequest : currentUser.getId();
        }
        if (payerIdFromRequest != null && !payerIdFromRequest.equals(currentUser.getId())) {
            throw new BusinessException(4032, "You cannot create transactions for another user.");
        }
        return currentUser.getId();
    }

    private void enforceTransactionAccess(Transaction transaction) {
        User currentUser = currentUserService.requireCurrentUser();
        if ("STAFF".equalsIgnoreCase(currentUser.getRole())
                && transaction.getPayerId() != null
                && !currentUser.getId().equals(transaction.getPayerId())) {
            throw new BusinessException(4033, "You are not allowed to access this transaction.");
        }
    }

    private String generateTransactionNo(String prefix) {
        for (int i = 0; i < 10; i++) {
            String candidate = prefix + "-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000, 10000);
            if (!transactionRepository.existsByTransactionNo(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(5001, "Unable to generate transaction number.");
    }
}

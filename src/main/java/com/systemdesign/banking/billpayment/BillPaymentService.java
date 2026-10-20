package com.systemdesign.banking.billpayment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemdesign.banking.account.Account;
import com.systemdesign.banking.account.AccountRepository;
import com.systemdesign.banking.ledger.LedgerEntry;
import com.systemdesign.banking.ledger.LedgerRepository;
import com.systemdesign.banking.notification.OutboxEvent;
import com.systemdesign.banking.notification.OutboxRepository;
import com.systemdesign.banking.systemdesign.cache.RedisCacheService;
import com.systemdesign.banking.systemdesign.idgenerator.UniqueIdGenerator;
import com.systemdesign.banking.transaction.Transaction;
import com.systemdesign.banking.transaction.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class BillPaymentService {

    private final BillerRepository billerRepository;
    private final BillPaymentRepository billPaymentRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final OutboxRepository outboxRepository;
    private final UniqueIdGenerator idGenerator;
    private final RedisCacheService cacheService;
    private final ObjectMapper objectMapper;

    private static final String BILLERS_ALL_CACHE_KEY = "cache:billers:all";

    public BillPaymentService(
            BillerRepository billerRepository,
            BillPaymentRepository billPaymentRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            LedgerRepository ledgerRepository,
            OutboxRepository outboxRepository,
            UniqueIdGenerator idGenerator,
            RedisCacheService cacheService,
            ObjectMapper objectMapper) {
        this.billerRepository = billerRepository;
        this.billPaymentRepository = billPaymentRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.outboxRepository = outboxRepository;
        this.idGenerator = idGenerator;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }

    /**
     * Cache-Aside implementation for Biller Catalogue.
     * Checks Redis first; if miss, fetches from MongoDB and populates Redis cache.
     */
    @SuppressWarnings("unchecked")
    public List<Biller> getAllBillers() {
        return cacheService.get(BILLERS_ALL_CACHE_KEY, List.class)
                .orElseGet(() -> {
                    List<Biller> billers = billerRepository.findAll();
                    cacheService.put(BILLERS_ALL_CACHE_KEY, billers, 600); // 10 minutes TTL
                    return billers;
                });
    }

    public List<Biller> getBillersByCategory(String category) {
        if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)) {
            return getAllBillers();
        }
        String cacheKey = "cache:billers:cat:" + category.toUpperCase();
        return cacheService.get(cacheKey, List.class)
                .orElseGet(() -> {
                    List<Biller> list = billerRepository.findByCategoryIgnoreCase(category);
                    cacheService.put(cacheKey, list, 600);
                    return list;
                });
    }

    /**
     * Atomic Bill Payment execution with multi-document transaction, double-entry ledger,
     * balance deduction, and outbox notification event.
     */
    @Transactional
    public BillPayment executeBillPayment(String customerId, String sourceAccountNumber,
                                          String billerCode, String consumerReference,
                                          BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bill payment amount must be greater than zero");
        }

        // 1. Validate Biller
        Biller biller = billerRepository.findByBillerCode(billerCode)
                .orElseThrow(() -> new IllegalArgumentException("Biller not found with code: " + billerCode));

        // 2. Validate Source Account & Ownership
        Account sourceAccount = accountRepository.findByAccountNumber(sourceAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + sourceAccountNumber));

        if (!sourceAccount.getCustomerId().equals(customerId)) {
            throw new SecurityException("Unauthorized: You do not own the source account");
        }

        if (!"ACTIVE".equalsIgnoreCase(sourceAccount.getStatus())) {
            throw new IllegalStateException("Source account is not active");
        }

        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds. Available: " +
                    sourceAccount.getBalance() + ", Bill Amount: " + amount);
        }

        // 3. Find or Credit Biller Synthetic Destination Account
        Account billerAccount = accountRepository.findByAccountNumber(biller.getSyntheticAccount())
                .orElseGet(() -> {
                    Account newBillerAcc = new Account(
                            biller.getSyntheticAccount(),
                            "CORP-BILLER",
                            "BUSINESS",
                            BigDecimal.valueOf(1000000.00),
                            "INR"
                    );
                    return accountRepository.save(newBillerAcc);
                });

        // 4. Update Balances Atomically
        BigDecimal newSourceBalance = sourceAccount.getBalance().subtract(amount);
        BigDecimal newBillerBalance = billerAccount.getBalance().add(amount);

        sourceAccount.setBalance(newSourceBalance);
        billerAccount.setBalance(newBillerBalance);

        accountRepository.save(sourceAccount);
        accountRepository.save(billerAccount);

        // 5. Generate Payment Record
        String paymentRef = idGenerator.generatePaymentReference();
        BillPayment billPayment = new BillPayment(
                paymentRef,
                customerId,
                sourceAccountNumber,
                biller.getBillerCode(),
                biller.getName(),
                consumerReference,
                amount,
                "COMMITTED"
        );
        billPaymentRepository.save(billPayment);

        // 6. Record Transaction Record
        String txnId = idGenerator.generateTransactionReference();
        Transaction transaction = new Transaction(
                txnId,
                paymentRef,
                sourceAccount.getAccountNumber(),
                billerAccount.getAccountNumber(),
                amount,
                sourceAccount.getCurrency(),
                "BILL_PAYMENT",
                "COMMITTED",
                null,
                "Bill Payment to " + biller.getName() + " (Ref: " + consumerReference + ")"
        );
        transactionRepository.save(transaction);

        // 7. Double-Entry Ledger Entries
        LedgerEntry debitEntry = new LedgerEntry(
                idGenerator.generateLedgerId(),
                txnId,
                paymentRef,
                sourceAccount.getAccountNumber(),
                "DEBIT",
                amount,
                newSourceBalance,
                "Bill payment to " + biller.getName() + " [" + consumerReference + "]"
        );
        ledgerRepository.save(debitEntry);

        LedgerEntry creditEntry = new LedgerEntry(
                idGenerator.generateLedgerId(),
                txnId,
                paymentRef,
                billerAccount.getAccountNumber(),
                "CREDIT",
                amount,
                newBillerBalance,
                "Payment received from " + sourceAccount.getAccountNumber() + " for consumer " + consumerReference
        );
        ledgerRepository.save(creditEntry);

        // 8. Durable Outbox Notification Event
        try {
            Map<String, Object> notificationPayload = Map.of(
                    "customerId", customerId,
                    "title", "Bill Payment Successful",
                    "message", "Payment of " + amount + " INR for " + biller.getName() +
                            " (Consumer Ref: " + consumerReference + ") was successful. Reference: " + paymentRef,
                    "type", "BILL_PAYMENT",
                    "transactionReference", paymentRef,
                    "amount", amount
            );

            OutboxEvent outboxEvent = new OutboxEvent(
                    idGenerator.generateOutboxEventId(),
                    "BILL_PAID",
                    paymentRef,
                    objectMapper.writeValueAsString(notificationPayload)
            );
            outboxRepository.save(outboxEvent);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to record bill payment outbox event", ex);
        }

        return billPayment;
    }

    public List<BillPayment> getCustomerPayments(String customerId) {
        return billPaymentRepository.findByCustomerIdOrderByTimestampDesc(customerId);
    }
}

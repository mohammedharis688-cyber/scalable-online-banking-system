package com.systemdesign.banking.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemdesign.banking.account.Account;
import com.systemdesign.banking.account.AccountRepository;
import com.systemdesign.banking.ledger.LedgerEntry;
import com.systemdesign.banking.ledger.LedgerRepository;
import com.systemdesign.banking.notification.OutboxEvent;
import com.systemdesign.banking.notification.OutboxRepository;
import com.systemdesign.banking.systemdesign.idgenerator.UniqueIdGenerator;
import com.systemdesign.banking.transaction.Transaction;
import com.systemdesign.banking.transaction.TransactionRepository;
import com.systemdesign.banking.transfer.dto.TransferRequest;
import com.systemdesign.banking.transfer.dto.TransferResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final OutboxRepository outboxRepository;
    private final UniqueIdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public TransferService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            LedgerRepository ledgerRepository,
            OutboxRepository outboxRepository,
            UniqueIdGenerator idGenerator,
            ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.outboxRepository = outboxRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes atomic, multi-document fund transfer across accounts.
     * Guaranteed atomic debit/credit, double-entry ledger consistency, idempotency,
     * and durable transactional outbox notification event creation.
     */
    @Transactional
    public TransferResponse executeTransfer(String customerId, TransferRequest request) {
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be strictly greater than zero");
        }

        // 1. Check Idempotency Key
        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Transaction> existingTx = transactionRepository.findByIdempotencyKey(idempotencyKey);
            if (existingTx.isPresent()) {
                Transaction tx = existingTx.get();
                Account srcAcc = accountRepository.findByAccountNumber(tx.getSourceAccountNumber())
                        .orElse(null);
                BigDecimal currentBalance = srcAcc != null ? srcAcc.getBalance() : BigDecimal.ZERO;

                return new TransferResponse(
                        tx.getTransactionReference(),
                        tx.getSourceAccountNumber(),
                        tx.getDestinationAccountNumber(),
                        tx.getAmount(),
                        currentBalance,
                        tx.getStatus(),
                        "Idempotent replay: transfer was already processed",
                        tx.getTimestamp(),
                        true
                );
            }
        }

        // 2. Validate Source Account & Ownership
        Account sourceAccount = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + request.getSourceAccountNumber()));

        if (!sourceAccount.getCustomerId().equals(customerId)) {
            throw new SecurityException("Unauthorized: You do not own the source account");
        }

        if (!"ACTIVE".equalsIgnoreCase(sourceAccount.getStatus())) {
            throw new IllegalStateException("Source account is not active");
        }

        // 3. Validate Destination Account
        if (request.getSourceAccountNumber().equalsIgnoreCase(request.getDestinationAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer funds to the same source account");
        }

        Account destAccount = accountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found: " + request.getDestinationAccountNumber()));

        if (!"ACTIVE".equalsIgnoreCase(destAccount.getStatus())) {
            throw new IllegalStateException("Destination account is not active");
        }

        // 4. Concurrency & Balance Validation
        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds. Available: " +
                    sourceAccount.getBalance() + ", Requested: " + amount);
        }

        // 5. Debit Source & Credit Destination
        BigDecimal newSourceBalance = sourceAccount.getBalance().subtract(amount);
        BigDecimal newDestBalance = destAccount.getBalance().add(amount);

        sourceAccount.setBalance(newSourceBalance);
        destAccount.setBalance(newDestBalance);

        accountRepository.save(sourceAccount);
        accountRepository.save(destAccount);

        // 6. Generate Transaction Record
        String txnId = idGenerator.generateTransactionReference();
        String txnRef = txnId;

        Transaction transaction = new Transaction(
                txnId,
                txnRef,
                sourceAccount.getAccountNumber(),
                destAccount.getAccountNumber(),
                amount,
                sourceAccount.getCurrency(),
                "TRANSFER",
                "COMMITTED",
                idempotencyKey,
                request.getDescription()
        );
        transactionRepository.save(transaction);

        // 7. Double-Entry Ledger Consistency
        // DEBIT entry for Source Account
        LedgerEntry debitEntry = new LedgerEntry(
                idGenerator.generateLedgerId(),
                txnId,
                txnRef,
                sourceAccount.getAccountNumber(),
                "DEBIT",
                amount,
                newSourceBalance,
                "Transfer to " + destAccount.getAccountNumber() + ": " + request.getDescription()
        );
        ledgerRepository.save(debitEntry);

        // CREDIT entry for Destination Account
        LedgerEntry creditEntry = new LedgerEntry(
                idGenerator.generateLedgerId(),
                txnId,
                txnRef,
                destAccount.getAccountNumber(),
                "CREDIT",
                amount,
                newDestBalance,
                "Transfer from " + sourceAccount.getAccountNumber() + ": " + request.getDescription()
        );
        ledgerRepository.save(creditEntry);

        // 8. Record Durable Outbox Event for Asynchronous Kafka Notification
        try {
            Map<String, Object> notificationPayload = Map.of(
                    "customerId", destAccount.getCustomerId(),
                    "senderCustomerId", sourceAccount.getCustomerId(),
                    "title", "Funds Received",
                    "message", "You received " + amount + " " + sourceAccount.getCurrency() +
                            " from account " + sourceAccount.getAccountNumber() + ". Reference: " + txnRef,
                    "type", "TRANSFER_IN",
                    "transactionReference", txnRef,
                    "amount", amount
            );

            OutboxEvent outboxEvent = new OutboxEvent(
                    idGenerator.generateOutboxEventId(),
                    "TRANSFER_COMPLETED",
                    txnRef,
                    objectMapper.writeValueAsString(notificationPayload)
            );
            outboxRepository.save(outboxEvent);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize outbox event payload", ex);
        }

        return new TransferResponse(
                txnRef,
                sourceAccount.getAccountNumber(),
                destAccount.getAccountNumber(),
                amount,
                newSourceBalance,
                "COMMITTED",
                "Transfer completed successfully",
                transaction.getTimestamp(),
                false
        );
    }
}

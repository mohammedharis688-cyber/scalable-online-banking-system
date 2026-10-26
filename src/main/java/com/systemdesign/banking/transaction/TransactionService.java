package com.systemdesign.banking.transaction;

import com.systemdesign.banking.account.Account;
import com.systemdesign.banking.account.AccountRepository;
import com.systemdesign.banking.ledger.LedgerEntry;
import com.systemdesign.banking.ledger.LedgerRepository;
import com.systemdesign.banking.transaction.dto.TransactionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final AccountRepository accountRepository;
    private final MongoTemplate mongoTemplate;

    public TransactionService(
            TransactionRepository transactionRepository,
            LedgerRepository ledgerRepository,
            AccountRepository accountRepository,
            MongoTemplate mongoTemplate) {
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.accountRepository = accountRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Retrieves paginated transaction history for all accounts owned by the customer.
     * In a sharded MongoDB cluster where transactions are partitioned on transactionId,
     * this executes a scatter-gather query across shard replica sets via mongos.
     */
    public Page<TransactionDto> getCustomerTransactionHistory(String customerId, String typeFilter, Pageable pageable) {
        List<Account> accounts = accountRepository.findByCustomerId(customerId);
        if (accounts.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<String> accountNumbers = accounts.stream()
                .map(Account::getAccountNumber)
                .collect(Collectors.toSet());

        Criteria accountCriteria = new Criteria().orOperator(
                Criteria.where("sourceAccountNumber").in(accountNumbers),
                Criteria.where("destinationAccountNumber").in(accountNumbers)
        );

        Query query = new Query();
        if (typeFilter != null && !typeFilter.isBlank() && !"ALL".equalsIgnoreCase(typeFilter)) {
            query.addCriteria(new Criteria().andOperator(
                    accountCriteria,
                    Criteria.where("type").is(typeFilter.toUpperCase())
            ));
        } else {
            query.addCriteria(accountCriteria);
        }

        long total = mongoTemplate.count(query, Transaction.class);
        query.with(pageable);
        List<Transaction> transactions = mongoTemplate.find(query, Transaction.class);

        List<TransactionDto> dtos = transactions.stream().map(tx -> {
            String direction;
            if (accountNumbers.contains(tx.getSourceAccountNumber()) && accountNumbers.contains(tx.getDestinationAccountNumber())) {
                direction = "SELF";
            } else if (accountNumbers.contains(tx.getSourceAccountNumber())) {
                direction = "DEBIT";
            } else {
                direction = "CREDIT";
            }

            return new TransactionDto(
                    tx.getTransactionReference(),
                    tx.getTimestamp(),
                    tx.getType(),
                    tx.getAmount(),
                    direction,
                    tx.getSourceAccountNumber(),
                    tx.getDestinationAccountNumber(),
                    tx.getStatus(),
                    tx.getDescription()
            );
        }).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, total);
    }

    /**
     * Double-Entry Ledger Consistency Audit.
     * Verifies that for a given transaction, sum(DEBIT) == sum(CREDIT).
     */
    public Map<String, Object> auditTransactionLedger(String transactionReference) {
        List<LedgerEntry> entries = ledgerRepository.findByTransactionReference(transactionReference);

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            if ("DEBIT".equalsIgnoreCase(entry.getEntryType())) {
                totalDebits = totalDebits.add(entry.getAmount());
            } else if ("CREDIT".equalsIgnoreCase(entry.getEntryType())) {
                totalCredits = totalCredits.add(entry.getAmount());
            }
        }

        boolean isBalanced = entries.size() >= 2 && totalDebits.compareTo(totalCredits) == 0;

        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("transactionReference", transactionReference);
        audit.put("entryCount", entries.size());
        audit.put("totalDebits", totalDebits);
        audit.put("totalCredits", totalCredits);
        audit.put("difference", totalDebits.subtract(totalCredits));
        audit.put("isBalanced", isBalanced);
        audit.put("ledgerEntries", entries);
        return audit;
    }
}

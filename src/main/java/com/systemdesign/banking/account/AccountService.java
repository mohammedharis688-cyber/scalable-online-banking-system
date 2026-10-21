package com.systemdesign.banking.account;

import com.systemdesign.banking.systemdesign.idgenerator.UniqueIdGenerator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UniqueIdGenerator idGenerator;
    private final MongoTemplate mongoTemplate;

    public AccountService(AccountRepository accountRepository, UniqueIdGenerator idGenerator, MongoTemplate mongoTemplate) {
        this.accountRepository = accountRepository;
        this.idGenerator = idGenerator;
        this.mongoTemplate = mongoTemplate;
    }

    public List<Account> getCustomerAccounts(String customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    public Account getAccountDetails(String accountNumber, String customerId) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));

        if (!account.getCustomerId().equals(customerId)) {
            throw new SecurityException("Access denied: You do not own this account");
        }
        return account;
    }

    /**
     * Authoritative balance enquiry fetched directly from MongoDB (never cached).
     */
    public BigDecimal getAuthoritativeBalance(String accountNumber, String customerId) {
        Account account = getAccountDetails(accountNumber, customerId);
        return account.getBalance();
    }

    /**
     * Updates an account's balance targeting the exact shard key (accountNumber).
     * Necessary for MongoDB sharded collections to route update operations properly.
     */
    public void updateBalance(String accountNumber, BigDecimal newBalance) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("accountNumber").is(accountNumber)),
                Update.update("balance", newBalance).set("updatedAt", Instant.now()),
                Account.class
        );
    }

    public Account openAdditionalAccount(String customerId, String accountType, BigDecimal initialDeposit) {
        String accountNumber = idGenerator.generateAccountNumber();
        BigDecimal balance = (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) >= 0)
                ? initialDeposit
                : BigDecimal.ZERO;

        String type = (accountType != null && !accountType.isBlank()) ? accountType.toUpperCase() : "SAVINGS";

        Account account = new Account(accountNumber, customerId, type, balance, "INR");
        return accountRepository.save(account);
    }
}

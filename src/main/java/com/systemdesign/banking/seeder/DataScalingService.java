package com.systemdesign.banking.seeder;

import com.systemdesign.banking.account.Account;
import com.systemdesign.banking.account.AccountRepository;
import com.systemdesign.banking.customer.Customer;
import com.systemdesign.banking.customer.CustomerRepository;
import com.systemdesign.banking.ledger.LedgerEntry;
import com.systemdesign.banking.ledger.LedgerRepository;
import com.systemdesign.banking.systemdesign.idgenerator.UniqueIdGenerator;
import com.systemdesign.banking.transaction.Transaction;
import com.systemdesign.banking.transaction.TransactionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class DataScalingService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final PasswordEncoder passwordEncoder;
    private final UniqueIdGenerator idGenerator;

    public DataScalingService(
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            LedgerRepository ledgerRepository,
            PasswordEncoder passwordEncoder,
            UniqueIdGenerator idGenerator) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.passwordEncoder = passwordEncoder;
        this.idGenerator = idGenerator;
    }

    /**
     * Dynamically generates synthetic datasets for Scalability Experiment B.
     */
    public Map<String, Object> generateDataset(int customerCount, int transactionsPerCustomer) {
        if (customerCount > 500) {
            customerCount = 500; // Cap to keep local run fast
        }
        if (transactionsPerCustomer > 20) {
            transactionsPerCustomer = 20;
        }

        String hashedPassword = passwordEncoder.encode("Password123!");
        List<Customer> customers = new ArrayList<>(customerCount);
        List<Account> accounts = new ArrayList<>(customerCount);
        List<Transaction> transactions = new ArrayList<>(customerCount * transactionsPerCustomer);
        List<LedgerEntry> ledgers = new ArrayList<>(customerCount * transactionsPerCustomer * 2);

        long start = System.currentTimeMillis();

        for (int i = 1; i <= customerCount; i++) {
            String custId = "CUST-SCALE-" + (1000 + i);
            String email = "scale.user" + i + "@syntheticbank.local";
            Customer c = new Customer(custId, email, hashedPassword, "Scale User " + i, "+91-90000" + String.format("%05d", i));
            customers.add(c);

            String accNum = "ACC-SCALE-" + String.format("%08d", i);
            Account a = new Account(accNum, custId, "SAVINGS", BigDecimal.valueOf(10000.00 + (i * 100)), "INR");
            accounts.add(a);
        }

        customerRepository.saveAll(customers);
        accountRepository.saveAll(accounts);

        // Generate transactions between generated accounts
        Random rand = new Random();
        for (int i = 0; i < accounts.size(); i++) {
            Account src = accounts.get(i);
            for (int t = 0; t < transactionsPerCustomer; t++) {
                int destIdx = (i + t + 1) % accounts.size();
                Account dst = accounts.get(destIdx);
                BigDecimal amount = BigDecimal.valueOf(10.00 + rand.nextInt(500));

                String txnId = idGenerator.generateTransactionReference();
                Transaction tx = new Transaction(
                        txnId,
                        txnId,
                        src.getAccountNumber(),
                        dst.getAccountNumber(),
                        amount,
                        "INR",
                        "TRANSFER",
                        "COMMITTED",
                        null,
                        "Synthetic benchmark transfer"
                );
                transactions.add(tx);

                // Add double-entry ledger entries
                ledgers.add(new LedgerEntry(
                        idGenerator.generateLedgerId(),
                        txnId,
                        txnId,
                        src.getAccountNumber(),
                        "DEBIT",
                        amount,
                        src.getBalance(),
                        "Synthetic scale debit"
                ));
                ledgers.add(new LedgerEntry(
                        idGenerator.generateLedgerId(),
                        txnId,
                        txnId,
                        dst.getAccountNumber(),
                        "CREDIT",
                        amount,
                        dst.getBalance(),
                        "Synthetic scale credit"
                ));
            }
        }

        transactionRepository.saveAll(transactions);
        ledgerRepository.saveAll(ledgers);

        long durationMs = System.currentTimeMillis() - start;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customersGenerated", customers.size());
        result.put("accountsGenerated", accounts.size());
        result.put("transactionsGenerated", transactions.size());
        result.put("ledgerEntriesGenerated", ledgers.size());
        result.put("totalCustomersInDatabase", customerRepository.count());
        result.put("totalAccountsInDatabase", accountRepository.count());
        result.put("totalTransactionsInDatabase", transactionRepository.count());
        result.put("durationMs", durationMs);
        return result;
    }
}

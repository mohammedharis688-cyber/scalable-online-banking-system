package com.systemdesign.banking.seeder;

import com.systemdesign.banking.account.Account;
import com.systemdesign.banking.account.AccountRepository;
import com.systemdesign.banking.beneficiary.Beneficiary;
import com.systemdesign.banking.beneficiary.BeneficiaryRepository;
import com.systemdesign.banking.billpayment.Biller;
import com.systemdesign.banking.billpayment.BillerRepository;
import com.systemdesign.banking.customer.Customer;
import com.systemdesign.banking.customer.CustomerRepository;
import com.systemdesign.banking.ledger.LedgerEntry;
import com.systemdesign.banking.ledger.LedgerRepository;
import com.systemdesign.banking.notification.Notification;
import com.systemdesign.banking.notification.NotificationRepository;
import com.systemdesign.banking.systemdesign.autocomplete.AutocompleteService;
import com.systemdesign.banking.systemdesign.idgenerator.UniqueIdGenerator;
import com.systemdesign.banking.transaction.Transaction;
import com.systemdesign.banking.transaction.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final BillerRepository billerRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final UniqueIdGenerator idGenerator;
    private final AutocompleteService autocompleteService;
    private final MongoTemplate mongoTemplate;

    public DatabaseSeeder(
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            BeneficiaryRepository beneficiaryRepository,
            BillerRepository billerRepository,
            TransactionRepository transactionRepository,
            LedgerRepository ledgerRepository,
            NotificationRepository notificationRepository,
            PasswordEncoder passwordEncoder,
            UniqueIdGenerator idGenerator,
            AutocompleteService autocompleteService,
            MongoTemplate mongoTemplate) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.billerRepository = billerRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.idGenerator = idGenerator;
        this.autocompleteService = autocompleteService;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        ensureCollectionsExist();

        log.info("Checking database initialization status...");
        if (customerRepository.existsByEmail("john.doe@example.com")) {
            log.info("Database is already seeded. Skipping duplicate seeding.");
            return;
        }

        log.info("Seeding synthetic banking environment with demo customers, accounts, billers, and ledger records...");

        String defaultHashedPassword = passwordEncoder.encode("Password123!");

        // 1. Create Demo Customers
        Customer john = new Customer("CUST-1001", "john.doe@example.com", defaultHashedPassword, "John Doe", "+91-9876543210");
        Customer alice = new Customer("CUST-1002", "alice.smith@example.com", defaultHashedPassword, "Alice Smith", "+91-9876543211");
        Customer bob = new Customer("CUST-1003", "bob.jones@example.com", defaultHashedPassword, "Bob Jones", "+91-9876543212");

        customerRepository.saveAll(List.of(john, alice, bob));

        // 2. Create Demo Accounts with High-Precision BigDecimal Balances
        Account johnSavings = new Account("ACC-1001-10000001", "CUST-1001", "SAVINGS", BigDecimal.valueOf(50000.00), "INR");
        Account johnChecking = new Account("ACC-1001-10000004", "CUST-1001", "CHECKING", BigDecimal.valueOf(15000.00), "INR");
        Account aliceSavings = new Account("ACC-1001-10000002", "CUST-1002", "SAVINGS", BigDecimal.valueOf(75000.00), "INR");
        Account bobSavings = new Account("ACC-1001-10000003", "CUST-1003", "SAVINGS", BigDecimal.valueOf(25000.00), "INR");

        accountRepository.saveAll(List.of(johnSavings, johnChecking, aliceSavings, bobSavings));

        // 3. Create Registered Beneficiaries for John Doe
        Beneficiary benAlice = new Beneficiary("CUST-1001", "Alice Smith", "ACC-1001-10000002", "Apex Reserve Bank", "alice.smith@example.com");
        Beneficiary benBob = new Beneficiary("CUST-1001", "Bob Jones", "ACC-1001-10000003", "Apex Reserve Bank", "bob.jones@example.com");

        beneficiaryRepository.saveAll(List.of(benAlice, benBob));

        // 4. Create Synthetic Billers
        Biller electricity = new Biller("ELEC-01", "Apex City Power & Electric", "ELECTRICITY",
                "Metropolitan electrical grid authority", "ACC-9001-11111111");
        Biller water = new Biller("WATER-01", "Metropolitan Water Works", "WATER",
                "Municipal clean water and wastewater services", "ACC-9001-22222222");
        Biller internet = new Biller("NET-01", "FiberLink High-Speed Internet", "INTERNET",
                "Gigabit fiber-optic broadband services", "ACC-9001-33333333");
        Biller mobile = new Biller("MOB-01", "Apex Cellular Mobile", "MOBILE",
                "5G nationwide mobile telecommunication operator", "ACC-9001-44444444");

        billerRepository.saveAll(List.of(electricity, water, internet, mobile));

        // Also create synthetic biller corporate destination accounts
        Account b1 = new Account("ACC-9001-11111111", "CORP-ELEC", "BUSINESS", BigDecimal.valueOf(5000000.00), "INR");
        Account b2 = new Account("ACC-9001-22222222", "CORP-WATER", "BUSINESS", BigDecimal.valueOf(2500000.00), "INR");
        Account b3 = new Account("ACC-9001-33333333", "CORP-NET", "BUSINESS", BigDecimal.valueOf(3500000.00), "INR");
        Account b4 = new Account("ACC-9001-44444444", "CORP-MOB", "BUSINESS", BigDecimal.valueOf(8000000.00), "INR");
        accountRepository.saveAll(List.of(b1, b2, b3, b4));

        // 5. Seed Historical Transaction and Balanced Double-Entry Ledger Entries
        String seedTxnId = "TXN-SEED-001";
        String seedTxnRef = "TXN-20261020-0001";
        BigDecimal seedAmount = BigDecimal.valueOf(2500.00);

        Transaction seedTxn = new Transaction(
                seedTxnId,
                seedTxnRef,
                johnSavings.getAccountNumber(),
                aliceSavings.getAccountNumber(),
                seedAmount,
                "INR",
                "TRANSFER",
                "COMMITTED",
                "IDEMP-SEED-001",
                "Initial test transfer for history demonstration"
        );
        transactionRepository.save(seedTxn);

        LedgerEntry debitEntry = new LedgerEntry(
                "LDG-SEED-01",
                seedTxnId,
                seedTxnRef,
                johnSavings.getAccountNumber(),
                "DEBIT",
                seedAmount,
                johnSavings.getBalance(),
                "Initial transfer debit to Alice"
        );
        LedgerEntry creditEntry = new LedgerEntry(
                "LDG-SEED-02",
                seedTxnId,
                seedTxnRef,
                aliceSavings.getAccountNumber(),
                "CREDIT",
                seedAmount,
                aliceSavings.getBalance(),
                "Initial transfer credit from John"
        );
        ledgerRepository.saveAll(List.of(debitEntry, creditEntry));

        // 6. Seed Welcome In-App Notification
        Notification welcomeNotif = new Notification(
                idGenerator.generateNotificationId(),
                "CUST-1001",
                "Welcome to Scalable Online Banking",
                "Your account ACC-1001-10000001 is active with a synthetic balance of 50,000.00 INR. Explore transfers, bill payments, and system insights.",
                "SYSTEM"
        );
        notificationRepository.save(welcomeNotif);

        // 7. Seed Terms into Trie Autocomplete
        autocompleteService.addTerm("Apex City Power & Electric", "ELECTRICITY", "ELEC-01", 100);
        autocompleteService.addTerm("Electricity", "ELECTRICITY", "ELEC-01", 95);
        autocompleteService.addTerm("ELEC-01", "ELECTRICITY", "ELEC-01", 90);
        autocompleteService.addTerm("Metropolitan Water Works", "WATER", "WATER-01", 100);
        autocompleteService.addTerm("Water", "WATER", "WATER-01", 95);
        autocompleteService.addTerm("WATER-01", "WATER", "WATER-01", 90);
        autocompleteService.addTerm("FiberLink High-Speed Internet", "INTERNET", "NET-01", 100);
        autocompleteService.addTerm("Internet", "INTERNET", "NET-01", 95);
        autocompleteService.addTerm("NET-01", "INTERNET", "NET-01", 90);
        autocompleteService.addTerm("Apex Cellular Mobile", "MOBILE", "MOB-01", 100);
        autocompleteService.addTerm("Mobile", "MOBILE", "MOB-01", 95);
        autocompleteService.addTerm("MOB-01", "MOBILE", "MOB-01", 90);
        autocompleteService.addTerm("Alice Smith", "BENEFICIARY", "ACC-1001-10000002", 90);
        autocompleteService.addTerm("Bob Jones", "BENEFICIARY", "ACC-1001-10000003", 90);

        log.info("Database seeding completed successfully! Demo accounts are ready.");
    }

    private void ensureCollectionsExist() {
        String[] requiredCollections = {
                "customers", "accounts", "beneficiaries", "billers",
                "transactions", "ledgers", "bill_payments", "notifications",
                "outbox_events", "short_urls"
        };
        for (String coll : requiredCollections) {
            if (!mongoTemplate.collectionExists(coll)) {
                mongoTemplate.createCollection(coll);
                log.info("Pre-created required MongoDB collection: {}", coll);
            }
        }
    }
}

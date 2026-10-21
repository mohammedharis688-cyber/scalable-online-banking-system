package com.systemdesign.banking.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemdesign.banking.account.Account;
import com.systemdesign.banking.account.AccountRepository;
import com.systemdesign.banking.account.AccountService;
import com.systemdesign.banking.ledger.LedgerEntry;
import com.systemdesign.banking.ledger.LedgerRepository;
import com.systemdesign.banking.notification.OutboxEvent;
import com.systemdesign.banking.notification.OutboxRepository;
import com.systemdesign.banking.systemdesign.idgenerator.UniqueIdGenerator;
import com.systemdesign.banking.transaction.Transaction;
import com.systemdesign.banking.transaction.TransactionRepository;
import com.systemdesign.banking.transfer.dto.TransferRequest;
import com.systemdesign.banking.transfer.dto.TransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private OutboxRepository outboxRepository;

    private UniqueIdGenerator idGenerator;
    private ObjectMapper objectMapper;
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        idGenerator = new UniqueIdGenerator();
        objectMapper = new ObjectMapper();
        transferService = new TransferService(
                accountRepository,
                accountService,
                transactionRepository,
                ledgerRepository,
                outboxRepository,
                idGenerator,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should successfully execute atomic transfer with balanced double-entry ledger")
    void testSuccessfulTransfer() {
        String customerId = "CUST-1001";
        String srcAccNum = "ACC-1001-0001";
        String dstAccNum = "ACC-1001-0002";
        BigDecimal transferAmount = BigDecimal.valueOf(500.00);

        Account srcAccount = new Account(srcAccNum, customerId, "SAVINGS", BigDecimal.valueOf(2000.00), "INR");
        Account dstAccount = new Account(dstAccNum, "CUST-1002", "SAVINGS", BigDecimal.valueOf(1000.00), "INR");

        when(accountRepository.findByAccountNumber(srcAccNum)).thenReturn(Optional.of(srcAccount));
        when(accountRepository.findByAccountNumber(dstAccNum)).thenReturn(Optional.of(dstAccount));

        TransferRequest request = new TransferRequest(srcAccNum, dstAccNum, transferAmount, "Rent payment", "IDEMP-12345");
        TransferResponse response = transferService.executeTransfer(customerId, request);

        // Assert response
        assertNotNull(response);
        assertEquals("COMMITTED", response.getStatus());
        assertEquals(BigDecimal.valueOf(1500.00), response.getSourceBalanceAfter());
        assertFalse(response.isIdempotentReplay());

        // Verify account balance updates
        assertEquals(BigDecimal.valueOf(1500.00), srcAccount.getBalance());
        assertEquals(BigDecimal.valueOf(1500.00), dstAccount.getBalance());
        verify(accountService, times(1)).updateBalance(eq(srcAccNum), any(BigDecimal.class));
        verify(accountService, times(1)).updateBalance(eq(dstAccNum), any(BigDecimal.class));

        // Verify Double-Entry Ledger Entries (1 DEBIT, 1 CREDIT)
        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerRepository, times(2)).save(ledgerCaptor.capture());

        var capturedEntries = ledgerCaptor.getAllValues();
        LedgerEntry debit = capturedEntries.stream().filter(e -> e.getEntryType().equals("DEBIT")).findFirst().orElseThrow();
        LedgerEntry credit = capturedEntries.stream().filter(e -> e.getEntryType().equals("CREDIT")).findFirst().orElseThrow();

        assertEquals(transferAmount, debit.getAmount());
        assertEquals(transferAmount, credit.getAmount());
        assertEquals(srcAccNum, debit.getAccountNumber());
        assertEquals(dstAccNum, credit.getAccountNumber());

        // Verify Outbox Event created for Kafka notification
        verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Should reject transfer when account has insufficient funds")
    void testInsufficientFunds() {
        String customerId = "CUST-1001";
        String srcAccNum = "ACC-1001-0001";
        String dstAccNum = "ACC-1001-0002";

        Account srcAccount = new Account(srcAccNum, customerId, "SAVINGS", BigDecimal.valueOf(100.00), "INR");
        Account dstAccount = new Account(dstAccNum, "CUST-1002", "SAVINGS", BigDecimal.valueOf(500.00), "INR");

        when(accountRepository.findByAccountNumber(srcAccNum)).thenReturn(Optional.of(srcAccount));
        when(accountRepository.findByAccountNumber(dstAccNum)).thenReturn(Optional.of(dstAccount));

        TransferRequest request = new TransferRequest(srcAccNum, dstAccNum, BigDecimal.valueOf(500.00), "Payment", null);

        assertThrows(IllegalArgumentException.class, () -> transferService.executeTransfer(customerId, request));

        // Ensure zero balances modified and zero writes
        assertEquals(BigDecimal.valueOf(100.00), srcAccount.getBalance());
        verify(transactionRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject transfer if customer does not own source account")
    void testUnauthorizedAccountAccess() {
        String customerId = "CUST-ATTACKER";
        String srcAccNum = "ACC-1001-0001";
        String dstAccNum = "ACC-1001-0002";

        Account victimAccount = new Account(srcAccNum, "CUST-VICTIM", "SAVINGS", BigDecimal.valueOf(50000.00), "INR");
        when(accountRepository.findByAccountNumber(srcAccNum)).thenReturn(Optional.of(victimAccount));

        TransferRequest request = new TransferRequest(srcAccNum, dstAccNum, BigDecimal.valueOf(100.00), "Steal", null);

        assertThrows(SecurityException.class, () -> transferService.executeTransfer(customerId, request));
    }

    @Test
    @DisplayName("Should return existing transaction on repeated idempotency key without duplicate deduction")
    void testIdempotencyReplay() {
        String customerId = "CUST-1001";
        String idempotencyKey = "IDEMP-DUPLICATE-KEY";

        Transaction existingTx = new Transaction(
                "TXN-EXISTING",
                "TXN-20261020-EXISTING",
                "ACC-1001-0001",
                "ACC-1001-0002",
                BigDecimal.valueOf(300.00),
                "INR",
                "TRANSFER",
                "COMMITTED",
                idempotencyKey,
                "Already processed"
        );

        when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingTx));
        Account srcAccount = new Account("ACC-1001-0001", customerId, "SAVINGS", BigDecimal.valueOf(1700.00), "INR");
        when(accountRepository.findByAccountNumber("ACC-1001-0001")).thenReturn(Optional.of(srcAccount));

        TransferRequest request = new TransferRequest("ACC-1001-0001", "ACC-1001-0002", BigDecimal.valueOf(300.00), "Repeat", idempotencyKey);
        TransferResponse response = transferService.executeTransfer(customerId, request);

        assertTrue(response.isIdempotentReplay());
        assertEquals("TXN-20261020-EXISTING", response.getTransactionReference());
        verify(ledgerRepository, never()).save(any());
    }
}

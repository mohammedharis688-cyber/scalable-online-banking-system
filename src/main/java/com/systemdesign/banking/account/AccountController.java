package com.systemdesign.banking.account;

import com.systemdesign.banking.auth.CustomerPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<List<Account>> listCustomerAccounts(@AuthenticationPrincipal CustomerPrincipal principal) {
        List<Account> accounts = accountService.getCustomerAccounts(principal.getCustomerId());
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<?> getAccountDetails(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal CustomerPrincipal principal) {
        try {
            Account account = accountService.getAccountDetails(accountNumber, principal.getCustomerId());
            return ResponseEntity.ok(account);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden", "message", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Not Found", "message", iae.getMessage()));
        }
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<?> getAccountBalance(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal CustomerPrincipal principal) {
        try {
            BigDecimal balance = accountService.getAuthoritativeBalance(accountNumber, principal.getCustomerId());
            return ResponseEntity.ok(Map.of(
                    "accountNumber", accountNumber,
                    "balance", balance,
                    "currency", "INR",
                    "authoritativeSource", "MongoDB (Uncached)"
            ));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden", "message", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Not Found", "message", iae.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> openAdditionalAccount(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomerPrincipal principal) {
        String accountType = (String) request.getOrDefault("accountType", "SAVINGS");
        Object depositObj = request.get("initialDeposit");
        BigDecimal initialDeposit = BigDecimal.ZERO;
        if (depositObj != null) {
            initialDeposit = new BigDecimal(depositObj.toString());
        }

        Account account = accountService.openAdditionalAccount(principal.getCustomerId(), accountType, initialDeposit);
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }
}

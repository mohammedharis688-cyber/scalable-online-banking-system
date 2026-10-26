package com.systemdesign.banking.transaction;

import com.systemdesign.banking.auth.CustomerPrincipal;
import com.systemdesign.banking.transaction.dto.TransactionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<Page<TransactionDto>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal CustomerPrincipal principal) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<TransactionDto> history = transactionService.getCustomerTransactionHistory(
                principal.getCustomerId(), type, pageRequest);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{reference}/audit")
    public ResponseEntity<?> auditTransactionLedger(@PathVariable String reference) {
        Map<String, Object> audit = transactionService.auditTransactionLedger(reference);
        return ResponseEntity.ok(audit);
    }
}

package com.systemdesign.banking.beneficiary;

import com.systemdesign.banking.auth.CustomerPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @GetMapping
    public ResponseEntity<List<Beneficiary>> listBeneficiaries(@AuthenticationPrincipal CustomerPrincipal principal) {
        return ResponseEntity.ok(beneficiaryService.getCustomerBeneficiaries(principal.getCustomerId()));
    }

    @PostMapping
    public ResponseEntity<?> addBeneficiary(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomerPrincipal principal) {
        try {
            String name = request.get("name");
            String accountNumber = request.get("accountNumber");
            String bankName = request.getOrDefault("bankName", "Apex Reserve Bank");
            String email = request.get("email");

            if (name == null || name.isBlank() || accountNumber == null || accountNumber.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Name and account number are required"));
            }

            Beneficiary beneficiary = beneficiaryService.addBeneficiary(
                    principal.getCustomerId(), name, accountNumber, bankName, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(beneficiary);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bad Request", "message", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeBeneficiary(
            @PathVariable String id,
            @AuthenticationPrincipal CustomerPrincipal principal) {
        try {
            beneficiaryService.removeBeneficiary(id, principal.getCustomerId());
            return ResponseEntity.ok(Map.of("message", "Beneficiary removed successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Not Found", "message", ex.getMessage()));
        }
    }
}

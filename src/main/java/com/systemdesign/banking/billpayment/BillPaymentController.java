package com.systemdesign.banking.billpayment;

import com.systemdesign.banking.auth.CustomerPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bills")
public class BillPaymentController {

    private final BillPaymentService billPaymentService;

    public BillPaymentController(BillPaymentService billPaymentService) {
        this.billPaymentService = billPaymentService;
    }

    @GetMapping("/billers")
    public ResponseEntity<List<Biller>> listBillers(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(billPaymentService.getBillersByCategory(category));
    }

    @PostMapping("/pay")
    public ResponseEntity<?> payBill(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomerPrincipal principal) {
        try {
            String sourceAccount = request.get("sourceAccountNumber") != null
                    ? (String) request.get("sourceAccountNumber")
                    : (String) request.get("accountNumber");
            String billerCode = request.get("billerCode") != null
                    ? (String) request.get("billerCode")
                    : (String) request.get("billerId");
            String consumerRef = (String) request.get("consumerReference");
            Object amountObj = request.get("amount");

            if (sourceAccount == null || billerCode == null || consumerRef == null || amountObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "All fields are required (sourceAccountNumber, billerCode, consumerReference, amount)"));
            }

            BigDecimal amount = new BigDecimal(amountObj.toString());

            BillPayment payment = billPaymentService.executeBillPayment(
                    principal.getCustomerId(), sourceAccount, billerCode, consumerRef, amount);

            return ResponseEntity.status(HttpStatus.CREATED).body(payment);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden", "message", se.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bad Request", "message", ex.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<BillPayment>> getBillHistory(@AuthenticationPrincipal CustomerPrincipal principal) {
        return ResponseEntity.ok(billPaymentService.getCustomerPayments(principal.getCustomerId()));
    }
}

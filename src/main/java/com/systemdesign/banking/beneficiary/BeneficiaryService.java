package com.systemdesign.banking.beneficiary;

import com.systemdesign.banking.account.Account;
import com.systemdesign.banking.account.AccountRepository;
import com.systemdesign.banking.systemdesign.autocomplete.AutocompleteService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;
    private final AutocompleteService autocompleteService;

    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository,
                              AccountRepository accountRepository,
                              AutocompleteService autocompleteService) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.accountRepository = accountRepository;
        this.autocompleteService = autocompleteService;
    }

    public Beneficiary addBeneficiary(String customerId, String name, String accountNumber, String bankName, String email) {
        // 1. Validate that destination account exists and is active
        Account destinationAccount = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found: " + accountNumber));

        if (!"ACTIVE".equalsIgnoreCase(destinationAccount.getStatus())) {
            throw new IllegalStateException("Destination account is not active");
        }

        // 2. Prevent adding customer's own account as external beneficiary
        if (destinationAccount.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Cannot add your own account as a beneficiary");
        }

        // 3. Prevent duplicate beneficiary
        if (beneficiaryRepository.existsByCustomerIdAndAccountNumber(customerId, accountNumber)) {
            throw new IllegalArgumentException("Beneficiary with this account number already exists");
        }

        Beneficiary beneficiary = new Beneficiary(
                customerId,
                name,
                accountNumber,
                bankName != null ? bankName : "Apex Reserve Bank",
                email
        );
        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        if (autocompleteService != null) {
            autocompleteService.addTerm(saved.getName(), "BENEFICIARY", saved.getAccountNumber(), 90);
            autocompleteService.addTerm(saved.getAccountNumber(), "BENEFICIARY", saved.getAccountNumber(), 85);
        }
        return saved;
    }

    public List<Beneficiary> getCustomerBeneficiaries(String customerId) {
        return beneficiaryRepository.findByCustomerId(customerId);
    }

    public void removeBeneficiary(String id, String customerId) {
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found or unauthorized"));
        beneficiaryRepository.delete(beneficiary);
    }
}

package com.systemdesign.banking.auth;

import com.systemdesign.banking.account.Account;
import com.systemdesign.banking.account.AccountRepository;
import com.systemdesign.banking.auth.dto.AuthResponse;
import com.systemdesign.banking.auth.dto.LoginRequest;
import com.systemdesign.banking.auth.dto.RegisterRequest;
import com.systemdesign.banking.auth.dto.UserDto;
import com.systemdesign.banking.customer.Customer;
import com.systemdesign.banking.customer.CustomerRepository;
import com.systemdesign.banking.systemdesign.idgenerator.UniqueIdGenerator;
import com.systemdesign.banking.systemdesign.ratelimiter.RateLimitResult;
import com.systemdesign.banking.systemdesign.ratelimiter.RedisTokenBucketRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UniqueIdGenerator idGenerator;
    private final RedisTokenBucketRateLimiter rateLimiter;

    @Value("${banking.ratelimit.login.capacity:10}")
    private double loginCapacity;

    @Value("${banking.ratelimit.login.refill-rate:1.0}")
    private double loginRefillRate;

    public AuthController(
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            UniqueIdGenerator idGenerator,
            RedisTokenBucketRateLimiter rateLimiter) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.idGenerator = idGenerator;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Conflict",
                    "message", "Email is already registered"
            ));
        }

        String customerId = idGenerator.generateCustomerId();
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        Customer customer = new Customer(
                customerId,
                request.getEmail(),
                hashedPassword,
                request.getFullName(),
                request.getPhoneNumber()
        );
        customerRepository.save(customer);

        // Open default primary synthetic account with initial balance
        String accountNumber = idGenerator.generateAccountNumber();
        BigDecimal deposit = request.getInitialDeposit() != null && request.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0
                ? request.getInitialDeposit()
                : BigDecimal.valueOf(5000.00);

        Account initialAccount = new Account(
                accountNumber,
                customerId,
                "SAVINGS",
                deposit,
                "INR"
        );
        accountRepository.save(initialAccount);

        String token = tokenProvider.generateToken(customer.getEmail(), customer.getCustomerId(), customer.getRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(
                token,
                customer.getCustomerId(),
                customer.getEmail(),
                customer.getFullName(),
                customer.getRole(),
                accountNumber
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        String bucketKey = "rate_limit:login:" + request.getEmail();

        // 1. Evaluate Redis Token Bucket Rate Limiter
        RateLimitResult rateCheck = rateLimiter.tryAcquire(bucketKey, loginCapacity, loginRefillRate, 1);
        if (!rateCheck.allowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "Too Many Requests",
                    "message", "Login rate limit exceeded. Please try again after " + rateCheck.retryAfterSeconds() + " seconds.",
                    "status", 429,
                    "remainingTokens", rateCheck.remainingTokens(),
                    "retryAfterSeconds", rateCheck.retryAfterSeconds()
            ));
        }

        // 2. Validate customer credentials
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (customer == null || !passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Unauthorized",
                    "message", "Invalid email or password"
            ));
        }

        // 3. Find primary account
        String primaryAccount = accountRepository.findByCustomerId(customer.getCustomerId()).stream()
                .findFirst()
                .map(Account::getAccountNumber)
                .orElse("N/A");

        // 4. Generate JWT
        String token = tokenProvider.generateToken(customer.getEmail(), customer.getCustomerId(), customer.getRole());

        return ResponseEntity.ok(new AuthResponse(
                token,
                customer.getCustomerId(),
                customer.getEmail(),
                customer.getFullName(),
                customer.getRole(),
                primaryAccount
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal CustomerPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        Customer customer = customerRepository.findByCustomerId(principal.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return ResponseEntity.ok(new UserDto(
                customer.getCustomerId(),
                customer.getEmail(),
                customer.getFullName(),
                customer.getPhoneNumber(),
                customer.getRole(),
                customer.getCreatedAt()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}

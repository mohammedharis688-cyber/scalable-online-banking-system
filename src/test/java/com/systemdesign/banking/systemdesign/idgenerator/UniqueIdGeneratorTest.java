package com.systemdesign.banking.systemdesign.idgenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UniqueIdGeneratorTest {

    private UniqueIdGenerator idGenerator;

    @BeforeEach
    void setUp() {
        idGenerator = new UniqueIdGenerator();
    }

    @Test
    @DisplayName("Should generate opaque collision-resistant identifiers with correct prefixes")
    void testPrefixes() {
        String custId = idGenerator.generateCustomerId();
        String accNum = idGenerator.generateAccountNumber();
        String txnRef = idGenerator.generateTransactionReference();
        String ldgId = idGenerator.generateLedgerId();

        assertTrue(custId.startsWith("CUST-"));
        assertTrue(accNum.startsWith("ACC-1001-"));
        assertTrue(txnRef.startsWith("TXN-"));
        assertTrue(ldgId.startsWith("LDG-"));
    }

    @Test
    @DisplayName("Should generate 10,000 unique transaction references with 0 collisions")
    void testBenchmarkZeroCollisions() {
        Map<String, Object> benchmark = idGenerator.benchmark(10000);

        assertEquals(10000, benchmark.get("sampleSize"));
        assertEquals(0, benchmark.get("collisionsObserved"));
        assertEquals(0.0, benchmark.get("collisionRatePercentage"));
        assertTrue((Long) benchmark.get("ratePerSecond") > 10000);
    }
}

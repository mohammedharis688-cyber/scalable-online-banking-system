package com.systemdesign.banking.systemdesign.consistenthashing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConsistentHashRouterTest {

    private ConsistentHashRouter router;

    @BeforeEach
    void setUp() {
        router = new ConsistentHashRouter();
    }

    @Test
    @DisplayName("Should initialize with 3 physical nodes and 300 virtual nodes")
    void testInitialization() {
        assertEquals(3, router.getNodes().size());
        assertEquals(300, router.getRingSize());
    }

    @Test
    @DisplayName("Should deterministically route identical keys to the same node")
    void testDeterministicRouting() {
        String key = "CUST-ACC-1001-998877";
        String node1 = router.routeKey(key);
        String node2 = router.routeKey(key);

        assertNotNull(node1);
        assertEquals(node1, node2);
    }

    @Test
    @DisplayName("Should execute topology experiment and remap a minority subset of keys when adding a node")
    void testAddNodeTopologyExperiment() {
        int sampleSize = 1000;
        Map<String, Object> result = router.runTopologyExperiment("ADD", "banking-srv-node-4", sampleSize);

        assertEquals("ADD", result.get("action"));
        assertEquals("banking-srv-node-4", result.get("targetNode"));

        int remapped = (Integer) result.get("remappedKeyCount");
        double remappedPct = (Double) result.get("remappedPercentage");

        // In consistent hashing with N nodes, adding 1 node remaps approximately 1/(N+1) = 25% of keys
        assertTrue(remapped > 0, "Some keys must be remapped to the new node");
        assertTrue(remappedPct < 45.0, "Consistent hashing ensures only a minority fraction of keys are moved");
        assertEquals(4, router.getNodes().size());
    }
}

package com.systemdesign.banking.systemdesign.consistenthashing;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Consistent Hashing Hash Ring Implementation with Virtual Nodes.
 * Demonstrates application-level distributed routing, load distribution,
 * and minimal key remapping during topology changes.
 */
@Component
public class ConsistentHashRouter {

    private final int defaultVirtualNodes;
    private final SortedMap<Long, String> ring = new ConcurrentSkipListMap<>();
    private final Set<String> physicalNodes = Collections.synchronizedSet(new LinkedHashSet<>());

    public ConsistentHashRouter() {
        this.defaultVirtualNodes = 100; // 100 virtual nodes per physical server for uniform dispersion
        // Initialize with default banking service cluster nodes
        addNode("banking-srv-node-1");
        addNode("banking-srv-node-2");
        addNode("banking-srv-node-3");
    }

    public synchronized void addNode(String node) {
        if (physicalNodes.add(node)) {
            for (int i = 0; i < defaultVirtualNodes; i++) {
                String vNodeName = node + "#VN" + i;
                long hash = hash(vNodeName);
                ring.put(hash, node);
            }
        }
    }

    public synchronized void removeNode(String node) {
        if (physicalNodes.remove(node)) {
            for (int i = 0; i < defaultVirtualNodes; i++) {
                String vNodeName = node + "#VN" + i;
                long hash = hash(vNodeName);
                ring.remove(hash);
            }
        }
    }

    public String routeKey(String key) {
        if (ring.isEmpty()) {
            return "NO_ACTIVE_NODES";
        }
        long hash = hash(key);
        if (!ring.containsKey(hash)) {
            SortedMap<Long, String> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        return ring.get(hash);
    }

    public Set<String> getNodes() {
        return new LinkedHashSet<>(physicalNodes);
    }

    public int getRingSize() {
        return ring.size();
    }

    public Map<String, Object> getDistributionAnalysis(int sampleSize) {
        List<String> keys = generateSampleBankingKeys(sampleSize);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String node : physicalNodes) {
            counts.put(node, 0);
        }

        for (String key : keys) {
            String targetNode = routeKey(key);
            counts.put(targetNode, counts.getOrDefault(targetNode, 0) + 1);
        }

        Map<String, Object> nodePercentages = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            double pct = sampleSize > 0 ? (entry.getValue() / (double) sampleSize) * 100.0 : 0.0;
            nodePercentages.put(entry.getKey(), Map.of(
                    "assignedKeys", entry.getValue(),
                    "percentage", Math.round(pct * 100.0) / 100.0
            ));
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalPhysicalNodes", physicalNodes.size());
        report.put("totalVirtualNodesOnRing", ring.size());
        report.put("virtualNodesPerPhysical", defaultVirtualNodes);
        report.put("sampleSize", sampleSize);
        report.put("nodeDistribution", nodePercentages);
        return report;
    }

    /**
     * Topology change experiment: Measures key remapping count and percentage
     * when a node is added or removed.
     */
    public synchronized Map<String, Object> runTopologyExperiment(String action, String nodeName, int sampleSize) {
        List<String> sampleKeys = generateSampleBankingKeys(sampleSize);

        // 1. Record pre-change routing assignments
        Map<String, String> preAssignments = new HashMap<>();
        for (String key : sampleKeys) {
            preAssignments.put(key, routeKey(key));
        }
        Map<String, Object> preDistribution = getDistributionAnalysis(sampleSize);

        // 2. Perform topology mutation
        if ("ADD".equalsIgnoreCase(action)) {
            addNode(nodeName);
        } else if ("REMOVE".equalsIgnoreCase(action)) {
            removeNode(nodeName);
        } else {
            throw new IllegalArgumentException("Unsupported action: " + action + " (Allowed: ADD, REMOVE)");
        }

        // 3. Record post-change routing assignments and calculate remapping
        int remappedCount = 0;
        for (String key : sampleKeys) {
            String newTarget = routeKey(key);
            String oldTarget = preAssignments.get(key);
            if (!Objects.equals(newTarget, oldTarget)) {
                remappedCount++;
            }
        }
        double remappedPercentage = (remappedCount / (double) sampleSize) * 100.0;
        Map<String, Object> postDistribution = getDistributionAnalysis(sampleSize);

        Map<String, Object> experimentResult = new LinkedHashMap<>();
        experimentResult.put("action", action.toUpperCase());
        experimentResult.put("targetNode", nodeName);
        experimentResult.put("sampleKeyCount", sampleSize);
        experimentResult.put("remappedKeyCount", remappedCount);
        experimentResult.put("remappedPercentage", Math.round(remappedPercentage * 100.0) / 100.0);
        experimentResult.put("theoreticalIdealRemappedPct", Math.round((100.0 / physicalNodes.size()) * 100.0) / 100.0);
        experimentResult.put("preChangeDistribution", preDistribution.get("nodeDistribution"));
        experimentResult.put("postChangeDistribution", postDistribution.get("nodeDistribution"));
        experimentResult.put("currentNodes", new ArrayList<>(physicalNodes));
        return experimentResult;
    }

    private List<String> generateSampleBankingKeys(int count) {
        List<String> keys = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            keys.add("CUST-ACC-" + (100000 + i));
        }
        return keys;
    }

    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            long h = 0;
            for (int i = 0; i < 8; i++) {
                h = (h << 8) | (digest[i] & 0xFF);
            }
            return h & 0x7FFFFFFFFFFFFFFFL; // positive 63-bit integer
        } catch (NoSuchAlgorithmException e) {
            return Math.abs(key.hashCode());
        }
    }
}

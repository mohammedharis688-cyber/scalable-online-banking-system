package com.systemdesign.banking.systemdesign.consistenthashing;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/system/consistent-hash", "/api/consistent-hash"})
public class ConsistentHashingController {

    private final ConsistentHashRouter router;

    public ConsistentHashingController(ConsistentHashRouter router) {
        this.router = router;
    }

    @GetMapping("/nodes")
    public ResponseEntity<?> getNodes() {
        return ResponseEntity.ok(Map.of(
                "nodes", router.getNodes(),
                "nodeCount", router.getNodes().size(),
                "totalNodes", router.getNodes().size(),
                "virtualNodesTotal", router.getRingSize()
        ));
    }

    @GetMapping("/route")
    public ResponseEntity<?> routeKey(@RequestParam String key) {
        String node = router.routeKey(key);
        return ResponseEntity.ok(Map.of(
                "routingKey", key,
                "assignedNode", node,
                "targetNode", node
        ));
    }

    @GetMapping("/distribution")
    public ResponseEntity<?> getDistribution(@RequestParam(defaultValue = "1000") int sampleSize) {
        return ResponseEntity.ok(router.getDistributionAnalysis(sampleSize));
    }

    @PostMapping("/experiment")
    public ResponseEntity<?> runExperiment(@RequestBody Map<String, Object> request) {
        String action = (String) request.getOrDefault("action", "ADD");
        String nodeName = (String) request.getOrDefault("nodeName", "banking-srv-node-4");
        int sampleSize = request.containsKey("sampleSize") ? Integer.parseInt(request.get("sampleSize").toString()) : 1000;

        try {
            Map<String, Object> result = router.runTopologyExperiment(action, nodeName, sampleSize);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}

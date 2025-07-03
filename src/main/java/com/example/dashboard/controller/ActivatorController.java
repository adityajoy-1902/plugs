package com.example.dashboard.controller;

import com.example.dashboard.service.ActivatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activator")
public class ActivatorController {

    @Autowired
    private ActivatorService activatorService;

    @PostMapping("/trigger-manual")
    public ResponseEntity<Map<String, String>> triggerManualRestart() {
        String result = activatorService.triggerManualRestart();
        Map<String, String> response = new HashMap<>();
        response.put("message", result);
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getRestartLogs() {
        List<String> logs = activatorService.getRestartLogs();
        Map<String, Object> response = new HashMap<>();
        response.put("logs", logs);
        response.put("count", logs.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/down-services")
    public ResponseEntity<Map<String, Object>> getCurrentDownServices() {
        List<String> downServices = activatorService.getCurrentDownServices();
        Map<String, Object> response = new HashMap<>();
        response.put("downServices", downServices);
        response.put("count", downServices.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/next-schedule")
    public ResponseEntity<Map<String, String>> getNextScheduledRestart() {
        String nextSchedule = activatorService.getNextScheduledRestart();
        Map<String, String> response = new HashMap<>();
        response.put("nextScheduledRestart", nextSchedule);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getActivatorStatus() {
        List<String> downServices = activatorService.getCurrentDownServices();
        String nextSchedule = activatorService.getNextScheduledRestart();
        List<String> logs = activatorService.getRestartLogs();
        
        Map<String, Object> response = new HashMap<>();
        response.put("downServicesCount", downServices.size());
        response.put("downServices", downServices);
        response.put("nextScheduledRestart", nextSchedule);
        response.put("recentLogs", logs.subList(Math.max(0, logs.size() - 5), logs.size())); // Last 5 logs
        response.put("totalLogs", logs.size());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh-status")
    public ResponseEntity<Map<String, String>> refreshStatus() {
        try {
            // Trigger immediate status refresh
            activatorService.refreshServiceStatuses();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Service statuses refreshed successfully");
            response.put("timestamp", new java.util.Date().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to refresh statuses: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
} 
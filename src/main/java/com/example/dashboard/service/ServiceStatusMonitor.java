package com.example.dashboard.service;

import com.example.dashboard.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;

@Service
public class ServiceStatusMonitor {

    @Autowired
    private YamlParserService yamlParserService;

    @Autowired
    private AnsibleExecutionService ansibleExecutionService;

    private final ConcurrentHashMap<String, String> statusCache = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    // Key format: app|env|server|service
    private String makeKey(String app, String env, String server, String service) {
        return app + "|" + env + "|" + server + "|" + service;
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000) // every 5 minutes
    public void checkAllServices() {
        System.out.println("=== Starting scheduled service status check ===");
        List<Application> applications = yamlParserService.parseYaml();
        if (applications == null) {
            System.out.println("No applications found in YAML");
            return;
        }

        for (Application app : applications) {
            if (app.getEnvironments() == null) continue;
            for (Environment env : app.getEnvironments()) {
                if (env.getServers() == null) continue;
                for (com.example.dashboard.model.Server server : env.getServers()) {
                    if (server.getServices() == null) continue;
                    for (com.example.dashboard.model.Service service : server.getServices()) {
                        String key = makeKey(app.getName(), env.getName(), server.getName(), service.getName());
                        String statusCmd = service.getStatusCmd();
                        String statusScript = service.getStatusScript();
                        String os = server.getOs();
                        String ip = server.getIp();
                        String application = app.getName();

                        if (statusCmd == null && statusScript == null) {
                            statusCache.put(key, "unknown");
                            continue;
                        }

                        executor.submit(() -> {
                                                    try {
                            String cmd = statusCmd != null ? statusCmd : statusScript;
                            String result = ansibleExecutionService.executeCommand(application, ip, cmd, os);
                                String status = parseStatus(result);
                                statusCache.put(key, status);
                                System.out.println("Updated status for " + key + ": " + status);
                            } catch (Exception e) {
                                statusCache.put(key, "down");
                                System.out.println("Error updating status for " + key + ": " + e.getMessage());
                            }
                        });
                    }
                }
            }
        }
        System.out.println("=== Scheduled service status check completed ===");
    }

    // Method to immediately update status of a specific service
    public void updateServiceStatus(String appName, String envName, String serverName, String serviceName) {
        String key = makeKey(appName, envName, serverName, serviceName);
        System.out.println("=== Immediate status update requested for: " + key + " ===");
        
        List<Application> applications = yamlParserService.parseYaml();
        if (applications == null) return;

        for (Application app : applications) {
            if (!app.getName().equals(appName)) continue;
            if (app.getEnvironments() == null) continue;
            
            for (Environment env : app.getEnvironments()) {
                if (!env.getName().equals(envName)) continue;
                if (env.getServers() == null) continue;
                
                for (com.example.dashboard.model.Server server : env.getServers()) {
                    if (!server.getName().equals(serverName)) continue;
                    if (server.getServices() == null) continue;
                    
                    for (com.example.dashboard.model.Service service : server.getServices()) {
                        if (!service.getName().equals(serviceName)) continue;
                        
                        String statusCmd = service.getStatusCmd();
                        String statusScript = service.getStatusScript();
                        String os = server.getOs();
                        String ip = server.getIp();
                        String application = app.getName();

                        if (statusCmd == null && statusScript == null) {
                            statusCache.put(key, "unknown");
                            return;
                        }

                        try {
                            String cmd = statusCmd != null ? statusCmd : statusScript;
                            String result = ansibleExecutionService.executeCommand(application, ip, cmd, os);
                            String status = parseStatus(result);
                            statusCache.put(key, status);
                            System.out.println("Immediate status update for " + key + ": " + status);
                        } catch (Exception e) {
                            statusCache.put(key, "down");
                            System.out.println("Error in immediate status update for " + key + ": " + e.getMessage());
                        }
                        return; // Found the service, exit
                    }
                }
            }
        }
    }

    public Map<String, String> getAllStatuses() {
        return new HashMap<>(statusCache);
    }

    private String parseStatus(String ansibleOutput) {
        if (ansibleOutput == null) return "down";
        String lower = ansibleOutput.toLowerCase();
        
        // First check for connection failures and errors
        if (lower.contains("unreachable") || 
            lower.contains("connection test failed") ||
            lower.contains("failed to connect") ||
            lower.contains("operation timed out") ||
            lower.contains("connection to") && lower.contains("timed out") ||
            lower.contains("could not retrieve password") ||
            lower.contains("could not retrieve username") ||
            lower.contains("ansible command execution failed")) {
            return "down";
        }
        
        // For systemctl status output - look for the Active: line specifically
        if (lower.contains("active: active (running)")) return "up";
        if (lower.contains("active: inactive") || lower.contains("active: dead")) return "down";
        if (lower.contains("active: failed")) return "down";
        
        // For other service status patterns
        if (lower.contains("stopped") || lower.contains("inactive") || lower.contains("dead")) return "down";
        if (lower.contains("failed")) return "down";
        
        // For service not found errors
        if (lower.contains("could not be found") || lower.contains("unit") && lower.contains("not found")) return "down";
        
        // For ps aux output (process running)
        if (lower.contains("tomcat") || lower.contains("jboss")) {
            if (lower.contains("grep") && !lower.contains("grep -v grep")) {
                return "down"; // Process not found
            }
            return "up"; // Process found
        }
        
        // For Windows service status
        if (lower.contains("running")) return "up";
        if (lower.contains("stopped")) return "down";
        
        // Default to unknown if we can't determine
        return "unknown";
    }
} 
package com.example.dashboard.service;

import com.example.dashboard.model.Application;
import com.example.dashboard.model.Environment;
import com.example.dashboard.model.Server;
import com.example.dashboard.model.Service;
import com.example.dashboard.model.GroupedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import jakarta.annotation.PreDestroy;

@org.springframework.stereotype.Service
public class ActivatorService {

    @Autowired
    private YamlParserService yamlParserService;

    @Autowired
    private AnsibleExecutionService ansibleExecutionService;

    @Autowired
    private ServiceStatusMonitor serviceStatusMonitor;

    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final List<String> restartLog = new ArrayList<>();

    // Key format: app|env|server|service
    private String makeKey(String app, String env, String server, String service) {
        return app + "|" + env + "|" + server + "|" + service;
    }

    @PostConstruct
    @Scheduled(cron = "0 52 16 * * THU") // Every Thursday at 4:30 PM
    public void autoRestartDownServices() {
        System.out.println("=== Starting Auto-Restart Service Check (Thursday 4:30 PM) ===");
        LocalDateTime now = LocalDateTime.now();
        System.out.println("Current time: " + now);
        
        List<Application> applications = yamlParserService.parseYaml();
        if (applications == null) {
            System.out.println("No applications found in YAML");
            return;
        }

        // First, update all service statuses
        serviceStatusMonitor.checkAllServices();
        
        // Wait a bit for status updates to complete
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // Get current statuses
        Map<String, String> currentStatuses = serviceStatusMonitor.getAllStatuses();
        
        List<String> downServices = new ArrayList<>();
        List<String> restartAttempts = new ArrayList<>();

        // Group services by their group tag
        Map<String, List<GroupedService>> groupedServices = groupServicesByGroup(applications, currentStatuses);
        
        // Handle grouped services first
        for (Map.Entry<String, List<GroupedService>> entry : groupedServices.entrySet()) {
            String groupName = entry.getKey();
            List<GroupedService> groupServices = entry.getValue();
            
            // Check if any service in the group is down
            boolean hasDownService = groupServices.stream()
                .anyMatch(gs -> "down".equals(gs.getStatus()));
            
            if (hasDownService) {
                System.out.println("=== Group '" + groupName + "' has down services. Performing coordinated restart ===");
                String result = restartGroupedServices(groupServices);
                restartAttempts.add("Group " + groupName + ": " + result);
            }
        }

        // Handle non-grouped services (existing logic)
        List<Future<String>> futures = new ArrayList<>();

        for (Application app : applications) {
            if (app.getEnvironments() == null) continue;
            for (Environment env : app.getEnvironments()) {
                if (env.getServers() == null) continue;
                for (Server server : env.getServers()) {
                    if (server.getServices() == null) continue;
                    for (Service service : server.getServices()) {
                        // Skip services that are part of a group (already handled above)
                        if (service.getGroup() != null && !service.getGroup().isEmpty()) {
                            continue;
                        }
                        
                        String key = makeKey(app.getName(), env.getName(), server.getName(), service.getName());
                        String status = currentStatuses.get(key);
                        
                        if ("down".equals(status)) {
                            downServices.add(key);
                            
                            // Submit restart task to thread pool for parallel execution
                            Future<String> future = executor.submit(() -> {
                                try {
                                    System.out.println("Thread " + Thread.currentThread().getName() + " starting restart for: " + key);
                                    String result = restartService(app.getName(), env.getName(), server.getName(), service.getName(), server, service);
                                    System.out.println("Thread " + Thread.currentThread().getName() + " completed restart for: " + key + " -> " + result);
                                    return key + " -> " + result;
                                } catch (Exception e) {
                                    String errorMsg = "ERROR: " + e.getMessage();
                                    System.err.println("Thread " + Thread.currentThread().getName() + " failed restart for: " + key + " -> " + errorMsg);
                                    return key + " -> " + errorMsg;
                                }
                            });
                            
                            futures.add(future);
                        }
                    }
                }
            }
        }
        
        // Wait for all restart tasks to complete
        System.out.println("Waiting for " + futures.size() + " restart tasks to complete...");
        for (Future<String> future : futures) {
            try {
                String result = future.get(5, TimeUnit.MINUTES); // 5 minute timeout per service
                restartAttempts.add(result);
            } catch (TimeoutException e) {
                String timeoutMsg = "TIMEOUT: Service restart timed out after 5 minutes";
                System.err.println(timeoutMsg);
                restartAttempts.add("UNKNOWN -> " + timeoutMsg);
            } catch (Exception e) {
                String errorMsg = "ERROR: " + e.getMessage();
                System.err.println(errorMsg);
                restartAttempts.add("UNKNOWN -> " + errorMsg);
            }
        }

        // Log the results
        String logEntry = String.format("[%s] Auto-restart check completed. Down services found: %d, Restart attempts: %d", 
            now, downServices.size(), restartAttempts.size());
        restartLog.add(logEntry);
        
        if (!downServices.isEmpty()) {
            restartLog.add("Down services: " + String.join(", ", downServices));
        }
        if (!restartAttempts.isEmpty()) {
            restartLog.add("Restart results: " + String.join("; ", restartAttempts));
        }
        
        // Keep only last 100 log entries
        if (restartLog.size() > 100) {
            restartLog.subList(0, restartLog.size() - 100).clear();
        }
        
        System.out.println(logEntry);
        
        // IMMEDIATE STATUS REFRESH: Update all service statuses after auto-restart
        if (!downServices.isEmpty() || !groupedServices.isEmpty()) {
            System.out.println("=== Refreshing service statuses after auto-restart ===");
            try {
                // Wait a bit for services to fully start up
                Thread.sleep(15000); // 15 seconds wait
                
                // Trigger immediate status check for all services
                serviceStatusMonitor.checkAllServices();
                
                // Wait for status updates to complete
                Thread.sleep(5000);
                
                System.out.println("=== Service statuses refreshed after auto-restart ===");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Status refresh interrupted: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Error refreshing statuses: " + e.getMessage());
            }
        }
        
        System.out.println("=== Auto-Restart Service Check Completed ===");
    }

    private String restartService(String appName, String envName, String serverName, String serviceName, 
                                 Server server, Service service) {
        try {
            String os = server.getOs();
            String ip = server.getIp();
            String application = appName;
            
            String restartCmd = service.getStartupCmd();
            if (restartCmd == null) {
                return "ERROR: No startup command configured";
            }

            // Prepend sudo for Linux commands
            if (os != null && !os.equalsIgnoreCase("windows") && !restartCmd.startsWith("sudo ")) {
                restartCmd = "sudo " + restartCmd;
            }

            System.out.println("Attempting to restart service: " + serviceName + " on " + serverName);
            String result = ansibleExecutionService.executeCommand(application, ip, restartCmd, os);
            
            if (result.startsWith("SUCCESS")) {
                // Wait a bit for service to start
                Thread.sleep(10000);
                
                // Check if service is now up
                String statusCmd = service.getStatusCmd();
                if (statusCmd != null) {
                    if (os != null && !os.equalsIgnoreCase("windows") && !statusCmd.startsWith("sudo ")) {
                        statusCmd = "sudo " + statusCmd;
                    }
                    String statusResult = ansibleExecutionService.executeCommand(application, ip, statusCmd, os);
                    if (statusResult.toLowerCase().contains("running") || statusResult.toLowerCase().contains("active")) {
                        return "SUCCESS: Service restarted and is now running";
                    } else {
                        return "PARTIAL: Service restarted but status unclear";
                    }
                }
                return "SUCCESS: Service restart command executed";
            } else {
                return "FAILED: " + result;
            }
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    // Manual trigger method for testing
    public String triggerManualRestart() {
        System.out.println("=== Manual Auto-Restart Triggered ===");
        
        // Run the auto-restart logic in a separate thread to avoid blocking
        executor.submit(() -> {
            try {
                autoRestartDownServices();
            } catch (Exception e) {
                System.err.println("Manual restart failed: " + e.getMessage());
            }
        });
        
        return "Manual auto-restart initiated. Check logs for details.";
    }

    // Get restart logs
    public List<String> getRestartLogs() {
        return new ArrayList<>(restartLog);
    }

    // Get current down services
    public List<String> getCurrentDownServices() {
        Map<String, String> currentStatuses = serviceStatusMonitor.getAllStatuses();
        List<String> downServices = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : currentStatuses.entrySet()) {
            if ("down".equals(entry.getValue())) {
                downServices.add(entry.getKey());
            }
        }
        
        return downServices;
    }

    // Get next scheduled restart time
    public String getNextScheduledRestart() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextThursday = now;
        
        // Find next Thursday
        while (nextThursday.getDayOfWeek() != DayOfWeek.THURSDAY) {
            nextThursday = nextThursday.plusDays(1);
        }
        
        // Set to 4:30 PM
        nextThursday = nextThursday.withHour(16).withMinute(30).withSecond(0).withNano(0);
        
        // If it's already past 4:30 PM on Thursday, move to next Thursday
        if (now.getDayOfWeek() == DayOfWeek.THURSDAY && now.isAfter(nextThursday)) {
            nextThursday = nextThursday.plusWeeks(1);
        }
        
        return nextThursday.toString();
    }
    
    // Manual status refresh method
    public void refreshServiceStatuses() {
        System.out.println("=== Manual service status refresh triggered ===");
        try {
            // Trigger immediate status check for all services
            serviceStatusMonitor.checkAllServices();
            
            // Wait for status updates to complete
            Thread.sleep(5000);
            
            System.out.println("=== Manual service status refresh completed ===");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Manual status refresh interrupted: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error during manual status refresh: " + e.getMessage());
        }
    }
    
    @PreDestroy
    public void shutdown() {
        System.out.println("Shutting down ActivatorService thread pool...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Helper class to hold grouped service information


    // Group services by their group tag
    private Map<String, List<GroupedService>> groupServicesByGroup(List<Application> applications, Map<String, String> currentStatuses) {
        Map<String, List<GroupedService>> groupedServices = new HashMap<>();
        
        for (Application app : applications) {
            if (app.getEnvironments() == null) continue;
            for (Environment env : app.getEnvironments()) {
                if (env.getServers() == null) continue;
                for (Server server : env.getServers()) {
                    if (server.getServices() == null) continue;
                    for (Service service : server.getServices()) {
                        if (service.getGroup() != null && !service.getGroup().isEmpty()) {
                            String groupName = service.getGroup();
                            String key = makeKey(app.getName(), env.getName(), server.getName(), service.getName());
                            String status = currentStatuses.get(key);
                            
                                                    GroupedService groupedService = new GroupedService(
                            app.getName(), env.getName(), server.getName(), service.getName(),
                            server, service, status, key, service.getGroup()
                        );
                            
                            groupedServices.computeIfAbsent(groupName, k -> new ArrayList<>()).add(groupedService);
                        }
                    }
                }
            }
        }
        
        return groupedServices;
    }

    // Restart grouped services in the correct order
    private String restartGroupedServices(List<GroupedService> groupServices) {
        try {
            System.out.println("=== Starting coordinated restart for group with " + groupServices.size() + " services ===");
            
            // Sort services: master first (ES-M), then slaves (ES-1, ES-2, etc.)
            groupServices.sort((a, b) -> {
                // If one is master (ends with -M), it should come first
                boolean aIsMaster = a.getServiceName().endsWith("-M");
                boolean bIsMaster = b.getServiceName().endsWith("-M");
                
                if (aIsMaster && !bIsMaster) return -1;
                if (!aIsMaster && bIsMaster) return 1;
                
                // If both are master or both are slave, sort alphabetically
                return a.getServiceName().compareTo(b.getServiceName());
            });
            
            System.out.println("Service restart order: " + groupServices.stream()
                .map(GroupedService::getServiceName)
                .collect(java.util.stream.Collectors.joining(" -> ")));
            
            // Step 1: Stop all services in reverse order (slaves first, then master)
            System.out.println("=== Step 1: Stopping services in reverse order ===");
            List<GroupedService> reverseOrder = new ArrayList<>(groupServices);
            java.util.Collections.reverse(reverseOrder);
            
            for (GroupedService gs : reverseOrder) {
                System.out.println("Stopping service: " + gs.getServiceName());
                String stopResult = stopService(gs.getAppName(), gs.getEnvName(), gs.getServerName(), 
                                              gs.getServiceName(), gs.getServer(), gs.getService());
                System.out.println("Stop result for " + gs.getServiceName() + ": " + stopResult);
                
                // Wait a bit between stops
                Thread.sleep(5000);
            }
            
            // Step 2: Start all services in correct order (master first, then slaves)
            System.out.println("=== Step 2: Starting services in correct order ===");
            for (GroupedService gs : groupServices) {
                System.out.println("Starting service: " + gs.getServiceName());
                String startResult = startService(gs.getAppName(), gs.getEnvName(), gs.getServerName(), 
                                                gs.getServiceName(), gs.getServer(), gs.getService());
                System.out.println("Start result for " + gs.getServiceName() + ": " + startResult);
                
                // Wait a bit between starts
                Thread.sleep(10000);
            }
            
            System.out.println("=== Coordinated restart completed ===");
            return "SUCCESS: All services in group restarted successfully";
            
        } catch (Exception e) {
            String errorMsg = "ERROR: " + e.getMessage();
            System.err.println("Coordinated restart failed: " + errorMsg);
            return errorMsg;
        }
    }

    // Stop a specific service
    private String stopService(String appName, String envName, String serverName, String serviceName, 
                              Server server, Service service) {
        try {
            String os = server.getOs();
            String ip = server.getIp();
            String application = appName;
            
            String stopCmd = service.getStopCmd();
            if (stopCmd == null) {
                return "ERROR: No stop command configured";
            }

            // Prepend sudo for Linux commands
            if (os != null && !os.equalsIgnoreCase("windows") && !stopCmd.startsWith("sudo ")) {
                stopCmd = "sudo " + stopCmd;
            }

            System.out.println("Stopping service: " + serviceName + " on " + serverName);
            String result = ansibleExecutionService.executeCommand(application, ip, stopCmd, os);
            
            if (result.startsWith("SUCCESS")) {
                return "SUCCESS: Service stopped successfully";
            } else {
                return "FAILED: " + result;
            }
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    // Start a specific service
    private String startService(String appName, String envName, String serverName, String serviceName, 
                               Server server, Service service) {
        try {
            String os = server.getOs();
            String ip = server.getIp();
            String application = appName;
            
            String startCmd = service.getStartupCmd();
            if (startCmd == null) {
                return "ERROR: No startup command configured";
            }

            // Prepend sudo for Linux commands
            if (os != null && !os.equalsIgnoreCase("windows") && !startCmd.startsWith("sudo ")) {
                startCmd = "sudo " + startCmd;
            }

            System.out.println("Starting service: " + serviceName + " on " + serverName);
            String result = ansibleExecutionService.executeCommand(application, ip, startCmd, os);
            
            if (result.startsWith("SUCCESS")) {
                // Wait a bit for service to start
                Thread.sleep(10000);
                
                // Check if service is now up
                String statusCmd = service.getStatusCmd();
                if (statusCmd != null) {
                    if (os != null && !os.equalsIgnoreCase("windows") && !statusCmd.startsWith("sudo ")) {
                        statusCmd = "sudo " + statusCmd;
                    }
                    String statusResult = ansibleExecutionService.executeCommand(application, ip, statusCmd, os);
                    if (statusResult.toLowerCase().contains("running") || statusResult.toLowerCase().contains("active")) {
                        return "SUCCESS: Service started and is now running";
                    } else {
                        return "PARTIAL: Service started but status unclear";
                    }
                }
                return "SUCCESS: Service start command executed";
            } else {
                return "FAILED: " + result;
            }
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
} 
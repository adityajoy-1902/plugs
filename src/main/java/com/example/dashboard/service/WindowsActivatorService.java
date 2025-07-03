package com.example.dashboard.service;

import com.example.dashboard.model.Application;
import com.example.dashboard.model.Environment;
import com.example.dashboard.model.Server;
import com.example.dashboard.model.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.io.IOException;

@org.springframework.stereotype.Service
public class WindowsActivatorService {

    @Autowired
    private YamlParserService yamlParserService;

    @Autowired
    private AnsibleExecutionService ansibleExecutionService;

    @Autowired
    private ServiceStatusMonitor serviceStatusMonitor;

    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final List<String> restartLog = new ArrayList<>();
    
    // Windows-specific configuration
    private final String WINDOWS_SERVICE_NAME = "TPLM-Dashboard";
    private final String APPLICATION_JAR_PATH = "C:\\apps\\dashboard.jar";
    private final String JAVA_HOME = System.getenv("JAVA_HOME");

    // Key format: app|env|server|service
    private String makeKey(String app, String env, String server, String service) {
        return app + "|" + env + "|" + server + "|" + service;
    }

    @PostConstruct
    @Scheduled(cron = "0 30 16 * * THU") // Every Thursday at 4:30 PM
    public void autoRestartDownServices() {
        System.out.println("=== Starting Windows Auto-Restart Service Check (Thursday 4:30 PM) ===");
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

        // MULTITHREADED RESTART: Submit all restart tasks to thread pool
        List<Future<String>> futures = new ArrayList<>();

        for (Application app : applications) {
            if (app.getEnvironments() == null) continue;
            for (Environment env : app.getEnvironments()) {
                if (env.getServers() == null) continue;
                for (Server server : env.getServers()) {
                    if (server.getServices() == null) continue;
                    for (Service service : server.getServices()) {
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
        String logEntry = String.format("[%s] Windows auto-restart check completed. Down services found: %d, Restart attempts: %d", 
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
        if (!downServices.isEmpty()) {
            System.out.println("=== Refreshing service statuses after Windows auto-restart ===");
            try {
                // Wait a bit for services to fully start up
                Thread.sleep(15000); // 15 seconds wait
                
                // Trigger immediate status check for all services
                serviceStatusMonitor.checkAllServices();
                
                // Wait for status updates to complete
                Thread.sleep(5000);
                
                System.out.println("=== Service statuses refreshed after Windows auto-restart ===");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Status refresh interrupted: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Error refreshing statuses: " + e.getMessage());
            }
        }
        
        System.out.println("=== Windows Auto-Restart Service Check Completed ===");
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

            // Windows-specific command adjustments
            if (os != null && os.equalsIgnoreCase("windows")) {
                // Use Windows service commands
                restartCmd = "Start-Service -Name " + serviceName;
            } else {
                // Prepend sudo for Linux commands
                if (!restartCmd.startsWith("sudo ")) {
                    restartCmd = "sudo " + restartCmd;
                }
            }

            System.out.println("Attempting to restart service: " + serviceName + " on " + serverName);
            String result = ansibleExecutionService.executeCommand(application, ip, restartCmd, os);
            
            if (result.startsWith("SUCCESS")) {
                // Wait a bit for service to start
                Thread.sleep(10000);
                
                // Check if service is now up
                String statusCmd = service.getStatusCmd();
                if (statusCmd != null) {
                    if (os != null && os.equalsIgnoreCase("windows")) {
                        statusCmd = "Get-Service -Name " + serviceName;
                    } else if (!statusCmd.startsWith("sudo ")) {
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

    // Windows-specific method to restart the dashboard application itself
    public String restartDashboardApplication() {
        try {
            System.out.println("=== Restarting TPLM Dashboard Application ===");
            
            // Check if running as Windows Service
            if (isRunningAsWindowsService()) {
                return restartWindowsService();
            } else {
                return restartAsJavaProcess();
            }
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private boolean isRunningAsWindowsService() {
        try {
            Process process = Runtime.getRuntime().exec("sc query " + WINDOWS_SERVICE_NAME);
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String restartWindowsService() throws IOException, InterruptedException {
        System.out.println("Restarting Windows Service: " + WINDOWS_SERVICE_NAME);
        
        // Stop the service
        Process stopProcess = Runtime.getRuntime().exec("sc stop " + WINDOWS_SERVICE_NAME);
        stopProcess.waitFor();
        
        // Wait a bit
        Thread.sleep(5000);
        
        // Start the service
        Process startProcess = Runtime.getRuntime().exec("sc start " + WINDOWS_SERVICE_NAME);
        int exitCode = startProcess.waitFor();
        
        if (exitCode == 0) {
            return "SUCCESS: Windows service restarted successfully";
        } else {
            return "FAILED: Windows service restart failed with exit code " + exitCode;
        }
    }

    private String restartAsJavaProcess() throws IOException, InterruptedException {
        System.out.println("Restarting Java process");
        
        // Create restart script
        String restartScript = createRestartScript();
        
        // Execute restart script
        Process process = Runtime.getRuntime().exec("cmd /c " + restartScript);
        int exitCode = process.waitFor();
        
        if (exitCode == 0) {
            return "SUCCESS: Java process restart initiated";
        } else {
            return "FAILED: Java process restart failed with exit code " + exitCode;
        }
    }

    private String createRestartScript() {
        StringBuilder script = new StringBuilder();
        script.append("@echo off\n");
        script.append("echo Stopping TPLM Dashboard...\n");
        script.append("taskkill /f /im java.exe /fi \"WINDOWTITLE eq TPLM Dashboard\"\n");
        script.append("timeout /t 5 /nobreak > nul\n");
        script.append("echo Starting TPLM Dashboard...\n");
        script.append("\"").append(JAVA_HOME).append("\\bin\\java.exe\" -jar \"").append(APPLICATION_JAR_PATH).append("\"\n");
        
        return script.toString();
    }

    // Manual trigger method for testing
    public String triggerManualRestart() {
        System.out.println("=== Manual Windows Auto-Restart Triggered ===");
        
        // Run the auto-restart logic in a separate thread to avoid blocking
        executor.submit(() -> {
            try {
                autoRestartDownServices();
            } catch (Exception e) {
                System.err.println("Manual restart failed: " + e.getMessage());
            }
        });
        
        return "Manual Windows auto-restart initiated. Check logs for details.";
    }

    // Manual status refresh method
    public void refreshServiceStatuses() {
        System.out.println("=== Manual Windows service status refresh triggered ===");
        try {
            // Trigger immediate status check for all services
            serviceStatusMonitor.checkAllServices();
            
            // Wait for status updates to complete
            Thread.sleep(5000);
            
            System.out.println("=== Manual Windows service status refresh completed ===");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Manual status refresh interrupted: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error during manual status refresh: " + e.getMessage());
        }
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
    
    @PreDestroy
    public void shutdown() {
        System.out.println("Shutting down Windows ActivatorService thread pool...");
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
} 
package com.example.dashboard.controller;

import com.example.dashboard.service.AnsibleExecutionService;
import com.example.dashboard.service.ServiceStatusMonitor;
import com.example.dashboard.model.CommandRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ServiceController {

    @Autowired
    private AnsibleExecutionService ansibleExecutionService;

    @Autowired
    private ServiceStatusMonitor serviceStatusMonitor;

    @PostMapping("/service-operation")
    public ResponseEntity<Map<String, String>> serviceOperation(
            @RequestBody ServiceOperationRequest request,
            HttpServletRequest httpRequest) {

        // Log the request details
        System.out.println("Service operation request received for: " + request.getAppName());
        System.out.println("Server: " + request.getServerName() + " (" + request.getServerIp() + ")");
        System.out.println("OS: " + request.getServerOs());
        System.out.println("Service: " + request.getServiceName());
        System.out.println("Type: " + request.getServiceType());
        System.out.println("Operation: " + request.getOperation());
        System.out.println("startupCmd: " + request.getStartupCmd());
        System.out.println("startScript: " + request.getStartScript());
        System.out.println("statusCmd: " + request.getStatusCmd());
        System.out.println("statusScript: " + request.getStatusScript());
        System.out.println("stopCmd: " + request.getStopCmd());
        System.out.println("stopScript: " + request.getStopScript());

        // Execute the operation
        String command = null;
        String op = request.getOperation();
        String os = request.getServerOs();
        if (op == null) op = "status";
        if (op.equalsIgnoreCase("status")) {
            command = (os != null && os.equalsIgnoreCase("windows")) ? request.getStatusCmd() : (request.getStatusCmd() != null ? request.getStatusCmd() : request.getStatusScript());
        } else if (op.equalsIgnoreCase("restart") || op.equalsIgnoreCase("start")) {
            command = (os != null && os.equalsIgnoreCase("windows")) ? request.getStartupCmd() : (request.getStartupCmd() != null ? request.getStartupCmd() : request.getStartScript());
            // Prepend sudo for Linux service operations (start/restart)
            if (os != null && !os.equalsIgnoreCase("windows") && command != null && !command.startsWith("sudo ")) {
                command = "sudo " + command;
            }
        } else if (op.equalsIgnoreCase("stop")) {
            command = (os != null && os.equalsIgnoreCase("windows")) ? request.getStopCmd() : (request.getStopCmd() != null ? request.getStopCmd() : request.getStopScript());
            // Prepend sudo for Linux service operations (stop)
            if (os != null && !os.equalsIgnoreCase("windows") && command != null && !command.startsWith("sudo ")) {
                command = "sudo " + command;
            }
        }
        if (command == null || command.isEmpty()) {
            command = "echo No command provided";
        }
        String result = ansibleExecutionService.executeCommand(
                request.getAppName(),
                request.getServerIp(),
                command,
                request.getServerOs()
        );

        // Log the result
        System.out.println("Service operation result: " + result);

        // If operation was successful, immediately update the service status
        if (result != null && result.contains("SUCCESS")) {
            System.out.println("Operation successful, updating service status immediately...");
            // Extract environment name from the application name or use a default
            String envName = "Production"; // You might want to extract this from the request or config
            serviceStatusMonitor.updateServiceStatus(
                request.getAppName(),
                envName,
                request.getServerName(),
                request.getServiceName()
            );
        }

        // Return response
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", request.getOperation() + " command executed");
        response.put("details", result);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/ping-server")
    public ResponseEntity<?> pingServer(@RequestBody CommandRequest request) {
        String result = ansibleExecutionService.pingServer(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/test-status")
    public ResponseEntity<Map<String, String>> testStatus() {
        String result = ansibleExecutionService.checkServiceStatus(
            "GCP Mumbai Production Environment",
            "34.47.226.211",
            "systemctl status tomcat",
            "linux"
        );

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Test status command executed");
        response.put("details", result);

        return ResponseEntity.ok(response);
    }

    // Request DTO
    public static class ServiceOperationRequest {
        private String appName;
        private String serverName;
        private String serverIp;
        private String serverOs;
        private String serviceName;
        private String serviceType;
        private String operation; // status, restart, start, stop
        private String startupCmd;
        private String startScript;
        private String statusCmd;
        private String statusScript;
        private String stopCmd;
        private String stopScript;

        // Getters and setters
        public String getAppName() { return appName; }
        public void setAppName(String appName) { this.appName = appName; }

        public String getServerName() { return serverName; }
        public void setServerName(String serverName) { this.serverName = serverName; }

        public String getServerIp() { return serverIp; }
        public void setServerIp(String serverIp) { this.serverIp = serverIp; }

        public String getServerOs() { return serverOs; }
        public void setServerOs(String serverOs) { this.serverOs = serverOs; }

        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }

        public String getServiceType() { return serviceType; }
        public void setServiceType(String serviceType) { this.serviceType = serviceType; }

        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }

        public String getStartupCmd() { return startupCmd; }
        public void setStartupCmd(String startupCmd) { this.startupCmd = startupCmd; }

        public String getStartScript() { return startScript; }
        public void setStartScript(String startScript) { this.startScript = startScript; }

        public String getStatusCmd() { return statusCmd; }
        public void setStatusCmd(String statusCmd) { this.statusCmd = statusCmd; }

        public String getStatusScript() { return statusScript; }
        public void setStatusScript(String statusScript) { this.statusScript = statusScript; }

        public String getStopCmd() { return stopCmd; }
        public void setStopCmd(String stopCmd) { this.stopCmd = stopCmd; }

        public String getStopScript() { return stopScript; }
        public void setStopScript(String stopScript) { this.stopScript = stopScript; }
    }
} 
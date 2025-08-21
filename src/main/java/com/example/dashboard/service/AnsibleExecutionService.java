package com.example.dashboard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import com.example.dashboard.model.CommandRequest;

@Service
public class AnsibleExecutionService {

    @Autowired
    private CyberArkService cyberArkService;

    public String executeCommand(String application, String ip, String command, String os) {
        try {
            Map<String, String> credentials = cyberArkService.getcreds(application, os, ip);
            String user = credentials.get("username");
            String password = credentials.get("password");

            if (password == null) {
                return "ERROR: Could not retrieve password for server: " + ip;
            }

            if (user == null) {
                return "ERROR: Could not retrieve username for server: " + ip;
            }

            // First check connectivity
            String connectionStatus = checkServerConnection(user, password, os, ip);
            if (!connectionStatus.startsWith("SUCCESS")) {
                return connectionStatus;
            }

            // Prepare the Ansible command
            String ansibleCmd;
            if (os.equalsIgnoreCase("windows")) {
                ansibleCmd = String.format(
                        "ansible all -i '%s,' -u %s --extra-vars \"ansible_password=%s ansible_connection=winrm ansible_winrm_transport=ntlm ansible_port=5986 ansible_winrm_server_cert_validation=ignore ansible_winrm_operation_timeout_sec=280 ansible_winrm_read_timeout_sec=300\" -m win_shell -a '%s'",
                        ip, user, password, command
                );
            } else {
                ansibleCmd = String.format(
                        "ansible all -i '%s,' -u %s --extra-vars \"ansible_password=%s\" --ssh-common-args='-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o PreferredAuthentications=password -o ConnectTimeout=30' -m shell -a '%s'",
                        ip, user, password, command
                );
            }

            // Log command execution
            System.out.println("Executing Ansible command on " + os + " server: " + ip);

            // Execute the command
            ProcessBuilder processBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                processBuilder.command("cmd.exe", "/c", ansibleCmd);
            } else {
                processBuilder.command("sh", "-c", ansibleCmd);
            }

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Read output silently
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            System.out.println("Command completed with exit code: " + exitCode);

            if (exitCode == 0) {
                return "SUCCESS: Command executed successfully on " + os + " server.\nOutput: " + output.toString();
            } else {
                return "ERROR: Command failed with exit code " + exitCode + " on " + os + " server.\n" +
                        "Details:\n" + output.toString() + "\n" +
                        "Please check:\n" +
                        "1. Command syntax\n" +
                        "2. User permissions\n" +
                        "3. Service availability on target machine";
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return "ERROR: Ansible command execution failed: " + e.getMessage() + "\nStack trace: " + sw.toString();
        }
    }

    private String checkServerConnection(String user, String password, String os, String ip) {
        try {
            String ansibleCmd;
            if (os.equalsIgnoreCase("windows")) {
                ansibleCmd = String.format(
                        "ansible all -i '%s,' -u %s --extra-vars \"ansible_password=%s ansible_connection=winrm ansible_winrm_transport=ntlm ansible_port=5986 ansible_winrm_server_cert_validation=ignore ansible_winrm_operation_timeout_sec=280 ansible_winrm_read_timeout_sec=300\" -m win_ping",
                        ip, user, password
                );
            } else {
                ansibleCmd = String.format(
                        "ansible all -i '%s,' -u %s --extra-vars \"ansible_password=%s\" --ssh-common-args='-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o PreferredAuthentications=password -o ConnectTimeout=30' -m ping",
                        ip, user, password
                );
            }

            System.out.println("Testing connection to " + os + " server: " + ip);

            ProcessBuilder processBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                processBuilder.command("cmd.exe", "/c", ansibleCmd);
            } else {
                processBuilder.command("sh", "-c", ansibleCmd);
            }

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            System.out.println("Connection test completed with exit code: " + exitCode);

            if (exitCode == 0) {
                System.out.println("Connection successful to " + os + " server: " + ip);
                return "SUCCESS: Connection established successfully.";
            } else {
                String errorMsg = String.format(
                        "ERROR: Connection test failed with exit code %d.\n" +
                                "Server may be unreachable or credentials may be invalid.\n" +
                                "Error details:\n%s\n" +
                                "Troubleshooting steps:\n" +
                                "1. Verify network connectivity to %s\n" +
                                "2. Check if %s service is running on the server\n" +
                                "3. Verify firewall rules\n" +
                                "4. Confirm credentials are valid",
                        exitCode, output.toString(), ip, os.equalsIgnoreCase("windows") ? "WinRM" : "SSH");
                System.err.println(errorMsg);
                return errorMsg;
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String errorMsg = "ERROR: Connection test failed: " + e.getMessage() +
                    "\nStack trace: " + sw.toString();
            System.err.println(errorMsg);
            return errorMsg;
        }
    }

    // Additional helper methods
    public String checkServiceStatus(String application, String ip, String statusCommand, String os) {
        return executeCommand(application, ip, statusCommand, os);
    }

    public String startService(String application, String ip, String startCommand, String os) {
        return executeCommand(application, ip, startCommand, os);
    }

    public String stopService(String application, String ip, String stopCommand, String os) {
        return executeCommand(application, ip, stopCommand, os);
    }

    public String pingServer(CommandRequest request) {
        String os = request.getOs();
        String ip = request.getIp();
        String application = request.getApplication();

        Map<String, String> creds = cyberArkService.getcreds(application, os, ip);
        String username = creds.get("username");
        String password = creds.get("password");

        String inventoryFile = null;
        String ansibleCommand = null;
        String output = "";

        try {
            // Create temporary inventory file
            File tempInventory = File.createTempFile("ansible_inventory_ping_", ".ini");
            inventoryFile = tempInventory.getAbsolutePath();
            String inventoryContent;
            if (os.equalsIgnoreCase("windows")) {
                inventoryContent = "[servers]\n" +
                    ip + " ansible_user=" + username +
                    " ansible_password=" + password +
                    " ansible_connection=winrm ansible_port=5986 ansible_winrm_server_cert_validation=ignore ansible_winrm_message_encryption=auto ansible_winrm_transport=ntlm\n";
                ansibleCommand = "OBJC_DISABLE_INITIALIZE_FORK_SAFETY=YES python3.11 -m ansible adhoc -i " + inventoryFile + " servers -m win_ping";
            } else {
                inventoryContent = "[servers]\n" +
                    ip + " ansible_user=" + username +
                    " ansible_password=" + password +
                    " ansible_connection=ssh\n";
                ansibleCommand = "ansible -i " + inventoryFile + " servers -m ping";
            }
            Files.write(tempInventory.toPath(), inventoryContent.getBytes());

            // Run the command
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", ansibleCommand);
            pb.environment().put("SSHPASS", password);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            process.waitFor();
            output = sb.toString();

            // Clean up
            tempInventory.delete();

        } catch (Exception e) {
            output = "Error: " + e.getMessage();
        }
        return output;
    }
} 
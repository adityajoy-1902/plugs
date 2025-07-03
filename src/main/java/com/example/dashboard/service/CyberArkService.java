package com.example.dashboard.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class CyberArkService {

    public Map<String, String> getcreds(String application, String os, String ip) {
        // This is a placeholder implementation
        // In a real implementation, this would connect to CyberArk to retrieve credentials
        Map<String, String> credentials = new HashMap<>();
        
        // Linux server credentials (34.47.226.211)
        if (ip.equals("34.47.226.211") && os.equalsIgnoreCase("linux")) {
            credentials.put("username", "dashboard_user");
            credentials.put("password", "password123");
        }
        // Windows server credentials (34.100.134.103)
        else if (ip.equals("34.100.134.103") && os.equalsIgnoreCase("windows")) {
            credentials.put("username", "adit");
            credentials.put("password", "?Jb16)c1Md}B[eA");
        }
        // Default credentials
        else {
            credentials.put("username", "admin");
            credentials.put("password", "password");
        }
        
        return credentials;
    }

    public String getUsername(String application, String os, String ip) {
        // Linux server credentials (34.47.226.211)
        if (ip.equals("34.47.226.211") && os.equalsIgnoreCase("linux")) {
            return "dashboard_user";
        }
        // Windows server credentials (34.100.134.103)
        else if (ip.equals("34.100.134.103") && os.equalsIgnoreCase("windows")) {
            return "adit";
        }
        // Default credentials
        return "admin";
    }

    public String getPasswordForServer(String application, String os, String ip) {
        // Linux server credentials (34.47.226.211)
        if (ip.equals("34.47.226.211") && os.equalsIgnoreCase("linux")) {
            return "password123";
        }
        // Windows server credentials (34.100.134.103)
        else if (ip.equals("34.100.134.103") && os.equalsIgnoreCase("windows")) {
            return "?Jb16)c1Md}B[eA";
        }
        // Default credentials
        return "password";
    }
} 
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
        
        // Linux server credentials (35.200.187.236)
        if (ip.equals("35.200.187.236") && os.equalsIgnoreCase("linux")) {
            credentials.put("username", "aditya2219jain");
            credentials.put("password", "pass");
        }
        // Windows server credentials (35.200.246.189)
        else if (ip.equals("35.200.246.189") && os.equalsIgnoreCase("windows")) {
            credentials.put("username", "aditya");
            credentials.put("password", "t}ER_~ZJVre5ZG!");
        }
        // Default credentials
        else {
            credentials.put("username", "admin");
            credentials.put("password", "password");
        }
        
        return credentials;
    }

    public String getUsername(String application, String os, String ip) {
        // Linux server credentials (35.200.187.236)
        if (ip.equals("35.200.187.236") && os.equalsIgnoreCase("linux")) {
            return "dashboard";
        }
        // Windows server credentials (35.200.246.189)
        else if (ip.equals("35.200.246.189") && os.equalsIgnoreCase("windows")) {
            return "aditya";
        }
        // Default credentials
        return "admin";
    }

    public String getPasswordForServer(String application, String os, String ip) {
        // Linux server credentials (35.200.187.236)
        if (ip.equals("35.200.187.236") && os.equalsIgnoreCase("linux")) {
            return "dashboard123";
        }
        // Windows server credentials (35.200.246.189)
        else if (ip.equals("35.200.246.189") && os.equalsIgnoreCase("windows")) {
            return "t}ER_~ZJVre5ZG!";
        }
        // Default credentials
        return "password";
    }
} 
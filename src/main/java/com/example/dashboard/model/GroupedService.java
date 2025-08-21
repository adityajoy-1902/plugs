package com.example.dashboard.model;

/**
 * Model class representing a service that belongs to a group for coordinated restart operations.
 * This class holds all the necessary information to perform grouped service restarts.
 */
public class GroupedService {
    private final String appName;
    private final String envName;
    private final String serverName;
    private final String serviceName;
    private final Server server;
    private final Service service;
    private final String status;
    private final String key;
    private final String groupName;

    public GroupedService(String appName, String envName, String serverName, String serviceName, 
                        Server server, Service service, String status, String key, String groupName) {
        this.appName = appName;
        this.envName = envName;
        this.serverName = serverName;
        this.serviceName = serviceName;
        this.server = server;
        this.service = service;
        this.status = status;
        this.key = key;
        this.groupName = groupName;
    }

    // Getters
    public String getAppName() { 
        return appName; 
    }
    
    public String getEnvName() { 
        return envName; 
    }
    
    public String getServerName() { 
        return serverName; 
    }
    
    public String getServiceName() { 
        return serviceName; 
    }
    
    public Server getServer() { 
        return server; 
    }
    
    public Service getService() { 
        return service; 
    }
    
    public String getStatus() { 
        return status; 
    }
    
    public String getKey() { 
        return key; 
    }
    
    public String getGroupName() { 
        return groupName; 
    }

    @Override
    public String toString() {
        return String.format("GroupedService{app='%s', env='%s', server='%s', service='%s', group='%s', status='%s'}", 
                           appName, envName, serverName, serviceName, groupName, status);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        GroupedService that = (GroupedService) obj;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
} 
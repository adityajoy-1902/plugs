# Windows Deployment Guide for TPLM Dashboard

## 🪟 **Overview**
This guide explains how to deploy the Spring Boot dashboard application on Windows and use the Windows-specific ActivatorService.

## 📋 **Key Changes for Windows Hosting**

### **1. Application Deployment Options**

#### **Option A: Windows Service (Recommended)**
```batch
# Create Windows Service
sc create "TPLM-Dashboard" binPath="java -jar C:\apps\dashboard.jar" start=auto
sc description "TPLM-Dashboard" "TPLM Health Check Dashboard Service"
sc start "TPLM-Dashboard"

# Service Management
sc stop "TPLM-Dashboard"
sc start "TPLM-Dashboard"
sc delete "TPLM-Dashboard"
```

#### **Option B: Windows Task Scheduler**
```batch
# Create scheduled task
schtasks /create /tn "TPLM-Dashboard" /tr "java -jar C:\apps\dashboard.jar" /sc onstart /ru "SYSTEM"

# Run manually
schtasks /run /tn "TPLM-Dashboard"
```

#### **Option C: PowerShell Script**
```powershell
# Create startup script
$script = @"
Start-Process java -ArgumentList "-jar", "C:\apps\dashboard.jar" -WindowStyle Hidden
"@
$script | Out-File -FilePath "C:\scripts\start-dashboard.ps1" -Encoding UTF8

# Run script
.\start-dashboard.ps1
```

### **2. Windows-Specific Configuration**

#### **Environment Variables**
```batch
# Set JAVA_HOME
setx JAVA_HOME "C:\Program Files\Java\jdk-17"

# Set application path
setx DASHBOARD_HOME "C:\apps"

# Set Ansible path (if using WSL)
setx ANSIBLE_PATH "C:\Windows\System32\wsl.exe ansible"
```

#### **File Structure**
```
C:\apps\
├── dashboard.jar
├── applications.yaml
├── application.properties
└── logs\
    └── dashboard.log
```

## 🔧 **WindowsActivatorService Features**

### **1. Windows Service Management**
```java
// Automatically detects if running as Windows Service
if (isRunningAsWindowsService()) {
    return restartWindowsService();
} else {
    return restartAsJavaProcess();
}
```

### **2. Windows-Specific Commands**
```java
// Windows service commands
restartCmd = "Start-Service -Name " + serviceName;
statusCmd = "Get-Service -Name " + serviceName;

// Linux commands (with sudo)
restartCmd = "sudo " + restartCmd;
```

### **3. Application Self-Restart**
```java
// Restart the dashboard application itself
public String restartDashboardApplication() {
    if (isRunningAsWindowsService()) {
        return restartWindowsService();
    } else {
        return restartAsJavaProcess();
    }
}
```

## 📊 **Deployment Steps**

### **Step 1: Prepare Windows Environment**
```batch
# Install Java 17
# Download from: https://adoptium.net/

# Set JAVA_HOME
setx JAVA_HOME "C:\Program Files\Java\jdk-17"

# Create application directory
mkdir C:\apps
mkdir C:\apps\logs
```

### **Step 2: Build and Deploy Application**
```bash
# Build the application
mvn clean package

# Copy JAR to Windows
scp target/dashboard-0.0.1-SNAPSHOT.jar windows-server:C:\apps\dashboard.jar
```

### **Step 3: Configure Application**
```yaml
# C:\apps\application.properties
server.port=8080
logging.file.name=C:/apps/logs/dashboard.log
logging.level.com.example.dashboard=INFO

# Windows-specific settings
dashboard.windows.service.name=TPLM-Dashboard
dashboard.windows.jar.path=C:\\apps\\dashboard.jar
```

### **Step 4: Create Windows Service**
```batch
# Create service
sc create "TPLM-Dashboard" binPath="\"C:\Program Files\Java\jdk-17\bin\java.exe\" -jar C:\apps\dashboard.jar" start=auto

# Set description
sc description "TPLM-Dashboard" "TPLM Health Check Dashboard - Monitors and manages Linux/Windows services"

# Start service
sc start "TPLM-Dashboard"

# Check status
sc query "TPLM-Dashboard"
```

### **Step 5: Configure Firewall**
```batch
# Allow inbound connections on port 8080
netsh advfirewall firewall add rule name="TPLM Dashboard" dir=in action=allow protocol=TCP localport=8080
```

## 🎯 **WindowsActivatorService vs Original ActivatorService**

### **Key Differences:**

| Feature | Original ActivatorService | WindowsActivatorService |
|---------|---------------------------|-------------------------|
| **Service Commands** | `sudo systemctl` | `Start-Service` / `Get-Service` |
| **Application Restart** | Process management | Windows Service management |
| **Self-Restart** | Not available | `restartDashboardApplication()` |
| **Path Handling** | Unix paths | Windows paths (`C:\apps\`) |
| **Service Detection** | N/A | `isRunningAsWindowsService()` |

### **Shared Features:**
- ✅ **Multithreaded restart operations**
- ✅ **Scheduled cron execution**
- ✅ **Manual trigger capabilities**
- ✅ **Status refresh after restart**
- ✅ **Comprehensive logging**
- ✅ **Error handling and timeouts**

## 🔍 **Monitoring and Troubleshooting**

### **Windows Event Logs**
```powershell
# View application logs
Get-EventLog -LogName Application -Source "TPLM-Dashboard" -Newest 50

# View system logs
Get-EventLog -LogName System -Newest 50 | Where-Object {$_.Message -like "*TPLM*"}
```

### **Service Status Commands**
```batch
# Check service status
sc query "TPLM-Dashboard"

# View service details
sc qc "TPLM-Dashboard"

# Check if service is running
sc query "TPLM-Dashboard" | findstr "RUNNING"
```

### **Application Logs**
```batch
# View application logs
type C:\apps\logs\dashboard.log

# Tail logs in real-time
powershell "Get-Content C:\apps\logs\dashboard.log -Wait"
```

## 🚀 **Performance Optimizations**

### **1. JVM Settings for Windows**
```batch
# Optimized JVM settings
java -Xms512m -Xmx2g -XX:+UseG1GC -jar C:\apps\dashboard.jar
```

### **2. Windows Service Configuration**
```batch
# Set service to restart on failure
sc failure "TPLM-Dashboard" reset=86400 actions=restart/60000/restart/60000/restart/60000
```

### **3. Memory Management**
```properties
# application.properties
spring.jpa.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true
```

## ✅ **Testing the Windows Deployment**

### **1. Test Service Installation**
```batch
# Check if service is installed
sc query "TPLM-Dashboard"

# Test service start/stop
sc stop "TPLM-Dashboard"
sc start "TPLM-Dashboard"
```

### **2. Test Application Access**
```batch
# Test web interface
curl http://localhost:8080

# Test API endpoints
curl http://localhost:8080/api/service-statuses
curl http://localhost:8080/api/activator/status
```

### **3. Test WindowsActivatorService**
```batch
# Trigger manual restart
curl -X POST http://localhost:8080/api/activator/trigger-manual

# Check logs
type C:\apps\logs\dashboard.log | findstr "Windows"
```

## 🚨 **Important Notes**

### **Security Considerations:**
- **Service Account**: Run as Local System or dedicated service account
- **Firewall**: Configure Windows Firewall for required ports
- **Permissions**: Ensure service account has necessary permissions
- **Logging**: Monitor Windows Event Logs for security events

### **Maintenance:**
- **Updates**: Plan for application updates and service restarts
- **Backups**: Regular backup of configuration files
- **Monitoring**: Set up Windows monitoring for service health
- **Log Rotation**: Configure log rotation to prevent disk space issues

### **Troubleshooting:**
- **Service Won't Start**: Check JAVA_HOME and file permissions
- **Port Conflicts**: Verify no other applications use port 8080
- **Memory Issues**: Adjust JVM heap settings based on server capacity
- **Network Issues**: Verify firewall and network connectivity

## 🎉 **Success Criteria**

1. **Service Installation**: Windows service installs and starts successfully
2. **Application Access**: Web interface accessible at http://localhost:8080
3. **Service Monitoring**: All configured services show correct status
4. **Auto-Restart**: WindowsActivatorService restarts down services automatically
5. **Self-Restart**: Application can restart itself when needed
6. **Logging**: All activities logged to Windows Event Logs and application logs
7. **Performance**: Application runs efficiently with minimal resource usage 
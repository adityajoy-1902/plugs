# ActivatorService Test Guide

## Overview
The ActivatorService automatically checks all service statuses and restarts any down services every Thursday at 4:30 PM.

## Features
- ✅ **Scheduled Auto-Restart**: Every Thursday at 4:30 PM
- ✅ **Manual Trigger**: Immediate restart of down services
- ✅ **Status Monitoring**: Real-time tracking of down services
- ✅ **Logging**: Complete audit trail of restart attempts
- ✅ **Cross-Platform**: Works with both Linux and Windows services

## API Endpoints

### 1. Manual Trigger
```bash
POST /api/activator/trigger-manual
```
Triggers immediate restart of all down services.

### 2. Get Status
```bash
GET /api/activator/status
```
Returns current activator status including:
- Number of down services
- Next scheduled restart time
- Recent logs

### 3. Get Logs
```bash
GET /api/activator/logs
```
Returns all restart logs.

### 4. Get Down Services
```bash
GET /api/activator/down-services
```
Returns list of currently down services.

### 5. Get Next Schedule
```bash
GET /api/activator/next-schedule
```
Returns next scheduled restart time.

## Testing Steps

### 1. Start the Application
```bash
export OBJC_DISABLE_INITIALIZE_FORK_SAFETY=YES
mvn spring-boot:run
```

### 2. Access the Dashboard
Navigate to `http://localhost:8081/dashboard`

### 3. Test Manual Trigger
- Scroll down to the "Auto-Restart Service (Activator)" section
- Click "Trigger Manual Restart" button
- Check the logs for restart attempts

### 4. Test API Endpoints
```bash
# Get activator status
curl http://localhost:8081/api/activator/status

# Trigger manual restart
curl -X POST http://localhost:8081/api/activator/trigger-manual

# Get logs
curl http://localhost:8081/api/activator/logs

# Get down services
curl http://localhost:8081/api/activator/down-services
```

### 5. Simulate Down Service
To test the auto-restart functionality:
1. Manually stop a service on one of your servers
2. Wait for the status to show as "down" (red indicator)
3. Click "Trigger Manual Restart" or wait for Thursday 4:30 PM
4. Verify the service gets restarted

## Configuration

### Schedule Configuration
The schedule is configured in `ActivatorService.java`:
```java
@Scheduled(cron = "0 30 16 * * THU") // Every Thursday at 4:30 PM
```

### Service Commands
Service restart commands are defined in `applications.yaml`:
```yaml
services:
  - name: "Tomcat"
    startupCmd: "systemctl start tomcat"  # Linux
  - name: "Tomcat-Windows"
    startupCmd: "Start-Service -Name Tomcat9"  # Windows
```

## Log Format
Logs include:
- Timestamp
- Number of down services found
- Restart attempts and results
- Success/failure status

Example log entry:
```
[2025-07-03T16:30:00] Auto-restart check completed. Down services found: 2, Restart attempts: 2
Down services: GCP Mumbai Production Environment|Production|Linux-Server|Tomcat, GCP Mumbai Production Environment|Production|Windows-Server|IIS
Restart results: GCP Mumbai Production Environment|Production|Linux-Server|Tomcat -> SUCCESS: Service restarted and is now running; GCP Mumbai Production Environment|Production|Windows-Server|IIS -> SUCCESS: Service restarted and is now running
```

## Troubleshooting

### Common Issues
1. **Service not restarting**: Check if startup command is correct in YAML
2. **Permission denied**: Ensure sudo is configured for Linux commands
3. **WinRM issues**: Verify WinRM is properly configured on Windows servers

### Debug Information
- Check application logs for detailed error messages
- Use the dashboard to view real-time status
- Monitor the activator logs for restart attempts

## Security Considerations
- All credentials are fetched from CyberArk service
- Commands are executed with proper authentication
- Logs are maintained for audit purposes
- Manual triggers require dashboard access 
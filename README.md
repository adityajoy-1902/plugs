# TPLM Health Check Dashboard

A Spring Boot web application for monitoring and managing services across multiple servers and environments. This dashboard provides a centralized interface for health checks and service management operations.

## Features

- **Multi-Environment Support**: Manage applications across different environments (Production, Development, etc.)
- **Service Management**: View and restart services on Linux and Windows servers
- **Ansible Integration**: Execute commands remotely using Ansible
- **CyberArk Integration**: Secure credential management (placeholder implementation)
- **Responsive UI**: Modern, responsive web interface built with Bootstrap and custom CSS
- **Security**: Spring Security integration with form-based authentication
- **YAML Configuration**: Flexible configuration using YAML files

## Technology Stack

- **Backend**: Spring Boot 3.2.0, Java 17
- **Frontend**: Thymeleaf, Bootstrap 5, JavaScript
- **Security**: Spring Security
- **Configuration**: YAML with SnakeYAML
- **Build Tool**: Maven
- **Logging**: Logback with SLF4J

## Project Structure

```
src/
├── main/
│   ├── java/com/example/dashboard/
│   │   ├── DashboardApplication.java          # Main application class
│   │   ├── controller/
│   │   │   ├── DashboardController.java       # Main dashboard controller
│   │   │   └── ServiceController.java         # REST API for service operations
│   │   ├── model/
│   │   │   ├── Application.java               # Application model
│   │   │   ├── Environment.java               # Environment model
│   │   │   ├── Server.java                    # Server model
│   │   │   ├── Service.java                   # Service model
│   │   │   ├── ApplicationList.java           # Application list wrapper
│   │   │   ├── YamlConfig.java                # YAML configuration model
│   │   │   └── CommandRequest.java            # Command request model
│   │   ├── service/
│   │   │   ├── AnsibleExecutionService.java   # Ansible command execution
│   │   │   ├── YamlParserService.java         # YAML file parsing
│   │   │   └── CyberArkService.java           # Credential management
│   │   └── config/
│   │       └── SecurityConfig.java            # Spring Security configuration
│   └── resources/
│       ├── static/
│       │   ├── css/
│       │   │   └── dashboard.css              # Custom CSS styles
│       │   └── js/
│       │       └── dashboard.js               # JavaScript functionality
│       ├── templates/
│       │   ├── dashboard.html                 # Main dashboard template
│       │   └── login.html                     # Login page template
│       ├── application.properties             # Application configuration
│       ├── logback-spring.xml                 # Logging configuration
│       ├── yaml-config.yaml                   # YAML files configuration
│       └── applications.yaml                  # Sample applications data
```

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Ansible (for remote command execution)
- Access to target servers (SSH for Linux, WinRM for Windows)

## Installation and Setup

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd dashboard
   ```

2. **Build the application**:
   ```bash
   mvn clean install
   ```

3. **Configure YAML files**:
   - Edit `src/main/resources/yaml-config.yaml` to specify your YAML files
   - Create application configuration files in `src/main/resources/`
   - See `applications.yaml` for the expected format

4. **Configure Ansible**:
   - Ensure Ansible is installed and configured
   - Configure SSH/WinRM access to target servers
   - Update the `CyberArkService` with your credential management system

5. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

6. **Access the application**:
   - Open your browser and navigate to `http://localhost:8081`
   - Login with username: `user`, password: `password`

## Configuration

### Application Properties

Key configuration options in `application.properties`:

```properties
# Server configuration
server.port=8081
server.servlet.context-path=/

# Logging
logging.level.com.example.dashboard=DEBUG
logging.file.name=logs/application.log

# YAML configuration
yaml.config.location=classpath:yaml-config.yaml
```

### YAML Configuration Format

The application expects YAML files with the following structure:

```yaml
applications:
  - name: "Application Name"
    environments:
      - name: "Environment Name"
        servers:
          - name: "Server Name"
            ip: "192.168.1.100"
            os: "linux"  # or "windows"
            services:
              - name: "Service Name"
                type: "app"  # or "db"
                statusCmd: "systemctl status service"
                startupCmd: "systemctl start service"
                statusScript: "/path/to/status/script.sh"
                startScript: "/path/to/start/script.sh"
                # For database services:
                # dbType: "Oracle"
                # tnsAlias: "DBALIAS"
```

## Usage

### Dashboard Interface

1. **Login**: Use the provided credentials to access the dashboard
2. **Navigate**: Use the application and environment tabs to navigate
3. **View Services**: See all services and their configurations
4. **Restart Services**: Click the restart button to restart a service

### API Endpoints

- `GET /dashboard` - Main dashboard page
- `GET /login` - Login page
- `POST /login` - Process login
- `POST /api/restart-service` - Restart a service (REST API)
- `POST /executeCommand` - Execute a custom command

### Service Restart Process

1. User clicks restart button
2. JavaScript collects service information
3. Request sent to `/api/restart-service`
4. Service retrieves credentials from CyberArk
5. Ansible command prepared and executed
6. Results returned to user

## Security Considerations

- **Authentication**: Form-based authentication with Spring Security
- **CSRF Protection**: Enabled by default
- **Credential Management**: Integration with CyberArk (placeholder)
- **Input Validation**: YAML validation and service parameter validation
- **Logging**: Comprehensive logging for audit trails

## Customization

### Adding New Service Types

1. Update the `Service` model class
2. Modify the YAML validation in `YamlParserService`
3. Update the dashboard template
4. Add corresponding JavaScript handling

### Custom Commands

1. Extend `AnsibleExecutionService`
2. Add new endpoints in `ServiceController`
3. Update the frontend to handle new operations

### Styling

- Modify `src/main/resources/static/css/dashboard.css`
- Update Bootstrap classes in templates
- Add custom JavaScript animations

## Troubleshooting

### Common Issues

1. **YAML Parsing Errors**:
   - Check YAML syntax
   - Verify required fields are present
   - Check log files for detailed error messages

2. **Ansible Connection Issues**:
   - Verify SSH/WinRM connectivity
   - Check credentials in CyberArk service
   - Review Ansible inventory format

3. **Service Restart Failures**:
   - Check server connectivity
   - Verify service commands
   - Review application logs

### Logging

- Application logs: `logs/application.log`
- Console output: Check terminal where application is running
- Debug mode: Set `logging.level.com.example.dashboard=DEBUG`

## Development

### Building for Production

```bash
mvn clean package -DskipTests
java -jar target/dashboard-1.0.0.jar
```

### Running Tests

```bash
mvn test
```

### Code Style

- Follow Java naming conventions
- Use Lombok annotations for boilerplate code
- Add comprehensive logging
- Include proper error handling

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:
- Check the troubleshooting section
- Review application logs
- Create an issue in the repository 
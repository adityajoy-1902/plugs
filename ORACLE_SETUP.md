# Oracle Database Integration Setup Guide

This guide explains how to set up Oracle database authentication for the Dashboard application.

## Prerequisites

1. **Oracle Database** (11g, 12c, 19c, or 21c)
2. **Oracle JDBC Driver** (included in pom.xml)
3. **Database Administrator access** to create users and tables

## Database Setup

### 1. Create Database User

Connect to Oracle as a database administrator (SYSDBA) and run:

```sql
-- Create tablespace (optional but recommended)
CREATE TABLESPACE dashboard_ts
DATAFILE 'dashboard_ts.dbf' SIZE 100M
AUTOEXTEND ON NEXT 10M;

-- Create application user
CREATE USER dashboard_user IDENTIFIED BY dashboard_pass
DEFAULT TABLESPACE dashboard_ts
QUOTA UNLIMITED ON dashboard_ts;

-- Grant necessary privileges
GRANT CONNECT, RESOURCE TO dashboard_user;
GRANT CREATE SESSION TO dashboard_user;
GRANT CREATE TABLE TO dashboard_user;
GRANT CREATE SEQUENCE TO dashboard_user;
```

### 2. Create Database Schema

Connect as `dashboard_user` and run the schema.sql script:

```sql
-- Connect as dashboard_user
CONNECT dashboard_user/dashboard_pass

-- Run the schema.sql content
-- (The tables will be created automatically by Hibernate)
```

## Application Configuration

### 1. Update application.properties

Modify the database connection settings in `src/main/resources/application.properties`:

```properties
# Oracle Database Configuration
spring.datasource.url=jdbc:oracle:thin:@your-oracle-host:1521:your-service-name
spring.datasource.username=dashboard_user
spring.datasource.password=dashboard_pass
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.OracleDialect
spring.jpa.properties.hibernate.format_sql=true
```

### 2. Connection String Examples

**Local Oracle Express Edition (XE):**
```
jdbc:oracle:thin:@localhost:1521:XE
```

**Oracle Standard/Enterprise Edition:**
```
jdbc:oracle:thin:@your-server:1521:ORCL
```

**Oracle Cloud Database:**
```
jdbc:oracle:thin:@your-cloud-host:1521:your-service-name
```

## Default Users

The application automatically creates two default users on startup:

1. **Admin User:**
   - Username: `admin`
   - Password: `admin`
   - Role: `ADMIN`

2. **Regular User:**
   - Username: `user`
   - Password: `password`
   - Role: `USER`

## Security Features

- **Password Encryption:** All passwords are encrypted using BCrypt
- **Role-based Access:** Users can have a single role (USER or ADMIN)
- **Account Management:** Support for account locking, expiration, and enabled/disabled status
- **Session Management:** Spring Security handles session management

## Adding New Users

You can add new users programmatically or directly in the database:

### Programmatically (Recommended)

Create a service method to add users:

```java
@Autowired
private UserRepository userRepository;

@Autowired
private PasswordEncoder passwordEncoder;

public void createUser(String username, String password, String role) {
    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));
    user.setRole(role);
    user.setEnabled(true);
    userRepository.save(user);
}
```

### Direct Database Insert

```sql
-- Insert user (password must be BCrypt encoded)
INSERT INTO USERS (USERNAME, PASSWORD, ROLE, ENABLED) 
VALUES ('newuser', '$2a$10$encodedpassword', 'USER', 1);
```

## Troubleshooting

### Common Issues

1. **Connection Refused:**
   - Verify Oracle service is running
   - Check firewall settings
   - Verify connection string format

2. **Authentication Failed:**
   - Verify username/password
   - Check if user has CONNECT privilege
   - Verify tablespace quotas

3. **Table Not Found:**
   - Run schema.sql script
   - Check if Hibernate DDL is set to `create` or `update`
   - Verify user has CREATE TABLE privilege

4. **BCrypt Encoding Issues:**
   - Ensure PasswordEncoder bean is configured
   - Check if passwords are properly encoded before saving

### Logging

Enable SQL logging for debugging:

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

## Migration from In-Memory Authentication

If you're migrating from the previous in-memory authentication:

1. **Backup existing data** (if any)
2. **Set up Oracle database** following this guide
3. **Update application.properties** with Oracle connection details
4. **Restart application** - default users will be created automatically
5. **Test login** with admin/admin or user/password

## Performance Considerations

- **Connection Pooling:** Spring Boot automatically configures HikariCP
- **Indexes:** Database indexes are created automatically
- **Caching:** Consider enabling Hibernate second-level cache for production
- **Monitoring:** Use Oracle Enterprise Manager or similar tools for monitoring

## Security Best Practices

1. **Change default passwords** after first login
2. **Use strong passwords** for database users
3. **Limit database user privileges** to minimum required
4. **Enable SSL/TLS** for database connections in production
5. **Regular security updates** for Oracle database
6. **Audit logging** for user authentication events

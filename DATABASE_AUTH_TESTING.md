# Database Authentication Testing Guide

This guide will help you test the Oracle database authentication integration step by step.

## Prerequisites

1. **Oracle Database** running and accessible
2. **Database user** created with proper permissions
3. **Application** configured with correct database connection

## Step 1: Database Setup

### 1.1 Create Database User (Run as SYSDBA)

```sql
-- Connect to Oracle as SYSDBA
CONNECT / AS SYSDBA

-- Create tablespace (optional)
CREATE TABLESPACE dashboard_ts
DATAFILE 'dashboard_ts.dbf' SIZE 100M
AUTOEXTEND ON NEXT 10M;

-- Create application user
CREATE USER dashboard_user IDENTIFIED BY dashboard_pass
DEFAULT TABLESPACE dashboard_ts
QUOTA UNLIMITED ON dashboard_ts;

-- Grant privileges
GRANT CONNECT, RESOURCE TO dashboard_user;
GRANT CREATE SESSION TO dashboard_user;
GRANT CREATE TABLE TO dashboard_user;
GRANT CREATE SEQUENCE TO dashboard_user;
```

### 1.2 Create Tables (Run as dashboard_user)

```sql
-- Connect as dashboard_user
CONNECT dashboard_user/dashboard_pass

-- Create USERS table
CREATE TABLE USERS (
    USER_ID NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    USERNAME VARCHAR2(50) UNIQUE NOT NULL,
    PASSWORD VARCHAR2(255) NOT NULL,
    ROLE VARCHAR2(50) NOT NULL,
    ENABLED NUMBER(1) DEFAULT 1 NOT NULL,
    ACCOUNT_NON_EXPIRED NUMBER(1) DEFAULT 1 NOT NULL,
    ACCOUNT_NON_LOCKED NUMBER(1) DEFAULT 1 NOT NULL,
    CREDENTIALS_NON_EXPIRED NUMBER(1) DEFAULT 1 NOT NULL
);

-- Create indexes
CREATE INDEX idx_users_username ON USERS(USERNAME);
CREATE INDEX idx_users_role ON USERS(ROLE);
```

## Step 2: Application Configuration

### 2.1 Update application.properties

Make sure your `src/main/resources/application.properties` has the correct Oracle connection:

```properties
# Oracle Database Configuration
spring.datasource.url=jdbc:oracle:thin:@localhost:1521:XE
spring.datasource.username=dashboard_user
spring.datasource.password=dashboard_pass
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.OracleDialect
spring.jpa.properties.hibernate.format_sql=true
```

### 2.2 Connection String Examples

**Local Oracle XE:**
```
jdbc:oracle:thin:@localhost:1521:XE
```

**Oracle Standard/Enterprise:**
```
jdbc:oracle:thin:@your-server:1521:ORCL
```

**Oracle Cloud:**
```
jdbc:oracle:thin:@your-cloud-host:1521:your-service-name
```

## Step 3: Test Database Connection

### 3.1 Start the Application

```bash
mvn spring-boot:run
```

### 3.2 Check Database Connection

Visit: `http://localhost:8081/auth/status`

**Expected Response:**
```
Database connection successful! Total users: 0
```

**If Error:**
- Check Oracle service is running
- Verify connection string
- Check username/password
- Ensure database user has proper permissions

### 3.3 View Test Page

Visit: `http://localhost:8081/auth/test-login`

This page will show:
- Database connection status
- Current users in database
- Test login form
- User creation form

## Step 4: Test Authentication Flow

### 4.1 Automatic User Creation

When the application starts, it automatically creates two default users:

1. **Admin User:**
   - Username: `admin`
   - Password: `admin`
   - Role: `ADMIN`

2. **Regular User:**
   - Username: `user`
   - Password: `password`
   - Role: `USER`

### 4.2 Test Login

1. Go to: `http://localhost:8081/auth/test-login`
2. Try logging in with:
   - Username: `admin`, Password: `admin`
   - Username: `user`, Password: `password`

**Expected Behavior:**
- ✅ **Success**: Redirected to dashboard
- ❌ **Failure**: "Invalid username or password" error

### 4.3 Create New User

1. On the test page, use the "Create New User" form
2. Enter:
   - Username: `testuser`
   - Password: `testpass`
   - Role: `USER`
3. Click "Create"

**Expected Response:**
```
User created successfully: testuser
```

### 4.4 Test New User Login

1. Try logging in with the newly created user
2. Username: `testuser`, Password: `testpass`

## Step 5: Debugging Common Issues

### 5.1 Database Connection Issues

**Error: "Database connection failed"**

**Solutions:**
1. Check Oracle service is running:
   ```bash
   # Windows
   services.msc
   # Look for "OracleServiceXE" or similar
   
   # Linux
   systemctl status oracle-xe
   ```

2. Test connection manually:
   ```bash
   sqlplus dashboard_user/dashboard_pass@localhost:1521/XE
   ```

3. Check firewall settings

4. Verify connection string format

### 5.2 Authentication Issues

**Error: "Invalid username or password"**

**Solutions:**
1. Check if users exist in database:
   ```sql
   SELECT username, role FROM USERS;
   ```

2. Verify password encoding:
   ```sql
   SELECT username, password FROM USERS;
   ```
   Passwords should start with `$2a$10$` (BCrypt format)

3. Check user status:
   ```sql
   SELECT username, enabled, account_non_locked FROM USERS;
   ```

### 5.3 Table Creation Issues

**Error: "Table USERS not found"**

**Solutions:**
1. Check if tables exist:
   ```sql
   SELECT table_name FROM user_tables WHERE table_name = 'USERS';
   ```

2. Run schema creation manually:
   ```sql
   -- Run the CREATE TABLE statements from schema.sql
   ```

3. Check user permissions:
   ```sql
   SELECT privilege FROM user_sys_privs;
   ```

## Step 6: Production Considerations

### 6.1 Security

1. **Change default passwords** after first login
2. **Use strong passwords** for database users
3. **Enable SSL/TLS** for database connections
4. **Limit database user privileges**

### 6.2 Performance

1. **Connection pooling** (automatically configured by Spring Boot)
2. **Database indexes** (created automatically)
3. **Monitor query performance**

### 6.3 Monitoring

1. **Enable SQL logging** for debugging:
   ```properties
   spring.jpa.show-sql=true
   logging.level.org.hibernate.SQL=DEBUG
   ```

2. **Database monitoring** tools
3. **Application logs** for authentication events

## Step 7: API Endpoints

### 7.1 Available Endpoints

- `GET /auth/status` - Check database connection
- `GET /auth/users` - List all users
- `POST /auth/create-user` - Create new user
- `GET /auth/test-login` - Test login page
- `POST /login` - Spring Security login endpoint

### 7.2 Example API Calls

**Check Database Status:**
```bash
curl http://localhost:8081/auth/status
```

**List Users:**
```bash
curl http://localhost:8081/auth/users
```

**Create User:**
```bash
curl -X POST http://localhost:8081/auth/create-user \
  -d "username=newuser&password=newpass&role=USER"
```

## Troubleshooting Checklist

- [ ] Oracle database is running
- [ ] Database user exists with proper permissions
- [ ] Tables are created successfully
- [ ] Application.properties has correct connection string
- [ ] Application starts without errors
- [ ] Database connection test passes
- [ ] Default users are created on startup
- [ ] Login form works correctly
- [ ] New users can be created
- [ ] Authentication works for all users

## Next Steps

Once database authentication is working:

1. **Customize the login page** styling
2. **Add user management** features
3. **Implement role-based** access control
4. **Add password reset** functionality
5. **Set up audit logging** for authentication events

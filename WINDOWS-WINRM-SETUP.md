# Windows VM WinRM Configuration Guide

This guide will help you configure WinRM on your Windows VM (34.100.134.103) to enable Ansible connections.

## Prerequisites

- Windows VM running Windows Server 2019/2022 or Windows 10/11
- Administrator access to the Windows VM
- PowerShell execution policy set to allow script execution

## Quick Setup

### Step 1: Upload Scripts to Windows VM

1. Copy the `windows-winrm-setup.ps1` and `test-winrm.ps1` scripts to your Windows VM
2. Place them in a directory (e.g., `C:\temp\`)

### Step 2: Run the Setup Script

1. **Open PowerShell as Administrator** on the Windows VM
2. **Navigate to the script directory**:
   ```powershell
   cd C:\temp\
   ```

3. **Set execution policy** (if needed):
   ```powershell
   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
   ```

4. **Run the setup script**:
   ```powershell
   .\windows-winrm-setup.ps1
   ```

### Step 3: Verify Configuration

1. **Run the test script**:
   ```powershell
   .\test-winrm.ps1
   ```

2. **Check for any errors** and resolve them if needed

## Manual Configuration (Alternative)

If you prefer to run commands manually, here are the key steps:

### 1. Enable WinRM Service
```powershell
Enable-PSRemoting -Force -SkipNetworkProfileCheck
```

### 2. Configure HTTPS Listener
```powershell
# Create self-signed certificate
$cert = New-SelfSignedCertificate -DnsName $env:COMPUTERNAME -CertStoreLocation "Cert:\LocalMachine\My"

# Configure HTTPS listener
winrm create winrm/config/Listener?Address=*+Transport=HTTPS "@{Hostname=`"$($env:COMPUTERNAME)`";CertificateThumbprint=`"$($cert.Thumbprint)`"}"
```

### 3. Configure Authentication
```powershell
# Enable NTLM authentication
Set-Item -Path WSMan:\localhost\Service\Auth\Negotiate -Value $true
Set-Item -Path WSMan:\localhost\Service\Auth\Kerberos -Value $true
Set-Item -Path WSMan:\localhost\Service\Auth\CredSSP -Value $true

# Allow basic authentication (for testing)
Set-Item -Path WSMan:\localhost\Service\Auth\Basic -Value $true
Set-Item -Path WSMan:\localhost\Service\AllowUnencrypted -Value $true
```

### 4. Configure Firewall
```powershell
# Allow WinRM HTTPS (Port 5986)
New-NetFirewallRule -DisplayName "Windows Remote Management (HTTPS-In)" -Name "Windows Remote Management (HTTPS-In)" -Profile Any -Direction Inbound -Action Allow -Protocol TCP -LocalPort 5986
```

### 5. Add User to Remote Management Group
```powershell
# Add user to Remote Management Users group
Add-LocalGroupMember -Group "Remote Management Users" -Member "aditya"
```

### 6. Restart WinRM Service
```powershell
Restart-Service WinRM
```

## Testing the Configuration

### From Windows VM (Local Test)
```powershell
# Test local WinRM connection
New-PSSession -ComputerName localhost

# Check WinRM configuration
winrm get winrm/config/service
winrm get winrm/config/client
winrm enumerate winrm/config/listener
```

### From Ansible Host (Remote Test)
```bash
# Test with Ansible
ansible -i inventory.ini servers -m win_ping -vvvv
```

## Troubleshooting

### Common Issues

1. **"Access Denied" Error**
   - Ensure you're running PowerShell as Administrator
   - Check if the user is in the "Remote Management Users" group

2. **"Connection Refused" Error**
   - Verify WinRM service is running: `Get-Service WinRM`
   - Check firewall rules: `Get-NetFirewallRule -DisplayName "*Remote Management*"`

3. **"Certificate Error"**
   - The script creates a self-signed certificate
   - For production, consider using a proper CA-signed certificate

4. **"Authentication Failed"**
   - Verify username and password are correct
   - Check if NTLM authentication is enabled
   - Ensure user has proper permissions

### Verification Commands

```powershell
# Check service status
Get-Service WinRM

# Check listeners
winrm enumerate winrm/config/listener

# Check authentication settings
winrm get winrm/config/service

# Check firewall rules
Get-NetFirewallRule -DisplayName "*Remote Management*"

# Test local connection
Test-WSMan -ComputerName localhost
```

## Security Considerations

1. **For Production Use**:
   - Use proper CA-signed certificates instead of self-signed
   - Disable basic authentication
   - Enable encryption
   - Restrict firewall rules to specific IP ranges

2. **Network Security**:
   - Consider using VPN or private networks
   - Implement proper access controls
   - Monitor WinRM access logs

## Next Steps

After successful WinRM configuration:

1. **Test from your Ansible host**:
   ```bash
   ansible -i inventory.ini servers -m win_ping
   ```

2. **Test service management**:
   ```bash
   ansible -i inventory.ini servers -m win_service -a "name=spooler state=started"
   ```

3. **Test command execution**:
   ```bash
   ansible -i inventory.ini servers -m win_shell -a "Get-Service"
   ```

## Support

If you encounter issues:
1. Check the Windows Event Logs for WinRM-related errors
2. Verify all prerequisites are met
3. Ensure network connectivity between Ansible host and Windows VM
4. Check that the user account has proper permissions 
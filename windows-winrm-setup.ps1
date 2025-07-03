# WinRM Configuration Script for Ansible
# Run this script on the Windows VM (34.100.134.103) as Administrator

Write-Host "=== WinRM Configuration for Ansible ===" -ForegroundColor Green

# 1. Enable WinRM Service
Write-Host "1. Enabling WinRM Service..." -ForegroundColor Yellow
Enable-PSRemoting -Force -SkipNetworkProfileCheck

# 2. Configure WinRM Service
Write-Host "2. Configuring WinRM Service..." -ForegroundColor Yellow
Set-Item -Path WSMan:\localhost\Service\Auth\Basic -Value $true
Set-Item -Path WSMan:\localhost\Service\AllowUnencrypted -Value $true
Set-Item -Path WSMan:\localhost\Client\Auth\Basic -Value $true
Set-Item -Path WSMan:\localhost\Client\AllowUnencrypted -Value $true

# 3. Configure WinRM Listener for HTTPS (Port 5986)
Write-Host "3. Configuring HTTPS Listener on Port 5986..." -ForegroundColor Yellow

# Create self-signed certificate for HTTPS
$cert = New-SelfSignedCertificate -DnsName $env:COMPUTERNAME -CertStoreLocation "Cert:\LocalMachine\My"

# Configure HTTPS listener
winrm create winrm/config/Listener?Address=*+Transport=HTTPS "@{Hostname=`"$($env:COMPUTERNAME)`";CertificateThumbprint=`"$($cert.Thumbprint)`"}"

# 4. Configure WinRM for NTLM Authentication
Write-Host "4. Configuring NTLM Authentication..." -ForegroundColor Yellow
Set-Item -Path WSMan:\localhost\Service\Auth\Negotiate -Value $true
Set-Item -Path WSMan:\localhost\Service\Auth\Kerberos -Value $true
Set-Item -Path WSMan:\localhost\Service\Auth\CredSSP -Value $true

# 5. Configure Firewall Rules
Write-Host "5. Configuring Firewall Rules..." -ForegroundColor Yellow

# Allow WinRM HTTPS (Port 5986)
New-NetFirewallRule -DisplayName "Windows Remote Management (HTTPS-In)" -Name "Windows Remote Management (HTTPS-In)" -Profile Any -Direction Inbound -Action Allow -Protocol TCP -LocalPort 5986

# Allow WinRM HTTP (Port 5985) - Optional
New-NetFirewallRule -DisplayName "Windows Remote Management (HTTP-In)" -Name "Windows Remote Management (HTTP-In)" -Profile Any -Direction Inbound -Action Allow -Protocol TCP -LocalPort 5985

# 6. Configure User Permissions
Write-Host "6. Configuring User Permissions..." -ForegroundColor Yellow

# Add user to Remote Management Users group
$username = "aditya"
$group = "Remote Management Users"
try {
    Add-LocalGroupMember -Group $group -Member $username -ErrorAction Stop
    Write-Host "Successfully added user '$username' to '$group' group" -ForegroundColor Green
} catch {
    Write-Host "User '$username' is already a member of '$group' group or user doesn't exist" -ForegroundColor Yellow
}

# 7. Configure WinRM for Ansible
Write-Host "7. Configuring WinRM for Ansible..." -ForegroundColor Yellow

# Set maximum memory per shell
Set-Item -Path WSMan:\localhost\Shell\MaxMemoryPerShellMB -Value 2048

# Set maximum operations per shell
Set-Item -Path WSMan:\localhost\Shell\MaxOperationsPerShell -Value 10000

# Set maximum concurrent operations per user
Set-Item -Path WSMan:\localhost\Shell\MaxConcurrentOperationsPerUser -Value 100

# Set timeout settings
Set-Item -Path WSMan:\localhost\Shell\IdleTimeout -Value 7200000
Set-Item -Path WSMan:\localhost\Service\MaxTimeoutms -Value 7200000

# 8. Restart WinRM Service
Write-Host "8. Restarting WinRM Service..." -ForegroundColor Yellow
Restart-Service WinRM

# 9. Display Configuration
Write-Host "9. Current WinRM Configuration:" -ForegroundColor Green
Write-Host "Service Status:" -ForegroundColor Cyan
Get-Service WinRM

Write-Host "`nListeners:" -ForegroundColor Cyan
winrm enumerate winrm/config/listener

Write-Host "`nService Configuration:" -ForegroundColor Cyan
winrm get winrm/config/service

Write-Host "`nClient Configuration:" -ForegroundColor Cyan
winrm get winrm/config/client

Write-Host "`n=== WinRM Configuration Complete ===" -ForegroundColor Green
Write-Host "The Windows VM is now ready for Ansible connections!" -ForegroundColor Green
Write-Host "Test connection from your Ansible host using:" -ForegroundColor Cyan
Write-Host "ansible -i inventory.ini servers -m win_ping" -ForegroundColor White 
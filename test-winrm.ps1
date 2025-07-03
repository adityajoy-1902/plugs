# Test WinRM Configuration
# Run this script on the Windows VM to verify WinRM is working

Write-Host "=== Testing WinRM Configuration ===" -ForegroundColor Green

# Test 1: Check WinRM Service Status
Write-Host "1. Checking WinRM Service Status..." -ForegroundColor Yellow
$service = Get-Service WinRM
Write-Host "Service Name: $($service.Name)" -ForegroundColor Cyan
Write-Host "Status: $($service.Status)" -ForegroundColor Cyan
Write-Host "Startup Type: $($service.StartType)" -ForegroundColor Cyan

# Test 2: Check WinRM Listeners
Write-Host "`n2. Checking WinRM Listeners..." -ForegroundColor Yellow
try {
    $listeners = winrm enumerate winrm/config/listener
    Write-Host "Listeners found:" -ForegroundColor Green
    $listeners
} catch {
    Write-Host "Error getting listeners: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Check Firewall Rules
Write-Host "`n3. Checking Firewall Rules..." -ForegroundColor Yellow
$rules = Get-NetFirewallRule -DisplayName "*Remote Management*" | Where-Object {$_.Enabled -eq $true}
if ($rules) {
    Write-Host "Firewall rules found:" -ForegroundColor Green
    $rules | Format-Table DisplayName, Enabled, Direction, Action, Protocol, LocalPort
} else {
    Write-Host "No Remote Management firewall rules found!" -ForegroundColor Red
}

# Test 4: Test Local WinRM Connection
Write-Host "`n4. Testing Local WinRM Connection..." -ForegroundColor Yellow
try {
    $session = New-PSSession -ComputerName localhost -ErrorAction Stop
    Write-Host "Local WinRM connection successful!" -ForegroundColor Green
    Remove-PSSession $session
} catch {
    Write-Host "Local WinRM connection failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: Check User Permissions
Write-Host "`n5. Checking User Permissions..." -ForegroundColor Yellow
$username = "aditya"
$group = "Remote Management Users"
$members = Get-LocalGroupMember -Group $group -ErrorAction SilentlyContinue
if ($members -and ($members.Name -like "*$username*" -or $members.Name -like "*$env:COMPUTERNAME\\$username*")) {
    Write-Host "User '$username' is a member of '$group' group" -ForegroundColor Green
} else {
    Write-Host "User '$username' is NOT a member of '$group' group" -ForegroundColor Red
}

# Test 6: Check WinRM Configuration
Write-Host "`n6. Checking WinRM Configuration..." -ForegroundColor Yellow
Write-Host "Service Configuration:" -ForegroundColor Cyan
winrm get winrm/config/service

Write-Host "`nClient Configuration:" -ForegroundColor Cyan
winrm get winrm/config/client

Write-Host "`n=== WinRM Test Complete ===" -ForegroundColor Green 
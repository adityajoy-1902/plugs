# Install IIS
Install-WindowsFeature -Name Web-Server -IncludeManagementTools

# Install Java
$javaUrl = "https://download.java.net/java/GA/jdk11.0.2/9f5f8c5b2c4b4b4b4b4b4b4b4b4b4b/9/GPL/openjdk-11.0.2_windows-x64_bin.zip"
$javaPath = "C:\Java"
New-Item -ItemType Directory -Path $javaPath -Force
Invoke-WebRequest -Uri $javaUrl -OutFile "C:\temp\java.zip"
Expand-Archive -Path "C:\temp\java.zip" -DestinationPath $javaPath

# Set JAVA_HOME
[Environment]::SetEnvironmentVariable("JAVA_HOME", "$javaPath\jdk-11.0.2", "Machine")
$env:JAVA_HOME = "$javaPath\jdk-11.0.2"

# Install Tomcat
$tomcatUrl = "https://downloads.apache.org/tomcat/tomcat-9/v9.0.85/bin/apache-tomcat-9.0.85-windows-x64.zip"
$tomcatPath = "C:\Tomcat"
New-Item -ItemType Directory -Path $tomcatPath -Force
Invoke-WebRequest -Uri $tomcatUrl -OutFile "C:\temp\tomcat.zip"
Expand-Archive -Path "C:\temp\tomcat.zip" -DestinationPath $tomcatPath

# Create Tomcat Windows Service
& "$tomcatPath\apache-tomcat-9.0.85\bin\service.bat" install Tomcat9
Start-Service Tomcat9

# Configure IIS
Import-Module WebAdministration
New-WebSite -Name "DefaultWebSite" -Port 80 -PhysicalPath "C:\inetpub\wwwroot"

# Configure Windows Firewall
New-NetFirewallRule -DisplayName "Allow Tomcat" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
New-NetFirewallRule -DisplayName "Allow IIS" -Direction Inbound -Protocol TCP -LocalPort 80 -Action Allow 
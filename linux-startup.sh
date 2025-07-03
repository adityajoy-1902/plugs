#!/bin/bash
# Update system
apt-get update
apt-get install -y openjdk-11-jdk wget curl unzip

# Create application user
useradd -m -s /bin/bash appuser
echo "appuser:password123" | chpasswd

# Install Tomcat
wget https://downloads.apache.org/tomcat/tomcat-9/v9.0.85/bin/apache-tomcat-9.0.85.tar.gz
tar -xzf apache-tomcat-9.0.85.tar.gz -C /opt/
ln -s /opt/apache-tomcat-9.0.85 /opt/tomcat
chown -R appuser:appuser /opt/tomcat

# Install JBoss/WildFly
wget https://download.jboss.org/wildfly/26.1.3.Final/wildfly-26.1.3.Final.tar.gz
tar -xzf wildfly-26.1.3.Final.tar.gz -C /opt/
ln -s /opt/wildfly-26.1.3.Final /opt/jboss
chown -R appuser:appuser /opt/jboss

# Create systemd services
cat > /etc/systemd/system/tomcat.service << 'EOF'
[Unit]
Description=Apache Tomcat
After=network.target

[Service]
Type=forking
User=appuser
Group=appuser
Environment=JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
Environment=CATALINA_HOME=/opt/tomcat
ExecStart=/opt/tomcat/bin/startup.sh
ExecStop=/opt/tomcat/bin/shutdown.sh
Restart=always

[Install]
WantedBy=multi-user.target
EOF

cat > /etc/systemd/system/jboss.service << 'EOF'
[Unit]
Description=JBoss Application Server
After=network.target

[Service]
Type=simple
User=appuser
Group=appuser
Environment=JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
Environment=JBOSS_HOME=/opt/jboss
ExecStart=/opt/jboss/bin/standalone.sh
Restart=always

[Install]
WantedBy=multi-user.target
EOF

# Enable and start services
systemctl daemon-reload
systemctl enable tomcat
systemctl enable jboss
systemctl start tomcat
systemctl start jboss

# Configure firewall
ufw allow 22
ufw allow 8080
ufw allow 8081
ufw allow 9990
ufw --force enable 
#!/bin/bash -e
# Redirect port 80 to 8080
iptables -A INPUT -i eth0 -p tcp --dport 80 -j ACCEPT
iptables -A INPUT -i eth0 -p tcp --dport 8080 -j ACCEPT
iptables -A PREROUTING -t nat -i eth0 -p tcp --dport 80 -j REDIRECT --to-port 8080
# Download application package
PKG=demo-multiverse-1.0.0-SNAPSHOT-runner.jar
URL=https://github.com/CaravanaCloud/aws-pod/releases/download/v20220803234451/$PKG
wget $URL
# Install Java
sudo yum -y install java-11-amazon-corretto
# Start application
java -jar $PKG
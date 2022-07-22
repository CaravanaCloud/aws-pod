#!/bin/bash
yum -y install httpd
echo "Hello from $(curl -s http://instance-data/latest/meta-data/instance-id)" >> /var/www/html/index.html
systemctl enable httpd.service
systemctl start httpd.service

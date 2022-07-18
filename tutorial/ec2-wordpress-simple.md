# Wordpress on EC2 - Single Instance

## Prepare Web Server

Assume root user:
```
sudo -s
```

Install required packages:
```
yum -y update all
amazon-linux-extras enable php8.0
yum -y clean metadata
yum -y install httpd php php-gd php-mysqlnd mariadb mariadb-server
```

Edit httpd configuration:
```
sudo vim /etc/httpd/conf/httpd.conf
```
So that served directory allow .htaccess overides:
```
/var/www/html -> AllowOverride All
```

Restart httpd
```
service httpd start
```

## Setup Database
Start mariadb and change password:
```
systemctl start mariadb
mysqladmin -uroot -p password 'Masterkey123' 
```

Connect to the database:
```
mysql -uroot pMasterkey123 -h127.0.0.1
```

Create application user:
```
CREATE USER 'wordpress-user'@'localhost' IDENTIFIED BY 'Wordkey123';
CREATE DATABASE `wordpress-db`;
GRANT ALL PRIVILEGES ON `wordpress-db`.* TO "wordpress-user"@"localhost";
FLUSH PRIVILEGES;
exit;
```

Check application user:
```
mysql -uwordpress-user -pWordkey123 -h127.0.0.1 wordpress-db
```

## Setup Wordpress
Donwload and uncompress:
```
wget https://wordpress.org/latest.tar.gz
tar -xzf latest.tar.gz
```
Edit Wordpress configuration:
```
cp wordpress/wp-config-sample.php wordpress/wp-config.php
vi wordpress/wp-config.php
```

Change DB_NAME, DB_USER, DB_PASWORD

Copy wordpress to served directory and tighten permission
```
cp -r wordpress/* /var/www/html/
sudo chown -R apache /var/www
sudo chgrp -R apache /var/www
sudo chmod 2775 /var/www
find /var/www -type d -exec sudo chmod 2775 {} \;
find /var/www -type f -exec sudo chmod 0644 {} \;
```

# Finish Setup

Check internal and externall access:
```
curl localhost
curl http://instance-data/latest/meta-data/public-ipv4
curl $(curl http://instance-data/latest/meta-data/public-ipv4)
```

# Success!


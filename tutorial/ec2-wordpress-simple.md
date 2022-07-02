# Wordpress on EC2

## Prepare Web Server

```
sudo -s
```

```
yum -y update all
amazon-linux-extras enable php8.0
yum clean metadata
yum install httpd php php-gd php-mysqlnd mariadb mariadb-server
```

```
sudo vim /etc/httpd/conf/httpd.conf
```

/var/www/html -> AllowOverride All

```
service httpd start
```

## Setup Database
```
systemctl start mariadb
mysqladmin -uroot -p password 'Masterkey123' 
mysql -uroot -p
```

```
CREATE USER 'wordpress-user'@'localhost' IDENTIFIED BY 'Wordkey123';
CREATE DATABASE `wordpress-db`;
GRANT ALL PRIVILEGES ON `wordpress-db`.* TO "wordpress-user"@"localhost";
FLUSH PRIVILEGES;
exit;
```

```
mysql -uwordpress-user -pWordkey123 -h127.0.0.1 wordpress-db
```

## Download Wordpress
```
wget https://wordpress.org/latest.tar.gz
tar -xzf latest.tar.gz
```

## Setup Wordpress
```
cp wordpress/wp-config-sample.php wordpress/wp-config.php
vi wordpress/wp-config.php
```

Change DB_NAME, DB_USER, DB_PASWORD

```
cp -r wordpress/* /var/www/html/
sudo chown -R apache /var/www
sudo chgrp -R apache /var/www
sudo chmod 2775 /var/www
find /var/www -type d -exec sudo chmod 2775 {} \;
find /var/www -type f -exec sudo chmod 0644 {} \;
```

# Finish Setup

```
curl localhost
curl http://instance-data/latest/meta-data/public-ipv4
curl $(curl http://instance-data/latest/meta-data/public-ipv4)
```

# Success!


### Setup Relational Database
1. Setup RDS Database Instance
    Define variables
    ```
    RDS_NETGRP=netgrp-$UNIQ
    RDS_NAME=rds-$UNIQ
    RDS_ROOT_USER=root
    RDS_ROOT_PASSWORD=Masterkey321
    RDS_APP_USER=multiverseuser
    RDS_APP_PASSWORD=Userkey321
    RDS_PORT=3306
    RDS_CIDR=0.0.0.0/0
    RDS_DB=multiversedb
    RDS_INSTANCE_TYPE=db.t3.micro
    ```

    Create RDS networking elements:
    ```
    RDS_SECG=$(aws ec2 create-security-group \
        --group-name telemo-rds-secgrp \
        --description "telemo-rds-secg" \
        --vpc-id $VPC_ID \
        --query "GroupId" \
        --output text)

    echo RDS_SECG=$RDS_SECG

    RDS_SECG_ID=$(aws ec2 describe-security-groups \
        --filter Name=vpc-id,Values=$VPC_ID Name=group-name,Values=telemo-rds-secgrp \
        --query 'SecurityGroups[*].[GroupId]' \
        --output text)
            
    echo RDS_SECG_ID=$RDS_SECG_ID
        
    aws ec2 authorize-security-group-ingress \
        --group-id $RDS_SECG \
        --protocol tcp \
        --port $RDS_PORT \
        --cidr $RDS_CIDR

    aws rds create-db-subnet-group \
        --db-subnet-group-name $RDS_NETGRP \
        --db-subnet-group-description "Telemo RDS Subnet Group" \
        --subnet-ids $NET_A $NET_B        
    ```

    Create RDS Database Instances
    ```
    RDS_ID=$(aws rds create-db-instance \
        --db-name $RDS_DB  \
        --db-instance-identifier $RDS_NAME \
        --allocated-storage 20 \
        --db-instance-class $RDS_INSTANCE_TYPE \
        --engine mysql \
        --engine-version 8.0 \
        --master-username $RDS_ROOT_USER \
        --master-user-password $RDS_ROOT_PASSWORD \
        --db-subnet-group-name  $RDS_NETGRP \
        --backup-retention-period 0 \
        --publicly-accessible \
        --vpc-security-group-ids $RDS_SECG_ID \
        --query "DBInstance.DBInstanceIdentifier" \
        --output text)

    echo RDS_ID=$RDS_ID
    ```
    Wait for database instance to be avaialbe
    ```
    aws rds wait db-instance-available --db-instance-identifier $RDS_ID && echo "Up and running!"
    ```
    
    Fetch instance address and properties
    ```
    RDS_ENDPOINT=$(aws rds describe-db-instances  \
        --db-instance-identifier $RDS_ID  \
        --query "DBInstances[0].Endpoint.Address"  \
        --output text)
        
    RDS_PORT=$(aws rds describe-db-instances  \
        --db-instance-identifier $RDS_ID  \
        --query "DBInstances[0].Endpoint.Port"\
        --output text)

    RDS_JDBC="jdbc:mysql://$RDS_ENDPOINT:$RDS_PORT/$RDS_DB?useSSL=false"
        
    echo RDS_JDBC=$RDS_JDBC
    ```

    Generate SQL query to create application user in database   
    ```
    SQL="CREATE USER '$RDS_APP_USER' IDENTIFIED BY '$RDS_APP_PASSWORD';"
    SQL="${SQL}GRANT ALL PRIVILEGES ON \`$RDS_DB\`.* TO '$RDS_APP_USER';"
    echo $SQL
    ```
    Connect to database and execute query
    ```
    mysql -u$RDS_ROOT_USER -p$RDS_ROOT_PASSWORD -h$RDS_ENDPOINT -P $RDS_PORT  -e "$SQL" $RDS_DB 
    ```
    Check application user database access
    ```
    mysql -u$RDS_APP_USER -p$RDS_APP_PASSWORD -h$RDS_ENDPOINT -P $RDS_PORT  -e "SELECT DATABASE();" $RDS_DB 
    ```
    Create your application environment configuration, to be used in the instances:
    ```
    echo -e "QUARKUS_DATASOURCE_DB_KIND=mysql\n"\
"QUARKUS_DATASOURCE_USERNAME=$RDS_APP_USER\n"\
"QUARKUS_DATASOURCE_PASSWORD=$RDS_APP_PASSWORD\n"\
"QUARKUS_DATASOURCE_JDBC_URL=$RDS_JDBC" | tee .env-remote
    cat .env-remote
    ```


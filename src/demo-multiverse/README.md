# code-with-quarkus Project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/code-with-quarkus-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Deploying on AWS using the AWS CLI

### (Optional) Tools Preferences
1. Start tmux session
    ```
    tmux new -s multiverse
    ```
1. AWS Cli Preferences
    ```
    export AWS_PAGER=""
    AWS_REGION=$(aws configure get region)
    echo $AWS_REGION
    ```

### Setup VPC networking

1. Set a unique identifier for resources
    ```
    UNIQ="multiversex"
    ```
1. Check AWS Authentication
    ```
    aws sts get-caller-identity
    ```
1. Setup VPC
Create VPC
    ```
    VPC_ID=$(aws ec2 create-vpc \
        --cidr-block 10.0.0.0/16 \
        --query "Vpc.VpcId" \
        --output text)

    echo $VPC_ID
    
    aws ec2 create-tags --resources $VPC_ID \
        --tags Key=Name,Value="vpc-$UNIQ"

    aws ec2 modify-vpc-attribute \
    --enable-dns-hostnames \
    --vpc-id $VPC_ID
    
    aws ec2 modify-vpc-attribute \
    --enable-dns-support \
    --vpc-id $VPC_ID
    ```
1. Setup Internet Gateway
    ```
    IGW_ID=$(aws ec2 create-internet-gateway \
        --query "InternetGateway.InternetGatewayId" \
        --output text)
        
    echo $IGW_ID

    aws ec2 attach-internet-gateway --vpc-id $VPC_ID --internet-gateway-id $IGW_ID
    ```
1. Setup public route table
    ```
    RTB_ID=$(aws ec2 create-route-table \
        --vpc-id $VPC_ID \
        --query "RouteTable.RouteTableId" \
        --output text)

    echo RTB_ID=$RTB_ID

    aws ec2 create-route \
        --route-table-id $RTB_ID \
        --destination-cidr-block 0.0.0.0/0 \
        --gateway-id $IGW_ID
    ```
1. Setup public subnet A
    ```
    AZ1=$(aws ec2 describe-availability-zones \
        --query "AvailabilityZones[0].ZoneName" \
        --output text)
    echo $AZ1

    NET_A=$(aws ec2 create-subnet \
        --vpc-id $VPC_ID \
        --cidr-block 10.0.200.0/24 \
        --availability-zone "$AZ1" \
        --query "Subnet.SubnetId" \
        --output text)
        
    echo NET_A=$NET_A

    aws ec2 associate-route-table \
        --subnet-id $NET_A \
        --route-table-id $RTB_ID
        
    aws ec2 modify-subnet-attribute  \
        --subnet-id $NET_A  \
        --map-public-ip-on-launch
    ```
1. Setup public subnet B
    ```
    AZ2=$(aws ec2 describe-availability-zones \
        --query "AvailabilityZones[1].ZoneName" \
        --output text)

    echo $AZ2

    NET_B=$(aws ec2 create-subnet \
        --vpc-id $VPC_ID \
        --cidr-block 10.0.201.0/24 \
        --availability-zone "$AZ2" \
        --query "Subnet.SubnetId" \
        --output text)
        
    echo NET_B=$NET_B

    aws ec2 associate-route-table \
        --subnet-id $NET_B \
        --route-table-id $RTB_ID
        
    aws ec2 modify-subnet-attribute  \
        --subnet-id $NET_B  \
        --map-public-ip-on-launch
    ```
### Setup Relational Database
1. Setup RDS Database Instance
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

    ```
    aws rds wait db-instance-available --db-instance-identifier $RDS_ID && echo done
    ```

    ```
    RDS_ENDPOINT=$(aws rds describe-db-instances  \
    --db-instance-identifier $RDS_ID  \
    --query "DBInstances[0].Endpoint.Address"  \
    --output text)
    
    RDS_PORT=$(aws rds describe-db-instances  \
    --db-instance-identifier $RDS_ID  \
    --query "DBInstances[0].Endpoint.Port"\
    --output text)

    RDS_JDBC=jdbc:mysql://$RDS_ENDPOINT:$RDS_PORT/$RDS_DB
    
    echo RDS_JDBC=$RDS_JDBC
    
    # Generate SQL query to create application user in database
    echo -e "\n\
          CREATE USER '$RDS_APP_USER'@'%' IDENTIFIED BY '$RDS_APP_PASSWORD';\n\
          GRANT ALL PRIVILEGES ON \`$RDS_DB\`.* TO '$RDS_APP_USER'@'%';\n\
          FLUSH PRIVILEGES;\n\
          exit;"
    
    mysql -u$RDS_ROOT_USER -p$RDS_ROOT_PASSWORD -h$RDS_ENDPOINT -P $RDS_PORT $RDS_DB
    ```

### Deployment Using Amazon EC2
1. Fetch the latest linux AMI
    ```
    AMI_ID=$(aws ssm get-parameters \
        --names /aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-x86_64-gp2 \
        --query "Parameters[0].Value" \
        --output text)
    ```
1. Create app instance security group:
    ```
    APP_PORT=8080
    APP_CIDR=0.0.0.0/0

    APP_SECG=$(aws ec2 create-security-group \
        --group-name "app-secgrp-$UNIQ" \
        --description "app-secgrp-$UNIQ" \
        --vpc-id $VPC_ID \
        --query "GroupId" \
        --output text)

    echo APP_SECG=$APP_SECG

    APP_SECG_ID=$(aws ec2 describe-security-groups \
        --filter Name=vpc-id,Values=$VPC_ID Name=group-name,Values=telemo-rds-secgrp \
        --query 'SecurityGroups[*].[GroupId]' \
        --output text)
            
    echo APP_SECG_ID=$APP_SECG_ID
            
    aws ec2 authorize-security-group-ingress \
        --group-id $APP_SECG \
        --protocol tcp \
        --port $APP_PORT \
        --cidr $APP_CIDR
    ```
1. Create instance profile
    ```
    APP_ROLE="role-$UNIQ"
    APP_POLICY="policy-$UNIQ"
    APP_PROFILE="profile-$UNIQ"

    aws iam create-role --role-name "$APP_ROLE" \
        --assume-role-policy-document "file://./scripts/instance-trust.json"

    aws iam attach-role-policy --role-name "$APP_ROLE" \
        --policy-arn "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"  

    aws iam attach-role-policy --role-name "$APP_ROLE" \
        --policy-arn "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"  
        
    aws iam attach-role-policy --role-name "$APP_ROLE" \
        --policy-arn "arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess"  

    aws iam create-instance-profile \
        --instance-profile-name "$APP_PROFILE"

    aws iam add-role-to-instance-profile \
        --instance-profile-name "$APP_PROFILE" \
        --role-name "$APP_ROLE"
    ```
1. Create a new instance
    ```
    APP_INSTANCE_TYPE=t3.micro
    APP_SUBNET="$NET_A"

    INSTANCE_ID=$(aws ec2 run-instances \
    --image-id "$AMI_ID" \
    --instance-type "$APP_INSTANCE_TYPE" \
    --subnet-id "$APP_SUBNET" \
    --security-group-ids "$APP_SECG_ID" \
    --iam-instance-profile "Name=$APP_PROFILE" \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value="app-$UNIQ"}]" \
    --query "Instances[0].InstanceId" \
    --output "text")

    echo INSTANCE_ID=$INSTANCE_ID
    ```
1. Access the instance using SSM
    ```
    echo  "https://$AWS_REGION.console.aws.amazon.com/systems-manager/session-manager/$INSTANCE_ID?region=$AWS_REGION"
    ```

1. Install Java
    ```
    curl -s "https://get.sdkman.io" | bash
    source "/home/ssm-user/.sdkman/bin/sdkman-init.sh"
    sdk install java 22.1.0.r17-grl
    ```

1. Install the application demo-multiverse
    ```
    
    ```
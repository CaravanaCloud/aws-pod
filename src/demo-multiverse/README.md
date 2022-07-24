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
1. AWS Cli Preferences
    ```
    export AWS_PAGER=""
    AWS_REGION=$(aws configure get region)
    echo $AWS_REGION
    ```

### Setup VPC networking

1. Set a unique identifier for resources
    ```
    UNIQ="multiverse$RANDOM"
    echo $UNIQ
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

### Deployment Using Amazon EC2
1. Fetch the latest linux AMI
    ```
    AMI_ID=$(aws ssm get-parameters \
        --names /aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-x86_64-gp2 \
        --query "Parameters[0].Value" \
        --output text)
    echo $AMI_ID 
    ```
1. Create app instance security group:
    ```
    APP_PORT=8080
    APP_CIDR=0.0.0.0/0

    APP_SECG=$(aws ec2 create-security-group \
        --group-name "secg-app-$UNIQ" \
        --description "secg-app-$UNIQ" \
        --vpc-id $VPC_ID \
        --query "GroupId" \
        --output text)

    echo APP_SECG=$APP_SECG

    APP_SECG_ID=$(aws ec2 describe-security-groups \
        --filter Name=vpc-id,Values=$VPC_ID Name=group-name,Values=secg-app-$UNIQ \
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
    Wait for instance to be up
    ```
    aws ec2 wait instance-status-ok --instance-ids $INSTANCE_ID  && echo "Instance up and running!" 
    ```
1. Access the instance using SSM
    ```
    echo  "https://$AWS_REGION.console.aws.amazon.com/systems-manager/session-manager/$INSTANCE_ID?region=$AWS_REGION"
    ```
1. Install Java
    ```
    sudo su - ec2-user

    JAVA_VERSION=22.1.0.r17-grl
    curl -s "https://get.sdkman.io" | bash
    source "/home/ec2-user/.sdkman/bin/sdkman-init.sh"
    sdk install java $JAVA_VERSION
    ```
1. Download Application
    ```
    APP_JAR_URL=https://github.com/CaravanaCloud/aws-pod/releases/download/v1.0.20220723105446/demo-multiverse-1.0.0-SNAPSHOT-runner.jar
    APP_DIR=/home/ec2-user/demo-multiverse
    APP_JAR_FILE=$APP_DIR/demo-multiverse-1.0.0-SNAPSHOT-runner.jar
    APP_ENV_FILE=$APP_DIR/.env

    mkdir -p $APP_DIR
    curl -Ls $APP_JAR_URL --output $APP_JAR_FILE
    ls -liah $APP_JAR_FILE
    ```
    
    Edit environment configuration
    ```
    vi $APP_ENV_FILE
    cat $APP_ENV_FILE
    ```

    Test app
    ```
    cd $APP_DIR
    java -jar $APP_JAR_FILE
    ```

    Use this to start app as ec2-user
    ```
    sudo -u ec2-user bash -c 'pushd /home/ec2-user/demo-multiverse/ && /home/ec2-user/.sdkman/candidates/java/current/bin/java -jar /home/ec2-user/demo-multiverse/demo-multiverse-1.0.0-SNAPSHOT-runner.jar && popd'
    ```

    Add it to system initialization
    ```
    sudo -s
    vi /etc/rc.d/rc.local
    chmod +x /etc/rc.d/rc.local
    ```

    Reboot check :)
    ```
    sudo reboot
    ```

    check app:
    ```
    INSTANCE_IP=$(curl -s http://instance-data/latest/meta-data/public-ipv4)
    echo $INSTANCE_IP

    curl -Ls http://$INSTANCE_IP:8080/
    curl -Ls http://$INSTANCE_IP:8080/_hc
    ```

    1. Create Image
    ```
    AMI_NAME=ami-$UNIQ
    AMI_ID=$(aws ec2 create-image \
        --instance-id $INSTANCE_ID \
        --name $AMI_NAME \
        --query ImageId \
        --output text)
    echo AMI_ID=$AMI_ID
    ```
    1. Create Launch Template
    ```
    LT_NAME=lt-$UNIQ
    
    LT=''
    LT=${LT}'{"ImageId":"'
    LT=${LT}$AMI_ID
    LT=${LT}'","InstanceType":"'
    LT=${LT}$APP_INSTANCE_TYPE
    LT=${LT}'","NetworkInterfaces":[{'
    LT=${LT}'"DeviceIndex":0,'
    LT=${LT}'"AssociatePublicIpAddress":true,'
    LT=${LT}'"DeleteOnTermination":true,'
    LT=${LT}'"Groups":["'
    LT=${LT}$APP_SECG_ID
    LT=${LT}'"]'
    LT=${LT}'}]'
    LT=${LT}'}'
    LT=${LT}''
    echo $LT 
    echo $LT | jq
    
    LT_ID=$(aws ec2 create-launch-template \
        --launch-template-name "$LT_NAME" \
        --launch-template-data "$LT" \
        --query "LaunchTemplate.LaunchTemplateId" \
        --output text)

    echo LT_ID=$LT_ID
    ```
    
    1. Create Target Group
    ```
    TG_NAME=tg-$UNIQ

    TG_ARN=RR$(aws elbv2 create-target-group \
        --name $TG_NAME \
        --protocol HTTP \
        --protocol-version HTTP1 \
        --port 8080 \
        --vpc-id $VPC_ID \
        --health-check-enabled \
        --health-check-path '/_hc' \
        --health-check-interval-seconds 30 \
        --health-check-timeout-seconds 5 \
        --healthy-threshold-count 3 \
        --unhealthy-threshold-count 2 \
        --target-type instance \
        --query "TargetGroups[0].TargetGroupArn" \
        --output text)
 
    ```

1. Create Load Balancer Security Group
```
    ALB_PORT=8080
    ALB_CIDR=0.0.0.0/0

    ALB_SECG=$(aws ec2 create-security-group \
        --group-name "secg-alb-$UNIQ" \
        --description "secg-alb-$UNIQ" \
        --vpc-id $VPC_ID \
        --query "GroupId" \
        --output text)

    echo ALB_SECG=$ALB_SECG

    ALB_SECG_ID=$(aws ec2 describe-security-groups \
        --filter Name=vpc-id,Values=$VPC_ID Name=group-name,Values=secg-app-$UNIQ \
        --query 'SecurityGroups[*].[GroupId]' \
        --output text)
            
    echo ALB_SECG_ID=$ALB_SECG_ID
            
    aws ec2 authorize-security-group-ingress \
        --group-id $ALB_SECG \
        --protocol tcp \
        --port $ALB_PORT \
        --cidr $ALB_CIDR
    ```
1. Create Load Balancer
    ```
    ALB_NAME=alb-$UNIQ

    ALB_ARN=$(aws elbv2 create-load-balancer \
        --name $ALB_NAME \
        --subnets $NET_A $NET_B \
        --security-groups $ALB_
        --query "LoadBalancers[0].LoadBalancerArn"
        --output text)

    aws elbv2 create-listener \
        --load-balancer-arn $ALB_SECG_ID \
        --protocol HTTP --port 80  \
        --default-actions Type=forward,TargetGroupArn=$TG_ARN

    ```

1. Create Auto-Scaling Group
    ```
    ASG_NAME=asg-$UNIQ
    aws autoscaling create-auto-scaling-group \
        --auto-scaling-group-name $ASG_NAME \
        --launch-template LaunchTemplateId=$LT_ID \
        --min-size 0 \
        --max-size 4 \
        --desired-capacity 1 \
        --availability-zones $AZ1 $AZ2 \
        --target-group-arns $TG_ARN \
        --vpc-zone-identifier "$NET_A, $NET_B"

    ```

1. Terminate instance
    ```
    aws ec2 terminate-instances --instance-ids $INSTANCE_ID
    ```
1. (Optional) Create Route53 Record
    ```
    ZONE_NAME=id42.cc
    ZONE_ID=Z04998672H3BXHYZIROP3

    export RECORD_NAME="$UNIQ.$ZONE_NAME"
    export RECORD_VALUE=$(aws elbv2 describe-load-balancers \
        --load-balancer-arns $ALB_ARB \
        --query "LoadBalancers[0].DNSName" \
        --output text)
    
    echo $RECORD_NAME=$RECORD_VALUE 
    envsubst < templates/route53_cname.tpl.json > .route53_cname.json
    cat .route53_cname.json

    aws route53 change-resource-record-sets --hosted-zone-id $ZONE_ID --change-batch file://.route53_cname.json
    ```
    	
    
    1. (Optional) Create Route53 Health Check
    ```
    HC_ID=hc-$UNIQ
    HC_CFG="Port=80,Type=HTTP,ResourcePath=/,FullyQualifiedDomainName=$RECORD_NAME,MeasureLatency=true,Disabled=false"

    echo $HC_CFG

    aws route53 create-health-check \
        --caller-reference="$HC_ID" \
        --health-check-config="$HC_CFG" 
    ```
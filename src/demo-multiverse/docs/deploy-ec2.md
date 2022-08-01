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
1. Check user
    ```
    whoami
    sudo su - ec2-user
    ```
1. Install Java
    ```
    JAVA_VERSION=22.1.0.1.r17-gln
__    curl -s "https://get.sdkman.io" | bash
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
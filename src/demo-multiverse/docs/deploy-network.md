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

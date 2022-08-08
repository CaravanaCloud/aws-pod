aws sts get-caller-identity

aws configure 

aws configure --profile myteam
export AWS_PROFILE="myteam"

aws ec2 describe-availability-zones

aws ec2 describe-availability-zones \
    --output "text"

aws ec2 describe-availability-zones \
    --query "AvailabilityZones[].ZoneName"

VPC_ID=$(aws ec2 create-vpc \
    --cidr-block "10.0.0.0/16" \
    --query "Vpc.VpcId" \
    --output text)

aws ec2 wait vpc-available \
    --vpc-ids="$VPC_ID" \
    && echo "$VPC_ID is available"


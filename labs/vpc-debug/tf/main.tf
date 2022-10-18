# aws sts get-caller-identity
# cd labs/vpc-debug/tf/
# terraform init
# terraform apply -auto-approve
# terraform destroy -auto-approve

# NO SPOILERS ;)
 
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

# Configure the AWS Provider
provider "aws" {
  region = "us-west-2"
}


variable "domain_name" {
  type    = string
  default = "hashcorp.cloud"
}

variable "username" {
  type    = string
  default = "gohorse"
}

variable "az1" {
  type    = string
  default = "us-west-2a"
}

variable "az2" {
  type    = string
  default = "us-west-2b"
}

data "aws_caller_identity" "current" {}

data "aws_iam_policy" "ssm-policy" {
  arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# Create the role
resource "aws_iam_role" "instance-role" {
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      },
    ]
  })
}

# Attach the policy to the role
resource "aws_iam_role_policy_attachment" "attach-s3" {
  role       = aws_iam_role.instance-role.name
  policy_arn = data.aws_iam_policy.ssm-policy.arn
}

resource "aws_iam_instance_profile" "ssm-profile" {
  name = "ssm-profile"
  role = aws_iam_role.instance-role.name
}

# Create a VPC
resource "aws_vpc" "lab_vpc" {
  cidr_block = "10.0.0.0/16"
  enable_dns_support = true
  enable_dns_hostnames = true
}

resource "aws_internet_gateway" "gw" {
  vpc_id = aws_vpc.lab_vpc.id
}

# Healthy side
resource "aws_route_table" "routes1" {
  vpc_id = aws_vpc.lab_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.gw.id
  }
}

resource "aws_subnet" "pub1" {
  vpc_id     = aws_vpc.lab_vpc.id
  cidr_block = "10.0.1.0/24"
  map_public_ip_on_launch = true
  availability_zone = var.az1
}

resource "aws_route_table_association" "pub1routes1" {
  subnet_id      = aws_subnet.pub1.id
  route_table_id = aws_route_table.routes1.id
}

resource "aws_security_group" "sg1" {
  name        = "lab-sg1"
  description = "lab-sg1"
  vpc_id      = aws_vpc.lab_vpc.id

  ingress {
    description      = "Allow 80"
    from_port        = 80
    to_port          = 80
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
  }

  ingress {
    description      = "Allow 22"
    from_port        = 22
    to_port          = 22
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}

resource "aws_instance" "web2048-ok" {
  ami           = "ami-a0cfeed8"
  instance_type = "t3.micro"
  iam_instance_profile = aws_iam_instance_profile.ssm-profile.name
  vpc_security_group_ids = [aws_security_group.sg1.id]
  subnet_id = aws_subnet.pub1.id

  tags = {
    Name = "lab-web2048-ok"
  }

  user_data = <<EOF
#!/bin/bash
yum -y install docker
service docker start
usermod -a -G docker ec2-user
docker run -d --rm -p80:80 alexwhen/docker-2048
echo "user data done"
EOF
}

# Unhealthy Side
resource "aws_route_table" "routes2" {
  vpc_id = aws_vpc.lab_vpc.id

# BUG: No route to gateway
#  route {
#    cidr_block = "0.0.0.0/24"
#    gateway_id = aws_internet_gateway.gw.id
#  }
}

resource "aws_subnet" "pub2" {
  vpc_id     = aws_vpc.lab_vpc.id
  cidr_block = "10.0.2.0/24"
  availability_zone = var.az2 
  #BUG: missing  map_public_ip_on_launch = true
}

resource "aws_route_table_association" "pub2routes2" {
  subnet_id      = aws_subnet.pub2.id
  route_table_id = aws_route_table.routes2.id
}

resource "aws_security_group" "sg2" {
  name        = "lab-sg2"
  description = "lab-sg2"
  vpc_id      = aws_vpc.lab_vpc.id

# BUG: No ingress
#  ingress {
#    description      = "Allow 80"
#    from_port        = 80
#    to_port          = 80
#    protocol         = "tcp"
#    cidr_blocks      = ["0.0.0.0/0"]
#  }

  ingress {
    description      = "Allow 22"
    from_port        = 22
    to_port          = 22
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
  }


# BUG: No egress allow
#  egress {
#    from_port        = 0
#    to_port          = 0
#    protocol         = "-1"
#    cidr_blocks      = ["0.0.0.0/0"]
#    ipv6_cidr_blocks = ["::/0"]
#  }

}

resource "aws_instance" "web2048-error" {
  ami           = "ami-a0cfeed8"
  instance_type = "t3.micro"
  vpc_security_group_ids = [aws_security_group.sg2.id]
  subnet_id = aws_subnet.pub2.id

  tags = {
    Name = "lab-web2048-error"
  }

  user_data = <<EOF
#!/bin/bash
yum -y install docker
service docker start
usermod -a -G docker ec2-user
docker run -d --rm -p80:80 alexwhen/docker-2048
echo "user data done"
EOF
}


resource "aws_security_group" "sglb" {
  name        = "lab-sglb"
  description = "lab-sglb"
  vpc_id      = aws_vpc.lab_vpc.id

  ingress {
    description      = "Allow 80"
    from_port        = 80
    to_port          = 80
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}

resource "aws_lb_target_group" "tg" {
  name     = "lab-tg"
  port     = 80
  protocol = "HTTP"
  vpc_id   = aws_vpc.lab_vpc.id 
  target_type  = "instance"
}

resource "aws_lb" "lb" {
  name               = "lab-lb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.sglb.id]
  subnets            = [aws_subnet.pub1.id, aws_subnet.pub2.id]
}

resource "aws_lb_listener" "lb_listener_http" {
   load_balancer_arn    = aws_lb.lb.id
   port                 = "80"
   protocol             = "HTTP"
   default_action {
    target_group_arn = aws_lb_target_group.tg.id
    type             = "forward"
  }
}

resource "aws_lb_target_group_attachment" "attach1" {
 target_group_arn = aws_lb_target_group.tg.arn
 target_id        = aws_instance.web2048-ok.id

#  port             = 80
}

resource "aws_lb_target_group_attachment" "attach2" {
  target_group_arn = aws_lb_target_group.tg.arn
  target_id        = aws_instance.web2048-error.id
#  port             = 80
}

resource "aws_vpc" "main" {
  cidr_block = "10.0.0.0/16"
}

resource "aws_iam_user" "user" {
  name = var.username
}

resource "aws_iam_user_policy_attachment" "grantpoweruser" {
  user       = aws_iam_user.user.name
  policy_arn = "arn:aws:iam::aws:policy/PowerUserAccess"
}

resource "aws_iam_access_key" "keys" {
  user = aws_iam_user.user.name
}

output "ak" {
  value = aws_iam_access_key.keys.id
}

output "lb_dns" {
  value = aws_lb.lb.dns_name
}

#output "sk" {
#  value = aws_iam_access_key.keys.secret
#}

output "acct_id" {
  value = data.aws_caller_identity.current.account_id
}

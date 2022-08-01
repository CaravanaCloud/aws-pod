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
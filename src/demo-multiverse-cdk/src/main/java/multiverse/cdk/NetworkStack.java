package multiverse.cdk;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.constructs.Construct;

import java.util.List;

import static software.amazon.awscdk.services.ec2.DefaultInstanceTenancy.DEFAULT;
import static software.amazon.awscdk.services.ec2.SubnetType.*;

public class NetworkStack extends BaseStack {
    Vpc vpc;
    SecurityGroup webSG;

    public NetworkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public NetworkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        var pubNets = SubnetConfiguration.builder()
                .cidrMask(24)
                .name("MultiverseNetPub")
                .subnetType(PUBLIC)
                .build();

        var subnets = List.of(pubNets);

        this.vpc = Vpc.Builder.create(this, "MultiverseVPC")
                .cidr("10.0.0.0/16")
                .maxAzs(3)
                .defaultInstanceTenancy(DEFAULT)
                .enableDnsSupport(true)
                .enableDnsHostnames(true)
                .subnetConfiguration(subnets)
                .build();

        this.webSG = SecurityGroup.Builder.create(this, "MultiverseWebSecg")
                .vpc(vpc)
                .build();
        webSG.addIngressRule(Peer.anyIpv4(), Port.allTcp());

        output("OutputVpcId", vpc.getVpcId());
        output("OutputWebSecgId", webSG.getSecurityGroupId());
    }



    public Vpc getVPC(){
        return vpc;
    }

    public SecurityGroup getWebSG() {
        return webSG;
    }
}

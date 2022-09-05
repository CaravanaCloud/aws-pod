package multiverse.cdk;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.List;

import static java.lang.String.format;

public class DatabaseStack extends BaseStack  {
    String jdbcURL;
    String username = "root";
    String password = "Masterkey123";
    String databaseName = "multiversedb";
    String databaseOpts = "?useSSL=false";

    public DatabaseStack(final Construct scope, final String id, StackProps stackProps) {
        this(scope, id, null, null);
    }
    public DatabaseStack(final Construct scope, final String id, final StackProps props, NetworkStack net) {
        super(scope, id, props);


        var vpc = net.vpc;
        var publicNets = SubnetSelection
                .builder()
                .subnets(net.vpc.getPublicSubnets())
                .build();

        var subnetGroup = SubnetGroup.Builder.create(this, "MultiverseRDSSubnetGroup")
                .vpc(net.vpc)
                .description("MultiverseRDSSubnetGroup")
                .vpcSubnets(publicNets)
                .build();

        var mysql8 = DatabaseInstanceEngine.mysql(
                MySqlInstanceEngineProps
                        .builder()
                        .version(MysqlEngineVersion.VER_8_0)
                        .build()
        );

        var creds = Credentials
                .fromPassword(username,
                        SecretValue.unsafePlainText(password));



        var dbSG = SecurityGroup.Builder.create(this, "MultiverseRDSSecG")
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();

        dbSG.addIngressRule(Peer.anyIpv4(), Port.tcp(3306), "Allow MySQL IN");

        var secgs = List.of(dbSG);
        var rds = DatabaseInstance.Builder.create(this, "MultiverseDBInstance")
                .vpc(vpc)
                .subnetGroup(subnetGroup)
                .securityGroups(secgs)
                .engine(mysql8)
                .instanceType(InstanceType.of(InstanceClass.T4G, InstanceSize.MICRO))
                .credentials(creds)
                .multiAz(false)
                .databaseName(databaseName)
                .publiclyAccessible(true)
                .build();


        var instanceEndpointAddress = rds.getDbInstanceEndpointAddress();
        var instanceEndpointPort = rds.getDbInstanceEndpointPort();

        var cliHelpout = format("mysql -h%s -P%s -u%s -p %s",
                instanceEndpointAddress,
                instanceEndpointPort,
                username,
                databaseName);


        jdbcURL = "jdbc:mysql://" + instanceEndpointAddress
                + ":"
                + instanceEndpointPort
                + "/"
                + databaseName
                + databaseOpts;
        output("InstanceIdentifier", rds.getInstanceIdentifier());
        output("InstanceEndpointAddress",instanceEndpointAddress);
        output("CLIHelpout", cliHelpout);
        output("JDBCURL",jdbcURL);
    }

    public String getJdbcURL(){
        return jdbcURL;
    }

    public String getUsername(){
        return username;
    }
    public String getPassword(){
        return password;
    }}

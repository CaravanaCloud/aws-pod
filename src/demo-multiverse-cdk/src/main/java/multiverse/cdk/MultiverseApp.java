package multiverse.cdk;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

import javax.xml.crypto.Data;

public class MultiverseApp {
    @SuppressWarnings("unused")
    public static void main(final String[] args) {
        App app = new App();
        var bucket = new BucketStack(app, "MultiverseBucketStack", stackProps());
        var net = new NetworkStack(app, "MultiverseNetworkStack", stackProps());
        var db = (DatabaseStack) null;
        /*
        var db = new DatabaseStack(app, "MultiverseDatabaseStack", stackProps(), net);
         */
        var lambda = new LambdaStack(app, "MultiverseLambdaStack", stackProps(), net, db);
        var distro = new DistributionStack(app, "MultiverseCDNStack", stackProps(), net, bucket, lambda);
        var mv = new MultiverseStack(app, "MultiverseStack", stackProps(), bucket, net, null, distro);

        app.synth();

    }

    @NotNull
    private static StackProps stackProps() {
        return StackProps.builder()
                .build();
    }
}


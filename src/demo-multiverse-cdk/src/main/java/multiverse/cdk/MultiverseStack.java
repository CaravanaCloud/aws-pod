package multiverse.cdk;

import org.jetbrains.annotations.NotNull;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;


public class MultiverseStack extends Stack {
    public MultiverseStack(final Construct scope, final String id, StackProps stackProps) {
        this(scope, id, null, null, null, null, null);
    }

    public MultiverseStack(final Construct scope, final String id, final StackProps props, BucketStack bucketStack, NetworkStack netStack, DatabaseStack dbStack, DistributionStack distroStack) {
        super(scope, id, props);
        var bucket = bucketStack.bucket;
        // bucket.grantRead(distroStack.oai);
    }
}

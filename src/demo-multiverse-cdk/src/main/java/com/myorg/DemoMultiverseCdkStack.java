package com.myorg;

import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.ec2.Vpc;

public class DemoMultiverseCdkStack extends Stack {
    public DemoMultiverseCdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public DemoMultiverseCdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // The code that defines your stack goes here

        // example resource
        // final Queue queue = Queue.Builder.create(this, "DemoMultiverseCdkQueue")
        //         .visibilityTimeout(Duration.seconds(300))
        //         .build();
        var vpc = Vpc.Builder.create(this, "multiverse-vpc")
            .build();
    }
}

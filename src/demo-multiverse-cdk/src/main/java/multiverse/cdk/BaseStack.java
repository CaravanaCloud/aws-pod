package multiverse.cdk;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

public abstract class BaseStack extends Stack {
    public BaseStack(Construct scope, String id, StackProps props) {
        super(scope, id, props);
    }

    protected CfnOutput output(String id, String value) {
        return CfnOutput.Builder.create(this, id)
                .value(value)
                .build();
    }
}

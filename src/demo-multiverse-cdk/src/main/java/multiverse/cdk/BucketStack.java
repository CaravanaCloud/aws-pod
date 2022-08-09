package multiverse.cdk;

import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cloudfront.OriginAccessIdentity;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.ISource;
import software.amazon.awscdk.services.s3.deployment.Source;
import software.constructs.Construct;

import java.util.List;

public class BucketStack extends BaseStack {
    Bucket bucket;
    OriginAccessIdentity oai;

    public BucketStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public BucketStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        this.bucket  = Bucket.Builder.create(this, "MultiverseBucket")
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .build();

        var resources = Source.asset("../demo-multiverse/src/main/resources/META-INF/resources");
        List<? extends ISource> srcs = List.of(resources);
        var deploy = BucketDeployment.Builder.create(this, "MultiverseObjects")
                .sources(srcs)
                .destinationBucket(bucket)
                .build();
        var bucketName = bucket.getBucketName();
        this.oai = OriginAccessIdentity.Builder.create(this, "MultiverseOAI")
                .comment("MultiverseOriginAccessIdentity")
                .build();
        bucket.grantRead(this.oai);
        output("MultiverseBucketName", bucketName);
        output("MultiverseBucketArn", bucket.getBucketArn());
        output("CLI Helpout", "aws s3 ls s3://"+bucketName+"/");
        output("OriginAccessIdentity", oai.getOriginAccessIdentityId());
    }

}

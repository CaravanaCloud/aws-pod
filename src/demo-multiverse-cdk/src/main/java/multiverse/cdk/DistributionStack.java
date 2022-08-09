package multiverse.cdk;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cloudfront.*;
import software.amazon.awscdk.services.cloudfront.origins.LoadBalancerV2Origin;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class DistributionStack extends BaseStack {
    CloudFrontWebDistribution  distro;

    public DistributionStack(final Construct scope, final String id, StackProps stackProps) {
        this(scope, id, null, null, null, null);
    }

    public DistributionStack(final Construct scope, final String id, final StackProps props,
                           NetworkStack net,
                             BucketStack bucketStack,
                             LambdaStack lambdaStack) {
        super(scope, id, props);


        var staticBucket = bucketStack.bucket;


        var staticOrigin = S3OriginConfig.builder()
                .originAccessIdentity(bucketStack.oai)
                .s3BucketSource(staticBucket)
                .build();

        var defaultBehavior = Behavior.builder()
                .isDefaultBehavior(true)
                .pathPattern("*")
                .defaultTtl(Duration.minutes(5))
                .maxTtl(Duration.minutes(5))
                .build();

        var imagesBehavior = Behavior.builder()
                .pathPattern("/images/*")
                .defaultTtl(Duration.minutes(5))
                .maxTtl(Duration.minutes(5))
                .build();

        var assetsBehavior = Behavior.builder()
                .pathPattern("/assets/*")
                .defaultTtl(Duration.minutes(5))
                .maxTtl(Duration.minutes(5))
                .build();

        var imagesSourceCfg = SourceConfiguration.builder()
                .behaviors(List.of(imagesBehavior))
                .s3OriginSource(staticOrigin)
                .build();

        var assetsSourceCfg = SourceConfiguration.builder()
                .behaviors(List.of(assetsBehavior))
                .s3OriginSource(staticOrigin)
                .build();

        var apiDomainName = Utils.domainName(lambdaStack.httpApi.getApiEndpoint());
        System.out.println("API domain name: "+apiDomainName);

        var apiOrigin = CustomOriginConfig
                .builder()
                .domainName(apiDomainName)
                .build();


        var defaultSourceCfg = SourceConfiguration.builder()
                .behaviors(List.of(defaultBehavior))
                //.s3OriginSource(staticOrigin)
                .customOriginSource(apiOrigin)
                .build();

        var sourceCfgs = List.of(
                imagesSourceCfg,
                assetsSourceCfg,
                defaultSourceCfg
        );

        this.distro = CloudFrontWebDistribution.Builder.create(this, "CloudFrontWebDistribution")
                .comment("Multiverse CloudFront distribution")
                .originConfigs(sourceCfgs)
                .priceClass(PriceClass.PRICE_CLASS_100)
                .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                .build();
        output("DistroDomain", distro.getDistributionDomainName());
        output("HttpApiDomainName", apiDomainName);
    }
}
// complexidade, custo, dada sync
// seguranca, chargeback, politicas, merger
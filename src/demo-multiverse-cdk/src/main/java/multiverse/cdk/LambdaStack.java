package multiverse.cdk;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.EndpointConfiguration;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigatewayv2.alpha.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.alpha.PayloadFormatVersion;
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.HttpLambdaIntegration;
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.HttpLambdaIntegrationProps;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class LambdaStack extends BaseStack {
    HttpApi httpApi;
    private FunctionUrl fnUrl;

    public LambdaStack(final Construct scope, final String id) {
        this(scope, id, null, null, null);
    }

    public LambdaStack(final Construct scope, final String id, final StackProps props,
                       NetworkStack netStack, DatabaseStack dbStack) {
        super(scope, id, props);
        var stamp = ""+System.currentTimeMillis();

        var jdbcUrl = dbStack.getJdbcURL();
        var user = dbStack.getUsername();
        var pass = dbStack.getPassword();
        var env = Map.of(
                "MV_REPO_TYPE", "MEM"
//                ,"QUARKUS_DATASOURCE_JDBC_URL", jdbcUrl
//               ,"QUARKUS_DATASOURCE_PASSWORD", pass
//                ,"QUARKUS_DATASOURCE_USERNAME", user
        );

        var adminPolicy = ManagedPolicy.fromManagedPolicyArn(this,
                "AdministratorAccess",
                "arn:aws:iam::aws:policy/AdministratorAccess");


        var lambdaRole = Role.Builder.create(this, "MultiverseLambdaRole")
                .assumedBy(ServicePrincipal.Builder.create("lambda.amazonaws.com").build())
                .managedPolicies(List.of(adminPolicy))
                .build();

        var code = Code.fromAsset("../demo-multiverse-sls/target/function.zip");

        var function = Function.Builder.create(this, "MultiverseLambda")
                .functionName("MultiverseLambda-1-"+stamp)
                .timeout(Duration.minutes(5))
                .runtime(Runtime.JAVA_11)
                .role(lambdaRole)
                .environment(env)
                .memorySize(2048)
                .handler("io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest")
                .code(code)
                .build();

        var cors = FunctionUrlCorsOptions.builder()
                .allowedOrigins(List.of("*"))
                .allowedHeaders(List.of("*"))
                .allowedMethods(List.of(HttpMethod.ALL))
                .build();

        fnUrl = FunctionUrl.Builder.create(this, "MultiverseFnUrl")
                .function(function)
                .authType(FunctionUrlAuthType.NONE)
                .cors(cors)
                .build();

        this.httpApi = new HttpApi(this, "HttpApi");

        var httpLambdaIntegration = new HttpLambdaIntegration(
                "this",
                function,
                HttpLambdaIntegrationProps.builder()
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                        .build()
        );

        List<? extends software.amazon.awscdk.services.apigatewayv2.alpha.HttpMethod> methods = List.of(software.amazon.awscdk.services.apigatewayv2.alpha.HttpMethod.ANY);
        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/{proxy+}")
                .methods(methods)
                .integration(httpLambdaIntegration)
                .build()
        );
        output("FnUrl",  fnUrl.getUrl());
        output("APIUrl", httpApi.getUrl());
        output("APIEndpoint", httpApi.getApiEndpoint());
    }

    public String getFnUrl(){
        return fnUrl.getUrl();
    }
}

package multiverse.cdk;

import software.amazon.awscdk.Fn;

import java.net.URI;
import java.net.URISyntaxException;

public class Utils {
    public static void main(String[] args) {
        String url = " https://cwzdjdbqre.execute-api.us-west-2.amazonaws.com/";
        String domainName = domainName(url);
        System.out.println(domainName);
    }

    public static String domainName(String url) {
        return Fn.parseDomainName(url);
    }
}

package multiverse.rs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.UUID;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("_uuid")
public class UUIDResource {
    static final String uuid = UUID.randomUUID().toString();

    @GET
    @Produces(TEXT_PLAIN)
    public static String getUuid() {
        return uuid;
    }
}

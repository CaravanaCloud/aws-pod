package multiverse.rs;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Template;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/index")
public class IndexResource{
    @Inject
    Template index;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getIndex(){
        return index.instance();
    }
}
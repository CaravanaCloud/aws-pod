package multiverse.rs;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Template;
import javax.ws.rs.*;

@Path("/")
public class IndexResource{
    @Inject
    Template indexTpl;

    @GET
    @Produces(MediaType)
    public void getIndex(){
        
    }
}
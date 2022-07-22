package multiverse.rs;

import com.mysql.cj.jdbc.ConnectionImpl;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import javax.sql.DataSource;
import java.sql.SQLException;

@Path("_hc")
public class HealthCheckResource {
    @Inject
    DataSource ds;

    @GET
    @Produces(TEXT_PLAIN)
    public Response getHealthCheck(){
        try(var conn = ds.getConnection();
            var stmt = conn.createStatement();
            var rs = stmt.executeQuery("SELECT 1+1")){
            if (rs.next()){
                var value = rs.getInt(1);
                if (2 == value){
                    return success();
                }
            }
        } catch (SQLException e) {
            return error();
        }
        return error();
    }

    private Response success() {
        return Response.ok()
                .entity("Health Check OK!")
                .build();
    }

    private Response error() {
        return Response.serverError()
                .entity("Health Check Failed")
                .build();
    }
}

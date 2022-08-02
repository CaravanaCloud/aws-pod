package multiverse.rs;

import multiverse.data.Repo;
import multiverse.data.Repos;
import multiverse.model.Tile;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("tiles")
public class TilesResource {
    @Inject
    Repos repos;

    @GET
    @Produces(APPLICATION_JSON)
    public List<Tile> getAllTiles(){
        return tiles().findAll();
    }

    private Repo<Tile> tiles() {
        return repos.tileRepo();
    }
}

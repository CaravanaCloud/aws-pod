package multiverse.data;

import multiverse.Configuration;
import multiverse.model.Tile;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class Repos {
    @Inject
    Configuration cfg;

    public static void init(Repo<Tile> tileRepo) {
        var tiles = tileRepo.findAll();
        if (tiles.isEmpty()){
            for (int i = 0; i < 20; i++) {
                tileRepo.create(Tile.random());
            }
        }
    }

    @Inject
    Instance<TileRepoMEM> tilesMEM;
    @Inject
    Instance<TileRepoDDB> tilesDDB;
    @Inject
    Instance<TileRepoJPA> tilesJPA;


    public Repo<Tile> tileRepo(){
        switch (cfg.repoType()){
            case MEM: return tilesMEM.get();
            case DDB: return tilesDDB.get();
            case JPA: return tilesJPA.get();
        }
        return null;
    }
}

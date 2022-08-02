package multiverse.data;

import multiverse.Configuration;
import multiverse.model.Tile;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
@ApplicationScoped

public class TileRepoMEM implements Repo<Tile> {
    static final Map<String/*UUID*/, Tile> tiles = new HashMap<>();

    @PostConstruct
    public void postConstruct(){
        Repos.init(this);
    }

    @Override
    public List<Tile> findAll() {
        return new ArrayList<>(tiles.values());
    }

    @Override
    public Tile create(Tile tile) {
        String uuid = UUID.randomUUID().toString();
        tile.setUuid(uuid);
        tiles.put(uuid, tile);
        return tile;
    }

    @Override
    public List<Tile> read(String... uuids) {
        var uuidSet = Set.of(uuids);
        return tiles.values().stream()
                .filter(t -> uuidSet.contains(t.getUuid()))
                .collect(Collectors.toList());
    }

    @Override
    public Tile update(Tile tile) {
        tiles.put(tile.getUuid(), tile);
        return tile;
    }

    @Override
    public void delete(String... uuid) {
        Stream.of(uuid).forEach(tiles::remove);
    }
}

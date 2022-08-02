
package multiverse.data;


import multiverse.Configuration;
import multiverse.model.Tile;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Produces;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
@ApplicationScoped
public class TileRepoDDB implements Repo<Tile> {
    @Inject
    Configuration cfg;

    DynamoDbEnhancedClient ddbx = ddbx();


    public static DynamoDbEnhancedClient ddbx(){
        return DynamoDbEnhancedClient.create();
    }

    public DynamoDbTable<Tile> getTable(){
        return ddbx.table(getTilesTableName(), TableSchema.fromBean(Tile.class));
    }

    private String getTilesTableName() {
        return cfg.tilesTable();
    }


    @Override
    public List<Tile> findAll() {
        var table = getTable();
        var scan = table.scan();
        var tiles = scan.stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
        return tiles;
    }

    @Override
    public Tile create(Tile tile) {
        getTable().putItem(tile);
        return tile;
    }

    @Override
    public List<Tile> read(String... uuid) {
        return Stream.of(uuid)
                .map(this::readOne)
                .collect(Collectors.toList());
    }

    private Tile readOne(String uuid) {
        Key key = key(uuid);
        var result = getTable().getItem(key);
        return result;
    }

    private static Key key(String uuid) {
        Key key = Key.builder()
                .partitionValue(uuid)
                .build();
        return key;
    }

    @Override
    public Tile update(Tile tile) {
        getTable().putItem(tile);
        return tile;
    }

    @Override
    public void delete(String... uuid) {
        Stream.of(uuid).forEach(this::deleteOne);
    }

    private void deleteOne(String uuid) {
        getTable().deleteItem(key(uuid));
    }
}

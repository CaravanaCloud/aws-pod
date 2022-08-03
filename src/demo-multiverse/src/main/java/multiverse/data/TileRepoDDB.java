package multiverse.data;


import multiverse.Configuration;
import multiverse.model.Tile;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
@ApplicationScoped
public class TileRepoDDB implements Repo<Tile> {
    @Inject
    Configuration cfg;


    public static DynamoDbClient ddb(){
        return DynamoDbClient.builder().build();
    }

    @PostConstruct
    public void postConstruct(){
        Repos.init(this);
    }

    private void ping(DynamoDbTable<Tile> table) {
        table.scan();
    }

    private String getTilesTableName() {
        var tableName = cfg.tilesTable();
        return tableName;
    }


    @Override
    public List<Tile> findAll() {
        var ddb = ddb();
        ScanRequest req = ScanRequest.builder()
                .tableName(getTilesTableName())
                .build();
        var tiles = ddb.scanPaginator(req).stream()
                .flatMap(p -> p.items().stream())
                .map(m -> toTile(m))
                .collect(Collectors.toList());
        return tiles;
    }

    private Tile toTile(Map<String, AttributeValue> item) {
        var uuid = item.get("uuid").s();
        var imgSrc = item.get("imgSrc").s();
        var title = item.get("title").s();
        return Tile.of(uuid, imgSrc, title);
    }

    @Override
    public Tile create(Tile tile) {
        var ddb = ddb();
        if (tile.getUuid() == null ||
                tile.getUuid().isEmpty()){
            tile.setUuid(UUID.randomUUID().toString());
        }
        var item = fromTile(tile);
        var req = PutItemRequest.builder()
                .tableName(getTilesTableName())
                .item(item)
                .build();
        ddb.putItem(req);
        return tile;
    }

    private Map<String, AttributeValue> fromTile(Tile tile) {
        return Map.of(
                "uuid", s(tile.getUuid()),
                "imgSrc", s(tile.getImgSrc()),
                "title", s(tile.getTitle())
        );
    }

    private AttributeValue s(String str) {
        return AttributeValue.builder()
                .s(str)
                .build();
    }

    @Override
    public List<Tile> read(String... uuid) {
        return Stream.of(uuid)
                .map(this::readOne)
                .collect(Collectors.toList());
    }

    private Tile readOne(String uuid) {
        var ddb = ddb();
        var uuidVal = AttributeValue.builder().s(uuid).build();
        var req = GetItemRequest.builder()
                .key(Map.of("uuid", uuidVal))
                .build();
        var item = ddb.getItem(req).item();
        var result = toTile(item);
        return result;
    }

    @Override
    public Tile update(Tile tile) {
        return create(tile);
    }

    @Override
    public void delete(String... uuid) {
        Stream.of(uuid).forEach(this::deleteOne);
    }

    private void deleteOne(String uuid) {
        var req = DeleteItemRequest.builder()
                .tableName(getTilesTableName())
                .key(Map.of(uuid, s(uuid)))
                .build();
        ddb().deleteItem(req);
    }
}

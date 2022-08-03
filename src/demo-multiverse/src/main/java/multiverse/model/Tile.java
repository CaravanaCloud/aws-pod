package multiverse.model;

import org.hibernate.annotations.GenericGenerator;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import java.util.UUID;

@Entity
@DynamoDbBean
@NamedNativeQuery(name = "Tile.findAll",
        query ="select t from Tile t")
public class Tile {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator( name = "uuid2", strategy = "uuid2" )
    String uuid;
    String title;
    String imgSrc;

    public Tile(){}

    public Tile(String title, String imgSrc) {
        this.title = title;
        this.imgSrc = imgSrc;
    }

    public static Tile random() {
        var t = new Tile();
        t.imgSrc = "https://source.unsplash.com/random";
        t.title = "Ipsum lorem magnus est";
        return t;
    }

    public static Tile empty() {
        return new Tile();
    }

    public static Tile of(String uuid, String imgSrc, String title) {
        var tile = empty();
        tile.setUuid(uuid);
        tile.setImgSrc(imgSrc);
        tile.setTitle(title);
        return tile;
    }

    @DynamoDbPartitionKey
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImgSrc() {
        return imgSrc;
    }

    public void setImgSrc(String imgSrc) {
        this.imgSrc = imgSrc;
    }


}
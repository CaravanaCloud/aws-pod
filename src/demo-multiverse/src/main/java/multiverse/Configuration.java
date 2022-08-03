package multiverse;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import multiverse.data.RepoType;

@ConfigMapping(prefix = "mv")
@StaticInitSafe
public interface Configuration {
    @WithDefault("MEM")
    @WithName("repoType")
    RepoType repoType();

    @WithName("ddb.tables.tile")
    @WithDefault("tiles")
    String tilesTable();
}

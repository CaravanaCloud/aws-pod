package multiverse.data;

import multiverse.Configuration;
import multiverse.model.Tile;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
@ApplicationScoped
public class TileRepoJPA implements Repo<Tile> {
    @Inject
    EntityManager em;


    @PostConstruct
    public void postConstruct(){
        Repos.init(this);
    }

    @Override
    public List<Tile> findAll() {
        return em.createNamedQuery("Tile.findAll", Tile.class)
                .getResultList();
    }

    @Override
    public Tile create(Tile entity) {
        return em.merge(entity);
    }

    @Override
    public List<Tile> read(String... uuid) {
        return em.createNamedQuery("Tile.findByUUID", Tile.class)
                .setParameter("uuids",List.of(uuid))
                .getResultList();
    }

    @Override
    public Tile update(Tile entity) {
        return em.merge(entity);
    }

    @Override
    public void delete(String... uuid) {
        List.of(uuid).forEach(this::deleteOne);
    }

    private void deleteOne(String uuid) {
        var tile = em.find(Tile.class, uuid);
        em.remove(tile);
    }
}

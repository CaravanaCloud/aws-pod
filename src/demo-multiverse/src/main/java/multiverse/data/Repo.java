package multiverse.data;

import java.util.List;

public interface Repo<T> {
    List<T> findAll();
    T create(T entity);
    List<T> read(String... uuid);
    T update(T entity);
    void delete(String... uuid);
}

package net.quazar.bsync.repository;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface Repository<V, K> {
    Optional<V> findByKey(@NotNull K key);
    List<V> findAll();
    @NotNull V save(@NotNull V value);
    void delete(@NotNull V value);
    void deleteByKey(@NotNull K key);
}

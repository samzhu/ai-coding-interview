import java.util.*;

/**
 * In-memory cache implementation.
 * Implement this class across 4 checkpoints.
 */
public class LocalCache<K, V> implements Cache<K, V> {
    private final CacheConfig config;

    public LocalCache(CacheConfig config) {
        this.config = config;
        // TODO: Initialize your data structures here
    }

    @Override
    public void put(K key, V value) {
        // TODO: Implement — store with default TTL
        throw new UnsupportedOperationException("put not implemented");
    }

    @Override
    public void put(K key, V value, long ttlMs) {
        // TODO: Implement — store with custom TTL
        throw new UnsupportedOperationException("put with TTL not implemented");
    }

    @Override
    public Optional<V> get(K key) {
        // TODO: Implement — return Optional.empty() if not found or expired
        throw new UnsupportedOperationException("get not implemented");
    }

    @Override
    public boolean remove(K key) {
        // TODO: Implement — return true if existed
        throw new UnsupportedOperationException("remove not implemented");
    }

    @Override
    public int size() {
        // TODO: Implement — count only non-expired entries
        throw new UnsupportedOperationException("size not implemented");
    }

    @Override
    public void clear() {
        // TODO: Implement — remove all entries
        throw new UnsupportedOperationException("clear not implemented");
    }

    @Override
    public void cleanUp() {
        // TODO: Implement — remove all expired entries
        throw new UnsupportedOperationException("cleanUp not implemented");
    }
}

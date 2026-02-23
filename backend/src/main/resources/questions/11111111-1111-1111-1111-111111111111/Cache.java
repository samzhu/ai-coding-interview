import java.util.Optional;

/**
 * Generic cache interface.
 * DO NOT MODIFY THIS FILE.
 */
public interface Cache<K, V> {

    /**
     * Store a value with the default TTL from CacheConfig.
     */
    void put(K key, V value);

    /**
     * Store a value with a custom TTL in milliseconds.
     */
    void put(K key, V value, long ttlMs);

    /**
     * Retrieve a value. Returns empty if not found or expired.
     */
    Optional<V> get(K key);

    /**
     * Remove an entry. Returns true if the entry existed.
     */
    boolean remove(K key);

    /**
     * Return the number of non-expired entries.
     */
    int size();

    /**
     * Remove all entries.
     */
    void clear();

    /**
     * Remove all expired entries.
     */
    void cleanUp();
}

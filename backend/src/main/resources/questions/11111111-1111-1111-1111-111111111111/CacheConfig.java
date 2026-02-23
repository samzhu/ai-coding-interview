/**
 * Cache configuration.
 * DO NOT MODIFY THIS FILE.
 */
public record CacheConfig(int maxSize, long defaultTtlMs) {

    public static CacheConfig of(int maxSize, long defaultTtlMs) {
        return new CacheConfig(maxSize, defaultTtlMs);
    }

    /** Default config: 100 entries, 60 second TTL */
    public static CacheConfig defaults() {
        return new CacheConfig(100, 60_000);
    }
}

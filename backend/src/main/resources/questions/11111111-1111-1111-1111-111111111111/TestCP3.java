import java.util.*;

/**
 * Checkpoint 3: LRU eviction tests.
 * DO NOT MODIFY THIS FILE.
 * NOTE: Assumes CP1 and CP2 are implemented.
 */
public class TestCP3 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Test 1: Cache does not exceed maxSize
        test("cache does not exceed maxSize", () -> {
            LocalCache<String, Integer> cache = new LocalCache<>(CacheConfig.of(3, 60000));
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("c", 3);
            cache.put("d", 4); // should evict "a" (LRU)
            assertTrue(cache.size() <= 3, "size should not exceed maxSize, got: " + cache.size());
        });

        // Test 2: LRU entry is evicted
        test("LRU entry is evicted on overflow", () -> {
            LocalCache<String, Integer> cache = new LocalCache<>(CacheConfig.of(3, 60000));
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("c", 3);
            cache.put("d", 4); // evicts "a"
            assertTrue(cache.get("a").isEmpty(), "LRU entry 'a' should be evicted");
            assertTrue(cache.get("d").isPresent(), "new entry 'd' should exist");
        });

        // Test 3: get() updates access order (prevents eviction)
        test("get updates access order", () -> {
            LocalCache<String, Integer> cache = new LocalCache<>(CacheConfig.of(3, 60000));
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("c", 3);
            cache.get("a"); // "a" is now most recently used
            cache.put("d", 4); // should evict "b" (now LRU), not "a"
            assertTrue(cache.get("a").isPresent(), "'a' was accessed, should survive");
            assertTrue(cache.get("b").isEmpty(), "'b' is LRU, should be evicted");
        });

        // Test 4: Expired entries evicted before LRU
        test("expired entries evicted before LRU", () -> {
            LocalCache<String, Integer> cache = new LocalCache<>(CacheConfig.of(3, 60000));
            cache.put("a", 1, 100); // short TTL
            cache.put("b", 2);
            cache.put("c", 3);
            sleep(150); // "a" is now expired
            cache.put("d", 4); // should evict expired "a", not LRU "b"
            assertTrue(cache.get("b").isPresent(), "'b' should survive (expired 'a' evicted first)");
            assertTrue(cache.get("d").isPresent(), "'d' should exist");
        });

        // Test 5: Multiple evictions
        test("multiple evictions maintain maxSize", () -> {
            LocalCache<Integer, String> cache = new LocalCache<>(CacheConfig.of(3, 60000));
            for (int i = 0; i < 10; i++) {
                cache.put(i, "val" + i);
            }
            assertTrue(cache.size() <= 3, "size should be <= 3 after 10 puts, got: " + cache.size());
            // Most recent 3 entries should exist
            assertTrue(cache.get(9).isPresent(), "entry 9 should exist");
            assertTrue(cache.get(8).isPresent(), "entry 8 should exist");
            assertTrue(cache.get(7).isPresent(), "entry 7 should exist");
        });

        // Test 6: put existing key does not cause eviction
        test("put existing key updates without eviction", () -> {
            LocalCache<String, Integer> cache = new LocalCache<>(CacheConfig.of(3, 60000));
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("c", 3);
            cache.put("a", 10); // update existing key
            assertEqual(3, cache.size(), "size should still be 3");
            assertEqual(10, cache.get("a").orElse(-1), "value should be updated");
        });

        printSummary();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static void test(String name, Runnable testCase) {
        try {
            testCase.run();
            passed++;
            System.out.println("  PASS: " + name);
        } catch (AssertionError e) {
            failed++;
            System.out.println("  FAIL: " + name + " — " + e.getMessage());
        } catch (Exception e) {
            failed++;
            System.out.println("  FAIL: " + name + " — " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void assertEqual(Object expected, Object actual, String field) {
        if (!Objects.equals(expected, actual))
            throw new AssertionError("Expected " + field + "='" + expected + "' but got '" + actual + "'");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void printSummary() {
        System.out.println("\n========================================");
        System.out.println("CP3 Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) System.out.println("ALL TESTS PASSED");
        else System.out.println("SOME TESTS FAILED");
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }
}

import java.util.*;

/**
 * Checkpoint 2: TTL expiration tests.
 * DO NOT MODIFY THIS FILE.
 * NOTE: Assumes CP1 is implemented.
 */
public class TestCP2 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Test 1: Entry expires after TTL
        test("entry expires after TTL", () -> {
            LocalCache<String, String> cache = new LocalCache<>(CacheConfig.of(100, 5000));
            cache.put("key", "value", 100); // 100ms TTL
            assertTrue(cache.get("key").isPresent(), "should exist before expiry");
            sleep(150);
            assertTrue(cache.get("key").isEmpty(), "should expire after TTL");
        });

        // Test 2: Entry with default TTL doesn't expire quickly
        test("entry with default TTL persists", () -> {
            LocalCache<String, String> cache = new LocalCache<>(CacheConfig.of(100, 5000));
            cache.put("key", "value"); // 5000ms default TTL
            sleep(100);
            assertTrue(cache.get("key").isPresent(), "should still exist within TTL");
        });

        // Test 3: size only counts non-expired
        test("size excludes expired entries", () -> {
            LocalCache<String, String> cache = new LocalCache<>(CacheConfig.of(100, 5000));
            cache.put("short", "val", 100);
            cache.put("long", "val", 5000);
            assertEqual(2, cache.size(), "before expiry");
            sleep(150);
            assertEqual(1, cache.size(), "after short entry expires");
        });

        // Test 4: cleanUp removes all expired
        test("cleanUp removes expired entries", () -> {
            LocalCache<String, String> cache = new LocalCache<>(CacheConfig.of(100, 5000));
            cache.put("a", "1", 100);
            cache.put("b", "2", 100);
            cache.put("c", "3", 5000);
            sleep(150);
            cache.cleanUp();
            assertEqual(1, cache.size(), "after cleanUp");
            assertTrue(cache.get("c").isPresent(), "non-expired should survive");
        });

        // Test 5: Overwriting key resets TTL
        test("overwrite resets TTL", () -> {
            LocalCache<String, String> cache = new LocalCache<>(CacheConfig.of(100, 5000));
            cache.put("key", "old", 100);
            sleep(50);
            cache.put("key", "new", 200); // reset TTL
            sleep(80); // 130ms total — old TTL would have expired
            assertTrue(cache.get("key").isPresent(), "should still exist after TTL reset");
            assertEqual("new", cache.get("key").get(), "should have new value");
        });

        // Test 6: Expired entry removed on get
        test("get removes expired entry", () -> {
            LocalCache<String, String> cache = new LocalCache<>(CacheConfig.of(100, 5000));
            cache.put("key", "value", 100);
            sleep(150);
            cache.get("key"); // triggers removal
            // Verify internal cleanup happened by checking size
            assertEqual(0, cache.size(), "size after get on expired");
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
        System.out.println("CP2 Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) System.out.println("ALL TESTS PASSED");
        else System.out.println("SOME TESTS FAILED");
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }
}

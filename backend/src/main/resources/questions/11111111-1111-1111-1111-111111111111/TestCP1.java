import java.util.*;

/**
 * Checkpoint 1: Basic CRUD operations.
 * DO NOT MODIFY THIS FILE.
 */
public class TestCP1 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Test 1: put and get
        test("put and get value", () -> {
            LocalCache<String, Integer> cache = new LocalCache<>(CacheConfig.defaults());
            cache.put("key1", 42);
            Optional<Integer> result = cache.get("key1");
            assertTrue(result.isPresent(), "expected value present");
            assertEqual(42, result.get(), "value");
        });

        // Test 2: get non-existing key
        test("get non-existing returns empty", () -> {
            LocalCache<String, Integer> cache = new LocalCache<>(CacheConfig.defaults());
            Optional<Integer> result = cache.get("nonexistent");
            assertTrue(result.isEmpty(), "expected empty optional");
        });

        // Test 3: size
        test("size tracks entries", () -> {
            LocalCache<String, String> cache = new LocalCache<>(CacheConfig.defaults());
            assertEqual(0, cache.size(), "initial size");
            cache.put("a", "1");
            assertEqual(1, cache.size(), "after first put");
            cache.put("b", "2");
            assertEqual(2, cache.size(), "after second put");
        });

        // Test 4: put overwrites existing key
        test("put overwrites existing key", () -> {
            LocalCache<String, String> cache = new LocalCache<>(CacheConfig.defaults());
            cache.put("key", "old");
            cache.put("key", "new");
            assertEqual("new", cache.get("key").orElse(null), "overwritten value");
            assertEqual(1, cache.size(), "size after overwrite");
        });

        // Test 5: remove
        test("remove existing key returns true", () -> {
            LocalCache<String, Integer> cache = new LocalCache<>(CacheConfig.defaults());
            cache.put("key", 1);
            boolean removed = cache.remove("key");
            assertTrue(removed, "remove should return true");
            assertTrue(cache.get("key").isEmpty(), "get after remove should be empty");
            assertEqual(0, cache.size(), "size after remove");
        });

        // Test 6: remove non-existing
        test("remove non-existing returns false", () -> {
            LocalCache<String, Integer> cache = new LocalCache<>(CacheConfig.defaults());
            boolean removed = cache.remove("nonexistent");
            assertTrue(!removed, "remove should return false");
        });

        // Test 7: clear
        test("clear removes all entries", () -> {
            LocalCache<String, Integer> cache = new LocalCache<>(CacheConfig.defaults());
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("c", 3);
            cache.clear();
            assertEqual(0, cache.size(), "size after clear");
            assertTrue(cache.get("a").isEmpty(), "get after clear should be empty");
        });

        // Test 8: multiple types
        test("works with Integer keys", () -> {
            LocalCache<Integer, String> cache = new LocalCache<>(CacheConfig.defaults());
            cache.put(1, "one");
            cache.put(2, "two");
            assertEqual("one", cache.get(1).orElse(null), "integer key 1");
            assertEqual("two", cache.get(2).orElse(null), "integer key 2");
        });

        printSummary();
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
        System.out.println("CP1 Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) System.out.println("ALL TESTS PASSED");
        else System.out.println("SOME TESTS FAILED");
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }
}

class AssertionError extends RuntimeException {
    public AssertionError(String message) { super(message); }
}

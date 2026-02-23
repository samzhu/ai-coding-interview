import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * Checkpoint 4: Thread safety tests.
 * DO NOT MODIFY THIS FILE.
 * NOTE: Assumes CP1, CP2, and CP3 are implemented.
 */
public class TestCP4 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Test 1: Concurrent puts — no data loss
        test("concurrent puts do not lose data", () -> {
            LocalCache<Integer, String> cache = new LocalCache<>(CacheConfig.of(1000, 60000));
            int threadCount = 10;
            int putsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < putsPerThread; i++) {
                            cache.put(threadId * putsPerThread + i, "val");
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            awaitLatch(latch);
            executor.shutdown();

            assertEqual(threadCount * putsPerThread, cache.size(),
                "total entries after concurrent puts");
        });

        // Test 2: Concurrent puts with maxSize — never exceeds
        test("concurrent puts never exceed maxSize", () -> {
            LocalCache<Integer, String> cache = new LocalCache<>(CacheConfig.of(50, 60000));
            int threadCount = 10;
            int putsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicBoolean exceededMax = new AtomicBoolean(false);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < putsPerThread; i++) {
                            cache.put(threadId * putsPerThread + i, "val");
                            if (cache.size() > 50) {
                                exceededMax.set(true);
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            awaitLatch(latch);
            executor.shutdown();

            assertTrue(!exceededMax.get(), "cache size exceeded maxSize during concurrent puts");
            assertTrue(cache.size() <= 50, "final size exceeds maxSize: " + cache.size());
        });

        // Test 3: Concurrent put + get — no exceptions
        test("concurrent put and get — no exceptions", () -> {
            LocalCache<String, Integer> cache = new LocalCache<>(CacheConfig.of(100, 60000));
            ExecutorService executor = Executors.newFixedThreadPool(4);
            CountDownLatch latch = new CountDownLatch(4);
            AtomicReference<Exception> error = new AtomicReference<>();

            // 2 writers
            for (int w = 0; w < 2; w++) {
                final int writerId = w;
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < 200; i++) {
                            cache.put("key" + writerId + "_" + i, i);
                        }
                    } catch (Exception e) {
                        error.set(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 2 readers
            for (int r = 0; r < 2; r++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < 200; i++) {
                            cache.get("key0_" + i);
                            cache.size();
                        }
                    } catch (Exception e) {
                        error.set(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            awaitLatch(latch);
            executor.shutdown();

            assertNull(error.get(), "concurrent put+get should not throw: " +
                (error.get() != null ? error.get().getClass().getSimpleName() + ": " + error.get().getMessage() : ""));
        });

        // Test 4: Concurrent put + cleanUp — no ConcurrentModificationException
        test("concurrent put and cleanUp — no CME", () -> {
            LocalCache<Integer, String> cache = new LocalCache<>(CacheConfig.of(100, 60000));
            ExecutorService executor = Executors.newFixedThreadPool(4);
            CountDownLatch latch = new CountDownLatch(4);
            AtomicReference<Exception> error = new AtomicReference<>();

            // 2 writers with short TTL
            for (int w = 0; w < 2; w++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < 200; i++) {
                            cache.put(i, "val", 50);
                        }
                    } catch (Exception e) {
                        error.set(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 2 cleanUp workers
            for (int c = 0; c < 2; c++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < 50; i++) {
                            Thread.sleep(10);
                            cache.cleanUp();
                        }
                    } catch (Exception e) {
                        error.set(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            awaitLatch(latch);
            executor.shutdown();

            assertNull(error.get(), "concurrent put+cleanUp should not throw: " +
                (error.get() != null ? error.get().getClass().getSimpleName() + ": " + error.get().getMessage() : ""));
        });

        // Test 5: Verify ReadWriteLock is used (not just synchronized)
        test("uses ReadWriteLock (field check)", () -> {
            // Reflective check for ReadWriteLock field
            boolean hasRWLock = false;
            for (java.lang.reflect.Field field : LocalCache.class.getDeclaredFields()) {
                if (ReadWriteLock.class.isAssignableFrom(field.getType()) ||
                    ReentrantReadWriteLock.class.isAssignableFrom(field.getType())) {
                    hasRWLock = true;
                    break;
                }
            }
            assertTrue(hasRWLock, "LocalCache should have a ReadWriteLock or ReentrantReadWriteLock field");
        });

        printSummary();
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new RuntimeException("Latch timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }
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

    private static void assertNull(Object obj, String message) {
        if (obj != null) throw new AssertionError(message);
    }

    private static void printSummary() {
        System.out.println("\n========================================");
        System.out.println("CP4 Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) System.out.println("ALL TESTS PASSED");
        else System.out.println("SOME TESTS FAILED");
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }
}

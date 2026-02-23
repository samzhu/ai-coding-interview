import java.util.*;

/**
 * Simulated Spring Data repository using HashMap.
 * DO NOT MODIFY THIS FILE.
 */
public class BookRepository {
    private final Map<Long, Book> store = new LinkedHashMap<>();
    private long idSequence = 100;

    public BookRepository() {
        // Pre-loaded test data
        save(new Book(1L, "Clean Code", "Robert Martin", "978-0132350884", 39.99, "AVAILABLE"));
        save(new Book(2L, "Effective Java", "Joshua Bloch", "978-0134685991", 45.00, "AVAILABLE"));
        save(new Book(3L, "Design Patterns", "Gang of Four", "978-0201633610", 54.99, "AVAILABLE"));
        save(new Book(4L, "Refactoring", "Martin Fowler", "978-0134757599", 49.99, "CHECKED_OUT"));
        save(new Book(5L, "The Pragmatic Programmer", "David Thomas", "978-0135957059", 42.50, "AVAILABLE"));
        save(new Book(6L, "Deleted Book", "Nobody", "978-0000000000", 10.00, "DELETED"));
    }

    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Optional<Book> findByIsbn(String isbn) {
        return store.values().stream()
                .filter(b -> b.getIsbn().equals(isbn))
                .findFirst();
    }

    public List<Book> findAll() {
        return new ArrayList<>(store.values());
    }

    public Book save(Book book) {
        store.put(book.getId(), book);
        return book;
    }

    public long nextId() {
        return ++idSequence;
    }
}

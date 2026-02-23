import java.util.*;
import java.util.stream.*;

/**
 * Book management service — Spring Boot @Service style.
 * This is the ONLY file you should edit.
 */
public class BookService {
    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    // ========== CP1: findById — HAS 2 BUGS ==========

    /**
     * Find a book by its string ID.
     * Should throw BookNotFoundException if not found.
     * Should throw IllegalArgumentException if ID is not a valid number.
     */
    public Book findById(String id) {
        // Bug 1: Directly calling .get() without handling Optional.empty()
        // Bug 2: Not converting String to Long properly
        Book book = repository.findById(Long.valueOf(id)).get();
        return book;
    }

    // ========== CP2: createBook — TODO ==========

    /**
     * Create a new book with validation.
     * - Validate ISBN using ValidationHelper.validateIsbn()
     * - Validate price using ValidationHelper.validatePrice()
     * - Check for duplicate ISBN
     * - Auto-generate ID, set status to "AVAILABLE"
     */
    public Book createBook(String title, String author, String isbn, double price) {
        // TODO: Implement this method
        throw new UnsupportedOperationException("createBook not implemented yet");
    }

    // ========== CP3: searchBooks — TODO ==========

    /**
     * Search books with optional filters (null = no filter).
     * Multiple filters combine with AND logic.
     * Results sorted by title (case-insensitive).
     */
    public List<Book> searchBooks(String author, String status, Double minPrice, Double maxPrice) {
        // TODO: Implement this method
        throw new UnsupportedOperationException("searchBooks not implemented yet");
    }
}

// Custom exception classes
class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(String message) {
        super(message);
    }
}

class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

class DuplicateIsbnException extends RuntimeException {
    public DuplicateIsbnException(String isbn) {
        super("Book with ISBN " + isbn + " already exists");
    }
}

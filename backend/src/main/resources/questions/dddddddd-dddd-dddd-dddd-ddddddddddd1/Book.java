/**
 * Book model representing a book in the bookstore.
 * DO NOT MODIFY THIS FILE.
 */
public class Book {
    private final Long id;
    private final String title;
    private final String author;
    private final String isbn;
    private final double price;
    private final String status;

    public Book(Long id, String title, String author, String isbn, double price, String status) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.price = price;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public double getPrice() { return price; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return "Book{id=" + id + ", title='" + title + "', author='" + author +
               "', isbn='" + isbn + "', price=" + price + ", status='" + status + "'}";
    }
}

package com.klu.spring.mvc.order;

public class Order {

    private final Long id;
    private final Long bookId;
    private final String bookTitle;
    private final int quantity;
    private final String username;
    private final String status;

    public Order(Long id, Long bookId, String bookTitle, int quantity, String username, String status) {
        this.id = id;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.quantity = quantity;
        this.username = username;
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public int getQuantity() { return quantity; }
    public String getUsername() { return username; }
    public String getStatus() { return status; }
}

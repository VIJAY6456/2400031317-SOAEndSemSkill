package com.klu.spring.mvc.order;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class OrderRequest {

    @NotNull(message = "bookId is required")
    private Long bookId;

    @Min(value = 1, message = "quantity must be at least 1")
    private int quantity;

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}

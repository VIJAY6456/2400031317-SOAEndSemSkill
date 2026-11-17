package com.klu.spring.mvc.book;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books")
public class BookController {

    private final Map<Long, Book> books = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public BookController() {
        save(new Book(null, "Clean Code", "Robert C. Martin", "9780132350884"));
        save(new Book(null, "Effective Java", "Joshua Bloch", "9780134685991"));
        save(new Book(null, "Refactoring", "Martin Fowler", "9780134757599"));
    }

    private Book save(Book book) {
        long id = sequence.incrementAndGet();
        Book saved = new Book(id, book.getTitle(), book.getAuthor(), book.getIsbn());
        books.put(id, saved);
        return saved;
    }

    /** Any authenticated user. */
    @GetMapping
    public List<Book> getAll() {
        List<Book> list = new ArrayList<>(books.values());
        list.sort(Comparator.comparing(Book::getId));
        return list;
    }

    /** Shows that THIS service validated the forwarded token. */
    @GetMapping("/whoami")
    public Map<String, Object> whoami(Authentication authentication) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("validatedBy", "book-service");
        body.put("username", authentication.getName());
        body.put("authorities", authentication.getAuthorities().toString());
        return body;
    }

    /** Any authenticated user (order-service calls this with the forwarded token). */
    @GetMapping("/{id}")
    public ResponseEntity<Book> getById(@PathVariable Long id) {
        Book book = books.get(id);
        return book == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(book);
    }

    /** ADMIN only (enforced in SecurityConfig). */
    @PostMapping
    public ResponseEntity<Book> create(@Valid @RequestBody Book book) {
        return ResponseEntity.status(HttpStatus.CREATED).body(save(book));
    }
}

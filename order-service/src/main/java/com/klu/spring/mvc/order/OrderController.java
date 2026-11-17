package com.klu.spring.mvc.order;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final RestTemplate restTemplate;
    private final String bookServiceUrl;

    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public OrderController(RestTemplate restTemplate, @Value("${book.service.url}") String bookServiceUrl) {
        this.restTemplate = restTemplate;
        this.bookServiceUrl = bookServiceUrl;
    }

    /** Users see their own orders, ADMIN sees all. */
    @GetMapping
    public List<Order> list(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return orders.values().stream()
                .filter(o -> admin || o.getUsername().equals(authentication.getName()))
                .sorted(Comparator.comparing(Order::getId))
                .collect(Collectors.toList());
    }

    /**
     * Creates an order. The caller's token is FORWARDED to book-service, which validates it again.
     */
    @PostMapping
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ResponseEntity<?> create(@Valid @RequestBody OrderRequest request,
                                    @RequestHeader("Authorization") String authorization,
                                    Authentication authentication) {

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);   // token forwarding

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    bookServiceUrl + "/books/" + request.getBookId(),
                    HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            Object title = response.getBody() == null ? null : response.getBody().get("title");

            Order order = new Order(sequence.incrementAndGet(), request.getBookId(),
                    String.valueOf(title), request.getQuantity(), authentication.getName(), "CREATED");
            orders.put(order.getId(), order);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);

        } catch (HttpStatusCodeException e) {
            String message = e.getStatusCode() == HttpStatus.NOT_FOUND
                    ? "Book " + request.getBookId() + " not found"
                    : "book-service rejected the forwarded token or request";
            return ResponseEntity.status(e.getStatusCode()).body(error(e.getStatusCode().value(), message));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(error(503, "book-service is not reachable"));
        }
    }

    private Map<String, Object> error(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("service", "order-service");
        body.put("message", message);
        return body;
    }
}

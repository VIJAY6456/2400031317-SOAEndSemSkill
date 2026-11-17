# Microservices + API Gateway + JWT token forwarding (com.klu.spring.mvc)

```
client --> gateway :9090 --validates JWT--> auth-service  :9091  (public login)
                                        --> book-service  :9092  (validates JWT again)
                                        --> order-service :9093  (validates JWT, forwards it to book-service)
```

| Module        | Port | Role |
|---------------|------|------|
| gateway       | 9090 | Single entry point. Validates the Bearer token, returns 401 for bad tokens, forwards good ones. |
| auth-service  | 9091 | POST /auth/login (issue JWT), POST /auth/validate (check a token). Public. |
| book-service  | 9092 | GET /books, GET /books/{id}, GET /books/whoami (any user); POST /books (ADMIN). Validates the forwarded token. |
| order-service | 9093 | POST /orders, GET /orders. Validates the token, then forwards it to book-service. |
| jwt-common    | -    | Shared JwtUtil (create/validate) and JwtAuthFilter. |

Demo users: `admin / admin123` (ADMIN), `user / user123` (USER).
Everything is called through the gateway on port 9090.

## Run in Spring Tool Suite
1. File > Import > Maven > Existing Maven Projects > select the `microservices-jwt-gateway` folder > Finish (6 projects appear).
2. If anything is red: right-click the root project > Maven > Update Project (Force Update), or run once
   `Run As > Maven build...` on `microservices-parent` with goals `clean install -DskipTests`.
3. Start each app: right-click its `...Application.java` > Run As > Spring Boot App
   (AuthServiceApplication, BookServiceApplication, OrderServiceApplication, GatewayApplication).
   Wait for "Started ..." in each Console.

## Test
Easiest: `powershell -ExecutionPolicy Bypass -File .\test-flow.ps1` (prints 10 checks with status codes).

By hand (Postman or curl):
1. POST http://localhost:9090/auth/login  body `{"username":"user","password":"user123"}` -> copy `token`.
2. GET  http://localhost:9090/books  -> 401 without a token, 200 with header `Authorization: Bearer <token>`.
3. GET  http://localhost:9090/books/whoami -> shows `validatedBy: book-service` (forwarded token validated again).
4. POST http://localhost:9090/orders  body `{"bookId":1,"quantity":2}` -> 201 (order-service forwarded your token to book-service).
5. POST http://localhost:9090/books as `user` -> 403; as `admin` -> 201.
6. Change one character of the token -> 401 "Invalid token signature".

Where the code is:
- Gateway validation: `gateway/.../JwtGatewayFilter.java`
- Service validation of the forwarded token: `jwt-common/.../JwtAuthFilter.java` + each service's `SecurityConfig`
- Service-to-service token forwarding: `order-service/.../OrderController.java` (`headers.set(AUTHORIZATION, authorization)`)

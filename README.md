# 🛒 Tema Cart — E-Commerce Order Management System

A **REST API backend** for an e-commerce platform built with **Spring Boot 3.3** and **Java 21**. It handles user authentication with JWT, product catalog management, shopping cart operations, and order placement with transactional integrity.

---

## 🚀 Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.3.4 |
| **Web Layer** | Spring MVC, RESTful APIs |
| **Security** | Spring Security 6, JWT (jjwt 0.12.6), BCrypt password hashing |
| **Persistence** | Spring Data JPA, Hibernate 6.5.3 |
| **Database** | MySQL 8 |
| **Validation** | Jakarta Bean Validation |
| **Build Tool** | Maven |
| **Java Features Used** | OOP, Collections, Streams, Exception Handling, Enums |
| **IDE / Tools** | IntelliJ IDEA, Postman |

---

## ✨ Features

### 🔐 Authentication & Authorization
- User registration and login with JWT tokens
- Password hashing with BCrypt
- Stateless authentication (no server-side sessions)
- Role-based access control: `ADMIN` and `CUSTOMER`
- Custom `JwtAuthFilter` that validates tokens on every request

### 📦 Category Management
- Create, read, update, delete categories
- Admin-only write operations
- Delete protection: categories with products cannot be deleted

### 🛍️ Product Management
- Full CRUD for products with category relationship
- Product filtering by category
- Case-insensitive name search
- Soft delete (products marked `active = false` instead of removed)
- Stock quantity tracking
- Price stored as `BigDecimal` (accurate money handling)

### 🛒 Shopping Cart
- Per-user cart (auto-created on first access)
- Add to cart with **upsert logic** — adding the same product increases quantity
- Update item quantity, remove individual items, clear entire cart
- Stock validation before adding
- Server-computed subtotals and total amount
- Price snapshot: cart remembers unit price even if product price changes later

### 📋 Order Management
- Place order directly from cart with shipping details
- **Transactional order placement** — validates stock, deducts inventory, creates order, clears cart atomically
- Human-readable order numbers: `TC-YYYYMMDD-####`
- Order item snapshots (product name + unit price at time of order)
- Order status lifecycle: `PENDING → CONFIRMED → SHIPPED → DELIVERED` (or `CANCELLED`)
- **Stock restoration** when an order is cancelled
- Order history for customers
- Admin can update order status with business rules (can't un-deliver or un-cancel)

### 🎛️ Admin Dashboard
- View all orders (optionally filter by status)
- Order statistics: counts by status, total revenue, delivered revenue
- View all registered users (without exposing passwords)

### 🛡️ Error Handling
- Global exception handler (`@RestControllerAdvice`)
- Proper HTTP status codes: 400, 401, 403, 404, 500
- Field-level validation errors with clear messages
- Consistent JSON error response structure

---

## 🔌 API Endpoints

### Authentication
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/auth/register` | Public | Register new user |
| POST | `/api/auth/login` | Public | Login and receive JWT |

### Categories
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/categories` | Authenticated | List all categories |
| GET | `/api/categories/{id}` | Authenticated | Get category by ID |
| POST | `/api/admin/categories` | ADMIN | Create category |
| PUT | `/api/admin/categories/{id}` | ADMIN | Update category |
| DELETE | `/api/admin/categories/{id}` | ADMIN | Delete (fails if products exist) |

### Products
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/products` | Authenticated | List active products |
| GET | `/api/products/{id}` | Authenticated | Get product by ID |
| GET | `/api/products/category/{categoryId}` | Authenticated | Filter by category |
| GET | `/api/products/search?keyword=...` | Authenticated | Search by name |
| POST | `/api/admin/products` | ADMIN | Create product |
| PUT | `/api/admin/products/{id}` | ADMIN | Update product |
| DELETE | `/api/admin/products/{id}` | ADMIN | Soft delete product |

### Cart
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/cart` | Authenticated | View my cart |
| POST | `/api/cart/items` | Authenticated | Add product to cart |
| PUT | `/api/cart/items/{itemId}` | Authenticated | Update item quantity |
| DELETE | `/api/cart/items/{itemId}` | Authenticated | Remove item |
| DELETE | `/api/cart` | Authenticated | Clear cart |

### Orders
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/orders` | Authenticated | Place order from cart |
| GET | `/api/orders` | Authenticated | My order history |
| GET | `/api/orders/{id}` | Authenticated | Order details (owner or admin) |

### Admin
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/admin/orders` | ADMIN | All orders (optional `?status=`) |
| GET | `/api/admin/orders/stats` | ADMIN | Order statistics & revenue |
| PUT | `/api/admin/orders/{id}/status` | ADMIN | Update order status |
| GET | `/api/admin/users` | ADMIN | List all users |

---

## 🗄️ Database Schema

```
users ──┬── user_roles
        │
        ├── carts ──── cart_items ──── products ──── categories
        │
        └── orders ──── order_items ──── products
```

**Tables created:**
- `users` — user accounts
- `user_roles` — roles per user (ADMIN, CUSTOMER)
- `categories` — product categories
- `products` — products with category reference
- `carts` — one cart per user
- `cart_items` — items in a cart with quantity and price snapshot
- `orders` — placed orders with shipping info and status
- `order_items` — order line items with snapshotted name and price

---

## 🏗️ Project Structure

```
com.temacart
├── config/          → SecurityConfig
├── controller/      → REST controllers
├── dto/
│   ├── request/     → incoming request bodies
│   └── response/    → outgoing response shapes
├── entity/          → JPA entities and enums
├── exception/       → custom exceptions + GlobalExceptionHandler
├── repository/      → Spring Data JPA repositories
├── security/        → JwtService, JwtAuthFilter, CustomUserDetailsService
├── service/         → business logic
└── TemaCartApplication.java
```

**Layered architecture:** Controller → Service → Repository → Entity → MySQL

---

## 🚦 Getting Started

### Prerequisites
- **Java 21** (JDK)
- **MySQL 8** running locally
- **Maven** (or use IntelliJ's bundled Maven)

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/<your-username>/tema-cart.git
   cd tema-cart
   ```

2. **Create the MySQL database**
   ```sql
   CREATE DATABASE tema_cart_db;
   ```

3. **Configure `src/main/resources/application.properties`**
   ```properties
   spring.application.name=tema-cart

   # MySQL
   spring.datasource.url=jdbc:mysql://localhost:3306/tema_cart_db
   spring.datasource.username=<your-db-username>
   spring.datasource.password=<your-db-password>

   # JPA
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true
   spring.jpa.properties.hibernate.format_sql=true
   spring.jpa.open-in-view=false

   # JWT
   jwt.secret=<a-secret-key-at-least-32-characters-long>
   jwt.expiration=86400000
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```
   Or run `TemaCartApplication.java` from your IDE.

5. **API is available at** `http://localhost:8080`

---

## 🧪 Testing with Postman

**Register a user:**
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "fullName": "John Doe",
  "phone": "9876543210"
}
```

**Login to get a JWT:**
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Use the token on protected endpoints:**
```http
GET http://localhost:8080/api/products
Authorization: Bearer <paste-jwt-here>
```

**Creating an ADMIN user:**

New users are registered as `CUSTOMER`. To promote a user to admin:

```sql
USE tema_cart_db;

DELETE FROM user_roles WHERE user_id = 1;
INSERT INTO user_roles (user_id, role) VALUES (1, 'ADMIN');
```

Then login again to receive a token with `ROLE_ADMIN`.

---

## 🎯 Key Design Decisions

### 1. JWT over Sessions
Stateless authentication suits modern REST APIs and mobile clients. No server-side session storage is required — every request carries its own token.

### 2. DTO Pattern
Entities are never exposed through the API. Request/Response DTOs:
- Keep the API contract stable even if internal models change
- Prevent leaking sensitive fields (e.g., password hashes)
- Allow different shapes for input and output

### 3. `JOIN FETCH` to Prevent Lazy Loading Issues
Lazy `@ManyToOne` relationships threw `LazyInitializationException` when accessed outside a Hibernate session. Solved by using JPQL `JOIN FETCH` queries in the repository. This also avoids the N+1 query problem.

### 4. Soft Delete for Products
Products are marked `active = false` instead of being deleted. Order history references products, so physically deleting them would break historical data.

### 5. Snapshot Pattern in Orders
Order items store the product name and unit price at the time of order. If the product is later renamed or repriced, past orders remain accurate — essential for invoices.

### 6. Transactional Order Placement
Placing an order performs multiple steps (validate stock → deduct inventory → save order → clear cart). The `placeOrder` method is `@Transactional`, ensuring atomicity — any failure rolls back the entire operation.

### 7. Cascade Protection on Category Deletion
Categories with products cannot be deleted — the service validates this and returns a clear error. Admins must reassign or remove products first, preventing accidental mass data loss.

### 8. Role-Based Access Control
Security is enforced at the filter chain level:
- `/api/auth/**` → public
- `/api/admin/**` → requires `ROLE_ADMIN`
- Everything else → any authenticated user

---

## 📚 What I Learned Building This

- Implementing JWT authentication end-to-end with Spring Security 6
- Understanding the difference between `AuthenticationProvider` and `UserDetailsService`
- Handling JPA lazy loading and N+1 problems with `JOIN FETCH`
- Designing RESTful APIs with proper HTTP status codes
- Writing transactional business logic with rollback safety
- Using the DTO pattern to decouple API contracts from entities
- Global exception handling with `@RestControllerAdvice`
- Snapshot patterns for preserving historical data integrity

---

## 📝 License

This project is for educational and portfolio purposes.

---

## 👤 Author

**Atul Sandilya**
- GitHub: https://github.com/Atulsandilya5
- Email: atulsandilya25@gmail.com

---

## 🙏 Acknowledgments

Built as a self-study project to demonstrate production-style Spring Boot backend development.
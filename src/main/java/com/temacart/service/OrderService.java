package com.temacart.service;

import com.temacart.dto.request.PlaceOrderRequest;
import com.temacart.dto.request.UpdateOrderStatusRequest;
import com.temacart.dto.response.OrderItemResponse;
import com.temacart.dto.response.OrderResponse;
import com.temacart.dto.response.OrderStatsResponse;
import com.temacart.entity.*;
import com.temacart.exception.ResourceNotFoundException;
import com.temacart.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        CartItemRepository cartItemRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.cartItemRepository = cartItemRepository;
    }

    // ========== PLACE ORDER ==========
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        User user = getCurrentUser();

        Cart cart = cartRepository.findByUserIdWithItemsAndProducts(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot place order with empty cart");
        }

        // Step 1 — Validate stock for all items
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (!product.getActive()) {
                throw new RuntimeException("Product not available: " + product.getName());
            }
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for: " + product.getName()
                        + " (available: " + product.getStockQuantity() + ")");
            }
        }

        // Step 2 — Build Order
        String orderNumber = generateOrderNumber();
        BigDecimal totalAmount = cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order(
                orderNumber,
                user,
                totalAmount,
                request.getShippingName(),
                request.getShippingPhone(),
                request.getShippingAddress()
        );
        order.setStatus(OrderStatus.PENDING);

        // Step 3 — Convert CartItems to OrderItems + deduct stock
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            BigDecimal subtotal = cartItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = new OrderItem(
                    order,
                    product,
                    cartItem.getQuantity(),
                    cartItem.getUnitPrice(),
                    subtotal,
                    product.getName()      // snapshot
            );
            orderItems.add(orderItem);

            // Deduct stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }
        order.setItems(orderItems);

        // Step 4 — Save order
        Order savedOrder = orderRepository.save(order);

        // Step 5 — Clear cart
        cart.getItems().clear();
        cartRepository.save(cart);

        return toResponse(savedOrder);
    }

    // ========== GET MY ORDERS ==========
    public List<OrderResponse> getMyOrders() {
        User user = getCurrentUser();
        return orderRepository.findByUserIdWithItems(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ========== GET ORDER BY ID ==========
    public OrderResponse getOrderById(Long id) {
        User user = getCurrentUser();
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        // Ownership check — only owner or ADMIN can view
        boolean isAdmin = user.getRoles().contains(Role.ADMIN);
        if (!isAdmin && !order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to view this order");
        }
        return toResponse(order);
    }

    // ========== ADMIN: GET ALL ORDERS ==========
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    // ========== ADMIN: GET ORDERS BY STATUS ==========
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findAllOrderByCreatedAtDesc()
                .stream()
                .filter(o -> o.getStatus() == status)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ========== ADMIN: ORDER STATS ==========
    public OrderStatsResponse getOrderStats() {
        List<Order> allOrders = orderRepository.findAll();

        long total = allOrders.size();
        long pending = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long delivered = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
        long cancelled = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();

        BigDecimal totalRevenue = allOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal deliveredRevenue = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderStatsResponse(total, pending, delivered, cancelled, totalRevenue, deliveredRevenue);
    }

    // ========== ADMIN: UPDATE STATUS ==========
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (order.getStatus() == OrderStatus.DELIVERED && request.getStatus() != OrderStatus.DELIVERED) {
            throw new RuntimeException("Cannot change status of a delivered order");
        }
        if (order.getStatus() == OrderStatus.CANCELLED && request.getStatus() != OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot change status of a cancelled order");
        }

        order.setStatus(request.getStatus());

        // Bonus: if cancelled, restore stock
        if (request.getStatus() == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }

        return toResponse(orderRepository.save(order));
    }

    // ========== HELPERS ==========

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private String generateOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "TC-" + date + "-";

        // Simple approach: count today's orders + 1
        // For a real system, you'd use a DB sequence
        long count = orderRepository.count() + 1;
        String candidate = prefix + String.format("%04d", count);

        // Ensure uniqueness (retry if collision)
        int attempts = 1;
        while (orderRepository.existsByOrderNumber(candidate)) {
            count++;
            candidate = prefix + String.format("%04d", count);
            attempts++;
            if (attempts > 100) throw new RuntimeException("Could not generate unique order number");
        }
        return candidate;
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .collect(Collectors.toList());

        int totalItems = itemResponses.stream()
                .mapToInt(OrderItemResponse::getQuantity)
                .sum();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUser().getId(),
                order.getUser().getEmail(),
                itemResponses,
                totalItems,
                order.getTotalAmount(),
                order.getStatus(),
                order.getShippingName(),
                order.getShippingPhone(),
                order.getShippingAddress(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
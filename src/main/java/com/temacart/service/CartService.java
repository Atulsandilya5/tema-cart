package com.temacart.service;

import com.temacart.dto.request.AddToCartRequest;
import com.temacart.dto.request.UpdateCartItemRequest;
import com.temacart.dto.response.CartItemResponse;
import com.temacart.dto.response.CartResponse;
import com.temacart.entity.Cart;
import com.temacart.entity.CartItem;
import com.temacart.entity.Product;
import com.temacart.entity.User;
import com.temacart.exception.ResourceNotFoundException;
import com.temacart.repository.CartItemRepository;
import com.temacart.repository.CartRepository;
import com.temacart.repository.ProductRepository;
import com.temacart.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    // ---------- GET MY CART ----------
    public CartResponse getMyCart() {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        return toResponse(cart);
    }

    // ---------- ADD TO CART ----------
    public CartResponse addToCart(AddToCartRequest request) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        if (!product.getActive()) {
            throw new RuntimeException("Product is not available: " + product.getName());
        }

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock. Available: " + product.getStockQuantity());
        }

        // Upsert logic: if product already in cart, increase quantity
        CartItem existing = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            cartItemRepository.save(existing);
        } else {
            CartItem newItem = new CartItem(cart, product, request.getQuantity(), product.getPrice());
            cartItemRepository.save(newItem);
        }

        // Reload cart with fresh items
        Cart refreshed = cartRepository.findByUserIdWithItemsAndProducts(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart reload failed"));
        return toResponse(refreshed);
    }

    // ---------- UPDATE ITEM QUANTITY ----------
    public CartResponse updateItemQuantity(Long itemId, UpdateCartItemRequest request) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        // Security check: does this item belong to the current user's cart?
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("This item does not belong to your cart");
        }

        if (item.getProduct().getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock. Available: " + item.getProduct().getStockQuantity());
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        Cart refreshed = cartRepository.findByUserIdWithItemsAndProducts(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart reload failed"));
        return toResponse(refreshed);
    }

    // ---------- REMOVE ITEM ----------
    public CartResponse removeItem(Long itemId) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("This item does not belong to your cart");
        }

        cartItemRepository.delete(item);

        Cart refreshed = cartRepository.findByUserIdWithItemsAndProducts(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart reload failed"));
        return toResponse(refreshed);
    }

    // ---------- CLEAR CART ----------
    public void clearCart() {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    // ---------- HELPERS ----------

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(new Cart(user)));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> {
                    BigDecimal subtotal = item.getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));
                    return new CartItemResponse(
                            item.getId(),
                            item.getProduct().getId(),
                            item.getProduct().getName(),
                            item.getProduct().getImageUrl(),
                            item.getUnitPrice(),
                            item.getQuantity(),
                            subtotal
                    );
                })
                .collect(Collectors.toList());

        int totalItems = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), itemResponses, totalItems, totalAmount);
    }
}
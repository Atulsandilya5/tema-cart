package com.temacart.repository;

import com.temacart.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // Basic CRUD inherited from JpaRepository — enough for now
}
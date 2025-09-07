package com.example.gsm.controller;

import com.example.gsm.entity.Order;
import com.example.gsm.entity.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
@RestController
@RequestMapping("/api/orders")
public class Test {
    @Autowired
    private OrderRepository orderRepository;

    // API lấy 1 record theo id
    @GetMapping("/{id}")
    public Optional<Order> getOrderById(@PathVariable String id) {
        return orderRepository.findById(id);
    }
}

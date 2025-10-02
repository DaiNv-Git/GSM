package com.example.gsm.dao.response;

import com.example.gsm.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter 
@Setter 
@AllArgsConstructor
public class OrderPageResponse {
    private long totalElements;
    private int totalPages;
    private Map<String, List<Order>> groupedOrders;
}

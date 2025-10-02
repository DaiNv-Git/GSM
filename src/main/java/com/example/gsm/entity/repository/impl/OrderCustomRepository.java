package com.example.gsm.entity.repository.impl;

import com.example.gsm.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderCustomRepository {

    private final MongoTemplate mongoTemplate;
    
    public Page<Order> findActiveOrders(Long accountId, Date now, String phoneNumber, String type, Pageable pageable) {
        Criteria criteria = Criteria.where("accountId").is(accountId)
                .and("stock.expiredAt").gt(now);

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            criteria.and("stock.phone").regex(phoneNumber, "i");
        }

        if (type != null && !type.isBlank()) {
            criteria.and("type").is(type);
        }

        Query query = new Query(criteria).with(pageable);

        List<Order> orders = mongoTemplate.find(query, Order.class);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Order.class);

        return new PageImpl<>(orders, pageable, total);
    }
}

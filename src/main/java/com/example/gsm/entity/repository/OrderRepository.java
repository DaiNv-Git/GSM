package com.example.gsm.entity.repository;

import com.example.gsm.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    @Query(value = "{ 'stock': { $elemMatch: { 'phone': ?0, 'serviceCode': { $in: ?1 }, 'expiredAt': { $gt: ?2 } } } }", count = true)
    long countByPhoneAndServiceCodesAndExpiredAtAfter(String phone, List<String> serviceCodes, Date now);
    Page<Order> findByAccountId(Long accountId, Pageable pageable);

    Page<Order> findByAccountIdAndStatusCode(Long accountId, String statusCode, Pageable pageable);
    @Query(value = "{ 'accountId': ?0, 'statusCode': 'SUCCESS', 'stock.expiredAt': { $gt: ?1 } }")
    Page<Order> findActiveOrders(Long accountId, Date now, Pageable pageable);
    
}


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
    @Query(value = "{ 'stock': { $elemMatch: { 'phone': ?0, 'serviceCode': { $in: ?1 } } } }", count = true)
    long countByPhoneAndServiceCodes(String phone, List<String> serviceCodes);

    @Query(value = "{ 'accountId': ?0, 'stock.expiredAt': { $gt: ?1 } }")
    Page<Order> findActiveOrders(Long accountId, Date now, Pageable pageable);

    @Query(value = "{ 'accountId': ?0, 'stock.expiredAt': { $gt: ?1 }, 'phoneNumber': { $regex: ?2, $options: 'i' } }")
    Page<Order> findActiveOrdersByPhone(Long accountId, Date now, String phoneNumber, Pageable pageable);

    @Query("{ 'stock': { $elemMatch: { 'phone': ?0, 'serviceCode': { $in: ?1 } } } }")
    List<Order> findByPhoneAndServiceCodes(String phone, List<String> serviceCodes);
}


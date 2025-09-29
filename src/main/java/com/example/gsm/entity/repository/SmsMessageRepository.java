package com.example.gsm.entity.repository;
import com.example.gsm.entity.SmsMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface SmsMessageRepository extends MongoRepository<SmsMessage, String> {
    List<SmsMessage> findByOrderId(String orderId);
}

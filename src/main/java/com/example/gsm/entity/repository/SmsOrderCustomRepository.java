package com.example.gsm.entity.repository;


import com.example.gsm.dao.response.SmsOrderDTO;
import com.example.gsm.entity.SmsMessage;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SmsOrderCustomRepository {

    private final MongoTemplate mongoTemplate;

    public Page<SmsOrderDTO> searchByCustomerAndDate(
            Long customerId,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        MatchOperation matchSms = Aggregation.match(Criteria.where("type").is("INBOX")
                .and("timestamp").gte(from).lte(to));

        LookupOperation lookupOrder = Aggregation.lookup(
                "orders",       // collection orders
                "orderId",      // field trong sms_messages
                "id",           // field trong orders
                "order"         // alias
        );

        UnwindOperation unwind = Aggregation.unwind("order");

        MatchOperation matchOrder = Aggregation.match(Criteria.where("order.type").is("buy.otp.service")
                .and("order.accountId").is(customerId));

        ProjectionOperation project = Aggregation.project()
                .and("simPhone").as("phone")
                .and("durationMinutes").as("durationMinutes")
                .and("timestamp").as("timestamp")
                .and("content").as("content")
                .and("serviceCode").as("serviceCode")
                // Regex để tách OTP từ content: chuỗi 6 số
                .andExpression("{ $regexFind: { input: \"$content\", regex: /[0-9]{4,8}/ } }")
                .as("otpCode");

        Aggregation aggregation = Aggregation.newAggregation(
                matchSms,
                lookupOrder,
                unwind,
                matchOrder,
                project,
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "timestamp")),
                Aggregation.skip(pageable.getOffset()),
                Aggregation.limit(pageable.getPageSize())
        );

        List<SmsOrderDTO> results = mongoTemplate.aggregate(aggregation, SmsMessage.class, SmsOrderDTO.class)
                .getMappedResults();

        // Count query (không skip/limit)
        Aggregation countAgg = Aggregation.newAggregation(
                matchSms,
                lookupOrder,
                unwind,
                matchOrder,
                Aggregation.count().as("total")
        );
        Long total = mongoTemplate.aggregate(countAgg, SmsMessage.class, CountResult.class)
                .getUniqueMappedResult() != null
                ? mongoTemplate.aggregate(countAgg, SmsMessage.class, CountResult.class)
                .getUniqueMappedResult().getTotal()
                : 0L;

        return new PageImpl<>(results, pageable, total);
    }

    @Data
    private static class CountResult {
        private long total;
    }
}

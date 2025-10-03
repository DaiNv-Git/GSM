package com.example.gsm.entity.repository.impl;

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
            String type,
            Pageable pageable
    ) {
        // default type = buy.otp.service
        if (type == null || type.isBlank()) {
            type = "buy.otp.service";
        }

        // Lọc SMS INBOX theo thời gian + accountId + serviceType
        MatchOperation matchSms = Aggregation.match(
                Criteria.where("type").is("INBOX")
                        .and("timestamp").gte(from).lte(to)
                        .and("accountId").is(customerId)
                        .and("serviceType").is(type)   // dùng serviceType thay vì order.type
        );

        // Join với orders theo orderId
        LookupOperation lookupByOrderId = LookupOperation.newLookup()
                .from("orders")
                .localField("orderId")
                .foreignField("id")
                .as("orderById");

        // Join với orders theo phone
        LookupOperation lookupByPhone = LookupOperation.newLookup()
                .from("orders")
                .localField("simPhone")
                .foreignField("stock.phone")
                .as("orderByPhone");

        // Projection: chọn orderById nếu có, nếu null thì fallback sang orderByPhone
        ProjectionOperation project = Aggregation.project()
                .and("simPhone").as("phone")
                .and("durationMinutes").as("durationMinutes")
                .and("timestamp").as("timestamp")
                .and("content").as("content")
                .and("serviceCode").as("serviceCode")
                // extract OTP code
                .and(RegexFindAggregationExpression.regexFind("content", "[0-9]{4,8}"))
                .as("otpCodeObj")
                // ưu tiên orderById, nếu null thì dùng orderByPhone
                .and(
                        ConditionalOperators.ifNull("orderById")
                                .thenValueOf("orderByPhone")
                ).as("order");

        // Projection lần 2 để lấy otpCode
        ProjectionOperation projectOtp = Aggregation.project()
                .and("phone").as("phone")
                .and("durationMinutes").as("durationMinutes")
                .and("timestamp").as("timestamp")
                .and("content").as("content")
                .and("serviceCode").as("serviceCode")
                .and("otpCodeObj.match").as("otpCode")
                .and("order").as("order");

        // Aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(
                matchSms,
                lookupByOrderId,
                lookupByPhone,
                project,
                projectOtp,
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "timestamp")),
                Aggregation.skip(pageable.getOffset()),
                Aggregation.limit(pageable.getPageSize())
        );

        List<SmsOrderDTO> results = mongoTemplate
                .aggregate(aggregation, SmsMessage.class, SmsOrderDTO.class)
                .getMappedResults();

        // Count query (không skip/limit)
        Aggregation countAgg = Aggregation.newAggregation(
                matchSms,
                lookupByOrderId,
                lookupByPhone,
                project,
                Aggregation.count().as("total")
        );

        long total = 0L;
        CountResult countResult = mongoTemplate
                .aggregate(countAgg, SmsMessage.class, CountResult.class)
                .getUniqueMappedResult();
        if (countResult != null) {
            total = countResult.getTotal();
        }

        return new PageImpl<>(results, pageable, total);
    }

    @Data
    private static class CountResult {
        private long total;
    }
}

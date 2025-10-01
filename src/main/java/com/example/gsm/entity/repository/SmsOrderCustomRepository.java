package com.example.gsm.entity.repository;

import com.example.gsm.dao.response.SmsOrderDTO;
import com.example.gsm.entity.SmsMessage;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
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
        // Lọc SMS INBOX theo thời gian
        MatchOperation matchSms = Aggregation.match(
                Criteria.where("type").is("INBOX")
                        .and("timestamp").gte(from).lte(to)
        );

        // Join với collection orders
        LookupOperation lookupOrder = Aggregation.lookup(
                "orders",       // collection orders
                "orderId",      // field trong sms_messages
                "id",           // field trong orders
                "order"         // alias
        );

        UnwindOperation unwind = Aggregation.unwind("order");

        // Lọc order theo type & accountId
        MatchOperation matchOrder = Aggregation.match(
                Criteria.where("order.type").is("buy.otp.service")
                        .and("order.accountId").is(customerId)
        );

        // Projection, dùng native Document để nhúng $regexFind
        ProjectionOperation project = Aggregation.project()
                .and("simPhone").as("phone")
                .and("durationMinutes").as("durationMinutes")
                .and("timestamp").as("timestamp")
                .and("content").as("content")
                .and("serviceCode").as("serviceCode")
                // Trích OTP bằng regex
                .and(context -> new Document("$regexFind",
                        new Document("input", "$content")
                                .append("regex", "[0-9]{4,8}")
                ))
                .as("otpCodeObj");

        // Thêm 1 projection để chỉ lấy phần match (otpCodeObj.match)
        ProjectionOperation projectOtp = Aggregation.project()
                .and("phone").as("phone")
                .and("durationMinutes").as("durationMinutes")
                .and("timestamp").as("timestamp")
                .and("content").as("content")
                .and("serviceCode").as("serviceCode")
                .and("otpCodeObj.match").as("otpCode");

        Aggregation aggregation = Aggregation.newAggregation(
                matchSms,
                lookupOrder,
                unwind,
                matchOrder,
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
                lookupOrder,
                unwind,
                matchOrder,
                Aggregation.count().as("total")
        );

        Long total = 0L;
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

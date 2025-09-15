package com.example.gsm.repositories.impl;

import com.example.gsm.dao.*;
import com.example.gsm.repositories.SmsService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.gsm.comon.Constants.*;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;

@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {
    private final MongoTemplate mongo;
    private static final String SMS_TYPE = "buy.sms.service";
    private static final List<String> LABELS = Arrays.asList(
            "1 day", "3 days", "1 week", "3 weeks", "1 month", "3 months"
    );
    @Override
    public ResponseCommon<SmsResponse> getSms(RentRequest req) {
        // Validate request
        String error = validateRequest(req);
        if (!error.isEmpty()) {
            return new ResponseCommon<>(CORE_ERROR_CODE, error, null);
        }

        // Resolve time range
        PeriodRange range = resolveRange(req);

        // 1) Revenue Metrics
        SmsResponse.RevenueMetrics revenueMetrics = aggregateRevenueMetrics(range,req);

        // 2) Line Chart: Total revenue by serviceCode
        List<SmsResponse.TimeSeriesItem> lineChartData = aggregateLineChart(range, req);

        // Build response
        SmsResponse response = new SmsResponse(revenueMetrics, lineChartData);
        return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, response);
    }
    private SmsResponse.RevenueMetrics aggregateRevenueMetrics(PeriodRange range, RentRequest req) {
        Criteria c = buildCriteria(range, req);

        Aggregation agg = newAggregation(
                match(c),
                project("cost", "discountRate"),
                group()
                        .sum("cost").as("totalRevenue")
                        .sum(
                                ArithmeticOperators.valueOf("cost")
                                        .multiplyBy(
                                                ArithmeticOperators.valueOf("discountRate").divideBy(100.0)
                                        )
                        ).as("commission")
        );

        Document result = mongo.aggregate(agg, "orders", Document.class).getUniqueMappedResult();

        double totalRevenue = round2(result != null && result.get("totalRevenue") != null
                ? ((Number) result.get("totalRevenue")).doubleValue() : 0.0);
        double commission = round2(result != null && result.get("commission") != null
                ? ((Number) result.get("commission")).doubleValue() : 0.0);

        double netRevenue = round2(totalRevenue - commission);

        return new SmsResponse.RevenueMetrics(totalRevenue, netRevenue, commission);
    }
    private List<SmsResponse.TimeSeriesItem> aggregateLineChart(PeriodRange range, RentRequest req) {
        Criteria c = buildCriteria(range, req);

        Aggregation agg = newAggregation(
                match(c),
                project("createdAt", "statusCode")
                        .andExpression(bucketExpr(range)).as("bucket"),
                group("bucket", "statusCode").count().as("count"),
                project("count").and("_id.bucket").as("timeLabel").and("_id.statusCode").as("statusCode"),
                sort(Sort.Direction.ASC, "timeLabel")
        );

        List<Document> rows = mongo.aggregate(agg, "orders", Document.class).getMappedResults();
        Map<Integer, Map<String, Long>> bucketStatusMap = rows.stream().collect(Collectors.groupingBy(
                d -> {
                    Object label = d.get("timeLabel");
                    return label instanceof Number ? ((Number) label).intValue() : 0;
                },
                Collectors.toMap(
                        d -> d.getString("statusCode"),
                        d -> {
                            Object count = d.get("count");
                            return count instanceof Number ? ((Number) count).longValue() : 0L;
                        },
                        (v1, v2) -> v1,
                        HashMap::new
                )
        ));

        List<SmsResponse.TimeSeriesItem> lineChartData = new ArrayList<>();

        if (req.getTimeType() == TimeType.WEEK) {
            // 7 ngày trong tuần
            for (int d = 1; d <= 7; d++) {
                Map<String, Long> statusCounts = bucketStatusMap.getOrDefault(d, new HashMap<>());
                long success = statusCounts.getOrDefault("SUCCESS", 0L);
                long refund = statusCounts.getOrDefault("REFUNDED", 0L);
                long total = success + refund + statusCounts.getOrDefault("FAIL", 0L);
                lineChartData.add(new SmsResponse.TimeSeriesItem("D" + d, success, refund, total));
            }
        } else if (req.getTimeType() == TimeType.MONTH) {
            // 4 tuần trong tháng
            for (int w = 1; w <= 4; w++) {
                Map<String, Long> statusCounts = bucketStatusMap.getOrDefault(w, new HashMap<>());
                long success = statusCounts.getOrDefault("SUCCESS", 0L);
                long refund = statusCounts.getOrDefault("REFUNDED", 0L);
                long total = success + refund + statusCounts.getOrDefault("FAIL", 0L);
                lineChartData.add(new SmsResponse.TimeSeriesItem("W" + w, success, refund, total));
            }
        } else if (req.getTimeType() == TimeType.YEAR) {
            // 12 tháng trong năm
            for (int m = 1; m <= 12; m++) {
                Map<String, Long> statusCounts = bucketStatusMap.getOrDefault(m, new HashMap<>());
                long success = statusCounts.getOrDefault("SUCCESS", 0L);
                long refund = statusCounts.getOrDefault("REFUNDED", 0L);
                long total = success + refund + statusCounts.getOrDefault("FAIL", 0L);
                lineChartData.add(new SmsResponse.TimeSeriesItem(
                        LocalDate.of(range.start.getYear(), m, 1)
                                .getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        success, refund, total
                ));
            }
        }
        return lineChartData;
    }

    private Criteria buildCriteria(PeriodRange range, RentRequest req) {
        Criteria c = Criteria.where("createdAt")
                .gte(toDate(range.start))
                .lt(toDate(range.end))
                .and("type").is(SMS_TYPE);

        if (req.getAccountID() != null && !req.getAccountID().isEmpty()) {
            try {
                // accountId trong DB là Number => parse sang long
                long accId = Long.parseLong(req.getAccountID());
                c = c.and("accountId").is(accId);
            } catch (NumberFormatException e) {
                // fallback: so sánh string (trường hợp dữ liệu lẫn string)
                c = c.and("accountId").is(req.getAccountID());
            }
        }
        if (req.getCountryCode() != null && !req.getCountryCode().isEmpty()) {
            // support cả string lẫn array
            c = c.and("countryCode").in(req.getCountryCode());
        }

        return c;
    }
    private String validateRequest(RentRequest req) {
        if (req.getTimeType() == null) return "timeType is required";
        if (req.getYear() == null) return "year is required";

        if (req.getTimeType() == TimeType.MONTH &&
                (req.getMonth() == null || req.getMonth() < 1 || req.getMonth() > 12)) {
            return "month is required for MONTH and must be 1..12";
        }

        if (req.getTimeType() == TimeType.WEEK) {
            if (req.getMonth() == null || req.getMonth() < 1 || req.getMonth() > 12) {
                return "month is required for WEEK and must be 1..12";
            }
        }
        return "";
    }
    private String bucketExpr(PeriodRange range) {
        if (range.timeType == TimeType.WEEK) {
            // Bucket theo ngày (1..7)
            return "dayOfWeek(createdAt)";
        }
        if (range.timeType == TimeType.MONTH) {
            // Bucket theo tuần trong tháng (1..4), mỗi tuần = 7 ngày
            return "ceil(dayOfMonth(createdAt) / 7)";
        }
        if (range.timeType == TimeType.YEAR) {
            // Bucket theo tháng trong năm (1..12)
            return "month(createdAt)";
        }
        throw new IllegalArgumentException("Unsupported timeType: " + range.timeType);
    }
    private static Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static class PeriodRange {
        final LocalDateTime start, end;
        final TimeType timeType;

        PeriodRange(LocalDateTime s, LocalDateTime e, TimeType t) {
            this.start = s;
            this.end = e;
            this.timeType = t;
        }
    }
    private PeriodRange resolveRange(RentRequest req) {
        TimeType type = req.getTimeType();
        int year = req.getYear();

        switch (type) {
            case YEAR: {
                // Cả năm
                LocalDate start = LocalDate.of(year, 1, 1);
                LocalDate end = start.plusYears(1);
                return new PeriodRange(start.atStartOfDay(), end.atStartOfDay(), type);
            }
            case MONTH: {
                // Một tháng
                int month = req.getMonth();
                LocalDate start = LocalDate.of(year, month, 1);
                LocalDate end = start.plusMonths(1);
                return new PeriodRange(start.atStartOfDay(), end.atStartOfDay(), type);
            }
            case WEEK: {
                // Một tuần (Mon → Sun) trong tháng đã chọn
                int month = req.getMonth();
                LocalDate anchor = LocalDate.of(year, month, 1);

                // Tìm tuần chứa ngày hiện tại
                LocalDate today = LocalDate.now();
                LocalDate baseDay = (today.getYear() == year && today.getMonthValue() == month)
                        ? today
                        : anchor;

                LocalDate startOfWeek = baseDay.with(DayOfWeek.MONDAY);
                LocalDate endOfWeek = startOfWeek.plusWeeks(1);

                return new PeriodRange(startOfWeek.atStartOfDay(), endOfWeek.atStartOfDay(), type);
            }
            default:
                throw new IllegalArgumentException("Unsupported TimeType: " + type);
        }
    }


}

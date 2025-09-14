package com.example.gsm.repositories.impl;

import com.example.gsm.dao.*;
import com.example.gsm.repositories.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

import static com.example.gsm.comon.Constants.*;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final MongoTemplate mongo;

    // ===== Types =====
    private static final String OTP = "buy.otp.service";
    private static final String RENT = "rent.otp.service";
    private static final String SMS = "buy.sms.service";
    private static final String CALL = "buy.call.service";
    private static final String PROXY = "buy.proxy.service";
    private static final List<String> ALL_TYPES = List.of(OTP, RENT, SMS, CALL, PROXY);

    // ======================================================================================
    // Entry
    // ======================================================================================
    @Override
    public ResponseCommon<StatisticsSimpleResponse> getStatistics(StatisticsRequest req) {

        String error = validateGetStatistics(req);
        if (!error.isEmpty()) {
            return new ResponseCommon<>(CORE_ERROR_CODE, error, null);
        }

        PeriodRange range = resolveRange(req);

        // 1) Overview (per-type + totals + revenue)
        List<StatisticsSimpleResponse.TypeTotal> perType = aggregatePerType(range);
        long successTotal = perType.stream().mapToLong(StatisticsSimpleResponse.TypeTotal::getSuccess).sum();
        long refundTotal = perType.stream().mapToLong(StatisticsSimpleResponse.TypeTotal::getRefund).sum();
        long totalAll = perType.stream().mapToLong(StatisticsSimpleResponse.TypeTotal::getTotal).sum();
        double revenueTot = perType.stream().mapToDouble(StatisticsSimpleResponse.TypeTotal::getRevenue).sum();
        long countryCount = distinctCountry(range);

        StatisticsSimpleResponse.Overview overview = StatisticsSimpleResponse.Overview.builder()
                .types(perType)
                .countryCount(countryCount)
                .successTotal(successTotal)
                .refundTotal(refundTotal)
                .total(totalAll)
                .revenueTotal(round2(revenueTot))
                .build();

        // 2) byAppType: type -> apps breakdown
        List<StatisticsSimpleResponse.TypeGroup> byAppType = aggregateTypeThenApps(range);

        // 3) timeSeries: per-type series (DAY/WEEK/MONTH)
        List<StatisticsSimpleResponse.TypeSeries> timeSeries = aggregateTimeSeries(range);

        StatisticsSimpleResponse statisticsSimpleResponse = StatisticsSimpleResponse.builder()
                .overviewStatistics(overview)
                .byAppType(byAppType)
                .timeSeries(timeSeries)
                .build();

        return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, statisticsSimpleResponse);
    }

    private String validateGetStatistics(StatisticsRequest req) {
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

    // ======================================================================================
    // Aggregations
    // ======================================================================================

    /**
     * Overview per-type:
     * - count SUCCESS/REFUNDED/TOTAL
     * - sum(cost) as revenue (tổng doanh thu)
     */
    private List<StatisticsSimpleResponse.TypeTotal> aggregatePerType(PeriodRange range) {
        Criteria c = Criteria.where("createdAt").gte(toDate(range.start)).lt(toDate(range.end));

        Aggregation agg = newAggregation(
                match(c),
                project("type", "statusCode", "cost"),
                group("type")
                        .sum(ConditionalOperators.when(Criteria.where("statusCode").is("SUCCESS"))
                                .then(1).otherwise(0)).as("success")
                        .sum(ConditionalOperators.when(Criteria.where("statusCode").is("REFUNDED"))
                                .then(1).otherwise(0)).as("refund")
                        .count().as("total")
                        .sum("cost").as("revenue"),
                project("success", "refund", "total", "revenue").and("_id").as("type"),
                sort(Sort.Direction.ASC, "type")
        );

        List<Document> rows = mongo.aggregate(agg, "orders", Document.class).getMappedResults();

        List<StatisticsSimpleResponse.TypeTotal> out = new ArrayList<>();
        for (Document d : rows) {
            String type = d.getString("type");
            if (!ALL_TYPES.contains(type)) continue;
            out.add(StatisticsSimpleResponse.TypeTotal.builder()
                    .type(type)
                    .success(((Number) d.get("success")).longValue())
                    .refund(((Number) d.get("refund")).longValue())
                    .total(((Number) d.get("total")).longValue())
                    .revenue(round2(((Number) d.getOrDefault("revenue", 0)).doubleValue()))
                    .build());
        }
        // Bổ sung các type không có dữ liệu để luôn đủ 5 type
        for (String t : ALL_TYPES) {
            boolean exists = out.stream().anyMatch(x -> x.getType().equals(t));
            if (!exists) {
                out.add(StatisticsSimpleResponse.TypeTotal.builder()
                        .type(t).success(0).refund(0).total(0).revenue(0.0).build());
            }
        }
        out.sort(Comparator.comparing(StatisticsSimpleResponse.TypeTotal::getType));
        return out;
    }

    /**
     * byAppType: group theo (type, platform) → gom theo type
     */
    private List<StatisticsSimpleResponse.TypeGroup> aggregateTypeThenApps(PeriodRange range) {
        Criteria c = Criteria.where("createdAt").gte(toDate(range.start)).lt(toDate(range.end));

        Aggregation agg = newAggregation(
                match(c),
                project("platform", "type", "statusCode"),
                group("type", "platform")
                        .sum(ConditionalOperators.when(Criteria.where("statusCode").is("SUCCESS"))
                                .then(1).otherwise(0)).as("success")
                        .sum(ConditionalOperators.when(Criteria.where("statusCode").is("REFUNDED"))
                                .then(1).otherwise(0)).as("refund")
                        .count().as("total"),
                project("success", "refund", "total")
                        .and("_id.type").as("type")
                        .and("_id.platform").as("app"),
                sort(Sort.Direction.ASC, "type", "app")
        );

        List<Document> rows = mongo.aggregate(agg, "orders", Document.class).getMappedResults();

        Map<String, List<StatisticsSimpleResponse.AppBreakdown>> map = new LinkedHashMap<>();
        for (Document d : rows) {
            String type = d.getString("type");
            if (!ALL_TYPES.contains(type)) continue;

            StatisticsSimpleResponse.AppBreakdown ab = StatisticsSimpleResponse.AppBreakdown.builder()
                    .app(d.getString("app"))
                    .success(((Number) d.get("success")).longValue())
                    .refund(((Number) d.get("refund")).longValue())
                    .total(((Number) d.get("total")).longValue())
                    .build();

            map.computeIfAbsent(type, k -> new ArrayList<>()).add(ab);
        }

        List<StatisticsSimpleResponse.TypeGroup> out = new ArrayList<>();
        for (String type : ALL_TYPES) {
            List<StatisticsSimpleResponse.AppBreakdown> apps = map.getOrDefault(type, new ArrayList<>());
            long s = apps.stream().mapToLong(StatisticsSimpleResponse.AppBreakdown::getSuccess).sum();
            long r = apps.stream().mapToLong(StatisticsSimpleResponse.AppBreakdown::getRefund).sum();
            long t = apps.stream().mapToLong(StatisticsSimpleResponse.AppBreakdown::getTotal).sum();

            out.add(StatisticsSimpleResponse.TypeGroup.builder()
                    .type(type)
                    .success(s)
                    .refund(r)
                    .total(t)
                    .apps(apps)
                    .build());
        }
        return out;
    }

    /**
     * timeSeries theo từng type:
     * - bucket: DAY → dayOfMonth(createdAt)
     * WEEK → floor((dayOfMonth(createdAt)-1)/7) in [0..3]
     * MONTH→ month(createdAt) in [1..12]
     * - group theo (type, bucket)
     */
    private List<StatisticsSimpleResponse.TypeSeries> aggregateTimeSeries(PeriodRange range) {
        Criteria c = Criteria.where("createdAt").gte(toDate(range.start)).lt(toDate(range.end));

        Aggregation agg = newAggregation(
                match(c),
                project("type", "statusCode", "createdAt")
                        .andExpression(bucketExpr(range)).as("bucket"),
                group("type", "bucket")
                        .sum(ConditionalOperators.when(Criteria.where("statusCode").is("SUCCESS"))
                                .then(1).otherwise(0)).as("success")
                        .sum(ConditionalOperators.when(Criteria.where("statusCode").is("REFUNDED"))
                                .then(1).otherwise(0)).as("refund")
                        .count().as("total"),
                project("success", "refund", "total")
                        .and("_id.type").as("type")
                        .and("_id.bucket").as("bucket"),
                sort(Sort.Direction.ASC, "type", "bucket")
        );

        List<Document> rows = mongo.aggregate(agg, "orders", Document.class).getMappedResults();

        // Map: type -> (bucket -> doc)
        Map<String, Map<Integer, Document>> map = new HashMap<>();
        for (Document d : rows) {
            String type = d.getString("type");
            if (!ALL_TYPES.contains(type)) continue;
            int bucket = ((Number) d.get("bucket")).intValue();
            map.computeIfAbsent(type, k -> new HashMap<>()).put(bucket, d);
        }

        List<StatisticsSimpleResponse.TypeSeries> out = new ArrayList<>();
        for (String type : ALL_TYPES) {
            Map<Integer, Document> byBucket = map.getOrDefault(type, new HashMap<>());
            List<StatisticsSimpleResponse.TimeSeriesItem> points = new ArrayList<>();

            if (range.timeType == TimeType.WEEK) {
                // 7 ngày trong tuần (Mon..Sun)
                String[] weekDays = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                for (int d = 1; d <= 7; d++) {
                    points.add(point(weekDays[d - 1], byBucket.get(d)));
                }
            } else if (range.timeType == TimeType.MONTH) {
                // 4 tuần trong tháng
                for (int w = 0; w < 4; w++) {
                    points.add(point("W" + (w + 1), byBucket.get(w)));
                }
            } else if (range.timeType == TimeType.YEAR) {
                // 12 tháng trong năm
                for (int m = 1; m <= 12; m++) {
                    points.add(point(Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH), byBucket.get(m)));
                }
            }

            out.add(StatisticsSimpleResponse.TypeSeries.builder()
                    .type(type)
                    .points(points)
                    .build());
        }
        return out;
    }

    private StatisticsSimpleResponse.TimeSeriesItem point(String label, Document r) {
        return StatisticsSimpleResponse.TimeSeriesItem.builder()
                .label(label)
                .success(r == null ? 0 : ((Number) r.get("success")).longValue())
                .refund(r == null ? 0 : ((Number) r.get("refund")).longValue())
                .total(r == null ? 0 : ((Number) r.get("total")).longValue())
                .build();
    }

    /**
     * Country distinct (toàn window)
     */
    private long distinctCountry(PeriodRange range) {
        Criteria c = Criteria.where("createdAt").gte(toDate(range.start)).lt(toDate(range.end));

        Aggregation agg = newAggregation(
                match(c),
                group().addToSet("countryCode").as("countries"),
                project().and(ArrayOperators.Size.lengthOfArray("countries")).as("count")
        );
        Document doc = mongo.aggregate(agg, "orders", Document.class).getUniqueMappedResult();
        return doc == null ? 0 : ((Number) doc.getOrDefault("count", 0)).longValue();
    }

    // ======================================================================================
    // Helpers
    // ======================================================================================
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

    private PeriodRange resolveRange(StatisticsRequest req) {
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
}

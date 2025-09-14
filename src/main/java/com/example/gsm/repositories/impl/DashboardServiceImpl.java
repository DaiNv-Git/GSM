package com.example.gsm.repositories.impl;

import com.example.gsm.comon.LogConfig;
import com.example.gsm.dao.*;
import com.example.gsm.repositories.DashboardService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
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
public class DashboardServiceImpl implements DashboardService {

    LogConfig logger = new LogConfig(DashboardServiceImpl.class);

    @Autowired
    private MessageSource messageSource;

    private final MongoTemplate mongo;

    // ====== TYPE CONSTANTS ======
    private static final String OTP = "buy.otp.service";
    private static final String RENT = "rent.otp.service";
    private static final String CALL = "buy.call.service";
    private static final String SMS = "buy.sms.service";
    private static final String PROXY = "buy.proxy.service";
    private static final List<String> ALL_TYPES = List.of(OTP, RENT, SMS, CALL, PROXY);

    @Override
    public ResponseCommon<DashboardResponse> getDashboard(DashboardRequest req) {
        logger.info(messageSource.getMessage("LOG.REQUEST_START", null, null), new JSONObject(req));

        String b_loi = validateDashboardRequest(req);
        if (!b_loi.isEmpty()) {
            return new ResponseCommon<>(CORE_ERROR_CODE, b_loi, null);
        }

        PeriodRange cur = resolveRange(req, false);
        PeriodRange prev = resolveRange(req, true);

        Totals curTotals = aggregateTotalsPerTypeAndOverall(cur);
        Totals prevTotals = aggregateTotalsPerTypeAndOverall(prev);

        String growthAmount = pct(curTotals.totalAmountSuccess, prevTotals.totalAmountSuccess);
        long countryCount = distinctCountryCount(cur);

        DashboardResponse.Overview overview = DashboardResponse.Overview.builder()
                .totalOtp(curTotals.countByType.getOrDefault(OTP, 0L))
                .totalRent(curTotals.countByType.getOrDefault(RENT, 0L))
                .totalSms(curTotals.countByType.getOrDefault(SMS, 0L))
                .totalCall(curTotals.countByType.getOrDefault(CALL, 0L))
                .totalProxy(curTotals.countByType.getOrDefault(PROXY, 0L))
                .countryCount(countryCount)
                .totalAmountAll(round2(curTotals.totalAmountAll))
                .totalAmountSuccess(round2(curTotals.totalAmountSuccess))
                .growthPercent(growthAmount)
                .build();

        List<DashboardResponse.StatusBox> statusBoxes = new ArrayList<>();
        for (String type : ALL_TYPES) {
            long sNow = curTotals.successByType.getOrDefault(type, 0L);
            long fNow = curTotals.failByType.getOrDefault(type, 0L);
            long sPrev = prevTotals.successByType.getOrDefault(type, 0L);
            statusBoxes.add(DashboardResponse.StatusBox.builder()
                    .type(type)
                    .success(sNow)
                    .fail(fNow)
                    .growthPercent(pct(sNow, sPrev))
                    .build());
        }

        DashboardResponse dashboardResponse = DashboardResponse.builder()
                .overview(overview)
                .statusBoxes(statusBoxes)
                .build();

        logger.info(messageSource.getMessage("LOG.RESPONSE_START", null, null), new JSONObject(dashboardResponse));

        return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, dashboardResponse);
    }

    private String validateDashboardRequest(DashboardRequest req) {
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

//    private PeriodRange resolveRange(DashboardRequest req, boolean previous) {
//        TimeType type = req.getTimeType();
//        int year = req.getYear();
//        if (type == TimeType.MONTH) {
//            LocalDate s = LocalDate.of(previous ? year - 1 : year, 1, 1);
//            return new PeriodRange(s.atStartOfDay(), s.plusYears(1).atStartOfDay(), type);
//        }
//        int month = req.getMonth();
//        LocalDate anchor = LocalDate.of(year, month, 1);
//        LocalDate s = previous ? anchor.minusMonths(1) : anchor;
//        return new PeriodRange(s.atStartOfDay(), s.plusMonths(1).atStartOfDay(), type);
//    }

    private PeriodRange resolveRange(DashboardRequest req, boolean previous) {
        TimeType type = req.getTimeType();
        int year = req.getYear();

        switch (type) {
            case YEAR: {
                LocalDate start = LocalDate.of(previous ? year - 1 : year, 1, 1);
                LocalDate end = start.plusYears(1);
                return new PeriodRange(start.atStartOfDay(), end.atStartOfDay(), type);
            }
            case MONTH: {
                int month = req.getMonth();
                LocalDate anchor = LocalDate.of(year, month, 1);
                LocalDate start = previous ? anchor.minusMonths(1) : anchor;
                LocalDate end = start.plusMonths(1);
                return new PeriodRange(start.atStartOfDay(), end.atStartOfDay(), type);
            }
            case WEEK: {
                int month = req.getMonth();
                // lấy ngày hiện tại
                LocalDate now = LocalDate.now();
                // nếu now không nằm trong month/year được chọn thì lấy ngày đầu tháng
                if (now.getYear() != year || now.getMonthValue() != month) {
                    now = LocalDate.of(year, month, 1);
                }

                LocalDate startOfWeek = now.with(DayOfWeek.MONDAY);
                if (previous) {
                    startOfWeek = startOfWeek.minusWeeks(1);
                }
                LocalDate endOfWeek = startOfWeek.plusWeeks(1);

                return new PeriodRange(startOfWeek.atStartOfDay(), endOfWeek.atStartOfDay(), type);
            }
            default:
                throw new IllegalArgumentException("Unsupported TimeType: " + type);
        }
    }



    private Totals aggregateTotalsPerTypeAndOverall(PeriodRange range) {
        Criteria base = criteriaForRange(range);

        Aggregation aggTypeStatus = newAggregation(
                match(base),
                project("type", "statusCode", "cost"),
                group("type", "statusCode").count().as("cnt")
        );
        List<Document> rows = mongo.aggregate(aggTypeStatus, "orders", Document.class).getMappedResults();

        Totals t = new Totals();
        for (Document d : rows) {
            Document id = (Document) d.get("_id");
            String type = id.getString("type");
            String status = id.getString("statusCode");
            long cnt = d.get("cnt", Number.class).longValue();
            if (!ALL_TYPES.contains(type)) continue;
            t.countByType.merge(type, cnt, Long::sum);
            if ("SUCCESS".equals(status)) t.successByType.merge(type, cnt, Long::sum);
            else t.failByType.merge(type, cnt, Long::sum);
        }

        Aggregation aggMoney = newAggregation(
                match(base),
                project("cost", "statusCode")
                        .andExpression("statusCode == 'SUCCESS'").as("isSuccess"),
                group()
                        .sum("cost").as("sumAll")
                        .sum(ConditionalOperators.when(Criteria.where("isSuccess").is(true))
                                .thenValueOf("cost").otherwise(0)).as("sumSuccess")
        );
        Map r = Optional.ofNullable(mongo.aggregate(aggMoney, "orders", Map.class).getUniqueMappedResult())
                .orElseGet(HashMap::new);

        t.totalAmountAll = ((Number) r.getOrDefault("sumAll", 0)).doubleValue();
        t.totalAmountSuccess = ((Number) r.getOrDefault("sumSuccess", 0)).doubleValue();
        return t;
    }

    private long distinctCountryCount(PeriodRange range) {
        Criteria c = criteriaForRange(range);
        Aggregation agg = newAggregation(
                match(c),
                group().addToSet("countryCode").as("countries"),
                project().and(ConditionalOperators.ifNull("countries").then(Collections.emptyList())).as("countries"),
                project().and(ArrayOperators.Size.lengthOfArray("countries")).as("count")
        );
        Map r = mongo.aggregate(agg, "orders", Map.class).getUniqueMappedResult();
        return r == null ? 0 : ((Number) r.getOrDefault("count", 0)).longValue();
    }

    @Override
    public ResponseCommon<TypeTotalsResponse> getTypeTotals(TypeTotalsRequest req) {

        String b_loi = validateTypeTotalsRequest(req);
        if (!b_loi.isEmpty()) {
            return new ResponseCommon<>(CORE_ERROR_CODE, b_loi, null);
        }

        PeriodRange range = resolveRange(req);

        List<String> labels;
        ProjectionOperation addBucket;

        if (range.timeType == TimeType.WEEK) {
            // Labels cố định: Thứ 2 đến Chủ nhật
            labels = List.of("Mon","Tue","Wed","Thu","Fri","Sat","Sun");

            addBucket = project("type", "cost", "createdAt")
                    .and(DateOperators.DayOfWeek.dayOfWeek("createdAt")).as("bucket");
            // MongoDB: 1=Sunday, 2=Monday,... 7=Saturday
        } else if (range.timeType == TimeType.MONTH) {
            // 4 tuần
            labels = List.of("W1","W2","W3","W4");
            AggregationExpression baseWeek = ArithmeticOperators.Floor.floorValueOf(
                    ArithmeticOperators.Divide.valueOf(
                            ArithmeticOperators.Subtract.valueOf(
                                    DateOperators.DayOfMonth.dayOfMonth("createdAt")
                            ).subtract(1)
                    ).divideBy(7)
            );

            AggregationExpression bucketCapped =
                    ConditionalOperators.when(
                            ComparisonOperators.Gte.valueOf(baseWeek).greaterThanEqualToValue(4)
                    ).then(3).otherwise(baseWeek);

            addBucket = project("type", "cost", "createdAt")
                    .and(bucketCapped).as("bucket");
        } else {
            // YEAR → 12 tháng
            labels = new ArrayList<>(12);
            for (int m = 1; m <= 12; m++) {
                labels.add(Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            }
            addBucket = project("type", "cost", "createdAt")
                    .and(DateOperators.Month.monthOf("createdAt")).as("bucket"); // 1..12
        }

        Criteria c = Criteria.where("createdAt")
                .gte(toDate(range.start))
                .lt(toDate(range.end));

        Aggregation agg = newAggregation(
                match(c),
                addBucket,
                group("type", "bucket").sum("cost").as("amount"), // nếu cần SUCCESS-only thì đổi như ghi chú
                project("amount").and("_id.type").as("type").and("_id.bucket").as("bucket"),
                sort(Sort.Direction.ASC, "type", "bucket")
        );

        List<Document> rows = mongo.aggregate(agg, "orders", Document.class).getMappedResults();

        Map<String, Map<Integer, Double>> typeBucketAmount = new HashMap<>();
        for (Document d : rows) {
            String type = d.getString("type");
            Integer bucket = ((Number) d.get("bucket")).intValue();
            double amount = ((Number) d.get("amount")).doubleValue();
            typeBucketAmount.computeIfAbsent(type, k -> new HashMap<>()).put(bucket, amount);
        }

        List<TypeTotalsResponse.TypeSeries> seriesByType = new ArrayList<>();
        for (String type : ALL_TYPES) {
            Map<Integer, Double> byBucket = typeBucketAmount.getOrDefault(type, Collections.emptyMap());
            List<Double> amounts = new ArrayList<>();

            if (range.timeType == TimeType.WEEK) {
                // 7 ngày trong tuần (Mon..Sun)
                for (int d = 1; d <= 7; d++) {
                    amounts.add(round2(byBucket.getOrDefault(d, 0.0)));
                }
            } else if (range.timeType == TimeType.MONTH) {
                // 4 tuần trong tháng
                for (int w = 0; w < 4; w++) {
                    amounts.add(round2(byBucket.getOrDefault(w, 0.0)));
                }
            } else if (range.timeType == TimeType.YEAR) {
                // 12 tháng
                for (int m = 1; m <= 12; m++) {
                    amounts.add(round2(byBucket.getOrDefault(m, 0.0)));
                }
            }

            double total = amounts.stream().mapToDouble(Double::doubleValue).sum();
            seriesByType.add(TypeTotalsResponse.TypeSeries.builder()
                    .type(type)
                    .totalAmount(round2(total))
                    .amounts(amounts)
                    .build());
        }

        TypeTotalsResponse typeTotalsResponse = TypeTotalsResponse.builder()
                .labels(labels)
                .seriesByType(seriesByType)
                .build();

        return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, typeTotalsResponse);

    }

    private String validateTypeTotalsRequest(TypeTotalsRequest req) {
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

    private PeriodRange resolveRange(TypeTotalsRequest req) {
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


    // ========================= COMMON =========================
    private Criteria criteriaForRange(PeriodRange range) {
        return Criteria.where("createdAt")
                .gte(toDate(range.start))
                .lt(toDate(range.end));
    }

    private static Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String pct(double cur, double prev) {
        if (prev <= 0 && cur <= 0) return "0%";
        if (prev <= 0 && cur > 0) return "100%";
        double p = (cur - prev) * 100.0 / prev;
        return String.format(Locale.US, "%.0f%%", p);
    }

    // ========================= INTERNAL POJOs =========================
    private static class PeriodRange {
        final LocalDateTime start;
        final LocalDateTime end;
        final TimeType timeType;

        PeriodRange(LocalDateTime s, LocalDateTime e, TimeType t) {
            this.start = s;
            this.end = e;
            this.timeType = t;
        }
    }

    private static class Totals {
        Map<String, Long> countByType = new HashMap<>();
        Map<String, Long> successByType = new HashMap<>();
        Map<String, Long> failByType = new HashMap<>();
        double totalAmountAll;
        double totalAmountSuccess;
    }
}

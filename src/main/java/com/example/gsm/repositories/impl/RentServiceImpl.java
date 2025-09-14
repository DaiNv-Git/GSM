package com.example.gsm.repositories.impl;

import com.example.gsm.dao.*;
import com.example.gsm.entity.ServiceEntity;
import com.example.gsm.repositories.RentService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.gsm.comon.Constants.CORE_ERROR_CODE;
import static com.example.gsm.comon.Constants.SUCCESS_CODE;
import static com.example.gsm.comon.Constants.SUCCESS_MESSAGE;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;

@Service
@RequiredArgsConstructor
public class RentServiceImpl implements RentService {
    private final MongoTemplate mongo;

    private static final String RENT_TYPE = "rent.otp.service";

    @Override
    public ResponseCommon<RentResponse> getRent(RentRequest req) {
        // Validate request
        String error = validateRequest(req);
        if (!error.isEmpty()) {
            return new ResponseCommon<>(CORE_ERROR_CODE, error, null);
        }

        // Resolve time range
        PeriodRange range = resolveRange(req);

        // 1) Revenue Metrics
        RentResponse.RevenueMetrics revenueMetrics = aggregateRevenueMetrics(range,req);

        // 2) Fetch rentInfos
        List<RentResponse.RentInfo> rentInfos = fetchRentInfos(range,req);

        // 3) Bar Chart: Success and Refund count by serviceCode
        List<RentResponse.ChartData> successRefundChart = aggregateSuccessRefundChart(range,req);

        // 4) Line Chart: Total revenue by serviceCode
//        List<RentResponse.RevenueData> revenueChart = aggregateRevenueChart(range);
        List<RentResponse.TimeSeriesItem> lineChartData = aggregateLineChart(range, req);

        // Build response
        RentResponse response = new RentResponse(revenueMetrics, rentInfos, successRefundChart, lineChartData);
        return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, response);
    }

    private Criteria buildCriteria(PeriodRange range, RentRequest req) {
        Criteria c = Criteria.where("createdAt")
                .gte(toDate(range.start))
                .lt(toDate(range.end))
                .and("type").is(RENT_TYPE);

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

    private List<RentResponse.RentInfo> fetchRentInfos(PeriodRange range, RentRequest req) {
        Criteria c = buildCriteria(range, req);

        Aggregation agg = newAggregation(
                match(c),
                project("_id", "statusCode", "cost", "createdAt", "accountId")
                        .and("stock.serviceCode").as("serviceCode")
                        .and("stock.phone").as("phoneNumber")
                        .and("stock.expiredAt").as("expiredAt")
        );

        List<Document> orders = mongo.aggregate(agg, "orders", Document.class).getMappedResults();
        if (orders.isEmpty()) return Collections.emptyList();

        // Lấy danh sách accountId kiểu String
        Set<String> accountIds = orders.stream()
                .map(d -> d.get("accountId"))
                .filter(Objects::nonNull)
                .flatMap(acc -> {
                    if (acc instanceof List) return ((List<?>) acc).stream().map(Object::toString);
                    else return Stream.of(acc.toString());
                })
                .collect(Collectors.toSet());

        // Lấy username từ users collection
        List<Document> userDocsRaw = mongo.findAll(Document.class, "users");
        Map<String, String> accountIdToUsername = new HashMap<>();

        for (Document u : userDocsRaw) {
            Object accObj = u.get("accountId");
            List<String> accIdList = new ArrayList<>();
            if (accObj instanceof List) {
                ((List<?>) accObj).forEach(a -> {
                    if (a != null && accountIds.contains(a.toString())) accIdList.add(a.toString());
                });
            } else if (accObj != null && accountIds.contains(accObj.toString())) {
                accIdList.add(accObj.toString());
            }

            String firstName = extractString(u.get("firstName"));
            String lastName = extractString(u.get("lastName"));
            String username = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

            for (String accId : accIdList) {
                accountIdToUsername.put(accId, username);
            }
        }

        // Map orders -> RentInfo, tránh duplicate cùng serviceCode và accountId
        List<RentResponse.RentInfo> rentInfos = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (Document d : orders) {
            Object accObj = d.get("accountId");
            List<String> accIdList = new ArrayList<>();
            if (accObj instanceof List) {
                ((List<?>) accObj).forEach(a -> {
                    if (a != null) accIdList.add(a.toString());
                });
            } else if (accObj != null) {
                accIdList.add(accObj.toString());
            }

            Object serviceCodeObj = d.get("serviceCode");
            List<String> serviceCodes = new ArrayList<>();
            if (serviceCodeObj instanceof List) {
                ((List<?>) serviceCodeObj).forEach(sc -> {
                    if (sc != null) serviceCodes.add(sc.toString());
                });
            } else if (serviceCodeObj != null) {
                serviceCodes.add(serviceCodeObj.toString());
            } else {
                serviceCodes.add("OTHER");
            }

            for (String accId : accIdList) {
                String username = accountIdToUsername.get(accId);
                for (String sc : serviceCodes) {
                    String key = accId + "|" + sc;

                    if (!seenKeys.contains(key)) {
                        seenKeys.add(key);
                        rentInfos.add(new RentResponse.RentInfo(
                                username,
                                "OTP",
                                d.getString("statusCode"),
                                accId,
                                d.getString("phoneNumber"),
                                sc,
                                d.get("cost") != null ? d.get("cost").toString() : "0",
                                d.get("createdAt") != null ? d.get("createdAt").toString() : null,
                                d.get("expiredAt") != null ? d.get("expiredAt").toString() : null
                        ));
                    }
                }
            }
        }

        return rentInfos;
    }

    private List<RentResponse.TimeSeriesItem> aggregateLineChart(PeriodRange range,RentRequest req) {
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

        List<RentResponse.TimeSeriesItem> lineChartData = new ArrayList<>();

        if (req.getTimeType() == TimeType.WEEK) {
            // 7 ngày trong tuần
            for (int d = 1; d <= 7; d++) {
                Map<String, Long> statusCounts = bucketStatusMap.getOrDefault(d, new HashMap<>());
                long success = statusCounts.getOrDefault("SUCCESS", 0L);
                long refund = statusCounts.getOrDefault("REFUNDED", 0L);
                long total = success + refund + statusCounts.getOrDefault("FAIL", 0L);
                lineChartData.add(new RentResponse.TimeSeriesItem("D" + d, success, refund, total));
            }
        } else if (req.getTimeType() == TimeType.MONTH) {
            // 4 tuần trong tháng
            for (int w = 1; w <= 4; w++) {
                Map<String, Long> statusCounts = bucketStatusMap.getOrDefault(w, new HashMap<>());
                long success = statusCounts.getOrDefault("SUCCESS", 0L);
                long refund = statusCounts.getOrDefault("REFUNDED", 0L);
                long total = success + refund + statusCounts.getOrDefault("FAIL", 0L);
                lineChartData.add(new RentResponse.TimeSeriesItem("W" + w, success, refund, total));
            }
        } else if (req.getTimeType() == TimeType.YEAR) {
            // 12 tháng trong năm
            for (int m = 1; m <= 12; m++) {
                Map<String, Long> statusCounts = bucketStatusMap.getOrDefault(m, new HashMap<>());
                long success = statusCounts.getOrDefault("SUCCESS", 0L);
                long refund = statusCounts.getOrDefault("REFUNDED", 0L);
                long total = success + refund + statusCounts.getOrDefault("FAIL", 0L);
                lineChartData.add(new RentResponse.TimeSeriesItem(
                        LocalDate.of(range.start.getYear(), m, 1)
                                .getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        success, refund, total
                ));
            }
        }
        return lineChartData;
    }

    private RentResponse.RevenueMetrics aggregateRevenueMetrics(PeriodRange range,RentRequest req) {
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

        return new RentResponse.RevenueMetrics(totalRevenue, netRevenue, commission);
    }

    // Helper method to safely extract String from Object
    private String extractString(Object obj) {
        if (obj == null) return null;

        if (obj instanceof String) {
            return ((String) obj).trim();
        }

        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)   // chuyển mọi phần tử thành string
                    .map(String::trim)       // xóa khoảng trắng
                    .filter(s -> !s.isEmpty()) // loại bỏ rỗng
                    .collect(Collectors.joining(" ")); // nối thành chuỗi
        }

        // Trường hợp khác: ép kiểu an toàn sang string
        String s = obj.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private List<RentResponse.ChartData> aggregateSuccessRefundChart(PeriodRange range,RentRequest req) {
        Criteria c = buildCriteria(range, req);

        Aggregation agg = newAggregation(
                match(c),
                project("statusCode", "isRefund", "isActive")
                        .and("stock.serviceCode").as("serviceCode"),
                group("serviceCode")
                        .sum(ConditionalOperators.when(Criteria.where("statusCode").is("SUCCESS")).then(1).otherwise(0)).as("successCount")
                        .sum(ConditionalOperators.when(Criteria.where("isRefund").is(true)).then(1).otherwise(0)).as("refundCount")
                        .sum(ConditionalOperators.when(Criteria.where("isActive").is(false)).then(1).otherwise(0)).as("bannedCount"),
                project("successCount", "refundCount", "bannedCount")
                        .and("_id").as("serviceCode"),
                sort(Sort.Direction.DESC, "successCount")
        );

        List<Document> rows = mongo.aggregate(agg, "orders", Document.class).getMappedResults();

        List<RentResponse.ChartData> result = new ArrayList<>();

        for (Document d : rows) {
            Object serviceCodeObj = d.get("serviceCode");
            double successCount = d.get("successCount") != null ? ((Number) d.get("successCount")).doubleValue() : 0L;
            double refundCount = d.get("refundCount") != null ? ((Number) d.get("refundCount")).doubleValue() : 0L;
            double bannedCount = d.get("bannedCount") != null ? ((Number) d.get("bannedCount")).doubleValue() : 0L;

            if (serviceCodeObj instanceof List) {
                List<?> list = (List<?>) serviceCodeObj;
                for (Object item : list) {
                    String code = item != null ? item.toString() : "OTHER";
                    result.add(new RentResponse.ChartData(code, successCount, refundCount, bannedCount));
                }
            } else {
                String code = serviceCodeObj != null ? serviceCodeObj.toString() : "OTHER";
                result.add(new RentResponse.ChartData(code, successCount, refundCount, bannedCount));
            }
        }

        return result;
    }

    private List<RentResponse.RevenueData> aggregateRevenueChart(PeriodRange range,RentRequest req) {
        Criteria c = buildCriteria(range, req);

        Aggregation agg = newAggregation(
                match(c),
                project("cost")
                        .and("stock.serviceCode").as("label"),
                group("label")
                        .sum("cost").as("totalRevenue"),
                project("totalRevenue")
                        .and("_id").as("label"),
                sort(Sort.Direction.DESC, "totalRevenue")
        );

        List<Document> rows = mongo.aggregate(agg, "orders", Document.class).getMappedResults();

        List<RentResponse.RevenueData> result = new ArrayList<>();

        for (Document d : rows) {
            Object labelObj = d.get("label");
            double totalRevenue = d.get("totalRevenue") != null ? ((Number) d.get("totalRevenue")).doubleValue() : 0.0;

            if (labelObj instanceof List) {
                List<?> list = (List<?>) labelObj;
                for (Object item : list) {
                    String code = item != null ? item.toString() : "OTHER";
                    result.add(new RentResponse.RevenueData(code, round2(totalRevenue)));
                }
            } else {
                String code = labelObj != null ? labelObj.toString() : "OTHER";
                result.add(new RentResponse.RevenueData(code, round2(totalRevenue)));
            }
        }

        return result;
    }

//    private List<RentResponse.RevenueData> aggregateRevenueChart(PeriodRange range) {
//        Criteria c = Criteria.where("createdAt")
//                .gte(toDate(range.start))
//                .lt(toDate(range.end))
//                .and("type").is(RENT_TYPE);
//
//        Aggregation agg = newAggregation(
//                match(c),
//                project("cost")
//                        .and("stock.serviceCode").as("label"),
//                group("label")
//                        .sum("cost").as("totalRevenue"),
//                project("totalRevenue")
//                        .and("_id").as("label"),
//                sort(Sort.Direction.DESC, "totalRevenue")
//        );
//
//        List<Document> rows = mongo.aggregate(agg, "orders", Document.class).getMappedResults();
//        return rows.stream()
//                .map(d -> new RentResponse.RevenueData(
//                        d.getString("label") != null ? d.getString("label") : "OTHER",
//                        round2(d.get("totalRevenue") != null ? ((Number) d.get("totalRevenue")).doubleValue() : 0.0)
//                ))
//                .collect(Collectors.toList());
//    }

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

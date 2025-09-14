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
        PeriodRange range = resolveRange(req.getTimeType(), req.getYear(), req.getMonth());

        // 1) Revenue Metrics
        RentResponse.RevenueMetrics revenueMetrics = aggregateRevenueMetrics(range);

        // 2) Fetch rentInfos
        List<RentResponse.RentInfo> rentInfos = fetchRentInfos(range);

        // 3) Bar Chart: Success and Refund count by serviceCode
        List<RentResponse.ChartData> successRefundChart = aggregateSuccessRefundChart(range);

        // 4) Line Chart: Total revenue by serviceCode
//        List<RentResponse.RevenueData> revenueChart = aggregateRevenueChart(range);
        List<RentResponse.TimeSeriesItem> lineChartData = aggregateLineChart(range, req.getTimeType());

        // Build response
        RentResponse response = new RentResponse(revenueMetrics, rentInfos, successRefundChart, lineChartData);
        return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, response);
    }


    private String validateRequest(RentRequest req) {
        if (req.getTimeType() == null) {
            return "timeType is required";
        }
        if (req.getYear() == null) {
            return "year is required";
        }
        if ((req.getTimeType() == TimeType.DAY || req.getTimeType() == TimeType.WEEK)
                && (req.getMonth() == null || req.getMonth() < 1 || req.getMonth() > 12)) {
            return "month is required for DAY/WEEK and must be 1..12";
        }
        return "";
    }

    private List<RentResponse.RentInfo> fetchRentInfos(PeriodRange range) {
        Criteria c = Criteria.where("createdAt")
                .gte(toDate(range.start))
                .lt(toDate(range.end))
                .and("type").is(RENT_TYPE);

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
    private List<RentResponse.TimeSeriesItem> aggregateLineChart(PeriodRange range, TimeType timeType) {
        Criteria c = Criteria.where("createdAt")
                .gte(toDate(range.start))
                .lt(toDate(range.end))
                .and("type").is(RENT_TYPE);

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

        if (timeType == TimeType.DAY) {
            List<LocalDate> dates = range.start.toLocalDate().datesUntil(range.end.toLocalDate())
                    .collect(Collectors.toList());
            for (LocalDate date : dates) {
                String label = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                Map<String, Long> statusCounts = bucketStatusMap.getOrDefault(date.getDayOfMonth(), new HashMap<>());
                long success = statusCounts.getOrDefault("SUCCESS", 0L);
                long refund = statusCounts.getOrDefault("REFUNDED", 0L);
                long total = success + refund + statusCounts.getOrDefault("FAIL", 0L);
                lineChartData.add(new RentResponse.TimeSeriesItem(label, success, refund, total));
            }
        } else if (timeType == TimeType.WEEK) {
            int weeks = (int) Math.ceil(range.end.toLocalDate().lengthOfMonth() / 7.0);
            for (int w = 0; w < weeks; w++) {
                Map<String, Long> statusCounts = bucketStatusMap.getOrDefault(w, new HashMap<>());
                long success = statusCounts.getOrDefault("SUCCESS", 0L);
                long refund = statusCounts.getOrDefault("REFUNDED", 0L);
                long total = success + refund + statusCounts.getOrDefault("FAIL", 0L);
                lineChartData.add(new RentResponse.TimeSeriesItem("W" + (w + 1), success, refund, total));
            }
        } else { // MONTH
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
    private RentResponse.RevenueMetrics aggregateRevenueMetrics(PeriodRange range) {
        Criteria c = Criteria.where("createdAt")
                .gte(toDate(range.start))
                .lt(toDate(range.end))
                .and("type").is(RENT_TYPE);

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

    private List<String> fetchUsernames(PeriodRange range) {
        Criteria c = Criteria.where("createdAt")
                .gte(toDate(range.start))
                .lt(toDate(range.end))
                .and("type").is(RENT_TYPE);

        Aggregation agg = newAggregation(
                match(c),
                project("accountId"), // Lấy accountId từ orders
                group("accountId")
        );

        List<Document> accountIds = mongo.aggregate(agg, "orders", Document.class).getMappedResults();
        List<Long> accountIdList = accountIds.stream()
                .map(d -> {
                    Object id = d.get("_id");
                    if (id instanceof Number) {
                        return ((Number) id).longValue();
                    } else if (id instanceof String) {
                        try {
                            return Long.parseLong((String) id);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (accountIdList.isEmpty()) {
            return Collections.emptyList();
        }

        // Truy vấn bảng users dựa trên accountIdList, lấy firstName và lastName
        Criteria userCriteria = Criteria.where("accountId").in(accountIdList);
        Aggregation userAgg = newAggregation(
                match(userCriteria),
                project("firstName", "lastName") // Lấy firstName và lastName
                        .andExclude("_id") // Loại trừ _id nếu không cần
        );
        List<Document> userDocs = mongo.aggregate(userAgg, "users", Document.class).getMappedResults();
        return userDocs.stream()
                .map(d -> {
                    // Xử lý firstName và lastName, kiểm tra nếu là ArrayList hoặc khác String
                    Object firstNameObj = d.get("firstName");
                    Object lastNameObj = d.get("lastName");
                    String firstName = extractString(firstNameObj);
                    String lastName = extractString(lastNameObj);

                    // Nối firstName và lastName, xử lý null/rỗng
                    if (firstName != null && !firstName.trim().isEmpty()) {
                        return lastName != null && !lastName.trim().isEmpty()
                                ? firstName.trim() + " " + lastName.trim()
                                : firstName.trim();
                    } else if (lastName != null && !lastName.trim().isEmpty()) {
                        return lastName.trim();
                    }
                    return null; // Trả về null nếu cả hai đều null/rỗng
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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

//    private List<RentResponse.ChartData> aggregateSuccessRefundChart(PeriodRange range) {
//        Criteria c = Criteria.where("createdAt")
//                .gte(toDate(range.start))
//                .lt(toDate(range.end))
//                .and("type").is(RENT_TYPE);
//
//        Aggregation agg = newAggregation(
//                match(c),
//                project("statusCode", "isRefund", "isActive")
//                        .and("stock.serviceCode").as("serviceCode"),
//                group("serviceCode")
//                        .sum(ConditionalOperators.when(Criteria.where("statusCode").is("SUCCESS")).then(1).otherwise(0)).as("successCount")
//                        .sum(ConditionalOperators.when(Criteria.where("isRefund").is(true)).then(1).otherwise(0)).as("refundCount")
//                        .sum(ConditionalOperators.when(Criteria.where("isActive").is(false)).then(1).otherwise(0)).as("bannedCount"),
//                project("successCount", "refundCount", "bannedCount")
//                        .and("_id").as("serviceCode"),
//                sort(Sort.Direction.DESC, "successCount")
//        );
//
//        List<Document> rows = mongo.aggregate(agg, "orders", Document.class).getMappedResults();
//        return rows.stream()
//                .map(d -> new RentResponse.ChartData(
//                        extractServiceCode(d.get("serviceCode")),
//                        d.get("successCount") != null ? ((Number) d.get("successCount")).doubleValue() : 0L,
//                        d.get("refundCount") != null ? ((Number) d.get("refundCount")).doubleValue() : 0L,
//                        0.0
//                ))
//                .collect(Collectors.toList());
//
//    }
private List<RentResponse.ChartData> aggregateSuccessRefundChart(PeriodRange range) {
    Criteria c = Criteria.where("createdAt")
            .gte(toDate(range.start))
            .lt(toDate(range.end))
            .and("type").is(RENT_TYPE);

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

    private String extractServiceCode(Object obj) {
        if (obj == null) return "OTHER";
        if (obj instanceof String) return (String) obj;
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .findFirst() // chỉ lấy phần tử đầu tiên nếu cần
                    .orElse("OTHER");
        }
        return obj.toString();
    }
    private List<RentResponse.RevenueData> aggregateRevenueChart(PeriodRange range) {
        Criteria c = Criteria.where("createdAt")
                .gte(toDate(range.start))
                .lt(toDate(range.end))
                .and("type").is(RENT_TYPE);

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
        if (range.timeType == TimeType.DAY) return "dayOfMonth(createdAt)";
        if (range.timeType == TimeType.WEEK) return "floor((dayOfMonth(createdAt)-1)/7)";
        return "month(createdAt)";
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

    private PeriodRange resolveRange(TimeType timeType, int year, int month) {

        if (timeType == TimeType.MONTH) {
            LocalDate s = LocalDate.of(year, 1, 1);
            return new PeriodRange(s.atStartOfDay(), s.plusYears(1).atStartOfDay(), timeType);
        } else {
            LocalDate anchor = LocalDate.of(year, month, 1);
            return new PeriodRange(anchor.atStartOfDay(), anchor.plusMonths(1).atStartOfDay(), timeType);
        }
    }

}

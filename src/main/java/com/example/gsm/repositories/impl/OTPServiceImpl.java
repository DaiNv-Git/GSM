package com.example.gsm.repositories.impl;

import com.example.gsm.dao.*;
import com.example.gsm.entity.ServiceEntity;
import com.example.gsm.repositories.OTPService;
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

import static com.example.gsm.comon.Constants.*;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
@RequiredArgsConstructor
public class OTPServiceImpl implements OTPService {

    private final MongoTemplate mongo;

    private static final String OTP_TYPE = "buy.otp.service";

    @Override
    public ResponseCommon<OtpResponse> getOverview(OtpRequest req) {
        // Validate request
        String error = validateRequest(req);
        if (!error.isEmpty()) {
            return new ResponseCommon<>(CORE_ERROR_CODE, error, null);
        }

        // Resolve time range
        PeriodRange range = resolveRange(req.getTimeType(), req.getYear(), req.getMonth());

        // 1) Revenue Metrics
        OtpResponse.RevenueMetrics revenueMetrics = aggregateRevenueMetrics(range);

        // 2) Bar Chart: Order count by platform (grouped by date)
        List<OtpResponse.PlatformBreakdown> barChartData = aggregateBarChart(range, req.getTimeType());

        // 3) Line Chart: Total revenue trend by timeType
        List<OtpResponse.TimeSeriesItem> lineChartData = aggregateLineChart(range, req.getTimeType());

        // Build response
        OtpResponse response = new OtpResponse(revenueMetrics, barChartData, lineChartData);
        return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, response);

    }


    private String validateRequest(OtpRequest req) {
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

    private OtpResponse.RevenueMetrics aggregateRevenueMetrics(PeriodRange range) {
        Criteria c = Criteria.where("createdAt")
                .gte(toDate(range.start))
                .lt(toDate(range.end))
                .and("type").is(OTP_TYPE);

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

        return new OtpResponse.RevenueMetrics(totalRevenue, netRevenue, commission);
    }

    private List<OtpResponse.PlatformBreakdown> aggregateBarChart(PeriodRange range, TimeType timeType) {
        Criteria c = Criteria.where("createdAt")
                .gte(toDate(range.start))
                .lt(toDate(range.end))
                .and("type").is(OTP_TYPE);

        Aggregation agg = newAggregation(
                match(c),
                project("statusCode")
                        .and("stock.serviceCode").as("serviceName"),
                group("serviceName")
                        .sum(ConditionalOperators.when(Criteria.where("statusCode").is("SUCCESS")).then(1).otherwise(0))
                        .as("orderCountSuccess")
                        .sum(ConditionalOperators.when(Criteria.where("statusCode").is("REFUNDED")).then(1).otherwise(0))
                        .as("orderCountRefund"),
                project("orderCountSuccess", "orderCountRefund")
                        .and("_id").as("serviceName"),
                sort(Sort.Direction.DESC, "orderCountSuccess") // có thể đổi thành total nếu muốn
        );

        List<Document> rows = mongo.aggregate(agg, "orders", Document.class).getMappedResults();
        List<OtpResponse.PlatformBreakdown> barChartData = new ArrayList<>();

        for (Document d : rows) {
            barChartData.add(new OtpResponse.PlatformBreakdown(
                    d.getString("serviceName") != null ? d.getString("serviceName") : "OTHER",
                    d.get("orderCountSuccess") != null ? ((Number) d.get("orderCountSuccess")).longValue() : 0L,
                    d.get("orderCountRefund") != null ? ((Number) d.get("orderCountRefund")).longValue() : 0L
            ));
        }
        return barChartData;
    }

    @Override
    public ResponseCommon<OtpDetailsPagedResponse> getOtpDetails(OtpDetailsRequest req) {

        PeriodRange range = resolveRange(req.getTimeType(), req.getYear(), req.getMonth());

        Criteria c = Criteria.where("createdAt")
                .gte(toDate(range.start))
                .lt(toDate(range.end))
                .and("type").is(OTP_TYPE);

        // Doanh thu tổng
        OtpResponse.RevenueMetrics revenueMetrics = aggregateRevenueMetrics(range);

        // build pipeline tính toán
        ProjectionOperation projService = project("cost", "discountRate", "statusCode", "isRefund", "isActive")
                .and("stock.serviceCode").as("serviceName");

        AggregationOperation groupOp = group("serviceName")
                .count().as("operations")
                .sum(ConditionalOperators.when(Criteria.where("statusCode").is("SUCCESS")).then(1).otherwise(0)).as("successCount")
                .sum(ConditionalOperators.when(Criteria.where("isRefund").is(true)).then(1).otherwise(0)).as("refundCount")
                .sum(ConditionalOperators.when(Criteria.where("isActive").is(false)).then(1).otherwise(0)).as("bannedCount")
                .avg("discountRate").as("avgDiscount")
                .sum("cost").as("income");

        ProjectionOperation projOut = project("operations", "successCount", "refundCount", "bannedCount", "avgDiscount", "income")
                .and("_id").as("serviceName");

        Aggregation agg = newAggregation(
                match(c),
                projService,
                groupOp,
                projOut
        );

        List<Document> rows = mongo.aggregate(agg, "orders", Document.class).getMappedResults();

        List<OtpDetailsResponse> items = new ArrayList<>();
        for (Document d : rows) {
            String serviceName = d.getString("serviceName");
            long operations = d.get("operations") == null ? 0L : ((Number) d.get("operations")).longValue();
            long successCount = d.get("successCount") == null ? 0L : ((Number) d.get("successCount")).longValue();
            long refundCount = d.get("refundCount") == null ? 0L : ((Number) d.get("refundCount")).longValue();
            long bannedCount = d.get("bannedCount") == null ? 0L : ((Number) d.get("bannedCount")).longValue();
            double avgDiscount = d.get("avgDiscount") == null ? 0.0 : ((Number) d.get("avgDiscount")).doubleValue();
            double income = d.get("income") == null ? 0.0 : ((Number) d.get("income")).doubleValue();

            double successRate = operations > 0 ? (100.0 * successCount / operations) : 0.0;

            items.add(new OtpDetailsResponse(
                    serviceName,
                    operations,
                    successCount,
                    refundCount,
                    bannedCount,
                    round2(successRate),
                    round2(avgDiscount),
                    round2(income)
            ));
        }

        // total records
        int total = items.size();

        // paging thủ công
        int pageSize = req.getPageSize() == null ? total : req.getPageSize();
        int pageNumber = req.getPageNumber() == null ? 1 : req.getPageNumber();

        // đảm bảo không out of range
        int fromIndex = Math.max(0, (pageNumber - 1) * pageSize);
        if (fromIndex >= total) {
            fromIndex = 0; // nếu pageNumber quá lớn thì reset về trang đầu
        }
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<OtpDetailsResponse> pagedItems = items.subList(fromIndex, toIndex);

        // đóng gói response
        PagedResponse<OtpDetailsResponse> data = new PagedResponse<>(
                total,
                pageNumber,
                pageSize,
                pagedItems
        );

        return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE,
                new OtpDetailsPagedResponse(revenueMetrics, data));

    }
    @Override
    public ResponseCommon<List<Document>> findServicesByAppName(String name) {
        // Kiểm tra đầu vào
        if (name == null || name.trim().isEmpty()) {
            return new ResponseCommon<>(CORE_ERROR_CODE, "Tên ứng dụng không được rỗng", Collections.emptyList());
        }

        // Chuyển giá trị name thành chữ hoa và thoát các ký tự đặc biệt
        String searchValue = Pattern.quote(name.trim().toUpperCase());

        // Tạo Aggregation với điều kiện tìm kiếm cho cả `text` và `code`
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(
                        new Criteria().orOperator(
                                Criteria.where("text").regex(searchValue, "i"),
                                Criteria.where("code").regex(searchValue, "i")
                        )
                )
        );

        // Thực hiện truy vấn và lấy kết quả
        List<Document> results = mongo.aggregate(agg, "services", Document.class).getMappedResults();


        // Kiểm tra kết quả
        if (results.isEmpty()) {
            return new ResponseCommon<>(SUCCESS_CODE, "Không tìm thấy dịch vụ phù hợp", Collections.emptyList());
        }

        return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, results);
    }
    private List<OtpResponse.TimeSeriesItem> aggregateLineChart(PeriodRange range, TimeType timeType) {
        Criteria c = Criteria.where("createdAt")
                .gte(toDate(range.start))
                .lt(toDate(range.end))
                .and("type").is(OTP_TYPE);

        Aggregation agg = newAggregation(
                match(c),
                project("cost", "createdAt")
                        .andExpression(bucketExpr(range)).as("bucket"),
                group("bucket")
                        .sum("cost").as("totalRevenue"),
                project("totalRevenue").and("_id").as("timeLabel"),
                sort(Sort.Direction.ASC, "timeLabel")
        );

        List<Document> rows = mongo.aggregate(agg, "orders", Document.class).getMappedResults();
        Map<Integer, Document> bucketMap = rows.stream().collect(Collectors.toMap(
                d -> ((Number) d.get("timeLabel")).intValue(),
                d -> d,
                (d1, d2) -> d1,
                HashMap::new
        ));

        List<OtpResponse.TimeSeriesItem> lineChartData = new ArrayList<>();

        if (timeType == TimeType.DAY) {
            List<LocalDate> dates = range.start.toLocalDate().datesUntil(range.end.toLocalDate())
                    .collect(Collectors.toList());
            for (LocalDate date : dates) {
                String label = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                Document doc = bucketMap.getOrDefault(date.getDayOfMonth(), new Document("totalRevenue", 0));
                lineChartData.add(new OtpResponse.TimeSeriesItem(
                        label,
                        round2(((Number) doc.get("totalRevenue")).doubleValue())
                ));
            }
        } else if (timeType == TimeType.WEEK) {
            int weeks = (int) Math.ceil(range.end.toLocalDate().lengthOfMonth() / 7.0);
            for (int w = 0; w < weeks; w++) {
                Document doc = bucketMap.getOrDefault(w, new Document("totalRevenue", 0));
                lineChartData.add(new OtpResponse.TimeSeriesItem(
                        "W" + (w + 1),
                        round2(((Number) doc.get("totalRevenue")).doubleValue())
                ));
            }
        } else { // MONTH
            for (int m = 1; m <= 12; m++) {
                Document doc = bucketMap.getOrDefault(m, new Document("totalRevenue", 0));
                lineChartData.add(new OtpResponse.TimeSeriesItem(
                        LocalDate.of(range.start.getYear(), m, 1)
                                .getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        round2(((Number) doc.get("totalRevenue")).doubleValue())
                ));
            }
        }
        return lineChartData;
    }
    public ResponseCommon<List<ServiceEntity>> advancedFilter(String code,
                                                              String countryCode,
                                                              Boolean isActive,
                                                              Boolean isPrivate,
                                                              Boolean smsSupport,
                                                              Boolean callSupport,
                                                              Integer minPrice,
                                                              Integer maxPrice,
                                                              String timePeriod) {

        List<Criteria> criteriaList = new ArrayList<>();

        // Code và text filter (regex, không phân biệt hoa/thường)
        if (code != null && !code.trim().isBlank()) {
            String escapedCode = Pattern.quote(code.trim().toUpperCase());
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("code").regex(escapedCode, "i"),
                    Criteria.where("text").regex(escapedCode, "i")
            ));
        }

        // Country code filter
        if (countryCode != null && !countryCode.trim().isBlank()) {
            criteriaList.add(Criteria.where("countryCode").is(countryCode.trim()));
        }

        // isActive filter
        if (isActive != null) {
            criteriaList.add(Criteria.where("isActive").is(isActive));
        }

        // isPrivate filter
        if (isPrivate != null) {
            criteriaList.add(Criteria.where("isPrivate").is(isPrivate));
        }

        // SMS support filter
        if (smsSupport != null) {
            criteriaList.add(Criteria.where("supportFeatures.SMSService").is(smsSupport));
        }

        // Call support filter
        if (callSupport != null) {
            criteriaList.add(Criteria.where("supportFeatures.CallService").is(callSupport));
        }

        // Price filter
        if (minPrice != null && maxPrice != null) {
            if (minPrice > maxPrice) {
                return new ResponseCommon<>(CORE_ERROR_CODE, "minPrice phải nhỏ hơn hoặc bằng maxPrice", Collections.emptyList());
            }
            criteriaList.add(Criteria.where("price").gte(minPrice).lte(maxPrice));
        } else if (minPrice != null) {
            criteriaList.add(Criteria.where("price").gte(minPrice));
        } else if (maxPrice != null) {
            criteriaList.add(Criteria.where("price").lte(maxPrice));
        }

        // Time period filter
        LocalDateTime now = LocalDateTime.now();
        Date fromDate = null;
        if (timePeriod != null) {
            switch (timePeriod.trim().toUpperCase()) {
                case "12H":
                    fromDate = Date.from(now.minusHours(12).atZone(ZoneId.systemDefault()).toInstant());
                    break;
                case "24H":
                    fromDate = Date.from(now.minusHours(24).atZone(ZoneId.systemDefault()).toInstant());
                    break;
                case "48H":
                    fromDate = Date.from(now.minusHours(48).atZone(ZoneId.systemDefault()).toInstant());
                    break;
                case "72H":
                    fromDate = Date.from(now.minusHours(72).atZone(ZoneId.systemDefault()).toInstant());
                    break;
                default:
                    return new ResponseCommon<>(CORE_ERROR_CODE, "Thời gian không hợp lệ (12H, 24H, 48H, 72H)", null);
            }
        }
        if (fromDate != null) {
            criteriaList.add(Criteria.where("createdAt").gte(fromDate));
        }

        // Kết hợp tất cả điều kiện
        Criteria finalCriteria = criteriaList.isEmpty() ? new Criteria() : new Criteria().andOperator(criteriaList);

        // Aggregation pipeline
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(finalCriteria),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        // Thực thi truy vấn
        List<ServiceEntity> results = mongo.aggregate(agg, "services", ServiceEntity.class).getMappedResults();

        // Trả về kết quả
        return new ResponseCommon<>(
                SUCCESS_CODE,
                results.isEmpty() ? "Không tìm thấy dịch vụ phù hợp" : SUCCESS_MESSAGE,
                results
        );
    }




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
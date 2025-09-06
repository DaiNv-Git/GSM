package com.example.gsm.service;

import com.example.gsm.dao.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final MongoTemplate mongoTemplate;

    public DashboardResponse getDashboard(String filter, LocalDate date) {
        DashboardResponse response = new DashboardResponse();
        response.setFilter(filter.toUpperCase());

        // --- 1. Xác định khoảng thời gian hiện tại và kỳ trước ---
        LocalDate start, end, prevStart, prevEnd;

        switch (filter.toUpperCase()) {
            case "DAY" -> {
                start = date;
                end = date.plusDays(1);
                prevStart = date.minusDays(1);
                prevEnd = date;
            }
            case "MONTH" -> {
                start = date.withDayOfMonth(1);
                end = start.plusMonths(1);
                prevStart = start.minusMonths(1);
                prevEnd = start;
            }
            default -> { // YEAR
                start = LocalDate.of(date.getYear(), 1, 1);
                end = start.plusYears(1);
                prevStart = start.minusYears(1);
                prevEnd = start;
            }
        }

        Date startDate = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(end.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date prevStartDate = Date.from(prevStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date prevEndDate = Date.from(prevEnd.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // --- 2. Lấy dữ liệu hiện tại và kỳ trước ---
        List<Document> currentAgg = aggregateOrders(filter, startDate, endDate);
        List<Document> prevAgg = aggregateOrders(filter, prevStartDate, prevEndDate);

        long currentTotal = currentAgg.stream().mapToLong(d -> d.getLong("count")).sum();
        long prevTotal = prevAgg.stream().mapToLong(d -> d.getLong("count")).sum();
        double growth = prevTotal > 0 ? ((double) (currentTotal - prevTotal) / prevTotal) * 100 : 100;

        // --- 3. Summary ---
        DashboardResponse.Summary summary = new DashboardResponse.Summary();
        summary.setTotalOtp(sumField(currentAgg, "otp"));
        summary.setTotalRent(sumField(currentAgg, "rent"));
        summary.setTotalSms(sumField(currentAgg, "sms"));
        summary.setTotalCall(sumField(currentAgg, "call"));
        summary.setProxy(sumField(currentAgg, "proxy"));
        summary.setCountry(currentAgg.stream().map(d -> d.getString("country")).filter(Objects::nonNull).distinct().count());
        summary.setGrowthRate(String.format("%.2f%%", growth));
        summary.setTrend(growth >= 0 ? "up" : "down");

        response.setSummary(summary);

        // --- 4. Revenue ---
        double revenue = currentAgg.stream().mapToDouble(d -> d.getDouble("revenue")).sum();
        response.setRevenue(revenue);

        // --- 5. Chart ---
        List<DashboardResponse.ChartPoint> chart = currentAgg.stream().map(d -> {
            DashboardResponse.ChartPoint cp = new DashboardResponse.ChartPoint();
            cp.setLabel(d.getString("label"));
            cp.setCount(d.getLong("count"));
            return cp;
        }).collect(Collectors.toList());
        response.setChart(chart);

        // --- 6. Services ---
        List<DashboardResponse.ServiceInfo> services = new ArrayList<>();
        Map<String, String> serviceNames = Map.of(
                "otp", "OTP",
                "rent", "Rent",
                "sms", "Sms",
                "call", "Call",
                "proxy", "Proxy"
        );

        for (String key : serviceNames.keySet()) {
            long count = sumField(currentAgg, key);
            long prevCount = sumField(prevAgg, key);

            DashboardResponse.ServiceInfo s = new DashboardResponse.ServiceInfo();
            s.setName(serviceNames.get(key));
            s.setCode(key + ".service");
            s.setAmount(count);
            s.setSuccess(count);
            s.setFailed(0L);

            double rate = prevCount > 0 ? ((double) (count - prevCount) / prevCount) * 100 : 100;
            s.setGrowthRate(String.format("%.2f%%", rate));
            s.setTrend(rate >= 0 ? "up" : "down");

            services.add(s);
        }
        response.setServices(services);

        return response;
    }

    private long sumField(List<Document> docs, String field) {
        return docs.stream()
                .mapToLong(d -> d.get(field) != null ? d.getLong(field) : 0L)
                .sum();
    }

    private List<Document> aggregateOrders(String filter, Date startDate, Date endDate) {
        MatchOperation match = Aggregation.match(Criteria.where("createdAt").gte(startDate).lt(endDate));

        ProjectionOperation project = Aggregation.project()
                .and("cost").as("cost")
                .and("type").as("type")
                .and("countryCode").as("country")
                .andExpression("dayOfMonth(createdAt)").as("day")
                .andExpression("month(createdAt)").as("month")
                .andExpression("week(createdAt)").as("week");

        GroupOperation group;
        if ("DAY".equalsIgnoreCase(filter)) {
            group = Aggregation.group("day")
                    .count().as("count")
                    .sum("cost").as("revenue")
                    .sum(ConditionalOperators.when(Criteria.where("type").is("buy.otp.service")).then(1).otherwise(0)).as("otp")
                    .sum(ConditionalOperators.when(Criteria.where("type").is("rent.service")).then(1).otherwise(0)).as("rent")
                    .sum(ConditionalOperators.when(Criteria.where("type").is("sms.service")).then(1).otherwise(0)).as("sms")
                    .sum(ConditionalOperators.when(Criteria.where("type").is("call.service")).then(1).otherwise(0)).as("call")
                    .sum(ConditionalOperators.when(Criteria.where("type").is("proxy.service")).then(1).otherwise(0)).as("proxy")
                    .first("country").as("country");
        } else if ("MONTH".equalsIgnoreCase(filter)) {
            group = Aggregation.group("week")
                    .count().as("count")
                    .sum("cost").as("revenue")
                    .sum(ConditionalOperators.when(Criteria.where("type").is("buy.otp.service")).then(1).otherwise(0)).as("otp")
                    .sum(ConditionalOperators.when(Criteria.where("type").is("rent.service")).then(1).otherwise(0)).as("rent")
                    .sum(ConditionalOperators.when(Criteria.where("type").is("sms.service")).then(1).otherwise(0)).as("sms")
                    .sum(ConditionalOperators.when(Criteria.where("type").is("call.service")).then(1).otherwise(0)).as("call")
                    .sum(ConditionalOperators.when(Criteria.where("type").is("proxy.service")).then(1).otherwise(0)).as("proxy")
                    .first("country").as("country");
        } else { // YEAR
            group = Aggregation.group("month")
                    .count().as("count")
                    .sum("cost").as("revenue")
                    .sum(ConditionalOperators.when(Criteria.where("type").is("buy.otp.service")).then(1).otherwise(0)).as("otp")
                    .sum(ConditionalOperators.when(Criteria.where("type").is("rent.service")).then(1).otherwise(0)).as("rent")
                    .sum(ConditionalOperators.when(Criteria.where("type").is("sms.service")).then(1).otherwise(0)).as("sms")
                    .sum(ConditionalOperators.when(Criteria.where("type").is("call.service")).then(1).otherwise(0)).as("call")
                    .sum(ConditionalOperators.when(Criteria.where("type").is("proxy.service")).then(1).otherwise(0)).as("proxy")
                    .first("country").as("country");
        }

        ProjectionOperation finalProject = Aggregation.project("count", "revenue", "otp", "rent", "sms", "call", "proxy", "country")
                .and("_id").as("label");

        Aggregation agg = Aggregation.newAggregation(match, project, group, finalProject, Aggregation.sort(Sort.by("label")));
        return mongoTemplate.aggregate(agg, "orders", Document.class).getMappedResults();
    }
}

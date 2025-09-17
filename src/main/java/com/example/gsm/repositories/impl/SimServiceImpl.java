package com.example.gsm.repositories.impl;

import com.example.gsm.dao.RentRequest;
import com.example.gsm.dao.ResponseCommon;
import com.example.gsm.dao.SimRequest;
import com.example.gsm.dao.TimeType;
import com.example.gsm.entity.Sim;
import com.example.gsm.repositories.SimService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static com.example.gsm.comon.Constants.CORE_ERROR_CODE;
import static com.example.gsm.comon.Constants.SUCCESS_CODE;

@Service
@RequiredArgsConstructor
public class SimServiceImpl implements SimService {

    private final MongoTemplate mongo;
    @Override
    public ResponseCommon<List<Sim>> getSim(SimRequest req) {
        // Validate request
        String error = validateRequest(req);
        if (!error.isEmpty()) {
            return new ResponseCommon<>(CORE_ERROR_CODE, error, null);
        }

        // Xây dựng điều kiện filter
        PeriodRange range = resolveRange(req);
        Criteria c = buildCriteria(range, req);

        // Aggregation: chỉ match theo điều kiện
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(c)
        );

        // Thực thi query và map về Sim entity
        List<Sim> sims = mongo.aggregate(agg, "sims", Sim.class).getMappedResults();

        return new ResponseCommon<>(SUCCESS_CODE, "SUCCESS", sims);
    }


    private Criteria buildCriteria(PeriodRange range, SimRequest req) {
        Criteria c = new Criteria().orOperator(
                Criteria.where("activeDate").exists(false),
                Criteria.where("activeDate").gte(toDate(range.start)).lt(toDate(range.end))
        );
        
        if (req.getCountryCode() != null && !req.getCountryCode().isEmpty()) {
            // support cả string lẫn array
            c = c.and("countryCode").in(req.getCountryCode());
        }

        return c;
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

    private PeriodRange resolveRange(SimRequest req) {
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
    private String validateRequest(SimRequest req) {
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
}

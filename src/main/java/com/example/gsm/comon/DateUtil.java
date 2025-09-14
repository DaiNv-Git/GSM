package com.example.gsm.comon;

import com.example.gsm.exceptions.BadRequestException;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeFormatter;

import static com.example.gsm.exceptions.ErrorCode.BAD_EXCEPTION;

@Slf4j
public class DateUtil {

    public static final String YYYYMMDD = "yyyyMMdd";
    public static final String DD_MM_YYYY = "dd/MM/yyyy";

    public static final String MM_YYYY = "MM/yyyy";

    public static final String YYYY_MM_DD = "yyyy-MM-dd";

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    public static final String DDMMYYYY_HHMMSS = "dd/MM/yyyy HH:mm:ss";

    public static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

    public static final String DDMMYYYY_HHMM = "dd/MM/yyyy HH:mm";

    public static final String YYYY_MM_DD_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static final String DATETIME_ISO_FORMAT_COMMON = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String NORMAL_DATE = "yyyy-MM-dd";

    private DateUtil() {
        super();
    }

    public static LocalDate parseDate(String dateStr, String dateFormat) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(dateFormat);
        return LocalDate.parse(dateStr, inputFormatter);
    }

    public static String formatFullDateTime(LocalDateTime date) {
        return DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS).format(date);
    }

    public static String parseToDDMMYYYY(String dateStr, String dateFormat) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(dateFormat);
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(YYYY_MM_DD_Z));
        return localDateTime.format(inputFormatter);
    }

    public static ZonedDateTime parseDate(String dateStr) {
        try {
            return ZonedDateTime.parse(dateStr);
        } catch (Exception e) {

            throw new BadRequestException(BAD_EXCEPTION, "Wrong format date: " + dateStr);
        }
    }

    public static LocalDateTime parseLocalDateTime(String dateStr, String dateFormat) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(dateFormat);
        return LocalDateTime.parse(dateStr, inputFormatter);
    }

    public static String convertTimestampToUtc(Long timestamp) {
        try {
            Instant instant = Instant.ofEpochMilli(timestamp);
            LocalDateTime utcLocalDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(YYYY_MM_DD_Z);
            return utcLocalDateTime.format(formatter);
        } catch (Exception e) {
            throw new BadRequestException(BAD_EXCEPTION, "Can not convert {} to UTC format date: " + timestamp);
        }
    }

    public static String convertTimeFormat(String dateTime) {
        DateTimeFormatter newFormat = DateTimeFormatter.ofPattern(YYYY_MM_DD_Z);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DDMMYYYY_HHMM);

        try {
            LocalDateTime time = LocalDateTime.parse(dateTime, formatter)
                    .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
            return time.format(newFormat);
        } catch (Exception e) {

            return null;
        }
    }

    public static LocalDate convertStringToLocalDate(String date, String pattern) {
        if (StringUtils.isBlank(date)) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        try {
            return LocalDate.parse(date, formatter);
        } catch (Exception e) {

            return null;
        }
    }

    public static String convertLocalDateTimeToString(LocalDateTime date, String pattern) {
        if (date == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        try {
            return date.format(formatter);
        } catch (Exception e) {

            return "";
        }
    }

    public static LocalDateTime convertStringToLocalDateTime(String date, String pattern) {
        if (StringUtils.isBlank(date) || "null".equalsIgnoreCase(date)) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        try {
            return LocalDateTime.parse(date, formatter);
        } catch (Exception e) {

            return null;
        }
    }

    public static LocalDateTime convertNowToUTCLocalDateTime() {
        return LocalDateTime.now().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public static LocalDate convertNowToUTCLocalDate() {
        return LocalDateTime.now().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDate();
    }

    public static ZonedDateTime convertNowToUTC() {
        return LocalDateTime.now().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC);
    }

}

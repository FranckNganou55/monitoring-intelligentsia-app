package org.intelligentsia.monitoring.shared.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private DateUtils() {}

    public static String format(LocalDate date) {
        return date == null ? null : date.format(DATE_FMT);
    }

    public static String format(LocalDateTime dt) {
        return dt == null ? null : dt.format(DATETIME_FMT);
    }

    public static LocalDate startOfDay() {
        return LocalDate.now();
    }

    public static LocalDateTime startOfDayTime() {
        return LocalDate.now().atStartOfDay();
    }
}

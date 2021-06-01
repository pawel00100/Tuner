package com.tuner.utils;

import org.quartz.DateBuilder;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class TimezoneUtils {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX");
    private static final DateTimeFormatter fileNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ssX");


    public static Date getDate(Duration duration) {
        return DateBuilder.futureDate((int) duration.toSeconds(), DateBuilder.IntervalUnit.SECOND);
    }

    public static Date getDate(ZonedDateTime time) {
        var localTime = time.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        return java.sql.Timestamp.valueOf(localTime);
    }

    public static String formattedAtLocal(ZonedDateTime time) {
        var localTime = time.withZoneSameInstant(ZoneId.systemDefault());
        return localTime.format(formatter);
    }

    public static String formattedAtLocalForFilename(ZonedDateTime time) {
        var localTime = time.withZoneSameInstant(ZoneId.systemDefault());
        return localTime.format(fileNameFormatter);
    }
}

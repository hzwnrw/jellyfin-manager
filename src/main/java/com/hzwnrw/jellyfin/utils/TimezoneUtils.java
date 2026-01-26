package com.hzwnrw.jellyfin.utils;

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for handling timezone conversions.
 * All times are stored in UTC in the database.
 * Conversions are done when displaying to users and parsing user input.
 */
@Slf4j
public class TimezoneUtils {

    public static final ZoneId UTC = ZoneId.of("UTC");
    public static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    public static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    /**
     * Convert UTC time from database to user's local timezone
     */
    public static ZonedDateTime convertUtcToUserTimezone(ZonedDateTime utcTime, String userTimezone) {
        if (utcTime == null) return null;
        try {
            return utcTime.withZoneSameInstant(ZoneId.of(userTimezone));
        } catch (Exception e) {
            log.warn("Invalid timezone: {}. Using UTC instead.", userTimezone);
            return utcTime;
        }
    }

    /**
     * Convert user input (in their local timezone) to UTC for storage
     */
    public static ZonedDateTime convertUserInputToUtc(String dateTimeString, String userTimezone) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, INPUT_FORMATTER);
            ZonedDateTime userZoneDateTime = localDateTime.atZone(ZoneId.of(userTimezone));
            return userZoneDateTime.withZoneSameInstant(UTC);
        } catch (Exception e) {
            log.error("Failed to parse datetime: {}", dateTimeString, e);
            throw new IllegalArgumentException("Invalid date/time format: " + dateTimeString);
        }
    }

    /**
     * Format UTC time for display in user's timezone
     */
    public static String formatForDisplay(ZonedDateTime utcTime, String userTimezone) {
        if (utcTime == null) return "N/A";
        try {
            ZonedDateTime userTime = convertUtcToUserTimezone(utcTime, userTimezone);
            return userTime.format(DISPLAY_FORMATTER);
        } catch (Exception e) {
            log.error("Failed to format time: {}", utcTime, e);
            return utcTime.format(DISPLAY_FORMATTER);
        }
    }

    /**
     * Validate if timezone string is valid
     */
    public static boolean isValidTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get available timezone IDs (common ones for Malaysia region)
     */
    public static String[] getCommonTimezones() {
        return new String[]{
            "Asia/Kuala_Lumpur",  // Malaysia
            "Asia/Bangkok",        // Thailand, Laos, Cambodia
            "Asia/Singapore",      // Singapore
            "Asia/Hong_Kong",      // Hong Kong
            "Asia/Tokyo",          // Japan
            "Asia/Shanghai",       // China
            "UTC"                  // UTC/GMT
        };
    }
}

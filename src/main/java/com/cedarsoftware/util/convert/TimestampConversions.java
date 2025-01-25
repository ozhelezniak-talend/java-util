package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
final class TimestampConversions {
    private TimestampConversions() {}

    static double toDouble(Object from, Converter converter) {
        Duration d = toDuration(from, converter);
        return BigDecimalConversions.secondsAndNanosToDouble(d.getSeconds(), d.getNano()).doubleValue();
    }
    
    static BigDecimal toBigDecimal(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        Instant instant = timestamp.toInstant();
        return InstantConversions.toBigDecimal(instant, converter);
    }
    
    static BigInteger toBigInteger(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        Instant instant = timestamp.toInstant();
        return InstantConversions.toBigInteger(instant, converter);
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        return timestamp.toInstant().atZone(converter.getOptions().getZoneId()).toLocalDateTime();
    }
    
    static Duration toDuration(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        Instant timestampInstant = timestamp.toInstant();
        return Duration.between(Instant.EPOCH, timestampInstant);
    }

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        ZonedDateTime zdt = ZonedDateTime.ofInstant(timestamp.toInstant(), converter.getOptions().getZoneId());
        return zdt.toOffsetDateTime();
    }

    static Calendar toCalendar(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        Calendar cal = Calendar.getInstance(converter.getOptions().getTimeZone());
        cal.setTimeInMillis(timestamp.getTime());
        return cal;
    }

    static Date toDate(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        Instant instant = timestamp.toInstant();
        return Date.from(instant);
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        return new java.sql.Date(timestamp.getTime());
    }

    static long toLong(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        return timestamp.getTime();
    }

    static String toString(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        int nanos = timestamp.getNanos();

        String pattern;
        if (nanos == 0) {
            pattern = "yyyy-MM-dd'T'HH:mm:ssXXX";  // whole seconds
        } else if (nanos % 1_000_000 == 0) {
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";  // milliseconds
        } else {
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX";  // nanoseconds
        }

        // Timestamps are always UTC internally
        String ts = timestamp.toInstant()
                .atZone(ZoneId.of("Z"))
                .format(DateTimeFormatter.ofPattern(pattern));
        return ts;
    }
    
    static Map<String, Object> toMap(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        long millis = timestamp.getTime();

        // 1) Convert Timestamp -> Instant -> UTC ZonedDateTime
        ZonedDateTime zdt = timestamp.toInstant().atZone(ZoneOffset.UTC);

        // 2) Extract nanoseconds
        int nanos = zdt.getNano(); // 0 to 999,999,999

        // 3) Build the output string in ISO-8601 w/ "Z" at the end
        String formatted;
        if (nanos == 0) {
            // No fractional seconds
            // e.g. 2025-01-01T10:15:30Z
            // Pattern approach:
            formatted = zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        } else if (nanos % 1_000_000 == 0) {
            // Exactly millisecond precision
            // e.g. 2025-01-01T10:15:30.123Z
            int ms = nanos / 1_000_000;
            formatted = zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    + String.format(".%03dZ", ms);
        } else {
            // Full nanosecond precision
            // e.g. 2025-01-01T10:15:30.123456789Z
            formatted = zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    + String.format(".%09dZ", nanos);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.TIMESTAMP, formatted);
        return map;
    }
}
package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

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
final class BigIntegerConversions {
    static final BigInteger BILLION = BigInteger.valueOf(1_000_000_000);
    static final BigInteger MILLION = BigInteger.valueOf(1_000_000);

    private BigIntegerConversions() { }
    
    static BigDecimal toBigDecimal(Object from, Converter converter) {
        return new BigDecimal((BigInteger)from);
    }

    static UUID toUUID(Object from, Converter converter) {
        BigInteger bigInteger = (BigInteger) from;
        if (bigInteger.signum() < 0) {
            throw new IllegalArgumentException("Cannot convert a negative number [" + bigInteger + "] to a UUID");
        }
        StringBuilder hex = new StringBuilder(bigInteger.toString(16));

        // Pad the string to 32 characters with leading zeros (if necessary)
        while (hex.length() < 32) {
            hex.insert(0, "0");
        }

        // Split into two 64-bit parts
        String highBitsHex = hex.substring(0, 16);
        String lowBitsHex = hex.substring(16, 32);

        // Combine and format into standard UUID format
        String uuidString = highBitsHex.substring(0, 8) + "-" +
                highBitsHex.substring(8, 12) + "-" +
                highBitsHex.substring(12, 16) + "-" +
                lowBitsHex.substring(0, 4) + "-" +
                lowBitsHex.substring(4, 16);

        // Create UUID from string
        return UUID.fromString(uuidString);
    }

    static Date toDate(Object from, Converter converter) {
        return Date.from(toInstant(from, converter));
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        return new java.sql.Date(toInstant(from, converter).toEpochMilli());
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        return Timestamp.from(toInstant(from, converter));
    }

    static Calendar toCalendar(Object from, Converter converter) {
        BigInteger epochNanos = (BigInteger) from;
        BigInteger epochMillis = epochNanos.divide(MILLION);
        Calendar calendar = GregorianCalendar.getInstance(converter.getOptions().getTimeZone());
        calendar.setTimeInMillis(epochMillis.longValue());
        return calendar;
    }
    
    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        return toInstant(from, converter).atZone(converter.getOptions().getZoneId());
    }

    static LocalTime toLocalTime(Object from, Converter converter) {
        BigInteger bigI = (BigInteger) from;
        try {
            return LocalTime.ofNanoOfDay(bigI.longValue());
        } catch (Exception e) {
            throw new IllegalArgumentException("Input value [" + bigI + "] for conversion to LocalTime must be >= 0 && <= 86399999999999", e);
        }
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDate();
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDateTime();
    }

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toOffsetDateTime();
    }

    static Instant toInstant(Object from, Converter converter) {
        BigInteger nanoseconds = (BigInteger) from;
        BigInteger[] secondsAndNanos = nanoseconds.divideAndRemainder(BILLION);
        long seconds = secondsAndNanos[0].longValue(); // Total seconds
        int nanos = secondsAndNanos[1].intValue(); // Nanoseconds part
        return Instant.ofEpochSecond(seconds, nanos);
    }

    static Duration toDuration(Object from, Converter converter) {
        BigInteger nanoseconds = (BigInteger) from;
        BigInteger[] secondsAndNanos = nanoseconds.divideAndRemainder(BILLION);
        long seconds = secondsAndNanos[0].longValue();
        int nanos = secondsAndNanos[1].intValue();
        return Duration.ofSeconds(seconds, nanos);
    }
}

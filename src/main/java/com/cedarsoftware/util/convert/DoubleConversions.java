package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

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
final class DoubleConversions {
    private DoubleConversions() { }

    static Instant toInstant(Object from, Converter converter) {
        double d = (Double) from;
        long seconds = (long) d / 1000;
        int nanoAdjustment = (int) ((d - seconds * 1000) * 1_000_000);
        return Instant.ofEpochSecond(seconds, nanoAdjustment);
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDateTime();
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        return toInstant(from, converter).atZone(converter.getOptions().getZoneId());
    }

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        return toInstant(from, converter).atZone(converter.getOptions().getZoneId()).toOffsetDateTime();
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        double milliseconds = (Double) from;
        long millisPart = (long) milliseconds;
        int nanosPart = (int) ((milliseconds - millisPart) * 1_000_000);
        Timestamp timestamp = new Timestamp(millisPart);
        timestamp.setNanos(timestamp.getNanos() + nanosPart);
        return timestamp;
    }
}

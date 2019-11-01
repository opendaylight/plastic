
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.format.SignStyle
import java.time.format.TextStyle
import java.time.temporal.TemporalAccessor

import static com.google.common.base.Preconditions.*
import static java.time.temporal.ChronoField.DAY_OF_MONTH
import static java.time.temporal.ChronoField.DAY_OF_WEEK
import static java.time.temporal.ChronoField.HOUR_OF_DAY
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR
import static java.time.temporal.ChronoField.MONTH_OF_YEAR
import static java.time.temporal.ChronoField.NANO_OF_SECOND
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE
import static java.time.temporal.ChronoField.YEAR


class Times {

    final static ZoneId UTC_ZID = ZoneId.of(ZoneOffset.UTC.getId())
    final static TZ_8601_MISSING = "-00:00"

    final static DateTimeFormatter CISCO_DATE_TIME;
    static {
        // manually code maps to ensure correct data always used
        // (locale data can be changed by application code)
        Map<Long, String> dow = new HashMap<>();
        dow.put(1L, "Mon");
        dow.put(2L, "Tue");
        dow.put(3L, "Wed");
        dow.put(4L, "Thu");
        dow.put(5L, "Fri");
        dow.put(6L, "Sat");
        dow.put(7L, "Sun");
        Map<Long, String> moy = new HashMap<>();
        moy.put(1L, "Jan");
        moy.put(2L, "Feb");
        moy.put(3L, "Mar");
        moy.put(4L, "Apr");
        moy.put(5L, "May");
        moy.put(6L, "Jun");
        moy.put(7L, "Jul");
        moy.put(8L, "Aug");
        moy.put(9L, "Sep");
        moy.put(10L, "Oct");
        moy.put(11L, "Nov");
        moy.put(12L, "Dec");
        CISCO_DATE_TIME = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .parseLenient()
                .appendText(DAY_OF_WEEK, dow)
                .appendLiteral(" ")
                .appendText(MONTH_OF_YEAR, moy)
                .appendLiteral(' ')
                .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                .appendLiteral(' ')
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .optionalStart()
                .appendFraction(NANO_OF_SECOND, 0, 9, true)
                .optionalEnd()
                .appendLiteral(' ')
                .appendZoneText(TextStyle.SHORT)
                .appendLiteral(' ')
                .appendValue(YEAR, 4)
                .toFormatter(ResolverStyle.SMART, IsoChronology.INSTANCE);
    }

    static String fromEpoch(String epoch) {
        return fromEpochWithOffset(epoch, UTC_ZID.toString())
    }

    static String fromEpochWithOffset(String epoch, String offset) {
        checkNotNull(epoch, "Epoch time cannot be missing")
        checkState(!epoch.isEmpty(), "Epoch time cannot be blank")

        long millis = 0

        if (epoch.length() > 10) {
            String fractional = epoch.substring(10)
            millis = Long.parseLong(fractional)
            epoch = epoch.substring(0, 10)
        }

        return fromEpochWithOffset(Long.parseLong(epoch), millis, offset)
    }

    static String fromEpochWithOffset(long epoch, long millis, String offset) {
        checkState(epoch >= 0, "Epoch time cannot be negative")
        checkNotNull(offset, "Time zone cannot be missing")
        checkState(!offset.isEmpty(), "Time zone cannot be blank")

        Instant instant = Instant.ofEpochSecond(epoch, millis*1000*1000) // UTC by definition

        ZoneId zid = ZoneId.of(offset, ZoneId.SHORT_IDS)
        ZonedDateTime zonedDateTime = instant.atZone(zid)

        zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    static enum Ambiguities {
        MissingT,
        MissingZone
    }

    static String fromAmbiguous(String input, Ambiguities... ambiguities) {
        checkNotNull(input, "Input time string cannot be missing")
        checkState(!input.isEmpty(), "Input time string cannot be blank")

        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        String addedZone = ""

        if (ambiguities.contains(Ambiguities.MissingT)) {
            char squashee = input.charAt(10)
            if (squashee == '-' || squashee == ' ' || squashee == 'T' || squashee == 't')
                input = input.substring(0,10) + 'T' + input.substring(11)
            else
                throw new RuntimeException("Could not superimpose time separator T onto "+input)
        }

        if (ambiguities.contains(Ambiguities.MissingZone)) {
            formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            addedZone = TZ_8601_MISSING
        }

        TemporalAccessor parsed = formatter.parse(input)
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(parsed)+addedZone
    }

    static String fromSpecific(String input) {
        checkNotNull(input, "Input time string cannot be missing")
        checkState(!input.isEmpty(), "Input time string cannot be blank")

        DateTimeFormatter formatter = CISCO_DATE_TIME

        TemporalAccessor parsed = formatter.parse(input)
        Instant instant = Instant.from(parsed)

        ZoneId zid = ZoneId.from(parsed)
        ZonedDateTime zonedDateTime = instant.atZone(zid)
        zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    static String now() {
        Instant instant = Instant.now();
        long epochMillis = instant.toEpochMilli()
        fromEpoch(Long.toString(epochMillis))
    }
}

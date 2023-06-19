/*******************************************************************************
 * Copyright 2014 CapitalOne, LLC.
 * Further development Copyright 2022 Sapient Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.publicissapient.kpidashboard.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;

import com.publicissapient.kpidashboard.common.model.application.Week;

import lombok.extern.slf4j.Slf4j;

/**
 * @author narsingh9
 *
 */

/**
 * Date util for common date operations
 */
@Slf4j
public class DateUtil {

	public static final String TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

	public static final String TIME_FORMAT_WITH_SEC = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

	public static final String TIME_FORMAT_WITH_SEC_DATE = "yyyy-MM-dd'T'HH:mm:ssX";

	public static final String ZERO_TIME_ZONE_FORMAT = "T00:00:00.000Z";

	public static final String DISPLAY_DATE_FORMAT = "dd-MMM-yyyy";

	public static final String DATE_FORMAT = "yyyy-MM-dd";

	public static final String BASIC_DATE_FORMAT = "dd-MM-yyyy";

	public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";

	private DateUtil() {
		// to prevent creation on object
	}

	/**
	 * returns the formatted date
	 * 
	 * @param dateTime
	 *            LocalDateTime object
	 * @param format
	 *            response format
	 * @return formatted date
	 */

	public static String dateTimeFormatter(LocalDateTime dateTime, final String format) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
		return dateTime.format(formatter);
	}

	/**
	 * 
	 * @param dateTime
	 *            string date
	 * @param format
	 *            response format
	 * @return parsed date
	 */

	public static Date dateTimeParser(String dateTime, final String format) {
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		Date date = null;
		try {
			date = formatter.parse(dateTime);
		} catch (ParseException e) {
			log.error("Exception while parse date..." + e.getMessage());
		}
		return date;
	}

	/**
	 * 
	 * @param dateTime
	 *            Date object
	 * @param format
	 *            response format
	 * @return formatted date
	 */
	public static String dateTimeFormatter(Date dateTime, final String format) {
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		return formatter.format(dateTime);
	}

	/**
	 * 
	 * @param dateTime
	 *            dateTime
	 * @param fromFormat
	 *            fromFormat
	 * @param toFormat
	 *            toFormat
	 * @return converted date
	 */
	public static String dateTimeConverter(String dateTime, final String fromFormat, final String toFormat) {
		String strDate = null;
		Date date = dateTimeParser(dateTime, fromFormat);
		if (date != null) {
			strDate = dateTimeFormatter(date, toFormat);
		}
		return strDate;
	}

	public static LocalDateTime stringToLocalDateTime(String time, String format) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
		return LocalDateTime.parse(time, formatter);
	}

	public static Week getWeek(LocalDate date) {
		Week week = new Week();
		LocalDate monday = date;
		while (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
			monday = monday.minusDays(1);
		}
		week.setStartDate(monday);
		LocalDate sunday = date;
		while (sunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
			sunday = sunday.plusDays(1);
		}
		week.setEndDate(sunday);
		return week;
	}

	public static boolean isWithinDateRange(LocalDate targetDate, LocalDate startDate, LocalDate endDate) {
		return !targetDate.isBefore(startDate) && !targetDate.isAfter(endDate);
	}

	public static String convertMillisToDateTime(long milliSeconds) {
		return convertMillisToLocalDateTime(milliSeconds).toString();
	}

	public static LocalDateTime convertMillisToLocalDateTime(long milliSeconds) {
		return Instant.ofEpochMilli(milliSeconds).atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	public static DateTime stringToDateTime(String date, String formater) {
		return DateTimeFormat.forPattern(formater).parseDateTime(date);
	}

	public static LocalDate stringToLocalDate(String time, String format) {
		LocalDate formattedDate;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
		try {
			formattedDate = LocalDate.parse(time, formatter);
		} catch (DateTimeParseException dateTimeParseException) {
			formattedDate = OffsetDateTime.parse(time).toLocalDate();
		}
		return formattedDate;
	}

	public static long convertStringToLong(String date) {
		return ZonedDateTime.of(stringToLocalDateTime(date, TIME_FORMAT), ZoneId.systemDefault()).toInstant()
				.toEpochMilli();
	}

	public static LocalDateTime convertingStringToLocalDateTime(String time, String format) {
		Instant timestamp = Instant.parse(time);
		return timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	public static String getFormattedDate(DateTime dateTime1) {
		String date = "";
		if (dateTime1 != null)
			date = dateTime1.toString();
		if (date != null && !date.isEmpty()) {
			try {
				DateTime dateTime = ISODateTimeFormat.dateOptionalTimeParser().parseDateTime(date);
				return ISODateTimeFormat.dateHourMinuteSecondMillis().print(dateTime) + "0000";
			} catch (IllegalArgumentException e) {
				log.error("error while parsing date: {} {}", date, e);
			}
		}

		return "";
	}

	public static String dateTimeConverter(DateTime dateTime, final String fromFormat) {
		String strDate = null;
		Date dateTimeData = dateTimeParser(dateTime.toString(fromFormat), fromFormat);
		if (dateTime != null) {
			strDate = dateTimeFormatter(dateTimeData, DISPLAY_DATE_FORMAT);
		}
		return strDate;
	}

	public static String localDateTimeConverter(LocalDate dateTime) {
		String strDate = null;
		Date dateTimeData = Date.from(dateTime.atStartOfDay(ZoneId.systemDefault()).toInstant());
		if (dateTime != null) {
			strDate = dateTimeFormatter(dateTimeData, DISPLAY_DATE_FORMAT);
		}
		return strDate;
	}

	public static String dateConverter(Date dateTime) {
		String strDate = null;
		if (dateTime != null) {
			strDate = dateTimeFormatter(dateTime, DISPLAY_DATE_FORMAT);
		}
		return strDate;
	}
}
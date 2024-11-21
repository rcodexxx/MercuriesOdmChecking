package com.firsttech.insurance.odmchecking.service.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtil {

	/**
	 * 
	 * @param fmt ex: yyyy-MM-dd HH:mm:ss
	 * @param date ex: new Date()
	 * @return
	 */
	public static String formatDateToString (String fmt, Date date) {
		SimpleDateFormat spdfmt = new SimpleDateFormat(fmt);
		return spdfmt.format(date);
	}
	
	public static boolean isGoodRocDateTime(String rocDateTimeStr) {
		if (rocDateTimeStr == null || rocDateTimeStr.length() != 13) {
			return false;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		
		try {
			// 民國年轉西元年
			int year = Integer.parseInt(rocDateTimeStr.substring(0, 3)) + 1911;
			String westernDateString = year + rocDateTimeStr.substring(3);
			sdf.parse(westernDateString);
			return true;
		} catch (Exception e) {
			return false;
		}
		
	}
	
	/**
	 * 
	 * @param tag: START | END
	 * @return 民國年月日時分秒 yyyMMddhhmmss
	 */
	public static String getROCDateTime(String tag) {
		// 檢查前一小時的案件
		LocalDateTime localDateTime = LocalDateTime.now().minusHours(1);
		
		if (tag.equals("START")) {
			localDateTime = localDateTime.withMinute(0).withSecond(0).withNano(0);
		} else {
			localDateTime = localDateTime.withMinute(59).withSecond(59).withNano(999999999);
		}
		
		// 格式化
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		String rocYear = String.valueOf(localDateTime.getYear() - 1911);
		String result = rocYear + localDateTime.format(formatter).substring(4);
        return result;
    }
}

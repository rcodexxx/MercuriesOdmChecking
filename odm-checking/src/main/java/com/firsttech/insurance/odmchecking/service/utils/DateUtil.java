package com.firsttech.insurance.odmchecking.service.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
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
		LocalDate localDate = LocalDate.now();
        LocalTime localTime = LocalTime.now();

        StringBuilder sb = new StringBuilder();
        sb.append(localDate.getYear() - 1911)
          .append(localDate.getMonthValue())
          .append(localDate.getDayOfMonth());
        sb.append(localTime.getHour());
        sb.append(tag.equals("START") ? "0000" : "5959");

        return sb.toString();
    }
	

}

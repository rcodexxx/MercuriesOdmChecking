package com.firsttech.insurance.odmchecking.service.utils;

import java.text.SimpleDateFormat;
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
}

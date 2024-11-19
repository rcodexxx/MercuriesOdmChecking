package com.firsttech.insurance.odmchecking.service.cronJob;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import com.firsttech.insurance.odmchecking.service.VersionComparingService;

public class OdmVersionComparingJob {
	
	private final static Logger logger = LoggerFactory.getLogger(OdmVersionComparingJob.class);
	
	@Autowired
	private VersionComparingService versionComparingService;
	
	@Scheduled(cron = "0 0/3 * * * ?")
	public void doComparing() {
		
		DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd");
		LocalDate today = LocalDate.now();
		String todayStr = today.format(f);
		String tomorrowStr = today.plusDays(1).format(f);
		todayStr = "20240815";
		tomorrowStr = "20241115";
		logger.info("[CRON JOB] start to do version comparing: {} ~ {}", todayStr, tomorrowStr);
		versionComparingService.doComparing(todayStr, tomorrowStr);
	}
}

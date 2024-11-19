package com.firsttech.insurance.odmchecking.service.cronJob;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import com.firsttech.insurance.odmchecking.service.VersionComparingService;

public class OdmVersionComparingJob {
	
	private final static Logger logger = LoggerFactory.getLogger(OdmVersionComparingJob.class);
	
	@Autowired
	private VersionComparingService versionComparingService;
	
	@Autowired
	private Environment environment;
	
	@Scheduled(cron = "0 0 22 * * ?")
	public void doComparing() {
		String isActivated = environment.getProperty("cron.version.comparing");
		if (isActivated == null || !isActivated.equals("Y")) {
			logger.info("[CRON JOB] 不執行, 因為未在設定檔中啟動版本比對排程 cron.version.comparing: {}", isActivated);
			return;
		}
		
		DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd");
		LocalDate today = LocalDate.now();
		String todayStr = today.format(f);
		String tomorrowStr = today.plusDays(1).format(f);
		logger.info("[CRON JOB] start to do version comparing: {} ~ {}", todayStr, tomorrowStr);
		versionComparingService.doComparing(todayStr, tomorrowStr);
	}
}

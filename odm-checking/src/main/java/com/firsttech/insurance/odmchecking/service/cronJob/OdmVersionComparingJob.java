package com.firsttech.insurance.odmchecking.service.cronJob;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import org.apache.http.ParseException;
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
	
	@Scheduled(cron = "0 5 * * * ?") // 每小時的五分執行
	public void doComparing() throws ParseException, IOException {
		String isActivated = environment.getProperty("cron.version.comparing");
		if (isActivated == null || !isActivated.equals("Y")) {
			logger.info("[CRON JOB] 不執行, 因為未在設定檔中啟動版本比對排程 cron.version.comparing: {}", isActivated);
			return;
		}
		
		String startRocDateTime = this.getROCDateTime("START");
		String endRocDateTime = this.getROCDateTime("END");
		logger.info("[CRON JOB] start to do version comparing: {} ~ {}", startRocDateTime, endRocDateTime);
		versionComparingService.doComparing(startRocDateTime, endRocDateTime);
	}
	
	/**
	 * 
	 * @param tag: START | END
	 * @return 民國年月日時分秒 yyyMMddhhmmss
	 */
	private String getROCDateTime(String tag) {
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

package com.firsttech.insurance.odmchecking.service.cronJob;

import java.io.IOException;
import org.apache.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import com.firsttech.insurance.odmchecking.service.VersionComparingService;
import com.firsttech.insurance.odmchecking.service.utils.DateUtil;

public class OdmVersionComparingJob {
	
	private final static Logger logger = LoggerFactory.getLogger(OdmVersionComparingJob.class);
	
	@Autowired
	private VersionComparingService versionComparingService;
	
	@Autowired
	private Environment environment;
	
//	@Scheduled(cron = "0 5 * * * ?") // 每小時的五分執行
	@Scheduled(cron = "0 0/3 * * * ?")
	public void doComparing() throws ParseException, IOException {
		String isActivated = environment.getProperty("cron.version.comparing");
		if (isActivated == null || !isActivated.equals("Y")) {
			logger.info("[CRON JOB] 不執行, 因為未在設定檔中啟動版本比對排程 cron.version.comparing: {}", isActivated);
			return;
		}
		
		String startRocDateTime = DateUtil.getROCDateTime("START");
		String endRocDateTime = DateUtil.getROCDateTime("END");
		logger.info("[CRON JOB] start to do version comparing: {} ~ {}", startRocDateTime, endRocDateTime);
		versionComparingService.doComparing(startRocDateTime, endRocDateTime);
	}
	
}

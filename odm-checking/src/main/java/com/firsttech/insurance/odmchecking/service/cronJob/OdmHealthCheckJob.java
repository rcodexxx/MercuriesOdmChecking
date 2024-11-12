package com.firsttech.insurance.odmchecking.service.cronJob;

import com.firsttech.insurance.odmchecking.service.EmailService;
import com.firsttech.insurance.odmchecking.service.SmsService;
import com.firsttech.insurance.odmchecking.service.utils.HttpUtil;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;


@Service
public class OdmHealthCheckJob {
	private final static Logger logger = LoggerFactory.getLogger(OdmHealthCheckJob.class);
	
	@Autowired
	private Environment environment;
	
	@Autowired
	private SmsService smsService;

	@Autowired
	private EmailService emailService;
	
	@Scheduled(cron = "0 0/10 * * * ?")
	public void odmHealthChecking() {
		boolean isAlive = false;
		logger.info("[CRON JOB] odmHealthChecking: start to do health checking for ODM");
		
		// 取得要驗證 ODM 的 URL
		String url = this.getODMResUrl();
		if (url == null) {
			logger.info("無法在設定檔中找到正確的 ODM check 連結");
			return;
		}
		
		// 對 ODM 做 health check
        try {
            HttpResponse response = HttpUtil.httpRequestGet(url, new HashMap<>());
			String returnBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			int statusCode = response.getStatusLine().getStatusCode();
			
			if (statusCode == 200) {
				isAlive = true;
			} 
			logger.info("[CRON JOB] odmHealthChecking: ODM checking result => statusCode: {}, body: {}", statusCode, returnBody);

        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | IOException e) {
            logger.info("[CRON JOB] odmHealthChecking: ODM ({}) Health Checking 發生錯誤, 錯誤訊息: {}",
					environment.getProperty("odm.health.check.origin"),
					e.getMessage());
		}

        // 如果確認失敗計送通知
		if (!isAlive) {
			// email 通知
			boolean isEmailSuccess = emailService.sendMail();
			logger.info("[CRON JOB] 提醒EMAIL發送結果: {}", isEmailSuccess ? "成功" : "失敗");
			
			// 簡訊 通知
			smsService.sendSMS();
		} else {
			logger.info("[CRON JOB] 於 {} 確認 ODM 運作正常", new Date());
		}

		logger.info("[CRON JOB] health checking for ODM is finished");
    }
	
	public String getODMResUrl() {
		
		String currentIP = HttpUtil.getCurrentIP();
		String prodMiddle1IP = environment.getProperty("odm.healthcheck.middle.prod.ip1");
		String prodMiddle2IP = environment.getProperty("odm.healthcheck.middle.prod.ip2");
		String uatMiddle1IP = environment.getProperty("odm.healthcheck.middle.uat.ip1");
		String uatMiddle2IP = environment.getProperty("odm.healthcheck.middle.uat.ip2");
		
		String prodOdm1Url = environment.getProperty("odm.healthcheck.target.prod.url1");
		String prodOdm2Url = environment.getProperty("odm.healthcheck.target.prod.url2");
		String uatOdmUrl = environment.getProperty("odm.healthcheck.target.uat.url");
		
		String rtnUrl = null;
		
		if (currentIP == null) {
			logger.info("[CRON JOB] cannot get a correct current IP ...");
			return rtnUrl;
		} else {
			logger.info("[CRON JOB] get a Current IP: {}", currentIP);
		}
		
		if (currentIP.equals(prodMiddle1IP)){
			rtnUrl = prodOdm1Url;
		} else if (currentIP.equals(prodMiddle2IP)){
			rtnUrl = prodOdm2Url;
		} else if (currentIP.equals(uatMiddle1IP) || currentIP.equals(uatMiddle2IP)){
			rtnUrl = uatOdmUrl;
		} else {
			logger.info("[CRON JOB] there is no correct mapping target ODM url ...");
			rtnUrl = null;
		}
		
		logger.info("[CRON JOB] health checking url is: {}", rtnUrl);
		return rtnUrl;
	}
	
	
}

package com.firsttech.insurance.odmchecking.service.cronJob;

import com.firsttech.insurance.odmchecking.service.EmailService;
import com.firsttech.insurance.odmchecking.service.SmsService;
import com.firsttech.insurance.odmchecking.service.utils.HttpUtil;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;


@Service
public class OdmHealthCheckJob {
	private final static Logger logger = LoggerFactory.getLogger(OdmHealthCheckJob.class);
	
	@Autowired
	private Environment environment;
	
	@Autowired
	private SmsService smsService;

	@Autowired
	private EmailService emailService;
	
	@Scheduled(cron = "0 0/5 * * * ?")
	public void odmHealthChecking() {
		boolean isAlive = false;
		logger.info("[CRON JOB] odmHealthChecking: start to do health checking for ODM");
        try {

			String url = environment.getProperty("odm.health.check.origin");
            HttpResponse response = HttpUtil.httpRequestGet(url, new HashMap<>());
			String returnBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.OK.value()) {
				isAlive = true;
				logger.info("[CRON JOB] odmHealthChecking: ODM is working: {}", returnBody);
			} else {
				logger.info("[CRON JOB] odmHealthChecking: ODM is NOT working: {}", returnBody);

			}

        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | IOException e) {
            logger.info("[CRON JOB] odmHealthChecking: ODM ({}) Health Checking 發生錯誤, 錯誤訊息: {}",
					environment.getProperty("spring.sms.phoneNum"),
					e.getMessage());
		}

		if (!isAlive) {
			boolean isSMSSuccess = smsService.sendSMS();
			logger.info("[CRON JOB] 提醒簡訊發送結果: {}", isSMSSuccess ? "成功" : "失敗");
			boolean isEmailSuccess = emailService.sendMail();
			logger.info("[CRON JOB] 提醒EMAIL發送結果: {}", isEmailSuccess ? "成功" : "失敗");
		}

		logger.info("[CRON JOB] health checking for ODM is finished");
    }
}

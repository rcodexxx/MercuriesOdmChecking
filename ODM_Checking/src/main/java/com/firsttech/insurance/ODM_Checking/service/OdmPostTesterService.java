package com.firsttech.insurance.ODM_Checking.service;

import com.firsttech.insurance.ODM_Checking.service.utils.HttpUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

@Service
public class OdmPostTesterService {

	private final static Logger logger = LoggerFactory.getLogger(OdmPostTesterService.class);

	@Autowired
	private Environment environment;

	@Autowired
	private SmsService smsService;

	@Autowired
	private EmailService emailService;

	@PostConstruct
	public void runOnceAtStartup() {
		sendEMail();
//		sendSMS();
//		doCronTest();
	}

	@Scheduled(cron = "0 0/5 * * * ?")
	public void sendSMS() {
		logger.info("[CRON JOB] sendSMS: preparing to send SMS .....");
		boolean isSMSSuccess = smsService.sendSMSTest();
		logger.info("[CRON JOB] sendSMS: 提醒簡訊發送結果: {}", isSMSSuccess ? "成功" : "失敗");
	}
	
	@Scheduled(cron = "0 0/5 * * * ?")
	public void sendEMail() {
		logger.info("[CRON JOB] sendEMail: preparing to send EMAIL .....");
		boolean isEmailSuccess = emailService.sendMail();
		logger.info("[CRON JOB] sendEMail: 提醒EMAIL發送結果: {}", isEmailSuccess ? "成功" : "失敗");
	}

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
			boolean isSMSSuccess = smsService.sendSMSTest();
			logger.info("[CRON JOB] 提醒簡訊發送結果: {}", isSMSSuccess ? "成功" : "失敗");
			boolean isEmailSuccess = emailService.sendMail();
			logger.info("[CRON JOB] 提醒EMAIL發送結果: {}", isEmailSuccess ? "成功" : "失敗");
		}

		logger.info("[CRON JOB] health checking for ODM is finished");
    }



//	@Scheduled(cron = "0 0/10 * * * ?")
//	public void doCronTest () {
//		doTest(null, null);
//	}
	
	public void doTest(String startDate, String endDate) {
		logger.info("[OdmPostTesterService] preparing to do ODM reuslt checking .....");

		TestManager testODM = new TestManager(environment);

		boolean testflag = testODM.getTestFlag();
		String env = testODM.getEnv();

		if (!testflag) {
			logger.info("Test is off");
			return;
		}

		if ("dev".equals(env)) {
			testODM.initTest();

			Runnable NBtest = () -> {
				testODM.createTest("nb", startDate, endDate);
			};
			Runnable TAtest = () -> {
				testODM.createTest("ta", startDate, endDate);
			};

			testODM.executeTest(NBtest);
			testODM.executeTest(TAtest);
			testODM.closeTest();

		}
		logger.info("-------------------------");
	}

}

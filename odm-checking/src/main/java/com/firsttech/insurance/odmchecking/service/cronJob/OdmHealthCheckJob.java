package com.firsttech.insurance.odmchecking.service.cronJob;

import com.firsttech.insurance.odmchecking.service.EmailService;
import com.firsttech.insurance.odmchecking.service.SmsService;
import com.firsttech.insurance.odmchecking.service.utils.FileUtil;
import com.firsttech.insurance.odmchecking.service.utils.HttpUtil;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;


@Service
public class OdmHealthCheckJob {
//	private final static Logger logger = LoggerFactory.getLogger(OdmHealthCheckJob.class);
	
	@Autowired
	private Environment environment;
	
	@Autowired
	private SmsService smsService;

	@Autowired
	private EmailService emailService;
	
	@Scheduled(cron = "0 0/5 * * * ?")
	public void odmHealthChecking() {
		boolean isAlive = false;
		System.out.println("[CRON JOB] odmHealthChecking: start to do health checking for ODM");
		
		// 取得 IP 資訊檔案位置
		String infoFilePath = environment.getProperty("current.ip.info");
		Map<String, String> infoMap = FileUtil.getLocalIpInfo(infoFilePath);
		String currentIP = infoMap.get("local.ip");
		String odmCheckUrl = infoMap.get("target.odm.url"); // 取得要驗證 ODM 的 URL
		System.out.println("[CRON JOB] currentIP: " + currentIP);
		System.out.println("[CRON JOB] odmCheckUrl: " + odmCheckUrl);
		
		if (currentIP == null || odmCheckUrl == null) {
			System.out.println("無法在設定檔中找到正確的 ODM check 連結");
			return;
		}
		
		// 對 ODM 做 health check
        try {
        	odmCheckUrl = odmCheckUrl == null 
        			? environment.getProperty("odm.healthcheck.target.prod.url1") : odmCheckUrl;
            HttpResponse response = HttpUtil.httpRequestGet(odmCheckUrl, new HashMap<>());
			String returnBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			int statusCode = response.getStatusLine().getStatusCode();
			
			if (statusCode == 200) {
				isAlive = true;
			} 
			System.out.println("[CRON JOB] odmHealthChecking: ODM checking result => statusCode: " + statusCode + ", body: " + returnBody);

        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | IOException e) {
            System.out.println("[CRON JOB] odmHealthChecking: ODM (" + odmCheckUrl + ") Health Checking 發生錯誤, 錯誤訊息: " + e.getMessage());
		}

        // 如果確認失敗計送通知
		if (!isAlive) {
			// email 通知
			boolean isEmailSuccess = emailService.sendMail();
			System.out.println("[CRON JOB] 提醒EMAIL發送結果: " + (isEmailSuccess ? "成功" : "失敗"));
			
			// 簡訊 通知
			smsService.sendSMS();
		} else {
			System.out.println("[CRON JOB] 於 " + new Date() + "確認 ODM 運作正常");
		}

		System.out.println("[CRON JOB] health checking for ODM is finished");
    }
	
	
	
	
}

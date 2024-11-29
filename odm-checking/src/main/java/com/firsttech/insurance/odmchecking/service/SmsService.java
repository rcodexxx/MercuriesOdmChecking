package com.firsttech.insurance.odmchecking.service;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.firsttech.insurance.odmchecking.service.utils.FileUtil;
import com.firsttech.insurance.odmchecking.service.utils.HttpUtil;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.net.URLEncoder;

@Service
public class SmsService {

    private final static Logger logger = LoggerFactory.getLogger(SmsService.class);

    @Value("${spring.sms.username}")
    private String userName;

    @Value("${spring.sms.password}")
    private String password;
    
    @Value("${spring.sms.url}")
    private String smsUrl;
    
    @Value("${current.ip.info}")
    private String infoFilePath;
    
    private final HttpUtil httpUtil = new HttpUtil();
    
    public void sendSMS () {
    	// 取得設定檔資訊
    	Map<String, String> infoMap = FileUtil.getLocalIpInfo(infoFilePath);
		String phoneNums = infoMap.get("sms.phoneNums");
		String isEnabled = infoMap.get("sms.isEnabled");
		
    	// 檢核
        if (isEnabled.isEmpty() || !isEnabled.equals("Y")) {
        	logger.info("未於 properties 檔案中啟用 sms 服務");
        	return;
        }
        
        if (phoneNums.isEmpty() || phoneNums.length() < 10) {
        	logger.info("未於 info 設定檔案中設定收簡訊人的電話號碼");
        	return;
        }
        
        // 依收件人電話發送
        String[] phoneNumsArray = phoneNums.split(";");
        logger.info("phoneNumsArray Num: " + phoneNumsArray.length);
        for (String phoneNum : phoneNumsArray) {
        	logger.info("============> phoneNum: " + phoneNum);
        	boolean isThisSuccess = this.doSending(phoneNum);
        	logger.info("簡訊寄送({}) 發送結果: {}", phoneNum, isThisSuccess ? "成功" : "失敗");
        }
        
    }
    
    private boolean doSending (String phoneNum) {
    	boolean isSuccess = false;
    	
    	try {
    		HttpResponse response = httpUtil.httpRequestPost(
    				smsUrl + this.getParamsUrl(phoneNum), 
    				phoneNum, 
    				this.getSMSHeaderMap());
    		String returnBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
			int statusCode = response.getStatusLine().getStatusCode();
			
			logger.info("SMS sending response => statusCode: {}, body: {}", statusCode, returnBody);
    		if (statusCode == HttpStatus.OK.value()) {
    			isSuccess = true;
    		}
    		
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return isSuccess;
    }
    
    private String getParamsUrl (String phoneNum) throws UnsupportedEncodingException {
        String sb = "&username=" + userName +
                "&password=" + password +
                "&dstaddr=" + phoneNum +
                "&smbody=" + this.getODMNotWorkingSmsContent();
    	return sb;
    }
    
    private Map<String, String> getSMSHeaderMap(){
    	Map<String, String> map = new HashMap<>();
    	map.put("Content-Type", "application/x-www-form-urlencoded");
    	return map;
    }

    private String getODMNotWorkingSmsContent () throws UnsupportedEncodingException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDateTimeStr = dateFormat.format(new Date());
        Map<String, String> infoMap = FileUtil.getLocalIpInfo(infoFilePath);
		String currentIP = infoMap.get("local.ip");

        String sb = "親愛的ODM管理者您好，從" + currentIP + "監控排程於" + currentDateTimeStr +
                "發現ODM有異常無法連通狀況，請盡快協助確認處理，謝謝";
        return URLEncoder.encode(sb, "UTF-8");
    }

}

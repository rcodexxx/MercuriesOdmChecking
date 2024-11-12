package com.firsttech.insurance.odmchecking.service;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.firsttech.insurance.odmchecking.service.utils.HttpUtil;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

    @Value("${spring.sms.isEnabled}")
    private String isEnabled;
    
    @Value("${spring.sms.phoneNums}")
    private String phoneNums;

    @Value("${spring.sms.username}")
    private String userName;

    @Value("${spring.sms.password}")
    private String password;
    
    @Value("${spring.sms.url}")
    private String smsUrl;
    
    public void sendSMS () {
    	// 檢核
        if (isEnabled.isEmpty() || !isEnabled.equals("Y")) {
        	logger.info("未於 properties 檔案中啟用 sms 服務");
        	return;
        }
        
        if (phoneNums.isEmpty() || phoneNums.length() < 10) {
        	logger.info("未於 properties 檔案中設定收簡訊人的電話號碼");
        	return;
        }
        
        // 依收件人電話發送
        String[] phoneNumsArray = phoneNums.split(";");
        for (String phoneNum : phoneNumsArray) {
        	boolean isThisSuccess = this.doSending(phoneNum);
        	logger.info("簡訊寄送({}) 發送結果: {}", phoneNum, isThisSuccess ? "成功" : "失敗");
        }
        
    }
    
    private boolean doSending (String phoneNum) {
    	boolean isSuccess = false;
    	
    	try {
    		HttpResponse response = HttpUtil.httpRequestPost(
    				smsUrl + this.getParamsUrl(phoneNum), 
    				phoneNum, 
    				this.getSMSHeaderMap());
    		String returnBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
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
    	String encodedContents = URLEncoder.encode(this.getODMNotWorkingSmsContent(), "UTF-8");
    	
    	StringBuilder sb = new StringBuilder();
    	sb.append("&username=").append(userName);
    	sb.append("&password=").append(password);
    	sb.append("&dstaddr=").append(phoneNum);
    	sb.append("&smbody=").append(encodedContents);
    	return sb.toString();
    }
    
    private Map<String, String> getSMSHeaderMap(){
    	Map<String, String> map = new HashMap<>();
    	map.put("Content-Type", "application/x-www-form-urlencoded");
    	return map;
    }
    
//    private boolean doSendingOrigin (String phoneNum) {
//    	boolean isSuccess = false;
//    	StringBuilder params = new StringBuilder();
//        params.append("username=").append(userName);
//        params.append("&password=").append(password);
//        params.append("&dstaddr=").append(phoneNum);
//        params.append("&smbody=").append(this.getODMNotWorkingSmsContent());
//
//        URL url = null;
//        HttpsURLConnection urlConnection = null;
//        DataOutputStream dos = null;
//
//        try {
//            url = new URL(smsUrl);
//            urlConnection = (HttpsURLConnection) url.openConnection();
//            urlConnection.setRequestMethod("POST");
//            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//            urlConnection.setDoOutput(true);
//            urlConnection.connect();
//
//            dos = new DataOutputStream(urlConnection.getOutputStream());
//            dos.write(params.toString().getBytes("utf-8"));
//            isSuccess = true;
//        } catch (IOException e) {
//            logger.info(e.getMessage());
//        } finally {
//            if (dos != null) {
//                try {
//                    dos.flush();
//                    dos.close();
//                } catch (IOException e) {
//                    logger.info(e.getMessage());
//                }
//            }
//        }
//        
//        return isSuccess;
//    }

    private String getODMNotWorkingSmsContent () {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDateTimeStr = dateFormat.format(new Date());
        String currentIP = HttpUtil.getCurrentIP();
        
        StringBuilder sb = new StringBuilder();
        sb.append("親愛的ODM管理者您好，從").append(currentIP).append("監控排程於").append(currentDateTimeStr)
                .append("發現ODM有異常無法連通狀況，請盡快協助確認處理，謝謝");
        return sb.toString();
    }

}

package com.firsttech.insurance.ODM_Checking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class SmsService {

    private final static Logger logger = LoggerFactory.getLogger(SmsService.class);

    @Value("${spring.sms.phoneNum}")
    private String phoneNum;

    @Autowired
    private Environment environment;
    public boolean sendSMSTest () {
        logger.info("aaaa: " + environment.getProperty("odm.nb.origin"));
        logger.info("phoneNum: " + phoneNum);
        return true;
    }

    public boolean sendSMS () {
        boolean isSuccess = false;

        StringBuilder params = new StringBuilder();
        params.append("username=username");
        params.append("&password=password");
        params.append("&dstaddr=").append(phoneNum);
        params.append("&smbody=").append(this.getODMNotWorkingSmsContent());

        URL url = null;
        HttpsURLConnection urlConnection = null;
        DataOutputStream dos = null;

        try {
            url = new URL("https://211.72.227.230:443/api/mtk/SmSend?CharsetURL=UTF-8");
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setDoOutput(true);
            urlConnection.connect();

            dos = new DataOutputStream(urlConnection.getOutputStream());
            dos.write(params.toString().getBytes("utf-8"));
            isSuccess = true;
        } catch (IOException e) {
            logger.info(e.getMessage());
        } finally {
            if (dos != null) {
                try {
                    dos.flush();
                    dos.close();
                } catch (IOException e) {
                    logger.info(e.getMessage());
                }
            }
        }

        return isSuccess;

    }

    private String getODMNotWorkingSmsContent () {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDateTimeStr = dateFormat.format(new Date());

        StringBuilder sb = new StringBuilder();
        sb.append("親愛的ODM管理者 您好, 監控排程於").append(currentDateTimeStr)
                .append("發現 ODM 有異常無法連通狀況，請盡快協助確認處理，謝謝");
        return sb.toString();
    }

}

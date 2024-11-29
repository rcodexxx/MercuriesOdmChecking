package com.firsttech.insurance.odmchecking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.firsttech.insurance.odmchecking.service.utils.FileUtil;
//import org.thymeleaf.context.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;


@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.sender}")
    private String sender;
    
    @Value("${current.ip.info}")
    private String infoFilePath;

    private final static Logger logger = LoggerFactory.getLogger(EmailService.class);

    public boolean sendMail() {
    	// 取得設定檔資訊
    	Map<String, String> infoMap = FileUtil.getLocalIpInfo(infoFilePath);
		String recipients = infoMap.get("mail.alert.recipients");
		String isEnabled = infoMap.get("mail.isEnabled");
		
    	// 檢核
        if (isEnabled.isEmpty() || !isEnabled.equals("Y")) {
        	logger.info("未於 properties 檔案中啟用 email 服務");
        	return false;
        }
        
        if (recipients.isEmpty() || recipients.length() == 0) {
        	logger.info("未於 info 設定檔案中設定收件人的email");
        	return false;
        }
        
    	// 開始寄信
        boolean isSuccess = false;
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            // 設定寄件人
            helper.setFrom(sender + "@" + mailHost);
            // 設定收件人
            helper.setTo(recipients.split(";"));
            // 信件主旨
            helper.setSubject("ODM 未正常運作請盡快協助處理");
//            Context context = new Context(LocaleContextHolder.getLocale());

            // 設定信件內容為指定文字，並設為HTML格式
            helper.setText(this.getODMNotWorkingEmailContent(), true);

            // 寄信
            mailSender.send(mimeMessage);

            logger.info("[EmailService] Sending Email successful");
            isSuccess = true;
        } catch (MessagingException e) {
            logger.info("[EmailService] Sending Email failed, error message is : " + e.getMessage());
        }

        return isSuccess;
    }

    private String getODMNotWorkingEmailContent () {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDateTimeStr = dateFormat.format(new Date());
        Map<String, String> infoMap = FileUtil.getLocalIpInfo(infoFilePath);
		String currentIP = infoMap.get("local.ip");

        String sb = "親愛的ODM管理者 您好, 從" + currentIP + "監控排程於" + currentDateTimeStr +
                "發現 ODM 有異常無法連通狀況，請盡快協助確認處理，謝謝";
        return sb;
    }
}

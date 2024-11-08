package com.firsttech.insurance.odmchecking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
//import org.thymeleaf.context.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    @Value("${spring.mail.alert.recipients}")
    private String recipients;

    private final static Logger logger = LoggerFactory.getLogger(EmailService.class);

    public boolean sendMail() {
        boolean isSuccess = false;
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            // 設定寄件人
            helper.setFrom(sender + "@" + mailHost);
            // 設定收件人
            helper.setTo(recipients);
            // 信件主旨
            helper.setSubject("ODM 未正常運作請盡快協助處理");
//            Context context = new Context(LocaleContextHolder.getLocale());

            // 設定信件內容為指定文字，並設為HTML格式
            helper.setText(this.getODMNotWorkingEmailContent(), true);

            // 寄信
            mailSender.send(mimeMessage);

            logger.info("[EmailService] Sending Email successful");
        }
        catch (MessagingException e) {
            logger.info("[EmailService] Sending Email failed, error message is : " + e.getMessage());
        }

        return isSuccess;
    }

    private String getODMNotWorkingEmailContent () {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDateTimeStr = dateFormat.format(new Date());

        StringBuilder sb = new StringBuilder();
        sb.append("親愛的ODM管理者 您好, 監控排程於").append(currentDateTimeStr)
                .append("發現 ODM 有異常無法連通狀況，請盡快協助確認處理，謝謝");
        return sb.toString();
    }
}

package com.firsttech.insurance.ODM_Checking.controller;

import com.firsttech.insurance.ODM_Checking.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SmsController {

    @Autowired
    private SmsService smsService;

    @GetMapping("/send-sms")
    public String sendEmail() {
        smsService.sendSMSTest();
        return "SMS sent successfully";
    }
}

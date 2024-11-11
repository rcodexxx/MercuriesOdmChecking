package com.firsttech.insurance.odmchecking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.firsttech.insurance.odmchecking.service.SmsService;

@RestController
@RequestMapping("/api")
public class SmsController {

    @Autowired
    private SmsService smsService;

    @GetMapping("/send-sms")
    public String sendEmail() {
        boolean isSuccess = smsService.sendSMS();
        return "SMS sent result: " + (isSuccess ? "SUCCESSFUL" : "FAIL");
    }
}

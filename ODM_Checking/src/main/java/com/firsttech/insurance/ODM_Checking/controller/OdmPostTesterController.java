package com.firsttech.insurance.ODM_Checking.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import com.firsttech.insurance.ODM_Checking.service.EmailService;
import com.firsttech.insurance.ODM_Checking.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.firsttech.insurance.ODM_Checking.domain.DateRange;
import com.firsttech.insurance.ODM_Checking.service.OdmPostTesterService;

@RestController
@RequestMapping("/api")
public class OdmPostTesterController {
	@Autowired
	private SmsService smsService;
	@Autowired
	private EmailService emailService;
	private OdmPostTesterService odmPostTesterService = new OdmPostTesterService();
	private SimpleDateFormat sdfmt = new SimpleDateFormat("yyyyMMdd");
	
	@PostMapping("/callODMResultChecking")
	public String callODMResultChecking(@RequestBody DateRange dateRange) {
		if (dateRange == null) {
			odmPostTesterService.doTest(null, null);
			return "Default date range testing for ODMResultChecking ...";
		}
		
		try {
			sdfmt.parse(dateRange.getStartDate());
			sdfmt.parse(dateRange.getEndDate());
		} catch (ParseException e) {
			return "Error for input date range";
		}
		
		odmPostTesterService.doTest(dateRange.getStartDate(), dateRange.getEndDate());
		
		return "API callODMResultChecking has done .....";
	}
	
	
	@PostMapping("/sendSMS")
	public String sendSMS() {
		boolean isSuccess = smsService.sendSMS();
		return "API calling sendSMS has done: " + (isSuccess ? "Success" : "Fail");
	}
	
	@PostMapping("/sendEMail")
	public String sendEMail() {
		boolean isSuccess = emailService.sendMail();
		return "API calling sendEMail has done: " + (isSuccess ? "Success" : "Fail");
	}
	
	
}

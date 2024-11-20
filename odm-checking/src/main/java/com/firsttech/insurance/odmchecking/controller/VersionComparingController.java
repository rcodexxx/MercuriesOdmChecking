package com.firsttech.insurance.odmchecking.controller;

import java.text.SimpleDateFormat;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.firsttech.insurance.odmchecking.domain.DateRange;
import com.firsttech.insurance.odmchecking.service.VersionComparingService;

@RestController
@RequestMapping("/api")
public class VersionComparingController {
	private final static Logger logger = LoggerFactory.getLogger(VersionComparingController.class);
	@Autowired
	private VersionComparingService versionComparingService;
	
    @GetMapping("/versionComparing")
    public boolean callODMResultChecking(@RequestBody DateRange dateRange) throws BadRequestException {
    	logger.info("[API] start to do version comparing: {}", dateRange.show());
    	if (dateRange == null 
    			|| this.isGoodRocDateTime(dateRange.getStartDate()) 
    			|| this.isGoodRocDateTime(dateRange.getEndDate())) {
    		throw new BadRequestException("輸入日期不符, 應為民國年 + 月 + 日 + 時 + 分 + 秒 (yyyMMddhhmmss)");
    	}
    	
    	return versionComparingService.doComparing(dateRange.getStartDate(), dateRange.getEndDate());
    }
    
    
	private boolean isGoodRocDateTime(String rocDateTimeStr) {
		if (rocDateTimeStr == null || rocDateTimeStr.length() != 13) {
			return false;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		
		try {
			// 民國年轉西元年
			int year = Integer.parseInt(rocDateTimeStr.substring(0, 3)) + 1911;
			String westernDateString = year + rocDateTimeStr.substring(3);
			sdf.parse(westernDateString);
			return true;
		} catch (Exception e) {
			return false;
		}
		
	}
    
}
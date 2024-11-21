package com.firsttech.insurance.odmchecking.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.firsttech.insurance.odmchecking.domain.DateRange;
import com.firsttech.insurance.odmchecking.service.VersionComparingService;
import com.firsttech.insurance.odmchecking.service.utils.DateUtil;

@RestController
@RequestMapping("/api")
public class VersionComparingController {
	private final static Logger logger = LoggerFactory.getLogger(VersionComparingController.class);
	@Autowired
	private VersionComparingService versionComparingService;
	
    @GetMapping("/versionComparing")
    public boolean callODMResultChecking(@RequestBody DateRange dateRange) {
    	logger.info("[API] start to do version comparing: {}", dateRange.show());
    	
    	if (DateUtil.isGoodRocDateTime(dateRange.getStartDate())) {
    		logger.info("[起始日期時間]格式不符, 應為民國年 + 月 + 日 + 時 + 分 + 秒 (yyyMMddhhmmss): {}", dateRange.getStartDate());
    	}
    	
    	if (DateUtil.isGoodRocDateTime(dateRange.getEndDate())) {
    		logger.info("[結束日期時間]格式不符, 應為民國年 + 月 + 日 + 時 + 分 + 秒 (yyyMMddhhmmss): {}", dateRange.getStartDate());
    	}
    	
    	return versionComparingService.doComparing(dateRange.getStartDate(), dateRange.getEndDate());
    }
	
    
}
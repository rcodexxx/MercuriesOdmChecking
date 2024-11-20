package com.firsttech.insurance.odmchecking.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
	private final static Logger logger = LoggerFactory.getLogger(HomeController.class);
	
    @GetMapping("/hello")
    public String home() {
    	logger.info("GGGGGGGGGGGGGGGGGGGGGGGGGG");
        return "Hello World";
    }
}
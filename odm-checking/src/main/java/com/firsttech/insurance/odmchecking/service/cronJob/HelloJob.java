package com.firsttech.insurance.odmchecking.service.cronJob;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class HelloJob {
	@Scheduled(cron = "0 0/2 * * * ?")
	public void sayHello() {
		System.out.println("Hello ~~~ Hello ~~~ ");
		System.out.println("Hello ~~~ Hello ~~~ ");
		System.out.println("Hello ~~~ Hello ~~~ ");
		System.out.println("Hello ~~~ Hello ~~~ ");
		System.out.println("Hello ~~~ Hello ~~~ ");
		System.out.println("Hello ~~~ Hello ~~~ ");
		System.out.println("Hello ~~~ Hello ~~~ ");
		System.out.println("Hello ~~~ Hello ~~~ ");
		System.out.println("Hello ~~~ Hello ~~~ ");
	}
}

package com.firsttech.insurance.odmchecking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.firsttech.insurance.odmchecking.service.cronJob.OdmHealthCheckJob;
import com.firsttech.insurance.odmchecking.service.cronJob.OdmVersionComparingJob;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class OdmCheckingApplication extends SpringBootServletInitializer {

	@Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        // The Scheduler Class needs to be added manually, because it dont have to be defined as Bean
        return application.sources(OdmCheckingApplication.class, OdmHealthCheckJob.class, OdmVersionComparingJob.class);
    }
	
	public static void main(String[] args) {
		SpringApplication.run(OdmCheckingApplication.class, args);
	}

}

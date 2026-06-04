package com.Basisttha.IronHold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IronHoldApplication {

	public static void main(String[] args) {
		SpringApplication.run(IronHoldApplication.class, args);
	}

}

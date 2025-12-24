package com.ssafy.yumcoach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YumcoachApplication {

	public static void main(String[] args) {
		SpringApplication.run(YumcoachApplication.class, args);
	}

}

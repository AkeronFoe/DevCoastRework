package com.example.devcoast;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DevcoastApplication {

	public static void main(String[] args) {
		SpringApplication.run(DevcoastApplication.class, args);
	}

}

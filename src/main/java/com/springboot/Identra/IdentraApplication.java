package com.springboot.Identra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IdentraApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentraApplication.class, args);
	}
}

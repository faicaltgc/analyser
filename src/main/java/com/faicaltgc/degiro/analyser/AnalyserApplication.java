package com.faicaltgc.degiro.analyser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AnalyserApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnalyserApplication.class, args);
	}

}

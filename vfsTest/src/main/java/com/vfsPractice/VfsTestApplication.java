package com.vfsPractice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableScheduling
@SpringBootApplication
public class VfsTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(VfsTestApplication.class, args);
	}

}

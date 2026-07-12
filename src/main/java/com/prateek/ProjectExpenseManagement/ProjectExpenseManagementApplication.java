package com.prateek.ProjectExpenseManagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProjectExpenseManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectExpenseManagementApplication.class, args);
	}

}

package io.github.ric4r2.jobtracker;

import org.springframework.boot.SpringApplication;

public class TestJobTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.from(JobTrackerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

package io.github.gothsins.hookforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HookforgeApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HookforgeApiApplication.class, args);
	}

}

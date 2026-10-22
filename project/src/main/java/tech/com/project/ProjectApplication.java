package tech.com.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProjectApplication {

	public static void main(String[] args) {
		tech.com.project.config.EnvLoader.loadEnv();
		SpringApplication.run(ProjectApplication.class, args);
	}

}

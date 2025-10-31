package ru.vicsergeev.GetwayUserService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.vicsergeev.GetwayUserService.config.ServicesProperties;

@SpringBootApplication
@EnableConfigurationProperties(ServicesProperties.class)
public class GetwayUserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GetwayUserServiceApplication.class, args);
	}

}

package com.ericmatsuno.smart_fridge_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // picks up @ConfigurationProperties records like FridgeProperties
public class SmartFridgeBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartFridgeBackendApplication.class, args);
	}

}

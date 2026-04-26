package com.banking.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@SpringBootApplication
@OpenAPIDefinition(
		info=@Info(
				title = "The Banking App",
				description = "Backend Rest APIs for The Bank App",
				version = "v1.0",
				contact = @Contact(
						name = "Vishakha Prasad",
						email = "prasadvishakha513@gmail.com",
						url = "https://github.com/Bishakha2502/the_bank_app"
				),
				license = @License(
						name = "The Banking App",
						url = "https://github.com/Bishakha2502/the_bank_app"
				)
		),
		externalDocs = @ExternalDocumentation(
				description = " The Banking Application Documentation.",
				url = "https://github.com/Bishakha2502/the_bank_app"
				)
)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}

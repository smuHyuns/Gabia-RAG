package gabia.internship.god;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GodApplication {

	public static void main(String[] args) {
		SpringApplication.run(GodApplication.class, args);
	}

}

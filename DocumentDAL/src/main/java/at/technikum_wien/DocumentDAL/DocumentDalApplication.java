package at.technikum_wien.DocumentDAL;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DocumentDalApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("GMT+2"));
		SpringApplication.run(DocumentDalApplication.class, args);
	}

}

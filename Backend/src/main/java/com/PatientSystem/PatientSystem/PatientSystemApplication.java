package com.PatientSystem.PatientSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

// Lägg till exkluderingen här!
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class // Exkludera även Hibernate/JPA
})
public class PatientSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(PatientSystemApplication.class, args);
	}

}

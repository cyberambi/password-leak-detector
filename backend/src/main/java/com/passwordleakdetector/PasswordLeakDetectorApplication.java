package com.passwordleakdetector;

import com.passwordleakdetector.config.DatabaseUrlSupport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PasswordLeakDetectorApplication {

    public static void main(String[] args) {
        DatabaseUrlSupport.toDatasourceProperties(System.getenv("DATABASE_URL"))
                .forEach(System::setProperty);
        SpringApplication.run(PasswordLeakDetectorApplication.class, args);
    }
}

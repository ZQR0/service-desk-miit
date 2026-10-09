package rut.miit.servicedesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ServiceDeskApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceDeskApplication.class, args);
    }
}

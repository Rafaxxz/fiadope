package pe.fiadope.api;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FiadoPeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FiadoPeApplication.class, args);
    }

    @PostConstruct
    void zonaHoraria() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
    }
}

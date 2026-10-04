package ar.portal.autoridadmesa;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortalApplication.class, args);
    }

    /** Reloj único del sistema (hora argentina); en los tests se reemplaza por uno controlable. */
    @Bean
    Clock reloj() {
        return Clock.system(ZoneId.of("America/Argentina/Buenos_Aires"));
    }
}

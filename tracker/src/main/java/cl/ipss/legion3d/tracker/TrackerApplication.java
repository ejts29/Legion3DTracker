package cl.ipss.legion3d.tracker;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Este archivo se llama TrackerApplication y es literalmente el motor de arranque de nuestro sistema.
 * Es el punto de entrada principal donde el framework de Spring Boot levanta y ensambla toda la arquitectura 
 * de repositorios, servicios y controladores que hemos construido para Legión 3D.
 * 
 */

@EnableAsync
@SpringBootApplication
public class TrackerApplication {

    @PostConstruct
    public void init(){
        TimeZone.setDefault(TimeZone.getTimeZone("America/Santiago"));
    }

    public static void main(String[] args) {
        // Aquí es donde la magia ocurre y se inicializa todo nuestro backend
        SpringApplication.run(TrackerApplication.class, args);
    }

}
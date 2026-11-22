package mx.edu.utch.productos.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Beans de infraestructura: reloj del sistema (inyectable para pruebas) y metadatos de OpenAPI.
 */
@Configuration
public class AppConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(new Info()
                .title("Microservicio de Productos (HU-01)")
                .description("CRUD RESTful sobre MongoDB con validaciones y manejo global de errores")
                .version("1.0.0"));
    }
}

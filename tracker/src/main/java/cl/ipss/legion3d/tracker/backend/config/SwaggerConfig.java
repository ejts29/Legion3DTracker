package cl.ipss.legion3d.tracker.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CONFIGURACIÓN DE SWAGGER / OPENAPI
 * Swagger es la herramienta que centraliza la documentación interactiva de la API.
 * Permite visualizar, probar y auditar todos los endpoints del sistema, simulando
 * el ciclo completo de desarrollo sin necesidad de herramientas externas como Postman.
 */

@Configuration // Indica a Spring que esta clase contiene definiciones de "beans" para la configuración del sistema.
public class SwaggerConfig {

    /**
     * Define los metadatos globales que aparecerán en la interfaz gráfica de Swagger UI.
     * Este Bean configura el título, versión y descripción técnica del proyecto.
     */
    @Bean
    public OpenAPI customOpenAPI() { 
        return new OpenAPI() 
            .info(new Info()
                // Título oficial según el informe de avance del proyecto.
                .title("API de Legión 3D Tracker") 
                
                // Versión actual del Middleware.
                .version("1.0")
                
                // Definición funcional del sistema para auditoría y logística.
                .description("Middleware logístico diseñado para centralizar y auditar el ciclo de vida de los pedidos de impresión 3D.")
                
                .contact(new Contact()
                    .name("Soporte Legión 3D")
                    
                    /**
                     * NOTA PARA PRODUCCIÓN:
                     * Al realizar el despliegue final, validar que este correo sea el oficial
                     * para atención de clientes externos y soporte técnico de Luis.
                     */
                    .email("pedidolegion3d@gmail.com")));
    }
}
package cl.ipss.legion3d.tracker.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

/**
 * CONFIGURACIÓN WEB MVC: WebConfig
 * Esta clase personaliza el comportamiento de Spring MVC para el proyecto
 * Legión 3D Tracker, actuando como el mapa oficial de accesos del sistema.
 */

// Aplicamos la sugerencia del log para evitar errores de mejora CGLIB (proxyBeanMethods = false)
@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private SecurityInterceptor interceptor;

    /**
     * CONFIGURACIÓN DE RECURSOS EXTERNOS (Soldadura Lógica)
     * Permite que el sistema sirva archivos físicos (STL, PDF, Imágenes) desde una carpeta 
     * externa llamada 'uploads' ubicada en la raíz del proyecto.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Obtenemos la ruta absoluta de la carpeta para asegurar la lectura en Windows/Linux
        String uploadPath = Paths.get("uploads").toAbsolutePath().toUri().toString();
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }

    /**
     * REGISTRO DEL GUARDIA DE SEGURIDAD
     * Define las reglas de acceso para proteger la administración de Legión 3D.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                // 1. RUTAS PROTEGIDAS (Requieren sesión de Administrador)
                .addPathPatterns(
                        "/solicitudes/**",
                        "/api/v1/pedidos/**"
                )
                // 2. RUTAS EXCLUIDAS (Paso libre para clientes y servicios externos)
                .excludePathPatterns(
                        "/login/**",
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/api/v1/webhook/**",
                        "/tracking/**",
                        "/api/v1/pedidos/*/detalles-tecnicos",
                        "/api/v1/pedidos/*/archivos",
                        "/api/v1/pedidos/*/comprobante-pago",
                        "/api/v1/pedidos/*/estado",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
}
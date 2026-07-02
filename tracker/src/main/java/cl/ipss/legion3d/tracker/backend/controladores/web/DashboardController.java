package cl.ipss.legion3d.tracker.backend.controladores.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador principal encargado de gestionar la vista de entrada (Dashboard) del Legión 3D Tracker.
 * 
 * Su función principal es enrutar las peticiones iniciales del usuario hacia el panel de control.
 * Al definir este mapeo mediante un @Controller explícito (en lugar de una redirección estática),
 * garantizamos que la petición atraviese el ciclo de vida completo de Spring MVC.
 * Esto es un requisito técnico indispensable para que el @ControllerAdvice (ControladorGlobal) 
 * intercepte la llamada e inyecte los contadores de estado en el modelo antes de renderizar la pantalla.
 */
@Controller
public class DashboardController {

    /**
     * Atiende las peticiones HTTP GET dirigidas tanto a la raíz del dominio como a la ruta específica /index.
     * 
     * @return El nombre lógico de la vista ("index"). El ViewResolver de Spring Boot 
     *         procesará este retorno y servirá la plantilla ubicada en src/main/resources/templates/index.html.
     */
    @GetMapping({"/", "/index"})
    public String index() {
        return "index";
    }
}
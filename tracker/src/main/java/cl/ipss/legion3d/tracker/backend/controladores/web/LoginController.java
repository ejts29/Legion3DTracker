package cl.ipss.legion3d.tracker.backend.controladores.web;

import cl.ipss.legion3d.tracker.backend.servicios.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador encargado de gestionar la autenticación y el acceso al panel administrativo.
 * Implementa un sistema de login ligero basado en sesiones de servlets (HttpSession),
 * ideal para manejar el acceso seguro del administrador principal al sistema Tracker.
 */
@Controller
public class LoginController {

    // Se inyecta la contraseña maestra directamente desde el archivo application.properties
    @Value("${app.admin.password}")
    private String adminPass;

    // Se inyecta el correo corporativo del administrador para la recuperación de credenciales
    @Value("${app.admin.email}")
    private String adminEmail;

    // Servicio de correo para enviar notificaciones de recuperación de contraseña
    @Autowired
    private EmailService emailService;

    /**
     * Muestra el formulario inicial de inicio de sesión.
     * 
     * @return El nombre de la vista Thymeleaf correspondiente a la pantalla de login (login.html).
     */
    @GetMapping("/login")
    public String mostrarLogin() { 
        return "login"; 
    }

    /**
     * Procesa el intento de inicio de sesión validando la contraseña ingresada contra la propiedad del sistema.
     * 
     * @param password La clave ingresada por el usuario en el formulario web.
     * @param session  La sesión HTTP actual donde se guardará el flag de autorización si la validación es exitosa.
     * @param model    El modelo utilizado para enviar mensajes de error de vuelta a la vista en caso de fallo.
     * @return Una redirección al panel principal de producción en caso de éxito o recarga la vista de login con un error.
     */
    @PostMapping("/login")
    public String procesarLogin(@RequestParam String password, HttpSession session, Model model) {
        // Verifica si la credencial proporcionada coincide con la configuración del servidor
        if (adminPass.equals(password)) {
            // Habilita el acceso marcando la sesión como válida con el atributo "admin_session"
            session.setAttribute("admin_session", true);
            // Redirige directamente a la vista de producción para agilizar el flujo de trabajo
            return "redirect:/solicitudes/produccion";
        }
        
        // Retorna al formulario indicando al usuario que la validación ha fallado
        model.addAttribute("error", "Contraseña incorrecta");
        return "login";
    }

    /**
     * Maneja la solicitud de recuperación de contraseña enviando un correo al administrador.
     * La anotación @ResponseBody indica que este método devuelve una respuesta de texto plano 
     * en lugar de renderizar una vista HTML completa (muy útil para llamadas asíncronas desde el frontend).
     * 
     * @return Mensaje de confirmación en texto indicando el estado del envío.
     */
    @PostMapping("/login/forgot")
    @ResponseBody
    public String recuperarClave() {
        String msj = "Hola Luis,\n\nHas solicitado recordar tu clave de acceso al Tracker.\n" +
                     "Tu contraseña actual es: " + adminPass + "\n\n" +
                     // (aqui modificar al desplegar a produccion: actualizar el nombre de la empresa en la firma del correo de rescate)
                     "Legión 3D System.";
        // (aqui modificar al desplegar a produccion: asegurar que el asunto del correo sea el institucional para rescate de credenciales)
        emailService.enviarCorreoSimple(adminEmail, "Rescate de Clave - Legión 3D", msj);
        return "Se ha enviado la clave a tu correo corporativo.";
    }
}
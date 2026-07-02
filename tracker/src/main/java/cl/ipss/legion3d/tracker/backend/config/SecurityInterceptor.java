package cl.ipss.legion3d.tracker.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Esta es nuestra seguridad: SecurityInterceptor
 * Esta clase actúa como un "Guardia de Seguridad" en la entrada del servidor.
 * Verifica cada solicitud antes de que llegue a los controladores.
 */
@Component
public class SecurityInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        HttpSession session = request.getSession();

        // Verificamos si existe la sesión activa del administrador
        if (session.getAttribute("admin_session") == null) {
            response.sendRedirect("/login");
            return false;
        }

        return true;
    }
}
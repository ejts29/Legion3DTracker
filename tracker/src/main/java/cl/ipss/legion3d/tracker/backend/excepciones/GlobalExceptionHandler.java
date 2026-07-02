package cl.ipss.legion3d.tracker.backend.excepciones;

import cl.ipss.legion3d.tracker.backend.dtos.ErrorRespuestaDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Este archivo se llama GlobalExceptionHandler y funciona como nuestro Centro
 * de Emergencias centralizado.
 * A nivel arquitectónico, la anotación @ControllerAdvice nos permite capturar
 * cualquier error que ocurra
 * en todo el sistema antes de que llegue al usuario o haga colapsar la
 * aplicación.
 * 
 * Lo diseñamos de forma híbrida e inteligente: es capaz de devolver respuestas
 * estructuradas en formato JSON
 * para proteger nuestra API (por ejemplo cuando el webhook envía datos
 * inválidos) y también renderizar
 * páginas HTML amigables para proteger la experiencia de Luis en su panel de
 * administración.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------
    // ESCENARIO 1: ERRORES DE VALIDACIÓN
    // Aquí es donde nuestro sistema atrapa las validaciones estrictas de los DTOs
    // (como campos vacíos o correos mal escritos).
    // En lugar de que el servidor colapse, interceptamos la falla y devolvemos un
    // JSON limpio explicando qué campo falló.
    // -------------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarErroresDeValidacion(MethodArgumentNotValidException ex,
            WebRequest request) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        // Construimos un DTO de respuesta con un mensaje claro para el cliente (en este
        // caso, el webhook de Stripe) y un código HTTP 400.
        ErrorRespuestaDTO errorRespuesta = new ErrorRespuestaDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Validación Fallida",
                "Revisa los campos del formulario: " + errores.toString(),
                request.getDescription(false));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRespuesta);
    }

    // --------------------------------------------------------------------------------
    // ESCENARIO 2: INTEGRIDAD DE DATOS (Nuestro escudo protector de la base de
    // datos)
    // Se activa automáticamente si se intenta borrar un cliente que tiene pedidos
    // activos.
    // MySQL bloquea la acción para no romper la trazabilidad (y nuestro borrado
    // lógico), y nosotros transformamos
    // ese choque técnico en una vista HTML comprensible para el administrador.
    // ----------------------------------------------------------------------------------------------------
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView manejarErrorDeIntegridad(DataIntegrityViolationException ex) {
        ModelAndView mav = new ModelAndView("error"); // Redirige a error.html
        mav.addObject("titulo", "Acción Protegida por Seguridad");
        mav.addObject("mensaje",
                "No es posible eliminar este registro porque existen pedidos o datos técnicos vinculados a él en la base de datos.");
        return mav;
    }

    // --------------------------------------------------------------------------------
    // ESCENARIO 3: ERRORES GENERALES O INESPERADOS (La red de seguridad final)
    // Este método atrapa cualquier fallo imprevisto. Le muestra la pantalla
    // amigable de Legión 3D a Luis
    // para no asustarlo con códigos de error, mientras guarda el problema real en
    // nuestra consola de desarrollo.
    // ----------------------------------------------------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public Object manejarTodoTipoDeErrores(Exception ex, HttpServletRequest request) {
        // Logueamos el error para diagnóstico
        System.err.println("❌ LOG TÉCNICO: Error procesando la petición [" + request.getRequestURI() + "]: " + ex.getMessage());

        String uri = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");

        // Condición: Si la ruta empieza con /api/ o el cliente explícitamente pide JSON
        if (uri.startsWith("/api/") || (acceptHeader != null && acceptHeader.contains("application/json"))) {
            // Devolvemos JSON limpio para evitar colisiones de serialización
            Map<String, String> errorJson = new HashMap<>();
            errorJson.put("error", "Error interno del servidor");
            errorJson.put("mensaje", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorJson);
        }

        // Condición: Si es navegación web estándar, devolvemos la vista Thymeleaf
        ModelAndView mav = new ModelAndView();
        mav.setViewName("error");
        mav.addObject("titulo", "Inconveniente en el Sistema");
        mav.addObject("mensaje", "El sistema ha detectado un comportamiento inesperado. No te preocupes, tus datos están a salvo. Por favor, intenta la acción nuevamente.");
        return mav;
    }
}
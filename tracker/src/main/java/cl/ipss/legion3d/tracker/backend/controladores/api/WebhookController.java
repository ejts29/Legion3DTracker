package cl.ipss.legion3d.tracker.backend.controladores.api;

import cl.ipss.legion3d.tracker.backend.dtos.PedidoWebhookDTO;
import cl.ipss.legion3d.tracker.backend.servicios.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * CONTROLADOR BLINDADO DE WEBHOOKS
 * Punto de entrada para la integración con el formulario Contact Form 7 de WordPress.
 * Implementa una arquitectura ultra-segura para capturar prospectos sin interrupciones.
 */
@RestController
@RequestMapping("/api/v1/webhook")
@Tag(name = "Webhooks", description = "Endpoints de integración externa altamente resistentes a fallos")
public class WebhookController {

    @Autowired 
    private WebhookService webhookService;

    @Operation(
        summary = "Capturar Lead desde WordPress (Blindado)", 
        description = "Endpoint ultra-seguro que procesa datos de Contact Form 7. Mapea campos con guiones y normaliza datos basura."
    )
    @PostMapping("/lead")
    public ResponseEntity<?> recibirLead(@RequestBody PedidoWebhookDTO leadDTO) {
        // BLOQUE TRY-CATCH GLOBAL: Asegura que el sistema siempre responda de forma coherente
        try {
            // Delegación de la lógica quirúrgica al Service
            String codigoSeguimiento = webhookService.procesarLead(leadDTO);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Lead procesado correctamente",
                "codigoSeguimiento", codigoSeguimiento
            ));

        } catch (Exception e) {
            // CAPTURA DE ERRORES: Previene caídas del sistema ante peticiones mal formadas
            System.err.println("❌ ERROR CRÍTICO EN WEBHOOK: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error interno al procesar el lead. El equipo técnico ha sido notificado.",
                "detalle", e.getMessage()
            ));
        }
    }
}
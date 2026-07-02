package cl.ipss.legion3d.tracker.backend.controladores.api;

import cl.ipss.legion3d.tracker.backend.entidades.Pago;
import cl.ipss.legion3d.tracker.backend.servicios.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/pagos")
@Tag(name = "Pagos", description = "Endpoint para la gestión financiera y comprobantes")
public class PagoController {

    @Autowired
    private PagoService pagoService;

    /**
     * ENDPOINT: Registro de Pago con Comprobante (Cloud Storage)
     * Procesa el abono, realiza la conversión de Bruto a Neto e integra
     * el archivo en la jerarquía dinámica de Google Drive.
     */
    @PostMapping(value = "/{pedidoId}/comprobante", consumes = "multipart/form-data")
    @Operation(summary = "Registrar pago con comprobante", description = "Sube el archivo a Drive y registra el abono neto en la base de datos.")
    public ResponseEntity<?> registrarPagoConComprobante(
            @PathVariable Long pedidoId,
            @RequestParam("montoBruto") Double montoBruto,
            @RequestParam("metodoPago") String metodoPago,
            @RequestParam("comprobante") MultipartFile comprobante) {
        
        try {
            Pago pago = pagoService.registrarPagoConComprobante(pedidoId, montoBruto, metodoPago, comprobante);
            return ResponseEntity.ok(pago);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al registrar el pago: " + e.getMessage());
        }
    }

    /**
     * ENDPOINT: Registro de Pago Manual (Efectivo / Presencial)
     * Permite registrar abonos sin necesidad de adjuntar un comprobante físico.
     */
    @PostMapping(value = "/{pedidoId}/pago-manual", produces = "application/json")
    @Operation(summary = "Registrar pago manual", description = "Registra un abono en efectivo o presencial sin archivo adjunto.")
    public ResponseEntity<?> registrarPagoManual(
            @PathVariable Long pedidoId,
            @RequestParam("monto") Double monto,
            @RequestParam("metodoPago") String metodoPago,
            @RequestParam(value = "concepto", defaultValue = "Pago Manual Directo") String concepto) {

        try {
            pagoService.registrarPagoManual(pedidoId, monto, metodoPago, concepto);
            return ResponseEntity.ok(java.util.Collections.singletonMap("mensaje", "Pago manual registrado con éxito"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Collections.singletonMap("error", e.getMessage()));
        }
    }
}

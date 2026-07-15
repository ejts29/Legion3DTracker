package cl.ipss.legion3d.tracker.backend.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * DTO Blindado y Bilingüe para la integración de Prospectos (Leads).
 * ----------------------------------------------------------------------------
 * A nivel de arquitectura, cambiamos @JsonProperty por @JsonAlias.
 * ¿Por qué? Porque @JsonProperty fuerza a que el JSON entrante tenga UN solo
 * nombre exacto,
 * lo que hacía chocar la entrada manual del dashboard cuando Luis llenaba el
 * formulario.
 * * @JsonAlias actúa como un "embudo bilingüe": le permite a Spring Boot
 * aceptar
 * tanto el formato con guiones que envía WordPress Contact Form 7 ("tu-email")
 * como el formato limpio que envía nuestro Frontend de Administración
 * ("email").
 * * Mantenemos String en todos los campos y el ignoreUnknown=true para evitar
 * errores 400 (Bad Request) ante datos inesperados o basura técnica.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO especializado para capturar prospectos desde múltiples orígenes (WordPress o Panel Manual) de forma ultra-segura")
public class PedidoWebhookDTO {

    @JsonAlias({ "tu-nombre", "nombre" })
    @Schema(description = "Nombre del cliente", example = "Nombre Apellido1 Apellido2")
    private String nombre;

    @JsonAlias({ "tu-email", "email" })
    @Schema(description = "Email del cliente (Identificador Único y Llave Maestra)", example = " Ejemplo@gmail.com")
    private String email;

    @JsonAlias({ "tu-telefono", "telefono" })
    @Schema(description = "Teléfono de contacto (Opcional)", example = "+56912345678")
    private String telefono;

    @JsonAlias({ "servicio", "servicioSolicitado" })
    @Schema(description = "Servicio solicitado", example = "Impresión 3D")
    private String servicio;

    @JsonAlias({ "tiene-archivo-3d", "tieneArchivo3d", "tieneArchivo" })
    @Schema(description = "Flag de archivo (Si/No)", example = "Si")
    private String tieneArchivo3d;

    @JsonAlias({ "google_drive", "linkArchivo", "link" })
    @Schema(description = "Enlace al archivo (Puede llegar con puntos o vacío)", example = "https://drive.google.com/...")
    private String linkArchivo;

    @JsonAlias({ "tu-mensaje", "mensaje", "descripcion" })
    @Schema(description = "Mensaje original del requerimiento o nota manual", example = "Hola, necesito imprimir esta pieza...")
    private String mensaje;

    @JsonAlias({ "como-supo", "origen", "origenContacto" })
    @Schema(description = "Origen del contacto", example = "Entrada Manual (Admin)")
    private String origenContacto;
}
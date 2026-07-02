package cl.ipss.legion3d.tracker.backend.servicios;

import cl.ipss.legion3d.tracker.backend.dtos.PedidoWebhookDTO;
import cl.ipss.legion3d.tracker.backend.entidades.Cliente;
import cl.ipss.legion3d.tracker.backend.entidades.HistorialEstado;
import cl.ipss.legion3d.tracker.backend.entidades.Pedido;
import cl.ipss.legion3d.tracker.backend.repositorios.ClienteRepository;
import cl.ipss.legion3d.tracker.backend.repositorios.HistorialEstadoRepository;
import cl.ipss.legion3d.tracker.backend.repositorios.PedidoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * SERVICIO QUIRÚRGICO DE WEBHOOKS
 * Encargado de procesar leads externos con máxima tolerancia a fallos y normalización de datos.
 */
@Service
public class WebhookService {

    @Autowired private ClienteRepository clienteRepo;
    @Autowired private PedidoRepository pedidoRepo;
    @Autowired private HistorialEstadoRepository historialRepo;
    @Autowired private EmailService emailService;
    @Autowired private GoogleDriveService driveService;

    @Transactional
    public String procesarLead(PedidoWebhookDTO dto) {
        
        // 1. GESTIÓN DE CLIENTES (Upsert Seguro)
        Optional<Cliente> clienteOpt = clienteRepo.findByEmail(dto.getEmail());
        Cliente clienteActual;

        if (clienteOpt.isPresent()) {
            clienteActual = clienteOpt.get();
            // Actualizamos el teléfono solo si llega un dato válido y el anterior era nulo o diferente
            if (dto.getTelefono() != null && !dto.getTelefono().isBlank()) {
                clienteActual.setTelefono(dto.getTelefono());
                clienteRepo.save(clienteActual);
            }
        } else {
            clienteActual = new Cliente();
            clienteActual.setNombre(dto.getNombre());
            clienteActual.setEmail(dto.getEmail());
            clienteActual.setTelefono(dto.getTelefono());
            clienteActual = clienteRepo.save(clienteActual);
        }

        // 2. CREACIÓN DE PEDIDO CON BLINDAJE LÓGICO
        Pedido pedido = new Pedido();
        pedido.setCliente(clienteActual);
        
        // Generación de Código Único NOT NULL
        String codigo = "LEG-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        pedido.setCodigoSeguimiento(codigo);

        // Mapeo de Servicios y Mensaje
        pedido.setServicioSolicitado(dto.getServicio());
        pedido.setMensajeOriginal(dto.getMensaje());
        pedido.setOrigenContacto(dto.getOrigenContacto());
        pedido.setEstadoActual("NUEVA");

        // Conversión Lógica de Flag de Archivo (Si/No -> 1/0)
        boolean tieneArchivo = "Si".equalsIgnoreCase(dto.getTieneArchivo3d());
        pedido.setTieneArchivoInicial(tieneArchivo);

        // Limpieza Quirúrgica de Enlaces (Evitar "puntitos" o basura técnica)
        String linkRaw = dto.getLinkArchivo();
        if (linkRaw == null || linkRaw.isBlank() || linkRaw.equals(".") || linkRaw.length() < 5) {
            pedido.setLinkArchivoInicial(null);
        } else {
            pedido.setLinkArchivoInicial(linkRaw);
        }

        // Persistencia en base de datos MySQL
        pedido = pedidoRepo.save(pedido);

        // AUTOMATIZACIÓN EN INGRESO DE LEADS: Duplicar archivo inicial a la carpeta destino
        String linkArchivo = dto.getLinkArchivo();
        if (linkArchivo != null && !linkArchivo.isBlank() && !".".equals(linkArchivo.trim())) {
            try {
                String fileId = extraerIdDeDrive(linkArchivo);
                if (fileId != null) {
                    // Copiar de manera lógica dentro de la carpeta destino de ingeniería: 1DPTS0dskr6p3Y57ctDq6Ggl_cTR182Bc
                    String nuevoNombre = pedido.getCodigoSeguimiento() + "-wps-" + (clienteActual.getNombre() != null ? clienteActual.getNombre().replace(" ", "_") : "archivo");
                    GoogleDriveService.DriveUploadResult result = driveService.copiarArchivo(fileId, nuevoNombre, "1DPTS0dskr6p3Y57ctDq6Ggl_cTR182Bc");
                    if (result != null) {
                        pedido.setLinkArchivoInicial(result.getWebViewLink());
                        pedido = pedidoRepo.save(pedido);
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ Fallo en automatización de copiado de Drive al ingreso: " + e.getMessage());
            }
        }

        // 3. REGISTRO EN HISTORIAL (Trazabilidad)
        HistorialEstado historial = new HistorialEstado();
        historial.setPedido(pedido);
        historial.setEstadoNuevo("NUEVA");
        historial.setComentario("Ingreso automatizado desde Webhook WordPress.");
        historialRepo.save(historial);

        // 4. NOTIFICACIÓN AUTOMÁTICA
        enviarCorreoBienvenida(clienteActual, codigo);

        return codigo;
    }

    private void enviarCorreoBienvenida(Cliente cliente, String codigo) {
        try {
            String msj = "Hola " + cliente.getNombre() + ",\n\n"
                       + "Hemos recibido tu solicitud en Legión 3D. Tu Código de Seguimiento es: " + codigo + ".\n"
                       + "Puedes revisar el estado de tu pedido en nuestro portal de seguimiento.\n\n"
                       + "Saludos,\nEl equipo de Legión 3D";
            emailService.enviarCorreoSimple(cliente.getEmail(), "🟢 Solicitud Recibida - Legión 3D Tracker", msj);
        } catch (Exception e) {
            // No bloqueamos el proceso principal si falla el correo
            System.err.println("⚠️ Error al enviar correo de bienvenida: " + e.getMessage());
        }
    }

    private String extraerIdDeDrive(String link) {
        if (link == null || link.isBlank())
            return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:/d/|id=|folders/)([\\w-]+)");
        java.util.regex.Matcher matcher = pattern.matcher(link);
        return matcher.find() ? matcher.group(1) : null;
    }
}

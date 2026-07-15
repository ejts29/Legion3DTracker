package cl.ipss.legion3d.tracker.backend.servicios;

import cl.ipss.legion3d.tracker.backend.entidades.Pago;
import cl.ipss.legion3d.tracker.backend.entidades.Pedido;
import cl.ipss.legion3d.tracker.backend.entidades.OrigenPago;
import cl.ipss.legion3d.tracker.backend.repositorios.PagoRepository;
import cl.ipss.legion3d.tracker.backend.repositorios.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * SERVICIO DE PAGOS Y AUDITORÍA FINANCIERA
 * Centraliza toda la lógica de negocio relacionada con abonos, cálculos de saldos
 * e integración de comprobantes en la nube.
 * Todas las operaciones de escritura están protegidas por @Transactional para
 * evitar inconsistencias en la base de datos en caso de fallos.
 */
@Service
public class PagoService {

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private GoogleDriveService googleDriveService;

    /**
     * Calcula el total de dinero (Neto) que ha abonado un cliente a un pedido específico.
     */
    public Double sumarAbonosPorPedido(Long pedidoId) {
        Double total = pagoRepository.sumarAbonosPorPedido(pedidoId);
        return (total != null) ? total : 0.0;
    }

    /**
     * REGISTRO DE PAGO CON COMPROBANTE (Flujo Principal)
     * Este método es invocado cuando el cliente sube su foto desde la plataforma.
     * Implementa almacenamiento jerárquico en la nube y persistencia inmutable.
     */
    @Transactional
    public Pago registrarPagoConComprobante(Long pedidoId, Double montoBruto, String metodoPago,
            org.springframework.web.multipart.MultipartFile comprobante) throws java.io.IOException {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        // 1. Matemática Inversa (De Bruto a Neto)
        // El cliente paga el total con IVA o comisiones, pero la base de datos siempre debe
        // registrar el valor técnico (Neto) para mantener la cuadratura financiera.
        Double montoNeto = Math.round((montoBruto / 1.19) * 100.0) / 100.0;

        // 2. Almacenamiento Seguro en Drive
        // Se invoca "subirComprobantePago" para asegurar que el archivo se dirija a la carpeta Pagos_NV.
        long count = pagoRepository.countByPedidoId(pedidoId);
        GoogleDriveService.DriveUploadResult uploadResult = googleDriveService.subirComprobantePago(comprobante,
                pedido.getCodigoSeguimiento(), (int) count + 1);

        // 3. Persistencia con Identificador Visual (🏦 para transferencias por portal)
        Pago nuevoPago = Pago.builder()
                .pedido(pedido)
                .monto(montoNeto)
                .fechaPago(LocalDateTime.now())
                .metodoPago(metodoPago)
                .driveFileId(uploadResult.getFileId())
                .referenciaComprobante(uploadResult.getWebViewLink())
                .concepto("🏦 Comprobante Auditado (Bruto: $" + montoBruto + ")")
                .origenPago(OrigenPago.CLIENTE_WEB)
                .build();

        return pagoRepository.save(nuevoPago);
    }

    /**
     * MOTOR DE AUDITORÍA: Calcula cuánto le falta pagar al cliente.
     * Toda la matemática de este motor funciona estrictamente en base Neto.
     */
    public Double calcularSaldoPendiente(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        Double netoPactado = (pedido.getPrecioFinal() != null) ? pedido.getPrecioFinal() : 0.0;
        Double totalAbonadoNeto = pagoRepository.sumarAbonosPorPedido(pedidoId);

        if (totalAbonadoNeto == null) {
            totalAbonadoNeto = 0.0;
        }

        return netoPactado - totalAbonadoNeto;
    }

    /**
     * Aplica un ajuste automático en la base de datos para salvar diferencias
     * milimétricas causadas por el redondeo de decimales del IVA.
     */
    @Transactional
    public void aplicarAjusteRedondeo(Pedido pedido, Double montoAjuste) {
        Pago ajuste = Pago.builder()
                .pedido(pedido)
                .monto(montoAjuste)
                .fechaPago(LocalDateTime.now())
                .metodoPago("SISTEMA_AJUSTE")
                .concepto("⚙️ Ajuste por redondeo mínimo ($5)")
                .build();
        pagoRepository.save(ajuste);
    }

    /**
     * REGISTRO DIRECTO (Caja Registradora)
     * Utilizado para pagos en efectivo o transferencias validadas directamente
     * por Luis donde no se requiere comprobante físico en Drive.
     */
    @Transactional
    public Pago registrarPagoManual(Long pedidoId, Double monto, String metodoPago, String concepto) {
        return registrarPagoManual(pedidoId, monto, metodoPago, concepto, null);
    }

    @Transactional
    public Pago registrarPagoManual(Long pedidoId, Double monto, String metodoPago, String concepto,
            org.springframework.web.multipart.MultipartFile evidencia) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        // INYECCIÓN DE EMOJIS: Unificación visual para el historial de Luis
        String prefijoEmoji = "";
        if ("EFECTIVO".equalsIgnoreCase(metodoPago)) {
            prefijoEmoji = "💵 ";
        } else if ("TRANSFERENCIA".equalsIgnoreCase(metodoPago)) {
            prefijoEmoji = "🏦 ";
        } else {
            prefijoEmoji = "💳 ";
        }
        
        String conceptoFinal = prefijoEmoji + concepto;

        Pago nuevoPago = Pago.builder()
                .pedido(pedido)
                .monto(monto)
                .fechaPago(LocalDateTime.now())
                .metodoPago(metodoPago)
                .concepto(conceptoFinal)
                .build();

        // Lógica de subida a Google Drive si Luis adjunta evidencia en el registro manual
        if (evidencia != null && !evidencia.isEmpty()) {
            try {
                int correlativo = (int) pagoRepository.countByPedidoId(pedidoId) + 1;
                GoogleDriveService.DriveUploadResult result = googleDriveService.subirComprobantePago(evidencia,
                        pedido.getCodigoSeguimiento(), correlativo);
                nuevoPago.setReferenciaComprobante(result.getWebViewLink());
                nuevoPago.setDriveFileId(result.getFileId());
                nuevoPago.setOrigenPago(OrigenPago.ADMIN_MANUAL);
            } catch (java.io.IOException e) {
                // Abortamos la transacción si falla la subida a la nube
                throw new RuntimeException("Error de conexión con Google Drive al subir la foto: " + e.getMessage());
            }
        } else if (pedido.getLinkComprobantePago() != null && !pedido.getLinkComprobantePago().isBlank()) {
            // El cliente ya subió un comprobante real por la web. Lo asociamos y no generamos voucher plano
            nuevoPago.setReferenciaComprobante(pedido.getLinkComprobantePago());
            nuevoPago.setDriveFileId(extraerIdDeDrive(pedido.getLinkComprobantePago()));
            nuevoPago.setOrigenPago(OrigenPago.CLIENTE_WEB);
            
            // Limpiamos el link temporal del pedido para liberar futuras transacciones del mismo expediente
            pedido.setLinkComprobantePago(null);
            pedidoRepository.save(pedido);
        } else {
            // Generación de Voucher Digital Automático (Texto Plano) con Nomenclatura Correlativa
            try {
                int numeroPago = (int) pagoRepository.countByPedidoId(pedidoId) + 1;
                
                StringBuilder sb = new StringBuilder();
                sb.append("==================================================\n");
                sb.append("       VOUCHER DE PAGO MANUAL N° ").append(numeroPago).append(" - LEGION 3D\n");
                sb.append("==================================================\n");
                sb.append("Código de Seguimiento : ").append(pedido.getCodigoSeguimiento()).append("\n");
                sb.append("Número de Pago        : ").append(numeroPago).append("\n");
                sb.append("Monto Neto            : $").append(String.format(new java.util.Locale("es", "CL"), "%,.0f", monto)).append(" CLP\n");
                sb.append("Monto Bruto (con IVA) : $").append(String.format(new java.util.Locale("es", "CL"), "%,.0f", (double) Math.round(monto * 1.19))).append(" CLP\n");
                sb.append("Método de Pago        : ").append(metodoPago).append("\n");
                sb.append("Concepto / Nota       : ").append(conceptoFinal).append("\n");
                sb.append("Fecha y Hora          : ").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
                sb.append("==================================================\n");

                String voucherText = sb.toString();
                byte[] bytes = voucherText.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                
                try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(bytes)) {
                    String fileName = "voucher_pago_" + numeroPago + "_" + pedido.getCodigoSeguimiento() + ".txt";
                    GoogleDriveService.DriveUploadResult result = googleDriveService.subirComprobantePago(
                            bis, fileName, "text/plain", bytes.length);
                    nuevoPago.setReferenciaComprobante(result.getWebViewLink());
                    nuevoPago.setDriveFileId(result.getFileId());
                    nuevoPago.setOrigenPago(OrigenPago.ADMIN_MANUAL);
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException("Error de conexión con Google Drive al subir el voucher automático: " + e.getMessage());
            }
        }

        return pagoRepository.save(nuevoPago);
    }

    /**
     * MATEMÁTICA INVERSA MANUAL
     * Helper que permite al frontend enviar un monto en Bruto y encarga al backend
     * la responsabilidad de deflactarlo a Neto antes de guardarlo.
     */
    @Transactional
    public Pago registrarPagoBrutoInverso(Long pedidoId, Double montoBruto, String metodoPago, String concepto) {
        Double montoNeto = montoBruto / 1.19;
        return registrarPagoManual(pedidoId, montoNeto, metodoPago, concepto + " (Audit: Bruto $" + montoBruto + ")");
    }

    @Transactional
    public void liquidarSaldoAdministrativo(Long pedidoId, String justificacion) {
        Double saldoPendiente = calcularSaldoPendiente(pedidoId);
        if (saldoPendiente > 0) {
            registrarPagoManual(pedidoId, saldoPendiente, "LIQUIDACION_ADMIN", "⚖️ " + justificacion);
        }
    }

    private String extraerIdDeDrive(String link) {
        if (link == null)
            return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:/d/|id=|folders/)([\\w-]+)");
        java.util.regex.Matcher matcher = pattern.matcher(link);
        return matcher.find() ? matcher.group(1) : null;
    }
}
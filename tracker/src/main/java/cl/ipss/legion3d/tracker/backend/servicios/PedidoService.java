package cl.ipss.legion3d.tracker.backend.servicios;

import cl.ipss.legion3d.tracker.backend.dtos.FormularioTecnicoDTO;
import cl.ipss.legion3d.tracker.backend.entidades.Cliente;
import cl.ipss.legion3d.tracker.backend.entidades.DetallesTecnicos;
import cl.ipss.legion3d.tracker.backend.entidades.Pedido;
import cl.ipss.legion3d.tracker.backend.entidades.HistorialEstado;
import cl.ipss.legion3d.tracker.backend.repositorios.ClienteRepository;
import cl.ipss.legion3d.tracker.backend.repositorios.PedidoRepository;
import cl.ipss.legion3d.tracker.backend.repositorios.DetallesTecnicosRepository;
import cl.ipss.legion3d.tracker.backend.repositorios.HistorialEstadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Este archivo se llama PedidoService y es el verdadero orquestador de nuestro sistema.
 * A nivel de arquitectura, centraliza la lógica de negocio más compleja de Legión 3D. 
 * 
 * Una de las decisiones técnicas más importantes que tomamos aquí es el uso de la anotación @Transactional. 
 * Como en el método 'guardarDetallesTecnicos' estamos tocando tres tablas distintas al mismo tiempo 
 * (Pedidos, Clientes y Detalles Técnicos), esta anotación actúa como un escudo de integridad. 
 * Garantiza que la operación sea de "todo o nada": si falla el guardado en el paso 5, el sistema 
 * revierte automáticamente los cambios del paso 2, evitando que la base de datos de Luis 
 * quede con información corrupta o a medias.
 */
@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepo;
    @Autowired
    private ClienteRepository clienteRepo;
    @Autowired
    private DetallesTecnicosRepository detallesRepo;
    @Autowired
    private HistorialEstadoRepository historialRepo;
    @Autowired
    private PagoService pagoService;

    @Transactional
    public Pedido guardarDetallesTecnicos(String codigo, FormularioTecnicoDTO dto) {

        // PASO 1: LOCALIZACIÓN DEL PEDIDO
        // Buscamos el pedido en la base de datos usando su código único de seguimiento
        // (LEG-XXXX).
        // Si no existe, lanzamos una excepción para detener el proceso.
        Pedido pedido = pedidoRepo.findByCodigoSeguimiento(codigo)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con el código: " + codigo));

        // PASO 2: SINCRONIZACIÓN DE DATOS DEL CLIENTE (CRM) - Aislamiento de Transacción
        Cliente cliente = pedido.getCliente();
        if (cliente != null) {
            // Si el cliente proporcionó un nuevo teléfono, lo actualizamos.
            if (dto.getTelefonoContacto() != null && !dto.getTelefonoContacto().isBlank()) {
                cliente.setTelefono(dto.getTelefonoContacto());
            }
            // Si el cliente proporcionó su RUT (necesario para despacho/facturación), lo
            // guardamos.
            if (dto.getRut() != null && !dto.getRut().isBlank()) {
                cliente.setRut(dto.getRut());
            }
            // Guardamos el cliente de forma aislada para evitar cascadas sobre 'pedidos'
            clienteRepo.saveAndFlush(cliente);
        }

        // PASO 3: GESTIÓN DE LA ENTIDAD DE DETALLES TÉCNICOS
        DetallesTecnicos dt = pedido.getDetallesTecnicos();
        if (dt == null) {
            dt = new DetallesTecnicos();
            dt.setPedido(pedido);
        }

        // PASO 4: MAPEO Y TRANSFERENCIA DE DATOS (Mapeo DTO a Entidad)
        // Trasladamos cada campo recibido desde el formulario web (DTO) hacia la
        // entidad persistente.
        dt.setMedidaAncho(dto.getMedidaAncho());
        dt.setMedidaAlto(dto.getMedidaAlto());
        dt.setMedidaProfundidad(dto.getMedidaProfundidad());
        dt.setCantidadUnidades(dto.getCantidadUnidades());
        dt.setTienePiezaFisica(dto.isTienePiezaFisica());
        dt.setNecesitaModificacion(dto.getNecesitaModificacion());
        dt.setToleranciaCheck(dto.isToleranciaCheck());
        dt.setEsCopiaExacta(dto.isEsCopiaExacta());
        dt.setMaterialSolicitado(dto.getMaterialSolicitado());
        dt.setColorSolicitado(dto.getColorSolicitado());
        dt.setEntornoUso(dto.getEntornoUso());
        dt.setPresupuestoEstimado(dto.getPresupuestoEstimado());
        dt.setDiasEntrega(dto.getDiasEntrega());
        dt.setMetodoEntrega(dto.getMetodoEntrega());
        dt.setRegion(dto.getRegion());
        dt.setComuna(dto.getComuna());
        dt.setCalleYNumero(dto.getCalleYNumero());
        dt.setDeptoCasaOficina(dto.getDeptoCasaOficina());
        dt.setInformacionAdicional(dto.getInformacionAdicional());
        dt.setTipoEnvioStarken(dto.getTipoEnvioStarken());
        dt.setRut(dto.getRut());
        dt.setTelefonoContacto(dto.getTelefonoContacto());
        // BLINDAJE DE CONCATENACIÓN (Prevent Overwrite): 
        // No permitimos que el formulario borre el link de Drive si ya existe un pipe.
        String linkExistente = dt.getLinkArchivoFinal();
        String linkNuevo = dto.getLinkArchivoFinal();
        if (linkExistente != null && linkExistente.contains("|")) {
            String linkDrive = linkExistente.split("\\|")[1];
            dt.setLinkArchivoFinal((linkNuevo != null ? linkNuevo : "") + "|" + linkDrive);
        } else {
            dt.setLinkArchivoFinal(linkNuevo);
        }

        // PASO 5: PERSISTENCIA PREVENTIVA
        // Guardamos primero los detalles técnicos para asegurar que tengan un ID antes
        // de actualizar el pedido.
        dt = detallesRepo.save(dt);

        // PASO 6: CIERRE Y EVOLUCIÓN DEL ESTADO
        // Vinculamos formalmente los detalles al pedido y cambiamos su estado a
        // "EN_EVALUACION".
        // Esto permite que el pedido aparezca ahora en la bandeja de entrada del
        // administrador.
        pedido.setDetallesTecnicos(dt);
        pedido.setEstadoActual("EN_EVALUACION");

        // Retornamos el pedido guardado con toda su nueva información técnica.
        return pedidoRepo.save(pedido);
    }

    // --------------------------------------------------------
    // NUEVO MÉTODO: MÁQUINA DE ESTADOS (Para la prueba unitaria)
    // Este bloque blinda la seguridad operativa de nuestro modelo de negocio.
    // ----------------------------------------------------------------------------
    /**
     * MOTOR DE LA MÁQUINA DE ESTADOS (Segmentación de Transición)
     * Centraliza la lógica de cambio de fase del pedido, aplicando guardias de seguridad 
     * para evitar saltos lógicos y protegiendo la integridad de las bandejas operativas.
     */
    @Transactional
    public Pedido actualizarEstado(Long idPedido, String nuevoEstado) {
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        String estadoAnterior = pedido.getEstadoActual();

        // 1. SEGMENTACIÓN DE LA TRANSICIÓN: Regla de Oro Financiera vs Logística (Problema 1)
        if ("PAGO_ENVIADO".equals(nuevoEstado)) {
            // Permitimos el retroceso a Bandeja 1 solo si el pedido está en etapas comerciales/iniciales.
            // Esto evita que pedidos en Producción o Listos para Entrega "retrocedan" visualmente.
            boolean esEstadoInicialParaPago = "NUEVA".equals(estadoAnterior) || 
                                              "PENDIENTE_TECNICOS".equals(estadoAnterior) || 
                                              "COTIZACION".equals(estadoAnterior) || 
                                              "PRESUPUESTADO".equals(estadoAnterior) || 
                                              "EXPIRADO".equals(estadoAnterior) ||
                                              "EN_EVALUACION".equals(estadoAnterior);

            if (!esEstadoInicialParaPago) {
                // El pedido ya está en taller o logística (Bandeja 2 o 3). 
                // Registramos la auditoría pero blindamos el estado logístico actual.
                guardarHistorial(pedido, estadoAnterior, "AUDITORIA_PAGO_REGISTRADA");
                return pedido; // Retorno temprano: El pedido permanece en su bandeja avanzada
            }
        } else {
            // SEGURIDAD OPERATIVA: Bloqueo de saltos lógicos (Anti-Shortcut)
            if ("NUEVA".equals(estadoAnterior) && 
                ("ENTREGADO".equals(nuevoEstado) || "LISTO_PARA_ENTREGA".equals(nuevoEstado))) {
                throw new IllegalStateException("Transición inválida. No se puede saltar a etapas finales sin presupuesto.");
            }

            // REGLA DE GARANTÍA (-G): Las garantías tienen costo $0 y no requieren validación de pagos.
            if (pedido.isEsGarantia() && "PAGO_ENVIADO".equals(nuevoEstado)) {
                return pedido; // Blindaje contra ruidos financieros en garantías
            }
        }

        // 2. VALIDACIÓN FINANCIERA: El Candado de Redondeo (Existente)
        if ("ENTREGADO".equalsIgnoreCase(nuevoEstado)) {
            Double totalAbonado = pagoService.sumarAbonosPorPedido(idPedido);
            Double saldo = (pedido.getPrecioFinal() != null ? pedido.getPrecioFinal() : 0.0) - totalAbonado;
            
            if (saldo > 5.0) {
                throw new IllegalStateException("Saldo pendiente detectado ($" + String.format("%.2f", saldo) + "). El pedido no puede entregarse.");
            } else if (saldo > 0.0 && saldo <= 5.0) {
                // Si el saldo es despreciable (<$5), el sistema lo asume como ajuste de redondeo.
                pagoService.aplicarAjusteRedondeo(pedido, saldo);
            }
        }

        // 3. APLICACIÓN DEL CAMBIO Y AUDITORÍA HISTÓRICA
        pedido.setEstadoActual(nuevoEstado);
        guardarHistorial(pedido, estadoAnterior, nuevoEstado);

        return pedidoRepo.save(pedido);
    }

    private void guardarHistorial(Pedido pedido, String estadoAntiguo, String estadoNuevo) {
        HistorialEstado historial = new HistorialEstado();
        historial.setPedido(pedido);
        historial.setEstadoAnterior(estadoAntiguo);
        historial.setEstadoNuevo(estadoNuevo);
        historialRepo.save(historial);
    }

    /**
     * MÉTODO DE ELIMINACIÓN LÓGICA EN CASCADA
     * Esta es la pieza central de nuestra nueva estrategia de integridad.
     * En lugar de borrar físicamente, desactivamos el pedido y sus dependencias técnicas.
     */
    @Transactional
    public void eliminarPedido(Long id) {
        // Buscamos el expediente maestro
        Pedido pedido = pedidoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: No se encontró el pedido para eliminar."));

        // PASO 1: Desactivación del Pedido
        pedido.setActivo(false);

        // PASO 2: Desactivación en Cascada (Detalles Técnicos)
        // Como equipo decidimos que si un pedido "muere" lógicamente, su ingeniería también.
        if (pedido.getDetallesTecnicos() != null) {
            pedido.getDetallesTecnicos().setActivo(false);
        }

        // PERSISTENCIA: Al guardar el pedido, JPA/Hibernate sincroniza los cambios 
        // en el grafo de objetos hacia la base de datos MySQL.
        pedidoRepo.save(pedido);
    }

    /**
     * MÓDULO DE GARANTÍAS (SOFT CLONE)
     * Crea un clon del pedido original para gestionar fallos o errores de producción 
     * manteniendo la trazabilidad con el padre y forzando costo $0.
     */
    @Transactional
    public Pedido prepararGarantia(Long idOriginal) {
        // 1. Recuperación del original
        Pedido original = pedidoRepo.findById(idOriginal)
                .orElseThrow(() -> new RuntimeException("Error: No se encontró el pedido original para clonar."));

        // 2. Creación de la nueva instancia (El Clon)
        Pedido clon = new Pedido();
        clon.setCliente(original.getCliente());
        clon.setServicioSolicitado(original.getServicioSolicitado() + " (GARANTÍA)");
        // Generador de sufijo alfanumérico único (4 caracteres) para evitar colisiones SQL en garantías múltiples
        String sufijoUnico = java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        clon.setCodigoSeguimiento(original.getCodigoSeguimiento() + "-G-" + sufijoUnico);
        clon.setEstadoActual("NUEVA");
        clon.setPrecioFinal(0.0);
        clon.setEsGarantia(true);
        clon.setPedidoPadreId(original.getId());
        clon.setOrigenContacto("Garantía Sistema");
        clon.setDesgloseCostos(original.getDesgloseCostos());
        clon.setAnotacionesInternas(original.getAnotacionesInternas());
        
        // Persistimos el clon para obtener su ID
        clon = pedidoRepo.save(clon);

        // 3. Clonación de Detalles Técnicos (Ingeniería de la Pieza)
        if (original.getDetallesTecnicos() != null) {
            DetallesTecnicos dtOriginal = original.getDetallesTecnicos();
            DetallesTecnicos dtClon = new DetallesTecnicos();
            
            // Copiamos los parámetros técnicos críticos
            dtClon.setPedido(clon);
            dtClon.setMaterialSolicitado(dtOriginal.getMaterialSolicitado());
            dtClon.setColorSolicitado(dtOriginal.getColorSolicitado());
            dtClon.setMedidaAncho(dtOriginal.getMedidaAncho());
            dtClon.setMedidaAlto(dtOriginal.getMedidaAlto());
            dtClon.setMedidaProfundidad(dtOriginal.getMedidaProfundidad());
            dtClon.setCantidadUnidades(dtOriginal.getCantidadUnidades());
            dtClon.setEntornoUso(dtOriginal.getEntornoUso());
            dtClon.setMetodoEntrega(dtOriginal.getMetodoEntrega());
            
            detallesRepo.save(dtClon);
            clon.setDetallesTecnicos(dtClon);
        }

        // 4. Registro de Auditoría en Historial
        HistorialEstado historial = new HistorialEstado();
        historial.setPedido(clon);
        historial.setEstadoAnterior("SISTEMA");
        historial.setEstadoNuevo("NUEVA");
        historial.setComentario("Pedido de garantía generado automáticamente desde el seguimiento original: " + original.getCodigoSeguimiento());
        historialRepo.save(historial);

        return pedidoRepo.save(clon);
    }

    @Transactional
    public Pedido marcarComoGarantia(Long id) {
        Pedido pedido = pedidoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        pedido.setEsGarantia(true);
        pedido.setPrecioFinal(0.0); // Las garantías no suman ingresos reales
        return pedidoRepo.save(pedido);
    }
}
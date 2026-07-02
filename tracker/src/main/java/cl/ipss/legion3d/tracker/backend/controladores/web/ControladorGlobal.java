package cl.ipss.legion3d.tracker.backend.controladores.web;
 
import cl.ipss.legion3d.tracker.backend.repositorios.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Controlador global (Interceptor de Vistas) para el Legión 3D Tracker.
 * Al estar anotado con @ControllerAdvice, los atributos definidos aquí
 * se inyectan automáticamente en todas las plantillas Thymeleaf del sistema.
 * 
 * Este enfoque arquitectónico centraliza la carga del panel de métricas,
 * evitando la duplicación de consultas a la base de datos en cada endpoint individual.
 * Ejemplo de consumo en la vista: <span th:text="${conteoNuevas}">0</span>
 */
@ControllerAdvice
public class ControladorGlobal {

    @Autowired
    private PedidoRepository pedidoRepo;

    /**
     * Método interceptor que inyecta los contadores del flujo de impresión 3D
     * directamente en el modelo de vista antes de renderizar cualquier página.
     *
     * //@param model Objeto provisto por Spring MVC para pasar datos a la capa de presentación.
     */

    @ModelAttribute
    public void inyectarContadoresGlobales(Model model) {
        try {
            // 1. Triage: Ingreso inicial de requerimientos por parte del cliente.
            model.addAttribute("conteoNuevas", pedidoRepo.countByEstadoActual("NUEVA"));

            // 2. Levantamiento Técnico: Pendiente de que se completen los detalles de ingeniería en el Formulario 2.
            model.addAttribute("conteoPendiente", pedidoRepo.countByEstadoActual("PENDIENTE_TECNICOS"));

            // 3. Revisión de Ingeniería
            model.addAttribute("conteoRevision", pedidoRepo.countByEstadoActual("EN_EVALUACION"));

            // 4. Valorización: Fase crítica donde Luis asigna el costo final de los materiales y tiempo de impresión.
            model.addAttribute("conteoCotizacion", pedidoRepo.countByEstadoActual("COTIZACION"));

            // *** NUEVA TARJETA EXPLÍCITA ***
            // 5. Presupuesto Enviado: El precio fue comunicado y la decisión de compra recae en el cliente.
            model.addAttribute("conteoEsperandoPago", pedidoRepo.countByEstadoActual("PRESUPUESTADO"));

            // 6. Validación de Pagos: El cliente reportó el comprobante. Activa las alertas de prioridad.
            model.addAttribute("conteoPagos", pedidoRepo.countByEstadoActual("PAGO_ENVIADO"));

            // 7. Área de Máquinas: El modelo ya está en la cama de impresión y la producción está activa.
            model.addAttribute("conteoProduccion", pedidoRepo.countByEstadoActual("EN_PRODUCCION"));

            // 8. Logística: Pieza terminada, post-procesada y lista para su despacho o retiro.
            model.addAttribute("conteoListos", pedidoRepo.countByEstadoActual("LISTO_PARA_ENTREGA"));

            // Métrica Financiera: Cálculo del volumen total de ventas netas procesadas en el sistema.
            java.time.LocalDate hoy = java.time.LocalDate.now();
            Double ventas = pedidoRepo.sumTotalVentasPorMes(hoy.getMonthValue(), hoy.getYear());
            model.addAttribute("totalVentas", (ventas != null) ? String.format("%,.0f", ventas) : "0");

        } catch (Exception e) {
            // Manejo de contingencia para evitar que un fallo en las métricas tumbe la carga de la interfaz gráfica.
            System.err.println("⚠️ Error crítico al inyectar las métricas del tracker: " + e.getMessage());
        }
    }
}

// Es importante mantener este nivel de documentación Del Código para asegurar la claridad
//  y mantenibilidad del proyecto, especialmente en un sistema tan dinámico como el Legión 3D Tracker.
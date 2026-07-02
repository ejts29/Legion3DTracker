package cl.ipss.legion3d.tracker.backend.controladores.web;

import cl.ipss.legion3d.tracker.backend.entidades.Cliente;
import cl.ipss.legion3d.tracker.backend.entidades.DetallesTecnicos;
import cl.ipss.legion3d.tracker.backend.entidades.Pedido;
import cl.ipss.legion3d.tracker.backend.repositorios.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

// -----------------------------------------
// CONTROLADOR DE VISTAS ADMINISTRATIVAS (Frontend Thymeleaf)
// -----------------------------------------------------------
// Este controlador es el puente visual del sistema Legión 3D Tracker. 
// A diferencia de nuestras APIs REST (que retornan datos puros o JSON), 
// esta clase se encarga de conectar la base de datos directamente con las 
// interfaces gráficas (plantillas HTML) que utilizará la administración.
//
// Estrategia Arquitectónica del Equipo:
// En cada método utilizamos 'pedidoRepository.findAll()' para enviar la lista 
// completa de expedientes a la vista. Decidimos hacer esto para delegar el 
// filtrado visual a las tablas dinámicas del frontend (como DataTables). 
// Esto permite que Luis pueda buscar, ordenar y filtrar pedidos en tiempo real 
// desde su pantalla, logrando una navegación ultrarrápida y sin recargar el servidor.

@Controller
@RequestMapping("/solicitudes")
public class AdminWebController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private cl.ipss.legion3d.tracker.backend.servicios.PedidoService pedidoService;

    // ----------------------------------------------------------------------
    // 1. VISTA DE TRIAJE (Recepción y Filtro Inicial)
    // -----------------------------------------------------
    // Es la sala de urgencias del taller. Aquí aterrizan todas las solicitudes 
    // entrantes (pedidos nuevos que acaban de llegar desde el Webhook de WordPress).
    // El administrador utiliza esta vista para evaluar rápidamente qué pide el cliente,
    // clasificar la urgencia y asignar el requerimiento a la siguiente etapa.
    @GetMapping("/triaje")
    public String mostrarTriaje(Model model) {
        model.addAttribute("pedidos", pedidoRepository.findAllActive());
        return "solicitudes/triaje";
    }

    // ------------------------------------------
    // 2. VISTA DE REVISIÓN TÉCNICA (Ingeniería y Viabilidad)
    // ----------------------------------------------------------------------
    // En esta etapa se analizan los planos, modelos 3D y medidas. 
    // Es una pantalla más compleja porque requiere manejar formularios de actualización.
    @GetMapping("/revision-datos-tecnicos")
    public String mostrarRevisionTecnica(Model model) {
        List<Pedido> listaPedidos = pedidoRepository.findAllActive();
        model.addAttribute("pedidos", listaPedidos);
        
        // PREVENCIÓN DE ERRORES (Thymeleaf Anti-Crash):
        // Como equipo implementamos esta inicialización preventiva. Al inyectar 
        // objetos vacíos (new Pedido, new DetallesTecnicos, new Cliente) en el modelo, 
        // aseguramos que cuando la vista HTML intente renderizar los modales o formularios 
        // de edición, no arroje excepciones de "Null Pointer". 
        // Esto mantiene la interfaz robusta y a prueba de fallos.
        model.addAttribute("pedido", new Pedido());
        model.addAttribute("detalles", new DetallesTecnicos());
        model.addAttribute("cliente", new Cliente());
        
        return "solicitudes/revision-datos-tecnicos";
    }

    // ----------------------------------------------------------------------
    // 3. VISTA DE COTIZACIONES (Gestión Financiera)
    // ----------------------------------------------------------------------
    // Espacio dedicado exclusivamente al cálculo de presupuestos.
    // Permite a la administración coordinar los costos de material, horas de impresión 
    // y tiempos asociados a cada solicitud, para luego enviar la propuesta formal al cliente.
    @GetMapping("/cotizaciones")
    public String mostrarCotizaciones(Model model) {
        model.addAttribute("pedidos", pedidoRepository.findAllActive());
        return "solicitudes/cotizaciones";
    }

    // ----------------------------------------------------------------------
    // 4. VISTA DE PRODUCCIÓN (Control de Taller)
    // ----------------------------------------------------------------------
    // Es el panel de control operativo. Aquí se supervisa el avance real de las máquinas.
    // Muestra los pedidos que ya están pagados y en pleno proceso de fabricación, 
    // permitiendo monitorear el progreso y gestionar cuellos de botella en la impresión 3D.
    @GetMapping("/produccion")
    public String mostrarProduccion(Model model) {
        model.addAttribute("pedidos", pedidoRepository.findAllActive());
        return "solicitudes/produccion";
    }

    // ----------------------------------------------------------------------
    // 5. VISTA DE HISTORIAL (Bóveda Maestra y Auditoría)
    // ----------------------------------------------------------------------
    // El registro histórico intocable. Actúa como la bóveda maestra que almacena 
    // todos los registros de solicitudes procesadas. Gracias a nuestra implementación 
    // de borrado lógico (Soft Delete), aquí se pueden consultar incluso los proyectos 
    // que fueron entregados hace meses o los que fueron rechazados.
    @GetMapping("/historial")
    public String mostrarHistorial(Model model) {
        model.addAttribute("pedidos", pedidoRepository.findAllWithRelations());
        return "solicitudes/historial";
    }

    /**
     * ENDPOINT DE GARANTÍA (SOFT CLONE)
     * Procesa la clonación de un pedido para postventa y redirige al editor.
     */
    @GetMapping("/garantia/{id}")
    public String generarGarantia(@org.springframework.web.bind.annotation.PathVariable Long id) {
        Pedido nuevaGarantia = pedidoService.prepararGarantia(id);
        // Redirigimos al historial con el parámetro de ID para que se abra el modal de edición automáticamente
        return "redirect:/solicitudes/historial?id=" + nuevaGarantia.getId();
    }
}
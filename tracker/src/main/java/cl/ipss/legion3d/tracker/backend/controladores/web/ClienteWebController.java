package cl.ipss.legion3d.tracker.backend.controladores.web;

import cl.ipss.legion3d.tracker.backend.entidades.Pedido;
import cl.ipss.legion3d.tracker.backend.repositorios.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * CONTROLADOR PÚBLICO DE CLIENTES (Portal de Seguimiento)
 * Gestiona la visualización del progreso del pedido y el acceso al formulario técnico.
 */
@Controller
@RequestMapping("/tracking")
public class ClienteWebController {

    @Autowired
    private PedidoRepository pedidoRepo;

    @GetMapping("/{codigo}")
    public String verSeguimiento(@PathVariable String codigo, Model model) {
        
        // Búsqueda del expediente mediante el código de seguimiento único
        Pedido pedido = pedidoRepo.findByCodigoSeguimiento(codigo).orElse(null);
        
        if (pedido == null) {
            return "redirect:/?error=codigo-invalido";
        }
        
        // --- MOTOR DE CÁLCULO DE NIVELES (7 PASOS) ---
        // Sincroniza dinámicamente el Stepper visual con los hitos del negocio.
        int nivel = 1;
        String estado = pedido.getEstadoActual();

        if ("NUEVA".equals(estado)) {
            nivel = 1;
        } else if ("PENDIENTE_TECNICOS".equals(estado) || "EN_EVALUACION".equals(estado)) {
            nivel = 2;
        } else if ("COTIZACION".equals(estado) || "PRESUPUESTADO".equals(estado)) {
            nivel = 3;
        } else if ("PAGO_ENVIADO".equals(estado)) {
            // DETERMINACIÓN DE ETAPA DE PAGO:
            // Si el pedido ya cuenta con detalles técnicos o link de archivo final, 
            // asumimos que el pago enviado corresponde al SALDO FINAL.
            if (pedido.getDetallesTecnicos() != null && 
               (pedido.getDetallesTecnicos().getLinkArchivoFinal() != null || 
                pedido.getDetallesTecnicos().getMetodoEntrega() != null)) {
                nivel = 5; // Hito: Pago Final
            } else {
                nivel = 3; // Hito: Pago Inicial
            }
        } else if ("EN_PRODUCCION".equals(estado)) {
            nivel = 4;
        } else if ("LISTO_PARA_ENTREGA".equals(estado)) {
            nivel = 5;
            // Si el taller ya registró información de despacho (Starken/Retiro), avanzamos al nivel 6
            if (pedido.getLinkComprobanteEnvio() != null && !pedido.getLinkComprobanteEnvio().isBlank()) {
                nivel = 6;
            }
        } else if ("ENTREGADO".equals(estado)) {
            nivel = 7;
        }

        model.addAttribute("pedido", pedido);
        model.addAttribute("nivel", nivel);
        
        return "clientes/seguimiento-pedido";
    }

    @GetMapping("/{codigo}/formulario")
    public String mostrarFormularioTecnico(@PathVariable String codigo, Model model) {
        Pedido pedido = pedidoRepo.findByCodigoSeguimiento(codigo).orElse(null);
        
        if (pedido == null) {
            return "redirect:/?error=no-encontrado";
        }
        
        // BLOQUEO DE SEGURIDAD: Solo permite acceso al formulario en etapas iniciales
        if (!"PENDIENTE_TECNICOS".equals(pedido.getEstadoActual()) && !"NUEVA".equals(pedido.getEstadoActual())) {
            return "redirect:/tracking/" + codigo; 
        }
        
        model.addAttribute("pedido", pedido);
        return "clientes/formulario-tecnico";
    }
}
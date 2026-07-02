package cl.ipss.legion3d.tracker.backend.servicios;

import cl.ipss.legion3d.tracker.backend.entidades.Pedido;
import cl.ipss.legion3d.tracker.backend.repositorios.PedidoRepository;
import cl.ipss.legion3d.tracker.backend.repositorios.PagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CicloVidaService {

    @Autowired
    private PedidoRepository pedidoRepo;

    @Autowired
    private PagoRepository pagoRepo;

    /**
     * Automatización del Ciclo de Vida: Expiración de Presupuestos.
     * Se ejecuta todos los días a las 2 AM.
     * Busca pedidos en estado 'PRESUPUESTADO' con más de 15 días desde su creación 
     * y sin abonos registrados, cambiándolos a 'EXPIRADO'.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void ejecutarExpiracionAutomatica() {
        LocalDateTime hace15Dias = LocalDateTime.now().minusDays(15);
        
        // Obtenemos todos los pedidos presupuestados
        List<Pedido> presupuestados = pedidoRepo.findAll().stream()
                .filter(p -> "PRESUPUESTADO".equalsIgnoreCase(p.getEstadoActual()))
                .filter(p -> p.getFechaCreacion().isBefore(hace15Dias))
                .collect(Collectors.toList());

        for (Pedido p : presupuestados) {
            Double totalAbonado = pagoRepo.sumarAbonosPorPedido(p.getId());
            if (totalAbonado <= 0) {
                p.setEstadoActual("EXPIRADO");
                pedidoRepo.save(p);
                // Opcional: Podríamos registrar esto en el historial también
            }
        }
    }
}

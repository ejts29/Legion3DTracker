package cl.ipss.legion3d.tracker; // <--- ¡Notarás que el paquete ahora es el mismo de tus otros tests!

import cl.ipss.legion3d.tracker.backend.entidades.Pedido;
import cl.ipss.legion3d.tracker.backend.repositorios.PedidoRepository;
import cl.ipss.legion3d.tracker.backend.servicios.PedidoService; // <--- Tuvimos que importar el servicio real
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * PRUEBAS UNITARIAS: Lógica de Negocio Aislada
 * Valida que la Máquina de Estados de Legión 3D Tracker sea inviolable.
 */
@ExtendWith(MockitoExtension.class)
public class PedidoServiceTest {

    // 1. MOCKING: Simulamos la base de datos para no tocar el MySQL real
    @Mock
    private PedidoRepository pedidoRepo; 

    // 2. INYECCIÓN: Inyectamos el mock dentro del servicio real que vamos a probar
    @InjectMocks
    private PedidoService pedidoService; 

    @Test
    void probarBloqueoDeSaltoDeEstadoInvalido() {
        // PREPARACIÓN (Arrange)
        // Creamos un pedido de prueba simulado en estado "NUEVA"
        Pedido pedidoSimulado = new Pedido();
        pedidoSimulado.setId(1L);
        pedidoSimulado.setEstadoActual("NUEVA");

        // Le decimos a Mockito cómo debe comportarse: 
        // "Cuando el servicio busque el pedido ID 1, entrégale este pedido simulado"
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedidoSimulado));

        // EJECUCIÓN Y VERIFICACIÓN (Act & Assert)
        // Intentamos forzar un salto ilegal hacia "ENTREGADO" y verificamos que "explote" con gracia
        Exception excepcion = assertThrows(IllegalStateException.class, () -> {
            pedidoService.actualizarEstado(1L, "ENTREGADO");
        });

        // Confirmamos que el escudo lanzó el mensaje de seguridad exacto
        assertEquals("Transición de estado inválida. No se puede saltar etapas.", excepcion.getMessage());
    }
}
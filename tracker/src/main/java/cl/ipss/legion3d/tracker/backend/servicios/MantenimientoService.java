package cl.ipss.legion3d.tracker.backend.servicios;

import cl.ipss.legion3d.tracker.backend.entidades.Pedido;
import cl.ipss.legion3d.tracker.backend.entidades.DetallesTecnicos;
import cl.ipss.legion3d.tracker.backend.repositorios.PedidoRepository;
import cl.ipss.legion3d.tracker.backend.repositorios.DetallesTecnicosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * SERVICIO DE MANTENIMIENTO OPERATIVO
 * ------------------------------------------------------------------
 * Este servicio actúa como el encargado de la "salud digital" del taller.
 * Su función principal es realizar tareas automáticas y programadas que
 * eviten que el almacenamiento de Google Drive se llene y que la base
 * de datos mantenga registros coherentes con la realidad física.
 */
@Service
public class MantenimientoService {

    // Inyección de dependencias para interactuar con la base de datos de pedidos
    @Autowired
    private PedidoRepository pedidoRepo;

    // Inyección de dependencias para actualizar detalles técnicos y links de archivos
    @Autowired
    private DetallesTecnicosRepository detallesRepo;

    // Inyección del servicio de Google Drive para realizar las eliminaciones físicas
    @Autowired
    private GoogleDriveService driveService;

    /**
     * PURGA AUTOMÁTICA DE ARCHIVOS PESADOS (60 DÍAS)
     * -------------------------------------------------------------------------
     * Cron: "0 0 3 * * SUN" (Se ejecuta todos los domingos a las 3:00 AM).
     * * Este proceso busca pedidos que ya fueron entregados satisfactoriamente
     * y cuyos archivos STL (que son pesados) tienen más de 60 días.
     * Al eliminarlos, liberamos espacio crítico en la nube de Legión 3D.
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    public void ejecutarPurgaArchivosNube() {
        // Establecemos la fecha límite (hoy menos 60 días)
        LocalDate limite = LocalDate.now().minusDays(60);
        
        // Consultamos a MySQL por pedidos en estado "ENTREGADO" anteriores a esa fecha
        List<Pedido> pedidosAntiguos = pedidoRepo.buscarPedidosParaLimpieza("ENTREGADO", limite);

        System.out.println("🧹 Iniciando purga de archivos STL obsoletos (60 días)... Pedidos encontrados: " + pedidosAntiguos.size());

        for (Pedido p : pedidosAntiguos) {
            DetallesTecnicos dt = p.getDetallesTecnicos();
            
            // Verificamos que el pedido tenga un ID de archivo de Drive asociado
            if (dt != null && dt.getDriveFileId() != null) {
                try {
                    // 1. Eliminación física: Llamamos a Drive para borrar el archivo
                    driveService.eliminarArchivo(dt.getDriveFileId());
                    
                    // 2. Sincronización DB: Limpiamos la referencia para que el sistema sepa que se borró
                    dt.setDriveFileId(null);
                    dt.setLinkArchivoFinal("PURGADO_POR_ANTIGUEDAD");
                    detallesRepo.save(dt);
                    
                    System.out.println(">>> Pedido " + p.getCodigoSeguimiento() + ": Archivo STL eliminado con éxito.");
                } catch (Exception e) {
                    // Si algo falla con la API de Google, registramos el error sin detener el proceso
                    System.err.println("⚠️ No se pudo purgar el archivo para el pedido " + p.getCodigoSeguimiento() + ": " + e.getMessage());
                }
            }
        }
        System.out.println("✅ Purga de archivos STL finalizada.");
    }

    /**
     * PURGA DE CARPETAS DE TRÁNSITO (WPS Y STARKEN - 60 DÍAS)
     * -------------------------------------------------------------------------
     * Cron: "0 0 4 * * SUN" (Se ejecuta todos los domingos a las 4:00 AM).
     * * A diferencia de la purga anterior, este es un barrido ciego sobre las 
     * carpetas de entrada de WordPress y de salida de Starken. Elimina archivos 
     * temporales, capturas de pantalla y JSONs que ya no son necesarios
     * para la operación diaria tras 2 meses.
     */
    @Scheduled(cron = "0 0 4 * * SUN")
    public void ejecutarPurgaCarpetasTransito() {
        System.out.println(" Iniciando purga de carpetas de tránsito en Drive (60 días)...");
        
        try {
            // 1. Limpieza de Carpeta WordPress (WPS)
            // Se utiliza el ID de carpeta definido en GoogleDriveService para barrer archivos viejos.
            driveService.limpiarCarpetaPorAntiguedad(GoogleDriveService.CARPETA_WPS_NV, 60);
            
            // 2. Limpieza de Carpeta de Envíos (Starken)
            // Se eliminan comprobantes y etiquetas de envío antiguos para mantener la privacidad.
            driveService.limpiarCarpetaPorAntiguedad(GoogleDriveService.CARPETA_STARKEN_NV, 60);
            
            System.out.println("✅ Purga de carpetas de tránsito finalizada con éxito.");
        } catch (Exception e) {
            System.err.println("⚠️ Error general en la purga de carpetas de tránsito: " + e.getMessage());
        }
    }
}

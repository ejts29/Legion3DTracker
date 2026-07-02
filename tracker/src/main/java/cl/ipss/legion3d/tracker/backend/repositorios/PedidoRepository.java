package cl.ipss.legion3d.tracker.backend.repositorios;

import cl.ipss.legion3d.tracker.backend.entidades.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * REPOSITORIO MAESTRO DE PEDIDOS (Bóveda de Datos)
 * ----------------------------------------------------------------------------
 * Este archivo es el puente de datos más crítico de nuestra aplicación.
 * Como el Pedido es la unidad central de nuestro modelo de negocio, este 
 * repositorio concentra el acceso a la información estratégica del taller.
 * * A nivel de arquitectura, aprovechamos la potencia de JPQL para realizar
 * cálculos financieros directamente en el motor MySQL, garantizando que el
 * sistema sea rápido incluso con miles de registros históricos.
 */
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> { 

    // ========================================================================
    // 1. INDICADORES OPERATIVOS (DASHBOARD)
    // ========================================================================

    /**
     * Cuenta pedidos en un estado específico. 
     * Fundamental para las tarjetas de colores del Dashboard (Nuevas, Producción, etc).
     */
    long countByEstadoActual(String estado);

    /**
     * VENTAS NETAS (HEADER - MES ACTUAL)
     * Calcula la recaudación bruta únicamente de los pedidos entregados en el mes en curso.
     * Se reinicia automáticamente el día 1 de cada mes a las 00:00.
     * CORRECCIÓN CRÍTICA: Mantenemos el nombre 'sumTotalVentas()' para evitar colapsos 
     * de compilación con el AdminWebController.
     */
    @Query("SELECT SUM(p.precioFinal) FROM Pedido p WHERE p.estadoActual = 'ENTREGADO' " +
           "AND MONTH(p.fechaCreacion) = MONTH(CURRENT_DATE) " +
           "AND YEAR(p.fechaCreacion) = YEAR(CURRENT_DATE)")
    Double sumTotalVentas();

    /**
     * VENTAS NETAS POR MES Y AÑO
     * Calcula la recaudación de los pedidos entregados en un mes y año específicos.
     */
    @Query("SELECT SUM(p.precioFinal) FROM Pedido p WHERE p.estadoActual = 'ENTREGADO' " +
           "AND MONTH(p.fechaCreacion) = :mes " +
           "AND YEAR(p.fechaCreacion) = :anio")
    Double sumTotalVentasPorMes(@Param("mes") int mes, @Param("anio") int anio);


    // ========================================================================
    // 2. KPIS DE LA BÓVEDA DE HISTORIAL (AÑO EN CURSO)
    // ========================================================================

    /**
     * TOTAL EXITOSOS (ANUAL)
     * Cuenta todos los expedientes finalizados con éxito desde el 1 de enero.
     */
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.estadoActual = 'ENTREGADO' " +
           "AND YEAR(p.fechaCreacion) = YEAR(CURRENT_DATE)")
    long countExitososAnuales();

    /**
     * RECHAZADOS / FALLIDOS (ANUAL)
     * Cuenta las solicitudes que no pudieron prosperar en el año actual.
     */
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.estadoActual = 'RECHAZADA' " +
           "AND YEAR(p.fechaCreacion) = YEAR(CURRENT_DATE)")
    long countRechazadosAnuales();

    /**
     * TOTAL RECAUDADO (ANUAL)
     * Sumatoria económica del año. 
     * REGLA DE NEGOCIO: Excluye pedidos de Garantía (es_garantia = true) para 
     * no inflar la contabilidad real con trabajos de post-venta a coste $0.
     */
    @Query("SELECT SUM(p.precioFinal) FROM Pedido p WHERE p.estadoActual = 'ENTREGADO' " +
           "AND p.esGarantia = false " +
           "AND YEAR(p.fechaCreacion) = YEAR(CURRENT_DATE)")
    Double sumRecaudadoAnual();


    // ========================================================================
    // 3. MOTOR DE LA MÁQUINA DEL TIEMPO (BÚSQUEDA PROFUNDA)
    // ========================================================================

    /**
     * BÚSQUEDA MULTIDOMINIO EN LA NUBE
     * Este es el cerebro del Sub-Buscador histórico. Permite extraer registros 
     * de cualquier año filtrando simultáneamente por Tracking, RUT, Nombre o Email.
     * @param anio - Texto 'TODO' o el año específico (Ej: '2024').
     * @param anioNum - Valor numérico del año para la función YEAR de SQL.
     * @param query - El texto de búsqueda ingresado por Luis.
     */
    @EntityGraph(attributePaths = {"cliente", "detallesTecnicos"})
    @Query("SELECT p FROM Pedido p JOIN p.cliente c " +
           "WHERE (:anio = 'TODO' OR YEAR(p.fechaCreacion) = :anioNum) " +
           "AND (p.codigoSeguimiento LIKE %:query% " +
           "OR c.nombre LIKE %:query% " +
           "OR c.email LIKE %:query% " +
           "OR c.rut LIKE %:query%) " +
           "ORDER BY p.fechaCreacion DESC")
    List<Pedido> buscarEnBovedaHistorica(
             @Param("anio") String anio, 
             @Param("anioNum") Integer anioNum, 
             @Param("query") String query);


    // ========================================================================
    // 4. CONSULTAS ESTRUCTURALES Y MANTENIMIENTO
    // ========================================================================

    /**
     * Recupera pedidos activos optimizando la carga de Cliente y Detalles 
     * mediante FETCH JOIN para evitar el error de N+1 consultas.
     */
    @EntityGraph(attributePaths = {"cliente", "detallesTecnicos", "pagos"})
    @Query("SELECT p FROM Pedido p LEFT JOIN FETCH p.cliente LEFT JOIN FETCH p.detallesTecnicos WHERE p.activo = true")
    List<Pedido> findAllActive();

    /**
     * Recupera todos los expedientes (incluyendo borrados lógicos) para auditoría total.
     */
    @EntityGraph(attributePaths = {"cliente", "detallesTecnicos"})
    @Query("SELECT p FROM Pedido p LEFT JOIN FETCH p.cliente LEFT JOIN FETCH p.detallesTecnicos")
    List<Pedido> findAllWithRelations();

    /**
     * Localizador para el servicio de purga automática de la nube (MantenimientoService).
     */
    @Query("SELECT p FROM Pedido p WHERE p.estadoActual = :estado AND p.fechaEntregaReal < :limite AND p.activo = true")
    List<Pedido> buscarPedidosParaLimpieza(@Param("estado") String estado, @Param("limite") java.time.LocalDate limite);

    /**
     * Búsqueda rápida por código LEG-XXXX.
     */
    @EntityGraph(attributePaths = {"cliente", "detallesTecnicos"})
    Optional<Pedido> findByCodigoSeguimiento(String codigoSeguimiento);
}
package cl.ipss.legion3d.tracker.backend.repositorios;
// el package indica la ubicación de esta clase dentro del proyecto

import cl.ipss.legion3d.tracker.backend.entidades.DetallesTecnicos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Este archivo se llama DetallesTecnicosRepository y es la herramienta que hace funcionar 
 * nuestra estrategia de normalización. Como decidimos aislar toda la carga técnica pesada 
 * para no saturar la tabla principal de pedidos, necesitamos este repositorio específico para 
 * gestionar la persistencia de las medidas, materiales y requerimientos de cada pieza de forma independiente.
 * 
 * Al heredar de JpaRepository, Spring Boot nos facilita todas las operaciones de base de datos (CRUD) 
 * sin tener que escribir sentencias SQL manuales, manteniendo la seguridad y velocidad de nuestro desarrollo.
 */
@Repository
public interface DetallesTecnicosRepository extends JpaRepository<DetallesTecnicos, Long> {
    
    /**
     * MÉTODO PERSONALIZADO: Recupera los detalles técnicos asociados a un pedido específico utilizando su ID.
     * 
     * Como físicamente los datos técnicos y el pedido maestro viven en tablas separadas, 
     * este método actúa como nuestro puente de búsqueda. Es vital para el panel de Luis porque 
     * le permite consultar las medidas exactas o el material solicitado de un proyecto para poder cotizarlo.
     * 
     * A nivel de arquitectura, el uso de 'Optional' aquí es crucial. Si intentamos buscar los detalles 
     * de un pedido que recién entró y fue rechazado en el Triage (y por ende nunca se le creó este registro técnico), 
     * el 'Optional' absorbe el impacto. De esta forma el sistema comprende que el dato simplemente no existe 
     * y no colapsa arrojando un error de puntero nulo en la pantalla de nuestro administrador.
     */
    Optional<DetallesTecnicos> findByPedidoId(Long pedidoId);
}
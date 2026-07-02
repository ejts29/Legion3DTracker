package cl.ipss.legion3d.tracker.backend.repositorios;
// el package indica la ubicación de esta clase dentro del proyecto

import cl.ipss.legion3d.tracker.backend.entidades.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Este archivo se llama HistorialEstadoRepository y es el motor de persistencia para nuestra auditoría.
 * Aunque visualmente no tiene métodos personalizados escritos por nosotros, a nivel arquitectónico es vital 
 * porque se encarga de guardar permanentemente el rastro de todos los cambios de estado en la base de datos MySQL.
 * 
 * Al heredar de JpaRepository, el framework de Spring Boot nos proporciona automáticamente las herramientas 
 * para insertar nuevos registros de forma segura. Cada vez que nuestra máquina de estados hace avanzar un pedido 
 * a una nueva fase, utilizamos este repositorio para dejar una huella inmutable. Esto es exactamente 
 * lo que le permitirá a Luis hacer análisis posteriores y auditar los tiempos reales de su taller.
 */
@Repository
public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {
}
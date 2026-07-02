package cl.ipss.legion3d.tracker.backend.repositorios;
// el package indica la ubicación de esta clase dentro del proyecto

import cl.ipss.legion3d.tracker.backend.entidades.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Este archivo se llama ClienteRepository y funciona como nuestro puente directo con la base de datos.
 * A nivel de arquitectura, al heredar de JpaRepository, Spring Boot nos otorga automáticamente 
 * todos los métodos básicos para crear, leer, actualizar y borrar (CRUD) registros. 
 * 
 * Tomamos la decisión técnica de usar esta interfaz porque nos evita escribir sentencias SQL manuales 
 * para las operaciones rutinarias, acelerando el desarrollo de nuestro equipo y reduciendo posibles 
 * vulnerabilidades de inyección en la base de datos.
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    /**
     * MÉTODO PERSONALIZADO: Busca un cliente utilizando su correo electrónico.
     * 
     * Como establecimos en el diseño de nuestros DTOs, el email funciona prácticamente como nuestra llave 
     * principal de identificación ya que no manejamos cuentas de usuario tradicionales en esta etapa. 
     * Este método es el corazón de nuestra lógica para mantener la base de datos limpia y evitar duplicidad.
     * 
     * Cuando entra un Webhook desde WordPress, usamos esta consulta para validar si la persona 
     * ya tiene un historial en nuestro taller. Además, al envolver la respuesta en un objeto 'Optional', 
     * blindamos el código para que el servidor no colapse con errores de puntero nulo (NullPointerException) 
     * si resulta ser un cliente totalmente nuevo que no existe en los registros.
     */
    Optional<Cliente> findByEmail(String email);
}
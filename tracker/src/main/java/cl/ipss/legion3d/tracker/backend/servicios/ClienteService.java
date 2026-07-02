package cl.ipss.legion3d.tracker.backend.servicios;

import org.springframework.stereotype.Service;

/**
 * Este archivo se llama ClienteService. Aunque en esta primera versión del sistema se ve como un cascarón vacío, 
 * su existencia refleja una decisión arquitectónica muy madura por parte de nuestro equipo: la separación de capas.
 * 
 * A nivel técnico, la anotación @Service le indica a Spring Boot que este componente será el "cerebro" de las operaciones. 
 * Lo dejamos estructurado desde ya porque en futuras actualizaciones aquí vivirá toda la lógica de negocio compleja 
 * relacionada con las personas (por ejemplo, calcular descuentos por historial de compras o agrupar usuarios 
 * para las campañas de Luis), aislando completamente nuestros repositorios de base de datos de las rutas web. 
 * Es nuestra forma de dejar la fundación lista para escalar el software de manera segura.
 */
@Service
public class ClienteService {
}
package cl.ipss.legion3d.tracker.backend.entidades;
// el package indica la ubicación de esta clase dentro del proyecto 

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * Este archivo se llama HistorialEstado y es el motor de trazabilidad de nuestro sistema. 
 * Funciona como la bitácora o la caja negra de nuestra máquina de estados. 
 * Cada vez que un pedido avanza de una fase a otra (por ejemplo de Triage a Cotizado), 
 * nosotros guardamos un registro automático aquí. Esto le permite a Luis tener una auditoría 
 * completa y medir exactamente los tiempos de respuesta operativos de su taller.
 */
@Entity
// (aqui modificar al desplegar a produccion: el historial es fundamental para auditoría, confirmar nombre de tabla)
@Table(name = "historial_estados")
@Data
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * RELACIÓN: MUCHOS historiales pertenecen a 1 Pedido.
     * @JsonBackReference: CRÍTICO para nuestra API. Como un pedido genera muchos registros de historial a lo largo de su vida, 
     * usamos esta anotación para proteger nuestro sistema. Evitamos que Swagger o Jackson entren en un bucle infinito 
     * al intentar mapear el Pedido dentro del Historial y viceversa (previniendo así el temido Error 500).
     */
    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    // Guardamos la fase previa para saber exactamente de dónde venía el proyecto. 
    // Al ingresar por primera vez como un webhook desde WordPress, es normal y esperado que este campo sea null.
    @Column(name = "estado_anterior")
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false)
    private String estadoNuevo;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;
    
    // NOTA TÉCNICA DE ARQUITECTURA: Decidimos intencionalmente no mapear el campo 'fecha_cambio' en este código Java. 
    // Delegamos esa responsabilidad directamente al motor de la base de datos MySQL para que lo genere automáticamente 
    // con DEFAULT CURRENT_TIMESTAMP. Así nos aseguramos de capturar la hora exacta e inalterable de la transición.
    //por que en cuando intentamos mapear la fecha con @Column(name = "fecha_cambio") private LocalDateTime fechaCambio; nos da error 500 en swagger
}
package cl.ipss.legion3d.tracker.backend.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString; // <--- Agregamos esto para evitar bucles en el log
//import com.fasterxml.jackson.annotation.JsonIdentityInfo;
//import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Este archivo se llama Cliente y es un pilar central de nuestro modelo de datos. 
 * A nivel arquitectónico, es la entidad (la representación en código de nuestra tabla de base de datos) 
 * donde guardamos la información de todas las personas que interactúan con nuestro sistema.
 * 
 * Lo diseñamos pensando en la escalabilidad a largo plazo, estructurando todo para que 
 * un mismo cliente pueda tener un historial de varios proyectos con nosotros a lo largo del tiempo, 
 * sin tener que duplicar sus datos de contacto por cada impresión 3D que nos solicite.
 */
@Entity
// (aqui modificar al desplegar a produccion: confirmar con el DBA si el nombre de la tabla 'clientes' cumple con las normativas de la organización)
@Table(name = "clientes")
@Getter
@Setter
// Excluimos 'pedidos' del toString para que al hacer un System.out.println(cliente) 
// tampoco se cree un bucle infinito en la consola de VS Code. Lo hicimos para proteger 
// la memoria de nuestro servidor y evitar colapsos por un simple log de depuración.
@ToString(exclude = "pedidos")
@SQLDelete(sql = "UPDATE clientes SET activo = false WHERE id = ?")
@SQLRestriction("activo = true")
public class Cliente {
    
    /**
     * Nuestro identificador único autoincremental en la base de datos MySQL.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "nombre")
    private String nombre;
    
    // (aqui modificar al desplegar a produccion: Hay que ver cómo Tratamos la restricción 'unique' es crítica para evitar duplicidad de prospectos)
    /**
     * Como definimos desde la captura del webhook, este campo es vital para nosotros. 
     * Al marcarlo como 'unique' a nivel de base de datos, garantizamos estructuralmente 
     * que no tendremos múltiples registros basura para la misma persona.
     */
    @Column(name = "email", unique = true)
    private String email;
    
    @Column(name = "telefono")
    private String telefono;
    
    /**
     * Identificador comercial que pedimos en fases más avanzadas para poder generar la facturación.
     */
    @Column(name = "rut")
    private String rut;

    // RELACIÓN: 1 Cliente -> Muchos Pedidos
    /**
     * Aquí definimos nuestra relación principal. Usamos FetchType.LAZY (carga perezosa) 
     * como una estrategia de optimización para nuestra base de datos. Esto significa que si Luis 
     * solo quiere ver el nombre y el correo del cliente, el sistema no saturará la memoria 
     * cargando todo el historial pesado de sus pedidos, a menos que se lo pidamos explícitamente.
     */
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // Inicializamos la lista para evitar errores de puntero nulo (NullPointerException)
    @EqualsAndHashCode.Exclude
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Pedido> pedidos = new ArrayList<>();

    @Column(nullable = false)
    private boolean activo = true;
}
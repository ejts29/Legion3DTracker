package cl.ipss.legion3d.tracker.backend.entidades;
// el package indica la ubicación de esta clase dentro del proyecto 

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.ArrayList;
import java.util.List;

// Importamos las herramientas de Hibernate para el "Borrado Lógico" (Soft Delete)
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Este archivo se llama Pedido y es literalmente el corazón de nuestro sistema
 * Tracker.
 * Representa la unidad central de negocio alrededor de la cual giran todas las
 * demás tablas.
 * 
 * A nivel arquitectónico, tomamos una decisión muy importante aquí:
 * implementamos el "Borrado Lógico" (Soft Delete).
 * Esto significa que si nuestro administrador (Luis) decide eliminar un pedido
 * desde su panel,
 * nuestro sistema no lo borrará físicamente de la base de datos MySQL. En su
 * lugar, simplemente lo ocultará
 * cambiando su estado a inactivo. Hicimos esto para proteger la integridad de
 * los datos históricos del taller
 * y no perder métricas valiosas de ventas o auditorías a largo plazo.
 */
@Entity
// (aqui modificar al desplegar a produccion: tabla principal de negocio,
// asegurar que el nombre 'pedidos' sea el definitivo)
@Table(name = "pedidos")
@Getter
@Setter
// Estas dos líneas son la magia de nuestro borrado lógico. Cuando le decimos a
// JPA que elimine un pedido,
// Hibernate intercepta la orden y ejecuta un UPDATE en lugar de un DELETE.
// Además, configuramos una restricción global para que el sistema solo nos
// muestre los pedidos que sigan activos.
@SQLDelete(sql = "UPDATE pedidos SET activo = false WHERE id=?")
@SQLRestriction("activo = true")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private java.time.LocalDateTime fechaCreacion = java.time.LocalDateTime.now();

    /**
     * RELACIÓN: MUCHOS Pedidos pertenecen a 1 Cliente.
     * Al igual que en nuestras otras entidades, tenemos mucho cuidado con la
     * recursión.
     * Esta relación conecta el proyecto con su dueño, permitiendo que un mismo
     * cliente
     * pueda tener un historial de múltiples solicitudes en nuestro taller.
     */
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private Cliente cliente;

    @Column(name = "codigo_seguimiento", unique = true)
    private String codigoSeguimiento;

    @Column(name = "servicio_solicitado")
    private String servicioSolicitado;

    @Column(name = "tiene_archivo_inicial")
    private Boolean tieneArchivoInicial;

    @Column(name = "link_archivo_inicial", updatable = false)
    private String linkArchivoInicial;

    // (aqui modificar al desplegar a produccion: el tipo TEXT permite hasta 64KB,
    // evaluar MEDIUMTEXT si los mensajes son masivos)
    @Column(name = "mensaje_original", columnDefinition = "TEXT")
    private String mensajeOriginal;

    @Column(name = "origen_contacto")
    private String origenContacto;

    @Column(name = "estado_actual")
    private String estadoActual;

    @Column(name = "precio_final")
    private Double precioFinal;

    @Column(name = "precio_original")
    private Double precioOriginal;

    @Column(name = "descuento_porcentaje")
    private Double descuentoPorcentaje = 0.0;

    @Column(name = "link_comprobante_pago")
    private String linkComprobantePago;

    @Column(name = "link_comprobante_envio")
    private String linkComprobanteEnvio;

    @Column(name = "fecha_entrega_estimada")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private java.time.LocalDate fechaEntregaEstimada;

    // (aqui modificar al desplegar a produccion: evaluar si el triage requiere más
    // espacio que un TEXT estándar)
    /**
     * Este campo almacena la justificación de Luis. Si él rechaza un proyecto por
     * ser inviable
     * o si lo aprueba con una nota técnica, guardamos ese comentario aquí para
     * enviárselo al cliente.
     */
    @Column(name = "mensaje_triage", columnDefinition = "TEXT")
    private String mensajeTriage;

    @Column(name = "anotaciones_internas", columnDefinition = "TEXT")
    private String anotacionesInternas;

    @Column(name = "fecha_vencimiento_presupuesto")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private java.time.LocalDate fechaVencimientoPresupuesto;

    /**
     * La bandera principal de nuestro borrado lógico. Por defecto, todo pedido nace
     * estando activo (true).
     */
    @Column(name = "activo")
    private Boolean activo = true;

    /**
     * MÓDULO DE GARANTÍAS (SOFT CLONE)
     * Estos campos permiten la trazabilidad de postventa.
     */
    @Column(name = "pedido_padre_id")
    private Long pedidoPadreId;

    @Column(name = "es_garantia")
    private boolean esGarantia = false;

    /**
     * RELACIÓN: 1 Pedido tiene 1 Formulario de Detalles Técnicos.
     * Aquí es donde se materializa la separación de tablas que tanto analizamos
     * como equipo.
     * Dejamos que la entidad DetallesTecnicos sea la dueña física de la relación
     * (mappedBy),
     * manteniendo esta tabla maestra de Pedidos libre de columnas vacías cuando los
     * proyectos mueren en la fase inicial.
     */
    @com.fasterxml.jackson.annotation.JsonManagedReference
    @OneToOne(mappedBy = "pedido", cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH,
            CascadeType.DETACH })
    private DetallesTecnicos detallesTecnicos;

    /**
     * RELACIÓN: 1 Pedido tiene MUCHOS registros de Historial de Estados.
     * Conectamos nuestra tabla de trazabilidad mediante una cascada controlada.
     * Esto asegura que las actualizaciones se propaguen sin riesgo de borrado
     * físico.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HistorialEstado> historialEstados = new ArrayList<>();

    @org.hibernate.annotations.BatchSize(size = 50)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Pago> pagos = new ArrayList<>();

    @Column(name = "fecha_entrega_real")
    private java.time.LocalDate fechaEntregaReal;

    public java.time.LocalDate getFechaEntregaReal() {
        return fechaEntregaReal;
    }

    public void setFechaEntregaReal(java.time.LocalDate fechaEntregaReal) {
        this.fechaEntregaReal = fechaEntregaReal;
    }
    
    // Añadir estos campos a la clase Pedido en tu proyecto Java
@Column(name = "desglose_costos", columnDefinition = "TEXT")
private String desgloseCostos;

@Column(name = "notas_auditoria", columnDefinition = "TEXT")
private String notasAuditoria;

@Column(name = "justificacion_cliente", columnDefinition = "TEXT")
private String justificacionCliente;

@Column(name = "resumen_financiero_operador", columnDefinition = "TEXT")
private String resumenFinancieroOperador;

    /**
     * Retorna la referencia del último comprobante de pago subido.
     * Prioriza el link directo del pedido y luego recorre el historial de pagos.
     */
    public String getUltimoComprobante() {
        if (this.linkComprobantePago != null && !this.linkComprobantePago.isBlank()) {
            return this.linkComprobantePago;
        }
        if (this.pagos == null || this.pagos.isEmpty()) {
            return "";
        }
        // Buscamos el último pago con referencia no nula ni vacía
        for (int i = this.pagos.size() - 1; i >= 0; i--) {
            String ref = this.pagos.get(i).getReferenciaComprobante();
            if (ref != null && !ref.isBlank()) {
                return ref;
            }
        }
        return "";
    }

    /**
     * Retorna la cantidad de pagos registrados de forma segura.
     */
    public int getPagosCount() {
        return this.pagos != null ? this.pagos.size() : 0;
    }

}
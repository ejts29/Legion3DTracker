package cl.ipss.legion3d.tracker.backend.entidades;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonBackReference;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * ENTIDAD: DETALLES TÉCNICOS (Ingeniería de Producto)
 * ----------------------------------------------------------------------------
 * Esta clase es el resultado de la normalización de nuestra base de datos.
 * Decidimos separar la carga técnica pesada en esta entidad exclusiva para 
 * mantener la tabla maestra de pedidos ligera y eficiente.
 * * Se vincula mediante una relación 1:1 con Pedido. Solo se llena cuando el 
 * proyecto supera la fase inicial de Triage.
 */
@Entity
@Table(name = "detalles_tecnicos")
@Data
// Implementación de Borrado Lógico: Protege la integridad histórica del taller.
@SQLDelete(sql = "UPDATE detalles_tecnicos SET activo = false WHERE id=?")
@SQLRestriction("activo = true")
public class DetallesTecnicos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activo")
    private boolean activo = true;

    /**
     * RELACIÓN 1 a 1: Vinculación con el Pedido Maestro.
     * @JsonBackReference: Evita la recursión infinita en la API y errores 500 en Swagger.
     */
    @JsonBackReference
    @OneToOne
    @JoinColumn(name = "pedido_id", referencedColumnName = "id", nullable = false, unique = true)
    private Pedido pedido;

    // --- ESPECIFICACIONES FÍSICAS (MILIMÉTRICAS) ---
    @Column(name = "medida_ancho") private Double medidaAncho;
    @Column(name = "medida_alto") private Double medidaAlto;
    @Column(name = "medida_profundidad") private Double medidaProfundidad;
    @Column(name = "cantidad_unidades") private Integer cantidadUnidades;
    @Column(name = "dias_entrega") private String diasEntrega;

    // --- LOGÍSTICA DE PIEZAS FÍSICAS (FORMULARIO 2) ---
    // Estos campos capturan si el cliente enviará una muestra física para clonar.
    @Column(name = "tiene_pieza_fisica") private Boolean tienePiezaFisica;
    @Column(name = "necesita_modificacion") private String necesitaModificacion;
    @Column(name = "es_copia_exacta") private Boolean esCopiaExacta;

    // --- MATERIALES Y ACABADOS ---
    @Column(name = "material_solicitado") private String materialSolicitado;
    @Column(name = "color_solicitado") private String colorSolicitado;
    @Column(name = "entorno_uso") private String entornoUso;
    @Column(name = "presupuesto_estimado") private String presupuestoEstimado;
    @Column(name = "metodo_entrega") private String metodoEntrega;

    // --- VALIDACIÓN TÉCNICA Y COMERCIAL ---
    @Column(name = "tolerancia_check") private boolean toleranciaCheck;
    @Column(name = "rut") private String rut;
    @Column(name = "telefono_contacto") private String telefonoContacto;

    // --- DATOS ESTRUCTURADOS DE DESPACHO ---
    @Column(name = "region") private String region;
    @Column(name = "comuna") private String comuna;
    @Column(name = "calle_y_numero") private String calleYNumero;
    @Column(name = "depto_casa_oficina") private String deptoCasaOficina;
    
    @Column(name = "informacion_adicional", length = 1000) 
    private String informacionAdicional;
    
    @Column(name = "tipo_envio_starken") private String tipoEnvioStarken; 
    
    // --- GESTIÓN DE ARCHIVOS Y NUBE (DRIVE) ---
    @Column(name = "link_archivo_final", length = 1000) 
    private String linkArchivoFinal;
    
    @Column(name = "drive_file_id") 
    private String driveFileId;

    @Column(name = "link_formulario_ingenieria", length = 1000)
    private String linkFormularioIngenieria;
}
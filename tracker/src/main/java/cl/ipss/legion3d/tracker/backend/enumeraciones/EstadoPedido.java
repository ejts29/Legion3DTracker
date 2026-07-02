package cl.ipss.legion3d.tracker.backend.enumeraciones;
// el package indica la ubicación de esta clase dentro del proyecto

/**
 * Este archivo se llama EstadoPedido y es la columna vertebral de nuestra máquina de estados.
 * A nivel arquitectónico, decidimos usar una ENUMERACIÓN (una lista cerrada y estricta de valores constantes).
 */
public enum EstadoPedido {
    
    // (aqui modificar al desplegar a produccion: si se agregan etapas en el funnel de ventas de WordPress, añadir aquí los nuevos estados)
    NUEVA,                 // Etapa 1: Cae el Webhook
    PENDIENTE_TECNICOS,    // Etapa 2A: Luis aprueba y espera datos
    RECHAZADA,             // Etapa 2B: Luis no puede hacer la pieza
    EN_EVALUACION,         // Etapa 3: Cliente llenó el formulario técnico
    PRESUPUESTADO,         // Etapa 4: Luis le puso precio y fecha límite
    EXPIRADO,              // Etapa 6B: Pasó el tiempo y no pagaron
    EN_PRODUCCION,         // Etapa 6A: Pagaron y se está imprimiendo
    LISTO_PARA_ENTREGA,    // Etapa 7: Impresión terminada
    ENTREGADO;             // Etapa 8: Ciclo cerrado, pieza despachada

    /* 
     * NOTA ARQUITECTÓNICA DEL EQUIPO:
     * Agrupamos la explicación técnica aquí abajo para mantener limpio el bloque superior 
     * y permitir una lectura visual rápida del ciclo de vida del requerimiento.
     * 
     * 1. Prevención de errores: Este diccionario estricto obliga a que todo nuestro sistema hable 
     * exactamente el mismo idioma. Sirve para obligar a que el sistema solo acepte estos estados exactos, 
     * evitando que un error de tipeo humano rompa el flujo operativo (ej. escribir "En_Produccion" 
     * en vez de "EN_PRODUCCION").
     * 
     * 2. Trazabilidad del Negocio: Cada una de estas etapas refleja el modelo real del taller. 
     * Desde el momento en que entra el prospecto hasta que se despacha la pieza, 
     * le garantizamos a Luis que cada solicitud tendrá un seguimiento perfecto en su base de datos.
     */
}
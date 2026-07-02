/**
 * LEGIÓN 3D TRACKER - MOTOR DE COMUNICACIÓN (API)
 * Este archivo centraliza todas las llamadas al Backend Spring Boot.
 * A nivel de arquitectura frontend, aplicar este patrón (un ApiService único) es vital. 
 * En lugar de tener las rutas (URLs) de internet esparcidas por decenas de archivos HTML o JS, 
 * centralizamos todo aquí. Si el día de mañana la ruta del servidor cambia, 
 * solo tenemos que modificar una sola línea de código en todo el proyecto.
 */

// (aqui modificar al desplegar a produccion: cambiar localhost por el dominio real con HTTPS, por ejemplo "https://api.legion3d.cl/api/v1")
// IMPORTANTE: En producción "localhost" apunta a la propia máquina del usuario. Si no cambiamos esto, cuando el cliente abra la web en su celular, el teléfono intentará buscar el servidor de Legión 3D dentro de sí mismo y el sistema fallará.
const API_BASE_URL = "/api/v1";

const ApiService = {

    /**
     * 1. Listar pedidos para los dashboards de Luis
     * Utilizamos 'async/await' para manejar la asincronía. Esto significa que nuestro frontend 
     * esperará pacientemente a que Spring Boot recolecte los datos de la base de datos 
     * antes de intentar dibujar las tarjetas en la pantalla, evitando errores visuales o pantallas en blanco.
     */
    // Método para obtener la lista de pedidos desde el backend
    // Este método se llama desde el dashboard de Luis para cargar los pedidos en las tarjetas
    // El método 'fetch' es la forma moderna de hacer peticiones HTTP en JavaScript.
    // tenemos try/catch para capturar cualquier error de red o del servidor y manejarlo adecuadamente en el frontend.
    async obtenerPedidos() {
        try {
            // fetch realiza una petición GET por defecto
            const response = await fetch(`${API_BASE_URL}/pedidos`);
            if (!response.ok) throw new Error("Error al obtener pedidos");
            return await response.json();
        } catch (error) {
            console.error("Error API:", error);
            throw error;
        }
    },

    /**
     * 2. Motor de cambio de flujo (Mueve el pedido entre etapas)
     * Este es el gatillo frontend de nuestra máquina de estados. 
     * Usamos el método 'PATCH' (en lugar de PUT o POST) porque es la convención técnica correcta 
     * en las APIs REST cuando solo queremos modificar una fracción del registro 
     * (en este caso, solo actualizamos el estado y un posible mensaje, sin sobreescribir todo el pedido).
     */
    // Método para actualizar el estado de un pedido (moverlo entre etapas)
    // Este método se llama desde los botones de acción en cada tarjeta del dashboard de Luis
    // El objeto 'datos' puede contener el nuevo estado y un mensaje opcional para el triage Ejemplo de 'datos': { nuevoEstado: 'EN_PRODUCCION', mensajeTriage: '...' }
    // usamos JSON.stringify para convertir el objeto JavaScript en un texto JSON que el backend pueda entender, y establecemos el header 'Content-Type' a 'application/json' para que Spring Boot sepa cómo interpretar la información entrante.
    //usamos try/catch para manejar cualquier error que pueda surgir durante la comunicación con el backend, como problemas de red o errores del servidor, y así evitar que el frontend se rompa o quede en un estado inconsistente. En caso de error, se lanza una excepción que puede ser capturada por el código que llamó a este método para mostrar un mensaje de error al usuario o tomar otras acciones correctivas.
    async actualizarEstado(pedidoId, datos) {
        // datos: { nuevoEstado: 'EN_PRODUCCION', mensajeTriage: '...' }
        try {
            const response = await fetch(`${API_BASE_URL}/pedidos/${pedidoId}/estado`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(datos) // Convertimos el objeto JS a un texto JSON para el backend
            });
            if (!response.ok) throw new Error("No se pudo actualizar el estado");
            return await response.json();
        } catch (error) {
            console.error("Error API:", error);
            throw error;
        }
    },

    /**
     * 3. Guardar detalles de ingeniería (Etapa 3)
     * Este método captura todas las medidas y requerimientos del formulario técnico.
     * Usamos 'PUT' porque aquí sí estamos inyectando o actualizando un bloque completo y masivo de información.
     */
    // Método para guardar los detalles técnicos de un pedido (medidas, requerimientos, etc.)
    // Este método se llama desde el formulario de ingeniería en la etapa 3
    // El objeto 'detallesDTO' es un Data Transfer Object que contiene toda la información técnica que el cliente ha ingresado en el formulario. Ejemplo: { medidas: {...}, requerimientos: '...' }
    // Al igual que en el método anterior, usamos JSON.stringify para enviar esta información al backend en formato JSON, y establecemos el header 'Content-Type' para que Spring Boot pueda procesarla correctamente. El uso de 'PUT' aquí es intencional porque estamos reemplazando o actualizando un bloque completo de detalles técnicos asociados a ese pedido.
    // Usamos try/catch para manejar cualquier error que pueda surgir durante la comunicación con el backend, como problemas de red o errores del servidor. Si ocurre un error, se lanza una excepción que puede ser capturada por el código que llamó a este método para mostrar un mensaje de error al usuario o tomar otras acciones correctivas.
    // Sustituir guardarIngenieria: usar POST y codigoTracker
    async guardarIngenieria(codigoTracker, detallesDTO) {
        try {
            const response = await fetch(`${API_BASE_URL}/pedidos/${codigoTracker}/detalles-tecnicos`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(detallesDTO)
            });
            if (!response.ok) throw new Error("Error al guardar ingeniería");
            return await response.json();
        } catch (error) {
            console.error("Error API:", error);
            throw error;
        }
    },

    // Añadir registrarPagoManual (para F2 y botón Revisar Banco)
    async registrarPagoManual(pedidoId, monto, metodo, concepto) {
        const url = `${API_BASE_URL}/pedidos/${pedidoId}/pago-manual?monto=${monto}&metodoPago=${metodo}&concepto=${concepto}`;
        try {
            const response = await fetch(url, { method: 'POST' });
            if (!response.ok) throw new Error("Error en registro de pago");
            return await response.text();
        } catch (error) {
            console.error("Error API Pago:", error);
            throw error;
        }
    },

    /**
     * 4. Subir archivos (Comprobantes de pago o STL)
     * NOTA TÉCNICA CRÍTICA: A diferencia de los métodos anteriores, aquí NO usamos JSON. 
     * Los archivos binarios (fotos, PDFs) romperían la estructura de un JSON tradicional. 
     * Por eso utilizamos la interfaz 'FormData', que emula el comportamiento de un formulario HTML clásico 
     * y permite empaquetar archivos pesados de forma segura para enviarlos a nuestro Spring Boot.
     */
    // Método para subir archivos relacionados a un pedido (comprobante de pago o archivo 3D)
    // Este método se llama desde los formularios donde el cliente puede adjuntar archivos, como el comprobante de pago o el modelo 3D en formato STL.
    // El parámetro 'fileField' es el nombre del campo que el backend espera para ese archivo específico (por ejemplo, 'comprobantePago' o 'archivo3d'), y 'file' es el objeto File que representa el archivo seleccionado por el usuario.
   // Usamos 'FormData' para construir un cuerpo de petición que pueda manejar archivos binarios. FormData se encarga de establecer los headers correctos (multipart/form-data) automáticamente, lo que es crucial para que Spring Boot pueda procesar el archivo correctamente. En este método, no necesitamos establecer manualmente los headers de Content-Type, ya que FormData lo hace por nosotros. De hecho, si intentáramos establecerlo manualmente, podríamos causar un error en la solicitud.
    // Usamos try/catch para manejar cualquier error que pueda surgir durante la comunicación con el backend, como problemas de red o errores del servidor. Si ocurre un error, se lanza una excepción que puede ser capturada por el código que llamó a este método para mostrar un mensaje de error al usuario o tomar otras acciones correctivas.
    async subirArchivo(pedidoId, fileField, file) {
        const formData = new FormData();
        formData.append(fileField, file); // fileField puede ser 'comprobantePago' o 'archivo3d'

        try {
            const response = await fetch(`${API_BASE_URL}/pedidos/${pedidoId}/archivos`, {
                method: 'POST',
                // IMPORTANTE: Al usar FormData, la función fetch configura automáticamente los headers correctos 
                // (multipart/form-data), por lo que no debemos escribirlos manualmente aquí o causaremos un error.
                body: formData
            });
            if (!response.ok) throw new Error("Error al subir el archivo");
            return await response.json();
        } catch (error) {
            console.error("Error API (File):", error);
            throw error;
        }
    }
};
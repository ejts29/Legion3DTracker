/**
 * LEGIÓN 3D TRACKER - UTILIDADES GLOBALES (main.js)

 * Este archivo centraliza las funciones transversales de nuestra interfaz de usuario.
 * A nivel arquitectónico, decidimos crear este script global para no tener que repetir 
 * el mismo comportamiento en cada pantalla de nuestro sistema. Aquí manejamos la experiencia 
 * visual de Luis y, más importante aún, implementamos defensas críticas en el frontend 
 * para proteger nuestra base de datos.
 */

document.addEventListener('DOMContentLoaded', () => {
    console.log("Legión 3D UI - Inicializada");

    // 1. AUTO-OCULTAR ALERTAS (Mejora de Experiencia de Usuario)
    // Para evitar que el panel de Luis se llene de notificaciones antiguas y confusas, 
    // diseñamos este bloque que desaparece suavemente los avisos de éxito o error 
    // después de 5 segundos, manteniendo su área de trabajo siempre limpia.
    const alertas = document.querySelectorAll('.alert:not(.alert-permanent)');
    alertas.forEach(alerta => {
        setTimeout(() => {
            alerta.style.transition = "opacity 0.5s ease";
            alerta.style.opacity = "0";
            setTimeout(() => alerta.remove(), 500);
        }, 5000);
    });

    // 2. PREVENIR DOBLE CLIC EN FORMULARIOS (Protección de Base de Datos y UX)
    // A nivel arquitectónico, esta es una de nuestras defensas más importantes en el lado del cliente.
    // Al deshabilitar el botón de envío y mostrar el texto "Procesando...", no solo le damos un excelente 
    // feedback visual al usuario (sabe que el sistema está trabajando), sino que cortamos de raíz la posibilidad 
    // de que un doble clic rápido genere registros duplicados en nuestra base de datos MySQL.
    // Este comportamiento se aplica a todos los formularios del sistema, garantizando una protección consistente sin necesidad de repetir código en cada pantalla. 
    const formularios = document.querySelectorAll('form');
    formularios.forEach(form => {
        form.addEventListener('submit', function (e) {
            // Solo si el formulario es válido según HTML5
            if (this.checkValidity()) {
                const btnSubmit = this.querySelector('button[type="submit"]');
                if (btnSubmit) {
                    // Cambiar texto a "Procesando..." y deshabilitar
                    const textoOriginal = btnSubmit.innerHTML;
                    btnSubmit.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i> Procesando...';
                    btnSubmit.disabled = true;
                    btnSubmit.classList.add('opacity-75');
                }
            }
        });
    });

    // 3. INICIALIZAR TOOLTIPS DE BOOTSTRAP (Ayudas visuales)
    // Preparamos el motor visual para poder colocar pequeños iconos de información interactivos 
    // en el panel, guiando al usuario sin recargar la pantalla con textos largos.
    // A nivel arquitectónico, esta es una mejora de experiencia que no solo hace que el sistema sea más amigable,
    // sino que también reduce la necesidad de soporte técnico, ya que los usuarios pueden obtener ayuda contextual 
    // al instante. usamos tooltips en elementos clave como botones de acción, campos de formulario 
    // y secciones de información para que Luis siempre tenga una guía clara sobre qué esperar al 
    // interactuar con cada parte del sistema.y cost tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // 4. PREVENIR CAMBIO ACCIDENTAL EN INPUTS NUMÉRICOS AL HACER SCROLL (UX Fix)
    // Esto asegura que al hacer scroll con el ratón, los campos numéricos como las medidas en mm 
    // no cambien accidentalmente de valor si tienen el foco. Al ejecutar blur(), devolvemos 
    // el control del scroll a la página. La opción passive: true mejora el rendimiento.
    document.addEventListener('wheel', function(event) {
        if (document.activeElement && document.activeElement.type === 'number') {
            document.activeElement.blur();
        }
    }, { passive: true });
    // 5. VALIDACIÓN DE SEGURIDAD PREVENTIVA GLOBAL PARA ARCHIVOS
    // Protegemos el servidor asegurando que ningún archivo subido (STL, PNG, JPG, PDF, ZIP)
    // exceda los 20MB antes de que se intente enviar el formulario.
    document.addEventListener('change', function(e) {
        if (e.target && e.target.type === 'file') {
            const inputArchivo = e.target;
            if (inputArchivo.files.length > 0) {
                const archivo = inputArchivo.files[0];
                const tamañoEnMB = archivo.size / (1024 * 1024);
                if (tamañoEnMB > 20) {
                    alert("⚠️ El archivo es demasiado pesado (" + tamañoEnMB.toFixed(1) + " MB). El límite máximo es 20 MB. Por favor, usa un enlace externo o comprime tu archivo.");
                    inputArchivo.value = ''; // Resetea el input para prevenir el envío
                }
            }
        }
    });
    
    // 6. MECANISMO DE ATAJO GLOBAL (Tecla F2)
    document.addEventListener('keydown', (e) => {
        if (e.key === 'F2') {
            e.preventDefault();
            const inputSidebar = document.getElementById('inputOrdenSidebar');
            if (inputSidebar) {
                inputSidebar.focus();
                const idExtraido = inputSidebar.value.trim();
                if (idExtraido !== "") {
                    window.abrirModalPagos(idExtraido, null);
                } else {
                    inputSidebar.style.borderColor = '#F2C05F';
                    setTimeout(() => inputSidebar.style.borderColor = '#333', 500);
                }
            }
        }
    });
});

/**
 * UTILIDAD: Formatear números a Moneda Chilena (CLP)
 * Como nuestro modelo de negocio incluye la generación de cotizaciones formales, 
 * creamos esta función global para garantizar que en cualquier parte del sistema 
 * donde se muestre un precio, este respete siempre el formato financiero local (ejemplo: $ 150.000).
 * 
 * @param {number} valor - Monto a formatear
 * @returns {string} - Ej: $ 150.000
 */
// Esta función es especialmente útil para mostrar precios en las cotizaciones, asegurando que Luis siempre vea los números de manera clara y profesional, lo que refuerza la confianza en nuestro sistema.
// usamos la API de Internacionalización de JavaScript para formatear los números según las convenciones, 
// eliminando decimales y agregando el símbolo de peso. function formatearCLP(valor) { es una función que toma un número 
// y lo convierte en una cadena formateada como moneda, lo que es esencial para mantener la coherencia visual 
// y profesionalismo en las cotizaciones que generamos para nuestros clientes. 
function formatearCLP(valor) {
    if (isNaN(valor)) return "$ 0";
    return new Intl.NumberFormat('es-CL', {
        style: 'currency',
        currency: 'CLP',
        minimumFractionDigits: 0
    }).format(valor);
}

// ---------------------------------------------------------
// LÓGICA DE GESTIÓN DE PAGOS MANUALES (ADMINISTRACIÓN)
// ---------------------------------------------------------

// Variable para instanciar el modal de Bootstrap
let modalPagosObj = null;

document.addEventListener('DOMContentLoaded', () => {
    const modalEl = document.getElementById('modalPagosL3D');
    if(modalEl) modalPagosObj = new bootstrap.Modal(modalEl);
});

// ========================================================
// MÓDULO FINANCIERO: SEPARACIÓN DE MOTORES
// ========================================================

/**
 * 1. MOTOR DE PAGO MANUAL (Vía Sidebar F2)
 * Se usa cuando el dinero físico ya está en el taller. No requiere validación de archivos.
 */
window.abrirModalPagoManual = function(id) {
    console.log("Iniciando Pago Manual (F2) para Pedido ID:", id);
    
    const inputId = document.getElementById('pagoPedidoId');
    if (inputId) inputId.value = id;
    
    // Preparar el modal para ingreso físico
    const inputMonto = document.getElementById('pagoMontoBruto');
    if (inputMonto) inputMonto.value = '';
    
    const inputConcepto = document.getElementById('pagoConcepto');
    if (inputConcepto) inputConcepto.value = 'Pago Presencial/Efectivo en Taller';
    
    // Ocultar elementos de auditoría
    const divVoucher = document.getElementById('divPagoVoucher');
    if (divVoucher) divVoucher.style.display = 'none';
    
    const alertBruto = document.getElementById('alertaMontoBruto');
    if (alertBruto) alertBruto.style.display = 'none';

    const modalEl = document.getElementById('modalPagosL3D');
    if (modalEl) {
        new bootstrap.Modal(modalEl).show();
    }
};


// --- MÓDULO DE PRODUCCIÓN Y LOGÍSTICA (RESTAURADO) ---
let pedidoActivo = null;
let modalFinalizarObj = null;
let modalDespacharObj = null;
let modalRecursosObj = null;

document.addEventListener("DOMContentLoaded", function() {
    const mf = document.getElementById('modalFinalizar');
    const md = document.getElementById('modalDespachar');
    const mr = document.getElementById('modalRecursos');
    if(mf) modalFinalizarObj = new bootstrap.Modal(mf);
    if(md) modalDespacharObj = new bootstrap.Modal(md);
    if(mr) modalRecursosObj = new bootstrap.Modal(mr);
});

async function abrirRecursos(id) {
    const btnArchivo = document.getElementById('btn-recurso-archivo');
    const btnLink = document.getElementById('btn-recurso-link');
    const msg = document.getElementById('no-recursos-msg');
    
    if(!btnArchivo || !btnLink || !msg) return;

    btnArchivo.classList.add('d-none');
    btnLink.classList.add('d-none');
    msg.classList.add('d-none');

    try {
        const response = await fetch(`/api/v1/pedidos/${id}`);
        if (response.ok) {
            const pedido = await response.json();
            const dt = pedido.detallesTecnicos || {};
            let count = 0;

            if (pedido.linkArchivoInicial) {
                btnArchivo.classList.remove('d-none');
                btnArchivo.onclick = () => descargarArchivoFisico(pedido.id);
                count++;
            }

            if (dt.linkArchivoFinal && dt.linkArchivoFinal.startsWith('http')) {
                btnLink.classList.remove('d-none');
                btnLink.onclick = () => window.open(dt.linkArchivoFinal, '_blank');
                count++;
            }

            if (count === 0) msg.classList.remove('d-none');
            if(modalRecursosObj) modalRecursosObj.show();
        }
    } catch (e) { console.error("Error al cargar recursos", e); }
}

function abrirModalFinalizar(id, tracking) {
    pedidoActivo = id;
    const span = document.getElementById('span-finalizar-tracking');
    if(span) span.innerText = tracking;
    if(modalFinalizarObj) modalFinalizarObj.show();
}

async function ejecutarFinalizar(btn) {
    if(!pedidoActivo) return;
    const txtOriginal = btn.innerHTML;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i> Procesando...';
    btn.disabled = true;

    try {
        const response = await fetch(`/api/v1/pedidos/${pedidoActivo}/estado`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nuevoEstado: "LISTO_PARA_ENTREGA", mensajeTriage: "Pieza finalizada en producción." })
        });

        if (response.ok) {
            Swal.fire({ icon: 'success', title: '¡Finalizado!', text: 'El cliente ha sido notificado.' }).then(() => location.reload());
        } else {
            const error = await response.text();
            throw new Error(error);
        }
    } catch(e) {
        Swal.fire({ icon: 'error', title: 'Error', text: e.message });
        btn.innerHTML = txtOriginal; btn.disabled = false;
    }
}

function abrirModalDespachar(id, tracking, metodoSugerido) {
    pedidoActivo = id;
    const span = document.getElementById('span-despachar-tracking');
    if(span) span.innerText = tracking;
    
    const select = document.getElementById('selectMetodoDespacho');
    if(select) {
        const m = (metodoSugerido || "").toLowerCase();
        if(m.includes("retiro")) select.value = "RETIRO";
        else if(m.includes("digital") || m.includes("diseño")) select.value = "DIGITAL";
        else select.value = "STARKEN";
        cambiarCamposDespacho();
    }
    
    const inputTrack = document.getElementById('inputTrackingLink');
    const inputDigi = document.getElementById('inputDigitalLink');
    if(inputTrack) inputTrack.value = "";
    if(inputDigi) inputDigi.value = "";
    
    if(modalDespacharObj) modalDespacharObj.show();
}

function cambiarCamposDespacho() {
    const sel = document.getElementById('selectMetodoDespacho');
    if(!sel) return;
    const metodo = sel.value;
    const bFisico = document.getElementById('bloqueFisico');
    const bDigital = document.getElementById('bloqueDigital');
    const inputTrack = document.getElementById('inputTrackingLink');

    if(metodo === "STARKEN") {
        if(bFisico) bFisico.classList.remove('d-none'); 
        if(bDigital) bDigital.classList.add('d-none');
        if(inputTrack) { inputTrack.placeholder = "https://starken.cl/seguimiento/..."; inputTrack.disabled = false; }
    } else if (metodo === "RETIRO") {
        if(bFisico) bFisico.classList.remove('d-none'); 
        if(bDigital) bDigital.classList.add('d-none');
        if(inputTrack) { inputTrack.value = "Retiro Presencial"; inputTrack.disabled = true; }
    } else if (metodo === "DIGITAL") {
        if(bFisico) bFisico.classList.add('d-none'); 
        if(bDigital) bDigital.classList.remove('d-none');
    } else if (metodo === "MIXTO") {
        if(bFisico) bFisico.classList.remove('d-none'); 
        if(bDigital) bDigital.classList.remove('d-none');
        if(inputTrack) { inputTrack.disabled = false; inputTrack.placeholder = "Link Starken o envío físico..."; }
    }
}

async function ejecutarDespacho(btn) {
    if(!pedidoActivo) return;
    const metodo = document.getElementById('selectMetodoDespacho').value;
    let trackLink = document.getElementById('inputTrackingLink').value.trim();
    let digitalLink = document.getElementById('inputDigitalLink').value.trim();

    if ((metodo === "STARKEN" || metodo === "MIXTO") && !trackLink) {
        Swal.fire({ icon: 'warning', text: "Debes ingresar el seguimiento físico." }); return;
    }
    if ((metodo === "DIGITAL" || metodo === "MIXTO") && !digitalLink) {
        Swal.fire({ icon: 'warning', text: "Debes ingresar el enlace digital." }); return;
    }

    const txtOriginal = btn.innerHTML;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i> Cerrando...';
    btn.disabled = true;

    try {
        const response = await fetch(`/api/v1/pedidos/${pedidoActivo}/despacho`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ 
                metodoRealLuis: metodo,
                linkComprobanteEnvio: trackLink,
                linkArchivoDigital: digitalLink
            })
        });

        if (response.ok) {
            Swal.fire({ icon: 'success', title: '¡Entregado!' }).then(() => location.reload());
        } else {
            const error = await response.text();
            throw new Error(error);
        }
    } catch(e) {
        Swal.fire({ icon: 'error', title: 'Error', text: e.message });
        btn.innerHTML = txtOriginal; btn.disabled = false;
    }
}

function filtrarTabla(inputId, tablaId) {
    const inp = document.getElementById(inputId);
    const tab = document.getElementById(tablaId);
    if(!inp || !tab) return;
    let filter = inp.value.toUpperCase();
    let trs = tab.getElementsByTagName("tr");
    for (let i = 1; i < trs.length; i++) {
        let txt = trs[i].textContent || trs[i].innerText;
        trs[i].style.display = txt.toUpperCase().indexOf(filter) > -1 ? "" : "none";
    }
}
# Informe de Auditoría y Diagnóstico de Seguridad: Credenciales OAuth 2.0 (Google Drive)

**Preparado por:** Auditor de Seguridad & Experto en Google Cloud  
**Proyecto:** Legion3DTracker  
**Estado del Sistema:** Analizado y Diagnosticado (Análisis Estático de Solo Lectura)  
**Archivo de Salida:** `diagnostico_credenciales_oauth.md`

---

## 1. Explicación del Flujo de Autorización Actual

Al revisar la clase `GoogleDriveService.java`, se identifica que la aplicación utiliza el flujo **OAuth 2.0 para aplicaciones instaladas/escritorio** (`OAuth 2.0 Desktop Flow`), específicamente implementado a través de las siguientes clases de la biblioteca de Google APIs para Java:

*   **`GoogleAuthorizationCodeFlow`**: Administra el flujo del código de autorización, incluyendo la configuración de ámbitos (`scopes`), el almacenamiento local de tokens y el intercambio de códigos de autorización por tokens de acceso/actualización.
*   **`AuthorizationCodeInstalledApp`**: Es el componente encargado de coordinar la apertura interactiva del navegador predeterminado del sistema operativo del usuario para solicitar el consentimiento explícito.
*   **`LocalServerReceiver`**: Levanta un servidor HTTP local embebido (Jetty) de manera temporal en el puerto configurado (específicamente `8888` en la línea 91) para escuchar el código de retorno redirigido (`localhost`) una vez el usuario finaliza la autenticación en el navegador.

### Relación y Diferencia entre Archivos de Credenciales

Para el correcto funcionamiento de este flujo, se requiere de dos archivos distintos con propósitos y ciclos de vida completamente diferentes:

| Característica | `client_secret.json` (Las Placas de la Aplicación) | `StoredCredential` (El Pase de Acceso Temporal) |
| :--- | :--- | :--- |
| **Ruta del Archivo** | `tracker/src/main/resources/credentials/client_secret.json` | `tracker/src/main/resources/credentials/tokens/StoredCredential` |
| **Definición** | Credenciales de identidad de la aplicación en la Google Cloud Console. Contiene el *Client ID* y el *Client Secret*. | Almacenamiento local del token de acceso (Access Token) y del token de actualización (Refresh Token) otorgados por un usuario específico. |
| **Función** | Funciona como la "placa de identificación" o firma del software frente a los servidores de Google. Le dice a Google *qué* aplicación está intentando conectarse. | Funciona como el "pase de acceso temporal" para interactuar con la cuenta de Google Drive del usuario sin volver a solicitar su contraseña. |
| **Sensibilidad** | **Alta**: Si es expuesto, un tercero podría suplantar la aplicación. No obstante, no da acceso directo a los datos del usuario por sí solo sin la debida aprobación interactiva. | **Crítica**: Contiene los privilegios activos para manipular directamente los archivos de la cuenta de Drive asociada. |
| **Ciclo de Vida** | Permanente (hasta que se revoque o cambie manualmente en la consola de Google Cloud). | Temporal. Por diseño de la biblioteca de Google, el Access Token vence cada 60 minutos, y la biblioteca intenta autorenovarlo usando el Refresh Token contenido en este mismo archivo. |

---

## 2. Diagnóstico del Vencimiento (El Límite de 7 Días)

El fallo repentino reportado por el cliente tras una semana (7 días calendario) de uso normal no se debe a un error de lógica del código ni a una falla de red, sino al cumplimiento de una política de seguridad estricta y predeterminada de la plataforma Google Cloud.

### El Motivo Exacto de la Inutilidad de `StoredCredential`

El archivo `StoredCredential` almacena tanto el **Access Token** como el **Refresh Token**:
1. El **Access Token** expira cada 1 hora.
2. Cada vez que la aplicación requiere interactuar con Drive, detecta que el Access Token expiró y utiliza silenciosamente el **Refresh Token** para solicitar uno nuevo a Google APIs.
3. Tras exactamente **7 días calendario**, la solicitud de renovación falló porque el **Refresh Token mismo fue revocado por Google**, dejando el archivo `StoredCredential` inválido. Cualquier intento de subida posterior desencadena una excepción de autorización (`401 Unauthorized` o `invalid_grant`).

### La Política de Google Cloud para Proyectos en Estado "Testing"

En la **Google Cloud Console**, bajo la sección del **Pantalla de Consentimiento OAuth (OAuth Consent Screen)** de este proyecto (`legion3d-tracker-drive-oauth`), el estado de publicación se encuentra configurado en **"Testing"** (Prueba).

De acuerdo con la documentación oficial de Google OAuth 2.0:
> "Un token de actualización (Refresh Token) emitido para una aplicación con un estado de publicación de 'Testing' expira automáticamente a los 7 días (168 horas) desde su creación."

Esta limitación existe para asegurar que los desarrolladores no utilicen proyectos sin verificar (pantallas de consentimiento no aprobadas formalmente por Google) para dar soporte a integraciones productivas de largo plazo. Google exige que la aplicación pase por un proceso de verificación si desea tokens persistentes (los cuales no expiran a los 7 días), o bien que el estado de publicación pase a **"In Production"** (en cuyo caso, si la aplicación no se verifica pero permanece en producción, se presentará una advertencia de seguridad "Aplicación no verificada" al loguearse, pero los tokens de actualización ya no expirarán automáticamente a los 7 días).

---

## 3. Pasos Manuales de Recuperación (Cero Código)

Dado que se ha establecido una restricción absoluta de modificar código o alterar la arquitectura del proyecto actual, se detallan a continuación los dos pasos físicos que el cliente debe ejecutar en su máquina local para restablecer el servicio de subida a Google Drive.

### Paso 1: Eliminar el Token Expirado del Sistema de Archivos
Dado que la aplicación lee el token existente en caliente desde su almacenamiento en disco, primero se debe forzar a la aplicación a limpiar la sesión inválida.
1. Localice la ruta física donde se encuentra el token almacenado:
   * **Ruta:** `tracker/src/main/resources/credentials/tokens/StoredCredential`
2. **Elimine** o **borre** el archivo `StoredCredential` por completo. *(Si prefiere no perderlo definitivamente por precaución, puede simplemente moverlo a otra ubicación temporal fuera del proyecto o renombrarlo a `StoredCredential.old`)*.

### Paso 2: Volver a Iniciar/Compilar la Aplicación y Autorizar
Una vez que el archivo del token obsoleto ha sido removido, la biblioteca de Google forzará un nuevo consentimiento interactivo en el navegador al no detectar credenciales válidas guardadas.
1. Detenga la aplicación Spring Boot en caso de que se encuentre en ejecución.
2. Vuelva a compilar e iniciar el proyecto (por ejemplo, ejecutando `./mvnw spring-boot:run` o iniciando la aplicación desde su IDE).
3. En la consola del sistema o IDE, observará que el servidor local de autenticación se levanta en el puerto `8888` y se abrirá automáticamente el navegador web predeterminado (o se le proveerá una URL de autorización en los logs del sistema).
4. Seleccione la cuenta de Google correspondiente, ignore la advertencia de "Google no ha verificado esta aplicación" (haciendo clic en *Configuración Avanzada* -> *Ir a Legion3DTracker (no seguro)*) y conceda los permisos de lectura/escritura requeridos para Google Drive.
5. El navegador mostrará un mensaje de confirmación exitosa y se creará automáticamente un nuevo archivo `StoredCredential` bajo la ruta original, restaurando el flujo de subida de archivos durante otros 7 días.

---

> [!NOTE]
> **Recomendación de Seguridad a Largo Plazo:** Para evitar tener que realizar este procedimiento de forma recurrente cada 7 días, se aconseja al cliente ingresar a la consola de Google Cloud del proyecto, dirigirse a la sección *Pantalla de Consentimiento OAuth* y cambiar el estado de publicación de la aplicación de **"Testing"** a **"In Production"**. Esto eliminará la regla de expiración de 7 días sobre los Refresh Tokens.

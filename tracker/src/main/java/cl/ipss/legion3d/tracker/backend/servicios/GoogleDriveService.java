package cl.ipss.legion3d.tracker.backend.servicios;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import cl.ipss.legion3d.tracker.backend.excepciones.MissingFolderConfigurationException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

/**
 * SERVICIO DE INTEGRACIÓN CLOUD - GOOGLE DRIVE API (OAuth 2.0 Desktop Flow)
 *
 * Este servicio centraliza toda la comunicación con Google Drive utilizando
 * flujo de autorización OAuth 2.0 para aplicaciones de escritorio.
 */
@Service
public class GoogleDriveService {

    private Drive driveService;

    // IDs de carpetas maestras en Google Drive.
    public static final String CARPETA_PAGOS_NV = "1IjCFceCS0m37PzvgN0EKpJGTXkxXc_pO";
    public static final String CARPETA_TECNICOS_NV = "166Vs5ZQq-QUmQ6PQ7ze38A6WZUsa1C3I";
    public static final String CARPETA_WPS_NV = "1DPTS0dskr6p3Y57ctDq6Ggl_cTR182Bc";
    public static final String CARPETA_STARKEN_NV = "1WTuwjfjGtxi9kyp9BMp1KWpDvShgvLIn";

    @Value("${temporales.folder.id}")
    private String folderId;

    @PostConstruct
    public void init() {
        try {
            com.google.api.client.http.HttpTransport httpTransport =
                    GoogleNetHttpTransport.newTrustedTransport();
            com.google.api.client.json.JsonFactory jsonFactory =
                    GsonFactory.getDefaultInstance();

            // Carga del archivo de credenciales de Google Drive (híbrido: variable de entorno o classpath)
            InputStream in;
            String clientSecretEnv = System.getenv("GOOGLE_CLIENT_SECRET_JSON");
            if (clientSecretEnv != null && !clientSecretEnv.isBlank()) {
                in = new java.io.ByteArrayInputStream(clientSecretEnv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                System.out.println("ℹ️ Cargando credenciales de Google Drive desde la variable de entorno GOOGLE_CLIENT_SECRET_JSON");
            } else {
                in = GoogleDriveService.class.getResourceAsStream("/credentials/client_secret.json");
                if (in == null) {
                    throw new java.io.FileNotFoundException("No se encontró el archivo client_secret.json en el classpath ni la variable de entorno GOOGLE_CLIENT_SECRET_JSON");
                }
                System.out.println("ℹ️ Cargando credenciales de Google Drive desde el archivo local en classpath");
            }

            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                    jsonFactory, new InputStreamReader(in));

            // Configurar el flujo de autorización con permisos de lectura y escritura totales (DRIVE)
            List<String> scopes = Collections.singletonList(DriveScopes.DRIVE);

            // Directorio para almacenar localmente el token de acceso
            java.io.File tokensFolder = new java.io.File("src/main/resources/credentials/tokens");
            if (!tokensFolder.exists()) {
                tokensFolder.mkdirs();
            }
            FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(tokensFolder);

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, jsonFactory, clientSecrets, scopes)
                    .setDataStoreFactory(dataStoreFactory)
                    .setAccessType("offline")
                    .build();

            // LocalServerReceiver levantará un receptor en un puerto local libre o configurado (ej: 8888)
            // Esto abrirá automáticamente el navegador web para conceder los permisos
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            // Construir el cliente oficial de Drive
            driveService = new Drive.Builder(
                    httpTransport, 
                    jsonFactory, 
                    credential)
                    .setApplicationName("Legion3DTracker")
                    .build();

            System.out.println("✅ Google Drive Service inicializado correctamente con OAuth 2.0 Desktop Flow. Hash: " + System.identityHashCode(driveService));

        } catch (Exception e) {
            System.err.println("❌ Error al inicializar Google Drive con OAuth 2.0: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Verifica que el servicio de Google Drive esté inicializado.
     *
     * Si no lo está, intenta inicializarlo nuevamente.
     */
    private void asegurarDriveInicializado() {
        if (driveService == null) {
            init();

            if (driveService == null) {
                throw new IllegalStateException(
                        "Google Drive Service no está inicializado. Revisa las credenciales de client_secret.json."
                );
            }
        }
    }

    /**
     * SUBIDA DE ARCHIVO TÉCNICO A GOOGLE DRIVE
     *
     * Este método se usa cuando el cliente sube un archivo desde el Formulario Técnico.
     */
    public DriveUploadResult subirArchivo(MultipartFile file, String codigoSeguimiento) throws IOException {
        asegurarDriveInicializado();
        System.out.println("DEBUG: En subirArchivo, hash de driveService: " + System.identityHashCode(driveService));

        if (file == null || file.isEmpty()) {
            throw new IOException("Archivo vacío o nulo.");
        }

        if (CARPETA_TECNICOS_NV == null || CARPETA_TECNICOS_NV.isBlank()) {
            throw new MissingFolderConfigurationException("No está configurada la carpeta de técnicos en Drive.");
        }

        String originalName = file.getOriginalFilename();
        String fileName = construirNombreDrive(codigoSeguimiento, "tec", originalName, null);

        String carpetaDestinoId = obtenerRutaFecha(CARPETA_TECNICOS_NV);

        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(carpetaDestinoId));

        // STREAMING DIRECTO: Sin archivos temporales en disco
        InputStreamContent mediaContent = new InputStreamContent(
                file.getContentType(), 
                file.getInputStream()
        );
        mediaContent.setLength(file.getSize());

        System.out.println("DEBUG: ID carpeta usada en subida (subirArchivo): " + carpetaDestinoId);

        File uploadedFile = driveService.files()
                .create(fileMetadata, mediaContent)
                .setFields("id, webViewLink")
                .execute();

        return new DriveUploadResult(uploadedFile.getWebViewLink(), uploadedFile.getId());
    }

    /**
     * SUBIDA DE COMPROBANTE DE PAGO A GOOGLE DRIVE
     *
     * Este método se usa cuando el cliente o Luis sube un comprobante de pago.
     */
    public DriveUploadResult subirComprobantePago(MultipartFile archivo, String tracking, int correlativo) throws IOException {
        asegurarDriveInicializado();
        System.out.println("DEBUG: En subirComprobantePago, hash de driveService: " + System.identityHashCode(driveService));

        if (archivo == null || archivo.isEmpty()) {
            throw new IOException("Comprobante vacío o nulo.");
        }

        if (CARPETA_PAGOS_NV == null || CARPETA_PAGOS_NV.isBlank()) {
            throw new MissingFolderConfigurationException("No está configurada la carpeta de pagos en Drive.");
        }

        String originalName = archivo.getOriginalFilename();
        String fileName = construirNombreDrive(tracking, "pago", originalName, correlativo);

        System.out.println("DEBUG: Intentando subir comprobante a carpeta ID: " + CARPETA_PAGOS_NV);

        String carpetaDestinoId = obtenerRutaFecha(CARPETA_PAGOS_NV);

        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(carpetaDestinoId));

        // STREAMING DIRECTO: Sin archivos temporales en disco
        InputStreamContent mediaContent = new InputStreamContent(
                archivo.getContentType(), 
                archivo.getInputStream()
        );
        mediaContent.setLength(archivo.getSize());

        System.out.println("DEBUG: ID carpeta usada en subida (subirComprobantePago): " + carpetaDestinoId);

        File uploadedFile = driveService.files()
                .create(fileMetadata, mediaContent)
                .setFields("id, webViewLink")
                .execute();

        return new DriveUploadResult(uploadedFile.getWebViewLink(), uploadedFile.getId());
    }

    /**
     * SUBIDA DE COMPROBANTE DE PAGO EN FLUJO (GENERACIÓN AUTOMÁTICA EN MEMORIA)
     *
     * Este método se usa cuando se autogenera un voucher txt en memoria.
     */
    public DriveUploadResult subirComprobantePago(java.io.InputStream stream, String fileName, String contentType, long size) throws IOException {
        return subirComprobantePago(stream, fileName, contentType, size, CARPETA_PAGOS_NV);
    }

    /**
     * SUBIDA DE COMPROBANTE DE PAGO EN FLUJO PARAMETRIZANDO LA CARPETA RAÍZ
     */
    public DriveUploadResult subirComprobantePago(java.io.InputStream stream, String fileName, String contentType, long size, String folderId) throws IOException {
        asegurarDriveInicializado();
        if (stream == null) {
            throw new IOException("Stream vacío o nulo.");
        }
        String carpetaRaiz = (folderId != null && !folderId.isBlank()) ? folderId : CARPETA_PAGOS_NV;
        String carpetaDestinoId = obtenerRutaFecha(carpetaRaiz);

        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(carpetaDestinoId));

        InputStreamContent mediaContent = new InputStreamContent(contentType, stream);
        mediaContent.setLength(size);

        File uploadedFile = driveService.files()
                .create(fileMetadata, mediaContent)
                .setFields("id, webViewLink")
                .execute();

        return new DriveUploadResult(uploadedFile.getWebViewLink(), uploadedFile.getId());
    }

    /**
     * BUSCAR ARCHIVO POR NOMBRE Y ID DE CARPETA PADRE
     */
    public String buscarArchivoPorNombreYPadre(String nombre, String parentId) throws IOException {
        asegurarDriveInicializado();
        String query = "name = '" + nombre.replace("'", "\\'") + "' and '" + parentId + "' in parents and trashed = false";
        com.google.api.services.drive.model.FileList result = driveService.files().list()
                .setQ(query)
                .setFields("files(id)")
                .execute();
        if (result.getFiles() != null && !result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }
        return null;
    }

    /**
     * BUSCAR ARCHIVO POR NOMBRE DE MANERA GLOBAL
     */
    public String buscarArchivoPorNombre(String nombre) throws IOException {
        asegurarDriveInicializado();
        String query = "name = '" + nombre.replace("'", "\\'") + "' and trashed = false";
        com.google.api.services.drive.model.FileList result = driveService.files().list()
                .setQ(query)
                .setFields("files(id)")
                .execute();
        if (result.getFiles() != null && !result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }
        return null;
    }

    /**
     * Aplica permiso de lectura pública con enlace.
     *
     * Desactivado por razones de seguridad (los archivos deben permanecer privados).
     */
    private void aplicarPermisoLectura(String fileId) throws IOException {
        // Método desactivado por seguridad.
    }

    /**
     * Obtiene los metadatos de un archivo almacenado en Google Drive.
     */
    public File obtenerMetadata(String fileId) throws IOException {
        asegurarDriveInicializado();

        if (fileId == null || fileId.isBlank()) {
            throw new IOException("El ID del archivo de Drive está vacío.");
        }

        return driveService.files()
                .get(fileId)
                .setFields("name, mimeType, size")
                .execute();
    }

    /**
     * Descarga un archivo desde Google Drive hacia un OutputStream.
     */
    public void descargarArchivo(String fileId, java.io.OutputStream outputStream) throws IOException {
        asegurarDriveInicializado();

        if (fileId == null || fileId.isBlank()) {
            throw new IOException("El ID del archivo de Drive está vacío.");
        }

        if (outputStream == null) {
            throw new IOException("El OutputStream de descarga está vacío.");
        }

        driveService.files()
                .get(fileId)
                .executeMediaAndDownloadTo(outputStream);
    }

    /**
     * Elimina un archivo de Google Drive por su ID.
     */
    public void eliminarArchivo(String fileId) throws IOException {
        asegurarDriveInicializado();

        if (fileId != null && !fileId.isBlank()) {
            driveService.files().delete(fileId).execute();
            System.out.println("🗑️ Archivo de Drive eliminado: " + fileId);
        }
    }

    /**
     * Copia un archivo hacia una carpeta de respaldo específica.
     */
    public void copiarArchivoARespaldo(String fileId, String originalName, String backupFolderId) {
        try {
            asegurarDriveInicializado();

            if (fileId == null || fileId.isBlank()) {
                return;
            }

            if (backupFolderId == null || backupFolderId.isBlank()) {
                return;
            }

            String nombreSeguro = sanitizarNombreArchivo(originalName);

            String query = "name = '" + nombreSeguro.replace("'", "\\'") + "' and '"
                    + backupFolderId + "' in parents and trashed = false";

            com.google.api.services.drive.model.FileList result = driveService.files()
                    .list()
                    .setQ(query)
                    .setFields("files(id)")
                    .execute();

            if (result.getFiles() != null && !result.getFiles().isEmpty()) {
                System.out.println("⚠️ El archivo '" + nombreSeguro + "' ya existe en respaldo. No se duplicará.");
                return;
            }

            File fileMetadata = new File();
            fileMetadata.setName(nombreSeguro);
            fileMetadata.setParents(Collections.singletonList(backupFolderId));

            driveService.files()
                    .copy(fileId, fileMetadata)
                    .execute();

            System.out.println("📦 Respaldo automático exitoso: " + nombreSeguro);

        } catch (Exception e) {
            System.err.println("❌ Fallo en copia de respaldo automática: " + e.getMessage());
        }
    }

    /**
     * COPIA DE ARCHIVO NATIVA DE GOOGLE DRIVE (Retorna resultado de subida)
     */
    public DriveUploadResult copiarArchivo(String fileId, String nuevoNombre, String destinoFolderId) throws IOException {
        asegurarDriveInicializado();

        if (fileId == null || fileId.isBlank()) {
            throw new IOException("El ID del archivo origen está vacío.");
        }
        if (destinoFolderId == null || destinoFolderId.isBlank()) {
            throw new IOException("El ID de la carpeta destino está vacío.");
        }

        String carpetaDestinoId = obtenerRutaFecha(destinoFolderId);

        File fileMetadata = new File();
        fileMetadata.setName(nuevoNombre);
        fileMetadata.setParents(Collections.singletonList(carpetaDestinoId));

        File clonedFile = driveService.files()
                .copy(fileId, fileMetadata)
                .setFields("id, webViewLink")
                .execute();

        return new DriveUploadResult(clonedFile.getWebViewLink(), clonedFile.getId());
    }

    /**
     * HELPER CENTRAL DE NOMENCLATURA DRIVE
     */
    private String construirNombreDrive(String trackingCode, String tipo, String originalName, Integer correlativo) {
        String tracking = (trackingCode != null && !trackingCode.isBlank())
                ? trackingCode.trim().toUpperCase()
                : "LEG-SIN-CODIGO";

        String tipoSeguro = (tipo != null && !tipo.isBlank())
                ? tipo.trim().toLowerCase()
                : "archivo";

        if (!tipoSeguro.equals("tec") && !tipoSeguro.equals("pago")) {
            tipoSeguro = "archivo";
        }

        String nombreSeguro = sanitizarNombreArchivo(originalName);

        nombreSeguro = nombreSeguro.replaceFirst("^" + java.util.regex.Pattern.quote(tracking) + "[-_]+", "");
        nombreSeguro = nombreSeguro.replaceFirst("^(pago-\\d+|tec|pago|wps|links|archivo)[-_]+", "");

        if (nombreSeguro.isBlank()) {
            nombreSeguro = "archivo";
        }

        String mid = tipoSeguro;
        if (correlativo != null && "pago".equals(tipoSeguro)) {
            mid = "pago-" + correlativo;
        }

        return tracking + "-" + mid + "-" + nombreSeguro;
    }

    /**
     * HELPER DE LIMPIEZA DE NOMBRE DE ARCHIVO
     */
    private String sanitizarNombreArchivo(String originalName) {
        String nombre = (originalName != null && !originalName.isBlank())
                ? originalName.trim()
                : "archivo_sin_nombre";

        return nombre
                .replaceAll("[áàäâÁÀÄÂ]", "a")
                .replaceAll("[éèëêÉÈËÊ]", "e")
                .replaceAll("[íìïîÍÌÏÎ]", "i")
                .replaceAll("[óòöôÓÒÖÔ]", "o")
                .replaceAll("[úùüûÚÙÛÜ]", "u")
                .replaceAll("[ñÑ]", "n")
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9._-]", "");
    }

    /**
     * Busca o crea una carpeta hija dentro de un parentId.
     */
    private String obtenerOCrearSubcarpeta(String nombre, String parentId) throws IOException {
        asegurarDriveInicializado();

        String nombreSeguro = nombre.replace("'", "\\'");

        String query = String.format(
                "mimeType='application/vnd.google-apps.folder' and '%s' in parents and name='%s' and trashed=false",
                parentId,
                nombreSeguro
        );

        com.google.api.services.drive.model.FileList result = driveService.files()
                .list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        if (result.getFiles() != null && !result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }

        File folderMetadata = new File();
        folderMetadata.setName(nombre);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        folderMetadata.setParents(Collections.singletonList(parentId));

        File folder = driveService.files()
                .create(folderMetadata)
                .setFields("id")
                .execute();

        return folder.getId();
    }

    /**
     * Construye la ruta por fecha dentro de una carpeta raíz.
     */
    private String obtenerRutaFecha(String carpetaRaizId) throws IOException {
        asegurarDriveInicializado();

        java.time.LocalDate hoy = java.time.LocalDate.now();

        String anio = String.valueOf(hoy.getYear());
        String mes = String.format("%02d", hoy.getMonthValue());
        String dia = String.format("%02d", hoy.getDayOfMonth());

        String idAnio = obtenerOCrearSubcarpeta(anio, carpetaRaizId);
        String idMes = obtenerOCrearSubcarpeta(mes, idAnio);
        String idDia = obtenerOCrearSubcarpeta(dia, idMes);

        return idDia;
    }

    /**
     * Clase auxiliar para capturar los datos de una subida a Drive.
     */
    public static class DriveUploadResult {
        private final String webViewLink;
        private final String fileId;

        public DriveUploadResult(String webViewLink, String fileId) {
            this.webViewLink = webViewLink;
            this.fileId = fileId;
        }

        public String getWebViewLink() {
            return webViewLink;
        }

        public String getFileId() {
            return fileId;
        }
    }

    /**
     * Limpia los archivos de una carpeta específica que superen los días de antigüedad.
     */
    public void limpiarCarpetaPorAntiguedad(String folderId, int diasAntiguedad) {
        try {
            java.time.ZonedDateTime fechaLimite = java.time.ZonedDateTime.now(java.time.ZoneId.of("UTC")).minusDays(diasAntiguedad);
            String fechaFormat = java.time.format.DateTimeFormatter.ISO_INSTANT.format(fechaLimite);

            String query = "'" + folderId + "' in parents and modifiedTime < '" + fechaFormat + "' and trashed = false";
            com.google.api.services.drive.model.FileList result = driveService.files().list()
                    .setQ(query)
                    .setFields("files(id, name)")
                    .execute();

            for (com.google.api.services.drive.model.File archivo : result.getFiles()) {
                com.google.api.services.drive.model.File contenidoUpdate = new com.google.api.services.drive.model.File();
                contenidoUpdate.setTrashed(true);
                driveService.files().update(archivo.getId(), contenidoUpdate).execute();
                System.out.println(" Movido a papelera (Transito Obsoleto): " + archivo.getName());
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error limpiando la carpeta en Drive (ID: " + folderId + "): " + e.getMessage());
        }
    }
}
package cl.ipss.legion3d.tracker;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DriveFireTest {
    public static void main(String[] args) {
        String credentialsPath = "src/main/resources/credentials/legion3d-tracker-drive-api.json";
        
        System.out.println("🔥 INICIANDO DIAGNÓSTICO PROFUNDO DE CARPETAS GOOGLE DRIVE 🔥");
        System.out.println("Ruta credenciales: " + credentialsPath);
        
        Map<String, String> carpetas = new LinkedHashMap<>();
        carpetas.put("CARPETA_PAGOS_NV", "1IjCFceCS0m37PzvgN0EKpJGTXkxXc_pO");
        carpetas.put("CARPETA_TECNICOS_NV", "166Vs5ZQq-QUmQ6PQ7ze38A6WZUsa1C3I");
        carpetas.put("CARPETA_WPS_NV", "1DPTS0dskr6p3Y57ctDq6Ggl_cTR182Bc");
        carpetas.put("CARPETA_STARKEN_NV", "1WTuwjfjGtxi9kyp9BMp1KWpDvShgvLIn");
        
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            
            InputStream in = new FileInputStream(credentialsPath);
            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singletonList(DriveScopes.DRIVE));
            
            Drive driveService = new Drive.Builder(
                    httpTransport,
                    jsonFactory,
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Legion3DTracker-Test")
                    .build();
            
            for (Map.Entry<String, String> entry : carpetas.entrySet()) {
                String nombreVariable = entry.getKey();
                String folderId = entry.getValue();
                
                System.out.println("\n------------------------------------------------");
                System.out.println("📁 Evaluando: " + nombreVariable + " (ID: " + folderId + ")");
                
                // 1. Diagnóstico de Lectura (VER)
                boolean tieneAccesoLectura = false;
                try {
                    File folder = driveService.files().get(folderId).setFields("id, name, mimeType").execute();
                    System.out.println("   [LECTURA] ✅ EXITO: Se puede LEER/VER la carpeta.");
                    System.out.println("             Nombre real en Drive: " + folder.getName());
                    tieneAccesoLectura = true;
                } catch (Exception e) {
                    System.err.println("   [LECTURA] ❌ FALLO: No se puede ver la carpeta.");
                    System.err.println("             Detalle: " + e.getMessage());
                }
                
                // 2. Diagnóstico de Creación de Subcarpeta de fecha (obtenerRutaFecha)
                if (tieneAccesoLectura) {
                    try {
                        System.out.println("   [FECHAS]    Intentando resolver/crear estructura de fecha (Año/Mes/Día)...");
                        java.time.LocalDate hoy = java.time.LocalDate.now();
                        String anio = String.valueOf(hoy.getYear());
                        String mes = String.format("%02d", hoy.getMonthValue());
                        String dia = String.format("%02d", hoy.getDayOfMonth());
                        
                        String idAnio = obtenerOCrearSubcarpeta(driveService, anio, folderId);
                        String idMes = obtenerOCrearSubcarpeta(driveService, mes, idAnio);
                        String idDia = obtenerOCrearSubcarpeta(driveService, dia, idMes);
                        System.out.println("   [FECHAS]    ✅ EXITO: Estructura de fechas resuelta. ID de hoy: " + idDia);
                    } catch (Exception e) {
                        System.err.println("   [FECHAS]    ❌ FALLO: No se pudo resolver/crear la estructura de fechas.");
                        System.err.println("               Detalle: " + e.getMessage());
                    }
                } else {
                    System.out.println("   [FECHAS]    ⚠️ Omitido porque falló el acceso de lectura.");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERROR AL INICIALIZAR EL DIAGNÓSTICO:");
            e.printStackTrace();
        }
    }

    private static String obtenerOCrearSubcarpeta(Drive driveService, String nombre, String parentId) throws Exception {
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
}

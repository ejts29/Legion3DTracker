package cl.ipss.legion3d.tracker.backend.excepciones;

/**
 * EXCEPCIÓN: CONFIGURACIÓN DE CARPETA DRIVE AUSENTE
 * Se lanza cuando el sistema no logra localizar el ID de la carpeta
 * necesaria para el almacenamiento en la nube.
 */
public class MissingFolderConfigurationException extends RuntimeException {
    public MissingFolderConfigurationException(String message) {
        super(message);
    }
}

package cl.ipss.legion3d.tracker.backend.servicios;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;

@Service
public class GmailApiService {

    private Gmail gmailService;

    @Value("${app.admin.email}")
    private String adminEmail;

    @PostConstruct
    public void init() {
        try {
            com.google.api.client.http.HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            com.google.api.client.json.JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            File secretFile = new File("/etc/secrets/client_secret.json");
            GoogleCredentials credentials;

            if (secretFile.exists()) {
                try (FileInputStream fis = new FileInputStream(secretFile)) {
                    GoogleCredentials baseCreds = GoogleCredentials.fromStream(fis);
                    if (baseCreds instanceof com.google.auth.oauth2.ServiceAccountCredentials) {
                        credentials = ((com.google.auth.oauth2.ServiceAccountCredentials) baseCreds)
                                .toBuilder()
                                .setServiceAccountUser(adminEmail)
                                .build()
                                .createScoped(Collections.singletonList(GmailScopes.GMAIL_SEND));
                    } else {
                        credentials = baseCreds.createScoped(Collections.singletonList(GmailScopes.GMAIL_SEND));
                    }
                }
                System.out.println("ℹ️ Cargando credenciales de Gmail desde archivo en disco: /etc/secrets/client_secret.json para impersonar a " + adminEmail);
            } else {
                throw new java.io.FileNotFoundException(
                        "No se encontró el archivo de credenciales en /etc/secrets/client_secret.json para Gmail");
            }

            gmailService = new Gmail.Builder(
                    httpTransport,
                    jsonFactory,
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Legion3DTracker")
                    .build();

            System.out.println("✅ Gmail API Service inicializado correctamente para " + adminEmail);

        } catch (Exception e) {
            System.err.println("❌ Error al inicializar Gmail API: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void enviarCorreoMime(MimeMessage mimeMessage) throws Exception {
        if (gmailService == null) {
            init();
            if (gmailService == null) {
                throw new IllegalStateException("Gmail API Service no está inicializado.");
            }
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        mimeMessage.writeTo(buffer);
        byte[] rawMessageBytes = buffer.toByteArray();
        String encodedEmail = com.google.api.client.util.Base64.encodeBase64URLSafeString(rawMessageBytes);

        Message message = new Message();
        message.setRaw(encodedEmail);

        gmailService.users().messages().send("me", message).execute();
    }
}

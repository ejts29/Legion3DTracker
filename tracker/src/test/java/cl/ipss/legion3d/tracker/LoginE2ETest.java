package cl.ipss.legion3d.tracker;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static com.codeborne.selenide.Selenide.*;

/**
 * CLASE DE PRUEBA E2E: LoginE2ETest
 * Este robot automatiza el flujo de ingreso al sistema Legión 3D Tracker.
 */
public class LoginE2ETest {

    @BeforeAll
    public static void setup() {
        // Configuración para correr las pruebas en modo "Headless" (sin abrir ventana visible) 
        // o normal. Aquí configuramos el timeout de espera a 5 segundos.
        Configuration.timeout = 5000;
        Configuration.browser = "chrome";
    }

    @Test
    public void testLoginExitoso() {
        // 1. El robot abre la URL de inicio de sesión del servidor local
        open("http://localhost:8080/login");

        // 2. El robot busca el campo de contraseña por su ID e ingresa el valor de prueba
        // Se asume que la contraseña de admin es '123' según application.properties
        // 2. El robot busca el campo de contraseña e ingresa el valor '123'
       $("input[name='password']").setValue("123");

        // 3. El robot localiza el botón de envío y hace clic para procesar el formulario
        // Usamos un selector de tipo submit para mayor precisión
        $("button[type='submit']").click();

        // 4. VERIFICACIÓN: El robot espera a que la página cambie y busca el título del Dashboard
        // Validamos que el texto "DASHBOARD" sea visible para confirmar el ingreso exitoso
        $("h1").shouldHave(Condition.text("DASHBOARD"));

        // 5. El robot confirma que el elemento de bienvenida está presente
        $(".welcome-section").shouldBe(Condition.visible);
        
        System.out.println(">>> [E2E SUCCESS] Robot completó el flujo de Login correctamente.");
    }
}

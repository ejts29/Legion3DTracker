package cl.ipss.legion3d.tracker;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.Selectors.*;

public class CicloCompletoE2ETest {

    @BeforeAll
    public static void setup() {
        Configuration.browser = "chrome";
        Configuration.timeout = 10000;
        Configuration.headless = false; 
        Configuration.holdBrowserOpen = true; 
    }

    @Test
    public void testFlujoMaestroLegion3D() {
        // --- 1. LOGIN Y APROBACIÓN INICIAL ---
        open("http://localhost:8080/login");
        $("input[name='password']").setValue("123");
        $("button[type='submit']").click();
        Selenide.sleep(2000); // Pausa para procesar el ingreso

        open("http://localhost:8080/solicitudes/triaje");
        $("table tbody tr:first-child").$(byText("Revisar")).click();
        Selenide.sleep(2000);
        
        $("textarea").setValue("Aprobación automatizada para pasar al siguiente nivel.");
        $(byText("APROBAR")).click();
        Selenide.sleep(2000);

        // --- 2. ACCESO RÁPIDO F2 ---
        // El robot busca el botón o enlace que dice exactamente ACCESO RÁPIDO F2
        $(byText("ACCESO RÁPIDO F2")).shouldBe(Condition.visible).click();
        Selenide.sleep(2000);

        // --- 3. BÚSQUEDA Y PANEL IZQUIERDO ---
        // Simulamos que el robot ingresa un ID de Cliente (ajusta el selector si es necesario)
        // Buscamos un input que mencione ID o usamos el primero del buscador
        $("input[placeholder*='ID'], input[name*='id']").setValue("1"); 
        Selenide.sleep(1000);

        // Colocamos el código en el panel izquierdo para la redirección
        // (Asumimos que hay un input en el sidebar o panel lateral para esto)
        $(".sidebar input, .panel-izquierdo input").setValue("TRACK-E2E-99").pressEnter();
        Selenide.sleep(2000);

        // --- 4. LLENADO DEL FORMULARIO COMPLETO ---
        // El robot ahora rellena los datos técnicos del pedido
        // Estamos usando los nombres de campos que definimos en los DTOs de seguridad
        
        $("input[name='precioFinal']").setValue("45000");
        $("input[name='medidaAncho']").setValue("150.5");
        $("input[name='medidaAlto']").setValue("200.0");
        $("input[name='medidaProfundidad']").setValue("10.5");
        $("input[name='cantidadUnidades']").setValue("5");
        
        // Seleccionamos opciones en los dropdowns
        if ($("select[name='materialSolicitado']").exists()) {
            $("select[name='materialSolicitado']").selectOption(1);
        }
        
        $("textarea[name='informacionAdicional']").setValue("Llenado automático: Material PLA Premium, acabado liso.");
        
        Selenide.sleep(2000); // Pausa final para que veas todo lleno

        // Enviar el formulario final
        $("button[type='submit']").click();

        // --- 5. CIERRE ---
        Selenide.sleep(3000);
        System.out.println("¡Misión Cumplida! El robot completó el login, aprobó, buscó y llenó el formulario.");
    }
}
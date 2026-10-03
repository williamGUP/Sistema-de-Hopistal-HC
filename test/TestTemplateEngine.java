import com.hospital.template.TemplateEngine;
import java.nio.file.Path;
import java.util.*;

public class TestTemplateEngine {
    public static void main(String[] args) {
        TemplateEngine engine = new TemplateEngine(Path.of("test/templates"));
        Map<String, Object> data = new HashMap<>();
        data.put("nombre", "Ana <3");
        data.put("total", 2);
        List<Map<String,Object>> items = new ArrayList<>();
        Map<String,Object> i1 = new HashMap<>(); i1.put("nombre","Gripe"); i1.put("estado","ACTIVA");
        Map<String,Object> i2 = new HashMap<>(); i2.put("nombre","Alergia"); i2.put("estado","CONTROLADA");
        items.add(i1); items.add(i2);
        data.put("items", items);
        data.put("activo", true);
        data.put("crudo", "<b>negrita</b>");

        String result = engine.render("prueba", data);
        System.out.println("=== RESULTADO CON ITEMS ===");
        System.out.println(result);

        boolean ok = true;
        ok &= check(result.contains("Hola Ana &lt;3, tienes 2 mensajes."), "escapa nombre con <3");
        ok &= check(result.contains("- Gripe (ACTIVA)"), "itera item 1");
        ok &= check(result.contains("- Alergia (CONTROLADA)"), "itera item 2");
        ok &= check(!result.contains("No hay items."), "no muestra 'no hay items' cuando sí hay");
        ok &= check(result.contains("Está activo"), "muestra bloque #activo");
        ok &= check(!result.contains("Está inactivo"), "no muestra bloque ^activo");
        ok &= check(result.contains("Raw: <b>negrita</b>"), "triple llave sin escapar");
        ok &= check(result.contains("Escapado: &lt;b&gt;negrita&lt;/b&gt;"), "doble llave escapada");
        ok &= check(result.contains("Partial dice hola a Ana &lt;3."), "partial ve el contexto exterior");

        // Ahora probar caso vacío (lista vacía -> debe activar el bloque invertido)
        Map<String, Object> data2 = new HashMap<>();
        data2.put("nombre", "Luis");
        data2.put("total", 0);
        data2.put("items", new ArrayList<>());
        data2.put("activo", false);
        data2.put("crudo", "x");
        String result2 = engine.render("prueba", data2);
        System.out.println("=== RESULTADO SIN ITEMS ===");
        System.out.println(result2);
        ok &= check(result2.contains("No hay items."), "muestra 'no hay items' cuando la lista está vacía");
        ok &= check(!result2.contains("- "), "no itera nada cuando la lista está vacía");
        ok &= check(result2.contains("Está inactivo"), "activo=false muestra bloque invertido");
        ok &= check(!result2.contains("Está activo"), "activo=false NO muestra bloque normal");

        System.out.println(ok ? "\nTODOS LOS CHECKS PASARON ✅" : "\nHAY FALLAS ❌");
        System.exit(ok ? 0 : 1);
    }

    static boolean check(boolean cond, String desc) {
        System.out.println((cond ? "OK   " : "FALLA") + " - " + desc);
        return cond;
    }
}

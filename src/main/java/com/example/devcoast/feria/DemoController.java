package com.example.devcoast.feria;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.devcoast.config.RoleGuard;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Usuario;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

// «Probar como talento / empresa»: entra con una cuenta demo sin registrarse. Solo existe con DEVCOAST_PERFIL=feria.
@Controller
@ConditionalOnProperty(name = "devcoast.perfil", havingValue = "feria")
public class DemoController {

    private static final long ESPERA_REINICIO_MS = 20_000L;

    private final MongoTemplate mongo;
    private final FeriaService feria;
    private final String token;
    private final AtomicLong ultimoReinicio = new AtomicLong();

    public DemoController(MongoTemplate mongo, FeriaService feria, @Value("${devcoast.feria.token:}") String token) {
        this.mongo = mongo;
        this.feria = feria;
        this.token = token == null ? "" : token.trim();
    }

    @GetMapping("/demo")
    public String pagina() {
        return "demo";
    }

    @PostMapping("/demo/{rol}")
    public String entrar(@PathVariable String rol, @RequestParam(defaultValue = "0") String presentacion, HttpServletRequest request,
            HttpSession session, RedirectAttributes ra) {
        String correo = switch (rol) {
            case "talento" -> FeriaService.DEMO_TALENTO;
            case "diseno" -> FeriaService.DEMO_DISENO;
            case "empresa" -> FeriaService.DEMO_EMPRESA;
            case "startup" -> FeriaService.DEMO_STARTUP;
            default -> null;
        };
        if (correo == null)
            return "redirect:/demo";
        boolean esEmpresa = rol.equals("empresa") || rol.equals("startup");
        String id;
        String nombre;
        String foto;
        if (esEmpresa) {
            Empresa e = mongo.findOne(Query.query(Criteria.where("correo").is(correo)), Empresa.class);
            if (e == null)
                return sinDatos(ra);
            id = e.getId();
            nombre = e.getNombreEmpresa();
            foto = e.getFotoUrl();
        } else {
            Usuario u = mongo.findOne(Query.query(Criteria.where("correo").is(correo)), Usuario.class);
            if (u == null)
                return sinDatos(ra);
            id = u.getId();
            nombre = u.getNombre();
            foto = u.getFotoUrl();
        }
        session.invalidate();
        HttpSession nueva = request.getSession(true);
        RoleGuard.iniciarSesion(nueva, id, correo, nombre, esEmpresa ? RoleGuard.TIPO_EMPRESA : RoleGuard.TIPO_TALENTO, foto);
        nueva.setAttribute("modoDemo", true);
        // el modo presentación cambia de cuenta varias veces y lleva su propia narración: no se le encima el recorrido guiado
        if (!"1".equals(presentacion))
            nueva.setAttribute("verRecorrido", true);
        return "redirect:/dashboard";
    }

    private String sinDatos(RedirectAttributes ra) {
        ra.addFlashAttribute("flashMessage", "La demo todavía no está lista. Avisa al equipo del stand.");
        ra.addFlashAttribute("flashType", "error");
        return "redirect:/login";
    }

    // botón «Reiniciar mi demo» de la barra lateral: deja las cuentas demo como al principio y conserva la sesión
    @PostMapping("/demo/reiniciar")
    public String reiniciarDesdeSesion(HttpSession session, RedirectAttributes ra) {
        if (!Boolean.TRUE.equals(session.getAttribute("modoDemo")))
            return "redirect:/login";
        long ahora = System.currentTimeMillis();
        if (ahora - ultimoReinicio.get() < ESPERA_REINICIO_MS) {
            ra.addFlashAttribute("flashMessage", "La demo se acaba de reiniciar. Prueba en unos segundos.");
            ra.addFlashAttribute("flashType", "info");
            return "redirect:/dashboard";
        }
        ultimoReinicio.set(ahora);
        try {
            DatosFeria datos = feria.leerDatos();
            if (!datos.valido())
                throw new IllegalStateException("datos con errores");
            feria.reiniciar(datos, false);
            ra.addFlashAttribute("flashMessage", "Demo reiniciada: todo vuelve a como estaba al principio.");
            ra.addFlashAttribute("flashType", "success");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("flashMessage", "No se pudo reiniciar la demo. Avisa al equipo del stand.");
            ra.addFlashAttribute("flashType", "error");
        }
        return "redirect:/dashboard";
    }

    // para el script feria/reiniciar-demo.sh. Sin DEVCOAST_FERIA_TOKEN configurado no existe (404); con él, exige la cabecera X-Feria-Token
    @PostMapping("/interno/demo/reiniciar")
    @ResponseBody
    public ResponseEntity<String> reiniciarDesdeScript(@RequestHeader(value = "X-Feria-Token", required = false) String enviado,
            @RequestParam(defaultValue = "false") boolean registros) {
        if (token.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        if (enviado == null || !MessageDigest.isEqual(enviado.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("token incorrecto\n");
        DatosFeria datos = feria.leerDatos();
        if (!datos.valido())
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body("los datos de la feria tienen errores:\n" + String.join("\n", datos.errores) + "\n");
        return ResponseEntity.ok(feria.reiniciar(datos, registros) + "\n");
    }
}
